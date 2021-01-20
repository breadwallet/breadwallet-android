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

import android.content.Context
import com.breadwallet.breadbox.BreadBox
import com.breadwallet.breadbox.addressFor
import com.breadwallet.breadbox.estimateFee
import com.breadwallet.breadbox.toBigDecimal
import com.breadwallet.crypto.Amount
import com.breadwallet.crypto.TransferFeeBasis
import com.breadwallet.crypto.TransferState.Type.CREATED
import com.breadwallet.crypto.TransferState.Type.DELETED
import com.breadwallet.crypto.TransferState.Type.FAILED
import com.breadwallet.crypto.TransferState.Type.INCLUDED
import com.breadwallet.crypto.TransferState.Type.PENDING
import com.breadwallet.crypto.TransferState.Type.SIGNED
import com.breadwallet.crypto.TransferState.Type.SUBMITTED
import com.breadwallet.crypto.WalletManagerState
import com.breadwallet.crypto.errors.FeeEstimationError
import com.breadwallet.crypto.errors.TransferSubmitError
import com.breadwallet.logger.logError
import com.breadwallet.tools.manager.BRClipboardManager
import com.breadwallet.tools.manager.BRSharedPrefs
import com.breadwallet.tools.security.BrdUserManager
import com.breadwallet.tools.security.isFingerPrintAvailableAndSetup
import com.breadwallet.tools.util.EventUtils
import com.breadwallet.ui.uistaking.Staking.E
import com.breadwallet.ui.uistaking.Staking.F
import com.breadwallet.ui.uistaking.Staking.M
import com.breadwallet.ui.uistaking.Staking.M.ViewValidator.State.PENDING_STAKE
import com.breadwallet.ui.uistaking.Staking.M.ViewValidator.State.PENDING_UNSTAKE
import com.breadwallet.ui.uistaking.Staking.M.ViewValidator.State.STAKED
import drewcarlson.mobius.flow.flowTransformer
import drewcarlson.mobius.flow.subtypeEffectHandler
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.dropWhile
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.mapNotNull
import java.util.Date
import kotlin.time.milliseconds

private const val DELEGATE = "Delegate"
private const val UNSTAKE_KEY = "Type"
private const val UNSTAKE_VALUE = "Delegation"
private const val DELEGATION_OP = "DelegationOp"
private const val DELEGATION_OP_VALUE = "1"
private val WALLET_UPDATE_DEBOUNCE = 250L.milliseconds

fun createStakingHandler(
    context: Context,
    currencyId: String,
    breadBox: BreadBox,
    userManager: BrdUserManager
) = subtypeEffectHandler<F, E> {
    addTransformer(handleLoadAccount(currencyId, breadBox))
    addFunction<F.ValidateAddress> { (address) ->
        val wallet = breadBox.wallet(currencyId).first()
        E.OnAddressValidated(
            isValid = wallet.addressFor(address) != null
        )
    }
    addFunction<F.Stake> { (address, feeBasis) ->
        breadBox.changeValidator(
            currencyId,
            targetAddress = address,
            feeEstimate = feeBasis,
            phrase = checkNotNull(userManager.getPhrase())
        )
    }
    addFunction<F.Unstake> { (feeBasis) ->
        breadBox.changeValidator(
            currencyId,
            targetAddress = null,
            feeEstimate = feeBasis,
            phrase = checkNotNull(userManager.getPhrase())
        )
    }
    addLatestValueCollector<F.PasteFromClipboard> { effect ->
        val wallet = breadBox.wallet(currencyId).first()
        val text = BRClipboardManager.getClipboard()
        if (wallet.addressFor(text) == null) {
            emit(E.OnAddressValidated(isValid = false))
        } else if (text != effect.currentDelegateAddress) {
            emit(E.OnAddressChanged(text))
        }
    }
    addFunction<F.EstimateFee> { (address) ->
        val wallet = breadBox.wallet(currencyId).first()
        val attrs = wallet.transferAttributes
            .filter { it.key == DELEGATION_OP }
            .onEach { it.setValue(DELEGATION_OP_VALUE) }
            .toSet()
        val networkFee = wallet.walletManager.network.fees.minByOrNull {
            it.confirmationTimeInMilliseconds.toLong()
        }.run(::checkNotNull)
        try {
            val amount = Amount.create(0, wallet.unitForFee)
            val coreAddress = if (address.isNullOrBlank()) {
                wallet.target
            } else {
                checkNotNull(wallet.addressFor(address))
            }
            val feeBasis = wallet.estimateFee(coreAddress, amount, networkFee, attrs = attrs)
            E.OnFeeUpdated(address, feeBasis, wallet.balance.toBigDecimal())
        } catch (e: FeeEstimationError) {
            E.OnTransferFailed(M.TransactionError.FeeEstimateFailed)
        }
    }
    addFunctionSync<F.LoadAuthenticationSettings> {
        val isEnabled = isFingerPrintAvailableAndSetup(context) && BRSharedPrefs.sendMoneyWithFingerprint
        E.OnAuthenticationSettingsUpdated(isEnabled)
    }
}

private val stakedTransferStates = listOf(SUBMITTED, INCLUDED, PENDING, CREATED, SIGNED)

private fun handleLoadAccount(
    currencyId: String,
    breadBox: BreadBox
) = flowTransformer<F.LoadAccount, E> { effects ->
    effects
        .flatMapLatest { breadBox.wallet(currencyId) }
        .dropWhile { it.walletManager.state != WalletManagerState.CONNECTED() }
        .debounce(WALLET_UPDATE_DEBOUNCE)
        .mapLatest { wallet ->
            val currencyCode = wallet.currency.code
            val transfer = wallet.transfers
                .filter { stakedTransferStates.contains(it.state.type) }
                .sortedByDescending { it.confirmation.orNull()?.confirmationTime ?: Date() }
                .firstOrNull { transfer ->
                    transfer.attributes.any {
                        it.key.equals(DELEGATION_OP, true) ||
                        it.key.equals(DELEGATE, true) ||
                            (it.key.equals(UNSTAKE_KEY, true) &&
                                it.value.or("").equals(UNSTAKE_VALUE, true))
                    } && (transfer.confirmation.orNull()?.success ?: true)
                }
            val balance = wallet.balance.toBigDecimal()

            when (transfer) {
                null -> E.AccountUpdated.Unstaked(currencyCode, balance)
                else -> {
                    val address = transfer.target.orNull()?.toString() ?: ""
                    val isConfirmed = transfer.confirmation.orNull()?.success ?: false
                    val delegateAddress = transfer.attributes.find { it.key.equals(DELEGATE, true) }?.value?.or(address)
                    val isStaked = delegateAddress != null
                    if (isConfirmed) {
                        if (isStaked) {
                            E.AccountUpdated.Staked(currencyCode, delegateAddress!!, STAKED, balance)
                        } else E.AccountUpdated.Unstaked(currencyCode, balance)
                    } else {
                        val state = if (isStaked) PENDING_STAKE else PENDING_UNSTAKE
                        E.AccountUpdated.Staked(currencyCode, address, state, balance)
                    }
                }
            }
        }
        .distinctUntilChanged()
}

private suspend fun BreadBox.changeValidator(
    currencyId: String,
    targetAddress: String? = null,
    feeEstimate: TransferFeeBasis,
    phrase: ByteArray
): E {
    val wallet = wallet(currencyId).first()
    val amount = Amount.create(0, wallet.unit)
    val attrs = wallet.transferAttributes
        .filter { it.key.equals(DELEGATION_OP, true) }
        .onEach { it.setValue(DELEGATION_OP_VALUE) }
        .toSet()

    return try {
        // Estimate and submit transfer
        val event: String
        val address = if (targetAddress == null) {
            event = EventUtils.EVENT_WALLET_UNSTAKE
            wallet.target // unstake
        } else {
            event = EventUtils.EVENT_WALLET_STAKE
            checkNotNull(wallet.addressFor(targetAddress))
        }
        val transfer = wallet.createTransfer(address, amount, feeEstimate, attrs).orNull()
        checkNotNull(transfer) { "Failed to create transfer." }
        wallet.walletManager.submit(transfer, phrase)
        val transferHash = checkNotNull(transfer.hash.orNull()).toString()
        // Await a result for the transfer
        walletTransfer(wallet.currency.code, transferHash)
            .mapNotNull { tx ->
                when (checkNotNull(tx.state.type)) {
                    // Ignore pre-submit states
                    CREATED, SIGNED -> null
                    INCLUDED, PENDING, SUBMITTED -> {
                        val target = checkNotNull(tx.target.orNull()).toString()
                        val balance = wallet.balance.toBigDecimal()
                        EventUtils.pushEvent(event)
                        E.AccountUpdated.Staked(wallet.currency.code, target, PENDING_STAKE, balance)
                    }
                    DELETED, FAILED -> {
                        logError("Failed to submit transfer ${tx.state.failedError.orNull()}")
                        E.OnTransferFailed(M.TransactionError.Unknown)
                    }
                }
            }.first()
    } catch (e: TransferSubmitError) {
        logError("Failed to submit transfer", e)
        E.OnTransferFailed(M.TransactionError.TransferFailed)
    } catch (e: IllegalStateException) {
        logError("Unexpected error when submitting transfer", e)
        E.OnTransferFailed(M.TransactionError.Unknown)
    }
}
