package com.breadwallet.ui.txdetails

import com.breadwallet.util.CurrencyCode
import java.math.BigDecimal

sealed class TxDetailsEffect {

    data class LoadTransaction(
        val currencyCode: CurrencyCode,
        val transactionHash: String
    ) : TxDetailsEffect()

    data class LoadTransactionMetaData(val transactionHash: String) : TxDetailsEffect()

    data class LoadFiatAmountNow(
        val cryptoTransferredAmount: BigDecimal,
        val currencyCode: String,
        val preferredFiatIso: String
    ) : TxDetailsEffect()

    data class UpdateMemo(
        val transactionHash: String,
        val memo: String
    ) : TxDetailsEffect()

    object Close : TxDetailsEffect()
}