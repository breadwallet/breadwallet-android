/**
 * BreadWallet
 * <p/>
 * Created by Drew Carlson <drew.carlson@breadwallet.com> 7/25/19.
 * Copyright (c) 2019 breadwallet LLC
 * <p/>
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 * <p/>
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 * <p/>
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package com.breadwallet.ui.util

import com.spotify.mobius.Connectable
import com.spotify.mobius.Connection
import com.spotify.mobius.functions.Consumer

/**
 * Produces a Connectable, which when connected, returns an outer effect handler
 * that maps effects to a different type, given a mapping function,
 * and dispatches to an inner effect handler.
 */
fun <OuterI, OuterO, InnerI, InnerO> nestedConnectable(
        connectionProducer: (Consumer<InnerO>) -> Connection<InnerI>,
        mapEffect: (OuterI) -> InnerI?,
        mapEvent: (InnerO) -> OuterO?
): Connectable<OuterI, OuterO> = object : Connectable<OuterI, OuterO> {
    override fun connect(output: Consumer<OuterO>): Connection<OuterI> {
        val connection = connectionProducer(Consumer {
            output.accept(mapEvent(it) ?: return@Consumer)
        })
        return object : Connection<OuterI> {
            override fun accept(value: OuterI) {
                connection.accept(mapEffect(value) ?: return)
            }

            override fun dispose() {
                connection.dispose()
            }
        }
    }
}

fun <OuterI, OuterO, InnerI> nestedConnectable(
        connectionProducer: () -> Connection<InnerI>,
        mapEffect: (OuterI) -> InnerI?
): Connectable<OuterI, OuterO> = object : Connectable<OuterI, OuterO> {
    override fun connect(output: Consumer<OuterO>): Connection<OuterI> {

        val connection = connectionProducer()
        return object : Connection<OuterI> {
            override fun accept(value: OuterI) {
                connection.accept(mapEffect(value) ?: return)
            }

            override fun dispose() {
                connection.dispose()
            }
        }
    }
}
