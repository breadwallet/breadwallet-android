package com.breadwallet.ui.atm

import cash.just.sdk.Cash
import com.breadwallet.BuildConfig

object BitcoinServer {
    fun getServer(): Cash.BtcNetwork{
        return if (BuildConfig.FLAVOR.contains("testnet", true)) {
            Cash.BtcNetwork.TEST_NET
        } else {
            Cash.BtcNetwork.MAIN_NET
        }
    }
}