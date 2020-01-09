package com.breadwallet.ui.wallet

import android.content.Context
import com.breadwallet.app.BreadApp
import com.breadwallet.breadbox.BreadBox
import com.breadwallet.breadbox.feeForToken
import com.breadwallet.breadbox.formatFiatForUi
import com.breadwallet.breadbox.hashString
import com.breadwallet.breadbox.toBigDecimal
import com.breadwallet.breadbox.toSanitizedString
import com.breadwallet.crypto.Amount
import com.breadwallet.crypto.Transfer
import com.breadwallet.crypto.TransferDirection
import com.breadwallet.crypto.TransferState
import com.breadwallet.effecthandler.metadata.MetaDataEffect
import com.breadwallet.effecthandler.metadata.MetaDataEvent
import com.breadwallet.model.PriceChange
import com.breadwallet.repository.RatesRepository
import com.breadwallet.tools.manager.BRSharedPrefs
import com.breadwallet.tools.sqlite.RatesDataSource
import com.breadwallet.tools.util.EventUtils
import com.breadwallet.tools.util.TokenUtil
import com.breadwallet.ui.navigation.NavEffectTransformer
import com.platform.util.AppReviewPromptManager
import com.spotify.mobius.Connectable
import com.spotify.mobius.flow.flowTransformer
import com.spotify.mobius.flow.subtypeEffectHandler
import com.spotify.mobius.flow.transform
import kotlinx.coroutines.Dispatchers.Default
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.transformLatest
import java.math.BigDecimal

@Suppress("TooManyFunctions")
object WalletScreenEffectHandler {

    fun createEffectHandler(
        context: Context,
        breadBox: BreadBox,
        navEffectHandler: NavEffectTransformer,
        metadataEffectHandler: Connectable<MetaDataEffect, MetaDataEvent>
    ) = subtypeEffectHandler<WalletScreenEffect, WalletScreenEvent> {
        addTransformer<WalletScreenEffect.Nav>(navEffectHandler)
        addTransformer(handleCheckReviewPrompt(context))
        addTransformer(handleLoadPricePerUnit(context))

        addTransformer(handleLoadBalance(breadBox))
        addTransformer(handleLoadTransactions(breadBox))
        addTransformer(handleLoadCurrencyName(breadBox))
        addTransformer(handleLoadSyncState(breadBox))

        addTransformer(handleLoadTransactionMetaData(metadataEffectHandler))

        addActionSync<WalletScreenEffect.RecordReviewPrompt>(Default, ::handleRecordReviewPrompt)
        addActionSync<WalletScreenEffect.RecordReviewPromptDismissed>(
            Default,
            handleRecordReviewPromptDismissed(context)
        )

        addConsumerSync(Default, ::handleTrackEvent)
        addConsumerSync(Default, ::handleUpdateCryptoPreferred)
        addFunctionSync(Default, ::handleLoadIsTokenSupported)
        addFunctionSync(Default, ::handleConvertCryptoTransactions)
        addFunctionSync(Default, handleLoadChartInterval(RatesRepository.getInstance(context)))
        addFunctionSync<WalletScreenEffect.LoadCryptoPreferred>(Default) {
            WalletScreenEvent.OnIsCryptoPreferredLoaded(BRSharedPrefs.isCryptoPreferred())
        }
    }

    private fun handleUpdateCryptoPreferred(
        effect: WalletScreenEffect.UpdateCryptoPreferred
    ) {
        EventUtils.pushEvent(EventUtils.EVENT_AMOUNT_SWAP_CURRENCY)
        BRSharedPrefs.setIsCryptoPreferred(b = effect.cryptoPreferred)
    }

    private fun handleConvertCryptoTransactions(
        effect: WalletScreenEffect.ConvertCryptoTransactions
    ) = effect.transactions
        .map { it.asWalletTransaction() }
        .run(WalletScreenEvent::OnTransactionsUpdated)

    private fun handleLoadIsTokenSupported(
        effect: WalletScreenEffect.LoadIsTokenSupported
    ) = TokenUtil.isTokenSupported(effect.currencyCode)
        .run(WalletScreenEvent::OnIsTokenSupportedUpdated)

    private fun handleCheckReviewPrompt(
        context: Context
    ) = flowTransformer<WalletScreenEffect.CheckReviewPrompt, WalletScreenEvent> { effects ->
        effects.transformLatest { (currencyCode, transactions) ->
            if (AppReviewPromptManager.showReview(context, currencyCode, transactions)) {
                emit(WalletScreenEvent.OnShowReviewPrompt)
            }
        }
    }

    private fun handleRecordReviewPrompt() {
        EventUtils.pushEvent(EventUtils.EVENT_REVIEW_PROMPT_DISPLAYED)
    }

    private fun handleRecordReviewPromptDismissed(
        context: Context
    ): () -> Unit = {
        EventUtils.pushEvent(EventUtils.EVENT_REVIEW_PROMPT_DISMISSED)
        AppReviewPromptManager.onReviewPromptDismissed(context)
    }

    private fun handleTrackEvent(value: WalletScreenEffect.TrackEvent) {
        EventUtils.pushEvent(value.eventName, value.attributes)
    }

    private fun handleLoadPricePerUnit(
        context: Context
    ) = flowTransformer<WalletScreenEffect.LoadFiatPricePerUnit, WalletScreenEvent> { effects ->
        val ratesRepository = RatesRepository.getInstance(context)
        val ratesDataSource = RatesDataSource.getInstance(context)
        val fiatIso = BRSharedPrefs.getPreferredFiatIso(context)
        effects
            .transformLatest { effect ->
                // Emit effect and observe rate changes,
                // dispatching the latest effect if needed
                emit(effect)
                emitAll(ratesDataSource
                    .rateChangesFlow()
                    .map { effect })
            }
            .mapLatest { effect ->
                val exchangeRate: BigDecimal? = ratesRepository.getFiatForCrypto(
                    BigDecimal.ONE,
                    effect.currencyId,
                    BRSharedPrefs.getPreferredFiatIso(context)
                )
                val fiatPricePerUnit = exchangeRate?.formatFiatForUi(fiatIso).orEmpty()
                val priceChange: PriceChange? = ratesRepository.getPriceChange(effect.currencyId)
                WalletScreenEvent.OnFiatPricePerUpdated(fiatPricePerUnit, priceChange)
            }
    }

    private fun handleLoadChartInterval(
        ratesRepository: RatesRepository
    ): (WalletScreenEffect.LoadChartInterval) -> WalletScreenEvent = { effect ->
        val dataPoints = ratesRepository.getHistoricalData(
            effect.currencyCode,
            BRSharedPrefs.getPreferredFiatIso(),
            effect.interval
        )
        WalletScreenEvent.OnMarketChartDataUpdated(dataPoints)
    }

    private fun handleLoadTransactions(
        breadBox: BreadBox
    ) = flowTransformer<WalletScreenEffect.LoadTransactions, WalletScreenEvent> { effects ->
        effects
            .flatMapLatest { effect ->
                breadBox.walletTransfers(effect.currencyId)
            }
            .mapLatest { wallets ->
                WalletScreenEvent.OnTransactionsUpdated(
                    wallets.map { it.asWalletTransaction() }
                        .sortedByDescending(WalletTransaction::timeStamp)
                )
            }
    }

    private fun handleLoadBalance(breadBox: BreadBox) =
        flowTransformer<WalletScreenEffect.LoadWalletBalance, WalletScreenEvent> { effects ->
            effects
                .flatMapLatest { effect ->
                    breadBox.wallet(effect.currencyId)
                        .map { it.balance }
                        .distinctUntilChanged()
                }
                .mapLatest { balance ->
                    WalletScreenEvent.OnBalanceUpdated(
                        balance.toBigDecimal(),
                        getBalanceInFiat(balance)
                    )
                }
        }

    private fun handleLoadCurrencyName(breadBox: BreadBox) =
        flowTransformer<WalletScreenEffect.LoadCurrencyName, WalletScreenEvent> { effects ->
            effects
                .flatMapLatest { effect ->
                    breadBox.wallet(effect.currencyId)
                        .map { it.currency.name }
                        .distinctUntilChanged()
                }
                .map { WalletScreenEvent.OnCurrencyNameUpdated(it) }
        }

    private fun handleLoadSyncState(breadBox: BreadBox) =
        flowTransformer<WalletScreenEffect.LoadSyncState, WalletScreenEvent> { effects ->
            effects
                .flatMapLatest { (currencyId) ->
                    breadBox.walletSyncState(currencyId)
                }
                .mapLatest { state ->
                    WalletScreenEvent.OnSyncProgressUpdated(
                        state.percentComplete,
                        state.timestamp,
                        state.isSyncing
                    )
                }
        }

    private fun handleLoadTransactionMetaData(
        metadataEffectHandler: Connectable<MetaDataEffect, MetaDataEvent>
    ) = flowTransformer<WalletScreenEffect.LoadTransactionMetaData, WalletScreenEvent> { effects ->
        effects
            .map { MetaDataEffect.LoadTransactionMetaData(it.transactionHashes) }
            .transform(metadataEffectHandler)
            .filterIsInstance<MetaDataEvent.OnTransactionMetaDataUpdated>()
            .map { event ->
                WalletScreenEvent.OnTransactionMetaDataUpdated(
                    event.transactionHash,
                    event.txMetaData
                )
            }
    }

    private fun RatesDataSource.rateChangesFlow() =
        callbackFlow<Unit> {
            val listener = RatesDataSource.OnDataChanged { offer(Unit) }
            addOnDataChangedListener(listener)
            awaitClose {
                removeOnDataChangedListener(listener)
            }
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
        currencyCode = wallet.currency.code,
        feeToken = feeForToken()
    )
}
