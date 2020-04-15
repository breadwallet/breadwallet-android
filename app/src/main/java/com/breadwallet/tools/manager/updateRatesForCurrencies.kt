/**
 * BreadWallet
 *
 * Created by Drew Carlson <drew.carlson@breadwallet.com> on 10/26/19.
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
package com.breadwallet.tools.manager

import android.content.Context
import com.breadwallet.logger.logDebug
import com.breadwallet.logger.logError
import com.breadwallet.tools.util.TokenUtil
import kotlinx.coroutines.Dispatchers.IO
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.transformLatest
import kotlinx.coroutines.invoke
import java.util.Locale

/** Delay between each data request. */
private const val REFRESH_DELAY_MS = 60_000L

/** Updates the token and exchange rate data for the current list of currency ids. */
fun Flow<List<String>>.updateRatesForCurrencies(
    context: Context
): Flow<Unit> =
    onStart {
        // Read the initial token list from disk
        IO { TokenUtil.getTokenItems(context) }
        // Initial sync of btc-fiat rates
        BRApiManager.getInstance()
            .updateFiatRates(context)
        emitAll(callbackFlow {
            // Fetch an updated token list from the server
            TokenUtil.fetchTokensFromServer(context)
            // Emit the current list again to ensure we update
            // the currency code for the given id. (DAI to SAI)
            offer(first())
            close()
        })
    }
        .filter { it.isNotEmpty() }
        // Currency APIs expect uppercase currency codes
        .map { currencyIds ->
            currencyIds.mapNotNull { id ->
                TokenUtil.getTokenItemForCurrencyId(id)
                    ?.symbol
                    ?.toUpperCase(Locale.ROOT)
            }
        }
        .distinctUntilChanged { old, new ->
            old.size == new.size && old.containsAll(new)
        }
        // Repeat the latest list every 60 seconds
        .transformLatest { codes ->
            while (true) {
                emit(codes)
                delay(REFRESH_DELAY_MS)
            }
        }
        .transformLatest { codes ->
            emit(codes)
            emitAll(BRSharedPrefs.preferredFiatIsoChanges().map { codes })
        }
        // Load data in parallel
        .map { codes ->
            logDebug("Updating currency and rate data", codes)
            BRApiManager.getInstance().apply {
                updateFiatRates(context)
                IO { updateCryptoData(context, codes) }
                IO { fetchPriceChanges(context, codes) }
            }
            Unit
        }
        // Log errors but do not stop collecting
        .catch { e ->
            logError("Failed to update currency and rate data", e)
        }
