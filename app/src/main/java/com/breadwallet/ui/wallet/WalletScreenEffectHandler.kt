package com.breadwallet.ui.wallet

import android.content.Context
import com.breadwallet.app.BreadApp
import com.breadwallet.breadbox.hashString
import com.breadwallet.breadbox.toBigDecimal
import com.breadwallet.breadbox.toSanitizedString
import com.breadwallet.crypto.Amount
import com.breadwallet.crypto.Transfer
import com.breadwallet.crypto.TransferDirection
import com.breadwallet.crypto.TransferState
import com.breadwallet.repository.RatesRepository
import com.breadwallet.tools.manager.BRSharedPrefs
import com.breadwallet.tools.sqlite.RatesDataSource
import com.breadwallet.tools.util.CurrencyUtils
import com.breadwallet.tools.util.EventUtils
import com.platform.network.service.CurrencyHistoricalDataClient
import com.platform.util.AppReviewPromptManager
import com.spotify.mobius.Connection
import com.spotify.mobius.functions.Consumer
import java.math.BigDecimal

class WalletScreenEffectHandler(
    private val output: Consumer<WalletScreenEvent>
) : Connection<WalletScreenEffect> {

    override fun accept(value: WalletScreenEffect) {
        when (value) {
            is WalletScreenEffect.LoadCryptoPreferred -> {
                val isCryptoPreferred = BRSharedPrefs.isCryptoPreferred()
                output.accept(WalletScreenEvent.OnIsCryptoPreferredLoaded(isCryptoPreferred))
            }
            is WalletScreenEffect.UpdateCryptoPreferred -> {
                EventUtils.pushEvent(EventUtils.EVENT_AMOUNT_SWAP_CURRENCY) // TODO: Is this needed?
                BRSharedPrefs.setIsCryptoPreferred(b = value.cryptoPreferred)
            }
        }
    }

    override fun dispose() {
    }
}

class WalletReviewPromptHandler(
    private val output: Consumer<WalletScreenEvent>,
    private val context: Context,
    private val currencyCode: String
) : Connection<WalletScreenEffect> {

    override fun accept(value: WalletScreenEffect) {
        when (value) {
            is WalletScreenEffect.CheckReviewPrompt -> {
                val showPrompt =
                    AppReviewPromptManager.showReview(context, currencyCode, value.transactions)
                if (showPrompt) {
                    output.accept(WalletScreenEvent.OnShowReviewPrompt)
                }
            }
            is WalletScreenEffect.ConvertCryptoTransactions -> {
                value.transactions.map {
                    it.asWalletTransaction()
                }.run {
                    output.accept(WalletScreenEvent.OnTransactionsUpdated(this))
                }
            }
            WalletScreenEffect.RecordReviewPrompt -> EventUtils.pushEvent(EventUtils.EVENT_REVIEW_PROMPT_DISPLAYED)
            WalletScreenEffect.RecordReviewPromptDismissed -> {
                EventUtils.pushEvent(EventUtils.EVENT_REVIEW_PROMPT_DISMISSED)
                AppReviewPromptManager.onReviewPromptDismissed(context)
            }
            is WalletScreenEffect.TrackEvent -> {
                EventUtils.pushEvent(value.eventName, value.attributes)
            }
        }
    }

    override fun dispose() {
    }
}

class WalletRatesHandler(
    private val output: Consumer<WalletScreenEvent>,
    private val context: Context,
    private val currencyCode: String
) : Connection<WalletScreenEffect>,
    RatesDataSource.OnDataChanged {

    init {
        RatesDataSource.getInstance(context).addOnDataChangedListener(this)
    }

    override fun accept(value: WalletScreenEffect) {
        when (value) {
            is WalletScreenEffect.LoadFiatPricePerUnit -> loadFiatPerPriceUnit()
        }
    }

    override fun onChanged() {
        loadFiatPerPriceUnit()
    }

    private fun loadFiatPerPriceUnit() {
        RatesRepository.getInstance(context).let {
            val exchangeRate = it.getFiatForCrypto(
                BigDecimal.ONE,
                currencyCode,
                BRSharedPrefs.getPreferredFiatIso(context)
            )
            val fiatPricePerUnit = CurrencyUtils.getFormattedAmount(
                context,
                BRSharedPrefs.getPreferredFiatIso(context),
                exchangeRate
            )
            val priceChange = it.getPriceChange(currencyCode)
            output.accept(WalletScreenEvent.OnFiatPricePerUpdated(fiatPricePerUnit, priceChange))
        }
    }

    override fun dispose() {
        RatesDataSource.getInstance(context).removeOnDataChangedListener(this)
    }
}

class WalletHistoricalPriceIntervalHandler(
    private val output: Consumer<WalletScreenEvent>,
    private val context: Context,
    private val currencyCode: String
) : Connection<WalletScreenEffect> {

    override fun accept(value: WalletScreenEffect) {
        when (value) {
            is WalletScreenEffect.LoadChartInterval -> {
                val toCurrency = BRSharedPrefs.getPreferredFiatIso()
                val dataPoints = when (value.interval) {
                    Interval.ONE_DAY -> CurrencyHistoricalDataClient.getPastDay(
                        context,
                        currencyCode,
                        toCurrency
                    )
                    Interval.ONE_WEEK -> CurrencyHistoricalDataClient.getPastWeek(
                        context,
                        currencyCode,
                        toCurrency
                    )
                    Interval.ONE_MONTH -> CurrencyHistoricalDataClient.getPastMonth(
                        context,
                        currencyCode,
                        toCurrency
                    )
                    Interval.THREE_MONTHS -> CurrencyHistoricalDataClient.getPastThreeMonths(
                        context,
                        currencyCode,
                        toCurrency
                    )
                    Interval.ONE_YEAR -> CurrencyHistoricalDataClient.getPastYear(
                        context,
                        currencyCode,
                        toCurrency
                    )
                    Interval.THREE_YEARS -> CurrencyHistoricalDataClient.getPastThreeYears(
                        context,
                        currencyCode,
                        toCurrency
                    )
                }
                output.accept(WalletScreenEvent.OnMarketChartDataUpdated(dataPoints))
            }
        }
    }

    override fun dispose() {
    }
}

private fun getBalanceInFiat(balanceAmt: Amount): BigDecimal {
    val context = BreadApp.getBreadContext()
    return RatesRepository.getInstance(context).getFiatForCrypto(
        balanceAmt.toBigDecimal(),
        balanceAmt.currency.code,
        BRSharedPrefs.getPreferredFiatIso(context)
    ) ?: BigDecimal.ZERO
}

fun Transfer.asWalletTransaction(): WalletTransaction {
    val confirmationsUntilFinal = wallet.walletManager.network.confirmationsUntilFinal
    val confirmation = confirmation.orNull()

    return WalletTransaction(
        txHash = hashString(),
        amount = amount.toBigDecimal(),
        amountInFiat = getBalanceInFiat(amount),
        toAddress = target.orNull()?.toSanitizedString() ?: "<unknown>",
        fromAddress = source.orNull()?.toSanitizedString() ?: "<unknown>",
        isReceived = direction == TransferDirection.RECEIVED,
        isErrored = state.type == TransferState.Type.FAILED,
        isValid = state.type != TransferState.Type.FAILED, // TODO: Is this correct?
        fee = fee.doubleAmount(unitForFee.base).or(0.0).toBigDecimal(),
        confirmations = confirmations.orNull()?.toInt() ?: 0,
        confirmationsUntilFinal = confirmationsUntilFinal.toInt(),
        timeStamp = confirmation?.confirmationTime?.time ?: System.currentTimeMillis(),
        currencyCode = wallet.currency.code
    )
}
