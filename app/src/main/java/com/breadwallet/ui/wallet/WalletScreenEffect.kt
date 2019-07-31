package com.breadwallet.ui.wallet

import com.breadwallet.presenter.entities.CryptoRequest


sealed class WalletScreenEffect {
    data class UpdateCryptoPreferred(val cryptoPreferred: Boolean) : WalletScreenEffect()

    data class GoToSend(val currencyId: String, val cryptoRequest: CryptoRequest? = null) : WalletScreenEffect()
    data class GoToReceive(val currencyId: String) : WalletScreenEffect()
    data class GoToTransaction(val currencyId: String, val txHash: String) : WalletScreenEffect()

    object GoBack : WalletScreenEffect()
    object GoToBrdRewards : WalletScreenEffect()

    data class LoadWalletBalance(val currencyId: String) : WalletScreenEffect()
    data class LoadTransactions(val currencyId: String) : WalletScreenEffect()
    data class LoadFiatPricePerUnit(val currencyId: String) : WalletScreenEffect()

    object LoadCryptoPreferred : WalletScreenEffect()

    data class CheckReviewPrompt(val transactions: List<WalletTransaction>) : WalletScreenEffect()
    object RecordReviewPrompt : WalletScreenEffect()
    object RecordReviewPromptDismissed : WalletScreenEffect()
    object GoToReview : WalletScreenEffect()
}
