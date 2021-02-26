/**
 * BreadWallet
 *
 * Created by Drew Carlson <drew.carlson@breadwallet.com> on 12/3/19.
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
package com.breadwallet.ui.importwallet

import com.breadwallet.app.GiftTracker
import com.breadwallet.breadbox.BreadBox
import com.breadwallet.breadbox.hashString
import com.breadwallet.breadbox.isBitcoin
import com.breadwallet.breadbox.isBitcoinCash
import com.breadwallet.breadbox.toBigDecimal
import com.breadwallet.crypto.Key
import com.breadwallet.crypto.Wallet
import com.breadwallet.crypto.WalletManagerState
import com.breadwallet.tools.util.EventUtils
import drewcarlson.mobius.flow.subtypeEffectHandler
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.singleOrNull
import kotlinx.coroutines.flow.take

fun createImportHandler(
    breadBox: BreadBox,
    walletImporter: WalletImporter,
    giftTracker: GiftTracker
) = subtypeEffectHandler<Import.F, Import.E> {
    addFunction(handleValidateKey(breadBox))
    addFunction(handleEstimateImport(breadBox, walletImporter))
    addFunction(handleSubmitTransfer(walletImporter, giftTracker))
    addConsumer<Import.F.TrackEvent> { (event) ->
        EventUtils.pushEvent(event)
    }
}

private val filterBtcLike: (Wallet) -> Boolean = {
    it.currency.run { isBitcoin() || isBitcoinCash() }
}

private fun handleValidateKey(
    breadBox: BreadBox
): suspend (Import.F.ValidateKey) -> Import.E = { effect ->
    val keyBytes = effect.privateKey.toByteArray()
    val passwordBytes = effect.password?.toByteArray() ?: byteArrayOf()
    val wallets = breadBox.wallets()
        .map { it.filter(filterBtcLike) }
        .first { wallets ->
            val btc = wallets.firstOrNull { it.currency.isBitcoin() }
            btc?.walletManager?.state?.type == WalletManagerState.Type.CONNECTED
        }

    // Sweeping only supports BTC and BCH, ensure one is active.
    when {
        wallets.isEmpty() -> Import.E.Key.NoWallets
        keyBytes.isEmpty() -> Import.E.Key.OnInvalid
        passwordBytes.isNotEmpty() -> when {
            Key.createFromPrivateKeyString(keyBytes, passwordBytes).isPresent ->
                Import.E.Key.OnValid(true)
            else ->
                Import.E.Key.OnPasswordInvalid
        }
        passwordBytes.isEmpty() && Key.isProtectedPrivateKeyString(keyBytes) ->
            Import.E.Key.OnValid(true)
        Key.createFromPrivateKeyString(keyBytes).isPresent ->
            Import.E.Key.OnValid()
        else -> Import.E.Key.OnInvalid
    }
}

private fun handleEstimateImport(
    breadBox: BreadBox,
    walletImporter: WalletImporter
): suspend (Import.F.EstimateImport) -> Import.E = { effect ->
    val privateKey = effect.privateKey.toByteArray()
    val password = when (effect) {
        is Import.F.EstimateImport.KeyWithPassword ->
            effect.password.toByteArray()
        else -> null
    }

    walletImporter.setKey(privateKey, password)

    val btcWallets = breadBox.wallets().first().filter(filterBtcLike)

    check(btcWallets.isNotEmpty()) {
        "Import requires an active BTC or BCH wallet."
    }

    val walletFound = btcWallets.asFlow()
        .map { wallet -> walletImporter.prepareSweeper(wallet) }
        .filterIsInstance<WalletImporter.PrepareResult.WalletFound>()
        .take(1)
        .singleOrNull()

    if (walletFound == null) {
        Import.E.Estimate.NoBalance
    } else {
        val walletBalance = walletFound.balance.toBigDecimal()
        when (val result = walletImporter.estimateFee()) {
            is WalletImporter.FeeResult.Success ->
                Import.E.Estimate.Success(
                    walletFound.balance,
                    result.feeBasis.fee,
                    walletFound.currencyCode
                )
            is WalletImporter.FeeResult.InsufficientFunds ->
                Import.E.Estimate.BalanceTooLow(walletBalance)
            is WalletImporter.FeeResult.Failed ->
                Import.E.Estimate.FeeError(walletBalance)
            else ->
                Import.E.Estimate.FeeError(walletBalance)
        }
    }
}

private fun handleSubmitTransfer(
    walletImporter: WalletImporter,
    giftTracker: GiftTracker
): suspend (Import.F.SubmitImport) -> Import.E = { submitTransfer ->
    val privateKey = submitTransfer.privateKey.toByteArray()
    val password = submitTransfer.password?.toByteArray()
    if (walletImporter.readyForImport(privateKey, password)) {
        walletImporter.import()?.let { transfer ->
            submitTransfer.reclaimGiftHash?.let {
                giftTracker.markGiftReclaimed(it)
            }
            Import.E.Transfer.OnSuccess(
                transferHash = transfer.hashString(),
                currencyCode = transfer.wallet.currency.code
            )
        } ?: Import.E.Transfer.OnFailed
    } else {
        Import.E.RetryImport(submitTransfer.privateKey, submitTransfer.password)
    }
}
