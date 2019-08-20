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
import com.breadwallet.ui.util.QueuedConsumer
import com.spotify.mobius.*
import com.spotify.mobius.android.AndroidLogger
import com.spotify.mobius.android.MobiusAndroid
import com.spotify.mobius.android.runners.MainThreadWorkRunner
import com.spotify.mobius.disposables.Disposable
import com.spotify.mobius.functions.Consumer
import com.spotify.mobius.runners.WorkRunners
import kotlinx.android.extensions.LayoutContainer

abstract class BaseMobiusController<M, E, F>(
        args: Bundle? = null
) : BaseController(args),
        LayoutContainer,
        EventSource<E> {

    /** The default model used to construct [loopController]. */
    abstract val defaultModel: M
    /** The update function used to construct [loopFactory]. */
    abstract val update: Update<M, E, F>
    /** The init function used to construct [loopFactory]. */
    abstract val init: Init<M, F>
    /** The effect handler used to construct [loopFactory]. */
    abstract val effectHandler: Connectable<F, E>

    private val loopFactory by lazy {
        Mobius.loop(update, effectHandler)
                .init(init)
                .eventRunner { WorkRunners.cachedThreadPool() }
                .effectRunner { MainThreadWorkRunner.create() }
                .logger(AndroidLogger.tag(this::class.java.simpleName))
                .eventSource(this)
    }

    private val loopController by lazy {
        MobiusAndroid.controller(loopFactory, defaultModel)
    }

    /**
     * An entrypoint for adding platform events into a [MobiusLoop].
     *
     * When the loop has not been started, a [QueuedConsumer] is used
     * and all events will be dispatched when the loop is started.
     * Events dispatched from [onActivityResult] are one example of
     * of this use-case.
     */
    var eventConsumer: Consumer<E> = QueuedConsumer()
        private set

    /** The currently rendered model. */
    val currentModel: M
        get() = loopController.model

    /** The previously rendered model or null  */
    var previousModel: M? = null
        private set

    /** Called when [view] can attach listeners to dispatch events via [output]. */
    abstract fun bindView(output: Consumer<E>): Disposable

    /** Called when the model is updated or additional rendering is required. */
    abstract fun render(model: M)

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup): View {
        return super.onCreateView(inflater, container).apply {
            loopController.connect { output ->
                object : Connection<M>, Disposable by bindView(output) {
                    override fun accept(value: M) {
                        render(value)
                        previousModel = value
                    }
                }.also {
                    /* Initial render */
                    previousModel = null
                    render(loopController.model)
                    previousModel = loopController.model
                }
            }
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
        (eventConsumer as? QueuedConsumer)?.dequeueAll(newConsumer)
        eventConsumer = newConsumer
        return Disposable {
            eventConsumer = QueuedConsumer()
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
}