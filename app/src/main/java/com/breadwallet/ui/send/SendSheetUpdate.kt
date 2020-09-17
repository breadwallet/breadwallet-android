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

import com.breadwallet.breadbox.TransferSpeed
import com.breadwallet.ext.isZero
import com.breadwallet.tools.util.BRConstants
import com.breadwallet.ui.send.SendSheet.E
import com.breadwallet.ui.send.SendSheet.E.OnAddressValidated.ValidAddress
import com.breadwallet.ui.send.SendSheet.E.OnAddressValidated.ResolvableAddress
import com.breadwallet.ui.send.SendSheet.E.OnAddressValidated.ResolveError
import com.breadwallet.ui.send.SendSheet.E.OnAddressValidated.InvalidAddress
import com.breadwallet.ui.send.SendSheet.E.OnAddressValidated.NoAddress
import com.breadwallet.ui.send.SendSheet.E.OnAmountChange.AddDecimal
import com.breadwallet.ui.send.SendSheet.E.OnAmountChange.AddDigit
import com.breadwallet.ui.send.SendSheet.E.OnAmountChange.Clear
import com.breadwallet.ui.send.SendSheet.E.OnAmountChange.Delete
import com.breadwallet.ui.send.SendSheet.F
import com.breadwallet.ui.send.SendSheet.M
import com.breadwallet.util.isEthereum
import com.spotify.mobius.Effects.effects
import com.spotify.mobius.Next
import com.spotify.mobius.Next.dispatch
import com.spotify.mobius.Next.next
import com.spotify.mobius.Next.noChange
import com.spotify.mobius.Update
import java.math.BigDecimal

// TODO: Is this specific to a given currency or just the app?
const val MAX_DIGITS = 8

@Suppress("TooManyFunctions", "ComplexMethod", "LargeClass")
object SendSheetUpdate : Update<M, E, F>, SendSheetUpdateSpec {

    override fun update(model: M, event: E) = patch(model, event)

    override fun onAmountChange(
        model: M,
        event: E.OnAmountChange
    ): Next<M, F> {
        return when (event) {
            AddDecimal -> addDecimal(model)
            Delete -> delete(model)
            Clear -> clear(model)
            is AddDigit -> addDigit(model, event)
        }
    }

    private fun addDecimal(
        model: M
    ): Next<M, F> {
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
        model: M
    ): Next<M, F> {
        return when {
            model.rawAmount.isEmpty() -> noChange()
            else -> {
                val newRawAmount = model.rawAmount.dropLast(1)
                val newModel = model.withNewRawAmount(newRawAmount)
                when {
                    newModel.amount.isZero() ->
                        next(
                            newModel.copy(
                                fiatNetworkFee = BigDecimal.ZERO,
                                networkFee = BigDecimal.ZERO,
                                transferFeeBasis = null
                            )
                        )
                    else -> {
                        val effects = mutableSetOf<F>()
                        if (newModel.canEstimateFee) {
                            F.EstimateFee(
                                newModel.currencyCode,
                                newModel.targetAddress,
                                newModel.amount,
                                newModel.transferSpeed
                            ).run(effects::add)
                        }
                        next(newModel, effects)
                    }
                }
            }
        }
    }

    private fun addDigit(
        model: M,
        event: AddDigit
    ): Next<M, F> {
        return when {
            model.rawAmount == "0" && event.digit == 0 -> noChange()
            model.rawAmount.split('.').run {
                // Ensure the length of the main or fraction
                // if present are less than MAX_DIGITS
                if (size == 1) first().length == MAX_DIGITS
                else getOrNull(1)?.length == MAX_DIGITS
            } -> noChange()
            else -> {
                val effects = mutableSetOf<F>()
                val newRawAmount = when (model.rawAmount) {
                    "0" -> event.digit.toString()
                    else -> model.rawAmount + event.digit
                }

                val newModel = model.withNewRawAmount(newRawAmount)

                if (newModel.canEstimateFee) {
                    effects.add(
                        F.EstimateFee(
                            newModel.currencyCode,
                            newModel.targetAddress,
                            newModel.amount,
                            newModel.transferSpeed
                        )
                    )
                }
                next(newModel, effects)
            }
        }
    }

    private fun clear(model: M): Next<M, F> {
        return when {
            model.rawAmount.isEmpty() && model.amount.isZero() -> noChange()
            else -> next(
                model.copy(
                    rawAmount = "",
                    fiatAmount = BigDecimal.ZERO,
                    amount = BigDecimal.ZERO,
                    networkFee = BigDecimal.ZERO,
                    fiatNetworkFee = BigDecimal.ZERO,
                    transferFeeBasis = null,
                    amountInputError = null
                )
            )
        }
    }

    @Suppress("ComplexCondition")
    override fun onSendClicked(model: M): Next<M, F> {
        val isBalanceTooLow = model.isTotalCostOverBalance
        val isAmountBlank = model.rawAmount.isBlank() || model.amount.isZero()
        val isTargetBlank = model.targetAddress.isBlank()
        val isEthBalanceLow = when {
            model.run { feeCurrencyCode.isEthereum() && !isFeeNative } ->
                model.networkFee > model.feeCurrencyBalance
            else -> false
        }

        if (isBalanceTooLow || isAmountBlank || isTargetBlank || isEthBalanceLow) {
            val effects = mutableSetOf<F>()
            if (isEthBalanceLow) {
                effects.add(F.ShowEthTooLowForTokenFee(model.feeCurrencyCode, model.networkFee))
            }
            return next(
                model.copy(
                    amountInputError = when {
                        isAmountBlank -> M.InputError.Empty
                        isBalanceTooLow -> M.InputError.BalanceTooLow
                        else -> null
                    },
                    targetInputError = when {
                        isTargetBlank -> M.InputError.Empty
                        else -> null
                    }
                ),
                effects
            )
        }
        return when {
            model.isSendingTransaction || model.isConfirmingTx -> noChange()
            model.feeEstimateFailed || model.transferFeeBasis == null -> noChange()
            model.transferFields.any {
                it.invalid || (it.required && it.value.isNullOrEmpty())
            } -> noChange()
            else -> next(
                model.copy(
                    isConfirmingTx = true,
                    amountInputError = null,
                    targetInputError = null
                )
            )
        }
    }

    override fun onScanClicked(model: M): Next<M, F> {
        return when {
            model.isConfirmingTx -> noChange()
            else -> dispatch(setOf(F.Nav.GoToScan))
        }
    }

    override fun onFaqClicked(model: M): Next<M, F> {
        return when {
            model.isConfirmingTx -> noChange()
            else -> dispatch(setOf(F.Nav.GoToFaq(model.currencyCode)))
        }
    }

    override fun onCloseClicked(model: M): Next<M, F> {
        return when {
            model.isConfirmingTx -> noChange()
            else -> dispatch(setOf(F.Nav.CloseSheet))
        }
    }

    override fun onPasteClicked(model: M): Next<M, F> {
        return when {
            model.isConfirmingTx -> noChange()
            else -> dispatch(
                setOf(
                    F.ParseClipboardData(model.currencyCode, model.feeCurrencyCode)
                )
            )
        }
    }

    override fun onToggleCurrencyClicked(model: M): Next<M, F> {
        if (model.fiatPricePerUnit == BigDecimal.ZERO) {
            return noChange()
        }

        val isAmountCrypto = !model.isAmountCrypto
        val newModel = model.copy(
            isAmountCrypto = isAmountCrypto,
            transferFeeBasis = null,
            rawAmount = when {
                model.amount.isZero() -> model.rawAmount
                else -> when {
                    isAmountCrypto -> model.amount.setScale(MAX_DIGITS, BRConstants.ROUNDING_MODE)
                    else -> model.fiatAmount.setScale(2, BRConstants.ROUNDING_MODE)
                }.toPlainString()
                    .dropLastWhile { it == '0' }
                    .removeSuffix(".")
            }
        )

        return when {
            newModel.canEstimateFee -> next(
                newModel,
                setOf(
                    F.EstimateFee(
                        newModel.currencyCode,
                        newModel.targetAddress,
                        newModel.amount,
                        newModel.transferSpeed
                    )
                )
            )
            else -> next(newModel)
        }
    }

    override fun onBalanceUpdated(
        model: M,
        event: E.OnBalanceUpdated
    ): Next<M, F> {
        val isTotalCostOverBalance = model.totalCost > event.balance
        return next(
            model.copy(
                balance = event.balance,
                fiatBalance = event.fiatBalance,
                feeCurrencyCode = event.feeCurrencyCode,
                feeCurrencyBalance = event.feeCurrencyBalance,
                isTotalCostOverBalance = isTotalCostOverBalance,
                amountInputError = if (isTotalCostOverBalance) {
                    M.InputError.BalanceTooLow
                } else null,
                networkFee = if (isTotalCostOverBalance) {
                    BigDecimal.ZERO
                } else model.networkFee,
                fiatNetworkFee = if (isTotalCostOverBalance) {
                    BigDecimal.ZERO
                } else model.fiatNetworkFee,
                transferFeeBasis = if (isTotalCostOverBalance) {
                    null
                } else model.transferFeeBasis
            )
        )
    }

    override fun onNetworkFeeUpdated(
        model: M,
        event: E.OnNetworkFeeUpdated
    ): Next<M, F> {
        val isTotalCostOverBalance = when {
            model.isFeeNative ->
                model.amount + event.networkFee > model.balance
            else -> model.amount > model.balance
        }
        return when {
            model.amount != event.amount -> noChange()
            model.targetAddress != event.targetAddress -> noChange()
            else -> next(
                model.copy(
                    networkFee = event.networkFee,
                    fiatNetworkFee = event.networkFee * model.fiatPricePerFeeUnit,
                    transferFeeBasis = event.transferFeeBasis,
                    feeEstimateFailed = false,
                    isTotalCostOverBalance = isTotalCostOverBalance,
                    amountInputError = if (isTotalCostOverBalance) {
                        M.InputError.BalanceTooLow
                    } else null
                )
            )
        }
    }

    override fun onNetworkFeeError(
        model: M
    ): Next<M, F> {
        return next(
            model.copy(
                feeEstimateFailed = true,
                amountInputError = M.InputError.FailedToEstimateFee
            )
        )
    }

    override fun onTransferSpeedChanged(
        model: M,
        event: E.OnTransferSpeedChanged
    ): Next<M, F> {
        val transferSpeed = when(event.transferSpeed) {
            TransferSpeedInput.ECONOMY -> TransferSpeed.Economy(model.currencyCode)
            TransferSpeedInput.REGULAR -> TransferSpeed.Regular(model.currencyCode)
            TransferSpeedInput.PRIORITY -> TransferSpeed.Priority(model.currencyCode)
        }
        return when {
            model.isConfirmingTx -> noChange()
            model.canEstimateFee -> next(
                model.copy(
                    transferSpeed = transferSpeed
                ),
                setOf(
                    F.EstimateFee(
                        model.currencyCode,
                        model.targetAddress,
                        model.amount,
                        transferSpeed
                    )
                )
            )
            else -> next(
                model.copy(transferSpeed = transferSpeed)
            )
        }
    }

    override fun onTargetStringChanged(
        model: M,
        event: E.OnTargetStringChanged
    ): Next<M, F> {
        return when {
            model.isConfirmingTx -> noChange()
            model.targetString == event.toAddress -> noChange()
            else -> next(
                model.copy(
                    targetString = event.toAddress,
                    targetAddress = "",
                    targetInputError = null
                )
            )
        }
    }

    override fun onMemoChanged(
        model: M,
        event: E.OnMemoChanged
    ): Next<M, F> {
        return when {
            model.isConfirmingTx -> noChange()
            else -> next(
                model.copy(
                    memo = event.memo
                )
            )
        }
    }

    override fun onAmountEditClicked(model: M): Next<M, F> {
        return when {
            model.isConfirmingTx -> noChange()
            else -> next(
                model.copy(
                    isAmountEditVisible = !model.isAmountEditVisible
                )
            )
        }
    }

    override fun onAmountEditDismissed(model: M): Next<M, F> {
        return when {
            model.isAmountEditVisible -> next(
                model.copy(
                    isAmountEditVisible = false
                )
            )
            else -> noChange()
        }
    }

    override fun onTargetStringEntered(
        model: M
    ): Next<M, F> =
        dispatch(
            setOf(
                F.ValidateAddress(
                    model.currencyCode,
                    model.targetString
                )
            )
        )

    override fun onAddressValidated(
        model: M,
        event: E.OnAddressValidated
    ): Next<M, F> {
        val transferFields = when {
            event is ValidAddress && event.type !is AddressType.NativePublic -> model.transferFields.copy(TransferField.DESTINATION_TAG, event.destinationTag ?: "")
            model.addressType !is AddressType.NativePublic -> model.transferFields.copy(TransferField.DESTINATION_TAG, "")
            else -> model.transferFields
        }

        return when {
            model.isConfirmingTx -> noChange()
            else -> when (event) {
                is ValidAddress -> processValidAddress(model, event.address, event.type, event.targetString, transferFields)
                is ResolvableAddress -> next(
                    model.copy(
                        targetString = event.targetString,
                        addressType = event.type,
                        isResolvingAddress = true,
                        transferFields = transferFields
                    ),
                    setOf<F>(F.ResolveAddress(model.currencyCode, event.targetString, event.type))
                )
                is InvalidAddress -> next(
                    model.copy(
                        addressType = event.type,
                        isResolvingAddress = false,
                        targetInputError = when {
                            event.type is AddressType.Resolvable.PayId -> M.InputError.PayIdInvalid
                            event.type is AddressType.Resolvable.Fio -> M.InputError.FioInvalid
                            event.fromClipboard -> M.InputError.ClipboardInvalid
                            else -> M.InputError.Invalid
                        },
                        transferFields = transferFields
                    )
                )
                is NoAddress -> next(
                    model.copy(
                        addressType = event.type,
                        isResolvingAddress = false,
                        targetInputError = when {
                            event.type is AddressType.Resolvable.PayId -> M.InputError.PayIdNoAddress
                            event.type is AddressType.Resolvable.Fio -> M.InputError.FioNoAddress
                            event.fromClipboard -> M.InputError.ClipboardEmpty
                            else -> M.InputError.Empty
                        },
                        transferFields = transferFields
                    )
                )
                is ResolveError -> next(
                    model.copy(
                        addressType = event.type,
                        isResolvingAddress = false,
                        targetInputError = if (event.type is AddressType.Resolvable.PayId) M.InputError.PayIdRetrievalError else M.InputError.FioRetrievalError,
                        transferFields = transferFields
                    )
                )
            }

        }
    }

    private fun processValidAddress(
        model: M,
        address: String,
        addressType: AddressType,
        targetString: String? = null,
        transferFields: List<TransferField>
    ): Next<M, F> {
        val effects = mutableSetOf<F>()
        if (model.canEstimateFee) {
            F.EstimateFee(
                model.currencyCode,
                address,
                model.amount,
                model.transferSpeed
            ).run(effects::add)
        }

        return next(
            model.copy(
                targetAddress = address,
                targetString = targetString ?: address, // Handle paste
                targetInputError = null,
                addressType = addressType,
                isResolvingAddress = false,
                transferFields = transferFields
            ),
            effects + F.GetTransferFields(model.currencyCode, address)
        )
    }

    override fun confirmTx(
        model: M,
        event: E.ConfirmTx
    ): Next<M, F> {
        return when {
            !model.isConfirmingTx -> noChange()
            else -> when (event) {
                E.ConfirmTx.OnConfirmClicked ->
                    next(
                        model.copy(
                            isConfirmingTx = false,
                            isAuthenticating = true
                        )
                    )
                E.ConfirmTx.OnCancelClicked ->
                    next(model.copy(isConfirmingTx = false))
            }
        }
    }

    override fun onExchangeRateUpdated(
        model: M,
        event: E.OnExchangeRateUpdated
    ): Next<M, F> {
        val pricePerUnit = event.fiatPricePerUnit
        val newAmount: BigDecimal
        val newFiatAmount: BigDecimal

        if (model.isAmountCrypto || model.isBitpayPayment) {
            newAmount = model.amount
            newFiatAmount = if (pricePerUnit > BigDecimal.ZERO) {
                (newAmount * pricePerUnit).setScale(2, BRConstants.ROUNDING_MODE)
            } else {
                model.fiatAmount
            }
        } else {
            newFiatAmount = model.fiatAmount
            newAmount = if (pricePerUnit > BigDecimal.ZERO) {
                newFiatAmount.setScale(
                    pricePerUnit.scale().coerceAtMost(MAX_DIGITS),
                    BRConstants.ROUNDING_MODE
                ) / pricePerUnit
            } else {
                model.amount
            }
        }

        return next(
            model.copy(
                fiatPricePerUnit = pricePerUnit,
                fiatPricePerFeeUnit = event.fiatPricePerFeeUnit,
                feeCurrencyCode = event.feeCurrencyCode,
                amount = newAmount,
                fiatAmount = newFiatAmount,
                fiatNetworkFee = if (pricePerUnit > BigDecimal.ZERO) {
                    (model.networkFee * pricePerUnit).setScale(2, BRConstants.ROUNDING_MODE)
                } else {
                    model.fiatNetworkFee
                }
            )
        )
    }

    override fun onSendComplete(
        model: M,
        event: E.OnSendComplete
    ): Next<M, F> {
        return when {
            model.isSendingTransaction -> {
                val effects = mutableSetOf(
                    F.Nav.GoToTransactionComplete,
                    F.AddTransactionMetaData(
                        event.transfer,
                        model.memo ?: "",
                        model.fiatCode,
                        model.fiatPricePerUnit
                    )
                )
                if (model.isBitpayPayment) {
                    effects.add(
                        F.PaymentProtocol.PostPayment(
                            model.paymentProtocolRequest!!,
                            event.transfer
                        )
                    )
                }
                dispatch(effects)
            }
            else -> noChange()
        }
    }

    override fun onAuthSuccess(
        model: M
    ): Next<M, F> {
        val effects = setOf(
            when {
                model.isBitpayPayment ->
                    F.PaymentProtocol.ContinueWitPayment(
                        model.paymentProtocolRequest!!,
                        transferFeeBasis = checkNotNull(model.transferFeeBasis)
                    )
                else ->
                    F.SendTransaction(
                        currencyCode = model.currencyCode,
                        address = model.targetAddress,
                        amount = model.amount,
                        transferFeeBasis = checkNotNull(model.transferFeeBasis),
                        transferFields = model.transferFields
                    )
            }
        )
        return when {
            model.isAuthenticating -> next(
                model.copy(
                    isAuthenticating = false,
                    isSendingTransaction = true
                ),
                effects
            )
            else -> noChange()
        }
    }

    override fun onAuthCancelled(
        model: M
    ): Next<M, F> {
        return when {
            model.isAuthenticating -> next(
                model.copy(
                    isAuthenticating = false
                )
            )
            else -> noChange()
        }
    }

    override fun onSendFailed(
        model: M
    ): Next<M, F> {
        return when {
            model.isSendingTransaction ->
                next(
                    model.copy(
                        isSendingTransaction = false
                    ),
                    effects(F.ShowTransferFailed)
                )
            else -> noChange()
        }
    }

    override fun goToEthWallet(model: M): Next<M, F> {
        return when {
            model.isSendingTransaction || model.isConfirmingTx -> noChange()
            else -> dispatch(setOf(F.Nav.GoToEthWallet))
        }
    }

    override fun onAuthenticationSettingsUpdated(
        model: M,
        event: E.OnAuthenticationSettingsUpdated
    ): Next<M, F> {
        return next(model.copy(isFingerprintAuthEnable = event.isFingerprintEnable))
    }

    override fun onRequestScanned(
        model: M,
        event: E.OnRequestScanned
    ): Next<M, F> {
        val currencyCode = event.link.currencyCode
        if (
            !currencyCode.equals(model.feeCurrencyCode, true) &&
            !currencyCode.equals(model.currencyCode, true)
        ) {
            return noChange()
        }
        val link = event.link.run {
            copy(
                // Replace the currencyCode because it may contain the
                // address for feeCurrencyCode which may be different
                currencyCode = model.currencyCode,
                amount = amount ?: if (!model.amount.isZero()) model.amount else null,
                message = message ?: model.memo
            )
        }

        val nextModel = link.asSendSheetModel(model.fiatCode).copy(
            balance = model.balance,
            transferSpeed = model.transferSpeed,
            fiatPricePerFeeUnit = model.fiatPricePerUnit,
            fiatPricePerUnit = model.fiatPricePerUnit,
            fiatBalance = model.fiatBalance,
            feeCurrencyCode = model.feeCurrencyCode,
            feeCurrencyBalance = model.feeCurrencyBalance,
            fiatAmount = if (link.amount != null && model.fiatPricePerUnit > BigDecimal.ZERO) {
                (link.amount * model.fiatPricePerUnit).setScale(2, BRConstants.ROUNDING_MODE)
            } else BigDecimal.ZERO
        )

        val effects = mutableSetOf<F>(
            F.GetTransferFields(
                nextModel.currencyCode,
                nextModel.targetAddress
            )
        )

        if (nextModel.transferFields.isNotEmpty()) {
            effects.add(
                F.ValidateTransferFields(
                    nextModel.currencyCode,
                    nextModel.targetAddress,
                    nextModel.transferFields
                )
            )
        }

        if (nextModel.targetAddress.isNotBlank() && !nextModel.amount.isZero()) {
            effects.add(
                F.EstimateFee(
                    nextModel.currencyCode,
                    nextModel.targetAddress,
                    nextModel.amount,
                    nextModel.transferSpeed
                )
            )
        }

        return next(nextModel, effects)
    }

    override fun paymentProtocol(
        model: M,
        event: E.PaymentProtocol
    ): Next<M, F> {
        return when (event) {
            is E.PaymentProtocol.OnPaymentLoaded -> {
                val paymentRequest = event.paymentRequest
                val amount = event.cryptoAmount
                val address = paymentRequest.primaryTarget.get().toString()
                val newModel = model.copy(
                    targetAddress = address,
                    targetString = address,
                    memo = paymentRequest.memo.get(),
                    amount = amount,
                    paymentProtocolRequest = paymentRequest,
                    fiatAmount = if (model.fiatPricePerUnit > BigDecimal.ZERO) {
                        (amount * model.fiatPricePerUnit).setScale(2, BRConstants.ROUNDING_MODE)
                    } else {
                        model.fiatAmount
                    },
                    rawAmount = amount.setScale(
                        MAX_DIGITS,
                        BRConstants.ROUNDING_MODE
                    ).toPlainString().dropLastWhile { it == '0' },
                    isFetchingPayment = false
                )
                next(
                    newModel,
                    effects(
                        F.EstimateFee(
                            model.currencyCode,
                            newModel.targetAddress,
                            amount,
                            model.transferSpeed
                        ),
                        F.GetTransferFields(
                            model.currencyCode,
                            newModel.targetAddress
                        )
                    )
                )
            }
            is E.PaymentProtocol.OnLoadFailed -> {
                next(
                    model.copy(isFetchingPayment = false),
                    effects(F.ShowErrorDialog(event.message))
                )
            }
            else -> noChange()
        }
    }

    override fun onTransferFieldsUpdated(
        model: M,
        event: E.OnTransferFieldsUpdated
    ): Next<M, F> {
        return when {
            model.transferFields.isEmpty() ->
                next(model.copy(transferFields = event.transferFields))
            else -> {
                val fields = event.transferFields
                    .map { field ->
                        val existingField = model.transferFields
                            .find { it.key == field.key }
                        when {
                            existingField != null ->
                                field.copy(
                                    invalid = field.invalid,
                                    value = existingField.value
                                )
                            else -> field
                        }
                    }
                next(
                    model.copy(
                        transferFields = fields
                    )
                )
            }
        }
    }

    override fun transferFieldUpdate(
        model: M,
        event: E.TransferFieldUpdate
    ): Next<M, F> {
        val updatedFields = model.transferFields
            .map { field ->
                if (field.key == event.key) {
                    when (event) {
                        is E.TransferFieldUpdate.Validation ->
                            field.copy(invalid = event.invalid)
                        is E.TransferFieldUpdate.Value ->
                            field.copy(value = if (event.value.isBlank()) null else event.value)
                    }
                } else field
            }

        val nextModel = model.copy(
            transferFields = updatedFields
        )

        return when (event) {
            is E.TransferFieldUpdate.Value ->
                next(
                    nextModel, effects(
                        F.ValidateTransferFields(
                            model.currencyCode,
                            model.targetAddress,
                            updatedFields
                        )
                    )
                )
            else -> next(nextModel)
        }
    }
}
