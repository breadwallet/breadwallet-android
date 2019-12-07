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

import android.os.Bundle
import android.view.KeyEvent
import android.view.View
import android.view.inputmethod.EditorInfo
import android.widget.TextView
import androidx.core.os.bundleOf
import androidx.core.view.isVisible
import com.bluelinelabs.conductor.RouterTransaction
import com.bluelinelabs.conductor.changehandler.FadeChangeHandler
import com.breadwallet.R
import com.breadwallet.breadbox.BreadBoxEffect
import com.breadwallet.breadbox.BreadBoxEffectHandler
import com.breadwallet.breadbox.BreadBoxEvent
import com.breadwallet.breadbox.SendTx
import com.breadwallet.breadbox.SendTxEffectHandler
import com.breadwallet.breadbox.TransferSpeed
import com.breadwallet.breadbox.formatCryptoForUi
import com.breadwallet.breadbox.formatFiatForUi
import com.breadwallet.crypto.Currency
import com.breadwallet.effecthandler.metadata.MetaDataEffect
import com.breadwallet.effecthandler.metadata.MetaDataEffectHandler
import com.breadwallet.effecthandler.metadata.MetaDataEvent
import com.breadwallet.legacy.presenter.entities.CryptoRequest
import com.breadwallet.mobius.CompositeEffectHandler
import com.breadwallet.mobius.nestedConnectable
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
import com.breadwallet.ui.navigation.NavigationEffect
import com.breadwallet.ui.navigation.RouterNavigationEffectHandler
import com.breadwallet.ui.scanner.ScannerController
import com.breadwallet.ui.send.SendSheetEvent.OnAmountChange
import com.breadwallet.ui.view
import com.spotify.mobius.Connectable
import com.spotify.mobius.functions.Consumer
import kotlinx.android.synthetic.main.controller_send_sheet.*
import org.kodein.di.direct
import org.kodein.di.erased.instance
import java.math.BigDecimal
import java.util.Locale

private const val CURRENCY_CODE = "CURRENCY_CODE"
private const val CRYPTO_REQUEST = "CRYPTO_REQUEST"
private const val CRYPTO_REQUEST_LINK = "CRYPTO_REQUEST_LINK"

/** A BottomSheet for sending crypto from the user's wallet to a specified target. */
@Suppress("TooManyFunctions")
class SendSheetController(args: Bundle? = null) :
    BaseMobiusController<SendSheetModel, SendSheetEvent, SendSheetEffect>(args),
    AuthenticationController.Listener,
    AlertDialogController.Listener,
    ScannerController.Listener {

    companion object {
        const val DIALOG_NO_ETH_FOR_TOKEN_TRANSFER = "adjust_for_fee"
    }

    /** An empty [SendSheetController] for [currencyCode]. */
    constructor(currencyCode: String) : this(
        bundleOf(CURRENCY_CODE to currencyCode)
    )

    /** A [SendSheetController] to fulfill the provided [CryptoRequest]. */
    constructor(cryptoRequest: CryptoRequest) : this(
        bundleOf(
            CURRENCY_CODE to cryptoRequest.currencyCode,
            CRYPTO_REQUEST to cryptoRequest
        )
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
    private val cryptoRequest = argOptional<CryptoRequest>(CRYPTO_REQUEST)
    private val cryptoRequestLink = argOptional<Link.CryptoRequestUrl>(CRYPTO_REQUEST_LINK)

    override val layoutId = R.layout.controller_send_sheet
    override val init = SendSheetInit
    override val update = SendSheetUpdate
    override val defaultModel: SendSheetModel
        get() {
            val fiatCode = BRSharedPrefs.getPreferredFiatIso()
            return cryptoRequest?.asSendSheetModel(fiatCode)
                ?: cryptoRequestLink?.asSendSheetModel(fiatCode)
                ?: SendSheetModel.createDefault(currencyCode, fiatCode)
        }

    override val effectHandler: Connectable<SendSheetEffect, SendSheetEvent> =
        CompositeEffectHandler.from(
            nestedConnectable({
                SendTxEffectHandler(
                    Consumer { event ->
                        when (event) {
                            is SendTx.Result.Success -> SendSheetEvent.OnSendComplete(event.transfer)
                            is SendTx.Result.Error -> SendSheetEvent.OnSendFailed
                        }.run(eventConsumer::accept)
                    },
                    controllerScope,
                    direct.instance(),
                    direct.instance()
                )
            }, { effect: SendSheetEffect ->
                when (effect) {
                    is SendSheetEffect.SendTransaction -> effect.run {
                        SendTx.Effect(currencyCode, address, amount, transferFeeBasis)
                    }
                    else -> null
                }
            }),
            Connectable { output ->
                SendSheetEffectHandler(output, activity!!, direct.instance(), router) {
                    val controller = ScannerController()
                    controller.targetController = this
                    router.pushController(
                        RouterTransaction.with(controller)
                            .popChangeHandler(FadeChangeHandler())
                            .pushChangeHandler(FadeChangeHandler())
                    )
                }
            },
            nestedConnectable({ output: Consumer<BreadBoxEvent> ->
                BreadBoxEffectHandler(output, currencyCode, direct.instance())
            }, { effect: SendSheetEffect ->
                when (effect) {
                    SendSheetEffect.LoadBalance ->
                        BreadBoxEffect.LoadWalletBalance(currencyCode)
                    else -> null
                }
            }, { event: BreadBoxEvent ->
                when (event) {
                    is BreadBoxEvent.OnBalanceUpdated ->
                        SendSheetEvent.OnBalanceUpdated(event.balance, event.fiatBalance)
                    else -> null
                } as? SendSheetEvent
            }),
            nestedConnectable({ output: Consumer<MetaDataEvent> ->
                MetaDataEffectHandler(output, direct.instance(), direct.instance())
            }, { effect ->
                when (effect) {
                    is SendSheetEffect.AddTransactionMetaData ->
                        MetaDataEffect.AddTransactionMetaData(
                            effect.transaction,
                            effect.memo,
                            effect.fiatCurrencyCode,
                            effect.fiatPricePerUnit
                        )
                    else -> null
                }
            }, { _: MetaDataEvent -> {} as? SendSheetEvent }),
            nestedConnectable({
                direct.instance<RouterNavigationEffectHandler>()
            }) { effect ->
                when (effect) {
                    SendSheetEffect.GoToEthWallet -> NavigationEffect.GoToWallet(Currency.CODE_AS_ETH)
                    SendSheetEffect.CloseSheet -> NavigationEffect.GoBack
                    is SendSheetEffect.GoToFaq -> NavigationEffect.GoToFaq(
                        BRConstants.FAQ_SEND,
                        effect.currencyCode
                    )
                    else -> null
                }
            }
        )

    override fun onCreateView(view: View) {
        super.onCreateView(view)

        keyboard.setBRButtonBackgroundResId(R.drawable.keyboard_white_button)
        keyboard.setBRKeyboardColor(R.color.white)
        keyboard.setDeleteImage(R.drawable.ic_delete_black)

        showKeyboard(false)

        layoutSignal.layoutTransition = UiUtils.getDefaultTransition()
    }

    override fun onDetach(view: View) {
        super.onDetach(view)
        Utils.hideKeyboard(activity)
    }

    @Suppress("ComplexMethod")
    override fun bindView(output: Consumer<SendSheetEvent>) = output.view {
        layoutSignal.setOnTouchListener(SlideDetector(router, layoutSignal))

        textInputAddress.onTextChanged(SendSheetEvent::OnTargetAddressChanged)
        textInputMemo.onTextChanged(SendSheetEvent::OnMemoChanged)
        textInputAmount.onClick(SendSheetEvent.OnAmountEditClicked)
        textInputAddress.onClick(SendSheetEvent.OnAmountEditDismissed)
        textInputMemo.onClick(SendSheetEvent.OnAmountEditDismissed)

        buttonPaste.onClick(SendSheetEvent.OnPasteClicked)
        buttonCurrencySelect.onClick(SendSheetEvent.OnToggleCurrencyClicked)
        buttonScan.onClick(SendSheetEvent.OnScanClicked)
        buttonSend.onClick(SendSheetEvent.OnSendClicked)

        buttonRegular.onClick(SendSheetEvent.OnTransferSpeedChanged(TransferSpeed.REGULAR))
        buttonEconomy.onClick(SendSheetEvent.OnTransferSpeedChanged(TransferSpeed.ECONOMY))
        buttonPriority.onClick(SendSheetEvent.OnTransferSpeedChanged(TransferSpeed.PRIORITY))

        buttonFaq.onClick(SendSheetEvent.OnFaqClicked)
        layoutBackground.onClick(SendSheetEvent.OnCloseClicked)
        buttonClose.onClick(SendSheetEvent.OnCloseClicked)

        keyboard.setOnInsertListener { key ->
            output.accept(
                when {
                    key.isEmpty() -> OnAmountChange.Delete
                    key[0] == '.' -> OnAmountChange.AddDecimal
                    Character.isDigit(key[0]) -> OnAmountChange.AddDigit(key.toInt())
                    else -> return@setOnInsertListener
                }
            )
        }

        textInputAddress.setOnEditorActionListener { _, actionId, event ->
            if (event?.keyCode == KeyEvent.KEYCODE_ENTER
                || actionId == EditorInfo.IME_ACTION_DONE
                || actionId == EditorInfo.IME_ACTION_NEXT
            ) {
                output.accept(SendSheetEvent.OnAmountEditClicked)
                true
            } else false
        }

        with(View.OnFocusChangeListener { _, hasFocus ->
            if (hasFocus) {
                output.accept(SendSheetEvent.OnAmountEditDismissed)
            } else {
                Utils.hideKeyboard(activity)
            }
        }) {
            apply(textInputMemo::setOnFocusChangeListener)
            apply(textInputAddress::setOnFocusChangeListener)
        }

        onDispose {
            layoutSignal.setOnTouchListener(null)
            keyboard.setOnInsertListener(null)
        }
    }

    @Suppress("ComplexMethod", "LongMethod")
    override fun SendSheetModel.render() {
        val res = checkNotNull(resources)

        ifChanged(SendSheetModel::targetInputError) {
            inputLayoutAddress.isErrorEnabled = targetInputError != null
            inputLayoutAddress.error = when (targetInputError) {
                is SendSheetModel.InputError.Empty ->
                    res.getString(R.string.Send_noAddress)
                is SendSheetModel.InputError.Invalid ->
                    res.getString(R.string.Send_invalidAddressMessage, currencyCode.toUpperCase())
                is SendSheetModel.InputError.ClipboardInvalid ->
                    res.getString(
                        R.string.Send_invalidAddressOnPasteboard,
                        currencyCode.toUpperCase()
                    )
                is SendSheetModel.InputError.ClipboardEmpty ->
                    res.getString(R.string.Send_emptyPasteboard)
                else -> null
            }
        }

        ifChanged(SendSheetModel::amountInputError) {
            inputLayoutAmount.isErrorEnabled = amountInputError != null
            inputLayoutAmount.error = when (amountInputError) {
                is SendSheetModel.InputError.Empty ->
                    res.getString(R.string.Send_noAmount)
                is SendSheetModel.InputError.BalanceTooLow ->
                    res.getString(R.string.Send_insufficientFunds)
                else -> null
            }
        }

        ifChanged(
            SendSheetModel::currencyCode,
            SendSheetModel::fiatCode,
            SendSheetModel::isAmountCrypto
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

        ifChanged(SendSheetModel::isAmountEditVisible, ::showKeyboard)

        ifChanged(SendSheetModel::rawAmount) {
            textInputAmount.setText(
                when {
                    rawAmount.contains('.') -> {
                        val parts = rawAmount.split('.')
                        val main = parts.first().separatePeriods()
                        "$main.${parts.getOrNull(1)}"
                    }
                    else -> rawAmount.separatePeriods()
                }
            )
        }

        ifChanged(
            SendSheetModel::networkFee,
            SendSheetModel::fiatNetworkFee,
            SendSheetModel::isErc20,
            SendSheetModel::isAmountCrypto
        ) {
            // TODO: add nativeCurrencyCode to model, remove eth constant
            val cryptoCurrencyCode = if (isErc20) "ETH" else currencyCode
            labelNetworkFee.isVisible = networkFee != BigDecimal.ZERO
            labelNetworkFee.text = res.getString(
                R.string.Send_fee, when {
                    isAmountCrypto ->
                        networkFee.formatCryptoForUi(
                            cryptoCurrencyCode,
                            scale = 8
                        ) // TODO: scale const
                    else -> fiatNetworkFee.formatFiatForUi(fiatCode)
                }
            )
        }

        ifChanged(
            SendSheetModel::balance,
            SendSheetModel::fiatBalance,
            SendSheetModel::isAmountCrypto
        ) {
            labelBalance.text = res.getString(
                R.string.Send_balance,
                when {
                    isAmountCrypto -> balance.formatCryptoForUi(currencyCode)
                    else -> fiatBalance.formatFiatForUi(fiatCode)
                }
            )
        }

        ifChanged(SendSheetModel::targetAddress) {
            if (textInputAddress.text.toString() != targetAddress) {
                textInputAddress.setText(targetAddress, TextView.BufferType.EDITABLE)
            }
        }

        ifChanged(SendSheetModel::memo) {
            if (textInputMemo.text.toString() != memo) {
                textInputMemo.setText(memo, TextView.BufferType.EDITABLE)
            }
        }

        ifChanged(
            SendSheetModel::showFeeSelect,
            SendSheetModel::transferSpeed
        ) {
            layoutFeeOption.isVisible = showFeeSelect
            setFeeOption(transferSpeed)
        }

        ifChanged(SendSheetModel::transferSpeed, ::setFeeOption)
        ifChanged(SendSheetModel::showFeeSelect) {
            layoutFeeOption.isVisible = showFeeSelect
        }

        ifChanged(SendSheetModel::isConfirmingTx) {
            val isConfirmOnTop = router.backstack.first().controller() is ConfirmTxDetailsController
            if (isConfirmingTx && !isConfirmOnTop) {
                val controller = ConfirmTxDetailsController()
                controller.targetController = this@SendSheetController
                router.pushController(RouterTransaction.with(controller))
            } else if (!isConfirmingTx && isConfirmOnTop) {
                router.popCurrentController()
            }
        }

        ifChanged(SendSheetModel::isAuthenticating) {
            val isAuthOnTop = router.backstack.first().controller() is AuthenticationController
            if (isAuthenticating && !isAuthOnTop) {
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
            } else if (!isAuthenticating && isAuthOnTop) {
                router.popCurrentController()
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
            SendSheetEvent.OnRequestScanned(
                currencyCode = link.currencyCode,
                amount = link.amount,
                targetAddress = link.address
            ).run(eventConsumer::accept)
        }
    }

    override fun onAuthenticationSuccess() {
        eventConsumer.accept(SendSheetEvent.OnAuthSuccess)
    }

    override fun onAuthenticationCancelled() {
        eventConsumer.accept(SendSheetEvent.OnAuthCancelled)
    }

    override fun onPositiveClicked(dialogId: String, controller: AlertDialogController) {
        when (dialogId) {
            DIALOG_NO_ETH_FOR_TOKEN_TRANSFER -> {
                eventConsumer.accept(SendSheetEvent.GoToEthWallet)
            }
        }
    }

    private fun setFeeOption(feeOption: TransferSpeed) {
        val context = applicationContext!!
        // TODO: Redo using a toggle button and a selector
        when (feeOption) {
            TransferSpeed.REGULAR -> {
                buttonRegular.setTextColor(context.getColor(R.color.white))
                buttonRegular.background = context.getDrawable(R.drawable.b_blue_square)
                buttonEconomy.setTextColor(context.getColor(R.color.dark_blue))
                buttonEconomy.background = context.getDrawable(R.drawable.b_half_left_blue_stroke)
                buttonPriority.setTextColor(context.getColor(R.color.dark_blue))
                buttonPriority.background =
                    context.getDrawable(R.drawable.b_half_right_blue_stroke)
                labelFeeDescription.text = context.getString(R.string.FeeSelector_estimatedDeliver)
                    .format(context.getString(R.string.FeeSelector_regularTime))
                labelFeeWarning.visibility = View.GONE
            }
            TransferSpeed.ECONOMY -> {
                buttonRegular.setTextColor(context.getColor(R.color.dark_blue))
                buttonRegular.background = context.getDrawable(R.drawable.b_blue_square_stroke)
                buttonEconomy.setTextColor(context.getColor(R.color.white))
                buttonEconomy.background = context.getDrawable(R.drawable.b_half_left_blue)
                buttonPriority.setTextColor(context.getColor(R.color.dark_blue))
                buttonPriority.background =
                    context.getDrawable(R.drawable.b_half_right_blue_stroke)
                labelFeeDescription.text = context.getString(R.string.FeeSelector_estimatedDeliver)
                    .format(context.getString(R.string.FeeSelector_economyTime))
                labelFeeWarning.visibility = View.VISIBLE
            }
            TransferSpeed.PRIORITY -> {
                buttonRegular.setTextColor(context.getColor(R.color.dark_blue))
                buttonRegular.background = context.getDrawable(R.drawable.b_blue_square_stroke)
                buttonEconomy.setTextColor(context.getColor(R.color.dark_blue))
                buttonEconomy.background = context.getDrawable(R.drawable.b_half_left_blue_stroke)
                buttonPriority.setTextColor(context.getColor(R.color.white))
                buttonPriority.background = context.getDrawable(R.drawable.b_half_right_blue)
                labelFeeDescription.text =
                    context.getString(R.string.FeeSelector_estimatedDeliver)
                        .format(context.getString(R.string.FeeSelector_priorityTime))
                labelFeeWarning.visibility = View.GONE
            }
        }
    }
}

private const val PERIOD_LENGTH = 3
private const val PERIOD_SEPARATOR = ","

/**
 * Given a number with at least two periods, returns the
 * same number with each period separated by a comma.
 */
fun String.separatePeriods(): String {
    return reversed()
        .chunked(PERIOD_LENGTH)
        .joinToString(PERIOD_SEPARATOR)
        .reversed()
}
