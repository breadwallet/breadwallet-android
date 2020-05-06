/**
 * BreadWallet
 *
 * Created by Drew Carlson <drew.carlson@breadwallet.com> on 9/23/19.
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
package com.breadwallet.app

import android.annotation.SuppressLint
import android.app.Activity
import android.app.Application
import android.app.KeyguardManager
import android.content.Context
import android.content.IntentFilter
import android.net.ConnectivityManager
import android.os.Handler
import android.os.Looper
import android.util.Log
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ProcessLifecycleOwner
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKeys
import com.breadwallet.BuildConfig
import com.breadwallet.breadbox.BdbAuthInterceptor
import com.breadwallet.breadbox.BreadBox
import com.breadwallet.breadbox.BreadBoxCloseWorker
import com.breadwallet.breadbox.CoreBreadBox
import com.breadwallet.corecrypto.CryptoApiProvider
import com.breadwallet.crypto.CryptoApi
import com.breadwallet.crypto.blockchaindb.BlockchainDb
import com.breadwallet.installHooks
import com.breadwallet.legacy.view.dialog.DialogActivity
import com.breadwallet.legacy.view.dialog.DialogActivity.DialogType
import com.breadwallet.logger.logDebug
import com.breadwallet.logger.logError
import com.breadwallet.repository.ExperimentsRepository
import com.breadwallet.repository.ExperimentsRepositoryImpl
import com.breadwallet.repository.RatesRepository
import com.breadwallet.tools.crypto.Base32
import com.breadwallet.tools.crypto.CryptoHelper
import com.breadwallet.tools.manager.BRReportsManager
import com.breadwallet.tools.manager.BRSharedPrefs
import com.breadwallet.tools.manager.InternetManager
import com.breadwallet.tools.manager.updateRatesForCurrencies
import com.breadwallet.tools.security.BrdUserManager
import com.breadwallet.tools.security.CryptoUserManager
import com.breadwallet.tools.security.KeyStoreStatus
import com.breadwallet.tools.services.BRDFirebaseMessagingService
import com.breadwallet.tools.util.EventUtils
import com.breadwallet.tools.util.ServerBundlesHelper
import com.breadwallet.tools.util.TokenUtil
import com.breadwallet.util.CryptoUriParser
import com.breadwallet.util.errorHandler
import com.breadwallet.util.isEthereum
import com.breadwallet.util.trackAddressMismatch
import com.breadwallet.util.usermetrics.UserMetricsUtil
import com.platform.APIClient
import com.platform.HTTPServer
import com.platform.interfaces.AccountMetaDataProvider
import com.platform.interfaces.KVStoreProvider
import com.platform.interfaces.MetaDataManager
import com.platform.interfaces.WalletProvider
import com.platform.tools.KVStoreManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.cancelChildren
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient
import org.kodein.di.DKodein
import org.kodein.di.Kodein
import org.kodein.di.KodeinAware
import org.kodein.di.android.x.androidXModule
import org.kodein.di.direct
import org.kodein.di.erased.bind
import org.kodein.di.erased.instance
import org.kodein.di.erased.on
import org.kodein.di.erased.singleton
import java.io.File
import java.io.UnsupportedEncodingException
import java.util.Locale
import java.util.regex.Pattern

private const val LOCK_TIMEOUT = 180_000L // 3 minutes in milliseconds

@Suppress("TooManyFunctions")
class BreadApp : Application(), KodeinAware {

    companion object {
        private val TAG = BreadApp::class.java.name

        init {
            CryptoApi.initialize(CryptoApiProvider.getInstance())
        }

        // The server(s) on which the API is hosted
        private val HOST = when {
            BuildConfig.DEBUG -> "stage2.breadwallet.com"
            else -> "api.breadwallet.com"
        }

        // The wallet ID is in the form "xxxx xxxx xxxx xxxx" where x is a lowercase letter or a number.
        private const val WALLET_ID_PATTERN = "^[a-z0-9 ]*$"
        private const val WALLET_ID_SEPARATOR = " "
        private const val NUMBER_OF_BYTES_FOR_SHA256_NEEDED = 10
        private const val SERVER_SHUTDOWN_DELAY_MILLIS = 60000L // 60 seconds

        @SuppressLint("StaticFieldLeak")
        private lateinit var mInstance: BreadApp

        @SuppressLint("StaticFieldLeak")
        private var mCurrentActivity: Activity? = null

        /** [CoroutineScope] matching the lifetime of the application. */
        val applicationScope = CoroutineScope(
            SupervisorJob() + Dispatchers.Default + errorHandler("applicationScope")
        )

        private val startedScope = CoroutineScope(
            SupervisorJob() + Dispatchers.Default + errorHandler("startedScope")
        )

        fun getBreadBox(): BreadBox = mInstance.direct.instance()
        fun getAccountMetaDataProvider(): AccountMetaDataProvider = mInstance.direct.instance()

        // TODO: For code organization only, to be removed
        fun getStorageDir(context: Context): File {
            return File(context.filesDir, "cryptocore")
        }

        // TODO: Find better place/means for this
        fun getDefaultEnabledWallets() = when {
            BuildConfig.BITCOIN_TESTNET -> listOf(
                "bitcoin-testnet:__native__",
                "ethereum-ropsten:__native__",
                "ethereum-mainnet:0x558ec3152e2eb2174905cd19aea4e34a23de9ad6"
            )
            else -> listOf(
                "bitcoin-mainnet:__native__",
                "ethereum-mainnet:__native__",
                "ethereum-mainnet:0x558ec3152e2eb2174905cd19aea4e34a23de9ad6"
            )
        }

        /**
         * Initialize the wallet id (rewards id), and save it in the SharedPreferences.
         */
        private fun initializeWalletId() {
            GlobalScope.launch(Dispatchers.Main) {
                val walletId = getBreadBox()
                    .wallets(false)
                    .mapNotNull { wallets ->
                        wallets.find { it.currency.code.isEthereum() }
                    }
                    .take(1)
                    .map { generateWalletId(it.target.toString()) }
                    .flowOn(Dispatchers.Default)
                    .first()
                if (walletId.isNullOrBlank() || !walletId.matches(WALLET_ID_PATTERN.toRegex())) {
                    val error = IllegalStateException("Generated corrupt walletId: $walletId")
                    BRReportsManager.reportBug(error)
                }
                BRSharedPrefs.putWalletRewardId(id = walletId ?: "")
            }
        }

        /**
         * Generates the wallet id (rewards id) based on the Ethereum address. The format of the id is
         * "xxxx xxxx xxxx xxxx", where x is a lowercase letter or a number.
         *
         * @return The wallet id.
         */
        // TODO: This entire operation should be moved into a separate class.
        @Synchronized
        @Suppress("ReturnCount")
        fun generateWalletId(address: String): String? {
            try {
                // Remove the first 2 characters i.e. 0x
                val rawAddress = address.drop(2)

                // Get the address bytes.
                val addressBytes = rawAddress.toByteArray()

                // Run SHA256 on the address bytes.
                val sha256Address = CryptoHelper.sha256(addressBytes) ?: byteArrayOf()
                if (sha256Address.isEmpty()) {
                    BRReportsManager.reportBug(IllegalAccessException("Failed to generate SHA256 hash."))
                    return null
                }

                // Get the first 10 bytes of the SHA256 hash.
                val firstTenBytes =
                    sha256Address.sliceArray(0 until NUMBER_OF_BYTES_FOR_SHA256_NEEDED)

                // Convert the first 10 bytes to a lower case string.
                val base32String = Base32.encode(firstTenBytes).toLowerCase(Locale.ROOT)

                // Insert a space every 4 chars to match the specified format.
                val builder = StringBuilder()
                val matcher = Pattern.compile(".{1,4}").matcher(base32String)
                var separator = ""
                while (matcher.find()) {
                    val piece = base32String.substring(matcher.start(), matcher.end())
                    builder.append(separator + piece)
                    separator = WALLET_ID_SEPARATOR
                }
                return builder.toString()
            } catch (e: UnsupportedEncodingException) {
                logError("Unable to get address bytes.", e)
                return null
            }
        }

        // TODO: Refactor so this does not store the current activity like this.
        @JvmStatic
        @Deprecated("")
        fun getBreadContext(): Context {
            var app: Context? = mCurrentActivity
            if (app == null) {
                app = mInstance
            }
            return app
        }

        // TODO: Refactor so this does not store the current activity like this.
        @JvmStatic
        fun setBreadContext(app: Activity?) {
            mCurrentActivity = app
        }

        /**
         * @return host or debug host if build is DEBUG
         */
        @JvmStatic
        val host: String
            get() {
                if (BuildConfig.DEBUG) {
                    val host = BRSharedPrefs.getDebugHost(mInstance)
                    if (!host.isNullOrBlank()) {
                        return host
                    }
                }
                return HOST
            }

        /**
         * Sets the debug host into the shared preferences, only do that if the build is DEBUG.
         *
         * @param host
         */
        @JvmStatic
        fun setDebugHost(host: String) {
            if (BuildConfig.DEBUG) {
                BRSharedPrefs.putDebugHost(mCurrentActivity, host)
            }
        }

        /** Provides access to [DKodein]. Meant only for Java compatibility. **/
        @JvmStatic
        fun getKodeinInstance(): DKodein {
            return mInstance.direct
        }
    }

    override val kodein by Kodein.lazy {
        importOnce(androidXModule(this@BreadApp))

        bind<CryptoUriParser>() with singleton {
            CryptoUriParser(instance())
        }

        bind<APIClient>() with singleton {
            APIClient(this@BreadApp, direct.instance())
        }

        bind<BrdUserManager>() with singleton {
            CryptoUserManager(
                EncryptedSharedPreferences.create(
                    "crypto_shared_prefs",
                    MasterKeys.getOrCreate(MasterKeys.AES256_GCM_SPEC),
                    instance(),
                    EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                    EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
                ),
                instance()
            )
        }

        bind<KVStoreProvider>() with singleton {
            KVStoreManager(this@BreadApp)
        }

        val metaDataManager by lazy { MetaDataManager(direct.instance()) }

        bind<WalletProvider>() with singleton { metaDataManager }

        bind<AccountMetaDataProvider>() with singleton { metaDataManager }

        bind<BlockchainDb>() with singleton {
            val httpClient = OkHttpClient()
            val authInterceptor = BdbAuthInterceptor(httpClient, direct.instance())
            BlockchainDb(
                httpClient.newBuilder()
                    .addInterceptor(authInterceptor)
                    .build()
            )
        }

        bind<BreadBox>() with singleton {
            CoreBreadBox(
                getStorageDir(this@BreadApp),
                !BuildConfig.BITCOIN_TESTNET,
                instance(),
                instance(),
                instance()
            )
        }

        bind<ExperimentsRepository>() with singleton { ExperimentsRepositoryImpl }

        bind<RatesRepository>() with singleton { RatesRepository.getInstance(this@BreadApp) }
    }

    private var mDelayServerShutdownCode = -1
    private var mDelayServerShutdown = false
    private var mServerShutdownHandler: Handler? = null
    private var mServerShutdownRunnable: Runnable? = null

    private var accountLockJob: Job? = null

    private val userManager: BrdUserManager by instance()
    private val apiClient: APIClient by instance()

    /**
     * Returns true if the device state is valid. The device state is considered valid, if the device password
     * is enabled and if the Android key store state is valid.  The Android key store can be invalided if the
     * device password was removed or if fingerprints are added/removed.
     *
     * @return True, if the device state is valid; false, otherwise.
     */
    // TODO: This operation should be extracted to a new class
    fun isDeviceStateValid(): Boolean {
        val isDeviceStateValid: Boolean
        var dialogType = DialogType.DEFAULT

        if (!direct.on(this).instance<KeyguardManager>().isKeyguardSecure) {
            isDeviceStateValid = false
            dialogType = DialogType.ENABLE_DEVICE_PASSWORD
        } else {
            when (userManager.getKeyStoreStatus()) {
                KeyStoreStatus.VALID -> isDeviceStateValid = true
                KeyStoreStatus.INVALID_WIPE -> {
                    isDeviceStateValid = false
                    dialogType = DialogType.KEY_STORE_INVALID_WIPE
                }
                KeyStoreStatus.INVALID_UNINSTALL -> {
                    isDeviceStateValid = false
                    dialogType = DialogType.KEY_STORE_INVALID_UNINSTALL
                }
            }
        }

        if (dialogType != DialogType.DEFAULT) {
            DialogActivity.startDialogActivity(this, dialogType)
        }

        return isDeviceStateValid
    }

    override fun onCreate() {
        super.onCreate()
        installHooks()
        mInstance = this

        BRSharedPrefs.provideContext(this)

        ProcessLifecycleOwner.get().lifecycle.addObserver(ApplicationLifecycleObserver())
        ApplicationLifecycleObserver.addApplicationLifecycleListener { event ->
            logDebug(event.name)
            when (event) {
                Lifecycle.Event.ON_START -> handleOnStart()
                Lifecycle.Event.ON_STOP -> handleOnStop()
                Lifecycle.Event.ON_DESTROY -> handleOnDestroy()
                else -> Unit
            }
        }

        if (!userManager.isInitialized()) {
            // extract the bundles from the resources to be ready when the wallet is initialized
            applicationScope.launch {
                ServerBundlesHelper.extractBundlesIfNeeded(mInstance)
            }

            applicationScope.launch(Dispatchers.Default) {
                TokenUtil.initialize(mInstance, false)
            }
        }

        registerReceiver(
            InternetManager.getInstance(),
            IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION)
        )

        // Start our local server as soon as the application instance is created, since we need to
        // display support WebViews during onboarding.
        HTTPServer.getInstance().startServer(this)
    }

    /**
     * Each time the app resumes, check to see if the device state is valid.
     * Even if the wallet is not initialized, we may need tell the user to enable the password.
     */
    private fun handleOnStart() {
        accountLockJob?.cancel()
        setDelayServerShutdown(false, -1)

        val breadBox = getBreadBox()
        if (isDeviceStateValid() && userManager.isInitialized()) {
            startWithInitializedWallet(breadBox)
        }
    }

    private fun handleOnStop() {
        if (userManager.isInitialized()) {
            accountLockJob = applicationScope.launch {
                delay(LOCK_TIMEOUT)
                userManager.lock()
            }
            BreadBoxCloseWorker.enqueueWork()
            applicationScope.launch {
                EventUtils.saveEvents(this@BreadApp)
                EventUtils.pushToServer(this@BreadApp)
            }
            if (!mDelayServerShutdown) {
                logDebug("Shutting down HTTPServer.")
                HTTPServer.getInstance().stopServer()
            } else {
                // If server shutdown needs to be delayed, it will occur after
                // SERVER_SHUTDOWN_DELAY_MILLIS.  This may be cancelled if the app
                // is closed before execution or the user returns to the app.
                logDebug("Delaying HTTPServer shutdown.")
                if (mServerShutdownHandler == null) {
                    mServerShutdownHandler = Handler(Looper.getMainLooper())
                }
                mServerShutdownRunnable = Runnable {
                    logDebug("Shutdown delay elapsed, shutting down HTTPServer.")
                    HTTPServer.getInstance().stopServer()
                    mServerShutdownRunnable = null
                    mServerShutdownHandler = null
                }
                mServerShutdownHandler!!.postDelayed(
                    mServerShutdownRunnable,
                    SERVER_SHUTDOWN_DELAY_MILLIS
                )
            }
        }
        startedScope.coroutineContext.cancelChildren()
    }

    private fun handleOnDestroy() {
        if (HTTPServer.getInstance().isRunning) {
            if (mServerShutdownHandler != null && mServerShutdownRunnable != null) {
                logDebug("Preempt delayed server shutdown callback")
                mServerShutdownHandler!!.removeCallbacks(mServerShutdownRunnable)
            }
            logDebug("Shutting down HTTPServer.")
            HTTPServer.getInstance().stopServer()
            mDelayServerShutdown = false
        }

        getBreadBox().apply { if (isOpen) close() }
    }

    fun startWithInitializedWallet(breadBox: BreadBox, migrate: Boolean = false) {
        val context = mInstance.applicationContext
        BreadBoxCloseWorker.cancelEnqueuedWork()
        incrementAppForegroundedCounter()

        if (!breadBox.isOpen) {
            val account = checkNotNull(userManager.getAccount()) {
                "Wallet is initialized but Account is null"
            }

            breadBox.open(account)
        }

        initializeWalletId()
        BRDFirebaseMessagingService.initialize(context)
        HTTPServer.getInstance().startServer(this)
        apiClient.updatePlatform()
        applicationScope.launch {
            UserMetricsUtil.makeUserMetricsRequest(context)
        }

        getAccountMetaDataProvider()
            .recoverAll(migrate)
            .launchIn(startedScope)

        breadBox.currencyCodes()
            .updateRatesForCurrencies(context)
            .launchIn(startedScope)

        applicationScope.launch {
            trackAddressMismatch(breadBox)
        }
    }

    private fun incrementAppForegroundedCounter() {
        BRSharedPrefs.putInt(
            this, BRSharedPrefs.APP_FOREGROUNDED_COUNT,
            BRSharedPrefs.getInt(this, BRSharedPrefs.APP_FOREGROUNDED_COUNT, 0) + 1
        )
    }

    /**
     * When delayServerShutdown is true, the HTTPServer will remain
     * running after onStop, until onDestroy.
     */
    @Synchronized
    fun setDelayServerShutdown(delayServerShutdown: Boolean, requestCode: Int) {
        Log.d(TAG, "setDelayServerShutdown($delayServerShutdown, $requestCode)")
        val isMatchingRequestCode = mDelayServerShutdownCode == requestCode ||
            requestCode == -1 || // Force the update regardless of current request
            mDelayServerShutdownCode == -1 // No initial request

        if (isMatchingRequestCode) {
            mDelayServerShutdown = delayServerShutdown
            mDelayServerShutdownCode = requestCode
            if (!mDelayServerShutdown &&
                mServerShutdownRunnable != null &&
                mServerShutdownHandler != null
            ) {
                Log.d(TAG, "Cancelling delayed HTTPServer execution.")
                mServerShutdownHandler!!.removeCallbacks(mServerShutdownRunnable)
                mServerShutdownHandler = null
                mServerShutdownRunnable = null
            }
            if (!mDelayServerShutdown) {
                mDelayServerShutdownCode = -1
            }
        }
    }
}
