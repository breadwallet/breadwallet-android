/**
 * BreadWallet
 *
 * Created by Pablo Budelli <pablo.budelli@breadwallet.com> on 10/17/19.
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
package com.breadwallet.ui.settings

import com.breadwallet.util.CurrencyCode

sealed class SettingsEffect {
    data class LoadOptions(val section: SettingsSection) : SettingsEffect()
    data class GoToSection(val section: SettingsSection) : SettingsEffect()

    object GoBack : SettingsEffect()
    object GoToSupport : SettingsEffect()
    object GoToQrScan : SettingsEffect()
    object GoToBrdRewards : SettingsEffect()
    object GoToGooglePlay : SettingsEffect()
    object GoToAbout : SettingsEffect()
    object SendAtmFinderRequest : SettingsEffect()
    object GoToDisplayCurrency : SettingsEffect()
    object GoToNotificationsSettings : SettingsEffect()
    object GoToShareData : SettingsEffect()
    object GoToFingerprintSpendingLimit : SettingsEffect()
    object GoToImportWallet : SettingsEffect()
    object GoToSyncBlockchain : SettingsEffect()
    object GoToNodeSelector : SettingsEffect()
    object GoToEnableSegWit : SettingsEffect()
    object GoToLegacyAddress : SettingsEffect()
    object GoToFingerprintAuth : SettingsEffect()
    object GoToUpdatePin : SettingsEffect()
    object GoToPaperKey : SettingsEffect()
    object GoToWipeWallet : SettingsEffect()
    object GoToOnboarding : SettingsEffect()
    object SendLogs : SettingsEffect()
    object ShowApiServerDialog : SettingsEffect()
    object ShowPlatformDebugUrlDialog : SettingsEffect()
    object ShowPlatformBundleDialog : SettingsEffect()
    object ShowTokenBundleDialog : SettingsEffect()

    data class GoToFastSync(
        val currencyCode: CurrencyCode
    ) : SettingsEffect()

    data class SetApiServer(val host: String) : SettingsEffect()
    data class SetPlatformDebugUrl(val url: String) : SettingsEffect()
    data class SetPlatformBundle(val bundle: String) : SettingsEffect()
    data class SetTokenBundle(val bundle: String) : SettingsEffect()
}
