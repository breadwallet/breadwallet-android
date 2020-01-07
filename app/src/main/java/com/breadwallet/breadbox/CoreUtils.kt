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
import com.breadwallet.crypto.Address
import com.breadwallet.crypto.AddressScheme
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
import com.breadwallet.crypto.Wallet
import com.breadwallet.crypto.WalletManager
import com.breadwallet.crypto.WalletManagerMode
import com.breadwallet.crypto.WalletManagerState
import com.breadwallet.crypto.WalletSweeper
import com.breadwallet.crypto.errors.FeeEstimationError
import com.breadwallet.crypto.errors.WalletSweeperError
import com.breadwallet.crypto.utility.CompletionHandler
import com.breadwallet.logger.logError
import com.breadwallet.tools.manager.BRSharedPrefs
import com.breadwallet.tools.util.BRConstants
import com.breadwallet.util.WalletDisplayUtils
import com.breadwallet.util.isBitcoin
import com.breadwallet.util.isBitcoinCash
import com.breadwallet.util.isEthereum
import com.google.common.primitives.UnsignedInteger
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.flow.transform
import kotlinx.coroutines.isActive
import java.math.BigDecimal
import java.text.DecimalFormat
import java.text.NumberFormat
import java.util.Locale
import java.util.concurrent.TimeUnit
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.suspendCoroutine
import kotlin.math.absoluteValue

/** Default port for [NetworkPeer] */
private const val DEFAULT_PORT = 8333L

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
fun Currency.isNative() = type.equals("native", true)

/** True when this is an erc20 token for the Ethereum network. */
fun Currency.isErc20() = type.equals("erc20", true)

/** True when this is Ethereum. */
fun Currency.isEthereum() = code.isEthereum() && !isErc20()

fun Currency.isBitcoin() = code.isBitcoin()

fun Currency.isBitcoinCash() = code.isBitcoinCash()

/** Returns the [Transfer]'s hash or an empty string. */
fun Transfer.hashString(): String =
    hash.orNull()?.toString() ?: ""

/** Returns the [Address] object for [address] from the [Wallet]'s [Network]*/
fun Wallet.addressFor(address: String): Address? {
    return Address.create(address, walletManager.network).orNull()
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
                if (isActive) {
                    offer(data)
                    close()
                }
            }

            override fun handleError(error: FeeEstimationError) {
                if (isActive) close(error)
            }
        }
    )

    awaitClose()
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

private const val ECONOMY_FEE_HOURS = 10L
private const val REGULAR_FEE_HOURS = 1L

enum class TransferSpeed(val targetTime: Long) {
    ECONOMY(TimeUnit.HOURS.toMillis(ECONOMY_FEE_HOURS)),
    REGULAR(TimeUnit.HOURS.toMillis(REGULAR_FEE_HOURS)),
    PRIORITY(0L);
}

fun Wallet.feeForSpeed(speed: TransferSpeed): NetworkFee {
    val fees = walletManager.network.fees
    return when (fees.size) {
        1 -> fees.single()
        else -> fees.minBy { fee ->
            (fee.confirmationTimeInMilliseconds.toLong() - speed.targetTime).absoluteValue
        } ?: walletManager.defaultNetworkFee
    }
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

/** Returns the [Currency] code if the [Transfer] is a ETH fee transfer, blank otherwise. */
fun Transfer.feeForToken(): String {
    val issuerCode = findIssuerCurrency()
    return when {
        !wallet.walletManager.currency.isEthereum() -> ""
        issuerCode == null || issuerCode.isEthereum() -> ""
        else -> issuerCode.code
    }
}

/** Returns the [Currency] for the issuer matching the target address, null otherwise. */
fun Transfer.findIssuerCurrency() =
    wallet.walletManager.network.currencies.find { networkCurrency ->
        networkCurrency
            .issuer
            .or("")
            .equals(target.get().toSanitizedString(), true)
    }

fun WalletManagerState.isTracked() =
    type == WalletManagerState.Type.CREATED ||
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
val Wallet.urlScheme: String
    get() = when {
        currency.code.isEthereum() || currency.isErc20() -> "ethereum"
        currency.code.isBitcoin() -> "bitcoin"
        currency.code.isBitcoinCash() -> when {
            BuildConfig.BITCOIN_TESTNET -> "bchtest"
            else -> "bitcoincash"
        }
        else -> ""
    }

/** Creates a [WalletManager] using the appropriate address scheme and [WalletManagerMode]. */
fun System.createWalletManager(
    network: Network,
    managerMode: WalletManagerMode?,
    currencies: Set<Currency>,
    addressScheme: AddressScheme? = getAddressScheme(network)
) {
    val wmMode = when {
        managerMode == null -> network.defaultWalletManagerMode
        network.supportsWalletManagerMode(managerMode) -> managerMode
        else -> network.defaultWalletManagerMode
    }
    createWalletManager(network, wmMode, addressScheme, currencies)
}

/** Returns a [Flow] providing the [Network] from [System] for a given [currencyId]. */
fun Flow<System>.findNetwork(currencyId: String): Flow<Network> =
    transform { system ->
        emit(system.networks.find {
            it.containsCurrency(currencyId)
        } ?: return@transform)

    }

/** Return the [AddressScheme] to be used by the given network */
fun System.getAddressScheme(network: Network): AddressScheme {
    return when {
        network.currency.code.isBitcoin() -> {
            if (BRSharedPrefs.getIsSegwitEnabled()) {
                AddressScheme.BTC_SEGWIT
            } else {
                AddressScheme.BTC_LEGACY
            }
        }
        else -> network.defaultAddressScheme
    }
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



