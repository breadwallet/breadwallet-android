/**
 * BreadWallet
 *
 * Created by Drew Carlson <drew.carlson@breadwallet.com> 7/22/19.
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
package com.breadwallet.mobius

import com.spotify.mobius.Connectable
import com.spotify.mobius.Connection
import com.spotify.mobius.functions.Consumer


/**
 * Creates a [Connectable] that delegates connection creation to [effectHandlers]
 * and the corresponding [Connection]s.
 */
class CompositeEffectHandler<I, O> private constructor(
    private val effectHandlers: Array<out Connectable<I, O>>
) : Connectable<I, O> {

    companion object {
        fun <I, O> from(vararg effectHandlers: Connectable<I, O>) =
            CompositeEffectHandler(effectHandlers)
    }

    override fun connect(output: Consumer<O>): Connection<I> {
        val consumers = effectHandlers.map { it.connect(output) }

        return object : Connection<I> {
            override fun accept(value: I) =
                consumers.forEach { it.accept(value) }

            override fun dispose() =
                consumers.forEach { it.dispose() }
        }
    }
}