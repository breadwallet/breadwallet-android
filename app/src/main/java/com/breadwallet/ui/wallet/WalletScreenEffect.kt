package com.breadwallet.ui.wallet

import com.breadwallet.crypto.Transfer
import com.breadwallet.crypto.Wallet
import com.breadwallet.legacy.presenter.entities.CryptoRequest
import com.breadwallet.ui.navigation.NavEffectHolder
import com.breadwallet.ui.navigation.NavigationEffect

sealed class WalletScreenEffect {
    data class UpdateCryptoPreferred(val cryptoPreferred: Boolean) : WalletScreenEffect()

    sealed class Nav : WalletScreenEffect(), NavEffectHolder {
        data class GoToSend(
            val currencyId: String,
            val cryptoRequest: CryptoRequest? = null
        ) : Nav() {
            override val navigationEffect =
                NavigationEffect.GoToSend(currencyId, cryptoRequest)
        }

        data class GoToReceive(val currencyId: String) : Nav() {
            override val navigationEffect =
                NavigationEffect.GoToReceive(currencyId)
        }
        data class GoToTransaction(
            val currencyId: String,
            val txHash: String
        ) : Nav() {
            override val navigationEffect =
                NavigationEffect.GoToTransaction(currencyId, txHash)
        }

        object GoBack : Nav() {
            override val navigationEffect = NavigationEffect.GoBack
        }
        object GoToBrdRewards : Nav() {
            override val navigationEffect = NavigationEffect.GoToReview
        }
    }

    data class LoadCurrencyName(val currencyId: String) : WalletScreenEffect()
    data class LoadSyncState(val currencyId: String) : WalletScreenEffect()
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
        val currencyCode: String,
        val transactions: List<WalletTransaction>
    ) : WalletScreenEffect() {
        override fun toString() = "CheckReviewPrompt(transactions=(size:${transactions.size}))"
    }

    object RecordReviewPrompt : WalletScreenEffect()
    object RecordReviewPromptDismissed : WalletScreenEffect()
    object GoToReview : WalletScreenEffect()

    data class LoadChartInterval(
        val interval: Interval,
        val currencyCode: String
    ) : WalletScreenEffect()

    data class TrackEvent(
        val eventName: String,
        val attributes: Map<String, String>? = null
    ) : WalletScreenEffect()
}
