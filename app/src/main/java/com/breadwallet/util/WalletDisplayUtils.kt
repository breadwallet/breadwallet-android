/**
 * BreadWallet
 *
 * Created by Ahsan Butt <ahsan.butt@breadwallet.com> on 10/10/19.
 * Copyright (c) 2019 breadwallet LLC
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package com.breadwallet.util

import android.content.Context
import com.breadwallet.legacy.wallet.configs.WalletUiConfiguration
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

        fun getUIConfiguration(
            currencyCode: CurrencyCode,
            context: Context
        ): WalletUiConfiguration {
            // validate color here or return delisted
            return WalletUiConfiguration(
                TokenUtil.getTokenStartColor(currencyCode, context),
                TokenUtil.getTokenEndColor(currencyCode, context),
                false,
                MAX_DECIMAL_PLACES_FOR_UI
            )
        }
    }
}
