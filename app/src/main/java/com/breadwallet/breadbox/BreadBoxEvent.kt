/**
 * BreadWallet
 *
 *
 * Created by Ahsan Butt <ahsan.butt@breadwallet.com> 8/1/19.
 * Copyright (c) 2019 breadwallet LLC
 *
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package com.breadwallet.breadbox

import com.breadwallet.ui.wallet.WalletTransaction
import java.math.BigDecimal

sealed class BreadBoxEvent {
    data class OnSyncProgressUpdated(
        val progress: Float,
        val syncThroughMillis: Long,
        val isSyncing: Boolean
    ) : BreadBoxEvent() {
        init {
            require(progress in 0.0..1.0) {
                "Sync progress must be in 0..1 but was $progress"
            }
        }
    }

    data class OnCurrencyNameUpdated(val name: String) : BreadBoxEvent()
    data class OnBalanceUpdated(val balance: BigDecimal, val fiatBalance: BigDecimal) :
        BreadBoxEvent()

    data class OnTransactionsUpdated(val walletTransactions: List<WalletTransaction>) :
        BreadBoxEvent()

    data class OnTransactionAdded(val walletTransaction: WalletTransaction) : BreadBoxEvent()
    data class OnTransactionRemoved(val walletTransaction: WalletTransaction) : BreadBoxEvent()
    data class OnTransactionUpdated(val walletTransaction: WalletTransaction) : BreadBoxEvent()
    data class OnConnectionUpdated(val isConnected: Boolean) : BreadBoxEvent()
}