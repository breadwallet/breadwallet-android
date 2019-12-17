/**
 * BreadWallet
 *
 * Created by Drew Carlson <drew.carlson@breadwallet.com> on 8/13/19.
 * Copyright (c) 2019 breadwallet LLC
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package com.breadwallet.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.breadwallet.legacy.presenter.activities.util.BRActivity
import com.breadwallet.mobius.ConsumerDelegate
import com.breadwallet.mobius.QueuedConsumer
import com.breadwallet.ui.navigation.NavEffectHolder
import com.breadwallet.ui.navigation.NavEffectTransformer
import com.breadwallet.ui.navigation.NavigationEffectHandler
import com.breadwallet.ui.navigation.RouterNavigationEffectHandler
import com.spotify.mobius.Connectable
import com.spotify.mobius.EventSource
import com.spotify.mobius.First
import com.spotify.mobius.Init
import com.spotify.mobius.MobiusLoop
import com.spotify.mobius.Update
import com.spotify.mobius.android.AndroidLogger
import com.spotify.mobius.android.MobiusAndroid
import com.spotify.mobius.disposables.Disposable
import com.spotify.mobius.flow.DispatcherWorkRunner
import com.spotify.mobius.flow.FlowMobius
import com.spotify.mobius.flow.FlowTransformer
import com.spotify.mobius.flow.flowConnectable
import com.spotify.mobius.flow.flowTransformer
import com.spotify.mobius.flow.transform
import com.spotify.mobius.functions.Consumer
import kotlinx.android.extensions.LayoutContainer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.consumeAsFlow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.merge
import kotlinx.coroutines.flow.onCompletion
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.onStart
import org.kodein.di.Kodein
import org.kodein.di.erased.bind
import org.kodein.di.erased.instance
import org.kodein.di.erased.provider

@UseExperimental(ExperimentalCoroutinesApi::class)
@Suppress("TooManyFunctions") // TODO: Extract render DSL or replace with Flows
abstract class BaseMobiusController<M, E, F>(
    args: Bundle? = null
) : BaseController(args),
    LayoutContainer,
    EventSource<E> {

    override val kodein by Kodein.lazy {
        extend(super.kodein)

        bind<RouterNavigationEffectHandler>() with provider {
            RouterNavigationEffectHandler(router)
        }

        bind<NavigationEffectHandler>() with provider {
            NavigationEffectHandler(activity as BRActivity)
        }

        bind<NavEffectTransformer>() with provider {
            NavEffectTransformer(instance())
        }
    }

    protected val uiBindScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    /** The default model used to construct [loopController]. */
    abstract val defaultModel: M
    /** The update function used to construct [loopFactory]. */
    abstract val update: Update<M, E, F>
    /** The init function used to construct [loopFactory]. */
    open val init: Init<M, F> = Init { First.first(it) }
    /** The effect handler used to construct [loopFactory]. */
    open val effectHandler: Connectable<F, E>? = null

    // TODO: Make abstract and non-nullable when children only implement flowEffectHandler
    /** The effect handler used to construct [loopFactory]. */
    open val flowEffectHandler: FlowTransformer<F, E>? = null

    private val loopFactory by lazy {
        // TODO: Remove this check when children implement only flowEffectHandler
        val handler = if (effectHandler == null) {
            checkNotNull(flowEffectHandler) {
                "flowEffectHandler must be implemented"
            }
        } else {
            flowTransformer { effects ->
                effects.transform(checkNotNull(effectHandler))
            }
        }

        FlowMobius.loop(update, handler)
            .init(init)
            .eventRunner { DispatcherWorkRunner(Dispatchers.Default) }
            .effectRunner { DispatcherWorkRunner(Dispatchers.Default) }
            .logger(AndroidLogger.tag(this::class.java.simpleName))
            .eventSource(this)
    }

    private val loopController by lazy {
        MobiusAndroid.controller(loopFactory, defaultModel)
    }

    private val eventConsumerDelegate = ConsumerDelegate<E>(QueuedConsumer())

    /**
     * An entrypoint for adding platform events into a [MobiusLoop].
     *
     * When the loop has not been started, a [QueuedConsumer] is used
     * and all events will be dispatched when the loop is started.
     * Events dispatched from [onActivityResult] are one example of
     * of this use-case.
     */
    val eventConsumer: Consumer<E> = eventConsumerDelegate

    /** The currently rendered model. */
    val currentModel: M
        get() = loopController.model

    /** The previously rendered model or null  */
    var previousModel: M? = null
        private set

    /** Called when [view] can attach listeners to dispatch events via [output]. */
    open fun bindView(output: Consumer<E>): Disposable = Disposable { }

    /** Called when the model is updated or additional rendering is required. */
    open fun M.render() = Unit

    /**
     * Called when instances of model [M] can be collected
     * from [modelFlow].
     *
     * The returned [Flow] will be collected while the loop
     * is running to dispatch each event [E].
     */
    open fun bindView(modelFlow: Flow<M>): Flow<E> = emptyFlow()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup): View {
        return super.onCreateView(inflater, container).apply {
            // TODO: This code maintains the old render and model diffing support
            //  once all callers have migrated, it can be replaced with this one line.
            //  loopController.connect(flowConnectable(transform = ::bindView))
            loopController.connect(flowConnectable { modelFlow ->
                val eventChannel = Channel<E>(Channel.BUFFERED)
                val disposable = bindView(Consumer { event ->
                    eventChannel.offer(event)
                })

                val scope = CoroutineScope(Dispatchers.Main)
                modelFlow
                    .onStart {
                        previousModel = null
                        loopController.model.render()
                        previousModel = loopController.model
                    }
                    .onEach { model ->
                        model.render()
                        previousModel = model
                    }
                    .launchIn(scope)

                merge(
                    bindView(modelFlow),
                    eventChannel.consumeAsFlow()
                ).onCompletion {
                    scope.cancel()
                    disposable.dispose()
                }
            })
        }
    }

    override fun onDestroyView(view: View) {
        loopController.disconnect()
        super.onDestroyView(view)
    }

    override fun onAttach(view: View) {
        super.onAttach(view)
        loopController.start()
    }

    override fun onDetach(view: View) {
        super.onDetach(view)
        loopController.stop()
    }

    override fun subscribe(newConsumer: Consumer<E>): Disposable {
        (eventConsumerDelegate.consumer as? QueuedConsumer)?.dequeueAll(newConsumer)
        eventConsumerDelegate.consumer = newConsumer
        return Disposable {
            eventConsumerDelegate.consumer = QueuedConsumer()
        }
    }

    /**
     * Invokes [block] only when the result of [extract] on
     * [this] is not equal to [extract] on [previousModel].
     *
     * [block] supplies the value extracted from [currentModel].
     */
    inline fun <T> M.ifChanged(
        crossinline extract: (M) -> T,
        crossinline block: (@ParameterName("value") T) -> Unit
    ) {
        val currentValue = extract(this)
        val previousValue = previousModel?.let(extract)
        if (currentValue != previousValue) {
            block(currentValue)
        }
    }

    /**
     * Invokes [block] if the result of any extract functions on
     * [this] are not equal to the same function on the [previousModel].
     */
    inline fun <T1, T2> M.ifChanged(
        crossinline extract1: (M) -> T1,
        crossinline extract2: (M) -> T2,
        crossinline block: () -> Unit
    ) {
        val currentValue1 = extract1(this)
        val previousValue1 = previousModel?.let(extract1)
        val currentValue2 = extract2(this)
        val previousValue2 = previousModel?.let(extract2)
        if (
            currentValue1 != previousValue1 ||
            currentValue2 != previousValue2
        ) {
            block()
        }
    }

    /**
     * Invokes [block] if the result of any extract functions on
     * [this] are not equal to the same function on the [previousModel].
     */
    inline fun <T1, T2, T3> M.ifChanged(
        crossinline extract1: (M) -> T1,
        crossinline extract2: (M) -> T2,
        crossinline extract3: (M) -> T3,
        crossinline block: () -> Unit
    ) {
        val currentValue1 = extract1(this)
        val previousValue1 = previousModel?.let(extract1)
        val currentValue2 = extract2(this)
        val previousValue2 = previousModel?.let(extract2)
        val currentValue3 = extract3(this)
        val previousValue3 = previousModel?.let(extract3)
        if (
            currentValue1 != previousValue1 ||
            currentValue2 != previousValue2 ||
            currentValue3 != previousValue3
        ) {
            block()
        }
    }

    /**
     * Invokes [block] if the result of any extract functions on
     * [this] are not equal to the same function on the [previousModel].
     */
    @Suppress("ComplexCondition")
    inline fun <T1, T2, T3, T4> M.ifChanged(
        crossinline extract1: (M) -> T1,
        crossinline extract2: (M) -> T2,
        crossinline extract3: (M) -> T3,
        crossinline extract4: (M) -> T4,
        crossinline block: () -> Unit
    ) {
        val currentValue1 = extract1(this)
        val previousValue1 = previousModel?.let(extract1)
        val currentValue2 = extract2(this)
        val previousValue2 = previousModel?.let(extract2)
        val currentValue3 = extract3(this)
        val previousValue3 = previousModel?.let(extract3)
        val currentValue4 = extract4(this)
        val previousValue4 = previousModel?.let(extract4)
        if (
            currentValue1 != previousValue1 ||
            currentValue2 != previousValue2 ||
            currentValue3 != previousValue3 ||
            currentValue4 != previousValue4
        ) {
            block()
        }
    }
}
