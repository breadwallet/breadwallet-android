package com.breadwallet.ui

import android.view.View
import com.spotify.mobius.disposables.Disposable
import com.spotify.mobius.functions.Consumer

class ViewBindingScope<E>(
    private val output: Consumer<E>
) : Disposable {

    private var _onDispose: (() -> Unit)? = null

    private val clickBoundViews = mutableSetOf<View>()

    /** Send [event] to [output] using [View.setOnClickListener].  */
    fun View.onClick(event: E) {
        clickBoundViews.add(this)
        setOnClickListener {
            output.accept(event)
        }
    }

    /**
     * Send the event returned by [produceEvent] to [output]
     * using [View.setOnClickListener].
     */
    fun View.onClick(
        produceEvent: (@ParameterName("view") View) -> E
    ) {
        clickBoundViews.add(this)
        setOnClickListener {
            output.accept(produceEvent(it))
        }
    }

    /** Invokes [dispose] when this scope is disposed. */
    fun onDispose(dispose: () -> Unit) {
        _onDispose = dispose
    }

    override fun dispose() {
        clickBoundViews.removeAll {
            it.setOnClickListener(null)
            true
        }
        _onDispose?.invoke()
    }
}

fun <E> Consumer<E>.view(
    bind: ViewBindingScope<E>.() -> Unit
): Disposable = ViewBindingScope(this).apply(bind)
