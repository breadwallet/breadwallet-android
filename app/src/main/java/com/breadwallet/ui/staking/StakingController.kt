/**
 * BreadWallet
 *
 * Created by Drew Carlson <drew.carlson@breadwallet.com> on 10/30/20.
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
package com.breadwallet.ui.uistaking

import android.content.res.ColorStateList
import android.content.res.Resources
import android.os.Bundle
import android.view.View
import androidx.core.os.bundleOf
import androidx.core.view.isVisible
import androidx.core.view.isInvisible
import com.bluelinelabs.conductor.RouterTransaction
import com.breadwallet.R
import com.breadwallet.breadbox.formatCryptoForUi
import com.breadwallet.databinding.ControllerConfirmTxDetailsBinding
import com.breadwallet.databinding.ControllerStakingBinding
import com.breadwallet.tools.animation.SlideDetector
import com.breadwallet.ui.BaseController
import com.breadwallet.ui.BaseMobiusController
import com.breadwallet.ui.ViewEffect
import com.breadwallet.ui.auth.AuthMode
import com.breadwallet.ui.auth.AuthenticationController
import com.breadwallet.ui.changehandlers.BottomSheetChangeHandler
import com.breadwallet.ui.changehandlers.DialogChangeHandler
import com.breadwallet.ui.flowbind.clicks
import com.breadwallet.ui.flowbind.textChanges
import com.breadwallet.ui.uistaking.Staking.E
import com.breadwallet.ui.uistaking.Staking.F
import com.breadwallet.ui.uistaking.Staking.M
import com.breadwallet.ui.uistaking.Staking.M.ViewValidator.State.PENDING_STAKE
import com.breadwallet.ui.uistaking.Staking.M.ViewValidator.State.PENDING_UNSTAKE
import com.breadwallet.ui.uistaking.Staking.M.ViewValidator.State.STAKED
import com.breadwallet.ui.web.WebController
import com.platform.HTTPServer
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.merge
import org.kodein.di.direct
import org.kodein.di.erased.instance
import java.math.BigDecimal
import java.util.Locale

interface ConfirmationListener {
    fun onConfirmed()
    fun onCancelled()
}

private const val CURRENCY_ID = "currency_id"

class StakingController(
    bundle: Bundle? = null
) : BaseMobiusController<M, E, F>(bundle),
    ConfirmationListener,
    AuthenticationController.Listener {

    constructor(
        currencyId: String
    ) : this(bundleOf(CURRENCY_ID to currencyId))

    init {
        overridePopHandler(BottomSheetChangeHandler())
        overridePushHandler(BottomSheetChangeHandler())
    }

    override val defaultModel: M = M.Loading()
    override val init = StakingInit
    override val update = StakingUpdate
    override val flowEffectHandler
        get() = createStakingHandler(
            context = direct.instance(),
            currencyId = arg(CURRENCY_ID),
            breadBox = direct.instance(),
            userManager = direct.instance()
        )

    private val binding by viewBinding(ControllerStakingBinding::inflate)

    override fun onCreateView(view: View) {
        super.onCreateView(view)
        binding.layoutHeader.setOnTouchListener(SlideDetector(router, view))
    }

    override fun bindView(modelFlow: Flow<M>): Flow<E> {
        return with(binding) {
            merge(
                buttonStake.clicks().map { E.OnStakeClicked },
                buttonPaste.clicks().map { E.OnPasteClicked },
                buttonClose.clicks().map { E.OnCloseClicked },
                buttonCancel.clicks().map { E.OnCancelClicked },
                buttonUnstake.clicks().map { E.OnUnstakeClicked },
                buttonConfirm.clicks().map { E.OnStakeClicked },
                buttonChangeValidator.clicks().map { E.OnChangeClicked },
                inputAddress.textChanges().map { E.OnAddressChanged(it) },
                buttonFaq.clicks().map { E.OnHelpClicked }
            )
        }
    }

    override fun handleViewEffect(effect: ViewEffect) {
        when (effect) {
            is F.ConfirmTransaction -> {
                val controller = ConfirmationController(
                    effect.currencyCode,
                    effect.address,
                    effect.balance,
                    effect.fee
                )
                router.pushController(RouterTransaction.with(controller))
            }
            is F.Help -> {
                val supportBaseUrl = HTTPServer.getPlatformUrl(HTTPServer.URL_SUPPORT)
                val url = "$supportBaseUrl/article?slug=staking"
                router.pushController(RouterTransaction.with(WebController(url)))
            }
            is F.Close -> router.popCurrentController()
        }
    }

    override fun M.render() {
        val res = checkNotNull(resources)
        with(binding) {
            ifChanged(M::currencyCode) {
                labelTitle.text = res.getString(
                    R.string.Staking_title,
                    currencyCode.toUpperCase(Locale.ROOT)
                )
            }

            when (this@render) {
                is M.Loading -> {
                    loadingView.isVisible = true
                    buttonStake.isVisible = false
                    labelStatus.isVisible = false
                    layoutChange.isVisible = false
                    buttonPaste.isVisible = false
                    layoutConfirmChange.isVisible = false
                    inputLayoutAddress.isVisible = false
                }
                is M.SetValidator -> renderSetBaker(res)
                is M.ViewValidator -> renderViewBaker(res)
            }
        }
    }

    private fun M.SetValidator.renderSetBaker(res: Resources) {
        val theme = checkNotNull(activity).theme
        with(binding) {
            loadingView.isVisible = false
            labelAddress.isVisible = false
            inputAddress.isEnabled = true
            inputLayoutAddress.isVisible = true

            ifChanged(M.SetValidator::isCancellable) {
                layoutChange.isVisible = false
                buttonStake.isVisible = !isCancellable
                layoutConfirmChange.isVisible = isCancellable
            }

            ifChanged(M.SetValidator::address) {
                buttonPaste.isVisible = address.isBlank()
                if (address.isNullOrBlank() || (address.isNotBlank() && inputAddress.text.isNullOrBlank())) {
                    inputAddress.setText(address)
                }
            }

            ifChanged(M.SetValidator::isAddressValid, M.SetValidator::canSubmitTransfer) {
                buttonConfirm.isEnabled = canSubmitTransfer
                inputLayoutAddress.apply {
                    if (isAddressValid) {
                        error = null
                        helperText = if (canSubmitTransfer) {
                            "Valid baker address!"
                        } else null
                        setHelperTextColor(
                            ColorStateList.valueOf(
                                res.getColor(com.breadwallet.R.color.green_text, theme)
                            )
                        )
                    } else {
                        error = "Invalid baker address"
                        helperText = null
                        setHelperTextColor(null)
                    }
                }
            }

            ifChanged(M.SetValidator::transactionError) {
                labelStatus.isInvisible = transactionError == null
                when (transactionError) {
                    is M.TransactionError.Unknown,
                    M.TransactionError.TransferFailed -> {
                        labelStatus.setText(com.breadwallet.R.string.Transaction_failed)
                    }
                    M.TransactionError.FeeEstimateFailed -> {
                        labelStatus.setText(com.breadwallet.R.string.Send_noFeesError)
                    }
                    null -> {
                        labelStatus.text = null
                    }
                }
            }

            ifChanged(M.SetValidator::isAuthenticating) {
                handleIsAuthenticating(it, isFingerprintEnabled, res)
            }
        }
    }

    private fun M.ViewValidator.renderViewBaker(res: Resources) {
        val theme = checkNotNull(activity).theme
        with(binding) {

            ifChanged(M::address) {
                labelAddress.isVisible = true
                labelAddress.text = address
            }

            ifChanged(M.ViewValidator::state) {
                labelStatus.isVisible = true
                layoutChange.isVisible = true
                loadingView.isVisible = false
                buttonPaste.isVisible = false
                buttonStake.isVisible = false
                layoutConfirmChange.isVisible = false
                inputLayoutAddress.isVisible = false

                when (state) {
                    PENDING_STAKE, PENDING_UNSTAKE -> {
                        buttonUnstake.isEnabled = false
                        buttonChangeValidator.isEnabled = false
                        labelStatus.setText(com.breadwallet.R.string.Transaction_pending)
                        labelStatus.setTextColor(res.getColor(com.breadwallet.R.color.ui_accent, theme))
                    }
                    STAKED -> {
                        buttonUnstake.isEnabled = true
                        buttonChangeValidator.isEnabled = true
                        labelStatus.setText(com.breadwallet.R.string.Staking_statusStaked)
                        labelStatus.setTextColor(res.getColor(com.breadwallet.R.color.green_text, theme))
                    }
                }
            }

            ifChanged(M.ViewValidator::isAuthenticating) {
                handleIsAuthenticating(it, isFingerprintEnabled, res)
            }
        }
    }

    private fun handleIsAuthenticating(
        isAuthenticating: Boolean,
        isFingerprintAuthEnable: Boolean,
        res: Resources
    ) {
        val isAuthVisible =
            router.backstack.lastOrNull()?.controller is AuthenticationController
        if (isAuthenticating && !isAuthVisible) {
            val authenticationMode = if (isFingerprintAuthEnable) {
                AuthMode.USER_PREFERRED
            } else {
                AuthMode.PIN_REQUIRED
            }
            val controller = AuthenticationController(
                mode = authenticationMode,
                title = res.getString(R.string.VerifyPin_touchIdMessage),
                message = res.getString(R.string.VerifyPin_authorize)
            )
            controller.targetController = this@StakingController
            router.pushController(RouterTransaction.with(controller))
        }
    }

    override fun onConfirmed() {
        eventConsumer.accept(E.OnTransactionConfirmClicked)
    }

    override fun onCancelled() {
        eventConsumer.accept(E.OnTransactionCancelClicked)
    }

    override fun onAuthenticationSuccess() {
        eventConsumer.accept(E.OnAuthSuccess)
    }

    override fun onAuthenticationCancelled() {
        eventConsumer.accept(E.OnAuthCancelled)
    }

    class ConfirmationController(args: Bundle) : BaseController(args) {

        constructor(
            currencyCode: String,
            address: String?,
            balance: BigDecimal,
            feeEstimate: BigDecimal
        ) : this(bundleOf(
            "currencyCode" to currencyCode,
            "address" to address,
            "balance" to balance,
            "feeEstimate" to feeEstimate
        ))

        private val currencyCode: String = arg("currencyCode")
        private val address: String? = argOptional("address")
        private val balance: BigDecimal = arg("balance")
        private val feeEstimate: BigDecimal = arg("feeEstimate")

        init {
            overridePopHandler(DialogChangeHandler())
            overridePushHandler(DialogChangeHandler())
        }

        private val binding by viewBinding(ControllerConfirmTxDetailsBinding::inflate)

        override fun onAttach(view: View) {
            super.onAttach(view)
            with(binding) {
                toLabel.isVisible = false
                toAddress.isVisible = false
                amountLabel.isVisible = false
                amountValue.isVisible = false
                totalCostLabel.isVisible = false
                totalCostValue.isVisible = false
                hederaMemoLabel.isVisible = false
                hederaMemoValue.isVisible = false
                destinationTagLabel.isVisible = false
                destinationTagValue.isVisible = false
                processingTimeLabel.text = view.resources.getString(
                    com.breadwallet.R.string.Confirmation_processingTime,
                    view.resources.getString(
                        com.breadwallet.R.string.FeeSelector_ethTime
                    )
                )
                sendLabel.setText(when (address) {
                    null -> com.breadwallet.R.string.Staking_unstake
                    else -> com.breadwallet.R.string.Staking_stake
                })
                sendValue.text = balance.formatCryptoForUi(currencyCode)
                networkFeeValue.text = feeEstimate.formatCryptoForUi(currencyCode)

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
