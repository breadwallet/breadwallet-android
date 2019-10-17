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

import com.spotify.mobius.Next
import com.spotify.mobius.Next.dispatch
import com.spotify.mobius.Next.next
import com.spotify.mobius.Next.noChange
import com.spotify.mobius.Update

object ReceiveUpdate : Update<ReceiveModel, ReceiveEvent, ReceiveEffect>,
    ReceiveUpdateSpec {
    override fun update(
        model: ReceiveModel,
        event: ReceiveEvent
    ): Next<ReceiveModel, ReceiveEffect> = patch(model, event)

    override fun onReceiveAddressUpdated(
        model: ReceiveModel,
        event: ReceiveEvent.OnReceiveAddressUpdated
    ): Next<ReceiveModel, ReceiveEffect> =
        next(
            model.copy(
                receiveAddress = event.address,
                sanitizedAddress = event.sanitizedAddress
            )
        )

    override fun onCloseClicked(model: ReceiveModel): Next<ReceiveModel, ReceiveEffect> =
        dispatch(setOf(ReceiveEffect.CloseSheet))

    override fun onFaqClicked(model: ReceiveModel): Next<ReceiveModel, ReceiveEffect> =
        dispatch(setOf(ReceiveEffect.GoToFaq(model.currencyCode)))

    override fun onCopyAddressClicked(model: ReceiveModel): Next<ReceiveModel, ReceiveEffect> =
        next(
            model.copy(isDisplayingCopyMessage = true),
            setOf(
                ReceiveEffect.CopyAddressToClipboard(model.receiveAddress),
                ReceiveEffect.ResetCopiedAfterDelay
            )
        )

    override fun onShareClicked(model: ReceiveModel): Next<ReceiveModel, ReceiveEffect> =
        dispatch(setOf(ReceiveEffect.ShareRequest(model.receiveAddress, model.walletName)))

    override fun onHideCopyMessage(model: ReceiveModel): Next<ReceiveModel, ReceiveEffect> =
        next(model.copy(isDisplayingCopyMessage = false))

    override fun onWalletNameUpdated(
        model: ReceiveModel,
        event: ReceiveEvent.OnWalletNameUpdated
    ): Next<ReceiveModel, ReceiveEffect> =
        next(model.copy(walletName = event.walletName))

    override fun onRequestAmountClicked(model: ReceiveModel): Next<ReceiveModel, ReceiveEffect> {
        return noChange()
    }
}
