/**
 * BreadWallet
 *
 * Created by Drew Carlson on <drew.carlson@breadwallet.com> 10/2/19.
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
import android.view.View
import androidx.core.view.isGone
import com.breadwallet.R
import com.breadwallet.breadbox.formatCryptoForUi
import com.breadwallet.breadbox.formatFiatForUi
import com.breadwallet.ui.BaseController
import com.breadwallet.ui.changehandlers.DialogChangeHandler
import kotlinx.android.synthetic.main.controller_confirm_tx_details.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach

/**
 * Transaction detail to be shown for user verification before requesting authentication.
 */
class ConfirmTxDetailsController(
    args: Bundle? = null
) : BaseController(args) {

    override val layoutId = R.layout.controller_confirm_tx_details

    private val sendSheetController
        get() = targetController as SendSheetController

    init {
        overridePushHandler(DialogChangeHandler())
        overridePopHandler(DialogChangeHandler())
    }

    override fun onCreateView(view: View) {
        super.onCreateView(view)
        ok_btn.setOnClickListener {
            sendSheetController.eventConsumer
                .accept(SendSheetEvent.ConfirmTx.OnConfirmClicked)
            router.popCurrentController()
        }
        val cancelTxListener = View.OnClickListener {
            sendSheetController.eventConsumer
                .accept(SendSheetEvent.ConfirmTx.OnCancelClicked)
            router.popCurrentController()
        }
        cancel_btn.setOnClickListener(cancelTxListener)
        close_btn.setOnClickListener(cancelTxListener)
        layoutBackground.setOnClickListener(cancelTxListener)
    }

    override fun onAttach(view: View) {
        super.onAttach(view)
        sendSheetController.model
            .onEach { it.render() }
            .flowOn(Dispatchers.Main)
            .launchIn(viewAttachScope)
    }

    override fun handleBack(): Boolean {
        sendSheetController.eventConsumer
            .accept(SendSheetEvent.ConfirmTx.OnCancelClicked)
        return super.handleBack()
    }

    private fun SendSheetModel.render() {
        val res = checkNotNull(resources)
        val fiatAmountString = fiatAmount.formatFiatForUi(fiatCode)
        send_value.text = "%s (%s)".format(amount.formatCryptoForUi(currencyCode), fiatAmountString)
        to_address.text = targetAddress
        amount_value.text = fiatAmountString

        val processingTime = res.getString(
            when {
                isErc20 -> R.string.FeeSelector_ethTime
                else -> when (transferSpeed) {
                    SendSheetModel.TransferSpeed.ECONOMY -> R.string.FeeSelector_economyTime
                    SendSheetModel.TransferSpeed.REGULAR -> R.string.FeeSelector_regularTime
                    SendSheetModel.TransferSpeed.PRIORITY -> R.string.FeeSelector_priorityTime
                }
            }
        )
        processing_time_label.text = res.getString(R.string.Confirmation_processingTime, processingTime)

        network_fee_label.text = res.getString(
            when {
                isErc20 -> R.string.Confirmation_feeLabelETH
                else -> R.string.Confirmation_feeLabel
            }
        )
        total_cost_label.isGone = isErc20
        total_cost_value.isGone = isErc20
        total_cost_value.text = fiatTotalCost.formatFiatForUi(fiatCode)
        network_fee_value.text = fiatNetworkFee.formatFiatForUi(fiatCode)
    }
}
