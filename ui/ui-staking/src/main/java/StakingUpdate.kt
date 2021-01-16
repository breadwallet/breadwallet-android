/**
 * BreadWallet
 *
 * Created by Drew Carlson <drew.carlson@breadwallet.com> on 10/30/20.
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
package com.breadwallet.ui.uistaking

import com.breadwallet.breadbox.toBigDecimal
import com.breadwallet.ui.uistaking.Staking.E
import com.breadwallet.ui.uistaking.Staking.F
import com.breadwallet.ui.uistaking.Staking.M
import com.breadwallet.ui.uistaking.Staking.M.ViewValidator.State.PENDING_STAKE
import com.breadwallet.ui.uistaking.Staking.M.ViewValidator.State.PENDING_UNSTAKE
import com.breadwallet.ui.uistaking.Staking.M.ViewValidator.State.STAKED
import com.spotify.mobius.Next
import com.spotify.mobius.Next.dispatch
import com.spotify.mobius.Next.next
import com.spotify.mobius.Next.noChange
import com.spotify.mobius.Update

object StakingUpdate : Update<M, E, F> {
    override fun update(model: M, event: E): Next<M, F> = when (event) {
        is E.AccountUpdated -> accountUpdated(model, event)
        is E.OnAddressChanged -> onAddressChanged(model, event)
        is E.OnAddressValidated -> onAddressValidated(model, event)
        is E.OnTransferFailed -> onTransferFailed(model, event)
        is E.OnFeeUpdated -> onFeeUpdated(model, event)
        is E.OnAuthenticationSettingsUpdated -> onAuthenticationSettingsUpdated(model, event)
        E.OnStakeClicked -> onStakeClicked(model)
        E.OnUnstakeClicked -> onUnstakeClicked(model)
        E.OnChangeClicked -> onChangeClicked(model)
        E.OnCancelClicked -> onCancelClicked(model)
        E.OnConfirmClicked -> onConfirmClicked(model)
        E.OnPasteClicked -> onPasteClicked(model)
        E.OnCloseClicked -> onCloseClicked()
        E.OnHelpClicked -> onHelpClicked()
        E.OnTransactionConfirmClicked -> onTransactionConfirmClicked(model)
        E.OnTransactionCancelClicked -> onTransactionCancelClicked(model)
        E.OnAuthSuccess -> onAuthSuccess(model)
        E.OnAuthCancelled -> onAuthCancelled(model)
    }

    private fun accountUpdated(
        model: M,
        event: E.AccountUpdated
    ): Next<M, F> = next(
        when (event) {
            is E.AccountUpdated.Unstaked ->
                M.SetValidator.createDefault(
                    event.balance,
                    model.currencyId,
                    event.currencyCode,
                    isFingerprintEnabled = model.isFingerprintEnabled
                )
            is E.AccountUpdated.Staked -> M.ViewValidator(
                state = event.state,
                balance = event.balance,
                address = event.address,
                currencyCode = event.currencyCode,
                currencyId = model.currencyId,
                isFingerprintEnabled = model.isFingerprintEnabled
            )
        }
    )

    private fun onAddressChanged(
        model: M,
        event: E.OnAddressChanged
    ): Next<M, F> {
        return if (model.address == event.address) {
            noChange()
        } else {
            when (model) {
                is M.SetValidator -> {
                    next(
                        model.copy(
                            address = event.address,
                            isAddressValid = (event.address.isBlank() || model.isAddressValid) && event.address != model.originalAddress,
                            isAddressChanged = event.address != model.originalAddress,
                            canSubmitTransfer = false,
                            feeEstimate = null
                        ),
                        setOfNotNull(
                            if (event.address.isNotBlank()) {
                                F.ValidateAddress(event.address)
                            } else null
                        )
                    )
                }
                else -> noChange()
            }
        }
    }

    private fun onAddressValidated(
        model: M,
        event: E.OnAddressValidated
    ): Next<M, F> = when (model) {
        is M.SetValidator -> next(
            model.copy(
                isAddressValid = event.isValid,
                canSubmitTransfer = event.isValid,
            ),
            setOfNotNull(
                if (event.isValid) {
                    F.EstimateFee(model.address)
                } else null
            )
        )
        else -> noChange()
    }

    private fun onTransferFailed(
        model: M,
        event: E.OnTransferFailed
    ): Next<M, F> = when (model) {
        is M.SetValidator -> next(
            model.copy(
                transactionError = event.transactionError,
                confirmWhenReady = false
            )
        )
        else -> noChange()
    }

    private fun onFeeUpdated(
        model: M,
        event: E.OnFeeUpdated
    ): Next<M, F> = when (model) {
        is M.ViewValidator -> if (model.state == PENDING_UNSTAKE) {
            next(
                model.copy(feeEstimate = event.feeEstimate),
                setOf(
                    F.ConfirmTransaction(
                        currencyCode = model.currencyCode,
                        address = null,
                        balance = event.balance,
                        fee = event.feeEstimate.fee.toBigDecimal()
                    )
                )
            )
        } else noChange()
        is M.SetValidator -> if (event.address == model.address) {
            next(model.copy(feeEstimate = event.feeEstimate))
        } else {
            next(model)
        }
        else -> noChange()
    }

    private fun onStakeClicked(model: M): Next<M, F> = when (model) {
        is M.SetValidator -> when {
            model.isAddressValid &&
                model.address.isNotBlank() &&
                model.feeEstimate != null ->
                dispatch(
                    setOf(
                        F.ConfirmTransaction(
                            currencyCode = model.currencyCode,
                            address = model.address,
                            balance = model.balance,
                            fee = model.feeEstimate.fee.toBigDecimal()
                        )
                    )
                )
            model.feeEstimate == null ->
                next(model.copy(confirmWhenReady = true))
            else -> noChange()
        }
        else -> noChange()
    }

    private fun onUnstakeClicked(model: M): Next<M, F> = when (model) {
        is M.ViewValidator -> if (model.state == STAKED) {
            next(
                model.copy(state = PENDING_UNSTAKE),
                setOf(F.EstimateFee(null))
            )
        } else noChange()
        else -> noChange()
    }

    private fun onChangeClicked(model: M): Next<M, F> = when (model) {
        is M.ViewValidator -> if (model.state == STAKED) {
            next(
                M.SetValidator.createDefault(
                    model.balance,
                    model.currencyId,
                    model.currencyCode,
                    model.address,
                    model.isFingerprintEnabled,
                )
            )
        } else noChange()
        else -> noChange()
    }

    private fun onCancelClicked(model: M): Next<M, F> = when (model) {
        is M.SetValidator -> if (model.isCancellable) {
            next(
                M.ViewValidator(
                    address = model.originalAddress,
                    balance = model.balance,
                    state = STAKED,
                    currencyId = model.currencyId,
                    currencyCode = model.currencyCode
                )
            )
        } else noChange()
        else -> noChange()
    }

    private fun onConfirmClicked(model: M): Next<M, F> = when (model) {
        is M.SetValidator -> if (
            model.isAddressValid &&
            model.isAddressChanged &&
            model.feeEstimate != null
        ) {
            next(
                M.ViewValidator(
                    currencyId = model.currencyId,
                    currencyCode = model.currencyCode,
                    address = model.address,
                    balance = model.balance,
                    state = PENDING_STAKE
                ),
                setOf(F.Stake(model.address, model.feeEstimate))
            )
        } else noChange()
        else -> noChange()
    }

    private fun onPasteClicked(model: M): Next<M, F> = when (model) {
        is M.SetValidator -> dispatch(setOf(
            if (model.originalAddress.isNotBlank()) {
                F.PasteFromClipboard(model.originalAddress)
            } else {
                F.PasteFromClipboard(model.address)
            }
        ))
        else -> noChange()
    }

    private fun onCloseClicked(): Next<M, F> =
        dispatch(setOf(F.Close))

    private fun onHelpClicked(): Next<M, F> =
        dispatch(setOf(F.Help))

    private fun onTransactionConfirmClicked(model: M): Next<M, F> =
        when (model) {
            is M.ViewValidator -> next(model.copy(isAuthenticating = true))
            is M.SetValidator -> next(model.copy(isAuthenticating = true))
            else -> noChange()
        }

    private fun onAuthSuccess(model: M): Next<M, F> =
        when (model) {
            is M.SetValidator -> if (
                model.isAddressValid &&
                model.address.isNotBlank() &&
                model.feeEstimate != null
            ) {
                next(
                    model.copy(isAuthenticating = false),
                    setOf(F.Stake(model.address, model.feeEstimate))
                )
            } else noChange()
            is M.ViewValidator -> if (model.state == PENDING_UNSTAKE && model.feeEstimate != null) {
                next(
                    model.copy(isAuthenticating = false),
                    setOf(F.Unstake(model.feeEstimate))
                )
            } else noChange()
            else -> noChange()
        }

    private fun onAuthCancelled(model:M): Next<M, F> =
        when (model) {
            is M.ViewValidator -> next(model.copy(isAuthenticating = false, state = STAKED))
            is M.SetValidator -> next(model.copy(isAuthenticating = false))
            else -> noChange()
        }

    private fun onTransactionCancelClicked(model: M): Next<M, F> =
        when (model) {
            is M.ViewValidator -> if (model.state == PENDING_UNSTAKE) {
                next(model.copy(state = STAKED))
            } else noChange()
            else -> noChange()
        }

    private fun onAuthenticationSettingsUpdated(model: M, event: E.OnAuthenticationSettingsUpdated): Next<M, F> =
        when (model) {
            is M.ViewValidator -> next(model.copy(isFingerprintEnabled = event.isFingerprintEnabled))
            is M.SetValidator -> next(model.copy(isFingerprintEnabled = event.isFingerprintEnabled))
            is M.Loading -> next(model.copy(isFingerprintEnabled = event.isFingerprintEnabled))
        }
}
