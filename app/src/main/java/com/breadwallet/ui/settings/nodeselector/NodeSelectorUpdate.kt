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