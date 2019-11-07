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

import com.breadwallet.tools.util.BRConstants
import com.breadwallet.ui.receive.ReceiveEvent.OnAmountChange.AddDecimal
import com.breadwallet.ui.receive.ReceiveEvent.OnAmountChange.AddDigit
import com.breadwallet.ui.receive.ReceiveEvent.OnAmountChange.Delete
import com.breadwallet.ui.send.MAX_DIGITS
import com.spotify.mobius.Next
import com.spotify.mobius.Next.dispatch
import com.spotify.mobius.Next.next
import com.spotify.mobius.Next.noChange
import com.spotify.mobius.Update
import java.math.BigDecimal

@Suppress("TooManyFunctions")
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
        dispatch(
            setOf(
                ReceiveEffect.ShareRequest(
                    model.receiveAddress,
                    model.amount,
                    model.walletName
                )
            )
        )

    override fun onHideCopyMessage(model: ReceiveModel): Next<ReceiveModel, ReceiveEffect> =
        next(model.copy(isDisplayingCopyMessage = false))

    override fun onWalletNameUpdated(
        model: ReceiveModel,
        event: ReceiveEvent.OnWalletNameUpdated
    ): Next<ReceiveModel, ReceiveEffect> =
        next(model.copy(walletName = event.walletName))

    override fun onAmountClicked(model: ReceiveModel): Next<ReceiveModel, ReceiveEffect> {
        return next(
            model.copy(
                isAmountEditVisible = !model.isAmountEditVisible
            )
        )
    }

    override fun onAmountChange(
        model: ReceiveModel,
        event: ReceiveEvent.OnAmountChange
    ): Next<ReceiveModel, ReceiveEffect> {
        return when (event) {
            AddDecimal -> addDecimal(model)
            Delete -> delete(model)
            is AddDigit -> addDigit(model, event)
        }
    }

    private fun addDecimal(
        model: ReceiveModel
    ): Next<ReceiveModel, ReceiveEffect> {
        return when {
            model.rawAmount.contains('.') -> noChange()
            model.rawAmount.isEmpty() -> next(
                model.copy(rawAmount = "0.")
            )
            else -> next(
                model.copy(
                    rawAmount = model.rawAmount + '.'
                )
            )
        }
    }

    private fun delete(
        model: ReceiveModel
    ): Next<ReceiveModel, ReceiveEffect> {
        return when {
            model.rawAmount.isEmpty() -> noChange()
            else -> next(
                model.withNewRawAmount(model.rawAmount.dropLast(1))
            )
        }
    }

    private fun addDigit(
        model: ReceiveModel,
        event: AddDigit
    ): Next<ReceiveModel, ReceiveEffect> {
        return when {
            model.rawAmount == "0" && event.digit == 0 -> noChange()
            model.rawAmount.split('.').run {
                // Ensure the length of the main or fraction
                // if present are less than MAX_DIGITS
                if (size == 1) first().length == MAX_DIGITS
                else getOrNull(1)?.length == MAX_DIGITS
            } -> noChange()
            else -> {
                val newRawAmount = when (model.rawAmount) {
                    "0" -> event.digit.toString()
                    else -> model.rawAmount + event.digit
                }
                next(model.withNewRawAmount(newRawAmount))
            }
        }
    }

    override fun onToggleCurrencyClicked(model: ReceiveModel): Next<ReceiveModel, ReceiveEffect> {
        val isAmountCrypto = !model.isAmountCrypto
        if (model.rawAmount.isBlank()) {
            return next(model.copy(isAmountCrypto = isAmountCrypto))
        }
        return next(
            model.copy(
                isAmountCrypto = isAmountCrypto,
                rawAmount = when {
                    isAmountCrypto -> model.amount.toPlainString()
                    else -> model.fiatAmount
                        .setScale(2, BRConstants.ROUNDING_MODE)
                        .toPlainString()
                }
            )
        )
    }

    override fun onExchangeRateUpdated(
        model: ReceiveModel,
        event: ReceiveEvent.OnExchangeRateUpdated
    ): Next<ReceiveModel, ReceiveEffect> {
        val pricePerUnit = event.fiatPricePerUnit
        val newAmount: BigDecimal
        val newFiatAmount: BigDecimal

        if (model.isAmountCrypto) {
            newAmount = model.amount
            newFiatAmount = if (pricePerUnit > BigDecimal.ZERO) {
                model.amount.setScale(2, BRConstants.ROUNDING_MODE) * pricePerUnit
            } else {
                model.fiatAmount
            }
        } else {
            newFiatAmount = model.fiatAmount
            newAmount = (model.fiatAmount.setScale(
                model.amount.scale().coerceIn(2..MAX_DIGITS),
                BRConstants.ROUNDING_MODE
            ) / pricePerUnit)
        }

        return next(
            model.copy(
                fiatPricePerUnit = pricePerUnit,
                fiatAmount = newFiatAmount,
                amount = newAmount
            )
        )
    }
}
