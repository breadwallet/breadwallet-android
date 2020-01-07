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
import org.json.JSONObject
import java.util.Date
import java.util.concurrent.TimeUnit

private const val JWT_EXP_DAYS = 7L
private const val MAX_TOKEN_RETRIES = 3
private const val JWT_EXP_PADDING_SECONDS = 10L

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

    private val authKeyBytes = checkNotNull(BRKeyStore.getAuthKey(context)) {
        "BdbAuthInterceptor created before API Auth Key was set."
    }

    private val authKey = checkNotNull(Key.createFromPrivateKeyString(authKeyBytes).orNull()) {
        "Failed to create Key from Auth Key Bytes"
    }

    @Volatile
    private var jwtString: String? = BRKeyStore.getBdbJwt(context)?.contentToString()

    @Volatile
    private var jwtExpiration: Long = BRKeyStore.getBdbJwtExp(context)

    private val tokenRequestSignature by lazy {
        val signingData = checkNotNull(sha256(BuildConfig.BDB_CLIENT_TOKEN.toByteArray()))
        signBasicDer(signingData, authKey).base64EncodedString()
    }

    private val publicKeyString = String(authKey.encodeAsPublic())
        .run(CryptoHelper::hexDecode)
        .base64EncodedString()

    override fun intercept(chain: Interceptor.Chain): Response = runBlocking {
        if (jwtString == null) createAndSetJwt()

        val currentSeconds = TimeUnit.MILLISECONDS.toSeconds(System.currentTimeMillis())
        if ((jwtExpiration - JWT_EXP_PADDING_SECONDS) < currentSeconds) {
            // Expired, cleanup and create new token
            jwtString = null
            jwtExpiration = 0
            createAndSetJwt()
        }

        // Fallback to client token if needed, try creating a token later
        val tokenString = if (jwtString == null) {
            BuildConfig.BDB_CLIENT_TOKEN
        } else {
            jwtString
        }

        chain.request()
            .newBuilder()
            .addHeader("Authorization", "Bearer $tokenString")
            .build()
            .run(chain::proceed)
    }

    private suspend fun createAndSetJwt() {
        val (newJwt, expiration) = createAccountJwt()
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

        val (token, clientId) = requestToken() ?: error("Failed to create Account JWT.")

        // The JWT deals with exp in seconds, but we will use millis everywhere else.
        val now = TimeUnit.MILLISECONDS.toSeconds(Date().time)
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
        } catch (e: Exception) {
            return if (attempt == MAX_TOKEN_RETRIES) {
                logError("Failed to create account token.", e)
                null
            } else {
                requestToken(attempt + 1)
            }
        }

        return JSONObject(checkNotNull(response.body).string()).run {
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
