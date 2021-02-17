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
package com.breadwallet.ui.platform

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.os.bundleOf
import com.bluelinelabs.conductor.RouterTransaction
import com.breadwallet.R
import com.breadwallet.breadbox.TransferSpeed
import com.breadwallet.tools.manager.BRSharedPrefs
import com.breadwallet.tools.security.isFingerPrintAvailableAndSetup
import com.breadwallet.ui.BaseController
import com.breadwallet.ui.auth.AuthenticationController
import com.breadwallet.ui.auth.AuthMode
import com.breadwallet.ui.changehandlers.DialogChangeHandler
import com.breadwallet.ui.send.ConfirmTxController
import com.platform.ConfirmTransactionMessage
import com.platform.PlatformTransactionBus
import com.platform.TransactionResultMessage

private const val KEY_CURRENCY_CODE = "currency_code"
private const val KEY_FIAT_CODE = "fiat_code"
private const val KEY_FEE_CODE = "fee_code"
private const val KEY_TARGET_ADDRESS = "target_address"
private const val KEY_TRANSFER_SPEED = "transfer_speed"
private const val KEY_AMOUNT = "amount"
private const val KEY_FIAT_AMOUNT = "fiat_amount"
private const val KEY_FIAT_TOTAL_COST = "fiat_total_cost"
private const val KEY_NETWORK_FEE = "fiat_network_fee"
private const val KEY_TRANSFER_FIELDS = "transfer_fields"

class PlatformConfirmTransactionController(
    args: Bundle? = null
) : BaseController(args), AuthenticationController.Listener, ConfirmTxController.Listener {

    init {
        overridePopHandler(DialogChangeHandler())
        overridePushHandler(DialogChangeHandler())
    }

    constructor(
        confirmationTxMessage: ConfirmTransactionMessage
    ) : this(
        bundleOf(
            KEY_CURRENCY_CODE to confirmationTxMessage.currencyCode,
            KEY_FIAT_CODE to confirmationTxMessage.fiatCode,
            KEY_FEE_CODE to confirmationTxMessage.feeCode,
            KEY_TARGET_ADDRESS to confirmationTxMessage.targetAddress,
            KEY_TRANSFER_SPEED to confirmationTxMessage.transferSpeed.toString(),
            KEY_AMOUNT to confirmationTxMessage.amount,
            KEY_FIAT_AMOUNT to confirmationTxMessage.fiatAmount,
            KEY_FIAT_TOTAL_COST to confirmationTxMessage.fiatTotalCost,
            KEY_NETWORK_FEE to confirmationTxMessage.fiatNetworkFee,
            KEY_TRANSFER_FIELDS to confirmationTxMessage.transferFields
        )
    )

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup, savedViewState: Bundle?): View {
        return View(container.context)
    }

    override fun onAttach(view: View) {
        super.onAttach(view)
        val controller = ConfirmTxController(
            arg(KEY_CURRENCY_CODE),
            arg(KEY_FIAT_CODE),
            arg(KEY_FEE_CODE),
            arg(KEY_TARGET_ADDRESS),
            TransferSpeed.valueOf(arg(KEY_TRANSFER_SPEED)),
            arg(KEY_AMOUNT),
            arg(KEY_FIAT_AMOUNT),
            arg(KEY_FIAT_TOTAL_COST),
            arg(KEY_NETWORK_FEE),
            arg(KEY_TRANSFER_FIELDS)
        )
        controller.targetController = this
        router.pushController(RouterTransaction.with(controller))
    }

    override fun onPositiveClicked(controller: ConfirmTxController) {
        val res = checkNotNull(resources)
        val authenticationMode =
            if (isFingerPrintAvailableAndSetup(activity!!) && BRSharedPrefs.sendMoneyWithFingerprint) {
                AuthMode.USER_PREFERRED
            } else {
                AuthMode.PIN_REQUIRED
            }

        val authController = AuthenticationController(
            mode = authenticationMode,
            title = res.getString(R.string.VerifyPin_title),
            message = res.getString(R.string.VerifyPin_authorize)
        )
        authController.targetController = this
        router.pushController(RouterTransaction.with(authController))
    }

    override fun onNegativeClicked(controller: ConfirmTxController) {
        PlatformTransactionBus.sendMessage(
            TransactionResultMessage.TransactionCancelled(messageFromBundle())
        )
        router.popController(this@PlatformConfirmTransactionController)
    }

    override fun onAuthenticationSuccess() {
        PlatformTransactionBus.sendMessage(
            TransactionResultMessage.TransactionConfirmed(messageFromBundle())
        )
        router.popController(this@PlatformConfirmTransactionController)
    }

    override fun onAuthenticationFailed() {
        handleNotAuthenticated()
    }

    override fun onAuthenticationCancelled() {
        handleNotAuthenticated()
    }

    private fun handleNotAuthenticated() {
        PlatformTransactionBus.sendMessage(
            TransactionResultMessage.TransactionCancelled(messageFromBundle())
        )
        router.popController(this@PlatformConfirmTransactionController)
    }

    private fun messageFromBundle() = ConfirmTransactionMessage(
        arg(KEY_CURRENCY_CODE),
        arg(KEY_FIAT_CODE),
        arg(KEY_FEE_CODE),
        arg(KEY_TARGET_ADDRESS),
        TransferSpeed.valueOf(arg(KEY_TRANSFER_SPEED)),
        arg(KEY_AMOUNT),
        arg(KEY_FIAT_AMOUNT),
        arg(KEY_FIAT_TOTAL_COST),
        arg(KEY_NETWORK_FEE),
        arg(KEY_TRANSFER_FIELDS)
    )
}
