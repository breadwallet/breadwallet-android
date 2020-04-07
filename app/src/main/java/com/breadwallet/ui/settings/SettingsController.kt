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

import android.app.AlertDialog
import android.os.Bundle
import android.view.View
import android.widget.EditText
import android.widget.TextView
import androidx.core.os.bundleOf
import androidx.core.view.isVisible
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import com.bluelinelabs.conductor.changehandler.HorizontalChangeHandler
import com.breadwallet.R
import com.breadwallet.mobius.CompositeEffectHandler
import com.breadwallet.mobius.nestedConnectable
import com.breadwallet.tools.util.Link
import com.breadwallet.ui.BaseMobiusController
import com.breadwallet.ui.auth.AuthenticationController
import com.breadwallet.ui.navigation.NavigationEffect
import com.breadwallet.ui.navigation.RouterNavigationEffectHandler
import com.breadwallet.ui.scanner.ScannerController
import com.breadwallet.ui.settings.SettingsScreen.E
import com.breadwallet.ui.settings.SettingsScreen.F
import com.breadwallet.ui.settings.SettingsScreen.M
import com.breadwallet.ui.view
import com.spotify.mobius.Connectable
import com.spotify.mobius.functions.Consumer
import kotlinx.android.synthetic.main.controller_settings.*
import org.kodein.di.direct
import org.kodein.di.erased.instance

class SettingsController(
    args: Bundle? = null
) : BaseMobiusController<M, E, F>(args),
    ScannerController.Listener,
    AuthenticationController.Listener {

    companion object {
        private const val EXT_SECTION = "section"
    }

    constructor(section: SettingsSection) : this(
        bundleOf(
            EXT_SECTION to section.name
        )
    )

    private val section: SettingsSection = SettingsSection.valueOf(arg(EXT_SECTION))

    init {
        if (section != SettingsSection.HOME) {
            overridePopHandler(HorizontalChangeHandler())
            overridePushHandler(HorizontalChangeHandler())
        }
    }

    override val layoutId = R.layout.controller_settings
    override val defaultModel = M.createDefault(section)
    override val update = SettingsUpdate
    override val effectHandler = CompositeEffectHandler.from<F, E>(
        Connectable { output ->
            SettingsScreenHandler(
                eventConsumer,
                viewCreatedScope,
                direct.instance(),
                checkNotNull(activity),
                direct.instance(),
                ::showApiServerDialog,
                ::showPlatformDebugUrlDialog,
                ::showPlatformBundleDialog,
                ::showTokenBundleDialog,
                direct.instance(),
                direct.instance()
            )
        },
        nestedConnectable({ direct.instance<RouterNavigationEffectHandler>() }, { effect ->
            when (effect) {
                F.GoToGooglePlay -> NavigationEffect.GoToGooglePlay
                F.GoToNotificationsSettings -> NavigationEffect.GoToNotificationsSettings
                F.GoToShareData -> NavigationEffect.GoToShareData
                F.GoToAbout -> NavigationEffect.GoToAbout
                F.GoToBrdRewards -> NavigationEffect.GoToBrdRewards
                F.GoToQrScan -> NavigationEffect.GoToQrScan
                F.GoToSupport -> NavigationEffect.GoToFaq("")
                is F.GoToSection -> NavigationEffect.GoToMenu(effect.section)
                F.GoBack -> NavigationEffect.GoBack
                is F.GoToPaperKey -> NavigationEffect.GoToPaperKey(effect.phrase, null)
                F.GoToAuthentication -> NavigationEffect.GoToAuthentication
                F.GoToUpdatePin -> NavigationEffect.GoToSetPin()
                F.GoToOnboarding -> NavigationEffect.GoToOnboarding
                F.GoToFingerprintAuth -> NavigationEffect.GoToFingerprintAuth
                F.GoToWipeWallet -> NavigationEffect.GoToWipeWallet
                F.GoToEnableSegWit -> NavigationEffect.GoToEnableSegWit
                F.GoToLegacyAddress -> NavigationEffect.GoToLegacyAddress
                F.GoToImportWallet -> NavigationEffect.GoToImportWallet
                F.GoToNodeSelector -> NavigationEffect.GoToBitcoinNodeSelector
                is F.GoToFastSync -> NavigationEffect.GoToFastSync(effect.currencyCode)
                F.GoToDisplayCurrency -> NavigationEffect.GoToDisplayCurrency
                F.GoToNativeApiExplorer -> NavigationEffect.GoToNativeApiExplorer
                is F.GoToSyncBlockchain -> NavigationEffect.GoToSyncBlockchain(effect.currencyCode)
                F.GoToHomeScreen -> NavigationEffect.GoToHome
                is F.GoToLink -> NavigationEffect.GoToDeepLink(link = effect.link, authenticated = true)
                else -> null
            }
        })
    )

    override val init = SettingsInit

    override fun onCreateView(view: View) {
        super.onCreateView(view)
        settings_list.layoutManager = LinearLayoutManager(activity!!)
        settings_list.addItemDecoration(
            DividerItemDecoration(
                activity!!,
                DividerItemDecoration.VERTICAL
            )
        )
    }

    override fun bindView(output: Consumer<E>) = output.view {
        close_button.onClick(E.OnCloseClicked)
        back_button.onClick(E.OnBackClicked)
    }

    override fun M.render() {
        val act = activity!!
        ifChanged(M::section) {
            title.text = when (section) {
                SettingsSection.HOME -> act.getString(R.string.Settings_title)
                SettingsSection.PREFERENCES -> act.getString(R.string.Settings_preferences)
                SettingsSection.DEVELOPER_OPTION -> "Developer Options"
                SettingsSection.SECURITY -> act.getString(R.string.MenuButton_security)
                SettingsSection.BTC_SETTINGS -> "Bitcoin ${act.getString(R.string.Settings_title)}"
                SettingsSection.BCH_SETTINGS -> "Bitcoin Cash ${act.getString(R.string.Settings_title)}"
            }
            val isHome = section == SettingsSection.HOME
            close_button.isVisible = isHome
            back_button.isVisible = !isHome
        }
        ifChanged(M::items) {
            val adapter = SettingsAdapter(items) { option ->
                eventConsumer.accept(E.OnOptionClicked(option))
            }
            settings_list.adapter = adapter
        }
    }

    override fun onLinkScanned(link: Link) {
        eventConsumer.accept(E.OnLinkScanned(link))
    }

    override fun onAuthenticationSuccess() {
        eventConsumer.accept(E.OnAuthenticated)
    }

    /** Developer options dialogs */

    private fun showApiServerDialog(host: String) {
        showInputTextDialog("API Server:", host) { newHost ->
            eventConsumer.accept(E.SetApiServer(newHost))
        }
    }

    private fun showPlatformDebugUrlDialog(url: String) {
        showInputTextDialog("Platform debug url:", url) { newUrl ->
            eventConsumer.accept(E.SetPlatformDebugUrl(newUrl))
        }
    }

    private fun showPlatformBundleDialog(platformBundle: String) {
        showInputTextDialog("Platform Bundle:", platformBundle) { newBundle ->
            eventConsumer.accept(E.SetPlatformBundle(newBundle))
        }
    }

    private fun showTokenBundleDialog(tokenBundle: String) {
        showInputTextDialog("Token Bundle:", tokenBundle) { newBundle ->
            eventConsumer.accept(E.SetTokenBundle(newBundle))
        }
    }

    private fun showInputTextDialog(
        message: String,
        currentValue: String,
        onConfirmation: (String) -> Unit
    ) {
        val act = checkNotNull(activity)
        val editText = EditText(act)
        editText.setText(currentValue, TextView.BufferType.EDITABLE)
        AlertDialog.Builder(act)
            .setMessage(message)
            .setView(editText)
            .setPositiveButton(R.string.Button_confirm) { _, _ ->
                val platformURL = editText.text.toString()
                onConfirmation(platformURL)
            }
            .setNegativeButton(R.string.Button_cancel, null)
            .create()
            .show()
    }
}
