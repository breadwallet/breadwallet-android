package com.breadwallet.ui.send

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
import kotlinx.coroutines.flow.flow
import java.math.BigDecimal

// TODO: Is this specific to a given currency or just the app?
private const val MAX_DIGITS = 8

@Suppress("TooManyFunctions")
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
                    newModel.amount == BigDecimal.ZERO ->
                        next(
                            newModel.copy(
                                fiatNetworkFee = BigDecimal.ZERO,
                                networkFee = BigDecimal.ZERO,
                                transferFeeBasis = null
                            )
                        )
                    else -> next(
                        newModel, setOf<SendSheetEffect>(
                            SendSheetEffect.EstimateFee(
                                newModel.currencyCode,
                                newModel.toAddress,
                                newModel.amount,
                                newModel.transferSpeed
                            )
                        )
                    )
                }
            }
        }
    }

    private fun addDigit(
        model: SendSheetModel,
        event: SendSheetEvent.OnAmountChange.AddDigit
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
                val newRawAmount = when (model.rawAmount) {
                    "0" -> event.digit.toString()
                    else -> model.rawAmount + event.digit
                }

                val newModel = model.withNewRawAmount(newRawAmount)
                next(
                    newModel, setOf<SendSheetEffect>(
                        SendSheetEffect.EstimateFee(
                            newModel.currencyCode,
                            newModel.toAddress,
                            newModel.amount,
                            newModel.transferSpeed
                        )
                    )
                )
            }
        }
    }

    private fun clear(model: SendSheetModel): Next<SendSheetModel, SendSheetEffect> {
        return when {
            model.rawAmount.isEmpty() && model.amount == BigDecimal.ZERO -> noChange()
            else -> next(
                model.copy(
                    rawAmount = "",
                    fiatAmount = BigDecimal.ZERO,
                    amount = BigDecimal.ZERO,
                    networkFee = BigDecimal.ZERO,
                    fiatNetworkFee = BigDecimal.ZERO,
                    transferFeeBasis = null
                )
            )
        }
    }

    override fun onSendClicked(model: SendSheetModel): Next<SendSheetModel, SendSheetEffect> {
        return when {
            model.isAmountOverBalance || model.isConfirmingTx -> noChange()
            model.rawAmount.isBlank() || model.amount == BigDecimal.ZERO ->
                dispatch(setOf(SendSheetEffect.ShowNoAmountError))
            model.toAddress.isBlank() ->
                dispatch(setOf(SendSheetEffect.ShowInvalidAddress(model.currencyCode)))
            else -> next(
                model.copy(isConfirmingTx = true)
            )
        }
    }

    override fun onScanClicked(model: SendSheetModel): Next<SendSheetModel, SendSheetEffect> {
        return when {
            model.isConfirmingTx -> noChange()
            else -> dispatch(setOf(SendSheetEffect.GoToScan))
        }
    }

    override fun onFaqClicked(model: SendSheetModel): Next<SendSheetModel, SendSheetEffect> {
        return when {
            model.isConfirmingTx -> noChange()
            else -> dispatch(setOf(SendSheetEffect.GoToFaq(model.currencyCode)))
        }
    }

    override fun onCloseClicked(model: SendSheetModel): Next<SendSheetModel, SendSheetEffect> {
        return when {
            model.isConfirmingTx -> noChange()
            else -> dispatch(setOf(SendSheetEffect.CloseSheet))
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
        val newModel = model
            .copy(isAmountCrypto = !model.isAmountCrypto)
            .withNewRawAmount(model.rawAmount)

        return when {
            newModel.amount == BigDecimal.ZERO -> next(newModel)
            else -> next(
                newModel, setOf(
                    SendSheetEffect.EstimateFee(
                        newModel.currencyCode,
                        newModel.toAddress,
                        newModel.amount,
                        newModel.transferSpeed
                    )
                )
            )
        }
    }

    override fun onBalanceUpdated(
        model: SendSheetModel,
        event: SendSheetEvent.OnBalanceUpdated
    ): Next<SendSheetModel, SendSheetEffect> {
        return next(
            model.copy(
                balance = event.balance,
                fiatBalance = event.fiatBalance,
                isAmountOverBalance = when {
                    model.isAmountCrypto -> model.amount > event.balance
                    else -> model.amount > event.fiatBalance
                }
            )
        )
    }

    override fun onNetworkFeeUpdated(
        model: SendSheetModel,
        event: SendSheetEvent.OnNetworkFeeUpdated
    ): Next<SendSheetModel, SendSheetEffect> {
        return when {
            model.amount == BigDecimal.ZERO -> noChange()
            else -> next(
                model.copy(
                    networkFee = event.networkFee,
                    fiatNetworkFee = event.networkFee * model.fiatPricePerUnit,
                    transferFeeBasis = event.transferFeeBasis
                )
            )
        }
    }

    override fun onTransferSpeedChanged(
        model: SendSheetModel,
        event: SendSheetEvent.OnTransferSpeedChanged
    ): Next<SendSheetModel, SendSheetEffect> {
        return when {
            model.isConfirmingTx -> noChange()
            else -> next(
                model.copy(
                    transferSpeed = event.transferSpeed
                ),
                setOf(
                    SendSheetEffect.EstimateFee(
                        model.currencyCode,
                        model.toAddress,
                        model.amount,
                        event.transferSpeed
                    )
                )
            )
        }
    }

    override fun onToAddressChanged(
        model: SendSheetModel,
        event: SendSheetEvent.OnToAddressChanged
    ): Next<SendSheetModel, SendSheetEffect> {
        return when {
            model.isConfirmingTx -> noChange()
            else -> next(
                model.copy(
                    toAddress = event.toAddress
                )
            )
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
        return when {
            model.isConfirmingTx -> noChange()
            else -> when (event) {
                is ValidAddress ->
                    next(model.copy(toAddress = event.address))
                is InvalidAddress -> dispatch(
                    setOf<SendSheetEffect>(
                        SendSheetEffect.ShowInvalidClipboardData(model.currencyCode)
                    )
                )
                is NoAddress -> dispatch(
                    setOf<SendSheetEffect>(
                        SendSheetEffect.ShowClipboardEmpty
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
        return next(
            model.copy(
                fiatPricePerUnit = pricePerUnit,
                fiatAmount = model.amount * pricePerUnit,
                fiatNetworkFee = model.networkFee * pricePerUnit
            )
        )
    }

    override fun onSendComplete(
        model: SendSheetModel
    ): Next<SendSheetModel, SendSheetEffect> {
        return when {
            model.isSendingTransaction ->
                dispatch(setOf(SendSheetEffect.ShowTransactionComplete))
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
                        address = model.toAddress,
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
            else -> dispatch(setOf(SendSheetEffect.GoToEthWallet))
        }
    }
}
