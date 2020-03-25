/**
 * BreadWallet
 *
 * Created by Drew Carlson <drew.carlson@breadwallet.com> on 9/17/19.
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
package com.breadwallet.ui.txdetails

import android.content.Context
import com.breadwallet.logger.logError
import com.breadwallet.repository.RatesRepository
import com.breadwallet.ui.txdetails.TxDetails.E
import com.breadwallet.ui.txdetails.TxDetails.F
import com.breadwallet.util.CurrencyCode
import com.spotify.mobius.Connection
import com.spotify.mobius.functions.Consumer
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.math.BigDecimal

class TxDetailsHandler(
    private val output: Consumer<E>,
    private val context: Context
) : Connection<F>, CoroutineScope {

    companion object {
        private const val RATE_UPDATE_MS = 60_000L
    }

    override val coroutineContext =
        SupervisorJob() + Dispatchers.Default +
            CoroutineExceptionHandler { _, throwable ->
                logError("Error in coroutine", throwable)
            }

    override fun accept(effect: F) {
        when (effect) {
            is F.LoadFiatAmountNow ->
                loadFiatAmountNow(
                    effect.cryptoTransferredAmount,
                    effect.currencyCode,
                    effect.preferredFiatIso
                )
        }
    }

    private fun loadFiatAmountNow(
        cryptoAmount: BigDecimal,
        currencyCode: CurrencyCode,
        fiatIso: String
    ) {
        launch {
            while (isActive) {
                RatesRepository.getInstance(context).getFiatForCrypto(
                    cryptoAmount,
                    currencyCode,
                    fiatIso
                )?.run {
                    output.accept(E.OnFiatAmountNowUpdated(this))
                }

                delay(RATE_UPDATE_MS)
            }
        }
    }

    override fun dispose() {
        coroutineContext.cancel()
    }
}
