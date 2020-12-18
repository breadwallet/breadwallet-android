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
package com.breadwallet.breadbox

import com.breadwallet.crypto.TransferAttribute
import com.breadwallet.crypto.Account
import com.breadwallet.crypto.Address
import com.breadwallet.crypto.Amount
import com.breadwallet.crypto.Currency
import com.breadwallet.crypto.ExportablePaperWallet
import com.breadwallet.crypto.Key
import com.breadwallet.crypto.Network
import com.breadwallet.crypto.NetworkFee
import com.breadwallet.crypto.System
import com.breadwallet.crypto.TransferFeeBasis
import com.breadwallet.crypto.Unit
import com.breadwallet.crypto.Wallet
import com.breadwallet.crypto.WalletManager
import com.breadwallet.crypto.WalletSweeper
import com.breadwallet.crypto.errors.AccountInitializationError
import com.breadwallet.crypto.errors.ExportablePaperWalletError
import com.breadwallet.crypto.errors.FeeEstimationError
import com.breadwallet.crypto.errors.LimitEstimationError
import com.breadwallet.crypto.errors.WalletSweeperError
import com.breadwallet.util.asyncApiCall
import java.math.BigDecimal
import java.math.RoundingMode

/** Returns the [Address] object for [address] from the [Wallet]'s [Network] */
fun Wallet.addressFor(address: String): Address? {
    return Address.create(address, walletManager.network).orNull()
}

/**
 * By default [WalletManager.getDefaultNetworkFee] is used for the [networkFee].
 */
suspend fun Wallet.estimateFee(
    address: Address,
    amount: Amount,
    networkFee: NetworkFee = walletManager.defaultNetworkFee,
    attrs: Set<TransferAttribute> = emptySet()
): TransferFeeBasis = asyncApiCall<TransferFeeBasis, FeeEstimationError> {
    estimateFee(address, amount, networkFee, attrs, this)
}

suspend fun WalletManager.createSweeper(wallet: Wallet, key: Key): WalletSweeper =
    asyncApiCall<WalletSweeper, WalletSweeperError> { createSweeper(wallet, key, this) }

suspend fun WalletSweeper.estimateFee(networkFee: NetworkFee): TransferFeeBasis =
    asyncApiCall<TransferFeeBasis, FeeEstimationError> { estimate(networkFee, this) }

suspend fun System.accountInitialize(
    account: Account,
    network: Network,
    create: Boolean
): ByteArray = asyncApiCall<ByteArray, AccountInitializationError> {
    accountInitialize(account, network, create, this)
}

suspend fun Wallet.estimateMaximum(
    address: Address,
    networkFee: NetworkFee
): Amount = asyncApiCall<Amount, LimitEstimationError> {
    estimateLimitMaximum(address, networkFee, this)
}

suspend fun WalletManager.createExportablePaperWallet(): ExportablePaperWallet =
    asyncApiCall<ExportablePaperWallet, ExportablePaperWalletError> {
        createExportablePaperWallet(this)
    }

/** Returns the [Amount] as a [BigDecimal]. */
fun Amount.toBigDecimal(
    unit: Unit = this.unit,
    roundingMode: RoundingMode = RoundingMode.HALF_EVEN
): BigDecimal {
    return BigDecimal(doubleAmount(unit).or(0.0))
        .setScale(unit.decimals.toInt(), roundingMode)
}

fun Currency.isNative() = type.equals("native", true)

fun Currency.isErc20() = type.equals("erc20", true)

fun Currency.isEthereum() =
    uids.equals("ethereum-mainnet:__native__", true) ||
        uids.equals("ethereum-ropsten:__native__", true)

fun Currency.isBitcoin() =
    uids.equals("bitcoin-mainnet:__native__", true) ||
        uids.equals("bitcoin-testnet:__native__", true)

fun Currency.isBitcoinCash() =
    uids.equals("bitcoincash-mainnet:__native__", true) ||
        uids.equals("bitcoincash-testnet:__native__", true)

fun Currency.isTezos() = uids.equals("tezos-mainnet:__native__", true)

/** Returns the default [Unit] for the [Wallet]'s [Network]. */
val Wallet.defaultUnit: Unit
    get() = walletManager.network.defaultUnitFor(currency).get()

/** Returns the base [Unit] for the [Wallet]'s [Network] */
val Wallet.baseUnit: Unit
    get() = walletManager.network.baseUnitFor(currency).get()

fun Wallet.feeForSpeed(speed: TransferSpeed): NetworkFee {
    if (currency.isTezos()) {
        return checkNotNull(
            walletManager.network.fees.minByOrNull {
                it.confirmationTimeInMilliseconds.toLong()
            }
        )
    }
    val fees = walletManager.network.fees
    return when (fees.size) {
        1 -> fees.single()
        else -> fees
            .filter { it.confirmationTimeInMilliseconds.toLong() <= speed.targetTime }
            .minByOrNull { fee ->
                speed.targetTime - fee.confirmationTimeInMilliseconds.toLong()
            }
            ?: fees.minByOrNull { it.confirmationTimeInMilliseconds }
            ?: walletManager.defaultNetworkFee
    }
}
