package com.breadwallet.ui.txdetails

import com.breadwallet.crypto.Transfer
import com.platform.entities.TxMetaData
import io.hypno.switchboard.MobiusUpdateSpec
import java.math.BigDecimal

@MobiusUpdateSpec(
    baseModel = TxDetailsModel::class,
    baseEffect = TxDetailsEffect::class
)

sealed class TxDetailsEvent {
    data class OnTransactionUpdated(
        val transaction: Transfer,
        val gasPrice: BigDecimal,
        val gasLimit: BigDecimal
    ) : TxDetailsEvent()

    data class OnFiatAmountNowUpdated(val fiatAmountNow: BigDecimal) : TxDetailsEvent()
    data class OnMetaDataUpdated(val metaData: TxMetaData) : TxDetailsEvent()
    data class OnMemoChanged(val memo: String) : TxDetailsEvent()
    object OnClosedClicked : TxDetailsEvent()
    object OnShowHideDetailsClicked : TxDetailsEvent()
}