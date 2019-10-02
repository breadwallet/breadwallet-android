package com.breadwallet.ui.managewallets

data class ManageWalletsModel(
    val wallets: List<Wallet> = emptyList()
) {
    companion object {
        fun createDefault() = ManageWalletsModel()
    }
}

data class Wallet(
    val name: String,
    val currencyCode: String,
    val isErc20: Boolean,
    val currencyId: String,
    val enabled: Boolean = true
)