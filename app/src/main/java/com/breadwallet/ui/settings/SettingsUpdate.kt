/**
 * BreadWallet
 *
 * Created by Pablo Budelli on <pablo.budelli@breadwallet.com> 10/17/19.
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

import com.spotify.mobius.Next
import com.spotify.mobius.Next.dispatch
import com.spotify.mobius.Next.next
import com.spotify.mobius.Update

object SettingsUpdate : Update<SettingsModel, SettingsEvent, SettingsEffect>, SettingsUpdateSpec {
    override fun update(
        model: SettingsModel,
        event: SettingsEvent
    ): Next<SettingsModel, SettingsEffect> = patch(model, event)

    override fun onOptionsLoaded(
        model: SettingsModel,
        event: SettingsEvent.OnOptionsLoaded
    ): Next<SettingsModel, SettingsEffect> = next(model.copy(items = event.options))

    override fun onBackClicked(model: SettingsModel): Next<SettingsModel, SettingsEffect> =
        dispatch(setOf(SettingsEffect.GoBack))

    override fun onCloseClicked(model: SettingsModel): Next<SettingsModel, SettingsEffect> =
        dispatch(setOf(SettingsEffect.GoBack))

    override fun setApiServer(
        model: SettingsModel,
        event: SettingsEvent.SetApiServer
    ): Next<SettingsModel, SettingsEffect> =
        dispatch(setOf(SettingsEffect.SetApiServer(event.host)))

    override fun setPlatformDebugUrl(
        model: SettingsModel,
        event: SettingsEvent.SetPlatformDebugUrl
    ): Next<SettingsModel, SettingsEffect> =
        dispatch(setOf(SettingsEffect.SetPlatformDebugUrl(event.url)))

    override fun setPlatformBundle(
        model: SettingsModel,
        event: SettingsEvent.SetPlatformBundle
    ): Next<SettingsModel, SettingsEffect> =
        dispatch(setOf(SettingsEffect.SetPlatformBundle(event.bundle)))

    override fun setTokenBundle(
        model: SettingsModel,
        event: SettingsEvent.SetTokenBundle
    ): Next<SettingsModel, SettingsEffect> =
        dispatch(setOf(SettingsEffect.SetTokenBundle(event.bundle)))

    override fun onOptionClicked(
        model: SettingsModel,
        event: SettingsEvent.OnOptionClicked
    ): Next<SettingsModel, SettingsEffect> {
        return dispatch(
            setOf(
                when (event.option) {
                    SettingsOption.SCAN_QR -> SettingsEffect.GoToQrScan
                    SettingsOption.PREFERENCES -> SettingsEffect.GoToSection(SettingsSection.PREFERENCES)
                    SettingsOption.SECURITY_SETTINGS -> SettingsEffect.GoToSection(SettingsSection.SECURITY)
                    SettingsOption.SUPPORT -> SettingsEffect.GoToSupport
                    SettingsOption.SUBMIT_REVIEW -> SettingsEffect.GoToGooglePlay
                    SettingsOption.REWARDS -> SettingsEffect.GoToBrdRewards
                    SettingsOption.ABOUT -> SettingsEffect.GoToAbout
                    SettingsOption.ATM_FINDER -> SettingsEffect.SendAtmFinderRequest
                    SettingsOption.DEVELOPER_OPTIONS -> SettingsEffect.GoToSection(SettingsSection.DEVELOPER_OPTION)
                    SettingsOption.CURRENCY -> SettingsEffect.GoToDisplayCurrency
                    SettingsOption.BTC_MENU -> SettingsEffect.GoToSection(SettingsSection.BTC_SETTINGS)
                    SettingsOption.BCH_MENU -> SettingsEffect.GoToSection(SettingsSection.BCH_SETTINGS)
                    SettingsOption.SHARE_ANONYMOUS_DATA -> SettingsEffect.GoToShareData
                    SettingsOption.NOTIFICATIONS -> SettingsEffect.GoToNotificationsSettings
                    SettingsOption.FINGERPRINT_SPENDING_LIMIT -> SettingsEffect.GoToFingerprintSpendingLimit
                    SettingsOption.REDEEM_PRIVATE_KEY -> SettingsEffect.GoToImportWallet
                    SettingsOption.SYNC_BLOCKCHAIN -> SettingsEffect.GoToSyncBlockchain
                    SettingsOption.ENABLE_SEG_WIT -> SettingsEffect.GoToEnableSegWit
                    SettingsOption.VIEW_LEGACY_ADDRESS -> SettingsEffect.GoToLegacyAddress
                    SettingsOption.BTC_NODES -> SettingsEffect.GoToNodeSelector
                    SettingsOption.FINGERPRINT_AUTH -> SettingsEffect.GoToFingerprintAuth
                    SettingsOption.PAPER_KEY -> SettingsEffect.GoToPaperKey
                    SettingsOption.UPDATE_PIN -> SettingsEffect.GoToUpdatePin
                    SettingsOption.WIPE -> SettingsEffect.GoToWipeWallet
                    SettingsOption.ONBOARDING_FLOW -> SettingsEffect.GoToOnboarding
                    SettingsOption.SEND_LOGS -> SettingsEffect.SendLogs
                    SettingsOption.API_SERVER -> SettingsEffect.ShowApiServerDialog
                    SettingsOption.WEB_PLAT_DEBUG_URL -> SettingsEffect.ShowPlatformDebugUrlDialog
                    SettingsOption.WEB_PLAT_BUNDLE -> SettingsEffect.ShowPlatformBundleDialog
                    SettingsOption.TOKEN_BUNDLE -> SettingsEffect.ShowTokenBundleDialog
                    SettingsOption.FAST_SYNC_BTC -> SettingsEffect.GoToFastSync("btc")
                    SettingsOption.FAST_SYNC_BCH -> SettingsEffect.GoToFastSync("bch")
                }
            )
        )
    }
}
