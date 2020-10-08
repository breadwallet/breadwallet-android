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
package com.breadwallet.ui.send

import android.content.Context
import android.os.Bundle
import android.view.KeyEvent
import android.view.View
import android.view.inputmethod.EditorInfo
import android.widget.EditText
import android.widget.TextView
import androidx.core.os.bundleOf
import androidx.core.view.isVisible
import com.bluelinelabs.conductor.RouterTransaction
import com.breadwallet.R
import com.breadwallet.breadbox.TransferSpeed
import com.breadwallet.breadbox.formatCryptoForUi
import com.breadwallet.breadbox.formatFiatForUi
import com.breadwallet.effecthandler.metadata.MetaDataEffectHandler
import com.breadwallet.legacy.presenter.customviews.BRKeyboard
import com.breadwallet.logger.logError
import com.breadwallet.tools.animation.SlideDetector
import com.breadwallet.tools.animation.UiUtils
import com.breadwallet.tools.manager.BRSharedPrefs
import com.breadwallet.tools.util.BRConstants
import com.breadwallet.tools.util.Link
import com.breadwallet.tools.util.Utils
import com.breadwallet.ui.BaseMobiusController
import com.breadwallet.ui.auth.AuthenticationController
import com.breadwallet.ui.changehandlers.BottomSheetChangeHandler
import com.breadwallet.ui.controllers.AlertDialogController
import com.breadwallet.ui.flowbind.clicks
import com.breadwallet.ui.flowbind.focusChanges
import com.breadwallet.ui.flowbind.textChanges
import com.breadwallet.ui.scanner.ScannerController
import com.breadwallet.ui.send.SendSheet.E
import com.breadwallet.ui.send.SendSheet.E.OnAmountChange
import com.breadwallet.ui.send.SendSheet.F
import com.breadwallet.ui.send.SendSheet.M
import com.breadwallet.util.isErc20
import com.breadwallet.util.isEthereum
import com.spotify.mobius.Connectable
import kotlinx.android.synthetic.main.controller_send_sheet.*
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.merge
import org.kodein.di.direct
import org.kodein.di.erased.instance
import java.math.BigDecimal
import java.text.DecimalFormat
import java.text.NumberFormat
import java.util.Locale
import java.util.concurrent.TimeUnit

private const val CURRENCY_CODE = "CURRENCY_CODE"
private const val CRYPTO_REQUEST_LINK = "CRYPTO_REQUEST_LINK"
private const val RESOLVED_ADDRESS_CHARS = 10
private const val FEE_TIME_PADDING = 1

/** A BottomSheet for sending crypto from the user's wallet to a specified target. */
@Suppress("TooManyFunctions")
class SendSheetController(args: Bundle? = null) :
    BaseMobiusController<M, E, F>(args),
    AuthenticationController.Listener,
    ConfirmTxController.Listener,
    AlertDialogController.Listener,
    ScannerController.Listener {

    companion object {
        const val DIALOG_NO_ETH_FOR_TOKEN_TRANSFER = "adjust_for_fee"
        const val DIALOG_PAYMENT_ERROR = "payment_error"
    }

    /** An empty [SendSheetController] for [currencyCode]. */
    constructor(currencyCode: String) : this(
        bundleOf(CURRENCY_CODE to currencyCode)
    )

    /** A [SendSheetController] to fulfill the provided [Link.CryptoRequestUrl]. */
    constructor(link: Link.CryptoRequestUrl) : this(
        bundleOf(
            CURRENCY_CODE to link.currencyCode,
            CRYPTO_REQUEST_LINK to link
        )
    )

    init {
        overridePushHandler(BottomSheetChangeHandler())
        overridePopHandler(BottomSheetChangeHandler())
    }

    private val currencyCode = arg<String>(CURRENCY_CODE)
    private val cryptoRequestLink = argOptional<Link.CryptoRequestUrl>(CRYPTO_REQUEST_LINK)

    override val layoutId = R.layout.controller_send_sheet
    override val init = SendSheetInit
    override val update = SendSheetUpdate
    override val defaultModel: M
        get() {
            val fiatCode = BRSharedPrefs.getPreferredFiatIso()
            return cryptoRequestLink?.asSendSheetModel(fiatCode)
                ?: M.createDefault(currencyCode, fiatCode)
        }

    override val flowEffectHandler
        get() = SendSheetHandler.create(
            checkNotNull(applicationContext),
            breadBox = direct.instance(),
            uriParser = direct.instance(),
            userManager = direct.instance(),
            apiClient = direct.instance(),
            ratesRepository = direct.instance(),
            metaDataEffectHandler = Connectable {
                MetaDataEffectHandler(it, direct.instance(), direct.instance())
            },
            addressServiceLocator = direct.instance()
        )

    override fun onCreateView(view: View) {
        super.onCreateView(view)

        keyboard.setBRButtonBackgroundResId(R.drawable.keyboard_white_button)
        keyboard.setBRKeyboardColor(R.color.white)
        keyboard.setDeleteImage(R.drawable.ic_delete_black)

        showKeyboard(false)

        textInputAmount.showSoftInputOnFocus = false

        layoutSheetBody.layoutTransition = UiUtils.getDefaultTransition()
        layoutSheetBody.setOnTouchListener(SlideDetector(router, layoutSheetBody))
    }

    override fun onDestroyView(view: View) {
        layoutSheetBody.setOnTouchListener(null)
        super.onDestroyView(view)
    }

    override fun onDetach(view: View) {
        super.onDetach(view)
        Utils.hideKeyboard(activity)
    }

    override fun bindView(modelFlow: Flow<M>): Flow<E> {
        return merge(
            keyboard.bindInput(),
            textInputMemo.bindFocusChanged(),
            textInputMemo.bindActionComplete(E.OnSendClicked),
            textInputMemo.clicks().map { E.OnAmountEditDismissed },
            textInputMemo.textChanges().map { E.OnMemoChanged(it) },
            textInputAddress.focusChanges().map { hasFocus ->
                if (hasFocus) {
                    E.OnAmountEditDismissed
                } else {
                    Utils.hideKeyboard(activity)
                    E.OnTargetStringEntered
                }
            },
            textInputAddress.clicks().map { E.OnAmountEditDismissed },
            textInputAddress.textChanges().map { E.OnTargetStringChanged(it) },
            textInputDestinationTag.textChanges().map {
                E.TransferFieldUpdate.Value(TransferField.DESTINATION_TAG, it)
            },
            textInputHederaMemo.textChanges().map {
                E.TransferFieldUpdate.Value(TransferField.HEDERA_MEMO, it)
            },
            buttonFaq.clicks().map { E.OnFaqClicked },
            buttonScan.clicks().map { E.OnScanClicked },
            buttonSend.clicks().map { E.OnSendClicked },
            buttonClose.clicks().map { E.OnCloseClicked },
            buttonPaste.clicks().map { E.OnPasteClicked },
            layoutSendSheet.clicks().map { E.OnCloseClicked },
            textInputAmount.clicks().map { E.OnAmountEditClicked },
            textInputAmount.focusChanges().map { hasFocus ->
                if (hasFocus) {
                    E.OnAmountEditClicked
                } else {
                    E.OnAmountEditDismissed
                }
            },
            buttonCurrencySelect.clicks().map { E.OnToggleCurrencyClicked },
            buttonRegular.clicks().map { E.OnTransferSpeedChanged(TransferSpeedInput.REGULAR) },
            buttonEconomy.clicks().map { E.OnTransferSpeedChanged(TransferSpeedInput.ECONOMY) },
            buttonPriority.clicks().map { E.OnTransferSpeedChanged(TransferSpeedInput.PRIORITY) }
        )
    }

    private fun EditText.bindActionComplete(output: E) =
        callbackFlow<E> {
            setOnEditorActionListener { _, actionId, event ->
                if (event?.keyCode == KeyEvent.KEYCODE_ENTER
                    || actionId == EditorInfo.IME_ACTION_DONE
                    || actionId == EditorInfo.IME_ACTION_NEXT
                ) {
                    offer(output)
                    Utils.hideKeyboard(activity)
                    true
                } else false
            }
            awaitClose { setOnEditorActionListener(null) }
        }

    private fun EditText.bindFocusChanged() =
        callbackFlow<E> {
            View.OnFocusChangeListener { _, hasFocus ->
                if (hasFocus) {
                    offer(E.OnAmountEditDismissed)
                } else {
                    Utils.hideKeyboard(activity)
                }
            }

            awaitClose { onFocusChangeListener = null }
        }

    private fun BRKeyboard.bindInput() =
        callbackFlow<E> {
            setOnInsertListener { key ->
                when {
                    key.isEmpty() -> offer(OnAmountChange.Delete)
                    key[0] == '.' -> offer(OnAmountChange.AddDecimal)
                    Character.isDigit(key[0]) -> offer(OnAmountChange.AddDigit(key.toInt()))
                    else -> return@setOnInsertListener
                }
            }
            awaitClose { setOnInsertListener(null) }
        }

    @Suppress("ComplexMethod", "LongMethod")
    override fun M.render() {
        val res = checkNotNull(resources)

        ifChanged(M::addressType, M::isResolvingAddress) {
            addressProgressBar.isVisible = isResolvingAddress
            if (addressType is AddressType.Resolvable) {
                inputLayoutAddress.hint = res.getString(
                    if (addressType is AddressType.Resolvable.PayId) R.string.Send_payId_toLabel else R.string.Send_fio_toLabel
                )
                inputLayoutAddress.helperText = if (isResolvingAddress) null else {
                    val first = targetAddress.take(RESOLVED_ADDRESS_CHARS)
                    val last = targetAddress.takeLast(RESOLVED_ADDRESS_CHARS)
                    "$first...$last"
                }
            } else {
                inputLayoutAddress.helperText = null
                inputLayoutAddress.hint = res.getString(R.string.Send_toLabel)
            }
        }

        ifChanged(M::targetInputError) {
            inputLayoutAddress.isErrorEnabled = targetInputError != null
            inputLayoutAddress.error = when (targetInputError) {
                is M.InputError.Empty ->
                    res.getString(R.string.Send_noAddress)
                is M.InputError.Invalid ->
                    res.getString(R.string.Send_invalidAddressMessage, currencyCode.toUpperCase())
                is M.InputError.ClipboardInvalid ->
                    res.getString(
                        R.string.Send_invalidAddressOnPasteboard,
                        currencyCode.toUpperCase()
                    )
                is M.InputError.ClipboardEmpty ->
                    res.getString(R.string.Send_emptyPasteboard)
                is M.InputError.PayIdInvalid -> res.getString(R.string.Send_payId_invalid)
                is M.InputError.PayIdNoAddress -> res.getString(
                    R.string.Send_payId_noAddress,
                    currencyCode.toUpperCase(Locale.ROOT)
                )
                is M.InputError.PayIdRetrievalError -> res.getString(R.string.Send_payId_retrievalError)
                is M.InputError.FioInvalid -> res.getString(R.string.Send_fio_invalid)
                is M.InputError.FioNoAddress -> res.getString(
                    R.string.Send_fio_noAddress,
                    currencyCode.toUpperCase(Locale.ROOT)
                )
                is M.InputError.FioRetrievalError -> res.getString(R.string.Send_fio_retrievalError)

                else -> null
            }
        }

        ifChanged(M::amountInputError) {
            inputLayoutAmount.isErrorEnabled = amountInputError != null
            inputLayoutAmount.error = when (amountInputError) {
                is M.InputError.Empty ->
                    res.getString(R.string.Send_noAmount)
                is M.InputError.BalanceTooLow ->
                    res.getString(R.string.Send_insufficientFunds)
                is M.InputError.FailedToEstimateFee ->
                    res.getString(R.string.Send_noFeesError)
                else -> null
            }
        }

        ifChanged(
            M::currencyCode,
            M::fiatCode,
            M::isAmountCrypto
        ) {
            val sendTitle = res.getString(R.string.Send_title)
            val upperCaseCurrencyCode = currencyCode.toUpperCase(Locale.getDefault())
            labelTitle.text = "%s %s".format(sendTitle, upperCaseCurrencyCode)
            buttonCurrencySelect.text = when {
                isAmountCrypto -> upperCaseCurrencyCode
                else -> {
                    val currency = java.util.Currency.getInstance(fiatCode)
                    "$fiatCode (${currency.symbol})".toUpperCase(Locale.getDefault())
                }
            }
        }

        ifChanged(M::isAmountEditVisible, ::showKeyboard)

        ifChanged(
            M::rawAmount,
            M::isAmountCrypto,
            M::fiatCode
        ) {
            val formattedAmount = if (isAmountCrypto || rawAmount.isBlank()) {
                rawAmount
            } else {
                rawAmount.formatFiatForInputUi(fiatCode)
            }
            textInputAmount.setText(formattedAmount)
        }

        ifChanged(
            M::networkFee,
            M::fiatNetworkFee,
            M::feeCurrencyCode,
            M::isAmountCrypto
        ) {
            labelNetworkFee.isVisible = networkFee != BigDecimal.ZERO
            labelNetworkFee.text = res.getString(
                R.string.Send_fee,
                when {
                    isAmountCrypto ->
                        networkFee.formatCryptoForUi(feeCurrencyCode, MAX_DIGITS)
                    else -> fiatNetworkFee.formatFiatForUi(fiatCode)
                }
            )
        }

        ifChanged(
            M::balance,
            M::fiatBalance,
            M::isAmountCrypto
        ) {
            labelBalance.text = res.getString(
                R.string.Send_balance,
                when {
                    isAmountCrypto -> balance.formatCryptoForUi(currencyCode)
                    else -> fiatBalance.formatFiatForUi(fiatCode)
                }
            )
        }

        ifChanged(M::targetString) {
            if (textInputAddress.text.toString() != targetString) {
                textInputAddress.setText(targetString, TextView.BufferType.EDITABLE)
            }
        }

        ifChanged(M::memo) {
            if (textInputMemo.text.toString() != memo) {
                textInputMemo.setText(memo, TextView.BufferType.EDITABLE)
            }
        }

        ifChanged(
            M::showFeeSelect,
            M::transferSpeed
        ) {
            layoutFeeOption.isVisible = showFeeSelect
            setFeeOption(transferSpeed)
        }

        ifChanged(M::showFeeSelect) {
            layoutFeeOption.isVisible = showFeeSelect
        }

        ifChanged(M::isConfirmingTx) {
            val isConfirmVisible =
                router.backstack.lastOrNull()?.controller() is ConfirmTxController
            if (isConfirmingTx && !isConfirmVisible) {
                val controller = ConfirmTxController(
                    currencyCode,
                    fiatCode,
                    feeCurrencyCode,
                    targetAddress,
                    transferSpeed,
                    amount,
                    fiatAmount,
                    fiatTotalCost,
                    fiatNetworkFee,
                    transferFields
                )
                controller.targetController = this@SendSheetController
                router.pushController(RouterTransaction.with(controller))
            }
        }

        ifChanged(M::isAuthenticating) {
            val isAuthVisible =
                router.backstack.lastOrNull()?.controller() is AuthenticationController
            if (isAuthenticating && !isAuthVisible) {
                val authenticationMode = if (isFingerprintAuthEnable) {
                    AuthenticationController.Mode.USER_PREFERRED
                } else {
                    AuthenticationController.Mode.PIN_REQUIRED
                }
                val controller = AuthenticationController(
                    mode = authenticationMode,
                    title = res.getString(R.string.VerifyPin_touchIdMessage),
                    message = res.getString(R.string.VerifyPin_authorize)
                )
                controller.targetController = this@SendSheetController
                router.pushController(RouterTransaction.with(controller))
            }
        }

        ifChanged(M::isBitpayPayment) {
            textInputAddress.isEnabled = !isBitpayPayment
            textInputAmount.isEnabled = !isBitpayPayment
            buttonScan.isVisible = !isBitpayPayment
            buttonPaste.isVisible = !isBitpayPayment
        }

        ifChanged(M::isFetchingPayment, M::isSendingTransaction) {
            loadingView.isVisible = isFetchingPayment || isSendingTransaction
        }

        ifChanged(M::destinationTag) {
            if (destinationTag != null) {
                groupDestinationTag.isVisible = true
                inputLayoutDestinationTag.error = if (destinationTag.invalid) {
                    res.getString(R.string.Send_destinationTag_required_error)
                } else null
                inputLayoutDestinationTag.hint = res.getString(
                    when {
                        destinationTag.required ->
                            R.string.Send_destinationTag_required
                        else -> R.string.Send_destinationTag_optional
                    }
                )

                if ((destinationTag.value.isNullOrBlank() &&
                        !textInputDestinationTag.text.isNullOrBlank()) ||
                    (!destinationTag.value.isNullOrBlank() &&
                        textInputDestinationTag.text.isNullOrBlank()) || isDestinationTagFromResolvedAddress
                ) {
                    textInputDestinationTag.setText(currentModel.destinationTag?.value)
                }

                textInputDestinationTag.isEnabled = !isDestinationTagFromResolvedAddress
            }
        }

        ifChanged(M::hederaMemo) {
            if (hederaMemo != null) {
                groupHederaMemo.isVisible = true
                if (!hederaMemo.value.isNullOrBlank() &&
                    textInputHederaMemo.text.isNullOrBlank()
                ) {
                    textInputHederaMemo.setText(currentModel.hederaMemo?.value)
                }
            }
        }
    }

    private fun showKeyboard(show: Boolean) {
        groupAmountSection.isVisible = show
        if (show) {
            Utils.hideKeyboard(activity)
        }
    }

    override fun onLinkScanned(link: Link) {
        if (link is Link.CryptoRequestUrl) {
            eventConsumer.accept(E.OnRequestScanned(link))
        }
    }

    override fun onAuthenticationSuccess() {
        eventConsumer.accept(E.OnAuthSuccess)
    }

    override fun onAuthenticationCancelled() {
        eventConsumer.accept(E.OnAuthCancelled)
    }

    override fun onPositiveClicked(
        dialogId: String,
        controller: AlertDialogController,
        result: AlertDialogController.DialogInputResult
    ) {
        when (dialogId) {
            DIALOG_NO_ETH_FOR_TOKEN_TRANSFER -> {
                eventConsumer.accept(E.GoToEthWallet)
            }
        }
    }

    override fun onPositiveClicked(controller: ConfirmTxController) {
        eventConsumer
            .accept(E.ConfirmTx.OnConfirmClicked)
    }

    override fun onNegativeClicked(controller: ConfirmTxController) {
        eventConsumer
            .accept(E.ConfirmTx.OnCancelClicked)
    }

    private fun setFeeOption(feeOption: TransferSpeed) {
        val context = applicationContext!!
        // TODO: Redo using a toggle button and a selector
        when (feeOption) {
            is TransferSpeed.Regular -> {
                buttonRegular.setTextColor(context.getColor(R.color.white))
                buttonRegular.background = context.getDrawable(R.drawable.b_blue_square)
                buttonEconomy.setTextColor(context.getColor(R.color.dark_blue))
                buttonEconomy.background = context.getDrawable(R.drawable.b_half_left_blue_stroke)
                buttonPriority.setTextColor(context.getColor(R.color.dark_blue))
                buttonPriority.background =
                    context.getDrawable(R.drawable.b_half_right_blue_stroke)
                labelFeeDescription.text = when {
                    feeOption.currencyCode.run {
                        isEthereum() || isErc20()
                    } -> ethFeeEstimateString(context, feeOption.targetTime)
                    else -> context.getString(R.string.FeeSelector_estimatedDeliver)
                        .format(context.getString(R.string.FeeSelector_regularTime))
                }
                labelFeeWarning.visibility = View.GONE
            }
            is TransferSpeed.Economy -> {
                buttonRegular.setTextColor(context.getColor(R.color.dark_blue))
                buttonRegular.background = context.getDrawable(R.drawable.b_blue_square_stroke)
                buttonEconomy.setTextColor(context.getColor(R.color.white))
                buttonEconomy.background = context.getDrawable(R.drawable.b_half_left_blue)
                buttonPriority.setTextColor(context.getColor(R.color.dark_blue))
                buttonPriority.background =
                    context.getDrawable(R.drawable.b_half_right_blue_stroke)
                labelFeeDescription.text = when {
                    feeOption.currencyCode.run {
                        isEthereum() || isErc20()
                    } -> ethFeeEstimateString(context, feeOption.targetTime)
                    else -> context.getString(R.string.FeeSelector_estimatedDeliver)
                        .format(context.getString(R.string.FeeSelector_economyTime))
                }
                labelFeeWarning.visibility = View.VISIBLE
            }
            is TransferSpeed.Priority -> {
                buttonRegular.setTextColor(context.getColor(R.color.dark_blue))
                buttonRegular.background = context.getDrawable(R.drawable.b_blue_square_stroke)
                buttonEconomy.setTextColor(context.getColor(R.color.dark_blue))
                buttonEconomy.background = context.getDrawable(R.drawable.b_half_left_blue_stroke)
                buttonPriority.setTextColor(context.getColor(R.color.white))
                buttonPriority.background = context.getDrawable(R.drawable.b_half_right_blue)
                labelFeeDescription.text = when {
                    feeOption.currencyCode.run {
                        isEthereum() || isErc20()
                    } -> ethFeeEstimateString(context, feeOption.targetTime)
                    else -> context.getString(R.string.FeeSelector_estimatedDeliver)
                        .format(context.getString(R.string.FeeSelector_priorityTime))
                }
                labelFeeWarning.visibility = View.GONE
            }
        }
    }

    private fun ethFeeEstimateString(context: Context, time: Long): String {
        val minutes = TimeUnit.MILLISECONDS.toMinutes(time) + FEE_TIME_PADDING
        return context.getString(R.string.FeeSelector_estimatedDeliver)
            .format(context.getString(R.string.FeeSelector_lessThanMinutes, minutes))
    }
}

fun String.formatFiatForInputUi(currencyCode: String): String {
    // Ensure decimal displayed when string has not fraction digits
    val forceSeparator = contains('.')
    // Ensure all fraction digits are displayed, even if they are all zero
    val minFractionDigits = substringAfter('.', "").count()

    val amount = toBigDecimalOrNull()

    val currencyFormat = NumberFormat.getCurrencyInstance(Locale.getDefault()) as DecimalFormat
    val decimalFormatSymbols = currencyFormat.decimalFormatSymbols
    currencyFormat.isGroupingUsed = true
    currencyFormat.roundingMode = BRConstants.ROUNDING_MODE
    currencyFormat.isDecimalSeparatorAlwaysShown = forceSeparator
    try {
        val currency = java.util.Currency.getInstance(currencyCode)
        val symbol = currency.symbol
        decimalFormatSymbols.currencySymbol = symbol
        currencyFormat.decimalFormatSymbols = decimalFormatSymbols
        currencyFormat.negativePrefix = "-$symbol"
        currencyFormat.maximumFractionDigits = MAX_DIGITS
        currencyFormat.minimumFractionDigits = minFractionDigits
    } catch (e: IllegalArgumentException) {
        logError("Illegal Currency code: $currencyCode")
    }

    return currencyFormat.format(amount)
}
