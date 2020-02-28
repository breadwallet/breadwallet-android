/**
 * BreadWallet
 *
 * Created by Drew Carlson <drew.carlson@breadwallet.com> on 1/7/20.
 * Copyright (c) 2020 breadwallet LLC
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
package com.breadwallet.breadbox

import android.content.Context
import android.util.Base64
import com.breadwallet.BuildConfig
import com.breadwallet.crypto.Key
import com.breadwallet.logger.logError
import com.breadwallet.tools.crypto.CryptoHelper
import com.breadwallet.tools.crypto.CryptoHelper.sha256
import com.breadwallet.tools.crypto.CryptoHelper.signBasicDer
import com.breadwallet.tools.crypto.CryptoHelper.signJose
import com.breadwallet.tools.manager.BRSharedPrefs
import com.breadwallet.tools.security.BRKeyStore
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import okhttp3.Interceptor
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import org.json.JSONException
import org.json.JSONObject
import java.io.IOException
import java.nio.charset.Charset
import java.util.Date
import java.util.concurrent.TimeUnit

private const val JWT_EXP_DAYS = 7L
private const val MAX_TOKEN_RETRIES = 3
private const val JWT_EXP_PADDING_MS = 10_000L

class BdbAuthInterceptor(
    private val context: Context,
    private val httpClient: OkHttpClient
) : Interceptor {

    private val mutex = Mutex()

    private val jwtHeader = JSONObject()
        .put("alg", "ES256")
        .put("typ", "JWT")
        .toString()
        .jwtEncode()

    private val authKeyBytes by lazy {
        checkNotNull(BRKeyStore.getAuthKey(context)) {
            "BdbAuthInterceptor created before API Auth Key was set."
        }
    }

    private val authKey by lazy {
        checkNotNull(Key.createFromPrivateKeyString(authKeyBytes).orNull()) {
            "Failed to create Key from Auth Key Bytes"
        }
    }

    @Volatile
    private var jwtString: String? = BRKeyStore.getBdbJwt(context)?.toString(Charset.forName("UTF-8"))

    @Volatile
    private var jwtExpiration: Long = BRKeyStore.getBdbJwtExp(context)

    private val tokenRequestSignature by lazy {
        val signingData = checkNotNull(sha256(BuildConfig.BDB_CLIENT_TOKEN.toByteArray()))
        signBasicDer(signingData, authKey).base64EncodedString()
    }

    private val publicKeyString by lazy {
        String(authKey.encodeAsPublic())
            .run(CryptoHelper::hexDecode)
            .base64EncodedString()
    }

    override fun intercept(chain: Interceptor.Chain): Response {
        if (!chain.request().url.host.endsWith("blockset.com")) {
            return chain.proceed(chain.request())
        }

        val tokenString = runBlocking {
            if (jwtString == null) createAndSetJwt()

            if ((jwtExpiration - JWT_EXP_PADDING_MS) <= System.currentTimeMillis()) {
                // Expired, cleanup and create new token
                jwtString = null
                jwtExpiration = 0
                createAndSetJwt()
            }

            // Fallback to client token if needed, try creating a token later
            if (jwtString == null) {
                BuildConfig.BDB_CLIENT_TOKEN
            } else {
                jwtString
            }
        }

        return chain.request()
            .newBuilder()
            .addHeader("Authorization", "Bearer $tokenString")
            .build()
            .run(chain::proceed)
    }

    private suspend fun createAndSetJwt() {
        val (newJwt, expiration) = try {
            createAccountJwt()
        } catch (e: IOException) {
            logError("Failed to create JWT", e)
            return
        }
        if (newJwt != null) {
            jwtString = newJwt
            jwtExpiration = expiration
            BRKeyStore.putBdbJwt(newJwt.toByteArray(), context)
            BRKeyStore.putBdbJwtExp(expiration, context)
        }
    }

    private suspend fun createAccountJwt(): Pair<String?, Long> = mutex.withLock {
        // Lock acquired after successful jwt creation
        if (jwtString != null) return@withLock null to 0

        val (token, clientId) = requestToken() ?: return@withLock null to 0

        // The JWT deals with exp in seconds, but we will use millis everywhere else.
        // Remove 5 second from our current time to ensure the issued at time is not
        // in the future.
        val now = TimeUnit.MILLISECONDS.toSeconds(Date().time) - 5
        val expiration = now + TimeUnit.DAYS.toSeconds(JWT_EXP_DAYS)

        val jwtBody = JSONObject()
            .put("sub", token)
            .put("iat", now)
            .put("exp", expiration)
            .put("brd:ct", "usr")
            .put("brd:cli", clientId)
            .toString()
            .jwtEncode()

        val signingData = "$jwtHeader.$jwtBody".toByteArray()

        val hashedData = checkNotNull(sha256(signingData))
        val jwtSignature = signJose(hashedData, authKey).jwtEncode()
        "$jwtHeader.$jwtBody.$jwtSignature" to TimeUnit.SECONDS.toMillis(expiration)
    }

    private fun requestToken(attempt: Int = 0): Pair<String, String>? {
        val request = Request.Builder()
            .addHeader("Authorization", "Bearer ${BuildConfig.BDB_CLIENT_TOKEN}")
            .url("https://api.blockset.com/users/token")
            .method(
                "POST",
                JSONObject()
                    .put("device_id", BRSharedPrefs.getDeviceId())
                    .put("pub_key", publicKeyString)
                    .put("signature", tokenRequestSignature)
                    .toString()
                    .toRequestBody("application/json".toMediaType())
            )
            .build()

        val response = try {
            httpClient.newCall(request).execute()
        } catch (e: IOException) {
            return if (attempt == MAX_TOKEN_RETRIES) {
                logError("Failed to create account token.", e)
                throw e
            } else {
                requestToken(attempt + 1)
            }
        }

        val body = if (response.isSuccessful) {
            val bodyString = checkNotNull(response.body).string()
            try {
                JSONObject(bodyString)
            } catch (e: JSONException) {
                logError("Failed to parse token response body", e)
                null
            }
        } else {
            logError("Token request failed at API with status '${response.code}'")
            null
        }

        return body?.run {
            getString("token") to getString("client_token")
        }
    }

    private fun String.jwtEncode() =
        base64EncodedString().base64ToBase64URL()

    private fun ByteArray.jwtEncode() =
        base64EncodedString().base64ToBase64URL()

    private fun String.base64EncodedString() =
        Base64.encodeToString(toByteArray(), Base64.NO_WRAP)

    private fun ByteArray.base64EncodedString() =
        Base64.encodeToString(this, Base64.NO_WRAP)

    private fun String.base64ToBase64URL() =
        replace("+", "-")
            .replace("/", "_")
            .replace("=", "")
}
