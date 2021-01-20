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
package com.breadwallet.ui.navigation

import com.breadwallet.model.InAppMessage
import com.breadwallet.tools.util.Link
import com.breadwallet.ui.auth.AuthMode
import com.breadwallet.ui.settings.SettingsSection
import dev.zacsweers.redacted.annotations.Redacted
import java.math.BigDecimal

sealed class NavigationTarget : INavigationTarget {
    data class SendSheet(
        val currencyId: String,
        val cryptoRequestUrl: Link.CryptoRequestUrl? = null
    ) : NavigationTarget()

    data class ReceiveSheet(val currencyCode: String) : NavigationTarget()
    data class ViewTransaction(
        val currencyId: String,
        val txHash: String
    ) : NavigationTarget()

    object Back : NavigationTarget()
    object BrdRewards : NavigationTarget()
    object ReviewBrd : NavigationTarget()
    object QRScanner : NavigationTarget()
    object LogcatViewer : NavigationTarget()
    object MetadataViewer : NavigationTarget()

    data class DeepLink(
        val url: String? = null,
        val authenticated: Boolean,
        val link: Link? = null
    ) : NavigationTarget()

    data class GoToInAppMessage(val inAppMessage: InAppMessage) : NavigationTarget()
    data class Wallet(val currencyCode: String) : NavigationTarget()
    data class SupportPage(
        val articleId: String,
        val currencyCode: String? = null
    ) : NavigationTarget()

    data class SetPin(
        val onboarding: Boolean = false,
        val skipWriteDownKey: Boolean = false,
        val onComplete: OnCompleteAction = OnCompleteAction.GO_HOME
    ) : NavigationTarget()

    data class AlertDialog(
        val dialogId: String = "",
        val title: String? = null,
        val message: String? = null,
        val titleResId: Int? = null,
        val messageResId: Int? = null,
        val messageArgs: List<Any> = emptyList(),
        val positiveButtonResId: Int? = null,
        val negativeButtonResId: Int? = null,
        val textInputPlaceholder: String? = null,
        val textInputPlaceholderResId: Int? = null
    ) : NavigationTarget()

    object BrdLogin : NavigationTarget()
    data class Authentication(
        val mode: AuthMode = AuthMode.PIN_REQUIRED,
        val titleResId: Int? = null,
        val messageResId: Int? = null
    ) : NavigationTarget()

    object Home : NavigationTarget()
    object Buy : NavigationTarget()
    object Trade : NavigationTarget()
    object AddWallet : NavigationTarget()
    object DisabledScreen : NavigationTarget()
    object NativeApiExplorer : NavigationTarget()

    data class WriteDownKey(
        val onComplete: OnCompleteAction,
        val requestAuth: Boolean = true
    ) : NavigationTarget()

    data class PaperKey(
        @Redacted val phrase: List<String>,
        val onComplete: OnCompleteAction?
    ) : NavigationTarget()

    data class PaperKeyProve(
        @Redacted val phrase: List<String>,
        val onComplete: OnCompleteAction
    ) : NavigationTarget()

    data class Menu(val settingsOption: SettingsSection) : NavigationTarget()

    object TransactionComplete : NavigationTarget()
    object About : NavigationTarget()
    object DisplayCurrency : NavigationTarget()
    object NotificationsSettings : NavigationTarget()
    object ShareDataSettings : NavigationTarget()
    object FingerprintSettings : NavigationTarget()
    object WipeWallet : NavigationTarget()
    object OnBoarding : NavigationTarget()
    data class ImportWallet(
        val privateKey: String? = null,
        val isPasswordProtected: Boolean = false,
        val reclaimingGift: String? = null,
        val scanned: Boolean = false,
        val gift: Boolean = false,
    ) : NavigationTarget()
    object BitcoinNodeSelector : NavigationTarget()
    object EnableSegWit : NavigationTarget()
    object LegacyAddress : NavigationTarget()
    data class SyncBlockchain(
        val currencyCode: String
    ) : NavigationTarget()

    data class FastSync(
        val currencyCode: String
    ) : NavigationTarget()

    data class ATMMap(
        val url: String,
        val mapJson: String
    ) : NavigationTarget()

    data class Signal(
        val titleResId: Int,
        val messageResId: Int,
        val iconResId: Int
    ) : NavigationTarget()

    data class Staking(
        val currencyId: String
    ) : NavigationTarget()

    data class CreateGift(
        val currencyId: String
    ) : NavigationTarget()

    data class ShareGift(
        @Redacted val giftUrl: String,
        @Redacted val txHash: String,
        @Redacted val recipientName: String,
        val giftAmount: BigDecimal,
        val giftAmountFiat: BigDecimal,
        val pricePerUnit: BigDecimal,
        val replaceTop: Boolean = false
    ) : NavigationTarget()
}
