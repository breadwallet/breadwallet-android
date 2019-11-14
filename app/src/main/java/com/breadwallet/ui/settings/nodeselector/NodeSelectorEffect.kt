package com.breadwallet.ui.settings.nodeselector

sealed class NodeSelectorEffect {
    object ShowNodeDialog : NodeSelectorEffect()
    object LoadConnectionInfo : NodeSelectorEffect()
    object SetToAutomatic : NodeSelectorEffect()
    data class SetCustomNode(val node: String) : NodeSelectorEffect()
}