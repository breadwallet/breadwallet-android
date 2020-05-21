/**
 * BreadWallet
 *
 * Created by Pablo Budelli <pablo.budelli@breadwallet.com> on 11/05/19.
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
package com.breadwallet.ui.settings.segwit

import android.content.Context
import com.breadwallet.breadbox.BreadBox
import com.breadwallet.crypto.AddressScheme
import com.breadwallet.tools.manager.BRSharedPrefs
import com.breadwallet.ui.settings.segwit.EnableSegWit.E
import com.breadwallet.ui.settings.segwit.EnableSegWit.F
import com.breadwallet.util.isBitcoin
import com.breadwallet.util.usermetrics.UserMetricsUtil
import drewcarlson.mobius.flow.subtypeEffectHandler
import kotlinx.coroutines.flow.first

fun createSegWitHandler(
    context: Context,
    breadBox: BreadBox
) = subtypeEffectHandler<F, E> {
    addAction<F.EnableSegWit> {
        BRSharedPrefs.putIsSegwitEnabled(true)
        UserMetricsUtil.logSegwitEvent(context, UserMetricsUtil.ENABLE_SEG_WIT)
        breadBox.system()
            .first()
            .walletManagers
            .find { it.currency.code.isBitcoin() }
            ?.addressScheme = AddressScheme.BTC_SEGWIT
    }
}
