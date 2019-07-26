package com.breadwallet.ui.wallet

sealed class WalletEffect {

    data class LoadWalletBalance(val currencyId: String) : WalletEffect()
    data class LoadTransactions(val currencyId: String) : WalletEffect()

}