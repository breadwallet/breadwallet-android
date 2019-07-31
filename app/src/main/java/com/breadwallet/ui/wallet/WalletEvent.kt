package com.breadwallet.ui.wallet

import java.math.BigDecimal


sealed class WalletEvent {
    data class OnSyncProgressUpdated(
            val progress: Double,
            val syncThroughMillis: Long
    ) : WalletEvent() {
        init {
            require(progress in 0.0..1.0) {
                "Sync progress must be in 0..1 but was $progress"
            }
        }
    }

    data class OnCurrencyNameUpdated(val name: String) : WalletEvent()
    data class OnBalanceUpdated(val balance: BigDecimal, val fiatBalance: BigDecimal) : WalletEvent()
    data class OnTransactionsUpdated(val walletTransactions: List<WalletTransaction>) : WalletEvent()
    data class OnTransactionAdded(val walletTransaction: WalletTransaction) : WalletEvent()
    data class OnTransactionRemoved(val walletTransaction: WalletTransaction) : WalletEvent()
    data class OnTransactionUpdated(val walletTransaction: WalletTransaction) : WalletEvent()
    data class OnConnectionUpdated(val isConnected: Boolean) : WalletEvent()

}