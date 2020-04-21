/**
 * BreadWallet
 *
 * Created by Ahsan Butt <ahsan.butt@breadwallet.com> on 1/15/20.
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
package com.platform.jsbridge

import android.content.Context
import android.security.keystore.UserNotAuthenticatedException
import android.webkit.JavascriptInterface
import com.breadwallet.BuildConfig
import com.breadwallet.R
import com.breadwallet.breadbox.BreadBox
import com.breadwallet.breadbox.TransferSpeed
import com.breadwallet.breadbox.addressFor
import com.breadwallet.breadbox.baseUnit
import com.breadwallet.breadbox.containsCurrency
import com.breadwallet.breadbox.currencyId
import com.breadwallet.breadbox.defaultUnit
import com.breadwallet.breadbox.estimateFee
import com.breadwallet.breadbox.feeForSpeed
import com.breadwallet.breadbox.findCurrency
import com.breadwallet.breadbox.hashString
import com.breadwallet.breadbox.isBitcoin
import com.breadwallet.breadbox.isNative
import com.breadwallet.breadbox.toBigDecimal
import com.breadwallet.breadbox.toSanitizedString
import com.breadwallet.crypto.Address
import com.breadwallet.crypto.AddressScheme
import com.breadwallet.crypto.Amount
import com.breadwallet.crypto.Transfer
import com.breadwallet.crypto.TransferFeeBasis
import com.breadwallet.crypto.TransferState
import com.breadwallet.crypto.Wallet
import com.breadwallet.crypto.errors.FeeEstimationError
import com.breadwallet.logger.logDebug
import com.breadwallet.crypto.errors.LimitEstimationError
import com.breadwallet.crypto.utility.CompletionHandler
import com.breadwallet.logger.logError
import com.breadwallet.repository.RatesRepository
import com.breadwallet.tools.manager.BRSharedPrefs
import com.breadwallet.tools.security.BRAccountManager
import com.breadwallet.tools.util.EventUtils
import com.breadwallet.tools.util.TokenUtil
import com.platform.ConfirmTransactionMessage
import com.platform.PlatformTransactionBus
import com.platform.TransactionResultMessage
import com.platform.entities.TxMetaDataValue
import com.platform.interfaces.AccountMetaDataProvider
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.transform
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import java.math.BigDecimal
import java.util.Currency
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.suspendCoroutine

class WalletJs(
    private val nativePromiseFactory: NativePromiseFactory,
    private val context: Context,
    private val metaDataProvider: AccountMetaDataProvider,
    private val breadBox: BreadBox,
    private val ratesRepository: RatesRepository,
    private val accountManager: BRAccountManager
) : JsApi {
    companion object {
        private const val KEY_BTC_DENOMINATION_DIGITS = "btc_denomination_digits"
        private const val KEY_LOCAL_CURRENCY_CODE = "local_currency_code"
        private const val KEY_LOCAL_CURRENCY_PRECISION = "local_currency_precision"
        private const val KEY_LOCAL_CURRENCY_SYMBOL = "local_currency_symbol"
        private const val KEY_APP_PLATFORM = "app_platform"
        private const val KEY_APP_VERSION = "app_version"
        private const val KEY_CURRENCY = "currency"
        private const val KEY_ADDRESS = "address"
        private const val KEY_CURRENCIES = "currencies"
        private const val KEY_ID = "id"
        private const val KEY_TICKER = "ticker"
        private const val KEY_NAME = "name"
        private const val KEY_COLORS = "colors"
        private const val KEY_BALANCE = "balance"
        private const val KEY_FIAT_BALANCE = "fiatBalance"
        private const val KEY_EXCHANGE = "exchange"
        private const val KEY_NUMERATOR = "numerator"
        private const val KEY_DENOMINATOR = "denominator"
        private const val KEY_HASH = "hash"
        private const val KEY_TRANSMITTED = "transmitted"

        private const val APP_PLATFORM = "android"

        private const val ERR_EMPTY_CURRENCIES = "currencies_empty"
        private const val ERR_ESTIMATE_FEE = "estimate_fee"
        private const val ERR_INSUFFICIENT_BALANCE = "insufficient_balance"
        private const val ERR_SEND_TXN = "send_txn"
        private const val ERR_CURRENCY_NOT_SUPPORTED = "currency_not_supported"

        private const val DOLLAR_IN_CENTS = 100
        private const val BASE_10 = 10

        private const val FORMAT_DELIMITER = "?format="
        private const val FORMAT_SEGWIT = "segwit"
    }

    @JavascriptInterface
    fun info() = nativePromiseFactory.create {
        val system = checkNotNull(breadBox.getSystemUnsafe())
        val btcWallet = system.wallets.first { it.currency.isBitcoin() }

        val preferredCode = BRSharedPrefs.getPreferredFiatIso(context)
        val fiatCurrency = Currency.getInstance(preferredCode)

        JSONObject().apply {
            put(KEY_BTC_DENOMINATION_DIGITS, btcWallet.defaultUnit.decimals)
            put(KEY_LOCAL_CURRENCY_CODE, fiatCurrency.currencyCode.toUpperCase())
            put(KEY_LOCAL_CURRENCY_PRECISION, fiatCurrency.defaultFractionDigits)
            put(KEY_LOCAL_CURRENCY_SYMBOL, fiatCurrency.symbol)
            put(KEY_APP_PLATFORM, APP_PLATFORM)
            put(KEY_APP_VERSION, "${BuildConfig.VERSION_NAME}.${BuildConfig.BUILD_VERSION}")
        }
    }

    @JvmOverloads
    @JavascriptInterface
    fun event(
        name: String,
        attributes: String? = null
    ) = nativePromiseFactory.create {
        if (attributes != null) {
            val json = JSONObject(attributes)
            val attr = mutableMapOf<String, String>()
            json.keys().forEach {
                attr[it] = json.getString(it)
            }
            EventUtils.pushEvent(name, attr)
        } else {
            EventUtils.pushEvent(name)
        }
        null
    }

    @JavascriptInterface
    fun addresses(
        currencyCodeQ: String
    ) = nativePromiseFactory.create {
        var (currencyCode, format) = parseCurrencyCodeQuery(currencyCodeQ)

        val system = checkNotNull(breadBox.getSystemUnsafe())
        val wallet = system.wallets.first { it.currency.code.equals(currencyCode, true) }
        val address = getAddress(wallet, format)

        JSONObject().apply {
            put(KEY_CURRENCY, currencyCode)
            put(KEY_ADDRESS, address)
        }
    }

    @Suppress("LongMethod", "ComplexMethod")
    @JavascriptInterface
    @JvmOverloads
    fun currencies(
        listAll: Boolean = false
    ) = nativePromiseFactory.create {
        val system = checkNotNull(breadBox.getSystemUnsafe())
        val wallets = if (listAll) {
            TokenUtil.getTokenItems(context).filter { it.isSupported }.map { it.currencyId }
        } else {
            checkNotNull(metaDataProvider.getEnabledWalletsUnsafe())
        }

        val currenciesJSON = JSONArray()
        wallets.forEach { currencyId ->
            val wallet = system.wallets.firstOrNull { it.currencyId.equals(currencyId, true) }
            val tokenItem = TokenUtil.getTokenItemForCurrencyId(currencyId)
            val currencyCode =
                wallet?.currency?.code?.toUpperCase() ?: tokenItem?.symbol

            if (currencyCode.isNullOrBlank()) return@forEach

            val json = JSONObject().apply {
                try {
                    put(KEY_ID, currencyCode)
                    put(KEY_TICKER, currencyCode)
                    put(KEY_NAME, tokenItem?.name ?: wallet?.name)

                    val colors = JSONArray().apply {
                        put(TokenUtil.getTokenStartColor(currencyCode, context))
                        put(TokenUtil.getTokenEndColor(currencyCode, context))
                    }
                    put(KEY_COLORS, colors)

                    // Add balance info if wallet is available
                    if (wallet != null) {
                        val balance = JSONObject().apply {
                            val numerator = wallet.balance.toStringWithBase(BASE_10, "")
                            val denominator =
                                Amount.create(
                                    "1",
                                    false,
                                    wallet.defaultUnit
                                ).orNull()?.toStringWithBase(BASE_10, "") ?: Amount.create(
                                    0,
                                    wallet.baseUnit
                                )
                            put(KEY_CURRENCY, currencyCode)
                            put(KEY_NUMERATOR, numerator)
                            put(KEY_DENOMINATOR, denominator)
                        }

                        val fiatCurrency = BRSharedPrefs.getPreferredFiatIso()

                        val fiatBalance = JSONObject().apply {
                            val fiatAmount = ratesRepository.getFiatForCrypto(
                                wallet.balance.toBigDecimal(),
                                currencyCode,
                                fiatCurrency
                            )?.multiply(BigDecimal(DOLLAR_IN_CENTS)) ?: BigDecimal.ZERO
                            put(KEY_CURRENCY, fiatCurrency)
                            put(KEY_NUMERATOR, fiatAmount.toPlainString())
                            put(KEY_DENOMINATOR, DOLLAR_IN_CENTS.toString())
                        }

                        val exchange = JSONObject().apply {
                            val fiatPerUnit = ratesRepository.getFiatForCrypto(
                                BigDecimal.ONE,
                                currencyCode,
                                fiatCurrency
                            )?.multiply(BigDecimal(DOLLAR_IN_CENTS)) ?: BigDecimal.ZERO
                            put(KEY_CURRENCY, fiatCurrency)
                            put(KEY_NUMERATOR, fiatPerUnit.toPlainString())
                            put(KEY_DENOMINATOR, DOLLAR_IN_CENTS.toString())
                        }

                        put(KEY_BALANCE, balance)
                        put(KEY_FIAT_BALANCE, fiatBalance)
                        put(KEY_EXCHANGE, exchange)
                    }
                } catch (ex: JSONException) {
                    logError("Failed to load currency data: $currencyCode")
                }
            }
            currenciesJSON.put(json)
        }

        if (currenciesJSON.length() == 0) throw IllegalStateException(ERR_EMPTY_CURRENCIES)
        JSONObject().put(KEY_CURRENCIES, currenciesJSON)
    }

    @JavascriptInterface
    fun transaction(
        toAddress: String,
        description: String,
        amountStr: String,
        currency: String
    ) = nativePromiseFactory.create {
        val system = checkNotNull(breadBox.getSystemUnsafe())
        val wallet = system.wallets.first { it.currency.code.equals(currency, true) }
        val amountJSON = JSONObject(amountStr)
        val numerator = amountJSON.getString(KEY_NUMERATOR).toDouble()
        val denominator = amountJSON.getString(KEY_DENOMINATOR).toDouble()
        val amount = Amount.create((numerator / denominator), wallet.unit)
        val address = checkNotNull(wallet.addressFor(toAddress))

        val feeBasis =
            checkNotNull(estimateFee(wallet, address, amount)) { ERR_ESTIMATE_FEE }

        val fee = feeBasis.fee.toBigDecimal()
        val totalCost = if (feeBasis.currency.code == wallet.currency.code) {
            amount.toBigDecimal() + fee
        } else {
            amount.toBigDecimal()
        }
        val balance = wallet.balance.toBigDecimal()

        check(!amount.isZero && totalCost <= balance) { ERR_INSUFFICIENT_BALANCE }

        val transaction =
            checkNotNull(
                sendTransaction(
                    wallet,
                    address,
                    amount,
                    totalCost,
                    feeBasis
                )
            ) { ERR_SEND_TXN }

        metaDataProvider.putTxMetaData(
            transaction,
            TxMetaDataValue(comment = description)
        )

        JSONObject().apply {
            put(KEY_HASH, transaction.hashString())
            put(KEY_TRANSMITTED, true)
        }
    }

    @JavascriptInterface
    fun enableCurrency(
        currencyCode: String
    ) = nativePromiseFactory.create {
        val system = checkNotNull(breadBox.system().first())
        val currencyId = TokenUtil.getTokenItemByCurrencyCode(currencyCode).currencyId.orEmpty()
        check(currencyId.isNotEmpty()) { ERR_CURRENCY_NOT_SUPPORTED }

        val network = system.networks.find { it.containsCurrency(currencyId) }
        when (network?.findCurrency(currencyId)?.isNative()) {
            null -> logError("No network or currency found for $currencyId.")
            false -> {
                val trackedWallets = breadBox.wallets().first()
                if (!trackedWallets.containsCurrency(network.currency.uids)) {
                    logDebug("Adding native wallet ${network.currency.uids} for $currencyId.")
                    metaDataProvider.enableWallet(network.currency.uids)
                }
            }
        }

        metaDataProvider.enableWallet(currencyId)

        JSONObject().apply {
            put(KEY_CURRENCY, currencyCode)
        }
    }

    @JavascriptInterface
    fun maxlimit(
        toAddress: String,
        currency: String
    ) = nativePromiseFactory.create {
        val system = checkNotNull(breadBox.getSystemUnsafe())
        val wallet = system.wallets.first { it.currency.code.equals(currency, true) }
        val address = checkNotNull(wallet.addressFor(toAddress))

        suspendCoroutine { continuation ->
            val handler = object : CompletionHandler<Amount, LimitEstimationError> {
                override fun handleData(limitMaxAmount: Amount?) {
                    if (limitMaxAmount == null) {
                        continuation.resumeWithException(Exception("Limit Estimation is null"))
                        return
                    }
                    val numerator = limitMaxAmount.convert(wallet.baseUnit).orNull()
                        ?.toStringWithBase(BASE_10, "") ?: "0"
                    val denominator = Amount.create("1", false, wallet.baseUnit).orNull()
                        ?.toStringWithBase(BASE_10, "")
                        ?: Amount.create(0, wallet.baseUnit).toStringWithBase(BASE_10, "")

                    continuation.resume(JSONObject().apply {
                        put(KEY_NUMERATOR, numerator)
                        put(KEY_DENOMINATOR, denominator)
                    })
                }

                override fun handleError(error: LimitEstimationError?) {
                    continuation.resumeWithException(
                        error ?: Exception("Unknown Limit Estimation Error")
                    )
                }
            }

            wallet.estimateLimitMaximum(
                address,
                wallet.feeForSpeed(TransferSpeed.PRIORITY),
                handler
            )
        }
    }

    private suspend fun estimateFee(
        wallet: Wallet,
        address: Address?,
        amount: Amount
    ): TransferFeeBasis? {
        if (address == null || wallet.containsAddress(address)) {
            return null
        }

        try {
            return wallet.estimateFee(address, amount, wallet.feeForSpeed(TransferSpeed.PRIORITY))
        } catch (e: FeeEstimationError) {
            logError("Failed get fee estimate", e)
        } catch (e: IllegalStateException) {
            logError("Failed get fee estimate", e)
        }
        return null
    }

    @Suppress("LongMethod")
    private suspend fun sendTransaction(
        wallet: Wallet,
        address: Address,
        amount: Amount,
        totalCost: BigDecimal,
        feeBasis: TransferFeeBasis
    ): Transfer? {
        val fiatCode = BRSharedPrefs.getPreferredFiatIso(context)
        val fiatAmount = ratesRepository.getFiatForCrypto(
            amount.toBigDecimal(),
            wallet.currency.code,
            fiatCode
        ) ?: BigDecimal.ZERO
        val fiatTotalCost = ratesRepository.getFiatForCrypto(
            totalCost,
            wallet.currency.code,
            fiatCode
        ) ?: BigDecimal.ZERO
        val fiatNetworkFee = ratesRepository.getFiatForCrypto(
            feeBasis.fee.toBigDecimal(),
            wallet.currency.code,
            fiatCode
        ) ?: BigDecimal.ZERO

        val result = PlatformTransactionBus.results().onStart {
            PlatformTransactionBus.sendMessage(
                ConfirmTransactionMessage(
                    wallet.currency.code,
                    fiatCode,
                    feeBasis.currency.code,
                    address.toSanitizedString(),
                    TransferSpeed.PRIORITY,
                    amount.toBigDecimal(),
                    fiatAmount,
                    fiatTotalCost,
                    fiatNetworkFee,
                    emptyList()
                )
            )
        }.first()

        return when (result) {
            is TransactionResultMessage.TransactionCancelled -> throw IllegalStateException(
                context.getString(R.string.Platform_transaction_cancelled)
            )
            is TransactionResultMessage.TransactionConfirmed -> {
                val phrase = try {
                    checkNotNull(accountManager.getPhrase())
                } catch (ex: UserNotAuthenticatedException) {
                    logError("Failed to get phrase.", ex)
                    return null
                }

                val newTransfer = wallet.createTransfer(address, amount, feeBasis, null).orNull()
                if (newTransfer == null) {
                    logError("Failed to create transfer.")
                    return null
                }

                wallet.walletManager.submit(newTransfer, phrase)

                breadBox.walletTransfer(wallet.currency.code, newTransfer.hashString())
                    .transform { transfer ->
                        when (checkNotNull(transfer.state.type)) {
                            TransferState.Type.INCLUDED,
                            TransferState.Type.PENDING,
                            TransferState.Type.SUBMITTED -> emit(transfer)
                            TransferState.Type.DELETED,
                            TransferState.Type.FAILED -> {
                                logError("Failed to submit transfer ${transfer.state.failedError.orNull()}")
                                emit(null)
                            }
                            // Ignore pre-submit states
                            TransferState.Type.CREATED,
                            TransferState.Type.SIGNED -> Unit
                        }
                    }.first()
            }
        }
    }

    private fun parseCurrencyCodeQuery(currencyCodeQuery: String) = when {
        currencyCodeQuery.contains(FORMAT_DELIMITER) -> {
            val parts = currencyCodeQuery.split(FORMAT_DELIMITER)
            parts[0] to parts[1]
        }
        else -> {
            currencyCodeQuery to ""
        }
    }

    private fun getAddress(wallet: Wallet, format: String) = when {
        wallet.currency.isBitcoin() -> {
            wallet.getTargetForScheme(
                when {
                    format.equals(FORMAT_SEGWIT, true) -> AddressScheme.BTC_SEGWIT
                    else -> AddressScheme.BTC_LEGACY
                }
            ).toString()
        }
        else -> wallet.target.toSanitizedString()
    }
}
