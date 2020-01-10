/**
 * BreadWallet
 *
 * Created by Ahsan Butt <ahsan.butt@breadwallet.com> on 7/26/19.
 * Copyright (c) 2019 breadwallet LLC
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
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
