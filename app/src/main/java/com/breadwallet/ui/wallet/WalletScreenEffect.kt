package com.breadwallet.ui.wallet

import com.breadwallet.crypto.Transfer
import com.breadwallet.legacy.presenter.entities.CryptoRequest

sealed class WalletScreenEffect {
    data class UpdateCryptoPreferred(val cryptoPreferred: Boolean) : WalletScreenEffect()

    data class GoToSend(
        val currencyId: String,
        val cryptoRequest: CryptoRequest? = null
    ) : WalletScreenEffect()

    data class GoToReceive(val currencyId: String) : WalletScreenEffect()
    data class GoToTransaction(val currencyId: String, val txHash: String) : WalletScreenEffect()

    object GoBack : WalletScreenEffect()
    object GoToBrdRewards : WalletScreenEffect()

    data class LoadWalletBalance(val currencyId: String) : WalletScreenEffect()
    data class LoadTransactions(val currencyId: String) : WalletScreenEffect()
    data class LoadFiatPricePerUnit(val currencyId: String) : WalletScreenEffect()
    data class LoadTransactionMetaData(val transactionHashes: List<String>) : WalletScreenEffect()
    data class LoadIsTokenSupported(val currencyCode: String) : WalletScreenEffect()

    object LoadCryptoPreferred : WalletScreenEffect()

    data class ConvertCryptoTransactions(
        val transactions: List<Transfer>
    ) : WalletScreenEffect() {
        override fun toString() =
            "ConvertCryptoTransactions(transactions=(size:${transactions.size}))"
    }

    data class CheckReviewPrompt(
        val transactions: List<WalletTransaction>
    ) : WalletScreenEffect() {
        override fun toString() = "CheckReviewPrompt(transactions=(size:${transactions.size}))"
    }

    object RecordReviewPrompt : WalletScreenEffect()
    object RecordReviewPromptDismissed : WalletScreenEffect()
    object GoToReview : WalletScreenEffect()

    data class LoadChartInterval(val interval: Interval) : WalletScreenEffect()

    data class TrackEvent(
        val eventName: String,
        val attributes: Map<String, String>? = null
    ) : WalletScreenEffect()
}
