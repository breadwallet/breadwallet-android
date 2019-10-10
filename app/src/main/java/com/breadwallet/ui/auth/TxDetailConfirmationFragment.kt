/**
 * BreadWallet
 *
 * Created by Pablo Budelli on <pablo.budelli@breadwallet.com> 9/26/19.
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
package com.breadwallet.ui.auth

import android.app.Fragment
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isGone
import com.breadwallet.R
import com.breadwallet.model.FeeOption
import com.breadwallet.model.TxConfirmationDetail
import kotlinx.android.synthetic.main.fragment_tx_detail_confirmation.*

/**
 * Transaction detail to be shown for user verification before requesting authentication.
 */
class TxDetailConfirmationFragment : Fragment() {

    companion object {
        fun newInstance(txDetail: TxConfirmationDetail, onAccept: () -> Unit): TxDetailConfirmationFragment =
                TxDetailConfirmationFragment().apply {
                    this.onAccept = onAccept
                    this.txDetail = txDetail
                }
    }

    private lateinit var onAccept: () -> Unit
    private lateinit var txDetail: TxConfirmationDetail

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_tx_detail_confirmation, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        loadTxData()

        ok_btn.setOnClickListener {
            onAccept.invoke()
            activity.fragmentManager.beginTransaction().remove(this).commit()
        }
        val cancelTxListener = View.OnClickListener {
            activity.fragmentManager.beginTransaction().remove(this).commit()
        }
        cancel_btn.setOnClickListener(cancelTxListener)
        close_btn.setOnClickListener(cancelTxListener)
    }

    private fun loadTxData() {
        send_value.text = "${txDetail.cryptoAmount} (${txDetail.fiatAmount})"
        to_address.text = txDetail.destinationAddress
        amount_value.text = txDetail.fiatAmount

        val processingTime = getString(when {
            txDetail.isErc20 -> R.string.FeeSelector_ethTime
            else -> when (txDetail.feeOption) {
                FeeOption.ECONOMY -> R.string.FeeSelector_economyTime
                FeeOption.REGULAR -> R.string.FeeSelector_regularTime
                FeeOption.PRIORITY -> R.string.FeeSelector_priorityTime
            }
        })
        processing_time_label.text = getString(R.string.Confirmation_processingTime, processingTime)

        network_fee_label.text = getString(when {
            txDetail.isErc20 -> R.string.Confirmation_feeLabelETH
            else -> R.string.Confirmation_feeLabel
        })
        network_fee_value.text = txDetail.networkFeeFiatAmount
        total_cost_label.isGone = txDetail.isErc20
        total_cost_value.isGone = txDetail.isErc20
        total_cost_value.text = txDetail.totalCost
    }
}