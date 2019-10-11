/**
 * BreadWallet
 *
 * Created by Drew Carlson <drew.carlson@breadwallet.com> on 9/25/19.
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
@file:Suppress("EXPERIMENTAL_API_USAGE", "TooManyFunctions")

package com.breadwallet.breadbox

import com.breadwallet.crypto.Address
import com.breadwallet.crypto.Amount
import com.breadwallet.crypto.Currency
import com.breadwallet.crypto.Network
import com.breadwallet.crypto.Transfer
import com.breadwallet.crypto.Wallet
import com.breadwallet.logger.logError
import com.breadwallet.tools.util.BRConstants
import com.breadwallet.util.WalletDisplayUtils
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import com.breadwallet.crypto.NetworkFee
import com.breadwallet.crypto.TransferFeeBasis
import com.breadwallet.crypto.errors.FeeEstimationError
import com.breadwallet.crypto.utility.CompletionHandler
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.isActive
import java.math.BigDecimal
import java.text.DecimalFormat
import java.text.NumberFormat
import java.util.Locale

/** Returns the [Amount] as a [BigDecimal]. */
fun Amount.toBigDecimal(): BigDecimal {
    return BigDecimal(doubleAmount(unit).or(0.0))
        .setScale(unit.decimals.toInt(), BRConstants.ROUNDING_MODE)
}

/** Returns the [Address] string removing any address prefixes. */
fun Address.toSanitizedString(): String =
    toString()
        .removePrefix("bitcoincash:")
        .removePrefix("bchtest:")

/** True when this is a native currency for the network. */
fun Currency.isNative() = type == "native"

/** True when this is an erc20 token for the Ethereum network. */
fun Currency.isErc20() = type == "erc20"

/** Returns the [Transfer]'s hash or an empty string. */
fun Transfer.hashString(): String =
    hash.orNull()?.toString() ?: ""

/** Returns the [Address] object for [address] from the [Wallet]'s [Network]*/
fun Wallet.addressFor(address: String): Address? {
    return walletManager.network.addressFor(address).orNull()
}

/**
 * Returns a [Flow] of [TransferFeeBasis] for the transfer
 * of [amount] to to the given [address].
 *
 * By default [com.breadwallet.corecrypto.WalletManager.getDefaultNetworkFee]
 * is used for the [networkFee].
 *
 * Nothing will be emitted if provided an invalid [address].
 */
fun Wallet.estimateFee(
    address: String,
    amount: Amount,
    networkFee: NetworkFee = walletManager.defaultNetworkFee
): Flow<TransferFeeBasis> = callbackFlow {
    estimateFee(
        addressFor(address) ?: return@callbackFlow,
        amount,
        networkFee,
        object : CompletionHandler<TransferFeeBasis, FeeEstimationError> {
            override fun handleData(data: TransferFeeBasis) {
                if (isActive) offer(data)
            }

            override fun handleError(error: FeeEstimationError) {
                if (isActive) close(error)
            }
        }
    )

    awaitClose()
}

/** Returns a [Flow] which will eventually emit a new [Transfer] or an error. */
fun Wallet.sendTransfer(
    phrase: ByteArray,
    address: String,
    amount: Amount,
    transferFeeBasis: TransferFeeBasis
): Flow<Transfer> = flow {
    val transfer = createTransfer(
        checkNotNull(addressFor(address)),
        amount,
        transferFeeBasis
    ).get()

    walletManager.submit(transfer, phrase)

    emit(transfer)
}

// TODO: Move somewhere UI related
fun BigDecimal.formatCryptoForUi(
    currencyCode: String,
    scale: Int = WalletDisplayUtils.MAX_DECIMAL_PLACES_FOR_UI,
    negate: Boolean = false
): String {
    val amount = if (negate) negate() else this

    val currencyFormat = DecimalFormat.getCurrencyInstance(Locale.getDefault()) as DecimalFormat
    val decimalFormatSymbols = currencyFormat.decimalFormatSymbols
    currencyFormat.isGroupingUsed = true
    currencyFormat.roundingMode = BRConstants.ROUNDING_MODE
    decimalFormatSymbols.currencySymbol = ""
    currencyFormat.decimalFormatSymbols = decimalFormatSymbols
    currencyFormat.maximumFractionDigits = scale
    currencyFormat.minimumFractionDigits = 0
    return "${currencyFormat.format(amount)} ${currencyCode.toUpperCase()}"
}

// TODO: Move somewhere UI related
fun BigDecimal.formatFiatForUi(currencyCode: String): String {
    val currencyFormat = NumberFormat.getCurrencyInstance(Locale.getDefault()) as DecimalFormat
    val decimalFormatSymbols = currencyFormat.decimalFormatSymbols
    currencyFormat.isGroupingUsed = true
    currencyFormat.roundingMode = BRConstants.ROUNDING_MODE
    try {
        val currency = java.util.Currency.getInstance(currencyCode)
        val symbol = currency.symbol
        decimalFormatSymbols.currencySymbol = symbol
        currencyFormat.decimalFormatSymbols = decimalFormatSymbols
        currencyFormat.negativePrefix = "-$symbol"
        currencyFormat.maximumFractionDigits = currency.defaultFractionDigits
        currencyFormat.minimumFractionDigits = currency.defaultFractionDigits
    } catch (e: IllegalArgumentException) {
        logError("Illegal Currency code: $currencyCode")
    }

    return currencyFormat.format(this)
}

val Wallet.currencyId: String
    get() = walletManager.network.currency.uids

/** Returns the [Wallet] with the given [currencyId] or null. */
fun List<Wallet>.findByCurrencyId(currencyId: String) =
    find { it.walletManager.network.currency.uids.equals(currencyId, true) }

/** Returns the [Network] with the given [currencyId] or null. */
fun List<Network>.findByCurrencyId(currencyId: String) =
    find { it.currency.uids.equals(currencyId, true) }

/** Returns [Wallet] [Flow] sorted by [displayOrderCurrencyIds]. */
fun Flow<List<Wallet>>.applyDisplayOrder(displayOrderCurrencyIds: Flow<List<String>>) =
    combine(displayOrderCurrencyIds) { systemWallets, currencyIds ->
        currencyIds.mapNotNull {
            systemWallets.findByCurrencyId(it)
        }
    }
