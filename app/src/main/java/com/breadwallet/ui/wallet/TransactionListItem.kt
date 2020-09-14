package com.breadwallet.ui.wallet

import android.graphics.Paint
import android.os.Build
import android.text.format.DateUtils
import android.view.View
import androidx.core.view.isVisible
import com.breadwallet.BuildConfig
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
            val context = containerView.context
            val commentString = transaction.memo

            val received = transaction.isReceived

            imageTransferDirection.setBackgroundResource(
                when {
                    transaction.progress < PROGRESS_FULL ->
                        R.drawable.transfer_in_progress
                    transaction.isErrored -> R.drawable.transfer_failed
                    received -> R.drawable.transfer_receive
                    else -> R.drawable.transfer_send
                }
            )

            tx_amount.setTextColor(
                context.getColor(
                    when {
                        received -> R.color.transaction_amount_received_color
                        else -> R.color.total_assets_usd_color
                    }
                )
            )

            val preferredCurrencyCode = when {
                isCryptoPreferred -> transaction.currencyCode
                else -> BRSharedPrefs.getPreferredFiatIso()
            }
            val amount = when {
                isCryptoPreferred -> transaction.amount
                else -> transaction.amountInFiat
            }.run { if (received) this else negate() }

            val formattedAmount = when {
                isCryptoPreferred -> amount.formatCryptoForUi(preferredCurrencyCode)
                else -> amount.formatFiatForUi(preferredCurrencyCode)
            }

            tx_amount.text = formattedAmount

            tx_description.text = when {
                commentString == null -> ""
                commentString.isNotEmpty() -> commentString
                transaction.isFeeForToken ->
                    context.getString(R.string.Transaction_tokenTransfer, transaction.feeToken)
                received -> {
                    val (res, address) = if (transaction.isComplete) {
                        if (transaction.currencyCode.isBitcoinLike()) {
                            R.string.TransactionDetails_receivedVia to transaction.truncatedToAddress
                        } else {
                            R.string.TransactionDetails_receivedFrom to transaction.truncatedFromAddress
                        }
                    } else {
                        if (transaction.currencyCode.isBitcoinLike()) {
                            R.string.TransactionDetails_receivingVia to transaction.truncatedToAddress
                        } else {
                            R.string.TransactionDetails_receivingFrom to transaction.truncatedFromAddress
                        }
                    }
                    context.getString(res, address)
                }
                else -> if (transaction.isComplete) {
                    context.getString(R.string.Transaction_sentTo, transaction.truncatedToAddress)
                } else {
                    context.getString(R.string.Transaction_sendingTo, transaction.truncatedToAddress)
                }
            }

            val timeStamp = transaction.timeStamp
            tx_date.text = when {
                timeStamp == 0L || transaction.isPending -> buildString {
                    append(transaction.confirmations)
                    append('/')
                    append(transaction.confirmationsUntilFinal)
                    append(' ')
                    append(context.getString(R.string.TransactionDetails_confirmationsLabel))
                }
                DateUtils.isToday(timeStamp) -> BRDateUtil.getTime(timeStamp)
                else -> BRDateUtil.getShortDate(timeStamp)
            }

            // If this transaction failed, show the "FAILED" indicator in the cell
            if (transaction.isErrored) {
                showTransactionFailed()
            } else {
                showTransactionProgress(transaction.progress)
            }
        }

        private fun showTransactionProgress(progress: Int) {
            val context = containerView.context
            tx_date.isVisible = true
            tx_amount.isVisible = true
            imageTransferDirection.isVisible = true

            val textColor = context.getColor(R.color.total_assets_usd_color)
            tx_date.setTextColor(textColor)
            tx_amount.paintFlags = 0 // clear strike-through

            if (progress < PROGRESS_FULL) {
                if (imageTransferDirection.progressDrawable == null) {
                    imageTransferDirection.progressDrawable = context.getDrawable(R.drawable.transfer_progress_drawable)
                }
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                    imageTransferDirection.setProgress(progress, true)
                } else {
                    imageTransferDirection.progress = progress
                }
                tx_description.maxWidth = Utils.getPixelsFromDps(context, DP_120)
            } else {
                imageTransferDirection.progressDrawable = null
                imageTransferDirection.progress = 0
            }
        }

        private fun showTransactionFailed() {
            val context = containerView.context
            imageTransferDirection.progressDrawable = null

            val errorColor = context.getColor(R.color.ui_error)
            tx_date.setText(R.string.Transaction_failed)
            tx_date.setTextColor(errorColor)
            tx_amount.setTextColor(errorColor)
            tx_amount.paintFlags = tx_amount.paintFlags or Paint.STRIKE_THRU_TEXT_FLAG
        }
    }
}
