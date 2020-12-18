/**
 * BreadWallet
 *
 * Created by Drew Carlson <drew.carlson@breadwallet.com> on 12/8/20.
 * Copyright (c) 2020 breadwallet LLC
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
package com.breadwallet.ui.uigift

import com.breadwallet.ui.uigift.ShareGift.M
import com.breadwallet.ui.uigift.ShareGift.E
import com.breadwallet.ui.uigift.ShareGift.F
import com.spotify.mobius.Next
import com.spotify.mobius.Update

object ShareGiftUpdate : Update<M, E, F> {
    override fun update(model: M, event: E): Next<M, F> {
        return when (event) {
            E.OnSendClicked -> onSendClicked(model)
        }
    }

    private fun onSendClicked(model: M): Next<M, F> =
        if (model.sharedImage) {
            Next.dispatch(setOf(F.GoBack))
        } else {
            Next.next(
                model.copy(sharedImage = false),
                setOf(
                    F.ExportGiftImage(
                        model.shareUrl,
                        model.recipientName,
                        model.pricePerUnit,
                        model.giftAmount,
                        model.giftAmountFiat
                    )
                )
            )
        }
}
