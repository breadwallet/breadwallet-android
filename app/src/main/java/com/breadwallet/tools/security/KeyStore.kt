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
import android.security.keystore.UserNotAuthenticatedException
import com.breadwallet.app.BreadApp
import com.breadwallet.crypto.Account
import com.breadwallet.logger.logInfo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.withContext

/** An encrypted object store for data required to manage a wallet. */
interface KeyStore {

    /** We intentionally expose [DefaultKeyStore] for Activity result handling. */
    @Deprecated("Inject KeyStore via Kodein.")
    companion object : DefaultKeyStore()

    suspend fun putPhrase(phrase: ByteArray)
    suspend fun getPhrase(): ByteArray?

    suspend fun putAccount(account: Account)
    suspend fun getAccount(): Account?

    suspend fun putAuthKey(authKey: ByteArray)
    suspend fun getAuthKey(): ByteArray?

    suspend fun putWalletCreationTime(creationTime: Long)
    suspend fun getWalletCreationTime(): Long?
}

@UseExperimental(ExperimentalCoroutinesApi::class)
abstract class DefaultKeyStore : KeyStore {

    companion object {
        private const val PUT_PHRASE_RC = 400
        private const val GET_PHRASE_RC = 401
    }

    private val context get() = BreadApp.getBreadContext()

    private val resultChannel = Channel<Int>()

    override suspend fun putPhrase(phrase: ByteArray): Unit =
        executeWithAuth { BRKeyStore.putPhrase(phrase, context, PUT_PHRASE_RC) }

    override suspend fun getPhrase(): ByteArray? =
        executeWithAuth { BRKeyStore.getPhrase(context, GET_PHRASE_RC) }

    override suspend fun putAccount(account: Account) {
        withContext(Dispatchers.IO) {
            BRKeyStore.putAccount(account, context)
        }
    }

    override suspend fun getAccount(): Account? {
        return withContext(Dispatchers.IO) {
            BRKeyStore.getAccount(context)
        }
    }

    override suspend fun putAuthKey(authKey: ByteArray) {
        withContext(Dispatchers.IO) {
            BRKeyStore.putAuthKey(authKey, context)
        }
    }

    override suspend fun getAuthKey(): ByteArray? {
        return withContext(Dispatchers.IO) {
            BRKeyStore.getAuthKey(context)
        }
    }

    override suspend fun putWalletCreationTime(creationTime: Long) {
        withContext(Dispatchers.IO) {
            BRKeyStore.putWalletCreationTime(creationTime.toLong(), context)
        }
    }

    override suspend fun getWalletCreationTime(): Long? {
        return withContext(Dispatchers.IO) {
            BRKeyStore.getWalletCreationTime(context).toLong()
        }
    }

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

    fun onActivityResult(requestCode: Int, resultCode: Int) {
        check(!resultChannel.isClosedForSend)

        resultChannel.offer(
            when (requestCode) {
                PUT_PHRASE_RC -> resultCode
                GET_PHRASE_RC -> resultCode
                else -> return
            }
        )
    }
}
