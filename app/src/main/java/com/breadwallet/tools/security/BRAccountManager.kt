/**
 * BreadWallet
 *
 * Created by Drew Carlson <drew.carlson@breadwallet.com> on 8/12/19.
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
package com.breadwallet.tools.security

import android.app.Activity
import android.content.SharedPreferences
import android.security.keystore.UserNotAuthenticatedException
import android.text.format.DateUtils
import com.breadwallet.app.BreadApp
import com.breadwallet.crypto.Account
import com.breadwallet.crypto.Key
import com.breadwallet.logger.logInfo
import com.breadwallet.tools.crypto.CryptoHelper.hexEncode
import com.breadwallet.tools.crypto.CryptoHelper.hexDecode
import com.breadwallet.tools.manager.BRSharedPrefs
import com.platform.interfaces.AccountMetaDataProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.util.Date
import kotlin.math.pow

const val PIN_LENGTH = 6
const val LEGACY_PIN_LENGTH = 4
private const val MAX_UNLOCK_ATTEMPTS = 3
private const val JWT_EXP_PADDING_MS = 10_000L

/** Manages creation, recovery, and access to an [Account]. */
@Suppress("TooManyFunctions")
interface BRAccountManager {
    suspend fun createAccount(phrase: ByteArray): Account
    suspend fun recoverAccount(phrase: ByteArray): Account
    suspend fun migrateAccount(): Boolean

    fun isAccountInitialized(): Boolean
    fun isMigrationRequired(): Boolean
    fun getKeyStoreStatus(): KeyStoreStatus
    fun getAccountState(): AccountState

    suspend fun getPhrase(): ByteArray?
    fun getAccount(): Account?
    fun getAuthKey(): ByteArray?

    suspend fun configurePinCode(pinCode: String)
    suspend fun clearPinCode(phrase: ByteArray)
    fun verifyPinCode(pinCode: String): Boolean
    fun hasPinCode(): Boolean
    fun pinCodeNeedsUpgrade(): Boolean

    fun getToken(): String?
    fun putToken(token: String)

    fun getBdbJwt(): String?
    fun putBdbJwt(jwt: String, exp: Long)

    fun onActivityResult(requestCode: Int, resultCode: Int)
}

private const val POLL_ATTEMPTS_MAX = 15
private const val POLL_TIMEOUT_MS = 1000L

private const val PUT_PHRASE_RC = 400
private const val GET_PHRASE_RC = 401

private const val KEY_ACCOUNT = "account"
private const val KEY_AUTH_KEY = "authKey"
private const val KEY_CREATION_TIME = "creationTimeSeconds"
private const val KEY_TOKEN = "token"
private const val KEY_BDB_JWT = "bdbJwt"
private const val KEY_BDB_JWT_EXP = "bdbJwtExp"
private const val KEY_PIN_CODE = "pinCode"
private const val KEY_FAIL_COUNT = "failCount"
private const val KEY_FAIL_TIMESTAMP = "failTimestamp"
private const val KEY_ETH_PUB_KEY = "ethPublicKey"

@Suppress("TooManyFunctions")
class CryptoAccountManager(
    private val store: SharedPreferences,
    private val metaDataProvider: AccountMetaDataProvider
) : BRAccountManager {

    // TODO: Remove this once BRKeyStore auth screen logic is refactored
    private val context get() = BreadApp.getBreadContext()

    private val mutex = Mutex()

    private val resultChannel = Channel<Int>()

    private var token: String? = null
    private var jwt: String? = null
    private var jwtExp: Long? = null

    override suspend fun createAccount(phrase: ByteArray): Account {
        val creationDate = Date()
        val account = initAccount(phrase, creationDate)
        withContext(Dispatchers.IO) {
            metaDataProvider.create(creationDate)
        }
        return account
    }

    override suspend fun recoverAccount(phrase: ByteArray): Account {
        val apiKey = Key.createForBIP32ApiAuth(
            phrase,
            Key.getDefaultWordList()
        ).orNull()

        checkNotNull(apiKey) { "Failed to generate Api Key." }

        store.write { e ->
            e.putBytes(KEY_AUTH_KEY, apiKey.encodeAsPrivate())
        }
        val creationDate = recoverCreationDate()
        return initAccount(phrase, creationDate, apiKey)
    }

    override suspend fun migrateAccount() = mutex.withLock {
        // Migrate fields required for Account
        val phrase = checkNotNull(getPhrase()) { "Migration failed, no phrase found." }
        try {
            val creationDate = Date(BRKeyStore.getWalletCreationTime(context))
            val account =
                Account.createFromPhrase(phrase, creationDate, BRSharedPrefs.getDeviceId())
            val apiKey = Key.createForBIP32ApiAuth(
                phrase,
                Key.getDefaultWordList()
            ).orNull()

            checkNotNull(apiKey) { "Migration failed, failed to create Api Key." }

            writeAccount(account, apiKey, creationDate)
        } catch (e: Exception) {
            wipeAccount()
            return@withLock false
        }

        // Migrate other fields
        BRKeyStore.getToken(context)?.let { putToken(String(it)) }
        BRKeyStore.getBdbJwt(context)
            ?.let { putBdbJwt(String(it), BRKeyStore.getBdbJwtExp(context)) }
        BRKeyStore.getEthPublicKey(context)?.let { putEthPublicKey(it) }
        putPinCode(BRKeyStore.getPinCode(context))
        putFailCount(BRKeyStore.getFailCount(context))
        putFailTimestamp(BRKeyStore.getFailTimeStamp(context))

        BreadApp.applicationScope.launch(Dispatchers.IO) {
            BRKeyStore.wipeAfterMigration(context)
        }

        true
    }

    override fun isAccountInitialized() = getAccountState() != AccountState.Uninitialized

    override fun isMigrationRequired() =
        !isAccountInitialized() &&
            (BRKeyStore.getMasterPublicKey(context) != null || BRKeyStore.getAccount(context) != null)

    override fun getKeyStoreStatus(): KeyStoreStatus = BRKeyStore.getValidityStatus()

    override fun getAccountState(): AccountState {
        if (getAccount() == null) return AccountState.Uninitialized
        val failCount = getFailCount()
        val disabledUntil = disabledUntil(failCount, getFailTimestamp())
        if (failCount >= MAX_UNLOCK_ATTEMPTS && disabledUntil > BRSharedPrefs.getSecureTime()) {
            return AccountState.Disabled(disabledUntil)
        }
        return AccountState.Enabled
    }

    override suspend fun getPhrase(): ByteArray? =
        executeWithAuth { BRKeyStore.getPhrase(context, GET_PHRASE_RC) }

    override fun getAccount(): Account? =
        store.getBytes(KEY_ACCOUNT, null)?.run {
            Account.createFromSerialization(
                this,
                BRSharedPrefs.getDeviceId()
            ).orNull()
        }

    override fun getAuthKey() = store.getBytes(KEY_AUTH_KEY, null)

    override suspend fun configurePinCode(pinCode: String) {
        checkNotNull(getPhrase()) { "Phrase is null, cannot set pin code." }
        check(pinCode.length == PIN_LENGTH && pinCode.toIntOrNull() != null) {
            "Invalid pin code."
        }

        putPinCode(pinCode)
        putFailCount(0)
    }

    override suspend fun clearPinCode(phrase: ByteArray) {
        val storedPhrase = checkNotNull(getPhrase()) { "Phrase is null, cannot clear pin code." }
        check(phrase.contentEquals(storedPhrase)) { "Phrase does not match." }
        putPinCode("")
        putFailCount(0)
    }

    override fun verifyPinCode(pinCode: String) =
        if (pinCode == getPinCode()) {
            putFailCount(0)
            true
        } else {
            val failCount = getFailCount()
            putFailCount(failCount + 1)
            if (failCount >= MAX_UNLOCK_ATTEMPTS) {
                putFailTimestamp(System.currentTimeMillis())
            }
            false
        }

    override fun hasPinCode() = getPinCode().isNotBlank()

    override fun pinCodeNeedsUpgrade() =
        getPinCode().let { it.isNotBlank() && it.length != PIN_LENGTH }

    @Synchronized
    override fun getToken() = token ?: store.getString(KEY_TOKEN, null)

    @Synchronized
    override fun putToken(tokenVal: String) {
        store.write { e -> e.putString(KEY_TOKEN, tokenVal) }
        token = tokenVal
    }

    @Synchronized
    override fun getBdbJwt() =
        if ((getBdbJwtExp() - JWT_EXP_PADDING_MS) <= System.currentTimeMillis()) {
            null
        } else {
            jwt ?: store.getString(KEY_BDB_JWT, null)
        }

    @Synchronized
    override fun putBdbJwt(jwtVal: String, expVal: Long) {
        store.write { e ->
            e.putString(KEY_BDB_JWT, jwtVal)
            e.putLong(KEY_BDB_JWT_EXP, expVal)
        }
        jwt = jwtVal
        jwtExp = expVal
    }

    @Synchronized
    private fun getBdbJwtExp() = jwtExp ?: store.getLong(KEY_BDB_JWT_EXP, 0)

    /**
     * Used to determine address mismatch cases for tracking purposes, therefore exposed
     * as part of the implementation rather than the [BRAccountManager] interface.
     */
    fun getEthPublicKey(): ByteArray? = store.getBytes(KEY_ETH_PUB_KEY, null)

    private suspend fun initAccount(phrase: ByteArray, creationDate: Date, apiKey: Key? = null) =
        mutex.withLock {
            check(getPhrase() == null) { "Phrase already exists." }

            try {
                val storedPhrase = executeWithAuth {
                    BRKeyStore.putPhrase(phrase, context, PUT_PHRASE_RC)
                    BRKeyStore.getPhrase(context, GET_PHRASE_RC)
                }

                checkNotNull(storedPhrase) { "Phrase failed to persist." }

                val account = Account.createFromPhrase(
                    storedPhrase,
                    creationDate,
                    BRSharedPrefs.getDeviceId()
                )
                
                checkNotNull(account) { "Failed to create Account." }

                val apiKey = apiKey ?: Key.createForBIP32ApiAuth(
                    phrase,
                    Key.getDefaultWordList()
                ).orNull()

                checkNotNull(apiKey) { "Failed to create Api Key." }

                writeAccount(account, apiKey, creationDate)

                check(
                    validateAccount(
                        storedPhrase,
                        account,
                        apiKey,
                        creationDate.time
                    )
                ) { "Invalid wallet." }

                logInfo("Account created.")
                account
            } catch (e: Exception) {
                wipeAccount()
                throw e
            }
        }

    private fun getPinCode() = store.getString(KEY_PIN_CODE, "") ?: ""

    private fun putEthPublicKey(ethPubKey: ByteArray) =
        store.write { e -> e.putBytes(KEY_ETH_PUB_KEY, ethPubKey) }

    private fun getWalletCreationTime() = store.getLong(KEY_CREATION_TIME, 0L)

    private fun disabledUntil(failCount: Int, failTimestamp: Long): Long {
        val pow =
            PIN_LENGTH.toDouble().pow((failCount - MAX_UNLOCK_ATTEMPTS).toDouble()) * DateUtils.MINUTE_IN_MILLIS
        return (failTimestamp + pow).toLong()
    }

    private fun putPinCode(pinCode: String) {
        store.write { e -> e.putString(KEY_PIN_CODE, pinCode) }
    }

    private fun getFailCount() = store.getInt(KEY_FAIL_COUNT, 0)

    private fun putFailCount(count: Int) = store.write { e -> e.putInt(KEY_FAIL_COUNT, count) }

    private fun getFailTimestamp(): Long = store.getLong(KEY_FAIL_TIMESTAMP, 0)

    private fun putFailTimestamp(timestamp: Long) =
        store.write { e -> e.putLong(KEY_FAIL_TIMESTAMP, timestamp) }

    /**
     * Invokes [action], catching [UserNotAuthenticatedException].
     * If authentication is required, it will be attempted and upon
     * success will invoke [action] again.
     *
     * If authentication fails the original [UserNotAuthenticatedException]
     * will be thrown.
     */
    private suspend fun <T> executeWithAuth(action: () -> T): T {
        return withContext(Dispatchers.Main) {
            try {
                action()
            } catch (e: UserNotAuthenticatedException) {
                logInfo("Attempting authentication")

                when (resultChannel.receive()) {
                    Activity.RESULT_OK -> action()
                    else -> throw e
                }
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int) {
        check(!resultChannel.isClosedForSend)

        resultChannel.offer(
            when (requestCode) {
                PUT_PHRASE_RC -> resultCode
                GET_PHRASE_RC -> resultCode
                else -> return
            }
        )
    }

    private suspend fun recoverCreationDate(): Date {
        BreadApp.applicationScope.launch {
            metaDataProvider.recoverAll(true).first()
        }
        return metaDataProvider.walletInfo()
            .onStart {
                // Poll for wallet-info metadata
                // This is a work-around to avoid blocking until recoverAll(migrate)
                // recovers *all* metadata
                for (i in 1..POLL_ATTEMPTS_MAX) {
                    metaDataProvider.getWalletInfoUnsafe()
                        ?.let { emit(it) }
                    delay(POLL_TIMEOUT_MS)
                }
            }
            .first()
            .creationDate
            .run(::Date)
    }

    private fun writeAccount(
        account: Account,
        apiKey: Key,
        creationDate: Date
    ) {
        store.write { e ->
            e.putBytes(KEY_ACCOUNT, account.serialize())
            e.putBytes(KEY_AUTH_KEY, apiKey.encodeAsPrivate())
            e.putLong(KEY_CREATION_TIME, creationDate.time)
        }
    }

    private suspend fun validateAccount(
        phrase: ByteArray,
        account: Account,
        apiKey: Key,
        creationTime: Long
    ) = withContext(Dispatchers.IO) {
        Account.validatePhrase(phrase, Key.getDefaultWordList()) &&
            getAccount()?.serialize()?.contentEquals(account.serialize()) == true &&
            getAuthKey()?.contentEquals(apiKey.encodeAsPrivate()) == true &&
            getWalletCreationTime() == creationTime
    }

    private fun wipeAccount() {
        BRKeyStore.deletePhrase(context)
        store.write { e ->
            e.putString(KEY_ACCOUNT, null)
            e.putString(KEY_AUTH_KEY, null)
            e.putLong(KEY_CREATION_TIME, 0)
        }
    }
}

fun SharedPreferences.write(action: (SharedPreferences.Editor) -> Unit) {
    edit().apply {
        action(this)
    }.apply()
}

fun SharedPreferences.getBytes(key: String, defaultValue: ByteArray?): ByteArray? {
    val valStr = getString(key, null) ?: return defaultValue
    return hexDecode(valStr)
}

fun SharedPreferences.Editor.putBytes(key: String, value: ByteArray) {
    putString(key, hexEncode(value))
}

