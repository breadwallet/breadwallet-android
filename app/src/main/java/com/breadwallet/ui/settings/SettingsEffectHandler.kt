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

import android.content.Context
import com.breadwallet.BuildConfig
import com.breadwallet.R
import com.breadwallet.app.BreadApp
import com.breadwallet.model.Experiments
import com.breadwallet.repository.ExperimentsRepository
import com.breadwallet.repository.ExperimentsRepositoryImpl
import com.breadwallet.tools.manager.BRSharedPrefs
import com.breadwallet.tools.security.AuthManager
import com.breadwallet.tools.util.LogsUtils
import com.breadwallet.tools.util.ServerBundlesHelper
import com.breadwallet.ui.navigation.NavigationEffectHandler
import com.platform.APIClient
import com.platform.HTTPServer
import com.platform.buildSignedRequest
import com.platform.middlewares.plugins.LinkPlugin
import com.spotify.mobius.Connection
import com.spotify.mobius.functions.Consumer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

class SettingsEffectHandler(
    private val output: Consumer<SettingsEvent>,
    private val context: Context,
    private val experimentsRepository: ExperimentsRepository,
    private val showApiServerDialog: (String) -> Unit,
    private val showPlatformDebugUrlDialog: (String) -> Unit,
    private val showPlatformBundleDialog: (String) -> Unit,
    private val showTokenBundleDialog: (String) -> Unit
) : Connection<SettingsEffect>, CoroutineScope {

    companion object {
        private const val DEVELOPER_OPTIONS_TITLE = "Developer Options"
    }

    override val coroutineContext = SupervisorJob() + Dispatchers.Default

    override fun accept(value: SettingsEffect) {
        when (value) {
            is SettingsEffect.LoadOptions -> loadOptions(value.section)
            SettingsEffect.SendAtmFinderRequest -> sendAtmFinderRequest()
            SettingsEffect.SendLogs -> LogsUtils.shareLogs(context)
            SettingsEffect.ShowApiServerDialog -> launch(Dispatchers.Main) {
                showApiServerDialog(
                    BreadApp.host
                )
            }
            SettingsEffect.ShowPlatformDebugUrlDialog -> launch(Dispatchers.Main) {
                showPlatformDebugUrlDialog(
                    ServerBundlesHelper.getWebPlatformDebugURL(context)
                )
            }
            SettingsEffect.ShowPlatformBundleDialog -> launch(Dispatchers.Main) {
                showPlatformBundleDialog(
                    ServerBundlesHelper.getBundle(
                        context,
                        ServerBundlesHelper.Type.WEB
                    )
                )
            }
            SettingsEffect.ShowTokenBundleDialog -> launch(Dispatchers.Main) {
                showTokenBundleDialog(
                    ServerBundlesHelper.getBundle(
                        context,
                        ServerBundlesHelper.Type.TOKEN
                    )
                )
            }
            is SettingsEffect.SetApiServer -> {
                BreadApp.setDebugHost(value.host)
                loadOptions(SettingsSection.DEVELOPER_OPTION)
            }
            is SettingsEffect.SetPlatformDebugUrl -> {
                ServerBundlesHelper.setWebPlatformDebugURL(context, value.url)
                loadOptions(SettingsSection.DEVELOPER_OPTION)
            }
            is SettingsEffect.SetPlatformBundle -> {
                ServerBundlesHelper.setDebugBundle(
                    context,
                    ServerBundlesHelper.Type.WEB,
                    value.bundle
                )
                loadOptions(SettingsSection.DEVELOPER_OPTION)
            }
            is SettingsEffect.SetTokenBundle -> {
                ServerBundlesHelper.setDebugBundle(
                    context,
                    ServerBundlesHelper.Type.TOKEN,
                    value.bundle
                )
                loadOptions(SettingsSection.DEVELOPER_OPTION)
            }
        }
    }

    override fun dispose() {
        coroutineContext.cancel()
    }

    private fun loadOptions(section: SettingsSection) {
        val items: List<SettingsItem> = when (section) {
            SettingsSection.HOME -> getHomeOptions()
            SettingsSection.PREFERENCES -> preferences
            SettingsSection.SECURITY -> securitySettings
            SettingsSection.DEVELOPER_OPTION -> getDeveloperOptions()
            SettingsSection.BTC_SETTINGS -> {
                BRSharedPrefs.putCurrentWalletCurrencyCode(
                    context,
                    NavigationEffectHandler.BITCOIN_CURRENCY_CODE
                )
                btcOptions
            }
            SettingsSection.BCH_SETTINGS -> {
                BRSharedPrefs.putCurrentWalletCurrencyCode(
                    context,
                    NavigationEffectHandler.BITCOIN_CASH_CURRENCY_CODE
                )
                bchOptions
            }
        }
        output.accept(SettingsEvent.OnOptionsLoaded(items))
    }

    private fun getHomeOptions(): List<SettingsItem> {
        return mutableListOf(
            SettingsItem(
                context.getString(R.string.MenuButton_scan),
                SettingsOption.SCAN_QR,
                R.drawable.ic_camera
            ),
            SettingsItem(
                context.getString(R.string.Settings_preferences),
                SettingsOption.PREFERENCES,
                R.drawable.ic_preferences
            ),
            SettingsItem(
                context.getString(R.string.MenuButton_security),
                SettingsOption.SECURITY_SETTINGS,
                R.drawable.ic_security_settings
            ),
            SettingsItem(
                context.getString(R.string.MenuButton_support),
                SettingsOption.SUPPORT,
                R.drawable.ic_support
            ),
            SettingsItem(
                context.getString(R.string.Settings_review),
                SettingsOption.SUBMIT_REVIEW,
                R.drawable.ic_review
            ),
            SettingsItem(
                context.getString(R.string.Settings_rewards),
                SettingsOption.REWARDS,
                R.drawable.ic_reward
            ),
            SettingsItem(
                context.getString(R.string.Settings_about),
                SettingsOption.ABOUT,
                R.drawable.ic_about
            )
        ).apply {
            if (experimentsRepository.isExperimentActive(Experiments.ATM_MAP)) {
                add(
                    SettingsItem(
                        context.getString(R.string.Settings_atmMapMenuItemTitle),
                        SettingsOption.ATM_FINDER,
                        R.drawable.ic_atm_finder
                    )
                )
            }
            if (BuildConfig.DEBUG) {
                add(
                    SettingsItem(
                        DEVELOPER_OPTIONS_TITLE,
                        SettingsOption.DEVELOPER_OPTIONS
                    )
                )
            }
        }
    }

    private val preferences = listOf(
        SettingsItem(
            context.getString(R.string.Settings_currency),
            SettingsOption.CURRENCY,
            addOn = BRSharedPrefs.getPreferredFiatIso()
        ),
        SettingsItem(
            "Bitcoin ${context.getString(R.string.Settings_title)}", // TODO move Bitcoin to a constant
            SettingsOption.BTC_MENU
        ),
        SettingsItem(
            "Bitcoin Cash ${context.getString(R.string.Settings_title)}", // TODO move Bitcoin Cash to a constant
            SettingsOption.BCH_MENU
        ),
        SettingsItem(
            context.getString(R.string.Prompts_ShareData_title),
            SettingsOption.SHARE_ANONYMOUS_DATA
        ),
        SettingsItem(
            context.getString(R.string.Settings_notifications),
            SettingsOption.NOTIFICATIONS
        )
    )

    private val securitySettings = listOf(
        SettingsItem(
            context.getString(R.string.TouchIdSettings_switchLabel_android),
            SettingsOption.FINGERPRINT_AUTH
        ),
        SettingsItem(
            context.getString(R.string.UpdatePin_updateTitle),
            SettingsOption.UPDATE_PIN
        ),
        SettingsItem(
            context.getString(R.string.SecurityCenter_paperKeyTitle),
            SettingsOption.PAPER_KEY
        ),
        SettingsItem(
            context.getString(R.string.Settings_wipe),
            SettingsOption.WIPE
        )
    )

    private fun getDeveloperOptions(): List<SettingsItem> {
        val currentWebPlatformDebugURL = ServerBundlesHelper.getWebPlatformDebugURL(context)
        val webPlatformBundleAddOn = if (currentWebPlatformDebugURL.isNotEmpty()) {
            "(not used if debug URL specified)"
        } else {
            ""
        }
        return listOf(
            SettingsItem(
                "Send Logs",
                SettingsOption.SEND_LOGS
            ),
            SettingsItem(
                "API Server",
                SettingsOption.API_SERVER,
                subHeader = BreadApp.host
            ),
            SettingsItem(
                "Onboarding flow",
                SettingsOption.ONBOARDING_FLOW
            ),
            SettingsItem(
                "Web Platform Debug URL",
                SettingsOption.WEB_PLAT_DEBUG_URL,
                subHeader = ServerBundlesHelper.getWebPlatformDebugURL(context)
            ),
            SettingsItem(
                "Web Platform Bundle",
                SettingsOption.WEB_PLAT_BUNDLE,
                subHeader = ServerBundlesHelper.getBundle(context, ServerBundlesHelper.Type.WEB),
                addOn = webPlatformBundleAddOn
            ),
            SettingsItem(
                "Token Bundle",
                SettingsOption.TOKEN_BUNDLE,
                subHeader = ServerBundlesHelper.getBundle(context, ServerBundlesHelper.Type.TOKEN)
            )
        )
    }

    private val btcOptions: List<SettingsItem> =
        mutableListOf(
            SettingsItem(
                context.getString(R.string.Settings_importTitle),
                SettingsOption.REDEEM_PRIVATE_KEY
            ),
            SettingsItem(
                context.getString(R.string.ReScan_header),
                SettingsOption.SYNC_BLOCKCHAIN
            ),
            SettingsItem(
                context.getString(R.string.NodeSelector_title),
                SettingsOption.BTC_NODES
            )
        ).apply {
            if (AuthManager.isFingerPrintAvailableAndSetup(context)) {
                val fingerprintOption = SettingsItem(
                    context.getString(R.string.Settings_ViewLegacyAddress),
                    SettingsOption.FINGERPRINT_SPENDING_LIMIT
                )
                add(0, fingerprintOption)
            }
            val segWitOption = if (BRSharedPrefs.getIsSegwitEnabled()) {
                SettingsItem(
                    context.getString(R.string.Settings_ViewLegacyAddress),
                    SettingsOption.VIEW_LEGACY_ADDRESS
                )
            } else {
                SettingsItem(
                    context.getString(R.string.Settings_EnableSegwit),
                    SettingsOption.ENABLE_SEG_WIT
                )
            }
            add(segWitOption)
        }

    private val bchOptions = listOf(
        SettingsItem(
            context.getString(R.string.Settings_importTitle),
            SettingsOption.REDEEM_PRIVATE_KEY
        ),
        SettingsItem(
            context.getString(R.string.ReScan_header),
            SettingsOption.SYNC_BLOCKCHAIN
        )
    )

    private fun sendAtmFinderRequest() {
        val mapExperiment = ExperimentsRepositoryImpl.experiments[Experiments.ATM_MAP.key]
        val mapPath = mapExperiment?.meta.orEmpty().replace("\\/", "/")
        val url = HTTPServer.getPlatformUrl(LinkPlugin.BROWSER_PATH)
        val request = buildSignedRequest(
            url,
            mapPath,
            "POST",
            LinkPlugin.BROWSER_PATH
        )
        APIClient.getInstance(context).sendRequest(request, false)
    }
}