package com.breadwallet.ui.managewallets

import io.hypno.switchboard.MobiusUpdateSpec

@MobiusUpdateSpec(
    baseEffect = ManageWalletsEffect::class,
    baseModel = ManageWalletsModel::class
)
sealed class ManageWalletsEvent {
    data class OnHideClicked(val currencyId: String) : ManageWalletsEvent()
    data class OnShowClicked(val currencyId: String) : ManageWalletsEvent()
    data class OnWalletsReorder(val wallets: List<String>) : ManageWalletsEvent()
    object OnAddWalletClicked : ManageWalletsEvent()
    object OnBackClicked : ManageWalletsEvent()

    data class OnWalletsUpdated(val wallets: List<Wallet>) : ManageWalletsEvent()
}
