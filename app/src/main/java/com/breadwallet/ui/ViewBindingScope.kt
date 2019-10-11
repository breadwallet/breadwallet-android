package com.breadwallet.ui

import android.view.View
import android.widget.EditText
import com.breadwallet.util.DefaultTextWatcher
import com.spotify.mobius.disposables.Disposable
import com.spotify.mobius.functions.Consumer

class ViewBindingScope<E>(
    private val output: Consumer<E>
) : Disposable {

    private var _onDispose: (() -> Unit)? = null

    private val clickBoundViews = mutableSetOf<View>()
    private val textBoundViews = mutableMapOf<EditText, DefaultTextWatcher>()

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

    /**
     * Send the event returned by [produceEvent] to [output] using
     * [EditText.addTextChangedListener].
     */
    fun EditText.onTextChanged(
        produceEvent: (@ParameterName("text") String) -> E
    ) {
        textBoundViews[this] = object : DefaultTextWatcher() {
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                output.accept(produceEvent(text.toString()))
            }
        }.also { addTextChangedListener(it) }
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
        textBoundViews.forEach {
            it.key.removeTextChangedListener(it.value)
            textBoundViews.remove(it.key)
        }

        _onDispose?.invoke()
    }
}

fun <E> Consumer<E>.view(
    bind: ViewBindingScope<E>.() -> Unit
): Disposable = ViewBindingScope(this).apply(bind)
