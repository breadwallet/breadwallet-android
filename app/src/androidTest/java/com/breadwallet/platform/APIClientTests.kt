/**
 * BreadWallet
 * <p/>
 * Created by Drew Carlson <drew.carlson@breadwallet.com> on 7/26/2019.
 * Copyright (c) 2019 breadwallet LLC
 * <p/>
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 * <p/>
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 * <p/>
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package com.breadwallet.platform

import android.support.test.InstrumentationRegistry
import android.support.test.runner.AndroidJUnit4

import com.breadwallet.tools.manager.BRApiManager
import com.breadwallet.tools.manager.BRSharedPrefs
import com.breadwallet.tools.security.BRKeyStore
import com.breadwallet.tools.util.BRConstants
import com.google.android.gms.common.util.Hex
import com.platform.APIClient
import com.platform.tools.TokenHolder

import org.junit.runner.RunWith

import java.io.IOException

import okhttp3.Request
import okhttp3.mockwebserver.Dispatcher
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import okhttp3.mockwebserver.RecordedRequest
import org.junit.*
import org.mockito.Mockito.*


@RunWith(AndroidJUnit4::class)
class APIClientTests {

    companion object {
        private const val AUTH_KEY = "4c33644e6a674136544e726b636d38354c48753358344c4c4c4d413263316e67626156577a32396d45434675633246335a68514e00"
        private const val TOKEN = "vyfzpvo1BkyzJ4IijOA5KveltxKu5xSDfLaYqO6YmDrL9DfGek3K1V0lNJ9WeJUXVt9XhF0E6irLUOgGWf1KCD4N6WLaa6TIcLByvc8ayapfFzNebGGHnumcIgcZLhcq"
        private const val TOKEN2 = "0yfzpvo1BkyzJ4IinOA5KveltxKu5xSDfLaYqO6YmDrL9DfGek3K1V0lNJ9WeJUXVt9XhF0E6irLUOgGWf1KCD4N6WLaa6TIcLByvc8ayapfFzNebGGHnumcIgcZLhcq"
    }

    private lateinit var server: MockWebServer

    @Before
    fun before() {
        server = MockWebServer()
        BRApiManager.getInstance().stopTimerTask()
    }

    @After
    fun after() {
        val context = InstrumentationRegistry.getContext()
        TokenHolder.reset()
        BRKeyStore.resetWalletKeyStore(context)
        BRSharedPrefs.clearAllPrefs(context)
        APIClient.setInstance(null)
        try {
            server.close()
        } catch (ignored: IOException) {
        }

    }

    fun startServer() {
        try {
            server.start()
        } catch (exception: IOException) {
            throw AssertionError("Failed to start mock server", exception)
        }

    }


    @Test
    fun testFetchesNewTokenWhenMissingFromUnauthorizedRequest() {
        val context = InstrumentationRegistry.getContext()
        // Mock HTTP interactions
        server.setDispatcher(object : Dispatcher() {
            override fun dispatch(request: RecordedRequest): MockResponse {
                // Return 401 with challenge for every request
                // Note this means the app will not be able to retrieve a new token
                return MockResponse()
                        .addHeader(BRConstants.HEADER_WWW_AUTHENTICATE, APIClient.BREAD)
                        .setResponseCode(401)
            }
        })
        startServer()
        val url = server.url("/")
        BRSharedPrefs.putDebugHost(host = "http://${url.host()}:${url.port()}")

        // Enter an authenticated state
        BRKeyStore.putAuthKey(Hex.stringToBytes(AUTH_KEY), context)

        // Note: we override the instance because TokenHolder
        //  may retrieve its own instance of APIClient.
        val apiClient = spy(APIClient(context))
        APIClient.setInstance(apiClient)

        // Simulate single API call with 401 response
        apiClient.sendRequest(Request.Builder()
                .url(url)
                .build(), true)

        val calls = inOrder(apiClient)
        // Attempt the request without a token
        calls.verify(apiClient, times(1)).sendHttpRequest(any(), eq(true), eq(null))
        // Fetch new token (here we test that it happens, not that it succeeds)
        calls.verify(apiClient, times(1)).sendHttpRequest(any(), eq(false), eq(null))
        // No new token, do not retry
        calls.verify(apiClient, never()).sendHttpRequest(any(), anyBoolean(), anyString())
    }

    @Test
    fun testFetchesNewTokenWhenExistingTokenIsInvalid() {
        val context = InstrumentationRegistry.getContext()
        // Mock HTTP interactions
        server.setDispatcher(object : Dispatcher() {
            override fun dispatch(request: RecordedRequest): MockResponse {
                // Return 401 with challenge for every request
                // Note this means the app will not be able to retrieve a new token
                return MockResponse()
                        .addHeader(BRConstants.HEADER_WWW_AUTHENTICATE, APIClient.BREAD)
                        .setResponseCode(401)
            }
        })
        startServer()
        val url = server.url("/")
        BRSharedPrefs.putDebugHost(host = "http://${url.host()}:${url.port()}")

        // Enter an authenticated state
        BRKeyStore.putAuthKey(Hex.stringToBytes(AUTH_KEY), context)
        BRKeyStore.putToken(TOKEN.toByteArray(), context)
        TokenHolder.retrieveToken(context)

        // Note: we override the instance because TokenHolder
        //  may retrieve its own instance of APIClient.
        val apiClient = spy(APIClient(context))
        APIClient.setInstance(apiClient)

        // Simulate single API call with 401 response
        apiClient.sendRequest(Request.Builder()
                .url(url)
                .build(), true)

        val calls = inOrder(apiClient)
        // Attempt the request once with an expired token
        calls.verify(apiClient, times(1)).sendHttpRequest(any(), eq(true), eq(TOKEN))
        // Fetch new token (here we test that it happens, not that it succeeds)
        calls.verify(apiClient, times(1)).sendHttpRequest(any(), eq(false), eq(null))
        // No new token, do not retry
        calls.verify(apiClient, never()).sendHttpRequest(any(), anyBoolean(), anyString())
    }

    @Test
    fun testRequestCompletesAfterRetryingUnauthorizedRequest() {
        val context = InstrumentationRegistry.getContext()

        // Mock HTTP interactions
        server.setDispatcher(object : Dispatcher() {
            private var callCount = 0
            override fun dispatch(
                    request: RecordedRequest
            ) = when {
                // Provide a new token when requested
                request.path == "/token" ->
                    MockResponse()
                            .setResponseCode(200)
                            .setBody("{\"token\": \"$TOKEN2\"}")
                // First call fails (bad token)
                callCount == 0 -> {
                    callCount += 1
                    MockResponse()
                            .addHeader(BRConstants.HEADER_WWW_AUTHENTICATE, APIClient.BREAD)
                            .setResponseCode(401)
                }
                // Next call succeeds as expected by the initial request
                callCount == 1 -> {
                    callCount += 1
                    MockResponse()
                            .setResponseCode(200)
                }
                else -> throw AssertionError("Unhandled request.")
            }
        })
        startServer()
        val url = server.url("/")
        BRSharedPrefs.putDebugHost(host = "http://${url.host()}:${url.port()}")

        // Enter an authenticated state
        BRKeyStore.putAuthKey(Hex.stringToBytes(AUTH_KEY), context)
        BRKeyStore.putToken(TOKEN.toByteArray(), context)
        TokenHolder.retrieveToken(context)

        // Note: we override the instance because TokenHolder
        //  may retrieve its own instance of APIClient.
        val apiClient = spy(APIClient(context))
        APIClient.setInstance(apiClient)

        // Simulate single API call with 401 response
        apiClient.sendRequest(Request.Builder()
                .url(url)
                .build(), true)

        val calls = inOrder(apiClient)
        // Fail the first request
        calls.verify(apiClient, times(1)).sendHttpRequest(any(), eq(true), eq(TOKEN))
        // Fetch new token
        calls.verify(apiClient, times(1)).sendHttpRequest(any(), eq(false), eq(null))
        // Retry initial request with new token
        calls.verify(apiClient, times(1)).sendHttpRequest(any(), eq(true), eq(TOKEN2))
    }
}
