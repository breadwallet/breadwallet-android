/**
 * BreadWallet
 *
 * Created by Ahsan Butt <ahsan.butt@breadwallet.com> on 8/1/19.
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
package com.breadwallet.ui.home

import com.breadwallet.model.InAppMessage
import com.breadwallet.model.PriceChange
import com.breadwallet.ui.navigation.NavEffectHolder
import com.breadwallet.ui.navigation.NavigationEffect
import com.breadwallet.ui.navigation.OnCompleteAction
import com.breadwallet.ui.settings.SettingsSection
import io.sweers.redacted.annotation.Redacted
import java.math.BigDecimal

object HomeScreen {
    data class M(
        val wallets: Map<String, Wallet> = emptyMap(),
        val displayOrder: List<String> = emptyList(),
        val promptId: PromptItem? = null,
        val hasInternet: Boolean = true,
        val isBuyBellNeeded: Boolean = false,
        val showBuyAndSell: Boolean = false
    ) {

        companion object {
            fun createDefault() = M()
        }

        val aggregatedFiatBalance: BigDecimal = wallets.values
            .fold(BigDecimal.ZERO) { acc, next ->
                acc.add(next.fiatBalance)
            }

        val showPrompt: Boolean = promptId != null
    }

    sealed class E {

        data class OnWalletSyncProgressUpdated(
            val currencyCode: String,
            val progress: Float,
            val syncThroughMillis: Long,
            val isSyncing: Boolean
        ) : E() {
            init {
                require(progress in 0f..1f) {
                    "Sync progress must be in 0..1 but was $progress"
                }
            }
        }

        data class OnEnabledWalletsUpdated(@Redacted val wallets: List<Wallet>) : E()

        data class OnWalletsUpdated(@Redacted val wallets: List<Wallet>) : E()
        data class OnWalletBalanceUpdated(
            val currencyCode: String,
            val balance: BigDecimal,
            val fiatBalance: BigDecimal
        ) : E()

        data class OnUnitPriceChanged(
            val currencyCode: String,
            val fiatPricePerUnit: BigDecimal,
            val priceChange: PriceChange? = null
        ) : E()

        data class OnWalletDisplayOrderUpdated(@Redacted val displayOrder: List<String>) : E()

        data class OnBuyBellNeededLoaded(val isBuyBellNeeded: Boolean) : E()

        data class OnConnectionUpdated(val isConnected: Boolean) : E()

        data class OnWalletClicked(val currencyCode: String) : E()

        object OnAddWalletsClicked : E()

        object OnBuyClicked : E()
        object OnTradeClicked : E()
        object OnMenuClicked : E()

        data class OnDeepLinkProvided(val url: String) : E()
        data class OnInAppNotificationProvided(val inAppMessage: InAppMessage) : E()

        data class OnPromptLoaded(val promptId: PromptItem?) : E()

        data class OnPushNotificationOpened(val campaignId: String) : E()

        data class OnShowBuyAndSell(val showBuyAndSell: Boolean) : E()

        data class OnPromptDismissed(val promptId: PromptItem) : E()
        object OnFingerprintPromptClicked : E()
        object OnPaperKeyPromptClicked : E()
        object OnUpgradePinPromptClicked : E()
        object OnRescanPromptClicked : E()
        data class OnEmailPromptClicked(@Redacted val email: String) : E()
    }

    sealed class F {

        object LoadWallets : F()
        object LoadIsBuyBellNeeded : F()
        object LoadPrompt : F()
        object CheckInAppNotification : F()
        object CheckIfShowBuyAndSell : F()

        data class GoToDeepLink(val url: String) : F(), NavEffectHolder {
            override val navigationEffect = NavigationEffect.GoToDeepLink(url, true)
        }
        data class GoToInappMessage(val inAppMessage: InAppMessage) : F(), NavEffectHolder {
            override val navigationEffect = NavigationEffect.GoToInAppMessage(inAppMessage)
        }
        data class GoToWallet(val currencyCode: String) : F(), NavEffectHolder {
            override val navigationEffect = NavigationEffect.GoToWallet(currencyCode)
        }
        object GoToAddWallet : F(), NavEffectHolder {
            override val navigationEffect = NavigationEffect.GoToAddWallet
        }

        object GoToBuy : F(), NavEffectHolder {
            override val navigationEffect = NavigationEffect.GoToBuy
        }
        object GoToTrade : F(), NavEffectHolder {
            override val navigationEffect = NavigationEffect.GoToTrade
        }
        object GoToMenu : F(), NavEffectHolder {
            override val navigationEffect = NavigationEffect.GoToMenu(SettingsSection.HOME)
        }
        object GoToFingerprintSettings : F(), NavEffectHolder {
            override val navigationEffect = NavigationEffect.GoToFingerprintAuth
        }
        object GoToWriteDownKey : F(), NavEffectHolder {
            override val navigationEffect = NavigationEffect.GoToWriteDownKey(
                OnCompleteAction.GO_HOME
            )
        }
        object GoToUpgradePin : F(), NavEffectHolder {
            override val navigationEffect = NavigationEffect.GoToSetPin()
        }

        data class RecordPushNotificationOpened(val campaignId: String) : F()

        data class UpdateWalletOrder(
            val orderedCurrencyIds: List<String>
        ) : F()

        data class TrackEvent(
            val eventName: String,
            val attributes: Map<String, String>? = null
        ) : F()

        data class DismissPrompt(val promptItem: PromptItem) : F()

        object StartRescan : F()

        data class SaveEmail(@Redacted val email: String) : F()
    }
}

data class Wallet(
    val currencyId: String,
    val currencyName: String,
    val currencyCode: String,
    val fiatPricePerUnit: BigDecimal = BigDecimal.ZERO,
    val balance: BigDecimal = BigDecimal.ZERO,
    val fiatBalance: BigDecimal = BigDecimal.ZERO,
    val syncProgress: Float = 0f,
    val syncingThroughMillis: Long = 0L,
    val isSyncing: Boolean = false,
    val priceChange: PriceChange? = null,
    val state: State = State.READY
) {
    enum class State {
        READY, LOADING, UNINITIALIZED
    }

    val hasSyncTime: Boolean = syncingThroughMillis != 0L

    val hasPricePerUnit: Boolean = fiatPricePerUnit != BigDecimal.ZERO
}

enum class PromptItem {
    EMAIL_COLLECTION,
    FINGER_PRINT,
    PAPER_KEY,
    UPGRADE_PIN,
    RECOMMEND_RESCAN
}
