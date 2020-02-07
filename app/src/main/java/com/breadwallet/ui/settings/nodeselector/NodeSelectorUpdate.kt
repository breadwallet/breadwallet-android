/**
 * BreadWallet
 *
 * Created by Pablo Budelli <pablo.budelli@breadwallet.com> on 11/14/19.
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
package com.breadwallet.ui.settings.nodeselector

import com.breadwallet.crypto.WalletManagerState
import com.breadwallet.ui.settings.nodeselector.NodeSelector.E
import com.breadwallet.ui.settings.nodeselector.NodeSelector.F
import com.breadwallet.ui.settings.nodeselector.NodeSelector.M
import com.spotify.mobius.Next
import com.spotify.mobius.Next.dispatch
import com.spotify.mobius.Next.next
import com.spotify.mobius.Update

object NodeSelectorUpdate : Update<M, E, F>, NodeSelectorUpdateSpec {

    override fun update(
        model: M,
        event: E
    ): Next<M, F> = patch(model, event)

    override fun onSwitchButtonClicked(model: M): Next<M, F> {
        return dispatch(
            if (model.mode == NodeSelector.Mode.AUTOMATIC) {
                setOf(F.ShowNodeDialog)
            } else {
                setOf(F.SetToAutomatic)
            }
        )
    }

    override fun setCustomNode(
        model: M,
        event: E.SetCustomNode
    ): Next<M, F> =
        next(
            model.copy(
                mode = NodeSelector.Mode.AUTOMATIC,
                currentNode = event.node
            ),
            setOf(F.SetCustomNode(event.node))
        )

    override fun onConnectionStateUpdated(
        model: M,
        event: E.OnConnectionStateUpdated
    ): Next<M, F> =
        next(
            model.copy(
                connected = when (event.state) {
                    WalletManagerState.SYNCING(), WalletManagerState.CONNECTED() -> true
                    else -> false
                }
            )
        )

    override fun onConnectionInfoLoaded(
        model: M,
        event: E.OnConnectionInfoLoaded
    ): Next<M, F> =
        next(model.copy(mode = event.mode, currentNode = event.node))
}
