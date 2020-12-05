/**
 * BreadWallet
 *
 * Created by Drew Carlson <drew.carlson@breadwallet.com> on 10/15/19.
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
package com.breadwallet.ui.receive

import android.content.Context
import com.breadwallet.breadbox.BreadBox
import com.breadwallet.breadbox.isBitcoin
import com.breadwallet.breadbox.toSanitizedString
import com.breadwallet.crypto.AddressScheme
import com.breadwallet.repository.RatesRepository
import com.breadwallet.tools.manager.BRClipboardManager
import com.breadwallet.tools.manager.BRSharedPrefs
import com.breadwallet.tools.util.EventUtils
import com.breadwallet.ui.receive.ReceiveScreen.E
import com.breadwallet.ui.receive.ReceiveScreen.F
import drewcarlson.mobius.flow.subtypeEffectHandler
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.transformLatest
import kotlinx.coroutines.invoke
import java.math.BigDecimal

private const val RATE_UPDATE_MS = 60_000L

fun createReceiveHandler(
    context: Context,
    breadBox: BreadBox
) = subtypeEffectHandler<F, E> {
    addConsumer<F.CopyAddressToClipboard> { effect ->
        Dispatchers.Main {
            BRClipboardManager.putClipboard(effect.address)
        }
        EventUtils.pushEvent(EventUtils.EVENT_RECEIVE_COPIED_ADDRESS)
    }

    addTransformer<F.LoadExchangeRate> { effects ->
        val rates = RatesRepository.getInstance(context)
        val fiatCode = BRSharedPrefs.getPreferredFiatIso()
        effects.transformLatest { effect ->
            while (true) {
                val fiatRate = rates.getFiatForCrypto(BigDecimal.ONE, effect.currencyCode, fiatCode)

                emit(E.OnExchangeRateUpdated(fiatRate ?: BigDecimal.ZERO))

                // TODO: Display out of date, invalid (0) rate, etc.
                delay(RATE_UPDATE_MS)
            }
        }
    }

    addTransformer<F.LoadWalletInfo> { effects ->
        effects.flatMapLatest { effect ->
            breadBox.wallet(effect.currencyCode).map { wallet ->
                val receiveAddress = if (wallet.currency.isBitcoin()) {
                    wallet.getTargetForScheme(
                        when (BRSharedPrefs.getIsSegwitEnabled()) {
                            true -> AddressScheme.BTC_SEGWIT
                            false -> AddressScheme.BTC_LEGACY
                        }
                    )
                } else {
                    wallet.target
                }
                E.OnWalletInfoLoaded(
                    walletName = wallet.currency.name,
                    address = receiveAddress.toString(),
                    sanitizedAddress = receiveAddress.toSanitizedString()
                )
            }
        }
    }
}
