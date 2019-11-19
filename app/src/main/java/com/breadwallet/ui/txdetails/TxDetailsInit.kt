package com.breadwallet.ui.txdetails

import com.spotify.mobius.First
import com.spotify.mobius.Init

object TxDetailsInit : Init<TxDetailsModel, TxDetailsEffect> {
    override fun init(model: TxDetailsModel): First<TxDetailsModel, TxDetailsEffect> {
        return First.first(
            model, setOf(
                TxDetailsEffect.LoadTransaction(model.currencyCode, model.transactionHash),
                TxDetailsEffect.LoadTransactionMetaData(model.transactionHash)
            )
        )
    }
}
