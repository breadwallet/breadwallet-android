/**
 * BreadWallet
 *
 *
 * Created by Ahsan Butt <ahsan.butt@breadwallet.com> 8/1/19.
 * Copyright (c) 2019 breadwallet LLC
 *
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package com.breadwallet.ui.home

import com.breadwallet.tools.manager.PromptManager
import java.math.BigDecimal

@Suppress("DataClassPrivateConstructor")
data class HomeScreenModel private constructor(
    val wallets: Map<String, Wallet> = emptyMap(),
    val promptId: PromptManager.PromptItem? = null,
    val hasInternet: Boolean = true,
    val isBuyBellNeeded: Boolean = false,
    val showBuyAndSell: Boolean = false
) {

    companion object {
        fun createDefault() =
                HomeScreenModel()
    }

    val aggregatedFiatBalance: BigDecimal = wallets.values
            .fold(BigDecimal.ZERO) {
                acc, next -> acc.add(next.fiatBalance)
            }

    val showPrompt: Boolean = promptId != null

    override fun toString(): String {
        return "HomeScreenModel(aggregatedFiatBalance=$aggregatedFiatBalance, " +
                "wallets=(size:${wallets.size}), " +
                "showPrompt='$showPrompt', " +
                "promptId=$promptId, " +
                "hasInternet=$hasInternet, " +
                "isBuyBellNeeded=$isBuyBellNeeded, " +
                "showBuyAndSell=$showBuyAndSell)"
    }
}

data class Wallet(
    val currencyName: String,
    val currencyCode: String,
    val fiatPricePerUnit: BigDecimal = BigDecimal.ZERO,
    val balance: BigDecimal = BigDecimal.ZERO,
    val fiatBalance: BigDecimal = BigDecimal.ZERO,
    val syncProgress: Double = 0.0,
    val syncingThroughMillis: Long = 0L
) {

    val isSyncing: Boolean = syncProgress < 1.0 && syncProgress > 0

    val hasSyncTime: Boolean = syncingThroughMillis != 0L

    val hasPricePerUnit: Boolean = fiatPricePerUnit != BigDecimal.ZERO
}