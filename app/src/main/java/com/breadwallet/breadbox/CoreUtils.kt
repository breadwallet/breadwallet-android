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

import com.breadwallet.BuildConfig
import com.breadwallet.crypto.Account
import com.breadwallet.crypto.Address
import com.breadwallet.crypto.Amount
import com.breadwallet.crypto.Currency
import com.breadwallet.crypto.Key
import com.breadwallet.crypto.Network
import com.breadwallet.crypto.NetworkFee
import com.breadwallet.crypto.NetworkPeer
import com.breadwallet.crypto.System
import com.breadwallet.crypto.Transfer
import com.breadwallet.crypto.TransferDirection
import com.breadwallet.crypto.TransferFeeBasis
import com.breadwallet.crypto.Unit
import com.breadwallet.crypto.Wallet
import com.breadwallet.crypto.WalletManager
import com.breadwallet.crypto.WalletManagerMode
import com.breadwallet.crypto.WalletManagerState
import com.breadwallet.crypto.WalletSweeper
import com.breadwallet.crypto.errors.AccountInitializationError
import com.breadwallet.crypto.errors.FeeEstimationError
import com.breadwallet.crypto.errors.LimitEstimationError
import com.breadwallet.crypto.errors.WalletSweeperError
import com.breadwallet.crypto.utility.CompletionHandler
import com.breadwallet.logger.logError
import com.breadwallet.tools.util.BRConstants
import com.breadwallet.util.isBitcoin
import com.breadwallet.util.isBitcoinCash
import com.breadwallet.util.isEthereum
import com.breadwallet.util.isRipple
import com.google.common.primitives.UnsignedInteger
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.mapNotNull
import java.math.BigDecimal
import java.text.DecimalFormat
import java.text.NumberFormat
import java.util.Locale
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.suspendCoroutine

/** Default port for [NetworkPeer] */
private const val DEFAULT_PORT = 8333L

/** Returns the [Amount] as a [BigDecimal]. */
fun Amount.toBigDecimal(unit: Unit = this.unit): BigDecimal {
    return BigDecimal(doubleAmount(unit).or(0.0))
        .setScale(unit.decimals.toInt(), BRConstants.ROUNDING_MODE)
}

/** Returns the [Address] string removing any address prefixes. */
fun Address.toSanitizedString(): String =
    toString()
        .removePrefix("bitcoincash:")
        .removePrefix("bchtest:")

/** True when this is a native currency for the network. */
fun Currency.isNative() = type.equals("native", true)

/** True when this is an erc20 token for the Ethereum network. */
fun Currency.isErc20() = type.equals("erc20", true)

/** True when this is Ethereum. */
fun Currency.isEthereum() = code.isEthereum() && !isErc20()

fun Currency.isBitcoin() = code.isBitcoin()

fun Currency.isBitcoinCash() = code.isBitcoinCash()

/** Returns the [Transfer]'s hash or an empty string. */
fun Transfer.hashString(): String =
    checkNotNull(hash.orNull()).toString()
        .let { hash ->
            val isEthHash = wallet.currency.run { isErc20() || isEthereum() }
            when {
                isEthHash -> hash
                else -> hash.removePrefix("0x")
            }
        }

/** Returns the [Address] object for [address] from the [Wallet]'s [Network]*/
fun Wallet.addressFor(address: String): Address? {
    return Address.create(address, walletManager.network).orNull()
}

/**
 * By default [com.breadwallet.corecrypto.WalletManager.getDefaultNetworkFee]
 * is used for the [networkFee].
 */
suspend fun Wallet.estimateFee(
    address: Address,
    amount: Amount,
    networkFee: NetworkFee = walletManager.defaultNetworkFee
): TransferFeeBasis = suspendCoroutine { continuation ->
    val handler = object : CompletionHandler<TransferFeeBasis, FeeEstimationError> {
        override fun handleData(data: TransferFeeBasis) {
            continuation.resume(data)
        }

        override fun handleError(error: FeeEstimationError) {
            continuation.resumeWithException(error)
        }
    }
    estimateFee(address, amount, networkFee, handler)
}

suspend fun WalletManager.createSweeper(
    wallet: Wallet,
    key: Key
): WalletSweeper = suspendCoroutine { continuation ->
    val handler = object : CompletionHandler<WalletSweeper, WalletSweeperError> {
        override fun handleData(sweeper: WalletSweeper) {
            continuation.resume(sweeper)
        }

        override fun handleError(error: WalletSweeperError) {
            continuation.resumeWithException(error)
        }
    }
    createSweeper(wallet, key, handler)
}

suspend fun WalletSweeper.estimateFee(networkFee: NetworkFee): TransferFeeBasis =
    suspendCoroutine { continuation ->
        val handler = object : CompletionHandler<TransferFeeBasis, FeeEstimationError> {
            override fun handleData(feeBasis: TransferFeeBasis) {
                continuation.resume(feeBasis)
            }

            override fun handleError(error: FeeEstimationError) {
                continuation.resumeWithException(error)
            }
        }
        estimate(networkFee, handler)
    }

fun Wallet.feeForSpeed(speed: TransferSpeed): NetworkFee {
    val fees = walletManager.network.fees
    return when (fees.size) {
        1 -> fees.single()
        else -> fees
            .filter { it.confirmationTimeInMilliseconds.toLong() <= speed.targetTime }
            .minBy { fee ->
                speed.targetTime - fee.confirmationTimeInMilliseconds.toLong()
            } ?: fees.minBy { it.confirmationTimeInMilliseconds } ?: walletManager.defaultNetworkFee
    }
}

// TODO: Move somewhere UI related
fun BigDecimal.formatCryptoForUi(
    currencyCode: String,
    scale: Int = 5,
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
fun BigDecimal.formatFiatForUi(currencyCode: String, scale: Int? = null): String {
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
        currencyFormat.maximumFractionDigits = scale ?: currency.defaultFractionDigits
        currencyFormat.minimumFractionDigits = scale ?: currency.defaultFractionDigits
    } catch (e: IllegalArgumentException) {
        logError("Illegal Currency code: $currencyCode")
    }

    return currencyFormat.format(this)
}

val Wallet.currencyId: String
    get() = currency.uids

fun List<Wallet>.filterByCurrencyIds(currencyIds: List<String>) =
    filter { wallet ->
        currencyIds.any {
            it.equals(
                wallet.currencyId,
                true
            )
        }
    }

/** Returns the [Wallet] with the given [currencyId] or null. */
fun List<Wallet>.findByCurrencyId(currencyId: String) =
    find { it.currencyId.equals(currencyId, true) }

/** Returns true if any of the [Wallet]s is for the given [currencyId]. */
fun List<Wallet>.containsCurrency(currencyId: String) =
    findByCurrencyId(currencyId) != null

/** Returns true if the [WalletManager]'s [Network] supports the given [currencyId]. */
fun WalletManager.networkContainsCurrency(currencyId: String) =
    network.containsCurrency(currencyId)

/** Returns the [Currency] if the [WalletManager]'s [Network] supports the given [currencyId], null otherwise. */
fun WalletManager.findCurrency(currencyId: String) =
    network.findCurrency(currencyId)

/** Returns true if the [Network] supports the given [currencyId]. */
fun Network.containsCurrency(currencyId: String) =
    findCurrency(currencyId) != null

/** Returns the [Currency] if the [Network] supports the given [currencyId], null otherwise. */
fun Network.findCurrency(currencyId: String) =
    currencies.find { networkCurrency ->
        networkCurrency.uids.equals(
            currencyId,
            true
        )
    }

/** Returns true if the [Network] supports the given [currencyCode]. */
fun Network.containsCurrencyCode(currencyCode: String) =
    currencies.find { networkCurrency ->
        networkCurrency.code.equals(
            currencyCode,
            true
        )
    } != null

/** Returns the [Currency] code if the [Transfer] is a ETH fee transfer, blank otherwise. */
fun Transfer.feeForToken(): String {
    val targetAddress = target.orNull()?.toSanitizedString() ?: return ""
    val issuerCode = wallet.walletManager.network.currencies.find { networkCurrency ->
        networkCurrency.issuer.or("").equals(targetAddress, true)
    }
    return when {
        !wallet.walletManager.currency.isEthereum() -> ""
        issuerCode == null || issuerCode.isEthereum() -> ""
        else -> issuerCode.code
    }
}

fun WalletManagerState.isTracked() =
    type == WalletManagerState.Type.CONNECTED ||
        type == WalletManagerState.Type.SYNCING

/** Returns [Wallet] [Flow] sorted by [displayOrderCurrencyIds]. */
fun Flow<List<Wallet>>.applyDisplayOrder(displayOrderCurrencyIds: Flow<List<String>>) =
    combine(displayOrderCurrencyIds) { systemWallets, currencyIds ->
        currencyIds.mapNotNull {
            systemWallets.findByCurrencyId(it)
        }
    }

/** Returns the url scheme for a payment request with this wallet. */
val Wallet.urlScheme: String?
    get() = when {
        currency.code.isEthereum() || currency.isErc20() -> "ethereum"
        currency.code.isRipple() -> "xrp"
        currency.code.isBitcoin() -> "bitcoin"
        currency.code.isBitcoinCash() -> when {
            BuildConfig.BITCOIN_TESTNET -> "bchtest"
            else -> "bitcoincash"
        }
        else -> null
    }

val Wallet.urlSchemes: List<String>
    get() = when {
        currency.code.isRipple() -> listOf(urlScheme!!, "xrpl", "ripple")
        else -> urlScheme?.run(::listOf) ?: emptyList()
    }

/** Return a [NetworkPeer] pointing to the given address */
fun Network.getPeerOrNull(node: String): NetworkPeer? {
    val nodeInfo = node.split(":")
    if (nodeInfo.isEmpty()) return null

    val address = nodeInfo[0]
    val port = if (nodeInfo.size > 1) {
        UnsignedInteger.valueOf(nodeInfo[1])
    } else {
        UnsignedInteger.valueOf(DEFAULT_PORT)
    }
    return createPeer(address, port, null).orNull()
}

/** True when the [Transfer] was received. */
fun Transfer.isReceived(): Boolean = direction == TransferDirection.RECEIVED

fun Transfer.getSize(): Double? {
    val currencyCode = wallet.currency.code
    return when {
        currencyCode.isBitcoin() || currencyCode.isBitcoinCash() ->
            (confirmedFeeBasis.orNull() ?: estimatedFeeBasis.orNull())?.costFactor
        else -> null
    }
}

/** Returns a [Flow] providing the default [WalletManagerMode] from [System] for a given [currencyId]. */
fun Flow<System>.getDefaultWalletManagerMode(currencyId: String): Flow<WalletManagerMode> =
    mapNotNull { system ->
        system.networks
            .find { it.containsCurrency(currencyId) }
            ?.run { defaultWalletManagerMode }
    }

/** Returns the default [Unit] for a given [Wallet] */
val Wallet.defaultUnit: Unit
    get() = walletManager
        .network
        .defaultUnitFor(currency)
        .get()

/** Returns the base [Unit] for a given [Wallet] */
val Wallet.baseUnit: Unit
    get() = walletManager
        .network
        .baseUnitFor(currency)
        .get()

suspend fun System.accountInitialize(
    account: Account,
    network: Network,
    create: Boolean
): ByteArray = suspendCoroutine { continuation ->
    val handler = object : CompletionHandler<ByteArray, AccountInitializationError> {
        override fun handleData(data: ByteArray) {
            continuation.resume(data)
        }

        override fun handleError(error: AccountInitializationError) {
            continuation.resumeWithException(error)
        }
    }
    accountInitialize(account, network, create, handler)
}

suspend fun Wallet.estimateMaximum(
    address: Address,
    networkFee: NetworkFee
): Amount = suspendCoroutine { continuation ->
    val handler = object : CompletionHandler<Amount, LimitEstimationError> {
        override fun handleData(limitMaxAmount: Amount?) {
            if (limitMaxAmount == null) {
                continuation.resumeWithException(Exception("Limit Estimation is null"))
                return
            }
            continuation.resume(limitMaxAmount)
        }

        override fun handleError(error: LimitEstimationError?) {
            continuation.resumeWithException(
                error ?: Exception("Unknown Limit Estimation Error")
            )
        }
    }

    estimateLimitMaximum(
        address,
        networkFee,
        handler
    )
}

