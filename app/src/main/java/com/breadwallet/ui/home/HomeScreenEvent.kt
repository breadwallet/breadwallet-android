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

import com.breadwallet.model.InAppMessage
import com.breadwallet.model.PriceChange
import com.breadwallet.tools.manager.PromptManager
import java.math.BigDecimal

sealed class HomeScreenEvent {

    data class OnWalletSyncProgressUpdated(
            val currencyCode: String,
            val progress: Float,
            val syncThroughMillis: Long,
            val isSyncing: Boolean
    ) : HomeScreenEvent() {
        init {
            require(progress in 0f..1f) {
                "Sync progress must be in 0..1 but was $progress"
            }
        }
    }
    data class OnWalletsAdded(val wallets: List<Wallet>) : HomeScreenEvent()
    data class OnWalletAdded(val wallet: Wallet) : HomeScreenEvent()
    data class OnWalletBalanceUpdated(val currencyCode: String,
                                      val balance : BigDecimal,
                                      val fiatBalance: BigDecimal,
                                      val fiatPricePerUnit: BigDecimal,
                                      val priceChange: PriceChange? = null
    ) : HomeScreenEvent()

    data class OnBuyBellNeededLoaded(val isBuyBellNeeded:Boolean) : HomeScreenEvent()

    data class OnConnectionUpdated(val isConnected: Boolean) : HomeScreenEvent()

    object OnAddWalletClicked : HomeScreenEvent()
    data class OnWalletClicked(val currencyCode: String) : HomeScreenEvent()

    object OnBuyClicked : HomeScreenEvent()
    object OnTradeClicked : HomeScreenEvent()
    object OnMenuClicked : HomeScreenEvent()

    data class OnDeepLinkProvided(val url: String) : HomeScreenEvent()
    data class OnInAppNotificationProvided(val inAppMessage : InAppMessage) : HomeScreenEvent()

    data class OnPromptLoaded(val promptId: PromptManager.PromptItem) : HomeScreenEvent()

    data class OnPushNotificationOpened(val campaignId : String) : HomeScreenEvent()

    data class OnShowBuyAndSell(val showBuyAndSell: Boolean) : HomeScreenEvent()
}