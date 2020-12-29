/**
 * BreadWallet
 *
 * Created by Ahsan Butt <ahsan.butt@breadwallet.com> on 12/09/20.
 * Copyright (c) 2020 breadwallet LLC
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
package com.breadwallet.ui.uigift

import android.os.Bundle
import android.view.View
import androidx.core.os.bundleOf
import androidx.core.view.isGone
import com.bluelinelabs.conductor.RouterTransaction
import com.breadwallet.breadbox.formatCryptoForUi
import com.breadwallet.ui.BaseController
import com.breadwallet.ui.BaseMobiusController
import com.breadwallet.ui.ViewEffect
import com.breadwallet.ui.changehandlers.BottomSheetChangeHandler
import com.breadwallet.ui.changehandlers.DialogChangeHandler
import com.breadwallet.ui.controllers.AlertDialogController
import com.breadwallet.ui.flowbind.clicks
import com.breadwallet.ui.flowbind.textChanges
import com.breadwallet.ui.formatFiatForUi
import com.breadwallet.ui.uigift.databinding.ControllerCreateGiftBinding
import com.breadwallet.ui.uigift.CreateGift.M
import com.breadwallet.ui.uigift.CreateGift.E
import com.breadwallet.ui.uigift.CreateGift.F
import com.breadwallet.ui.uigift.CreateGift.State
import com.breadwallet.ui.uigift.CreateGift.FiatAmountOption
import com.breadwallet.ui.uigift.databinding.ControllerConfirmGiftDetailsBinding
import com.breadwallet.util.CurrencyCode
import com.google.android.material.button.MaterialButton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.merge
import org.kodein.di.direct
import org.kodein.di.erased.instance
import java.math.BigDecimal

private const val CURRENCY_ID = "currency_id"
private const val FIAT_AMOUNT_OPTIONS = 5

interface ConfirmationListener {
    fun onConfirmed()
    fun onCancelled()
}

class CreateGiftController(
    args: Bundle
) : BaseMobiusController<M, E, F>(args), ConfirmationListener, AlertDialogController.Listener {

    constructor(
        currencyId: String
    ) : this(bundleOf(CURRENCY_ID to currencyId))

    init {
        overridePopHandler(BottomSheetChangeHandler())
        overridePushHandler(BottomSheetChangeHandler())
    }

    override val defaultModel: M = M.createDefault(arg(CURRENCY_ID))
    override val init = CreateGiftInit
    override val update = CreateGiftUpdate
    override val flowEffectHandler
        get() = createCreateGiftingHandler(
            currencyId = arg(CURRENCY_ID),
            breadBox = direct.instance(),
            userManager = direct.instance(),
            rates = direct.instance(),
            giftBackup = direct.instance(),
            metaDataManager = direct.instance()
        )

    private val binding by viewBinding(ControllerCreateGiftBinding::inflate)
    private val amountChoices: List<MaterialButton>
        get() = with(binding) {
            listOf(
                amountChoice1,
                amountChoice2,
                amountChoice3,
                amountChoice4,
                amountChoice5,
            )
        }

    override fun bindView(modelFlow: Flow<M>): Flow<E> {
        with(binding) {
            return merge(
                buttonClose.clicks().map { E.OnCloseClicked },
                amountChoices.mapIndexed { index, input ->
                    input.clicks().map { E.OnAmountClicked(index, true) }
                }.merge(),
                recipientNameInput.textChanges().map { E.OnNameChanged(it) },
                createBtn.clicks().map { E.OnCreateClicked }
            )
        }
    }

    override fun M.render() {
        with(binding) {
            check(fiatAmountOptions.size == FIAT_AMOUNT_OPTIONS) {
                "Only $FIAT_AMOUNT_OPTIONS gift amounts are supported."
            }

            ifChanged(M::state, M::amountSelectionIndex) {
                toggleInputs(state, amountSelectionIndex, fiatAmountOptions)

                loadingView.root.isGone = state != State.LOADING && state != State.SENDING
            }

            ifChanged(M::fiatAmountOptions, M::amountSelectionIndex) {
                amountChoices.initialize(state, amountSelectionIndex, fiatAmountOptions)
            }
        }
    }

    override fun handleBack(): Boolean {
        eventConsumer.accept(E.OnCloseClicked)
        return true
    }

    override fun handleViewEffect(effect: ViewEffect) {
        when (effect) {
            is F.Close -> router.popCurrentController()
            is F.ConfirmTransaction -> {
                val controller = ConfirmationController(
                    effect.name,
                    effect.amount,
                    effect.currencyCode,
                    effect.fiatAmount,
                    effect.fiatFee,
                    effect.fiatCurrencyCode
                )
                router.pushController(RouterTransaction.with(controller))
            }
        }
    }

    private fun toggleInputs(state: State, selection: Int, fiatAmountOptions: List<FiatAmountOption>) {
        with(binding) {
            val enable = state == State.READY
            amountChoices.initialize(state, selection, fiatAmountOptions)
            recipientNameInput.isEnabled = enable
            createBtn.isEnabled = enable
        }
    }

    fun List<MaterialButton>.initialize(state: State, selection: Int, fiatAmountOptions: List<FiatAmountOption>) {
        forEachIndexed { index, button ->
            val opt = fiatAmountOptions[index]
            button.isEnabled = (state == State.READY) && opt.enabled
            button.text = opt.amount.formatFiatForUi(opt.fiatCurrencyCode, 0)
            button.isChecked = selection == index
        }
    }

    override fun onPositiveClicked(
        dialogId: String,
        controller: AlertDialogController,
        result: AlertDialogController.DialogInputResult
    ) {
        if (CreateGift.Error.valueOf(dialogId).isCritical) {
            router.popController(this)
        }
    }

    override fun onConfirmed() {
        eventConsumer.accept(E.OnTransactionConfirmClicked)
    }

    override fun onCancelled() {
        eventConsumer.accept(E.OnTransactionCancelClicked)
    }

    class ConfirmationController(args: Bundle) : BaseController(args) {

        constructor(
            name: String,
            amount: BigDecimal,
            currencyCode: CurrencyCode,
            fiatAmount: BigDecimal,
            fiatFee: BigDecimal,
            fiatCurrencyCode: String
        ) : this(
            bundleOf(
                "name" to name,
                "amount" to amount,
                "currencyCode" to currencyCode,
                "fiatAmount" to fiatAmount,
                "fiatFee" to fiatFee,
                "fiatCurrencyCode" to fiatCurrencyCode
            )
        )

        private val name: String = arg("name")
        private val amount: BigDecimal = arg("amount")
        private val currencyCode: String = arg("currencyCode")
        private val fiatAmount: BigDecimal = arg("fiatAmount")
        private val fiatFee: BigDecimal = arg("fiatFee")
        private val fiatCurrencyCode: String = arg("fiatCurrencyCode")

        init {
            overridePopHandler(DialogChangeHandler())
            overridePushHandler(DialogChangeHandler())
        }

        private val binding by viewBinding(ControllerConfirmGiftDetailsBinding::inflate)

        override fun onAttach(view: View) {
            super.onAttach(view)

            with(binding) {
                val fiatAmountString = fiatAmount.formatFiatForUi(fiatCurrencyCode)
                sendValue.text = "%s (%s)".format(amount.formatCryptoForUi(currencyCode), fiatAmountString)
                toAddress.text = name
                networkFeeValue.text = fiatFee.formatFiatForUi(fiatCurrencyCode)

                val listener = findListener<ConfirmationListener>()
                val cancel = View.OnClickListener {
                    listener?.onCancelled()
                    router.popController(this@ConfirmationController)
                }
                okBtn.setOnClickListener {
                    listener?.onConfirmed()
                    router.popController(this@ConfirmationController)
                }
                closeBtn.setOnClickListener(cancel)
                cancelBtn.setOnClickListener(cancel)
            }
        }

        override fun handleBack(): Boolean {
            findListener<ConfirmationListener>()?.onCancelled()
            return super.handleBack()
        }
    }
}
