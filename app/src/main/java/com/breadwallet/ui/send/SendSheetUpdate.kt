package com.breadwallet.ui.send

import com.breadwallet.breadbox.toBigDecimal
import com.breadwallet.ext.isZero
import com.breadwallet.tools.util.BRConstants
import com.breadwallet.ui.send.SendSheetEvent.OnAddressPasted
import com.breadwallet.ui.send.SendSheetEvent.OnAddressPasted.InvalidAddress
import com.breadwallet.ui.send.SendSheetEvent.OnAddressPasted.NoAddress
import com.breadwallet.ui.send.SendSheetEvent.OnAddressPasted.ValidAddress
import com.breadwallet.ui.send.SendSheetEvent.OnAmountChange.AddDecimal
import com.breadwallet.ui.send.SendSheetEvent.OnAmountChange.AddDigit
import com.breadwallet.ui.send.SendSheetEvent.OnAmountChange.Clear
import com.breadwallet.ui.send.SendSheetEvent.OnAmountChange.Delete
import com.spotify.mobius.Next
import com.spotify.mobius.Next.dispatch
import com.spotify.mobius.Next.next
import com.spotify.mobius.Next.noChange
import com.spotify.mobius.Update
import java.math.BigDecimal

// TODO: Is this specific to a given currency or just the app?
const val MAX_DIGITS = 8

@Suppress("TooManyFunctions", "ComplexMethod")
object SendSheetUpdate : Update<SendSheetModel, SendSheetEvent, SendSheetEffect>,
    SendSheetUpdateSpec {

    override fun update(model: SendSheetModel, event: SendSheetEvent) = patch(model, event)

    override fun onAmountChange(
        model: SendSheetModel,
        event: SendSheetEvent.OnAmountChange
    ): Next<SendSheetModel, SendSheetEffect> {
        return when (event) {
            AddDecimal -> addDecimal(model)
            Delete -> delete(model)
            Clear -> clear(model)
            is AddDigit -> addDigit(model, event)
        }
    }

    private fun addDecimal(
        model: SendSheetModel
    ): Next<SendSheetModel, SendSheetEffect> {
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
        model: SendSheetModel
    ): Next<SendSheetModel, SendSheetEffect> {
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
                        val effects = mutableSetOf<SendSheetEffect>()
                        if (newModel.canEstimateFee) {
                            SendSheetEffect.EstimateFee(
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
        model: SendSheetModel,
        event: AddDigit
    ): Next<SendSheetModel, SendSheetEffect> {
        return when {
            model.rawAmount == "0" && event.digit == 0 -> noChange()
            model.rawAmount.split('.').run {
                // Ensure the length of the main or fraction
                // if present are less than MAX_DIGITS
                if (size == 1) first().length == MAX_DIGITS
                else getOrNull(1)?.length == MAX_DIGITS
            } -> noChange()
            else -> {
                val effects = mutableSetOf<SendSheetEffect>()
                val newRawAmount = when (model.rawAmount) {
                    "0" -> event.digit.toString()
                    else -> model.rawAmount + event.digit
                }

                val newModel = model.withNewRawAmount(newRawAmount)

                if (newModel.canEstimateFee) {
                    effects.add(
                        SendSheetEffect.EstimateFee(
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

    private fun clear(model: SendSheetModel): Next<SendSheetModel, SendSheetEffect> {
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
    override fun onSendClicked(model: SendSheetModel): Next<SendSheetModel, SendSheetEffect> {
        val isBalanceTooLow = model.isTotalCostOverBalance
        val isAmountBlank = model.rawAmount.isBlank() || model.amount.isZero()
        val isTargetBlank = model.targetAddress.isBlank()

        if (isBalanceTooLow || isAmountBlank || isTargetBlank) {
            return next(
                model.copy(
                    amountInputError = when {
                        isAmountBlank -> SendSheetModel.InputError.Empty
                        isBalanceTooLow -> SendSheetModel.InputError.BalanceTooLow
                        else -> null
                    },
                    targetInputError = when {
                        isTargetBlank -> SendSheetModel.InputError.Empty
                        else -> null
                    }
                )
            )
        }
        return when {
            model.isSendingTransaction || model.isConfirmingTx -> noChange()
            model.feeEstimateFailed || model.transferFeeBasis == null -> noChange()
            else -> next(
                model.copy(
                    isConfirmingTx = true,
                    amountInputError = null,
                    targetInputError = null
                )
            )
        }
    }

    override fun onScanClicked(model: SendSheetModel): Next<SendSheetModel, SendSheetEffect> {
        return when {
            model.isConfirmingTx -> noChange()
            else -> dispatch(setOf(SendSheetEffect.Nav.GoToScan))
        }
    }

    override fun onFaqClicked(model: SendSheetModel): Next<SendSheetModel, SendSheetEffect> {
        return when {
            model.isConfirmingTx -> noChange()
            else -> dispatch(setOf(SendSheetEffect.Nav.GoToFaq(model.currencyCode)))
        }
    }

    override fun onCloseClicked(model: SendSheetModel): Next<SendSheetModel, SendSheetEffect> {
        return when {
            model.isConfirmingTx -> noChange()
            else -> dispatch(setOf(SendSheetEffect.Nav.CloseSheet))
        }
    }

    override fun onPasteClicked(model: SendSheetModel): Next<SendSheetModel, SendSheetEffect> {
        return when {
            model.isConfirmingTx -> noChange()
            else -> dispatch(
                setOf(
                    SendSheetEffect.ParseClipboardData(model.currencyCode)
                )
            )
        }
    }

    override fun onToggleCurrencyClicked(model: SendSheetModel): Next<SendSheetModel, SendSheetEffect> {
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
                    SendSheetEffect.EstimateFee(
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
        model: SendSheetModel,
        event: SendSheetEvent.OnBalanceUpdated
    ): Next<SendSheetModel, SendSheetEffect> {
        val isTotalCostOverBalance = model.totalCost > event.balance
        return next(
            model.copy(
                balance = event.balance,
                fiatBalance = event.fiatBalance,
                isTotalCostOverBalance = isTotalCostOverBalance,
                amountInputError = if (isTotalCostOverBalance) {
                    SendSheetModel.InputError.BalanceTooLow
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
        model: SendSheetModel,
        event: SendSheetEvent.OnNetworkFeeUpdated
    ): Next<SendSheetModel, SendSheetEffect> {
        val isTotalCostOverBalance = model.amount + event.networkFee > model.balance
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
                        SendSheetModel.InputError.BalanceTooLow
                    } else null
                )
            )
        }
    }

    override fun onNetworkFeeError(
        model: SendSheetModel
    ): Next<SendSheetModel, SendSheetEffect> {
        return next(
            model.copy(
                feeEstimateFailed = true
            )
        )
    }

    override fun onTransferSpeedChanged(
        model: SendSheetModel,
        event: SendSheetEvent.OnTransferSpeedChanged
    ): Next<SendSheetModel, SendSheetEffect> {
        return when {
            model.isConfirmingTx -> noChange()
            model.canEstimateFee -> next(
                model.copy(
                    transferSpeed = event.transferSpeed
                ),
                setOf(
                    SendSheetEffect.EstimateFee(
                        model.currencyCode,
                        model.targetAddress,
                        model.amount,
                        event.transferSpeed
                    )
                )
            )
            else -> next(
                model.copy(transferSpeed = event.transferSpeed)
            )
        }
    }

    override fun onTargetAddressChanged(
        model: SendSheetModel,
        event: SendSheetEvent.OnTargetAddressChanged
    ): Next<SendSheetModel, SendSheetEffect> {
        return when {
            model.isConfirmingTx -> noChange()
            model.targetAddress == event.toAddress -> noChange()
            else -> {
                val effects = mutableSetOf<SendSheetEffect>()
                val newModel = model.copy(
                    targetAddress = event.toAddress,
                    targetInputError = null
                )

                if (newModel.canEstimateFee) {
                    effects.add(
                        SendSheetEffect.EstimateFee(
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

    override fun onMemoChanged(
        model: SendSheetModel,
        event: SendSheetEvent.OnMemoChanged
    ): Next<SendSheetModel, SendSheetEffect> {
        return when {
            model.isConfirmingTx -> noChange()
            else -> next(
                model.copy(
                    memo = event.memo
                )
            )
        }
    }

    override fun onAmountEditClicked(model: SendSheetModel): Next<SendSheetModel, SendSheetEffect> {
        return when {
            model.isConfirmingTx -> noChange()
            else -> next(
                model.copy(
                    isAmountEditVisible = !model.isAmountEditVisible
                )
            )
        }
    }

    override fun onAmountEditDismissed(model: SendSheetModel): Next<SendSheetModel, SendSheetEffect> {
        return when {
            model.isAmountEditVisible -> next(
                model.copy(
                    isAmountEditVisible = false
                )
            )
            else -> noChange()
        }
    }

    override fun onAddressPasted(
        model: SendSheetModel,
        event: OnAddressPasted
    ): Next<SendSheetModel, SendSheetEffect> {
        val effects = mutableSetOf<SendSheetEffect>()
        return when {
            model.isConfirmingTx -> noChange()
            else -> when (event) {
                is ValidAddress -> {
                    if (model.canEstimateFee) {
                        SendSheetEffect.EstimateFee(
                            model.currencyCode,
                            event.address,
                            model.amount,
                            model.transferSpeed
                        ).run(effects::add)
                    }

                    next(
                        model.copy(
                            targetAddress = event.address,
                            targetInputError = null
                        ),
                        effects
                    )
                }
                is InvalidAddress -> next(
                    model.copy(
                        targetInputError = SendSheetModel.InputError.ClipboardInvalid
                    )
                )
                is NoAddress -> next(
                    model.copy(
                        targetInputError = SendSheetModel.InputError.ClipboardEmpty
                    )
                )
            }
        }
    }

    override fun confirmTx(
        model: SendSheetModel,
        event: SendSheetEvent.ConfirmTx
    ): Next<SendSheetModel, SendSheetEffect> {
        return when {
            !model.isConfirmingTx -> noChange()
            else -> when (event) {
                SendSheetEvent.ConfirmTx.OnConfirmClicked ->
                    next(
                        model.copy(
                            isConfirmingTx = false,
                            isAuthenticating = true
                        )
                    )
                SendSheetEvent.ConfirmTx.OnCancelClicked ->
                    next(model.copy(isConfirmingTx = false))
            }
        }
    }

    override fun onExchangeRateUpdated(
        model: SendSheetModel,
        event: SendSheetEvent.OnExchangeRateUpdated
    ): Next<SendSheetModel, SendSheetEffect> {
        val pricePerUnit = event.fiatPricePerUnit
        val newAmount: BigDecimal
        val newFiatAmount: BigDecimal

        if (model.isAmountCrypto) {
            newAmount = model.amount
            newFiatAmount = if (pricePerUnit > BigDecimal.ZERO) {
                (newAmount * pricePerUnit).setScale(2, BRConstants.ROUNDING_MODE)
            } else {
                model.fiatAmount
            }
        } else {
            newFiatAmount = model.fiatAmount
            newAmount = if (pricePerUnit > BigDecimal.ZERO) {
                newFiatAmount.divide(pricePerUnit, BRConstants.ROUNDING_MODE)
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
        model: SendSheetModel,
        event: SendSheetEvent.OnSendComplete
    ): Next<SendSheetModel, SendSheetEffect> {
        return when {
            model.isSendingTransaction -> {
                val effects = mutableSetOf<SendSheetEffect>(
                    SendSheetEffect.Nav.GoToTransactionComplete
                )
                if (!model.memo.isNullOrBlank()) {
                    SendSheetEffect.AddTransactionMetaData(
                        event.transfer,
                        model.memo,
                        model.fiatCode,
                        model.fiatPricePerUnit
                    ).run(effects::add)
                }
                dispatch(effects)
            }
            else -> noChange()
        }
    }

    override fun onAuthSuccess(
        model: SendSheetModel
    ): Next<SendSheetModel, SendSheetEffect> {
        return when {
            model.isAuthenticating -> next(
                model.copy(
                    isAuthenticating = false,
                    isSendingTransaction = true
                ),
                setOf(
                    SendSheetEffect.SendTransaction(
                        currencyCode = model.currencyCode,
                        address = model.targetAddress,
                        amount = model.amount,
                        transferFeeBasis = checkNotNull(model.transferFeeBasis)
                    )
                )
            )
            else -> noChange()
        }
    }

    override fun onAuthCancelled(
        model: SendSheetModel
    ): Next<SendSheetModel, SendSheetEffect> {
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
        model: SendSheetModel
    ): Next<SendSheetModel, SendSheetEffect> {
        return when {
            model.isSendingTransaction ->
                next(
                    model.copy(
                        isSendingTransaction = false
                    )
                )// TODO: Display error (not "something went wrong")
            else -> noChange()
        }
    }

    override fun goToEthWallet(model: SendSheetModel): Next<SendSheetModel, SendSheetEffect> {
        return when {
            model.isSendingTransaction || model.isConfirmingTx -> noChange()
            else -> dispatch(setOf(SendSheetEffect.Nav.GoToEthWallet))
        }
    }

    override fun onAddressValidated(
        model: SendSheetModel,
        event: SendSheetEvent.OnAddressValidated
    ): Next<SendSheetModel, SendSheetEffect> {
        if (model.targetAddress != event.address) return noChange()

        val effects = mutableSetOf<SendSheetEffect>()
        val newModel = model.copy(
            targetInputError = if (event.isValid) null
            else SendSheetModel.InputError.Invalid
        )

        if (newModel.canEstimateFee) {
            effects.add(
                SendSheetEffect.EstimateFee(
                    currencyCode = newModel.currencyCode,
                    address = newModel.targetAddress,
                    amount = newModel.amount,
                    transferSpeed = newModel.transferSpeed
                )
            )
        }

        return next(newModel, effects)
    }

    override fun onAuthenticationSettingsUpdated(
        model: SendSheetModel,
        event: SendSheetEvent.OnAuthenticationSettingsUpdated
    ): Next<SendSheetModel, SendSheetEffect> {
        return next(model.copy(isFingerprintAuthEnable = event.isFingerprintEnable))
    }

    override fun onRequestScanned(
        model: SendSheetModel,
        event: SendSheetEvent.OnRequestScanned
    ): Next<SendSheetModel, SendSheetEffect> {
        if (!event.currencyCode.equals(model.currencyCode, ignoreCase = true)) {
            return noChange()
        }

        val targetAddress = event.targetAddress ?: ""
        val amount = event.amount ?: model.amount
        val rawAmount = event.amount?.stripTrailingZeros()?.toPlainString() ?: model.rawAmount

        val effects = mutableSetOf<SendSheetEffect>()

        if (!event.targetAddress.isNullOrBlank() && !amount.isZero()) {
            effects.add(
                SendSheetEffect.EstimateFee(
                    model.currencyCode,
                    targetAddress,
                    amount,
                    model.transferSpeed
                )
            )
        }

        return next(
            model.copy(
                isAmountCrypto = true,
                targetAddress = targetAddress,
                amount = amount,
                rawAmount = rawAmount,
                fiatAmount = if (model.fiatPricePerUnit > BigDecimal.ZERO) {
                    (amount * model.fiatPricePerUnit).setScale(2, BRConstants.ROUNDING_MODE)
                } else {
                    model.fiatAmount
                }
            ),
            effects
        )
    }
}
