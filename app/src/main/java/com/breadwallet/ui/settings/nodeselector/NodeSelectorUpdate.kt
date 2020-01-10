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
import com.spotify.mobius.Next
import com.spotify.mobius.Next.dispatch
import com.spotify.mobius.Next.next
import com.spotify.mobius.Update

object NodeSelectorUpdate : Update<NodeSelectorModel, NodeSelectorEvent, NodeSelectorEffect>,
    NodeSelectorUpdateSpec {

    override fun update(
        model: NodeSelectorModel,
        event: NodeSelectorEvent
    ): Next<NodeSelectorModel, NodeSelectorEffect> = patch(model, event)

    override fun onSwitchButtonClicked(model: NodeSelectorModel): Next<NodeSelectorModel, NodeSelectorEffect> {
        return dispatch(
            if (model.mode == NodeSelectorModel.Mode.AUTOMATIC) {
                setOf(NodeSelectorEffect.ShowNodeDialog)
            } else {
                setOf(NodeSelectorEffect.SetToAutomatic)
            }
        )
    }

    override fun setCustomNode(
        model: NodeSelectorModel,
        event: NodeSelectorEvent.SetCustomNode
    ): Next<NodeSelectorModel, NodeSelectorEffect> =
        next(
            model.copy(mode = NodeSelectorModel.Mode.AUTOMATIC, currentNode = event.node),
            setOf(NodeSelectorEffect.SetCustomNode(event.node))
        )

    override fun onConnectionStateUpdated(
        model: NodeSelectorModel,
        event: NodeSelectorEvent.OnConnectionStateUpdated
    ): Next<NodeSelectorModel, NodeSelectorEffect> =
        next(model.copy(connected = event.state == WalletManagerState.CONNECTED()))

    override fun onConnectionInfoLoaded(
        model: NodeSelectorModel,
        event: NodeSelectorEvent.OnConnectionInfoLoaded
    ): Next<NodeSelectorModel, NodeSelectorEffect> =
        next(model.copy(mode = event.mode, currentNode = event.node))
}
