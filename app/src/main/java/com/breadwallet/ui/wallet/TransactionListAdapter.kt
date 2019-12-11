/**
 * BreadWallet
 *
 * Created by Drew Carlson <drew.carlson@breadwallet.com> on 9/17/19.
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
package com.breadwallet.ui.wallet

import android.content.Context
import android.support.v7.widget.RecyclerView
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ProgressBar
import android.widget.RelativeLayout

import com.breadwallet.R
import com.breadwallet.legacy.presenter.customviews.BaseTextView
import com.breadwallet.tools.manager.BRSharedPrefs
import com.breadwallet.tools.util.BRDateUtil
import com.breadwallet.tools.util.Utils
import com.breadwallet.breadbox.formatCryptoForUi
import com.breadwallet.breadbox.formatFiatForUi
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancelChildren

import java.util.ArrayList

class TransactionListAdapter(
    private val mContext: Context,
    items: List<WalletTransaction>?,
    private val mOnItemClickListener: (@ParameterName("item") WalletTransaction) -> Unit
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    private val mTxResourceId = R.layout.tx_item
    var items: List<WalletTransaction> = items ?: ArrayList()
    private var mIsCryptoPreferred = false

    fun setIsCryptoPreferred(isCryptoPreferred: Boolean) {
        mIsCryptoPreferred = isCryptoPreferred
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        return TxHolder(inflater.inflate(mTxResourceId, parent, false))
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        if (holder.itemViewType == TX_TYPE) {
            holder.itemView.setOnClickListener {
                if (holder.adapterPosition != RecyclerView.NO_POSITION) {
                    mOnItemClickListener(items[holder.adapterPosition])
                }
            }
            setTexts(holder as TxHolder, position)
        }
    }

    override fun onViewDetachedFromWindow(holder: RecyclerView.ViewHolder) {
        super.onViewDetachedFromWindow(holder)
        (holder as TxHolder).coroutineContext.cancelChildren()
    }

    override fun getItemViewType(position: Int): Int {
        return TX_TYPE
    }

    override fun getItemCount(): Int {
        return items.size
    }

    @Suppress("LongMethod", "ComplexMethod")
    private fun setTexts(convertView: TxHolder, position: Int) {
        val item = items[position]
        val commentString = item.memo

        val received = item.isReceived
        val amountColor = when {
            received -> R.color.transaction_amount_received_color
            else -> R.color.total_assets_usd_color
        }

        convertView.transactionAmount.setTextColor(mContext.resources.getColor(amountColor, null))

        // If this transaction failed, show the "FAILED" indicator in the cell
        if (!item.isValid || item.isErrored) {
            showTransactionFailed(convertView, item, received)
        }

        var cryptoAmount = item.amount.abs()

        // TODO: Properly handle fee case -- if fee & fiat preferred, need fee in fiat
        if (item.isFeeForToken) {
            cryptoAmount = item.fee
        }

        val preferredCurrencyCode = when {
            mIsCryptoPreferred -> item.currencyCode
            else -> BRSharedPrefs.getPreferredFiatIso(mContext)
        }
        var amount = when {
            mIsCryptoPreferred -> cryptoAmount
            else -> item.amountInFiat
        }
        if (!received && amount != null) {
            amount = amount.negate()
        }
        val formattedAmount = when {
            mIsCryptoPreferred -> amount.formatCryptoForUi(preferredCurrencyCode)
            else -> amount.formatFiatForUi(preferredCurrencyCode)
        }

        convertView.transactionAmount.text = formattedAmount

        if (item.isPending) {
            showTransactionProgress(convertView, item.confirmations * PROGRESS_PACE)
        }
        val sentTo = mContext.getString(R.string.Transaction_sentTo).format(item.toAddress)
        val receivedVia = mContext.getString(R.string.TransactionDetails_receivedVia)
            .format(item.fromAddress)

        val sendingTo = mContext.getString(R.string.Transaction_sendingTo).format(item.toAddress)
        val receivingVia = mContext.getString(R.string.TransactionDetails_receivingVia)
            .format(item.fromAddress)

        if (item.confirmations > item.confirmationsUntilFinal) {
            convertView.transactionDetail.text = when {
                commentString.isNotEmpty() -> commentString
                received -> receivedVia
                else -> sentTo
            }
        } else {
            convertView.transactionDetail.text = when {
                commentString.isNotEmpty() -> commentString
                !received -> sendingTo
                else -> receivingVia
            }
        }
        // it's a token transfer ETH tx
        if (item.isFeeForToken) {
            convertView.transactionDetail.text =
                mContext.getString(R.string.Transaction_tokenTransfer)
                    .format(item.feeToken)
        }

        //if it's 0 we use the current time.
        val timeStamp = if (item.timeStamp == 0L) System.currentTimeMillis() else item.timeStamp

        convertView.transactionDate.text = BRDateUtil.getShortDate(timeStamp)
    }

    private fun showTransactionProgress(holder: TxHolder, progress: Int) {
        if (progress < PROGRESS_FULL) {
            holder.transactionProgress.visibility = View.VISIBLE
            holder.transactionDate.visibility = View.GONE
            holder.transactionProgress.progress = progress
            holder.transactionDetail.maxWidth = Utils.getPixelsFromDps(mContext, DP_120)
            holder.transactionDetail.layoutParams = RelativeLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            ).apply {
                addRule(RelativeLayout.RIGHT_OF, holder.transactionProgress.id)
                addRule(RelativeLayout.CENTER_VERTICAL)
                setMargins(
                    Utils.getPixelsFromDps(mContext, DP_16),
                    Utils.getPixelsFromDps(mContext, DP_36),
                    0,
                    0
                )
            }
        } else {
            holder.transactionDate.visibility = View.VISIBLE
            holder.transactionProgress.visibility = View.INVISIBLE
            holder.transactionDetail.layoutParams =
                RelativeLayout.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
                ).apply {
                    addRule(RelativeLayout.ALIGN_PARENT_LEFT)
                    addRule(RelativeLayout.CENTER_VERTICAL)
                    setMargins(Utils.getPixelsFromDps(mContext, DP_16), 0, 0, 0)
                }
        }
    }

    private fun showTransactionFailed(holder: TxHolder, tx: WalletTransaction, received: Boolean) {
        holder.transactionDate.visibility = View.INVISIBLE
        holder.transactionFailed.visibility = View.VISIBLE

        // Align txn description with respect to 'Failed' msg
        val params = RelativeLayout.LayoutParams(
            ViewGroup.LayoutParams.WRAP_CONTENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        )
        params.addRule(RelativeLayout.RIGHT_OF, holder.transactionFailed.id)
        holder.transactionDetail.layoutParams = params
        holder.transactionDetail.gravity = Gravity.CENTER_VERTICAL

        if (!received) {
            holder.transactionDetail.text =
                mContext.getString(R.string.Transaction_sendingTo)
                    .format(tx.toAddress)
        }
    }

    private inner class TxHolder constructor(view: View) : RecyclerView.ViewHolder(view),
        CoroutineScope {

        override val coroutineContext = SupervisorJob() + Dispatchers.Default

        val transactionDate: BaseTextView = view.findViewById(R.id.tx_date)
        val transactionAmount: BaseTextView = view.findViewById(R.id.tx_amount)
        val transactionDetail: BaseTextView = view.findViewById(R.id.tx_description)
        val transactionFailed: Button = view.findViewById(R.id.tx_failed_button)
        val transactionProgress: ProgressBar = view.findViewById(R.id.tx_progress)
    }

    companion object {
        private const val DP_120 = 120
        private const val DP_36 = 36
        private const val DP_16 = 16
        private const val PROGRESS_FULL = 100
        private const val PROGRESS_PACE = 20
        private const val TX_TYPE = 0
    }
}
