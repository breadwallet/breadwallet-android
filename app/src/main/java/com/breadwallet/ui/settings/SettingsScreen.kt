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

import com.breadwallet.tools.util.Link
import com.breadwallet.util.CurrencyCode
import drewcarlson.switchboard.MobiusUpdateSpec
import io.sweers.redacted.annotation.Redacted

object SettingsScreen {
    data class M(
        val section: SettingsSection,
        @Redacted val items: List<SettingsItem> = listOf()
    ) {
        companion object {
            fun createDefault(section: SettingsSection) = M(section)
        }
    }

    @MobiusUpdateSpec(
        prefix = "SettingsScreen",
        baseModel = M::class,
        baseEffect = F::class
    )
    sealed class E {

        data class OnLinkScanned(val link: Link) : E()
        data class OnOptionClicked(val option: SettingsOption) : E()

        data class OnOptionsLoaded(@Redacted val options: List<SettingsItem>) : E()

        object OnBackClicked : E()
        object OnCloseClicked : E()
        
        object OnAuthenticated : E()

        data class ShowPhrase(@Redacted val phrase: List<String>) : E()
        data class SetApiServer(val host: String) : E()
        data class SetPlatformDebugUrl(val url: String) : E()
        data class SetPlatformBundle(val bundle: String) : E()
        data class SetTokenBundle(val bundle: String) : E()
        object OnWalletsUpdated : E()
    }

    sealed class F {
        data class LoadOptions(val section: SettingsSection) : F()
        data class GoToSection(val section: SettingsSection) : F()

        object GoBack : F()
        object GoToSupport : F()
        object GoToQrScan : F()
        object GoToBrdRewards : F()
        object GoToGooglePlay : F()
        object GoToAbout : F()
        object SendAtmFinderRequest : F()
        object GoToDisplayCurrency : F()
        object GoToNotificationsSettings : F()
        object GoToShareData : F()
        object GoToImportWallet : F()
        data class GoToSyncBlockchain(
            val currencyCode: CurrencyCode
        ) : F()

        object GoToNodeSelector : F()
        object GoToEnableSegWit : F()
        object GoToLegacyAddress : F()
        object GoToFingerprintAuth : F()
        object GoToUpdatePin : F()
        object GoToWipeWallet : F()
        object GoToOnboarding : F()
        object GoToNativeApiExplorer : F()
        object SendLogs : F()
        object ShowApiServerDialog : F()
        object ShowPlatformDebugUrlDialog : F()
        object ShowPlatformBundleDialog : F()
        object ShowTokenBundleDialog : F()
        object ResetDefaultCurrencies : F()
        object GoToHomeScreen : F()
        object WipeNoPrompt : F()
        object GoToAuthentication : F()
        object GetPaperKey : F()
        data class GoToPaperKey(
            @Redacted val phrase: List<String>
        ) : F()

        data class GoToFastSync(
            val currencyCode: CurrencyCode
        ) : F()

        data class SetApiServer(val host: String) : F()
        data class SetPlatformDebugUrl(val url: String) : F()
        data class SetPlatformBundle(val bundle: String) : F()
        data class SetTokenBundle(val bundle: String) : F()

        data class GoToLink(val link: Link) : F()
    }
}
