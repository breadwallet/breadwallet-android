package com.breadwallet.util

import com.breadwallet.legacy.wallet.configs.WalletUiConfiguration
import com.breadwallet.tools.manager.BRSharedPrefs
import com.breadwallet.tools.util.BRConstants
import com.breadwallet.tools.util.TokenUtil
import java.math.BigDecimal

// TODO: Collecting wallet display methods from Wallet Managers here for now
//  class organization (whether hierarchy or not) is TBD, once we figure out what's needed
class WalletDisplayUtils {
    companion object {
        const val MAX_DECIMAL_PLACES_FOR_UI = 5
        private const val ETHER_WEI = "1000000000000000000"
        private val ONE_ETH = BigDecimal(ETHER_WEI)
        private const val SCALE_ETH = 8

        fun getUIConfiguration(currencyCode: CurrencyCode): WalletUiConfiguration {
            return WalletUiConfiguration(
                TokenUtil.getTokenStartColor(currencyCode),
                TokenUtil.getTokenEndColor(currencyCode),
                false,
                MAX_DECIMAL_PLACES_FOR_UI
            )
        }
    }
}
