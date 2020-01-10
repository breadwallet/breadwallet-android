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

import com.breadwallet.crypto.Transfer
import com.platform.entities.TxMetaData
import io.hypno.switchboard.MobiusUpdateSpec
import java.math.BigDecimal

@MobiusUpdateSpec(
    baseModel = TxDetailsModel::class,
    baseEffect = TxDetailsEffect::class
)

sealed class TxDetailsEvent {
    data class OnTransactionUpdated(
        val transaction: Transfer,
        val gasPrice: BigDecimal,
        val gasLimit: BigDecimal
    ) : TxDetailsEvent()

    data class OnFiatAmountNowUpdated(val fiatAmountNow: BigDecimal) : TxDetailsEvent()
    data class OnMetaDataUpdated(val metaData: TxMetaData) : TxDetailsEvent()
    data class OnMemoChanged(val memo: String) : TxDetailsEvent()
    object OnClosedClicked : TxDetailsEvent()
    object OnShowHideDetailsClicked : TxDetailsEvent()
}
