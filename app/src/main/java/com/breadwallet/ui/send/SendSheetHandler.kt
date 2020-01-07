package com.breadwallet.ui.send

import android.content.Context
import android.security.keystore.UserNotAuthenticatedException
import com.bluelinelabs.conductor.Router
import com.bluelinelabs.conductor.RouterTransaction
import com.breadwallet.R
import com.breadwallet.breadbox.BreadBox
import com.breadwallet.breadbox.addressFor
import com.breadwallet.breadbox.estimateFee
import com.breadwallet.breadbox.feeForSpeed
import com.breadwallet.breadbox.formatCryptoForUi
import com.breadwallet.breadbox.toBigDecimal
import com.breadwallet.crypto.Amount
import com.breadwallet.crypto.errors.FeeEstimationError
import com.breadwallet.crypto.errors.TransferSubmitError
import com.breadwallet.effecthandler.metadata.MetaDataEffect
import com.breadwallet.effecthandler.metadata.MetaDataEvent
import com.breadwallet.logger.logError
import com.breadwallet.repository.RatesRepository
import com.breadwallet.tools.manager.BRClipboardManager
import com.breadwallet.tools.manager.BRSharedPrefs
import com.breadwallet.tools.security.KeyStore
import com.breadwallet.tools.security.isFingerPrintAvailableAndSetup
import com.breadwallet.tools.util.Link
import com.breadwallet.tools.util.asLink
import com.breadwallet.ui.controllers.AlertDialogController
import com.breadwallet.ui.navigation.NavEffectTransformer
import com.spotify.mobius.Connectable
import com.spotify.mobius.flow.flowTransformer
import com.spotify.mobius.flow.subtypeEffectHandler
import com.spotify.mobius.flow.transform
import com.spotify.mobius.functions.Consumer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.singleOrNull
import kotlinx.coroutines.flow.transform
import kotlinx.coroutines.flow.transformLatest
import kotlinx.coroutines.withContext
import java.math.BigDecimal

private const val RATE_UPDATE_MS = 60_000L

@UseExperimental(ExperimentalCoroutinesApi::class, FlowPreview::class)
object SendSheetHandler {

    fun create(
        context: Context,
        router: Router,
        retainedScope: CoroutineScope,
        outputProducer: () -> Consumer<SendSheetEvent>,
        breadBox: BreadBox,
        keyStore: KeyStore,
        navEffectHandler: NavEffectTransformer,
        metaDataEffectHandler: Connectable<MetaDataEffect, MetaDataEvent>
    ) = subtypeEffectHandler<SendSheetEffect, SendSheetEvent> {
        addTransformer(pollExchangeRate(context, breadBox))
        addTransformer(handleLoadBalance(context, breadBox))
        addTransformer(validateAddress(breadBox))
        addTransformer(handleEstimateFee(breadBox))
        addTransformer<SendSheetEffect.Nav>(navEffectHandler)
        addTransformer(handleSendTransaction(breadBox, keyStore, retainedScope, outputProducer))
        addTransformer(handleAddTransactionMetadata(metaDataEffectHandler))

        addFunction(parseClipboard(context, breadBox))
        addConsumerSync(Dispatchers.Main, showBalanceTooLowForFee(router))

        addFunctionSync<SendSheetEffect.LoadAuthenticationSettings> {
            val isEnabled = isFingerPrintAvailableAndSetup(context) && BRSharedPrefs.sendMoneyWithFingerprint
            SendSheetEvent.OnAuthenticationSettingsUpdated(isEnabled)
        }
    }

    private fun handleLoadBalance(
        context: Context,
        breadBox: BreadBox
    ) = flowTransformer<SendSheetEffect.LoadBalance, SendSheetEvent> { effects ->
        effects.map { effect ->
            val wallet = breadBox.wallet(effect.currencyCode).first()
            val balanceBig = wallet.balance.toBigDecimal()
            val fiatBig = getBalanceInFiat(context, balanceBig, wallet.balance)
            SendSheetEvent.OnBalanceUpdated(balanceBig, fiatBig)
        }
    }

    private fun handleEstimateFee(
        breadBox: BreadBox
    ) = flowTransformer<SendSheetEffect.EstimateFee, SendSheetEvent> { effects ->
        effects.mapLatest { effect ->
            val wallet = breadBox.wallet(effect.currencyCode).first()
            val amount = Amount.create(effect.amount.toDouble(), wallet.unit)
            val networkFee = wallet.feeForSpeed(effect.transferSpeed)

            try {
                val data = wallet.estimateFee(effect.address, amount, networkFee).singleOrNull()
                checkNotNull(data)
                SendSheetEvent.OnNetworkFeeUpdated(data.fee.toBigDecimal(), data)
            } catch (e: FeeEstimationError) {
                logError("Failed get fee estimate", e)
                SendSheetEvent.OnNetworkFeeError
            } catch (e: IllegalStateException) {
                logError("Failed get fee estimate", e)
                SendSheetEvent.OnNetworkFeeError
            }
        }
    }

    private fun showBalanceTooLowForFee(
        router: Router
    ) = { effect: SendSheetEffect.ShowEthTooLowForTokenFee ->
        val res = checkNotNull(router.activity).resources
        // TODO: Handle user acceptance
        val controller = AlertDialogController(
            dialogId = SendSheetController.DIALOG_NO_ETH_FOR_TOKEN_TRANSFER,
            title = res.getString(R.string.Send_insufficientGasTitle),
            message = res.getString(R.string.Send_insufficientGasMessage)
                .format(effect.networkFee.formatCryptoForUi(effect.currencyCode)),
            positiveText = res.getString(R.string.Button_continueAction),
            negativeText = res.getString(R.string.Button_cancel)
        )
        router.pushController(RouterTransaction.with(controller))
    }

    private fun pollExchangeRate(
        context: Context,
        breadBox: BreadBox
    ) = flowTransformer<SendSheetEffect.LoadExchangeRate, SendSheetEvent> { effects ->
        effects.transformLatest { effect ->
            val rates = RatesRepository.getInstance(context)
            val wallet = breadBox.wallet(effect.currencyCode).first()
            val feeCurrencyCode = wallet.unitForFee.currency.code
            while (true) {
                val fiatRate =
                    rates.getFiatForCrypto(BigDecimal.ONE, effect.currencyCode, effect.fiatCode)
                val fiatFeeRate = when {
                    !effect.currencyCode.equals(feeCurrencyCode, false) ->
                        rates.getFiatForCrypto(BigDecimal.ONE, feeCurrencyCode, effect.fiatCode)
                    else -> fiatRate
                }

                emit(SendSheetEvent.OnExchangeRateUpdated(fiatRate, fiatFeeRate, feeCurrencyCode))

                // TODO: Display out of date, invalid (0) rate, etc.
                delay(RATE_UPDATE_MS)
            }
        }
    }

    private fun validateAddress(
        breadBox: BreadBox
    ) = flowTransformer<SendSheetEffect.ValidateAddress, SendSheetEvent> { effects ->
        effects.mapLatest { effect ->
            val wallet = breadBox.wallet(effect.currencyCode).first()
            val address = wallet.addressFor(effect.address)
            val isValid = address != null && !wallet.containsAddress(address)
            SendSheetEvent.OnAddressValidated(
                address = effect.address,
                isValid = isValid,
                clear = effect.clearWhenInvalid && !isValid
            )
        }
    }

    private fun parseClipboard(
        context: Context,
        breadBox: BreadBox
    ): suspend (SendSheetEffect.ParseClipboardData) -> SendSheetEvent = { effect ->
        val text = withContext(Dispatchers.Main) {
            BRClipboardManager.getClipboard(context)
        }

        val cryptoRequest = (text.asLink() as? Link.CryptoRequestUrl)
        val reqAddress = cryptoRequest?.address ?: text
        val reqCurrencyCode = cryptoRequest?.currencyCode

        when {
            text.isNullOrBlank() -> SendSheetEvent.OnAddressPasted.NoAddress
            reqAddress.isNullOrBlank() -> SendSheetEvent.OnAddressPasted.NoAddress
            !reqCurrencyCode.isNullOrBlank() && reqCurrencyCode != effect.currencyCode ->
                SendSheetEvent.OnAddressPasted.InvalidAddress
            else -> {
                val wallet = breadBox.wallet(effect.currencyCode).first()
                val address = wallet.addressFor(reqAddress)
                if (address == null || wallet.containsAddress(address)) {
                    SendSheetEvent.OnAddressPasted.InvalidAddress
                } else {
                    SendSheetEvent.OnAddressPasted.ValidAddress(reqAddress)
                }
            }
        }
    }

    private fun handleSendTransaction(
        breadBox: BreadBox,
        keyStore: KeyStore,
        retainedScope: CoroutineScope,
        outputProducer: () -> Consumer<SendSheetEvent>
    ) = flowTransformer<SendSheetEffect.SendTransaction, SendSheetEvent> { effects ->
        effects
            .mapLatest { effect ->
                val wallet = breadBox.wallet(effect.currencyCode).first()
                val address = wallet.addressFor(effect.address)
                val amount = Amount.create(effect.amount.toDouble(), wallet.unit)
                val feeBasis = effect.transferFeeBasis

                checkNotNull(address) { "Failed to create address." }

                try {
                    val transfer = wallet.createTransfer(address, amount, feeBasis).orNull()
                    checkNotNull(transfer) { "Failed to create transfer." }

                    val phrase = checkNotNull(keyStore.getPhrase())

                    wallet.walletManager.submit(transfer, phrase)
                    SendSheetEvent.OnSendComplete(transfer)
                } catch (e: TransferSubmitError) {
                    logError("Transaction submit failed", e)
                    SendSheetEvent.OnSendFailed
                } catch (e: UserNotAuthenticatedException) {
                    logError("Failed to get phrase.", e)
                    SendSheetEvent.OnSendFailed
                }
            }
            // outputProducer]must return a valid [Consumer] that
            // can accept events even if the original loop has been
            // disposed.
            .onEach { outputProducer().accept(it) }
            // This operation is launched in retainedScope which will
            // not be cancelled when Connection.dispose is called.
            .launchIn(retainedScope)
        // This transformer is bound to a different lifecycle
        emptyFlow()
    }

    private fun handleAddTransactionMetadata(
        metaDataEffectHandler: Connectable<MetaDataEffect, MetaDataEvent>
    ) = flowTransformer<SendSheetEffect.AddTransactionMetaData, SendSheetEvent> { effects ->
        effects
            .map { effect ->
                MetaDataEffect.AddTransactionMetaData(
                    effect.transaction,
                    effect.memo,
                    effect.fiatCurrencyCode,
                    effect.fiatPricePerUnit
                )
            }
            .transform(metaDataEffectHandler)
            .transform { } // Ignore output
    }

    private fun getBalanceInFiat(
        context: Context,
        balanceBig: BigDecimal,
        balanceAmt: Amount
    ) = RatesRepository.getInstance(context)
        .getFiatForCrypto(
            balanceBig,
            balanceAmt.currency.code,
            BRSharedPrefs.getPreferredFiatIso(context)
        ) ?: BigDecimal.ZERO
}
