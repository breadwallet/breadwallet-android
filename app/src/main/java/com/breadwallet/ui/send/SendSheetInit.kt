/**
 * BreadWallet
 *
 * Created by Drew Carlson <drew.carlson@breadwallet.com> on 8/13/19.
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
package com.breadwallet.ui.send

import com.breadwallet.ui.send.SendSheet.F
import com.breadwallet.ui.send.SendSheet.M
import com.spotify.mobius.First
import com.spotify.mobius.Init


object SendSheetInit : Init<M, F> {
    override fun init(model: M): First<M, F> {
        val effects = mutableSetOf<F>()

        if (model.targetAddress.isNotBlank()) {
            effects.add(
                F.ValidateAddress(
                model.currencyCode,
                model.targetAddress
            ))
        }

        var isPaymentProtocolRequest = false
        if (!model.cryptoRequestUrl?.rUrlParam.isNullOrBlank()) {
            effects.add(F.PaymentProtocol.LoadPaymentData(model.cryptoRequestUrl!!))
            isPaymentProtocolRequest = true
        }

        return First.first(
            model.copy(isFetchingPayment = isPaymentProtocolRequest),
            effects + setOf(
                F.LoadBalance(model.currencyCode),
                F.LoadExchangeRate(model.currencyCode, model.fiatCode),
                F.LoadAuthenticationSettings,
                F.GetTransferFields(model.currencyCode, model.targetAddress)
            )
        )
    }
}
