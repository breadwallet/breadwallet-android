/**
 * BreadWallet
 *
 * Created by Mihail Gutan on <mihail@breadwallet.com> 11/5/18.
 * Copyright (c) 2018 breadwallet LLC
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
package com.breadwallet.tools.util

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.widget.Toast
import androidx.core.content.FileProvider
import com.breadwallet.BuildConfig
import com.breadwallet.app.BreadApp.Companion.generateWalletId
import com.breadwallet.breadbox.BreadBox
import com.breadwallet.breadbox.isEthereum
import com.breadwallet.crypto.Address
import com.breadwallet.crypto.TransferState
import com.breadwallet.crypto.errors.TransferSubmitPosixError
import com.breadwallet.tools.manager.BRSharedPrefs.getBundleHash
import com.breadwallet.tools.manager.BRSharedPrefs.getDeviceId
import com.breadwallet.tools.manager.BRSharedPrefs.getWalletRewardId
import com.breadwallet.tools.security.BrdUserManager
import com.breadwallet.tools.security.CryptoUserManager
import com.breadwallet.util.pubKeyToEthAddress
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import org.apache.commons.io.IOUtils
import java.io.IOException
import java.util.Locale
import java.util.TimeZone

// Filters out our apps events at log level = verbose
private const val LOGCAT_COMMAND = "logcat -d ${BuildConfig.APPLICATION_ID}:V"

private const val DEFAULT_EMAIL_SUBJECT = "BRD Android App Feedback [ID:%s]" // Placeholder is for a unique id.
private const val DEFAULT_EMAIL_BODY = "[Please add your feedback.]"
private val DEFAULT_DEBUG_INFO = listOf(DebugInfo.APPLICATION, DebugInfo.DEVICE, DebugInfo.WALLET)

private const val DEFAULT_LOG_ATTACHMENT_BODY = "No logs."
private const val FAILED_ERROR_MESSAGE = "Failed to get logs."
private const val NO_EMAIL_APP_ERROR_MESSAGE = "No email app found."

private const val LOGS_FILE_NAME = "Logs.txt"

const val CUSTOM_DATA_KEY_TITLE = "title"

enum class DebugInfo {
    DEVICE,
    WALLET,
    APPLICATION,
    CUSTOM
}

enum class EmailTarget(val address: String) {
    ANDROID_TEAM("android@brd.com"),
    SUPPORT_TEAM("support@brd.com")
}

class SupportManager(
    private val context: Context,
    private val breadBox: BreadBox,
    private val userManager: BrdUserManager
) {

    fun submitEmailRequest(
        to: EmailTarget = EmailTarget.SUPPORT_TEAM,
        subject: String = String.format(DEFAULT_EMAIL_SUBJECT, getDeviceId()),
        body: String = DEFAULT_EMAIL_BODY,
        diagnostics: List<DebugInfo> = DEFAULT_DEBUG_INFO,
        customData: Map<String, String> = emptyMap(),
        attachLogs: Boolean = true
    ) {
        val emailIntent = createEmailIntent(to.address, subject, body, diagnostics, customData, attachLogs)
        launchIntent(emailIntent, to.address)
    }

    private fun createEmailIntent(
        to: String,
        subject: String,
        body: String,
        diagnostics: List<DebugInfo>,
        customData: Map<String, String>,
        attachLogs: Boolean
    ) = Intent(Intent.ACTION_SEND).apply {
        putExtra(Intent.EXTRA_EMAIL, arrayOf(to))
        putExtra(Intent.EXTRA_SUBJECT, subject)
        putExtra(Intent.EXTRA_TEXT, buildBody(body, diagnostics, customData))

        if (attachLogs) {
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            putExtra(Intent.EXTRA_STREAM, getLogsUri())
        }
    }

    private fun launchIntent(
        emailIntent: Intent,
        to: String
    ) {
        try {
            context.startActivity(
                emailIntent.apply {
                    selector = Intent.parseUri("mailto:$to", 0)
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
            )
        } catch (e: ActivityNotFoundException) {
            GlobalScope.launch(Dispatchers.Main) {
                Toast.makeText(context, NO_EMAIL_APP_ERROR_MESSAGE, Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun getLogsUri(): Uri {
        val file = FileHelper.saveToExternalStorage(context, LOGS_FILE_NAME, getLogs())
        return FileProvider.getUriForFile(context, BuildConfig.APPLICATION_ID, file)
    }

    private fun buildBody(
        bodyText: String,
        debugInfo: List<DebugInfo>,
        customData: Map<String, String>
    ) = buildString {
        addFeedbackBlock(bodyText)
        appendln()

        debugInfo.forEach { debugInfo ->
            when (debugInfo) {
                DebugInfo.APPLICATION -> addApplicationBlock()
                DebugInfo.DEVICE -> addDeviceBlock()
                DebugInfo.WALLET -> addWalletBlock()
                DebugInfo.CUSTOM -> addDebugBlock(customData)
            }
            appendln()
        }
    }

    private fun getLogs(): String = try {
        val process = Runtime.getRuntime().exec(LOGCAT_COMMAND)
        IOUtils.toString(process.inputStream, Charsets.UTF_8)
    } catch (ex: IOException) {
        GlobalScope.launch(Dispatchers.Main) {
            Toast.makeText(context, FAILED_ERROR_MESSAGE, Toast.LENGTH_LONG).show()
        }
        DEFAULT_LOG_ATTACHMENT_BODY
    }

    private fun StringBuilder.addFeedbackBlock(feedback: String) {
        appendln("Feedback")
        appendln("------------")
        appendln(feedback)
    }

    private fun StringBuilder.addApplicationBlock() {
        appendln("Application (${BuildConfig.BUILD_TYPE})")
        appendln("------------")
        appendln("Package: ${BuildConfig.APPLICATION_ID}")
        appendln("Version: ${BuildConfig.VERSION_NAME} Build ${BuildConfig.BUILD_VERSION}")
        appendln(
            if (BuildConfig.BITCOIN_TESTNET) {
                "Network: Testnet"
            } else {
                "Network: Mainnet"
            }
        )
        for (bundleName in ServerBundlesHelper.getBundleNames()) {
            appendln("Bundle '$bundleName' version: ${getBundleHash(bundleName)}")
        }
    }

    private fun StringBuilder.addDeviceBlock() {
        appendln("Device")
        appendln("------------")
        appendln("Android Version: ${Build.VERSION.RELEASE}")
        appendln("Device Model: ${Build.MANUFACTURER} ${Build.MODEL}")
        appendln("Time Zone: ${TimeZone.getDefault().id}")
        appendln("Locale: ${Locale.getDefault().displayName}")
    }

    private fun StringBuilder.addWalletBlock() {
        appendln("Wallet")
        appendln("------------")
        appendln("Wallet id: ${getWalletRewardId()}")
        appendln("Device id: ${getDeviceId()}")
        breadBox.getSystemUnsafe()?.let { system ->
            system.walletManagers?.forEach { manager ->
                append("${manager.currency.name}: ")
                append("mode=${manager.mode.name}, ")
                append("state=${manager.state.type.name}, ")
                append("height=${manager.network.height}")
                appendln()
            }

            system.wallets.forEach { wallet ->
                val count = wallet.transfers.count { transfer ->
                    transfer.state.type == TransferState.Type.SUBMITTED
                }
                if (count > 0) {
                    appendln("Submitted ${wallet.currency.code} transfers: $count")
                }
            }

            system.wallets
                .flatMap { wallet ->
                    wallet.transfers.filter { transfer ->
                        transfer.state.type == TransferState.Type.FAILED &&
                            !transfer.confirmation.isPresent
                    }
                }
                .forEach { transfer ->
                    val currencyCode = transfer.wallet.currency.code
                    val errorMessage =
                        when (val transferError = transfer.state.failedError.orNull()) {
                            is TransferSubmitPosixError -> {
                                "Posix Error: ${transferError.errnum}, ${transferError.message}"
                            }
                            else -> "Unknown Error ${transferError?.message ?: ""}"
                        }
                    appendln("Failed $currencyCode Transfer: error='$errorMessage'")
                }

            if (userManager is CryptoUserManager) {
                val ethWallet = system.wallets
                    .firstOrNull { wallet ->
                        wallet.currency.isEthereum()
                    }
                val storedAddress = userManager.getEthPublicKey().pubKeyToEthAddress()
                if (storedAddress != null && ethWallet != null) {
                    val network = ethWallet.walletManager.network
                    val coreAddress = Address.create(storedAddress, network).orNull()?.toString()
                    val walletAddress = ethWallet.target.toString()
                    if (!walletAddress.equals(coreAddress, true)) {
                        appendln("Stored Address: $storedAddress")
                        appendln("Stored Address Id: ${generateWalletId(storedAddress)}")
                    }
                }
            }
        }
    }

    private fun StringBuilder.addDebugBlock(debugData: Map<String, String>) {
        if (debugData.isEmpty()) return
        appendln(
            if (debugData.containsKey(CUSTOM_DATA_KEY_TITLE)) {
                debugData[CUSTOM_DATA_KEY_TITLE]
            } else {
                "Debug"
            }
        )
        appendln("------------")
        debugData.entries
            .filter { !it.key.equals(CUSTOM_DATA_KEY_TITLE) }
            .forEach {
                appendln("${it.key} : ${it.value}")
            }
    }
}
