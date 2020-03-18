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

import com.breadwallet.breadbox.BreadBox
import com.breadwallet.breadbox.hashString
import com.breadwallet.breadbox.toBigDecimal
import com.breadwallet.crypto.Key
import com.breadwallet.crypto.Wallet
import com.breadwallet.ui.navigation.NavEffectTransformer
import com.breadwallet.util.isBitcoin
import com.breadwallet.util.isBitcoinCash
import drewcarlson.mobius.flow.flowTransformer
import drewcarlson.mobius.flow.subtypeEffectHandler
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.singleOrNull
import kotlinx.coroutines.flow.take

object ImportHandler {
    fun create(
        breadBox: BreadBox,
        walletImporter: WalletImporter,
        navEffectHandler: NavEffectTransformer,
        view: ImportViewActions
    ) = subtypeEffectHandler<Import.F, Import.E> {
        addTransformer<Import.F.Nav>(navEffectHandler)
        addTransformer(handleValidateKey(breadBox))
        addFunction(handleEstimateImport(breadBox, walletImporter))
        addFunction(handleSubmitTransfer(walletImporter))

        addConsumerSync<Import.F.ShowConfirmImport>(Dispatchers.Main) { effect ->
            view.showConfirmImport(effect.receiveAmount, effect.feeAmount)
        }
        addActionSync<Import.F.ShowNoBalance>(Dispatchers.Main, view::showNoBalance)
        addActionSync<Import.F.ShowKeyInvalid>(Dispatchers.Main, view::showKeyInvalid)
        addActionSync<Import.F.ShowImportFailed>(Dispatchers.Main, view::showImportFailed)
        addActionSync<Import.F.ShowPasswordInput>(Dispatchers.Main, view::showPasswordInput)
        addActionSync<Import.F.ShowBalanceTooLow>(Dispatchers.Main, view::showBalanceTooLow)
        addActionSync<Import.F.ShowImportSuccess>(Dispatchers.Main, view::showImportSuccess)
        addActionSync<Import.F.ShowPasswordInvalid>(Dispatchers.Main, view::showPasswordInvalid)
        addActionSync<Import.F.ShowNoWalletsEnabled>(Dispatchers.Main, view::showNoWalletsEnabled)
    }

    private fun List<Wallet>.filterBtcLike(): List<Wallet> =
        filter { wallet ->
            wallet.currency.code.run { isBitcoin() || isBitcoinCash() }
        }

    @UseExperimental(ExperimentalCoroutinesApi::class)
    private fun handleValidateKey(
        breadBox: BreadBox
    ) = flowTransformer<Import.F.ValidateKey, Import.E> { effects ->
        effects.mapLatest { effect ->
            val keyBytes = effect.privateKey.toByteArray()
            val passwordBytes = effect.password?.toByteArray() ?: byteArrayOf()
            val wallets = breadBox.wallets().first().filterBtcLike()

            // Sweeping only supports BTC and BCH, ensure one is active.
            when {
                wallets.isEmpty() -> Import.E.Key.NoWallets
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
    }

    @UseExperimental(ExperimentalCoroutinesApi::class)
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

        val btcWallets = breadBox.wallets().first().filterBtcLike()

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
            when (val result = walletImporter.estimateFee()) {
                is WalletImporter.FeeResult.Success ->
                    Import.E.Estimate.Success(
                        walletFound.balance,
                        result.feeBasis.fee,
                        walletFound.currencyCode
                    )
                is WalletImporter.FeeResult.InsufficientFunds ->
                    Import.E.Estimate.BalanceTooLow(walletFound.balance.toBigDecimal())
                is WalletImporter.FeeResult.Failed ->
                    Import.E.Estimate.FeeError(walletFound.balance.toBigDecimal())
                else ->
                    Import.E.Estimate.FeeError(walletFound.balance.toBigDecimal())
            }
        }
    }

    @UseExperimental(ExperimentalCoroutinesApi::class)
    private fun handleSubmitTransfer(
        walletImporter: WalletImporter
    ): suspend (Import.F.SubmitImport) -> Import.E = { submitTransfer ->
        val privateKey = submitTransfer.privateKey.toByteArray()
        val password = submitTransfer.password?.toByteArray()
        if (!walletImporter.readyForImport(privateKey, password)) {
            Import.E.RetryImport(submitTransfer.privateKey, submitTransfer.password)
        } else {
            val transfer = walletImporter.import()
            if (transfer != null) {
                Import.E.Transfer.OnSuccess(
                    transferHash = transfer.hashString(),
                    currencyCode = transfer.wallet.currency.code
                )
            } else {
                Import.E.Transfer.OnFailed
            }
        }
    }
}

