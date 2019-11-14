package com.breadwallet.ui.settings.nodeselector

import com.breadwallet.crypto.WalletManagerState
import io.hypno.switchboard.MobiusUpdateSpec

@MobiusUpdateSpec(
    baseModel = NodeSelectorModel::class,
    baseEffect = NodeSelectorEffect::class
)
sealed class NodeSelectorEvent {
    object OnSwitchButtonClicked : NodeSelectorEvent()

    data class OnConnectionStateUpdated(val state: WalletManagerState) : NodeSelectorEvent()

    data class OnConnectionInfoLoaded(
        val mode: NodeSelectorModel.Mode,
        val node: String = ""
    ) : NodeSelectorEvent()

    data class SetCustomNode(val node: String) : NodeSelectorEvent()
}