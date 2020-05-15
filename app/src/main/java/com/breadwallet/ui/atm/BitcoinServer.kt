package com.breadwallet.ui.atm

import cash.just.wac.Wac
import com.breadwallet.BuildConfig

object BitcoinServer {
    fun getServer(): Wac.BtcSERVER{
        return if (BuildConfig.FLAVOR.contains("testnet", true)) {
            Wac.BtcSERVER.TEST_NET
        } else {
            Wac.BtcSERVER.MAIN_NET
        }
    }
}