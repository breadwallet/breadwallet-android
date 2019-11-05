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

import android.app.AlertDialog
import android.os.Bundle
import android.support.v7.widget.LinearLayoutManager
import android.view.View
import android.widget.EditText
import android.widget.TextView
import androidx.core.os.bundleOf
import androidx.core.view.isVisible
import com.breadwallet.R
import com.breadwallet.mobius.CompositeEffectHandler
import com.breadwallet.mobius.nestedConnectable
import com.breadwallet.ui.BaseMobiusController
import com.breadwallet.ui.navigation.NavigationEffect
import com.breadwallet.ui.navigation.NavigationEffectHandler
import com.breadwallet.ui.navigation.OnCompleteAction
import com.breadwallet.ui.navigation.RouterNavigationEffectHandler
import com.breadwallet.ui.view
import com.spotify.mobius.Connectable
import com.spotify.mobius.functions.Consumer
import kotlinx.android.synthetic.main.activity_settings.*
import org.kodein.di.direct
import org.kodein.di.erased.instance

class SettingsController(args: Bundle? = null) :
    BaseMobiusController<SettingsModel, SettingsEvent, SettingsEffect>(args) {

    companion object {
        private const val EXT_SECTION = "section"
    }

    constructor(section: SettingsSection) : this(
        bundleOf(
            EXT_SECTION to section.name
        )
    )

    private val section: SettingsSection = SettingsSection.valueOf(arg(EXT_SECTION))
    override val layoutId = R.layout.activity_settings
    override val defaultModel = SettingsModel.createDefault(section)
    override val update = SettingsUpdate
    override val effectHandler = CompositeEffectHandler.from<SettingsEffect, SettingsEvent>(
        Connectable { output ->
            SettingsEffectHandler(
                output,
                direct.instance(),
                direct.instance(),
                ::showApiServerDialog,
                ::showPlatformDebugUrlDialog,
                ::showPlatformBundleDialog,
                ::showTokenBundleDialog
            )
        },
        nestedConnectable({ direct.instance<NavigationEffectHandler>() }, { effect ->
            when (effect) {
                SettingsEffect.GoToSupport -> NavigationEffect.GoToFaq("")
                SettingsEffect.GoToQrScan -> NavigationEffect.GoToQrScan
                SettingsEffect.GoToBrdRewards -> NavigationEffect.GoToBrdRewards
                SettingsEffect.GoToGooglePlay -> NavigationEffect.GoToGooglePlay
                SettingsEffect.GoToAbout -> NavigationEffect.GoToAbout
                SettingsEffect.GoToDisplayCurrency -> NavigationEffect.GoToDisplayCurrency
                SettingsEffect.GoToNotificationsSettings -> NavigationEffect.GoToNotificationsSettings
                SettingsEffect.GoToShareData -> NavigationEffect.GoToShareData
                SettingsEffect.GoToImportWallet -> NavigationEffect.GoToImportWallet
                SettingsEffect.GoToSyncBlockchain -> NavigationEffect.GoToSyncBlockchain
                SettingsEffect.GoToNodeSelector -> NavigationEffect.GoToBitcoinNodeSelector
                else -> null
            }
        }),
        nestedConnectable({ direct.instance<RouterNavigationEffectHandler>() }, { effect ->
            when (effect) {
                is SettingsEffect.GoToSection -> NavigationEffect.GoToMenu(effect.section)
                SettingsEffect.GoBack -> NavigationEffect.GoBack
                SettingsEffect.GoToPaperKey -> NavigationEffect.GoToWriteDownKey(OnCompleteAction.GO_HOME)
                SettingsEffect.GoToUpdatePin -> NavigationEffect.GoToSetPin()
                SettingsEffect.GoToOnboarding -> NavigationEffect.GoToOnboarding
                SettingsEffect.GoToFingerprintAuth -> NavigationEffect.GoToFingerprintAuth
                SettingsEffect.GoToWipeWallet -> NavigationEffect.GoToWipeWallet
                SettingsEffect.GoToEnableSegWit -> NavigationEffect.GoToEnableSegWit
                SettingsEffect.GoToLegacyAddress -> NavigationEffect.GoToLegacyAddress
                else -> null
            }
        })
    )

    override val init = SettingsInit

    override fun onCreateView(view: View) {
        super.onCreateView(view)
        settings_list.layoutManager = LinearLayoutManager(activity!!)
    }

    override fun bindView(output: Consumer<SettingsEvent>) = output.view {
        close_button.onClick(SettingsEvent.OnCloseClicked)
        back_button.onClick(SettingsEvent.OnBackClicked)
    }

    override fun SettingsModel.render() {
        val act = activity!!
        ifChanged(SettingsModel::section) {
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
        ifChanged(SettingsModel::items) {
            val adapter = SettingsAdapter(items) { option ->
                eventConsumer.accept(SettingsEvent.OnOptionClicked(option))
            }
            settings_list.adapter = adapter
        }
    }

    /** Developer options dialogs */

    private fun showApiServerDialog(host: String) {
        showInputTextDialog("API Server:", host) { newHost ->
            eventConsumer.accept(SettingsEvent.SetApiServer(newHost))
        }
    }

    private fun showPlatformDebugUrlDialog(url: String) {
        showInputTextDialog("Platform debug url:", url) { newUrl ->
            eventConsumer.accept(SettingsEvent.SetPlatformDebugUrl(newUrl))
        }
    }

    private fun showPlatformBundleDialog(platformBundle: String) {
        showInputTextDialog("Platform Bundle:", platformBundle) { newBundle ->
            eventConsumer.accept(SettingsEvent.SetPlatformBundle(newBundle))
        }
    }

    private fun showTokenBundleDialog(tokenBundle: String) {
        showInputTextDialog("Token Bundle:", tokenBundle) { newBundle ->
            eventConsumer.accept(SettingsEvent.SetTokenBundle(newBundle))
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