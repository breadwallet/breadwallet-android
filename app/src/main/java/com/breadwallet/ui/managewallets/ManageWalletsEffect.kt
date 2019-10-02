package com.breadwallet.ui.managewallets

sealed class ManageWalletsEffect {
    object LoadEnabledWallets : ManageWalletsEffect()

    data class UpdateWallet(val currencyId: String, val isEnabled: Boolean) :
        ManageWalletsEffect()

    object GoToAddWallet : ManageWalletsEffect()
    object GoBack : ManageWalletsEffect()

    object DoNothing : ManageWalletsEffect() // TODO: Remove when issue with effects() is fixed
}