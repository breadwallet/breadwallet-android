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
import com.breadwallet.ui.ViewEffect
import com.breadwallet.ui.navigation.NavigationEffect
import com.breadwallet.ui.navigation.NavigationTarget
import com.breadwallet.util.CurrencyCode
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
        object ShowHiddenOptions : E()
        object OnCloseHiddenMenu : E()

        data class OnATMMapClicked(val url: String, val mapJson: String) : E()
    }

    sealed class F {
        object SendAtmFinderRequest : F()
        object SendLogs : F()
        object ShowApiServerDialog : F(), ViewEffect
        object ShowPlatformDebugUrlDialog : F(), ViewEffect
        object ShowPlatformBundleDialog : F(), ViewEffect
        object ShowTokenBundleDialog : F(), ViewEffect
        object ResetDefaultCurrencies : F()
        object WipeNoPrompt : F()
        object GetPaperKey : F()
        object EnableAllWallets : F()
        object ClearBlockchainData : F()
        object ToggleRateAppPrompt : F()
        object RefreshTokens : F()

        data class SetApiServer(val host: String) : F()
        data class SetPlatformDebugUrl(val url: String) : F()
        data class SetPlatformBundle(val bundle: String) : F()
        data class SetTokenBundle(val bundle: String) : F()
        data class LoadOptions(val section: SettingsSection) : F()

        data class GoToSection(val section: SettingsSection) : F(), NavigationEffect {
            override val navigationTarget = NavigationTarget.Menu(section)
        }

        object GoBack : F(), NavigationEffect {
            override val navigationTarget = NavigationTarget.Back
        }

        object GoToSupport : F(), NavigationEffect {
            override val navigationTarget = NavigationTarget.SupportPage("")
        }

        object GoToQrScan : F(), NavigationEffect {
            override val navigationTarget = NavigationTarget.QRScanner
        }

        object GoToBrdRewards : F(), NavigationEffect {
            override val navigationTarget = NavigationTarget.BrdRewards
        }

        object GoToGooglePlay : F(), NavigationEffect {
            override val navigationTarget = NavigationTarget.ReviewBrd
        }

        object GoToAbout : F(), NavigationEffect {
            override val navigationTarget = NavigationTarget.About
        }

        object GoToDisplayCurrency : F(), NavigationEffect {
            override val navigationTarget = NavigationTarget.DisplayCurrency
        }

        object GoToNotificationsSettings : F(), NavigationEffect {
            override val navigationTarget = NavigationTarget.NotificationsSettings
        }

        object GoToShareData : F(), NavigationEffect {
            override val navigationTarget = NavigationTarget.ShareDataSettings
        }

        object GoToImportWallet : F(), NavigationEffect {
            override val navigationTarget = NavigationTarget.ImportWallet
        }

        data class GoToSyncBlockchain(
            val currencyCode: CurrencyCode
        ) : F(), NavigationEffect {
            override val navigationTarget = NavigationTarget.SyncBlockchain(currencyCode)
        }

        object GoToNodeSelector : F(), NavigationEffect {
            override val navigationTarget = NavigationTarget.BitcoinNodeSelector
        }

        object GoToEnableSegWit : F(), NavigationEffect {
            override val navigationTarget = NavigationTarget.EnableSegWit
        }

        object GoToLegacyAddress : F(), NavigationEffect {
            override val navigationTarget = NavigationTarget.LegacyAddress
        }

        object GoToFingerprintAuth : F(), NavigationEffect {
            override val navigationTarget = NavigationTarget.FingerprintSettings
        }

        object GoToUpdatePin : F(), NavigationEffect {
            override val navigationTarget = NavigationTarget.SetPin()
        }

        object GoToWipeWallet : F(), NavigationEffect {
            override val navigationTarget = NavigationTarget.WipeWallet
        }

        object GoToOnboarding : F(), NavigationEffect {
            override val navigationTarget = NavigationTarget.OnBoarding
        }

        object GoToNativeApiExplorer : F(), NavigationEffect {
            override val navigationTarget = NavigationTarget.NativeApiExplorer
        }

        object GoToHomeScreen : F(), NavigationEffect {
            override val navigationTarget = NavigationTarget.Home
        }

        object GoToAuthentication : F(), NavigationEffect {
            override val navigationTarget = NavigationTarget.Authentication()
        }

        data class GoToPaperKey(
            @Redacted val phrase: List<String>
        ) : F(), NavigationEffect {
            override val navigationTarget =
                NavigationTarget.PaperKey(phrase, null)
        }

        data class GoToFastSync(
            val currencyCode: CurrencyCode
        ) : F(), NavigationEffect {
            override val navigationTarget = NavigationTarget.FastSync(currencyCode)
        }

        data class GoToLink(val link: Link) : F(), NavigationEffect {
            override val navigationTarget = NavigationTarget.DeepLink(
                link = link,
                authenticated = true
            )
        }

        data class GoToATMMap(val url: String, val mapJson: String) : F(), NavigationEffect {
            override val navigationTarget = NavigationTarget.ATMMap(url, mapJson)
        }

        object RelaunchHomeScreen : F(), NavigationEffect {
            override val navigationTarget = NavigationTarget.Home
        }
    }
}
