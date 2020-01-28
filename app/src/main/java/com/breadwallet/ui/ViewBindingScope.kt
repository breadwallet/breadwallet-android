/**
 * BreadWallet
 *
 * Created by Drew Carlson <drew.carlson@breadwallet.com> on 10/10/19.
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

import android.view.View
import android.widget.CompoundButton
import android.widget.EditText
import android.widget.Switch
import com.breadwallet.util.DefaultTextWatcher
import com.spotify.mobius.disposables.Disposable
import com.spotify.mobius.functions.Consumer

class ViewBindingScope<E>(
    private val output: Consumer<E>
) : Disposable {

    private var _onDispose: (() -> Unit)? = null

    private val clickBoundViews = mutableSetOf<View>()
    private val textBoundViews = mutableMapOf<EditText, DefaultTextWatcher>()
    private val switchBoundViews = mutableMapOf<Switch, CompoundButton.OnCheckedChangeListener>()

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
     * [Switch.setOnCheckedChangeListener]
     */
    fun Switch.onCheckChanged(
        produceEvent: (@ParameterName("isChecked") Boolean) -> E
    ) {
        switchBoundViews[this] = CompoundButton.OnCheckedChangeListener { _, isChecked ->
            output.accept(produceEvent(isChecked))
        }.also { setOnCheckedChangeListener(it) }
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
        }
        textBoundViews.clear()
        switchBoundViews.forEach {
            it.key.setOnCheckedChangeListener(null)
        }
        switchBoundViews.clear()

        _onDispose?.invoke()
    }
}

fun <E> Consumer<E>.view(
    bind: ViewBindingScope<E>.() -> Unit
): Disposable = ViewBindingScope(this).apply(bind)
