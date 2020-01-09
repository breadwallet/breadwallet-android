package com.breadwallet.ui.settings.fastsync

import com.breadwallet.util.CurrencyCode

data class FastSyncModel(
    val currencyCode: CurrencyCode,
    val fastSyncEnable: Boolean = false,
    val currencyId: String = ""
) {

    companion object {
        fun createDefault(currencyCode: CurrencyCode): FastSyncModel {
            return FastSyncModel(currencyCode)
        }
    }
}
