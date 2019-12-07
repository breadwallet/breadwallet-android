package com.breadwallet.ui.settings.fastsync

data class FastSyncModel(
    val fastSyncEnable: Boolean = false,
    val bitcoinCurrencyId: String = ""
) {

    companion object {
        fun createDefault(): FastSyncModel {
            return FastSyncModel()
        }
    }
}