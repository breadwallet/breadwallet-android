/**
 * BreadWallet
 *
 * Created by Drew Carlson <drew.carlson@breadwallet.com> on 8/13/19.
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
import com.breadwallet.breadbox.hashString
import com.breadwallet.breadbox.toBigDecimal
import com.breadwallet.crypto.Address
import com.breadwallet.crypto.Amount
import com.breadwallet.crypto.Transfer
import com.breadwallet.crypto.TransferState
import com.breadwallet.crypto.errors.FeeEstimationError
import com.breadwallet.effecthandler.metadata.MetaDataEffect
import com.breadwallet.effecthandler.metadata.MetaDataEvent
import com.breadwallet.ext.isZero
import com.breadwallet.logger.logError
import com.breadwallet.logger.logWarning
import com.breadwallet.repository.RatesRepository
import com.breadwallet.tools.manager.BRClipboardManager
import com.breadwallet.tools.manager.BRSharedPrefs
import com.breadwallet.tools.security.BrdUserManager
import com.breadwallet.tools.security.isFingerPrintAvailableAndSetup
import com.breadwallet.tools.util.BRConstants
import com.breadwallet.tools.util.Link
import com.breadwallet.tools.util.asLink
import com.breadwallet.ui.controllers.AlertDialogController
import com.breadwallet.ui.navigation.NavEffectTransformer
import com.breadwallet.ui.send.SendSheet.E
import com.breadwallet.ui.send.SendSheet.F
import com.breadwallet.util.HEADER_BITPAY_PARTNER
import com.breadwallet.util.HEADER_BITPAY_PARTNER_KEY
import com.breadwallet.util.buildPaymentProtocolRequest
import com.breadwallet.util.getAcceptHeader
import com.breadwallet.util.getContentTypeHeader
import com.breadwallet.util.getPaymentRequestHeader
import com.platform.APIClient
import com.spotify.mobius.Connectable
import com.spotify.mobius.functions.Consumer
import drewcarlson.mobius.flow.flowTransformer
import drewcarlson.mobius.flow.subtypeEffectHandler
import drewcarlson.mobius.flow.transform
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.transform
import kotlinx.coroutines.flow.transformLatest
import kotlinx.coroutines.withContext
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.math.BigDecimal

private const val RATE_UPDATE_MS = 60_000L

object SendSheetHandler {

    @Suppress("LongParameterList")
    fun create(
        context: Context,
        router: Router,
        retainedScope: CoroutineScope,
        outputProducer: () -> Consumer<E>,
        breadBox: BreadBox,
        userManager: BrdUserManager,
        apiClient: APIClient,
        ratesRepository: RatesRepository,
        navEffectHandler: NavEffectTransformer,
        metaDataEffectHandler: Connectable<MetaDataEffect, MetaDataEvent>
    ) = subtypeEffectHandler<F, E> {
        addTransformer(pollExchangeRate(breadBox, ratesRepository))
        addTransformer(handleLoadBalance(context, breadBox, ratesRepository))
        addTransformer(validateAddress(breadBox))
        addTransformer(handleEstimateFee(breadBox))
        addTransformer<F.Nav>(navEffectHandler)
        addTransformer(
            handleSendTransaction(
                breadBox,
                userManager,
                retainedScope,
                outputProducer
            )
        )
        addTransformer(handleAddTransactionMetadata(metaDataEffectHandler))
        addTransformer(handleLoadCryptoRequestData(breadBox, apiClient, context))
        addTransformer(
            handleContinueWithPayment(
                userManager,
                breadBox,
                retainedScope,
                outputProducer
            )
        )
        addTransformer(handlePostPayment(apiClient))
        addFunction(parseClipboard(context, breadBox))
        addFunction(handleGetTransferFields(breadBox))
        addFunction(handleValidateTransferFields(breadBox))
        addConsumerSync(Dispatchers.Main, showBalanceTooLowForFee(router))
        addConsumerSync(Dispatchers.Main, showErrorDialog(router))
        addConsumerSync(Dispatchers.Main, showTransferFailed(router))

        addFunctionSync<F.LoadAuthenticationSettings> {
            val isEnabled =
                isFingerPrintAvailableAndSetup(context) && BRSharedPrefs.sendMoneyWithFingerprint
            E.OnAuthenticationSettingsUpdated(isEnabled)
        }
    }

    private fun handleGetTransferFields(
        breadBox: BreadBox
    ): suspend (F.GetTransferFields) -> E = { (currencyCode, targetAddress) ->
        val wallet = breadBox.wallet(currencyCode).first()
        val network = wallet.walletManager.network
        val address = Address.create(targetAddress, network).orNull()
        val fields = when (address) {
            null -> wallet.transferAttributes
            else -> wallet.getTransferAttributesFor(address)
        }.map { attribute ->
            TransferField(
                attribute.key,
                attribute.isRequired,
                false,
                attribute.value.orNull()
            )
        }
        E.OnTransferFieldsUpdated(fields)
    }

    private fun handleValidateTransferFields(
        breadBox: BreadBox
    ): suspend (F.ValidateTransferFields) -> E = { effect ->
        val (currencyCode, targetAddress, transferFields) = effect

        val wallet = breadBox.wallet(currencyCode).first()
        val network = wallet.walletManager.network
        val address = Address.create(targetAddress, network).orNull()

        val validatedFields = when (address) {
            null -> wallet.transferAttributes
            else -> wallet.getTransferAttributesFor(address)
        }.mapNotNull { attribute ->
            val field = transferFields.find { it.key == attribute.key }
            if (field != null) {
                attribute.setValue(field.value)
                field.copy(
                    invalid = wallet.validateTransferAttribute(attribute).isPresent
                )
            } else null
        }

        E.OnTransferFieldsUpdated(validatedFields)
    }

    private fun handleLoadBalance(
        context: Context,
        breadBox: BreadBox,
        rates: RatesRepository
    ) = flowTransformer<F.LoadBalance, E> { effects ->
        effects.map { effect ->
            val wallet = breadBox.wallet(effect.currencyCode).first()
            val balanceMin = wallet.balanceMinimum.orNull()?.toBigDecimal() ?: BigDecimal.ZERO
            val balanceBig =
                (wallet.balance.toBigDecimal() - balanceMin).coerceAtLeast(BigDecimal.ZERO)
            val fiatBig = getBalanceInFiat(context, balanceBig, wallet.balance, rates)
            val feeCurrencyBalance = if (effect.currencyCode.equals(effect.feeCurrencyCode, true)) {
                balanceBig
            } else {
                val feeWallet = breadBox.wallet(effect.feeCurrencyCode).first()
                val feeBalanceMin =
                    feeWallet.balanceMinimum.orNull()?.toBigDecimal() ?: BigDecimal.ZERO
                (feeWallet.balance.toBigDecimal() - feeBalanceMin).coerceAtLeast(BigDecimal.ZERO)
            }
            E.OnBalanceUpdated(balanceBig, fiatBig, feeCurrencyBalance)
        }
    }

    private fun handleEstimateFee(
        breadBox: BreadBox
    ) = flowTransformer<F.EstimateFee, E> { effects ->
        effects.mapNotNull { effect ->
            val wallet = breadBox.wallet(effect.currencyCode).first()

            // Skip if address is not valid
            val address = wallet.addressFor(effect.address) ?: return@mapNotNull null
            if (wallet.containsAddress(address))
                return@mapNotNull null

            val amount = Amount.create(effect.amount.toDouble(), wallet.unit)
            val networkFee = wallet.feeForSpeed(effect.transferSpeed)

            try {
                val data = wallet.estimateFee(address, amount, networkFee)
                val fee = data.fee.toBigDecimal()
                check(!fee.isZero()) { "Estimated fee was zero" }
                E.OnNetworkFeeUpdated(effect.address, effect.amount, fee, data)
            } catch (e: FeeEstimationError) {
                logError("Failed get fee estimate", e)
                E.OnNetworkFeeError
            } catch (e: IllegalStateException) {
                logError("Failed get fee estimate", e)
                E.OnNetworkFeeError
            }
        }
    }

    private fun showBalanceTooLowForFee(
        router: Router
    ) = { effect: F.ShowEthTooLowForTokenFee ->
        val res = checkNotNull(router.activity).resources
        val controller = AlertDialogController(
            dialogId = SendSheetController.DIALOG_NO_ETH_FOR_TOKEN_TRANSFER,
            title = res.getString(R.string.Send_insufficientGasTitle),
            message = res.getString(R.string.Send_insufficientGasMessage)
                .format(effect.networkFee.formatCryptoForUi(effect.currencyCode)),
            positiveText = res.getString(R.string.Button_continueAction),
            negativeText = res.getString(R.string.Button_cancel)
        )
        controller.targetController = router.backstack.last().controller()
        router.pushController(RouterTransaction.with(controller))
    }

    private fun pollExchangeRate(
        breadBox: BreadBox,
        rates: RatesRepository
    ) = flowTransformer<F.LoadExchangeRate, E> { effects ->
        effects.transformLatest { effect ->
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

                emit(
                    E.OnExchangeRateUpdated(
                        fiatPricePerUnit = fiatRate ?: BigDecimal.ZERO,
                        fiatPricePerFeeUnit = fiatFeeRate ?: BigDecimal.ZERO,
                        feeCurrencyCode = feeCurrencyCode
                    )
                )

                // TODO: Display out of date, invalid (0) rate, etc.
                delay(RATE_UPDATE_MS)
            }
        }
    }

    private fun validateAddress(
        breadBox: BreadBox
    ) = flowTransformer<F.ValidateAddress, E> { effects ->
        effects.mapLatest { effect ->
            val wallet = breadBox.wallet(effect.currencyCode).first()
            val address = wallet.addressFor(effect.address)
            val isValid = address != null && !wallet.containsAddress(address)
            E.OnAddressValidated(
                address = effect.address,
                isValid = isValid
            )
        }
    }

    private fun parseClipboard(
        context: Context,
        breadBox: BreadBox
    ): suspend (F.ParseClipboardData) -> E = { effect ->
        val text = withContext(Dispatchers.Main) {
            BRClipboardManager.getClipboard(context)
        }

        val cryptoRequest = (text.asLink() as? Link.CryptoRequestUrl)
        val reqAddress = cryptoRequest?.address ?: text
        val reqCurrencyCode = cryptoRequest?.currencyCode

        when {
            text.isNullOrBlank() -> E.OnAddressPasted.NoAddress
            reqAddress.isNullOrBlank() -> E.OnAddressPasted.NoAddress
            !reqCurrencyCode.isNullOrBlank() &&
                reqCurrencyCode != effect.currencyCode &&
                reqCurrencyCode != effect.feeCurrencyCode ->
                E.OnAddressPasted.InvalidAddress
            else -> {
                val wallet = breadBox.wallet(effect.currencyCode).first()
                val address = wallet.addressFor(reqAddress)
                if (address == null || wallet.containsAddress(address)) {
                    E.OnAddressPasted.InvalidAddress
                } else {
                    E.OnAddressPasted.ValidAddress(reqAddress)
                }
            }
        }
    }

    private fun handleSendTransaction(
        breadBox: BreadBox,
        userManager: BrdUserManager,
        retainedScope: CoroutineScope,
        outputProducer: () -> Consumer<E>
    ) = flowTransformer<F.SendTransaction, E> { effects ->
        effects
            .mapLatest { effect ->
                val wallet = breadBox.wallet(effect.currencyCode).first()
                val address = wallet.addressFor(effect.address)
                val amount = Amount.create(effect.amount.toDouble(), wallet.unit)
                val feeBasis = effect.transferFeeBasis
                val fields = effect.transferFields

                if (address == null || wallet.containsAddress(address)) {
                    return@mapLatest E.OnAddressValidated(effect.address, false)
                }

                val attributes = wallet.getTransferAttributesFor(address)
                attributes.forEach { attribute ->
                    fields.find { it.key == attribute.key }
                        ?.let { field ->
                            attribute.setValue(field.value)
                        }
                }

                if (attributes.any { wallet.validateTransferAttribute(it).isPresent }) {
                    return@mapLatest E.OnSendFailed
                }

                val phrase = try {
                    checkNotNull(userManager.getPhrase())
                } catch (e: UserNotAuthenticatedException) {
                    logError("Failed to get phrase.", e)
                    return@mapLatest E.OnSendFailed
                }

                val newTransfer =
                    wallet.createTransfer(address, amount, feeBasis, attributes).orNull()

                if (newTransfer == null) {
                    logError("Failed to create transfer.")
                    E.OnSendFailed
                } else {
                    wallet.walletManager.submit(newTransfer, phrase)

                    val hash = newTransfer.hashString()

                    breadBox.walletTransfer(effect.currencyCode, hash)
                        .mapToSendEvent()
                        .first()
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
    ) = flowTransformer<F.AddTransactionMetaData, E> { effects ->
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
        balanceAmt: Amount,
        rates: RatesRepository
    ) = rates.getFiatForCrypto(
        balanceBig,
        balanceAmt.currency.code,
        BRSharedPrefs.getPreferredFiatIso(context)
    ) ?: BigDecimal.ZERO

    private fun handleLoadCryptoRequestData(
        breadBox: BreadBox,
        apiClient: APIClient,
        context: Context
    ) = flowTransformer<F.PaymentProtocol.LoadPaymentData, E.PaymentProtocol> { effects ->
        effects.map { effect ->
            val acceptHeader = effect.cryptoRequestUrl.currencyCode.getPaymentRequestHeader()
            val request: Request =
                Request.Builder().url(effect.cryptoRequestUrl.rUrlParam.orEmpty()).get()
                    .addHeader(BRConstants.HEADER_ACCEPT, acceptHeader)
                    .build()
            val response = apiClient.sendRequest(request, false)

            if (response.isSuccessful) {
                val wallet = breadBox.wallet(effect.cryptoRequestUrl.currencyCode).first()
                val paymentProtocolRequest = buildPaymentProtocolRequest(wallet, response)
                if (paymentProtocolRequest != null) {
                    E.PaymentProtocol.OnPaymentLoaded(
                        paymentProtocolRequest,
                        paymentProtocolRequest.totalAmount.get().convert(wallet.unit).get()
                            .toBigDecimal()
                    )
                } else {
                    E.PaymentProtocol.OnLoadFailed(
                        context.getString(R.string.PaymentProtocol_Errors_badPaymentRequest)
                    )
                }
            } else {
                E.PaymentProtocol.OnLoadFailed(context.getString(R.string.Send_remoteRequestError))
            }
        }
    }

    private fun handleContinueWithPayment(
        userManager: BrdUserManager,
        breadBox: BreadBox,
        retainedScope: CoroutineScope,
        outputProducer: () -> Consumer<E>
    ) = flowTransformer<F.PaymentProtocol.ContinueWitPayment, E> { effects ->
        effects
            .mapLatest { effect ->
                val paymentRequest = effect.paymentProtocolRequest
                val transfer = paymentRequest.createTransfer(effect.transferFeeBasis).orNull()
                checkNotNull(transfer) { "Failed to create transfer." }

                val phrase = try {
                    checkNotNull(userManager.getPhrase())
                } catch (e: UserNotAuthenticatedException) {
                    logError("Failed to get phrase.", e)
                    return@mapLatest E.OnSendFailed
                }

                check(paymentRequest.signTransfer(transfer, phrase)) {
                    "Failed to sign transfer"
                }

                paymentRequest.submitTransfer(transfer)

                val currencyCode = transfer.wallet.currency.code
                val transferHash = transfer.hashString()

                breadBox.walletTransfer(currencyCode, transferHash)
                    .mapToSendEvent()
                    .first()
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

    private fun handlePostPayment(
        apiClient: APIClient
    ) = flowTransformer<F.PaymentProtocol.PostPayment, E.PaymentProtocol> { effects ->
        effects
            .mapLatest { effect ->
                val paymentRequest = effect.paymentProtocolRequest
                val payment = paymentRequest.createPayment(effect.transfer).orNull()
                checkNotNull(payment) { "failed to create payment" }

                val encodedPayment = payment.encode().orNull()
                checkNotNull(encodedPayment) { "failed to encode payment" }

                val request: Request =
                    Request.Builder().url(paymentRequest.paymentUrl.get()).get()
                        .addHeader(
                            BRConstants.HEADER_ACCEPT, paymentRequest.getAcceptHeader()
                        )
                        .addHeader(
                            BRConstants.HEADER_CONTENT_TYPE,
                            paymentRequest.getContentTypeHeader()
                        )
                        .addHeader(
                            HEADER_BITPAY_PARTNER_KEY, HEADER_BITPAY_PARTNER
                        )
                        .post(encodedPayment.toRequestBody())
                        .build()

                if (apiClient.sendRequest(request, false).isSuccessful) {
                    E.PaymentProtocol.OnPostCompleted
                } else {
                    logWarning("Failed to post payment to bitpay")
                    E.PaymentProtocol.OnPostFailed
                }
            }
    }

    private fun showErrorDialog(
        router: Router
    ) = { effect: F.ShowErrorDialog ->
        val res = checkNotNull(router.activity).resources
        val controller = AlertDialogController(
            dialogId = SendSheetController.DIALOG_PAYMENT_ERROR,
            title = res.getString(R.string.Alert_error),
            message = effect.message,
            positiveText = res.getString(R.string.Button_ok)
        )
        router.pushController(RouterTransaction.with(controller))
    }

    private fun showTransferFailed(
        router: Router
    ) = { _: F.ShowTransferFailed ->
        val res = checkNotNull(router.activity).resources
        val controller = AlertDialogController(
            title = res.getString(R.string.Alert_error),
            message = res.getString(R.string.Send_publishTransactionError),
            positiveText = res.getString(R.string.Button_ok)
        )
        router.pushController(RouterTransaction.with(controller))
    }
}

/**
 * Map the post-submit transfer state to an [E] result or fail.
 */
private fun Flow<Transfer>.mapToSendEvent(): Flow<E> =
    mapNotNull { transfer ->
        when (checkNotNull(transfer.state.type)) {
            TransferState.Type.INCLUDED,
            TransferState.Type.PENDING,
            TransferState.Type.SUBMITTED -> E.OnSendComplete(transfer)
            TransferState.Type.DELETED,
            TransferState.Type.FAILED -> {
                logError("Failed to submit transfer ${transfer.state.failedError.orNull()}")
                E.OnSendFailed
            }
            // Ignore pre-submit states
            TransferState.Type.CREATED,
            TransferState.Type.SIGNED -> null
        }
    }
