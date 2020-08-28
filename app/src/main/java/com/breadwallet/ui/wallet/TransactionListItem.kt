package com.breadwallet.ui.wallet

import android.text.TextUtils
import android.view.View
import android.view.ViewGroup
import android.widget.RelativeLayout
import com.breadwallet.R
import com.breadwallet.breadbox.formatCryptoForUi
import com.breadwallet.breadbox.formatFiatForUi
import com.breadwallet.tools.manager.BRSharedPrefs
import com.breadwallet.tools.util.BRDateUtil
import com.breadwallet.tools.util.Utils
import com.breadwallet.util.isBitcoinLike
import com.mikepenz.fastadapter.FastAdapter
import com.mikepenz.fastadapter.items.ModelAbstractItem
import kotlinx.android.extensions.LayoutContainer
import kotlinx.android.synthetic.main.tx_item.*

private const val DP_120 = 120
private const val DP_36 = 36
private const val DP_16 = 16
private const val PROGRESS_FULL = 100

class TransactionListItem(
    transaction: WalletTransaction,
    var isCryptoPreferred: Boolean
) : ModelAbstractItem<WalletTransaction, TransactionListItem.ViewHolder>(transaction) {

    override val layoutRes: Int = R.layout.tx_item

    override val type: Int = R.id.transaction_item

    override var identifier: Long = model.txHash.hashCode().toLong()

    override fun getViewHolder(v: View) = ViewHolder(v)

    inner class ViewHolder(
        override val containerView: View
    ) : FastAdapter.ViewHolder<TransactionListItem>(containerView),
        LayoutContainer {

        override fun bindView(item: TransactionListItem, payloads: List<Any>) {
            setTexts(item.model, item.isCryptoPreferred)
        }

        override fun unbindView(item: TransactionListItem) {
            tx_amount.text = null
            tx_date.text = null
            tx_description.text = null
        }

        @Suppress("LongMethod", "ComplexMethod")
        private fun setTexts(transaction: WalletTransaction, isCryptoPreferred: Boolean) {
            val mContext = containerView.context
            val commentString = transaction.memo

            val received = transaction.isReceived
            val amountColor = when {
                received -> R.color.transaction_amount_received_color
                else -> R.color.total_assets_usd_color
            }

            tx_amount.setTextColor(mContext.resources.getColor(amountColor, null))

            val preferredCurrencyCode = when {
                isCryptoPreferred -> transaction.currencyCode
                else -> BRSharedPrefs.getPreferredFiatIso()
            }
            var amount = when {
                isCryptoPreferred -> transaction.amount
                else -> transaction.amountInFiat
            }
            if (!received) {
                amount = amount.negate()
            }
            val formattedAmount = when {
                isCryptoPreferred -> amount.formatCryptoForUi(preferredCurrencyCode)
                else -> amount.formatFiatForUi(preferredCurrencyCode)
            }

            tx_amount.text = formattedAmount

            // If this transaction failed, show the "FAILED" indicator in the cell
            if (transaction.isErrored) {
                showTransactionFailed(transaction, received)
            } else {
                showTransactionProgress(transaction.progress)
            }



            tx_description.text = when {
                commentString == null -> ""
                commentString.isNotEmpty() -> commentString
                transaction.isFeeForToken ->
                    mContext.getString(R.string.Transaction_tokenTransfer, transaction.feeToken)
                received -> {
                    val (res, address) = if (transaction.isComplete) {
                        if (transaction.currencyCode.isBitcoinLike()) {
                            R.string.TransactionDetails_receivedVia to transaction.toAddress
                        } else {
                            R.string.TransactionDetails_receivedFrom to transaction.fromAddress
                        }
                    } else {
                        if (transaction.currencyCode.isBitcoinLike()) {
                            R.string.TransactionDetails_receivingVia to transaction.toAddress
                        } else {
                            R.string.TransactionDetails_receivingFrom to transaction.fromAddress
                        }
                    }
                    mContext.getString(res, address)
                }
                else -> if (transaction.isComplete) {
                    mContext.getString(R.string.Transaction_sentTo, transaction.toAddress)
                } else {
                    mContext.getString(R.string.Transaction_sendingTo, transaction.toAddress)
                }
            }

            //if it's 0 we use the current time.
            val timeStamp =
                if (transaction.timeStamp == 0L) System.currentTimeMillis() else transaction.timeStamp

            tx_date.text = BRDateUtil.getShortDate(timeStamp)
        }

        private fun showTransactionProgress(progress: Int) {
            val context = containerView.context
            tx_failed_button.visibility = View.GONE
            tx_amount.visibility = View.VISIBLE
            if (progress < PROGRESS_FULL) {
                tx_progress.visibility = View.VISIBLE
                tx_date.visibility = View.GONE
                tx_progress.progress = progress
                tx_description.maxWidth = Utils.getPixelsFromDps(context, DP_120)
                tx_description.layoutParams = RelativeLayout.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                    ViewGroup.LayoutParams.MATCH_PARENT
                ).apply {
                    addRule(RelativeLayout.RIGHT_OF, tx_progress.id)
                    addRule(RelativeLayout.CENTER_VERTICAL)
                    setMargins(
                        Utils.getPixelsFromDps(context, DP_16),
                        Utils.getPixelsFromDps(context, DP_36),
                        0,
                        0
                    )
                }
            } else {
                tx_date.visibility = View.VISIBLE
                tx_progress.visibility = View.INVISIBLE
                tx_description.layoutParams =
                    RelativeLayout.LayoutParams(
                        ViewGroup.LayoutParams.WRAP_CONTENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT
                    ).apply {
                        addRule(RelativeLayout.ALIGN_LEFT, tx_date.id)
                        addRule(RelativeLayout.BELOW, tx_date.id)
                        addRule(RelativeLayout.START_OF, tx_amount.id)
                        setMargins(0, 0, 0, 0)
                    }
            }
        }

        private fun showTransactionFailed(tx: WalletTransaction, received: Boolean) {
            val context = containerView.context
            tx_date.visibility = View.INVISIBLE
            tx_failed_button.visibility = View.VISIBLE

            // Align txn description with respect to 'Failed' msg
            tx_description.layoutParams =
                RelativeLayout.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
                ).apply {
                    addRule(RelativeLayout.RIGHT_OF, tx_failed_button.id)
                    addRule(RelativeLayout.CENTER_VERTICAL)
                    setMargins(Utils.getPixelsFromDps(context, DP_16), 0, 0, 0)
                }

            if (!received) {
                tx_description.text =
                    context.getString(R.string.Transaction_sendingTo)
                        .format(tx.toAddress)
                tx_description.ellipsize = TextUtils.TruncateAt.END
                tx_description.maxWidth = Utils.getPixelsFromDps(context, DP_120)
            } else {
                tx_amount.visibility = View.GONE
            }
        }
    }
}
