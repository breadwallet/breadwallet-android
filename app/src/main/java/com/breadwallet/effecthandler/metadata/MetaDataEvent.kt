package com.breadwallet.effecthandler.metadata

import com.platform.entities.TxMetaData

sealed class MetaDataEvent {
    data class OnTransactionMetaDataUpdated(
        val transactionHash: String,
        val txMetaData: TxMetaData
    ) : MetaDataEvent()
}