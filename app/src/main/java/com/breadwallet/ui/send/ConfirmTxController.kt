/**
 * BreadWallet
 *
 * Created by Drew Carlson <drew.carlson@breadwallet.com> on 10/2/19.
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
import androidx.core.os.bundleOf
import androidx.core.view.isGone
import androidx.core.view.isVisible
import com.breadwallet.R
import com.breadwallet.breadbox.TransferSpeed
import com.breadwallet.breadbox.formatCryptoForUi
import com.breadwallet.databinding.ControllerConfirmTxDetailsBinding
import com.breadwallet.ui.formatFiatForUi
import com.breadwallet.tools.util.eth
import com.breadwallet.ui.BaseController
import com.breadwallet.ui.changehandlers.DialogChangeHandler
import java.math.BigDecimal

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

/**
 * Transaction detail to be shown for user verification before requesting authentication.
 */
class ConfirmTxController(
    args: Bundle? = null
) : BaseController(args) {

    interface Listener {
        fun onPositiveClicked(controller: ConfirmTxController) = Unit
        fun onNegativeClicked(controller: ConfirmTxController) = Unit
    }

    constructor(
        currencyCode: String,
        fiatCode: String,
        feeCode: String,
        targetAddress: String,
        transferSpeed: TransferSpeed,
        amount: BigDecimal,
        fiatAmount: BigDecimal,
        fiatTotalCost: BigDecimal,
        fiatNetworkFee: BigDecimal,
        transferFields: List<TransferField>
    ) : this(
        bundleOf(
            KEY_CURRENCY_CODE to currencyCode,
            KEY_FIAT_CODE to fiatCode,
            KEY_FEE_CODE to feeCode,
            KEY_TARGET_ADDRESS to targetAddress,
            KEY_TRANSFER_SPEED to transferSpeed.toString(),
            KEY_AMOUNT to amount,
            KEY_FIAT_AMOUNT to fiatAmount,
            KEY_FIAT_TOTAL_COST to fiatTotalCost,
            KEY_NETWORK_FEE to fiatNetworkFee,
            KEY_TRANSFER_FIELDS to transferFields
        )
    )

    val model = ConfirmTxModel(
        arg(KEY_CURRENCY_CODE),
        arg(KEY_FIAT_CODE),
        arg(KEY_FEE_CODE),
        arg(KEY_TARGET_ADDRESS),
        TransferSpeed.valueOf(arg(KEY_TRANSFER_SPEED)),
        BigDecimal(arg<Double>(KEY_AMOUNT)),
        BigDecimal(arg<Double>(KEY_FIAT_AMOUNT)),
        BigDecimal(arg<Double>(KEY_FIAT_TOTAL_COST)),
        BigDecimal(arg<Double>(KEY_NETWORK_FEE)),
        arg(KEY_TRANSFER_FIELDS)
    )

    override val layoutId = R.layout.controller_confirm_tx_details

    init {
        overridePushHandler(DialogChangeHandler())
        overridePopHandler(DialogChangeHandler())
    }

    private val binding by viewBinding(ControllerConfirmTxDetailsBinding::inflate)

    override fun onCreateView(view: View) {
        super.onCreateView(view)
        with(binding) {
            okBtn.setOnClickListener {
                router.popCurrentController()
                findListener<Listener>()?.onPositiveClicked(this@ConfirmTxController)
            }
            val cancelTxListener = View.OnClickListener {
                router.popCurrentController()
                findListener<Listener>()?.onNegativeClicked(this@ConfirmTxController)
            }
            cancelBtn.setOnClickListener(cancelTxListener)
            closeBtn.setOnClickListener(cancelTxListener)
            layoutBackground.setOnClickListener(cancelTxListener)
        }
    }

    override fun onAttach(view: View) {
        super.onAttach(view)
        model.render()
    }

    override fun handleBack(): Boolean {
        findListener<Listener>()?.onNegativeClicked(this)
        return super.handleBack()
    }

    private fun ConfirmTxModel.render() {
        val res = checkNotNull(resources)
        val fiatAmountString = fiatAmount.formatFiatForUi(fiatCode)
        with (binding) {
            sendValue.text = "%s (%s)".format(amount.formatCryptoForUi(currencyCode), fiatAmountString)
            toAddress.text = targetAddress
            amountValue.text = fiatAmountString

            val isErc20 = !currencyCode.equals(eth, true) && feeCode.equals(eth, true)

            val processingTime = res.getString(
                when {
                    isErc20 -> R.string.FeeSelector_ethTime
                    else -> when (transferSpeed) {
                        is TransferSpeed.Economy -> R.string.FeeSelector_economyTime
                        is TransferSpeed.Regular -> R.string.FeeSelector_regularTime
                        is TransferSpeed.Priority -> R.string.FeeSelector_priorityTime
                    }
                }
            )
            processingTimeLabel.text =
                res.getString(R.string.Confirmation_processingTime, processingTime)

            networkFeeLabel.setText(R.string.Confirmation_feeLabel)
            totalCostLabel.isGone = isErc20
            totalCostValue.isGone = isErc20
            totalCostValue.text = fiatTotalCost.formatFiatForUi(fiatCode)
            networkFeeValue.text = fiatNetworkFee.formatFiatForUi(fiatCode)

            transferFields.forEach { field ->
                when (field.key) {
                    TransferField.DESTINATION_TAG -> {
                        groupDestinationTag.isVisible = true
                        if (field.value.isNullOrEmpty()) {
                            destinationTagValue.setText(R.string.Confirmation_destinationTag_EmptyHint)
                        } else {
                            destinationTagValue.text = field.value
                        }
                    }
                    TransferField.HEDERA_MEMO -> {
                        groupHederaMemo.isVisible = true
                        if (field.value.isNullOrEmpty()) {
                            hederaMemoValue.setText(R.string.Confirmation_destinationTag_EmptyHint)
                        } else {
                            hederaMemoValue.text = field.value
                        }
                    }
                }
            }
        }
    }
}

data class ConfirmTxModel(
    val currencyCode: String,
    val fiatCode: String,
    val feeCode: String,
    val targetAddress: String,
    val transferSpeed: TransferSpeed,
    val amount: BigDecimal,
    val fiatAmount: BigDecimal,
    val fiatTotalCost: BigDecimal,
    val fiatNetworkFee: BigDecimal,
    val transferFields: List<TransferField>
)
