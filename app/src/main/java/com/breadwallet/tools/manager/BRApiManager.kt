package com.breadwallet.tools.manager

import android.content.Context
import com.breadwallet.logger.logDebug
import com.breadwallet.logger.logError
import com.breadwallet.tools.util.TokenUtil
import kotlinx.coroutines.Dispatchers.IO
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.transformLatest
import kotlinx.coroutines.invoke

/** Debounce timeout to prevent excessive api requests when System boots. */
private const val RATE_UPDATE_DEBOUNCE_MS = 500L

/** Delay between each data request. */
private const val REFRESH_DELAY_MS = 60_000L

/** Updates the token and exchange rate data for the current list of currency codes. */
@UseExperimental(ExperimentalCoroutinesApi::class, FlowPreview::class)
fun Flow<List<String>>.updateRatesForCurrencies(
    context: Context
): Flow<Unit> =
    onStart {
        // Load token items from disk
        IO { TokenUtil.getTokenItems(context) }
        IO {
            BRApiManager.getInstance()
                .updateFiatRates(context)
        }
    }
    .filter { it.isNotEmpty() }
    // Prevent System boot from spamming api calls
    .debounce(RATE_UPDATE_DEBOUNCE_MS)
    // Currency APIs expect uppercase currency codes
    .map { it.map(String::toUpperCase) }
    // Repeat the latest list every 60 seconds
    .transformLatest { codes ->
        while (true) {
            emit(codes)
            delay(REFRESH_DELAY_MS)
        }
    }
    // Load data in parallel
    .map { codes ->
        logDebug("Updating currency and rate data", codes)
        BRApiManager.getInstance().apply {
            IO { updateFiatRates(context) }
            IO { updateCryptoData(context, codes) }
            IO { fetchPriceChanges(context, codes) }
        }
        Unit
    }
    // Log errors but do not stop collecting
    .catch { e ->
        logError("Failed to update currency and rate data", e)
    }
