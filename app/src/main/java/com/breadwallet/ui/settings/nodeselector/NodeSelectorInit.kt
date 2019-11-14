package com.breadwallet.ui.settings.nodeselector

import com.spotify.mobius.First
import com.spotify.mobius.Init

object NodeSelectorInit : Init<NodeSelectorModel, NodeSelectorEffect> {
    override fun init(model: NodeSelectorModel): First<NodeSelectorModel, NodeSelectorEffect> =
        First.first(model, setOf(NodeSelectorEffect.LoadConnectionInfo))
}