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

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.support.constraint.ConstraintSet
import android.view.KeyEvent
import android.view.View
import android.view.inputmethod.EditorInfo
import android.widget.TextView
import androidx.core.os.bundleOf
import androidx.core.view.isInvisible
import androidx.core.view.isVisible
import com.bluelinelabs.conductor.RouterTransaction

import com.breadwallet.R
import com.breadwallet.breadbox.BreadBoxEffect
import com.breadwallet.breadbox.BreadBoxEffectHandler
import com.breadwallet.breadbox.BreadBoxEvent
import com.breadwallet.breadbox.SendTx
import com.breadwallet.breadbox.SendTxEffectHandler
import com.breadwallet.breadbox.formatCryptoForUi
import com.breadwallet.breadbox.formatFiatForUi
import com.breadwallet.crypto.Currency
import com.breadwallet.legacy.presenter.entities.CryptoRequest
import com.breadwallet.mobius.CompositeEffectHandler
import com.breadwallet.mobius.nestedConnectable
import com.breadwallet.tools.animation.UiUtils
import com.breadwallet.tools.animation.SlideDetector
import com.breadwallet.tools.manager.BRSharedPrefs
import com.breadwallet.tools.util.BRConstants
import com.breadwallet.tools.util.CurrencyUtils
import com.breadwallet.tools.util.Utils
import com.breadwallet.ui.BaseMobiusController
import com.breadwallet.ui.MainActivity
import com.breadwallet.ui.auth.AuthenticationController
import com.breadwallet.ui.changehandlers.BottomSheetChangeHandler
import com.breadwallet.ui.controllers.AlertDialogController
import com.breadwallet.ui.navigation.NavigationEffect
import com.breadwallet.ui.navigation.NavigationEffectHandler
import com.breadwallet.ui.navigation.RouterNavigationEffectHandler
import com.breadwallet.ui.send.SendSheetEvent.OnAmountChange
import com.breadwallet.ui.send.SendSheetModel.TransferSpeed.PRIORITY
import com.breadwallet.ui.send.SendSheetModel.TransferSpeed.REGULAR
import com.breadwallet.ui.send.SendSheetModel.TransferSpeed.ECONOMY
import com.breadwallet.ui.view
import com.spotify.mobius.Connectable
import com.spotify.mobius.functions.Consumer
import kotlinx.android.synthetic.main.controller_send_sheet.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import org.kodein.di.direct
import org.kodein.di.erased.instance

/** A BottomSheet for sending crypto from the user's wallet to a specified target. */
@Suppress("TooManyFunctions")
class SendSheetController(args: Bundle? = null) :
    BaseMobiusController<SendSheetModel, SendSheetEvent, SendSheetEffect>(args),
    AuthenticationController.Listener,
    AlertDialogController.Listener {

    companion object {
        private const val CURRENCY_CODE = "CURRENCY_CODE"
        private const val CRYPTO_REQUEST = "CRYPTO_REQUEST"

        const val QR_SCAN_RC = 350

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

    init {
        overridePushHandler(BottomSheetChangeHandler())
        overridePopHandler(BottomSheetChangeHandler())

        registerForActivityResult(QR_SCAN_RC)
    }

    private val currencyCode = arg<String>(CURRENCY_CODE)
    private val cryptoRequest = argOptional<CryptoRequest>(CRYPTO_REQUEST)

    private var mKeyboardIndex: Int = 0
    private var mIsAmountLabelShown = true

    override val layoutId = R.layout.controller_send_sheet
    override val init = SendSheetInit
    override val update = SendSheetUpdate
    override val defaultModel: SendSheetModel
        get() {
            val fiatCode = BRSharedPrefs.getPreferredFiatIso()
            return cryptoRequest?.asSendSheetModel(fiatCode) ?: SendSheetModel.createDefault(currencyCode, fiatCode)
        }

    private val sendCoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val retainedSendConsumer = Consumer<SendTx.Result> { event ->
        eventConsumer.accept(
            when (event) {
                is SendTx.Result.Success -> SendSheetEvent.OnSendComplete
                is SendTx.Result.Error -> SendSheetEvent.OnSendFailed
            }
        )
    }

    override val effectHandler: Connectable<SendSheetEffect, SendSheetEvent> =
        CompositeEffectHandler.from(
            nestedConnectable({
                SendTxEffectHandler(
                    { retainedSendConsumer },
                    sendCoroutineScope,
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
            Connectable { SendSheetEffectHandler(it, activity!!, direct.instance(), router) },
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
            nestedConnectable({
                direct.instance<NavigationEffectHandler>()
            }) { effect: SendSheetEffect ->
                when (effect) {
                    SendSheetEffect.GoToScan -> NavigationEffect.GoToQrScan
                    else -> null
                }
            },
            nestedConnectable({
                direct.instance<RouterNavigationEffectHandler>()
            }) { effect ->
                when (effect) {
                    SendSheetEffect.GoToEthWallet -> NavigationEffect.GoToWallet(Currency.CODE_AS_ETH)
                    SendSheetEffect.CloseSheet -> NavigationEffect.GoBack
                    is SendSheetEffect.GoToFaq -> NavigationEffect.GoToFaq(BRConstants.FAQ_SEND, effect.currencyCode)
                    else -> null
                }
            }
        )

    override fun onCreateView(view: View) {
        super.onCreateView(view)

        keyboard.setBRButtonBackgroundResId(R.drawable.keyboard_white_button)
        keyboard.setBRKeyboardColor(R.color.white)
        keyboard.setDeleteImage(R.drawable.ic_delete_black)

        mKeyboardIndex = signal_layout.indexOfChild(keyboard_layout)

        showKeyboard(false)

        signal_layout.layoutTransition = UiUtils.getDefaultTransition()
    }

    override fun onDestroy() {
        super.onDestroy()
        sendCoroutineScope.cancel()
    }

    override fun onDetach(view: View) {
        super.onDetach(view)
        Utils.hideKeyboard(activity)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == QR_SCAN_RC && resultCode == Activity.RESULT_OK) {// TODO const, handle failure?
            val cryptoRequest = data?.extras?.get(MainActivity.EXTRA_CRYPTO_REQUEST) as? CryptoRequest ?: return
            if (cryptoRequest.address.isNullOrBlank()) return
            eventConsumer.accept(SendSheetEvent.OnAddressPasted.ValidAddress(cryptoRequest.address))
        }
    }

    @Suppress("ComplexMethod")
    override fun bindView(output: Consumer<SendSheetEvent>) = output.view {
        signal_layout.setOnTouchListener(SlideDetector(router, signal_layout))

        amount_edit.onClick(SendSheetEvent.OnAmountEditClicked)
        address_edit.onClick(SendSheetEvent.OnAmountEditDismissed)
        comment_edit.onClick(SendSheetEvent.OnAmountEditDismissed)

        paste_button.onClick(SendSheetEvent.OnPasteClicked)
        iso_button.onClick(SendSheetEvent.OnToggleCurrencyClicked)
        scan.onClick(SendSheetEvent.OnScanClicked)
        send_button.onClick(SendSheetEvent.OnSendClicked)

        regular_button.onClick(SendSheetEvent.OnTransferSpeedChanged(REGULAR))
        economy_button.onClick(SendSheetEvent.OnTransferSpeedChanged(ECONOMY))
        priority_button.onClick(SendSheetEvent.OnTransferSpeedChanged(PRIORITY))

        faq_button.onClick(SendSheetEvent.OnFaqClicked)
        background_layout.onClick(SendSheetEvent.OnCloseClicked)
        close_button.onClick(SendSheetEvent.OnCloseClicked)

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

        address_edit.setOnEditorActionListener { _, actionId, event ->
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
            apply(comment_edit::setOnFocusChangeListener)
            apply(address_edit::setOnFocusChangeListener)
        }

        onDispose {
            signal_layout.setOnTouchListener(null)
            keyboard.setOnInsertListener(null)
        }
    }

    @Suppress("ComplexMethod", "LongMethod")
    override fun SendSheetModel.render() {
        val res = checkNotNull(resources)

        ifChanged(
            SendSheetModel::currencyCode,
            SendSheetModel::fiatCode,
            SendSheetModel::isAmountCrypto,
            SendSheetModel::isAmountEditVisible
        ) {
            val active = when {
                isAmountCrypto -> currencyCode
                else -> fiatCode
            }.toUpperCase()

            iso_button.text = active
            iso_text.text = when {
                !isAmountEditVisible &&
                    rawAmount.isBlank() &&
                    balance_text.isInvisible ->
                    res.getString(R.string.Send_amountLabel)
                else -> when {
                    isAmountCrypto -> active
                    else -> CurrencyUtils.getSymbolByIso(applicationContext, active)
                }
            }
        }

        @Suppress("MagicNumber")
        ifChanged(SendSheetModel::isAmountEditVisible) {
            fee_text.isVisible = isAmountEditVisible
            if (mIsAmountLabelShown) {
                // TODO: Don't use a constraint layout for this.
                ConstraintSet().apply {
                    clone(amount_layout)
                    val px4 = Utils.getPixelsFromDps(applicationContext, 4)
                    connect(balance_text.id, ConstraintSet.TOP, iso_text.id, ConstraintSet.BOTTOM, px4)
                    connect(fee_text.id, ConstraintSet.TOP, balance_text.id, ConstraintSet.BOTTOM, px4)
                    connect(fee_text.id, ConstraintSet.BOTTOM, ConstraintSet.PARENT_ID, ConstraintSet.BOTTOM, px4)
                    connect(iso_text.id, ConstraintSet.TOP, ConstraintSet.PARENT_ID, ConstraintSet.TOP, px4)
                    connect(iso_text.id, ConstraintSet.BOTTOM, -1, ConstraintSet.TOP, -1)
                    applyTo(amount_layout)
                }
            }
        }

        ifChanged(SendSheetModel::isAmountEditVisible) {
            showKeyboard(isAmountEditVisible)
            if (isAmountEditVisible) {
                startEditingAmount()
            }
        }

        ifChanged(SendSheetModel::rawAmount) {
            amount_edit.setText(
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
            fee_text.text = res.getString(
                R.string.Send_fee, when {
                    isAmountCrypto -> networkFee.formatCryptoForUi(cryptoCurrencyCode, scale = 8) // TODO: scale const
                    else -> fiatNetworkFee.formatFiatForUi(fiatCode)
                }
            )
        }

        ifChanged(
            SendSheetModel::balance,
            SendSheetModel::fiatBalance,
            SendSheetModel::isAmountCrypto
        ) {
            balance_text.text = res.getString(
                R.string.Send_balance,
                when {
                    isAmountCrypto -> balance.formatCryptoForUi(currencyCode)
                    else -> fiatBalance.formatFiatForUi(fiatCode)
                }
            )
        }

        ifChanged(SendSheetModel::toAddress) {
            if (address_edit.text.toString() != toAddress) {
                address_edit.setText(toAddress, TextView.BufferType.EDITABLE)
            }
        }

        ifChanged(SendSheetModel::memo) {
            if (address_edit.text.toString() != memo) {
                address_edit.setText(memo, TextView.BufferType.EDITABLE)
            }
        }

        ifChanged(
            SendSheetModel::isAmountOverBalance,
            SendSheetModel::isAmountEditVisible
        ) {
            val activity = checkNotNull(activity)
            if (isAmountOverBalance) {
                balance_text.setTextColor(activity.getColor(R.color.warning_color))
                fee_text.setTextColor(activity.getColor(R.color.warning_color))
                amount_edit.setTextColor(activity.getColor(R.color.warning_color))
                if (isAmountEditVisible)
                    iso_text.setTextColor(activity.getColor(R.color.warning_color))
            } else {
                balance_text.setTextColor(activity.getColor(R.color.light_gray))
                fee_text.setTextColor(activity.getColor(R.color.light_gray))
                amount_edit.setTextColor(activity.getColor(R.color.almost_black))
                if (isAmountEditVisible)
                    iso_text.setTextColor(activity.getColor(R.color.almost_black))
            }
        }

        ifChanged(
            SendSheetModel::showFeeSelect,
            SendSheetModel::transferSpeed
        ) {
            fee_buttons_layout.isVisible = showFeeSelect
            setFeeOption(transferSpeed)
        }

        ifChanged(SendSheetModel::transferSpeed, ::setFeeOption)
        ifChanged(SendSheetModel::showFeeSelect) {
            fee_buttons_layout.isVisible = showFeeSelect
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
                val controller = AuthenticationController(
                    title = res.getString(R.string.VerifyPin_title),
                    message = res.getString(R.string.VerifyPin_authorize)
                )
                controller.targetController = this@SendSheetController
                router.pushController(RouterTransaction.with(controller))
            } else if (!isAuthenticating && isAuthOnTop) {
                router.popCurrentController()
            }
        }
    }

    @Suppress("MagicNumber")
    private fun startEditingAmount() {
        if (mIsAmountLabelShown) { //only first time
            mIsAmountLabelShown = false
            amount_edit.hint = "0"
            amount_edit.textSize = resources!!.getDimension(R.dimen.amount_text_size)
            amount_edit.visibility = View.VISIBLE
            balance_text.visibility = View.VISIBLE
            iso_text.setTextColor(activity!!.getColor(R.color.almost_black))
            iso_text.textSize = resources!!.getDimension(R.dimen.currency_code_text_size_large)

            val set = ConstraintSet()
            set.clone(amount_layout)

            // Re-space elements due to new UI elements being visible
            val px4 = Utils.getPixelsFromDps(applicationContext, 4)
            set.connect(balance_text.id, ConstraintSet.TOP, iso_text.id, ConstraintSet.BOTTOM, px4)
            set.connect(fee_text.id, ConstraintSet.TOP, balance_text.id, ConstraintSet.BOTTOM, px4)
            set.connect(fee_text.id, ConstraintSet.BOTTOM, ConstraintSet.PARENT_ID, ConstraintSet.BOTTOM, px4)
            set.connect(iso_text.id, ConstraintSet.TOP, ConstraintSet.PARENT_ID, ConstraintSet.TOP, px4)
            set.connect(iso_text.id, ConstraintSet.BOTTOM, -1, ConstraintSet.TOP, -1)
            set.applyTo(amount_layout)
        }
    }

    private fun showKeyboard(b: Boolean) {
        if (!b) {
            signal_layout.removeView(keyboard_layout)
        } else {
            Utils.hideKeyboard(activity)
            if (signal_layout.indexOfChild(keyboard_layout) == -1) {
                signal_layout.addView(keyboard_layout, mKeyboardIndex)
            } else {
                signal_layout.removeView(keyboard_layout)
            }
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

    private fun setFeeOption(feeOption: SendSheetModel.TransferSpeed) {
        val context = applicationContext!!
        // TODO: Redo using a toggle button and a selector
        when (feeOption) {
            REGULAR -> {
                regular_button.setTextColor(context.getColor(R.color.white))
                regular_button.background = context.getDrawable(R.drawable.b_blue_square)
                economy_button.setTextColor(context.getColor(R.color.dark_blue))
                economy_button.background = context.getDrawable(R.drawable.b_half_left_blue_stroke)
                priority_button.setTextColor(context.getColor(R.color.dark_blue))
                priority_button.background = context.getDrawable(R.drawable.b_half_right_blue_stroke)
                fee_description.text = context.getString(R.string.FeeSelector_estimatedDeliver)
                    .format(context.getString(R.string.FeeSelector_regularTime))
                warning_text.visibility = View.GONE
            }
            ECONOMY -> {
                regular_button.setTextColor(context.getColor(R.color.dark_blue))
                regular_button.background = context.getDrawable(R.drawable.b_blue_square_stroke)
                economy_button.setTextColor(context.getColor(R.color.white))
                economy_button.background = context.getDrawable(R.drawable.b_half_left_blue)
                priority_button.setTextColor(context.getColor(R.color.dark_blue))
                priority_button.background = context.getDrawable(R.drawable.b_half_right_blue_stroke)
                fee_description.text = context.getString(R.string.FeeSelector_estimatedDeliver)
                    .format(context.getString(R.string.FeeSelector_economyTime))
                warning_text.visibility = View.VISIBLE
            }
            PRIORITY -> {
                regular_button.setTextColor(context.getColor(R.color.dark_blue))
                regular_button.background = context.getDrawable(R.drawable.b_blue_square_stroke)
                economy_button.setTextColor(context.getColor(R.color.dark_blue))
                economy_button.background = context.getDrawable(R.drawable.b_half_left_blue_stroke)
                priority_button.setTextColor(context.getColor(R.color.white))
                priority_button.background = context.getDrawable(R.drawable.b_half_right_blue)
                fee_description.text =
                    context.getString(R.string.FeeSelector_estimatedDeliver)
                        .format(context.getString(R.string.FeeSelector_priorityTime))
                warning_text.visibility = View.GONE
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
