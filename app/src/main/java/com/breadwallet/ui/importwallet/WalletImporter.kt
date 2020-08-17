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

import com.breadwallet.breadbox.TransferSpeed
import com.breadwallet.breadbox.createSweeper
import com.breadwallet.breadbox.estimateFee
import com.breadwallet.breadbox.feeForSpeed
import com.breadwallet.crypto.Amount
import com.breadwallet.crypto.Key
import com.breadwallet.crypto.Transfer
import com.breadwallet.crypto.TransferFeeBasis
import com.breadwallet.crypto.Wallet
import com.breadwallet.crypto.WalletSweeper
import com.breadwallet.crypto.errors.WalletSweeperError
import com.breadwallet.crypto.errors.WalletSweeperInsufficientFundsError
import com.breadwallet.crypto.errors.WalletSweeperNoTransfersFoundError
import com.breadwallet.logger.logError
import com.breadwallet.util.CurrencyCode

/**
 * Import a wallet using a private key, supports only BTC and BCH.
 *
 * Set a valid key using [setKey] then call [prepareSweeper] to scan
 * the wallet and report the balance.  Next estimate the transfer
 * fee using [estimateFee].  If the fee is approved, call [import]
 * to submit the transaction.
 *
 * Before running [import], [readyForImport] can be called with the
 * private key information to ensure the [WalletImporter] is ready
 * to submit the transfer.
 */
class WalletImporter {
    private var key: Key? = null
    private var targetWallet: Wallet? = null
    private var walletSweeper: WalletSweeper? = null
    private var transferFeeBasis: TransferFeeBasis? = null

    fun readyForImport(privateKey: ByteArray, password: ByteArray?): Boolean {
        val newKey = when (password) {
            null -> Key.createFromPrivateKeyString(privateKey)
            else -> Key.createFromPrivateKeyString(privateKey, password)
        }.orNull()
        return this.key?.privateKeyMatch(newKey) == true &&
            targetWallet != null &&
            walletSweeper != null &&
            transferFeeBasis != null
    }

    fun setKey(privateKey: ByteArray, password: ByteArray?) {
        require(privateKey.isNotEmpty())

        key = when (password) {
            null -> Key.createFromPrivateKeyString(privateKey)
            else -> Key.createFromPrivateKeyString(privateKey, password)
        }.orNull()

        checkNotNull(key) {
            "Failed to create Key from privateKey and password"
        }
    }

    suspend fun prepareSweeper(wallet: Wallet): PrepareResult {
        val key = checkNotNull(key) {
            "setKey() must first be called with a valid private key."
        }
        if (walletSweeper != null) {
            walletSweeper = null
        }
        return try {
            val sweeper = wallet.walletManager.createSweeper(wallet, key)
            targetWallet = wallet
            walletSweeper = sweeper
            val balance = checkNotNull(sweeper.balance.orNull())
                .convert(wallet.unit)
                .orNull() ?: Amount.create(0, wallet.unit)
            PrepareResult.WalletFound(balance, wallet.currency.code)
        } catch (e: WalletSweeperError) {
            when (e) {
                is WalletSweeperInsufficientFundsError ->
                    PrepareResult.NoBalance
                is WalletSweeperNoTransfersFoundError ->
                    PrepareResult.NoBalance
                else -> {
                    logError("Failed to create wallet sweeper", e)
                    PrepareResult.Failed
                }
            }
        } catch (e: IllegalArgumentException) {
            PrepareResult.Failed
        }
    }

    suspend fun estimateFee(): FeeResult {
        if (transferFeeBasis != null) {
            transferFeeBasis = null
        }
        val targetWallet = checkNotNull(targetWallet) {
            "prepareSweep() must first be called with the target wallet."
        }
        val walletSweeper = checkNotNull(walletSweeper)

        val networkFee = targetWallet.feeForSpeed(TransferSpeed.Regular(targetWallet.currency.code))
        return try {
            val feeBasis = walletSweeper.estimateFee(networkFee)
            transferFeeBasis = feeBasis
            // TODO: InsufficientFundsErrors never occur, this check
            //   should not be required.
            if (Amount.create(0, feeBasis.unit) == feeBasis.fee) {
                FeeResult.InsufficientFunds
            } else {
                FeeResult.Success(feeBasis)
            }
        } catch (e: WalletSweeperError) {
            when (e) {
                is WalletSweeperInsufficientFundsError ->
                    FeeResult.InsufficientFunds
                else -> {
                    logError("Failed to estimate fee", e)
                    FeeResult.Failed
                }
            }
        }
    }

    fun import(): Transfer? {
        val walletSweeper = checkNotNull(walletSweeper) {
            "Not ready for import, check with readyForImport()."
        }
        val transferFeeBasis = checkNotNull(transferFeeBasis)

        return try {
            checkNotNull(walletSweeper.submit(transferFeeBasis).orNull())
        } catch (e: WalletSweeperError) {
            logError("Failed to submit import transfer", e)
            null
        } finally {
            reset()
        }
    }

    private fun reset() {
        key = null
        walletSweeper = null
        transferFeeBasis = null
        targetWallet = null
    }

    sealed class FeeResult {
        data class Success(
            val feeBasis: TransferFeeBasis
        ) : FeeResult()

        object InsufficientFunds : FeeResult()
        object Failed : FeeResult()
    }

    sealed class PrepareResult {
        data class WalletFound(
            val balance: Amount,
            val currencyCode: CurrencyCode
        ) : PrepareResult()

        object NoBalance : PrepareResult()
        object Failed : PrepareResult()
    }
}
