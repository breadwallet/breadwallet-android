package com.breadwallet.effecthandler.metadata

import com.breadwallet.crypto.WalletManagerMode
import com.platform.entities.TxMetaData

sealed class MetaDataEvent {
    data class OnTransactionMetaDataUpdated(
        val transactionHash: String,
        val txMetaData: TxMetaData
    ) : MetaDataEvent()

    data class OnWalletModesUpdated(
        val modeMap: Map<String, WalletManagerMode>
    ) : MetaDataEvent()
}