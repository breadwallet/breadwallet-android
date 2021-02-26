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

import android.app.ActivityManager
import android.content.Context
import android.security.keystore.UserNotAuthenticatedException
import android.widget.Toast
import androidx.core.content.FileProvider
import com.breadwallet.BuildConfig
import com.breadwallet.R
import com.breadwallet.app.BreadApp
import com.breadwallet.breadbox.BdbAuthInterceptor
import com.breadwallet.breadbox.BreadBox
import com.breadwallet.breadbox.feeForToken
import com.breadwallet.breadbox.hashString
import com.breadwallet.breadbox.isNative
import com.breadwallet.breadbox.toSanitizedString
import com.breadwallet.crypto.Transfer
import com.breadwallet.crypto.TransferDirection
import com.breadwallet.crypto.TransferState
import com.breadwallet.crypto.Wallet
import com.breadwallet.crypto.WalletManagerMode
import com.breadwallet.crypto.WalletManagerState
import com.breadwallet.ext.throttleLatest
import com.breadwallet.logger.Logger
import com.breadwallet.logger.logDebug
import com.breadwallet.model.Experiments
import com.breadwallet.model.TokenItem
import com.breadwallet.platform.entities.TxMetaDataEmpty
import com.breadwallet.platform.entities.TxMetaDataValue
import com.breadwallet.repository.ExperimentsRepository
import com.breadwallet.repository.ExperimentsRepositoryImpl
import com.breadwallet.tools.manager.BRClipboardManager
import com.breadwallet.tools.manager.BRSharedPrefs
import com.breadwallet.tools.security.BrdUserManager
import com.breadwallet.tools.security.isFingerPrintAvailableAndSetup
import com.breadwallet.tools.util.EmailTarget
import com.breadwallet.tools.util.ServerBundlesHelper
import com.breadwallet.tools.util.SupportManager
import com.breadwallet.tools.util.TokenUtil
import com.breadwallet.tools.util.btc
import com.breadwallet.ui.settings.SettingsScreen.E
import com.breadwallet.ui.settings.SettingsScreen.F
import com.breadwallet.util.errorHandler
import com.platform.APIClient
import com.breadwallet.platform.interfaces.AccountMetaDataProvider
import com.breadwallet.util.isBitcoinLike
import com.spotify.mobius.Connection
import com.spotify.mobius.functions.Consumer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Dispatchers.Main
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.merge
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.invoke
import kotlinx.coroutines.launch
import org.json.JSONObject
import java.io.File
import java.math.BigDecimal
import java.text.DecimalFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.logging.Level
import kotlin.text.Charsets.UTF_8

private const val DEVELOPER_OPTIONS_TITLE = "Developer Options"
private const val DETAILED_LOGGING_MESSAGE = "Detailed logging is enabled for this session."
private const val CLEAR_BLOCKCHAIN_DATA_MESSAGE = "Clearing blockchain data"
private const val TEZOS_ID = "tezos-mainnet:__native__"  // TODO: DROID-1854 remove tezos filter
private const val OPTION_UPDATE_THROTTLE = 1000L
private const val TRANSACTIONS_FILE_PREFIX = "BRD-transactions-"
private const val AUTHORITY_BASE = "com.breadwallet"
private const val HEADER =
    "currency_code,timestamp,transaction_id,direction,from_address,to_address,amount,amount_unit,fee,fee_unit,memo\n"

private val df = (DecimalFormat.getInstance(Locale.ROOT) as DecimalFormat).apply {
    negativePrefix = ""
    negativeSuffix = ""
    maximumFractionDigits = 99
}
private val dtf = SimpleDateFormat("MMMM-d-yyy_hh-mm-ss", Locale.ROOT)

class SettingsScreenHandler(
    private val output: Consumer<E>,
    private val context: Context,
    private val experimentsRepository: ExperimentsRepository,
    private val metaDataManager: AccountMetaDataProvider,
    private val userManager: BrdUserManager,
    private val breadBox: BreadBox,
    private val bdbAuthInterceptor: BdbAuthInterceptor,
    private val supportManager: SupportManager
) : Connection<F>, CoroutineScope {

    override val coroutineContext = SupervisorJob() + Dispatchers.Default + errorHandler()

    @Suppress("ComplexMethod")
    override fun accept(value: F) {
        when (value) {
            is F.LoadOptions -> {
                merge(
                    BRSharedPrefs.promptChanges(),
                    BRSharedPrefs.preferredFiatIsoChanges().map { }
                ).onStart { emit(Unit) }
                    .throttleLatest(OPTION_UPDATE_THROTTLE)
                    .onEach { loadOptions(value.section) }
                    .launchIn(this)
            }
            F.SendAtmFinderRequest -> sendAtmFinderRequest()
            F.SendLogs -> launch(Main) {
                supportManager.submitEmailRequest(EmailTarget.ANDROID_TEAM)
            }
            is F.SetApiServer -> {
                if (BuildConfig.DEBUG) {
                    BRSharedPrefs.putDebugHost(value.host)
                }
                loadOptions(SettingsSection.DEVELOPER_OPTION)
            }
            is F.SetPlatformDebugUrl -> {
                ServerBundlesHelper.setWebPlatformDebugURL(value.url)
                loadOptions(SettingsSection.DEVELOPER_OPTION)
            }
            is F.SetPlatformBundle -> {
                ServerBundlesHelper.setDebugBundle(
                    context,
                    ServerBundlesHelper.Type.WEB,
                    value.bundle
                )
                loadOptions(SettingsSection.DEVELOPER_OPTION)
            }
            is F.SetTokenBundle -> {
                ServerBundlesHelper.setDebugBundle(
                    context,
                    ServerBundlesHelper.Type.TOKEN,
                    value.bundle
                )
                loadOptions(SettingsSection.DEVELOPER_OPTION)
            }
            F.ResetDefaultCurrencies -> {
                metaDataManager.resetDefaultWallets()
                output.accept(E.OnWalletsUpdated)
            }
            F.WipeNoPrompt -> {
                context.getSystemService(ActivityManager::class.java)
                    ?.clearApplicationUserData()
            }
            F.GetPaperKey -> launch {
                try {
                    val phrase = checkNotNull(userManager.getPhrase()).toString(UTF_8)
                    output.accept(E.ShowPhrase(phrase.split(" ")))
                } catch (e: UserNotAuthenticatedException) {
                    // User denied confirmation, ignored
                }
            }
            F.EnableAllWallets -> {
                TokenUtil.getTokenItems()
                    .filter(TokenItem::isSupported)
                    .forEach { token ->
                        metaDataManager.enableWallet(token.currencyId)
                    }
            }
            F.ClearBlockchainData -> launch {
                logDebug(CLEAR_BLOCKCHAIN_DATA_MESSAGE)
                breadBox.run {
                    close(true)
                    open(checkNotNull(userManager.getAccount()))
                }
                Main {
                    Toast.makeText(context, CLEAR_BLOCKCHAIN_DATA_MESSAGE, Toast.LENGTH_LONG).show()
                }
            }
            F.ToggleRateAppPrompt -> {
                BRSharedPrefs.appRatePromptShouldPromptDebug =
                    !BRSharedPrefs.appRatePromptShouldPromptDebug
            }
            F.RefreshTokens -> {
                BreadApp.applicationScope.launch {
                    userManager.putBdbJwt("", 0)
                    userManager.removeToken()
                    bdbAuthInterceptor.refreshClientToken()
                }
                output.accept(E.OnCloseHiddenMenu)
            }
            F.DetailedLogging -> launch {
                Logger.setJulLevel(Level.ALL)
                Main {
                    Toast.makeText(context, DETAILED_LOGGING_MESSAGE, Toast.LENGTH_LONG).show()
                }
            }
            F.CopyPaperKey -> {
                BreadApp.applicationScope.launch {
                    val phrase = userManager.getPhrase()?.toString(UTF_8)
                    Main {
                        BRClipboardManager.putClipboard(phrase)
                        Toast.makeText(context, "Paper Key copied!", Toast.LENGTH_SHORT).show()
                    }
                }
            }
            F.ToggleTezos -> launch {
                val enabled = metaDataManager.enabledWallets().first()
                if (enabled.contains(TEZOS_ID)) {
                    metaDataManager.disableWallet(TEZOS_ID)
                } else {
                    metaDataManager.enableWallet(TEZOS_ID)
                }
            }
            F.GenerateTransactionsExportFile -> launch { exportTransactionsToFile() }
        }
    }

    override fun dispose() {
        coroutineContext.cancel()
    }

    private fun loadOptions(section: SettingsSection) {
        val items: List<SettingsItem> = when (section) {
            SettingsSection.HOME -> getHomeOptions()
            SettingsSection.PREFERENCES -> preferences
            SettingsSection.SECURITY -> securitySettings()
            SettingsSection.DEVELOPER_OPTION -> getDeveloperOptions()
            SettingsSection.BTC_SETTINGS -> btcOptions
            SettingsSection.BCH_SETTINGS -> bchOptions
            SettingsSection.HIDDEN -> getHiddenOptions()
        }
        output.accept(E.OnOptionsLoaded(items))
    }

    private fun getHiddenOptions(): List<SettingsItem> {
        return listOf(
            SettingsItem(
                "Refresh API Tokens",
                SettingsOption.REFRESH_TOKENS
            ),
            SettingsItem(
                "Clear Blockchain Data",
                SettingsOption.CLEAR_BLOCKCHAIN_DATA
            ),
            SettingsItem(
                "Enable Detailed Logging",
                SettingsOption.DETAILED_LOGGING
            )
        )
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
                        R.drawable.ic_atm_finder,
                        subHeader = context.getString(R.string.Settings_atmMapMenuItemSubtitle)
                    )
                )
            }
            add(
                SettingsItem(
                    context.getString(R.string.Settings_exportTransaction),
                    SettingsOption.EXPORT_TRANSACTIONS,
                    R.drawable.ic_export
                )
            )
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

    private val preferences get() = listOf(
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
            context.getString(R.string.Settings_resetCurrencies),
            SettingsOption.RESET_DEFAULT_CURRENCIES
        ),
        SettingsItem(
            context.getString(R.string.Settings_notifications),
            SettingsOption.NOTIFICATIONS
        )
    )

    private fun securitySettings(): List<SettingsItem> {
        val items = mutableListOf(
            SettingsItem(
                context.getString(R.string.UpdatePin_updateTitle),
                SettingsOption.UPDATE_PIN
            ),
            SettingsItem(
                context.getString(R.string.SecurityCenter_paperKeyTitle),
                SettingsOption.PAPER_KEY
            ),
            SettingsItem(
                context.getString(R.string.Settings_wipe_android),
                SettingsOption.WIPE
            )
        )
        if (isFingerPrintAvailableAndSetup(context)) {
            items.add(
                0, SettingsItem(
                    context.getString(R.string.TouchIdSettings_switchLabel_android),
                    SettingsOption.FINGERPRINT_AUTH
                )
            )
        }
        return items.toList()
    }

    private fun getDeveloperOptions(): List<SettingsItem> {
        val currentWebPlatformDebugURL = ServerBundlesHelper.getWebPlatformDebugURL()
        val webPlatformBundleAddOn = if (currentWebPlatformDebugURL.isNotEmpty()) {
            "(not used if debug URL specified)"
        } else {
            ""
        }
        val toggleRateAppPromptAddOn = BRSharedPrefs.appRatePromptShouldPromptDebug
        return listOf(
            SettingsItem(
                "Copy Paper Key",
                SettingsOption.COPY_PAPER_KEY
            ),
            SettingsItem(
                "Send Logs",
                SettingsOption.SEND_LOGS
            ),
            SettingsItem(
                "View Logs",
                SettingsOption.VIEW_LOGS
            ),
            SettingsItem(
                "View Metadata",
                SettingsOption.METADATA_VIEWER
            ),
            SettingsItem(
                "API Server",
                SettingsOption.API_SERVER,
                subHeader = APIClient.host
            ),
            SettingsItem(
                "Onboarding flow",
                SettingsOption.ONBOARDING_FLOW
            ),
            SettingsItem(
                "Web Platform Debug URL",
                SettingsOption.WEB_PLAT_DEBUG_URL,
                subHeader = ServerBundlesHelper.getWebPlatformDebugURL()
            ),
            SettingsItem(
                "Web Platform Bundle",
                SettingsOption.WEB_PLAT_BUNDLE,
                subHeader = ServerBundlesHelper.getBundle(ServerBundlesHelper.Type.WEB),
                addOn = webPlatformBundleAddOn
            ),
            SettingsItem(
                "Token Bundle",
                SettingsOption.TOKEN_BUNDLE,
                subHeader = ServerBundlesHelper.getBundle(ServerBundlesHelper.Type.TOKEN)
            ),
            SettingsItem(
                "Enable All Wallets",
                SettingsOption.ENABLE_ALL_WALLETS
            ),
            SettingsItem(
                "Native API Explorer",
                SettingsOption.NATIVE_API_EXPLORER
            ),
            SettingsItem(
                "Wipe Wallet (no prompt)",
                SettingsOption.WIPE_NO_PROMPT
            ),
            SettingsItem(
                "Toggle Rate App Prompt",
                SettingsOption.TOGGLE_RATE_APP_PROMPT,
                addOn = "show=$toggleRateAppPromptAddOn"
            ),
            SettingsItem(
                "Toggle Tezos",
                SettingsOption.TOGGLE_TEZOS
            )
        ) + getHiddenOptions()
    }

    private val btcOptions: List<SettingsItem> =
        mutableListOf(
            SettingsItem(
                context.getString(R.string.WalletConnectionSettings_menuTitle),
                SettingsOption.FAST_SYNC_BTC
            ),
            SettingsItem(
                context.getString(R.string.Settings_importTitle),
                SettingsOption.REDEEM_PRIVATE_KEY
            ),
            SettingsItem(
                context.getString(R.string.ReScan_header),
                SettingsOption.SYNC_BLOCKCHAIN_BTC
            )
        ).apply {
            launch {
                val modeMap = metaDataManager.walletModes().first()
                val btcCurrencyId = TokenUtil.tokenForCode(btc)?.currencyId ?: ""
                if (modeMap[btcCurrencyId] != WalletManagerMode.API_ONLY) {
                    add(
                        SettingsItem(
                            context.getString(R.string.NodeSelector_title),
                            SettingsOption.BTC_NODES
                        )
                    )
                }
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
            SettingsOption.SYNC_BLOCKCHAIN_BCH
        )
    )

    private fun sendAtmFinderRequest() {
        val mapExperiment = ExperimentsRepositoryImpl.experiments[Experiments.ATM_MAP.key]
        val mapPath = mapExperiment?.meta.orEmpty().replace("\\/", "/")
        val mapJsonObj = JSONObject(mapPath)
        val url = mapJsonObj.getString("url")

        output.accept(E.OnATMMapClicked(url, mapPath))
    }

    private suspend fun exportTransactionsToFile() {
        val wallets = getEnabledWallets()
        val file = File(context.externalCacheDir, "$TRANSACTIONS_FILE_PREFIX${dtf.format(Date())}.csv")
        file.bufferedWriter().use { out ->
            out.write(HEADER)
            wallets.forEach { wallet ->
                val feeWallet = if (wallet.currency.isNative()) wallet else breadBox.wallet(wallet.unitForFee.currency.code).first()
                wallet.transfers
                    .filter { it.state.type == TransferState.Type.INCLUDED }
                    .sortedBy { it.confirmation.orNull()?.confirmationTime ?: Date()}
                    .forEach { transfer ->
                        val memo = getMemo(transfer)
                        out.write("${transfer.export(memo, feeWallet)}\n")
                    }
            }

        }
        val authority = buildString {
            append(AUTHORITY_BASE)
            if (!breadBox.isMainnet) append(".testnet")
            if (BuildConfig.DEBUG) append(".debug")
        }
        output.accept(E.OnTransactionsExportFileGenerated(FileProvider.getUriForFile(context, authority, file)))
    }

    private fun Transfer.export(memo: String, feeWallet: Wallet): String {
        var dir = ""
        var src = source.orNull()?.toSanitizedString() ?: ""

        var amt = BigDecimal(
            amount.toStringAsUnit(wallet.unit, df).or("")
        ).stripTrailingZeros().toPlainString()

        var f = ""
        var feeCode = ""

        if (direction == TransferDirection.RECEIVED) {
            dir = "received"
        } else {
            dir = "sent"
            src = if (wallet.currency.code.isBitcoinLike()) "" else src
            amt = "-$amt"
            f = BigDecimal(
                fee.toStringAsUnit(feeWallet.unit, df).or("")
            ).stripTrailingZeros().toPlainString()
            feeCode = feeWallet.unit.currency.code.toLowerCase(Locale.ROOT)
        }

        val feeToken = feeForToken()
        val memoOrFeeToken = if (feeToken.isNotBlank()) "Fee for token transfer: $feeToken" else memo

        return listOf(
            "\"${wallet.currency.code.toLowerCase(Locale.ROOT)}\"",
            "\"${confirmation.orNull()?.confirmationTime?.toInstant() ?: ""}\"",
            "\"${hashString()}\"",
            "\"$dir\"",
            "\"$src\"",
            "\"${target.orNull()?.toSanitizedString() ?: ""}\"",
            amt,
            "\"${wallet.unit.currency.code.toLowerCase(Locale.ROOT)}\"",
            f,
            "\"$feeCode\"",
            "\"$memoOrFeeToken\""
        ).joinToString(",")
    }

    private suspend fun getMemo(transfer: Transfer) =
        when (val metadata = metaDataManager.txMetaData(transfer).first()) {
            is TxMetaDataEmpty -> ""
            is TxMetaDataValue -> metadata.comment ?: ""
        }

    private suspend fun getEnabledWallets() =
        metaDataManager.enabledWallets().first()
            .map { currencyId ->
                breadBox.wallet(currencyId).filter {
                        wallet -> wallet.walletManager.state.type != WalletManagerState.Type.SYNCING
                }.first()
            }.sortedBy { it.currency.code }
}