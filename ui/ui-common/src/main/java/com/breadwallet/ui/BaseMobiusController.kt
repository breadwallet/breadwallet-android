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
import android.view.View
import com.breadwallet.ext.throttleFirst
import com.breadwallet.mobius.ConsumerDelegate
import com.breadwallet.mobius.QueuedConsumer
import com.breadwallet.ui.navigation.NavigationEffect
import com.breadwallet.ui.navigation.Navigator
import com.breadwallet.util.errorHandler
import com.spotify.mobius.Connectable
import com.spotify.mobius.Connection
import com.spotify.mobius.EventSource
import com.spotify.mobius.First
import com.spotify.mobius.Init
import com.spotify.mobius.Mobius
import com.spotify.mobius.MobiusLoop
import com.spotify.mobius.Update
import com.spotify.mobius.android.AndroidLogger
import com.spotify.mobius.android.runners.MainThreadWorkRunner
import com.spotify.mobius.disposables.Disposable
import com.spotify.mobius.functions.Consumer
import drewcarlson.mobius.flow.DispatcherWorkRunner
import drewcarlson.mobius.flow.FlowTransformer
import drewcarlson.mobius.flow.asConnectable
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart.ATOMIC
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Dispatchers.Unconfined
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.channels.BroadcastChannel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.transform
import kotlinx.coroutines.launch
import org.kodein.di.Kodein
import org.kodein.di.erased.instance
import java.util.concurrent.atomic.AtomicBoolean

private const val MAX_QUEUED_VIEW_EFFECTS = 100

@Suppress("TooManyFunctions")
abstract class BaseMobiusController<M, E, F>(
    args: Bundle? = null
) : BaseController(args),
    EventSource<E> {

    override val kodein by Kodein.lazy {
        extend(super.kodein)
    }

    protected val uiBindScope = CoroutineScope(
        SupervisorJob() + Dispatchers.Main + errorHandler("uiBindScope")
    )

    private val navigator by instance<Navigator>()
    private val viewEffectChannel = Channel<ViewEffect>(MAX_QUEUED_VIEW_EFFECTS)

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

    private var loop: MobiusLoop<M, E, F>? = null
    private val isLoopActive = AtomicBoolean(false)
    private val logger = AndroidLogger.tag<M, E, F>(this::class.java.simpleName)
    private val loopFactory by lazy {
        val loopConnection = Connectable<F, E> { output ->
            val handler = checkNotNull(effectHandler ?: flowEffectHandler?.asConnectable()) {
                "effectHandler or flowEffectHandler must be implemented."
            }

            val connection = handler.connect(output)
            object : Connection<F> {
                override fun accept(value: F) {
                    if (value is ViewEffect) {
                        check(viewEffectChannel.offer(value)) {
                            "ViewEffect queue capacity exceeded."
                        }
                    } else {
                        connection.accept(value)
                    }
                }

                override fun dispose() {
                    connection.dispose()
                    // Dispose any queued effects
                    viewEffectChannel.receiveAsFlow()
                        .launchIn(GlobalScope)
                }
            }
        }
        Mobius.loop(update, loopConnection)
            .eventRunner { DispatcherWorkRunner(Dispatchers.Default) }
            .effectRunner { DispatcherWorkRunner(Dispatchers.Default) }
            .logger(logger)
            .eventSource(this)
    }

    private var disposeConnection: (() -> Unit)? = null

    private val eventConsumerDelegate = ConsumerDelegate<E>(QueuedConsumer())
    private val modelChannel = BroadcastChannel<M>(Channel.CONFLATED)

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
        get() = loop?.mostRecentModel ?: defaultModel

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

    open fun handleViewEffect(effect: ViewEffect) = Unit

    override fun onAttach(view: View) {
        super.onAttach(view)

        collectViewEffects()

        if (loop == null) {
            createLoop()
            connectView(checkNotNull(loop))
        } else {
            previousModel = null
            connectView(checkNotNull(loop))
            currentModel.render()
        }
    }

    override fun onDetach(view: View) {
        disconnectView()
        super.onDetach(view)
    }

    override fun onDestroy() {
        disposeLoop()
        super.onDestroy()
    }

    override fun subscribe(newConsumer: Consumer<E>): Disposable {
        (eventConsumerDelegate.consumer as? QueuedConsumer)?.dequeueAll(newConsumer)
        eventConsumerDelegate.consumer = newConsumer
        return Disposable {
            eventConsumerDelegate.consumer = QueuedConsumer()
        }
    }

    private fun createLoop() {
        logger.beforeInit(currentModel)
        val first = init.init(currentModel)
        logger.afterInit(currentModel, first)
        val workRunner = MainThreadWorkRunner.create()
        loop = loopFactory.startFrom(first.model(), first.effects()).apply {
            observe { model ->
                modelChannel.offer(model)

                workRunner.post {
                    if (isAttached) model.render()
                    previousModel = model
                }
            }
        }
        isLoopActive.set(true)
    }

    private fun disposeLoop() {
        isLoopActive.set(false)
        disposeConnection = null
        loop?.dispose()
        loop = null
    }

    private fun connectView(loop: MobiusLoop<M, E, F>) {
        val legacyDispose = bindView(Consumer { event ->
            if (isLoopActive.get()) {
                loop.dispatchEvent(event)
            }
        })

        val job = SupervisorJob()
        val scope = CoroutineScope(job + Dispatchers.Main + errorHandler("modelConsumer"))
        scope.launch(Unconfined) {
            bindView(modelChannel.asFlow()).collect { event ->
                ensureActive()
                if (isLoopActive.get()) {
                    loop.dispatchEvent(event)
                }
            }
        }
        disposeConnection = {
            legacyDispose.dispose()
            // Ensure the view binding flow is fully cancelled before
            // the view is detached.
            // Unconfined: Immediately execute to first suspension point.
            // Atomic: Immediately execute coroutine in launched context.
            scope.launch(Unconfined, ATOMIC) {
                job.cancelAndJoin()
            }
        }
    }

    private fun disconnectView() {
        disposeConnection?.invoke()
        disposeConnection = null
    }

    private fun collectViewEffects() {
        viewEffectChannel.receiveAsFlow()
            .transform { effect ->
                if (effect is NavigationEffect) {
                    emit(effect)
                } else {
                    handleViewEffect(effect)
                }
            }
            .filterIsInstance<NavigationEffect>()
            .throttleFirst(500L)
            .onEach { effect ->
                navigator.navigateTo(effect.navigationTarget)
            }
            .flowOn(Dispatchers.Main)
            .launchIn(viewAttachScope)
    }

    inline fun <T, reified M2 : M> extractOrUnit(
        model: M?,
        crossinline extract: (M2) -> T
    ): Any? {
        return if (model is M2) model.run(extract) else Unit
    }

    /**
     * Invokes [block] only when the result of [extract] on
     * [this] is not equal to [extract] on [previousModel].
     *
     * [block] supplies the value extracted from [currentModel].
     */
    inline fun <T, reified M2 : M> M2.ifChanged(
        crossinline extract: (M2) -> T,
        crossinline block: (@ParameterName("value") T) -> Unit
    ) {
        val currentValue = extract(this)
        val previousValue = extractOrUnit(previousModel, extract)
        if (currentValue != previousValue) {
            block(currentValue)
        }
    }

    /**
     * Invokes [block] if the result of any extract functions on
     * [this] are not equal to the same function on the [previousModel].
     */
    inline fun <T1, T2, reified M2 : M> M2.ifChanged(
        crossinline extract1: (M2) -> T1,
        crossinline extract2: (M2) -> T2,
        crossinline block: () -> Unit
    ) {
        if (
            extract1(this) != extractOrUnit(previousModel, extract1) ||
            extract2(this) != extractOrUnit(previousModel, extract2)
        ) {
            block()
        }
    }

    /**
     * Invokes [block] if the result of any extract functions on
     * [this] are not equal to the same function on the [previousModel].
     */
    inline fun <T1, T2, T3, reified M2 : M> M2.ifChanged(
        crossinline extract1: (M2) -> T1,
        crossinline extract2: (M2) -> T2,
        crossinline extract3: (M2) -> T3,
        crossinline block: () -> Unit
    ) {
        if (
            extract1(this) != extractOrUnit(previousModel, extract1) ||
            extract2(this) != extractOrUnit(previousModel, extract2) ||
            extract3(this) != extractOrUnit(previousModel, extract3)
        ) {
            block()
        }
    }

    /**
     * Invokes [block] if the result of any extract functions on
     * [this] are not equal to the same function on the [previousModel].
     */
    @Suppress("ComplexCondition")
    inline fun <T1, T2, T3, T4, reified M2 : M> M2.ifChanged(
        crossinline extract1: (M2) -> T1,
        crossinline extract2: (M2) -> T2,
        crossinline extract3: (M2) -> T3,
        crossinline extract4: (M2) -> T4,
        crossinline block: () -> Unit
    ) {
        if (
            extract1(this) != extractOrUnit(previousModel, extract1) ||
            extract2(this) != extractOrUnit(previousModel, extract2) ||
            extract3(this) != extractOrUnit(previousModel, extract3) ||
            extract4(this) != extractOrUnit(previousModel, extract4)
        ) {
            block()
        }
    }
}
