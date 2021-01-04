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
import com.breadwallet.app.Buy
import com.breadwallet.app.ConversionTracker
import com.breadwallet.app.Trade
import com.breadwallet.breadbox.BreadBox
import com.breadwallet.breadbox.TransferSpeed
import com.breadwallet.breadbox.addressFor
import com.breadwallet.breadbox.baseUnit
import com.breadwallet.breadbox.containsCurrency
import com.breadwallet.breadbox.currencyId
import com.breadwallet.breadbox.defaultUnit
import com.breadwallet.breadbox.estimateFee
import com.breadwallet.breadbox.estimateMaximum
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
import com.breadwallet.crypto.TransferAttribute
import com.breadwallet.crypto.TransferFeeBasis
import com.breadwallet.crypto.TransferState
import com.breadwallet.crypto.Wallet
import com.breadwallet.crypto.errors.FeeEstimationError
import com.breadwallet.logger.logDebug
import com.breadwallet.logger.logError
import com.breadwallet.model.TokenItem
import com.breadwallet.repository.RatesRepository
import com.breadwallet.tools.manager.BRSharedPrefs
import com.breadwallet.tools.security.BrdUserManager
import com.breadwallet.tools.util.EventUtils
import com.breadwallet.tools.util.TokenUtil
import com.breadwallet.ui.send.TransferField
import com.platform.ConfirmTransactionMessage
import com.platform.PlatformTransactionBus
import com.platform.TransactionResultMessage
import com.breadwallet.platform.entities.TxMetaDataValue
import com.breadwallet.platform.interfaces.AccountMetaDataProvider
import com.platform.util.getStringOrNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.transform
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import java.math.BigDecimal
import java.util.Currency
import java.util.Locale

class WalletJs(
    private val promise: NativePromiseFactory,
    private val context: Context,
    private val metaDataProvider: AccountMetaDataProvider,
    private val breadBox: BreadBox,
    private val ratesRepository: RatesRepository,
    private val userManager: BrdUserManager,
    private val conversionTracker: ConversionTracker
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
        private const val FORMAT_LEGACY = "legacy"
        private const val FORMAT_SEGWIT = "segwit"
    }

    @JavascriptInterface
    fun info() = promise.create {
        val system = checkNotNull(breadBox.getSystemUnsafe())
        val btcWallet = system.wallets.first { it.currency.isBitcoin() }

        val preferredCode = BRSharedPrefs.getPreferredFiatIso()
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
    ) = promise.create {
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
    ) = promise.create {
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
    fun currencies(listAll: Boolean = false) = promise.create {
        val system = checkNotNull(breadBox.getSystemUnsafe())
        val wallets = if (listAll) {
            TokenUtil.getTokenItems()
                .filter(TokenItem::isSupported)
                .map(TokenItem::currencyId)
        } else {
            checkNotNull(metaDataProvider.getEnabledWalletsUnsafe())
        }

        val currenciesJSON = JSONArray()
        wallets.forEach { currencyId ->
            val wallet = system.wallets.firstOrNull { it.currencyId.equals(currencyId, true) }
            val tokenItem = TokenUtil.tokenForCurrencyId(currencyId)
            val currencyCode =
                wallet?.currency?.code?.toUpperCase() ?: tokenItem?.symbol

            if (currencyCode.isNullOrBlank()) return@forEach

            val json = JSONObject().apply {
                try {
                    put(KEY_ID, currencyCode)
                    put(KEY_TICKER, currencyCode)
                    put(KEY_NAME, tokenItem?.name ?: wallet?.name)

                    val colors = JSONArray().apply {
                        put(TokenUtil.getTokenStartColor(currencyCode))
                        put(TokenUtil.getTokenEndColor(currencyCode))
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
    fun transferAttrsFor(currency: String, addressString: String) = promise.createForArray {
        val wallet = breadBox.wallet(currency).first()
        val address = wallet.addressFor(addressString)
        val attrs = address?.run(wallet::getTransferAttributesFor) ?: wallet.transferAttributes
        val jsonAttrs = attrs.map { attr ->
            JSONObject().apply {
                put("key", attr.key)
                put("required", attr.isRequired)
                put("value", attr.value.orNull())
            }
        }
        JSONArray(jsonAttrs)
    }

    @JvmOverloads
    @JavascriptInterface
    fun transaction(
        toAddress: String,
        description: String,
        amountStr: String,
        currency: String,
        transferAttrsString: String = "[]"
    ) = promise.create {
        val system = checkNotNull(breadBox.getSystemUnsafe())
        val wallet = system.wallets.first { it.currency.code.equals(currency, true) }
        val amountJSON = JSONObject(amountStr)
        val numerator = amountJSON.getString(KEY_NUMERATOR).toDouble()
        val denominator = amountJSON.getString(KEY_DENOMINATOR).toDouble()
        val amount = Amount.create((numerator / denominator), wallet.unit)
        val address = checkNotNull(wallet.addressFor(toAddress)) {
            "Invalid address ($toAddress)"
        }
        val transferAttrs = wallet.getTransferAttributesFor(address)
        val transferAttrsInput = JSONArray(transferAttrsString).run {
            List(length(), ::getJSONObject)
        }

        transferAttrs.forEach { attribute ->
            val key = attribute.key.toLowerCase(Locale.ROOT)
            val attrJson = transferAttrsInput.firstOrNull { attr ->
                attr.getStringOrNull("key").equals(key, true)
            }
            attribute.setValue(attrJson?.getStringOrNull("value"))

            val error = wallet.validateTransferAttribute(attribute).orNull()
            require(error == null) {
                "Invalid attribute key=${attribute.key} value=${attribute.value.orNull()} error=${error?.name}"
            }
        }

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
                    feeBasis,
                    transferAttrs
                )
            ) { ERR_SEND_TXN }

        metaDataProvider.putTxMetaData(
            transaction,
            TxMetaDataValue(comment = description)
        )

        conversionTracker.track(Trade(currency, transaction.hashString()))

        JSONObject().apply {
            put(KEY_HASH, transaction.hashString())
            put(KEY_TRANSMITTED, true)
        }
    }

    @JavascriptInterface
    fun enableCurrency(currencyCode: String) = promise.create {
        val system = checkNotNull(breadBox.system().first())
        val currencyId = TokenUtil.tokenForCode(currencyCode)?.currencyId.orEmpty()
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

        // Suspend until the wallet exists, i.e. an address is available.
        val wallet = breadBox.wallet(currencyCode).first()
        JSONObject().apply {
            put(KEY_CURRENCY, currencyCode)
            put(KEY_ADDRESS, getAddress(wallet, FORMAT_LEGACY))
            if (wallet.currency.isBitcoin()) {
                put("${KEY_ADDRESS}_${FORMAT_SEGWIT}", getAddress(wallet, FORMAT_SEGWIT))
            }
        }
    }

    @JavascriptInterface
    fun maxlimit(
        toAddress: String,
        currency: String
    ) = promise.create {
        val system = checkNotNull(breadBox.getSystemUnsafe())
        val wallet = system.wallets.first { it.currency.code.equals(currency, true) }
        val address = checkNotNull(wallet.addressFor(toAddress))

        val limitMaxAmount =
            wallet.estimateMaximum(address, wallet.feeForSpeed(TransferSpeed.Priority(currency)))

        checkNotNull(limitMaxAmount)

        val numerator = limitMaxAmount.convert(wallet.baseUnit).orNull()
            ?.toStringWithBase(BASE_10, "") ?: "0"
        val denominator = Amount.create("1", false, wallet.baseUnit).orNull()
            ?.toStringWithBase(BASE_10, "")
            ?: Amount.create(0, wallet.baseUnit).toStringWithBase(BASE_10, "")

        JSONObject().apply {
            put(KEY_NUMERATOR, numerator)
            put(KEY_DENOMINATOR, denominator)
        }
    }

    @JavascriptInterface
    fun trackBuy(
        currencyCode: String,
        amount: String
    ) = promise.createForUnit {
        conversionTracker.track(Buy(currencyCode, amount.toDouble(), System.currentTimeMillis()))
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
            return wallet.estimateFee(
                address,
                amount,
                wallet.feeForSpeed(TransferSpeed.Priority(wallet.currency.code))
            )
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
        feeBasis: TransferFeeBasis,
        transferAttributes: Set<TransferAttribute>
    ): Transfer? {
        val fiatCode = BRSharedPrefs.getPreferredFiatIso()
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

        val transferFields = transferAttributes.map { attribute ->
            TransferField(
                attribute.key,
                attribute.isRequired,
                false,
                attribute.value.orNull()
            )
        }
        val result = PlatformTransactionBus.results().onStart {
            PlatformTransactionBus.sendMessage(
                ConfirmTransactionMessage(
                    wallet.currency.code,
                    fiatCode,
                    feeBasis.currency.code,
                    address.toSanitizedString(),
                    TransferSpeed.Priority(wallet.currency.code),
                    amount.toBigDecimal(),
                    fiatAmount,
                    fiatTotalCost,
                    fiatNetworkFee,
                    transferFields
                )
            )
        }.first()

        return when (result) {
            is TransactionResultMessage.TransactionCancelled -> throw IllegalStateException(
                context.getString(R.string.Platform_transaction_cancelled)
            )
            is TransactionResultMessage.TransactionConfirmed -> {
                val phrase = try {
                    checkNotNull(userManager.getPhrase())
                } catch (ex: UserNotAuthenticatedException) {
                    logError("Failed to get phrase.", ex)
                    return null
                }

                val newTransfer =
                    wallet.createTransfer(address, amount, feeBasis, transferAttributes).orNull()
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
