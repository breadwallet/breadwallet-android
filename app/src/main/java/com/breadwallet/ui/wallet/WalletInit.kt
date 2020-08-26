/**
 * BreadWallet
 *
 * Created by Ahsan Butt <ahsan.butt@breadwallet.com> on 7/26/19.
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
package com.breadwallet.ui.wallet

import com.breadwallet.tools.util.EventUtils
import com.breadwallet.ui.wallet.WalletScreen.F
import com.spotify.mobius.Effects.effects
import com.spotify.mobius.First.first
import com.spotify.mobius.Init

val WalletInit = Init<WalletScreen.M, F> { model ->
    val effects = effects(
        F.LoadWalletState(model.currencyCode),
        F.LoadWalletBalance(model.currencyCode),
        F.LoadFiatPricePerUnit(model.currencyCode),
        F.LoadCryptoPreferred,
        F.LoadCurrencyName(model.currencyCode),
        F.LoadSyncState(model.currencyCode),
        F.LoadChartInterval(model.priceChartInterval, model.currencyCode),
        F.LoadMarketData(model.currencyCode),
        F.TrackEvent(
            String.format(
                EventUtils.EVENT_WALLET_APPEARED,
                model.currencyCode
            )
        ),
        F.LoadIsTokenSupported(model.currencyCode),
        F.LoadTransactions(model.currencyCode)
    )
    first(model, effects)
}
