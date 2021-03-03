package com.breadwallet.ui.wallet

import android.graphics.Paint
import android.os.Build
import android.text.format.DateUtils
import android.view.View
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import com.breadwallet.R
import com.breadwallet.breadbox.formatCryptoForUi
import com.breadwallet.databinding.TxItemBinding
import com.breadwallet.ui.formatFiatForUi
import com.breadwallet.tools.manager.BRSharedPrefs
import com.breadwallet.tools.util.BRDateUtil
import com.breadwallet.tools.util.Utils
import com.breadwallet.util.isBitcoinLike
import com.mikepenz.fastadapter.FastAdapter
import com.mikepenz.fastadapter.items.ModelAbstractItem

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
        v: View
    ) : FastAdapter.ViewHolder<TransactionListItem>(v) {

        var binding: TxItemBinding? = null

        override fun bindView(item: TransactionListItem, payloads: List<Any>) {
            binding = TxItemBinding.bind(itemView)
            setTexts(binding!!, item.model, item.isCryptoPreferred)
        }

        override fun unbindView(item: TransactionListItem) {
            with(binding ?: return) {
                txAmount.text = null
                txDate.text = null
                txDescriptionValue.text = null
                txDescriptionLabel.text = null
            }
            binding = null
        }

        @Suppress("LongMethod", "ComplexMethod")
        private fun setTexts(
            binding: TxItemBinding,
            transaction: WalletTransaction,
            isCryptoPreferred: Boolean
        ) {
            val context = itemView.context
            val commentString = transaction.memo

            val received = transaction.isReceived

            binding.imageTransferDirection.setBackgroundResource(
                when {
                    transaction.progress < PROGRESS_FULL ->
                        R.drawable.transfer_in_progress
                    transaction.isErrored -> R.drawable.transfer_failed
                    transaction.gift != null -> when {
                        transaction.gift.claimed -> R.drawable.transfer_gift_claimed
                        transaction.gift.reclaimed -> R.drawable.transfer_gift_reclaimed
                        else -> R.drawable.transfer_gift_unclaimed
                    }
                    received -> R.drawable.transfer_receive
                    else -> R.drawable.transfer_send
                }
            )

            binding.txAmount.setTextColor(
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

            binding.txAmount.text = formattedAmount

           when {
                !transaction.gift?.recipientName.isNullOrBlank() -> {
                    binding.txDescriptionLabel.text =
                        context.getString(R.string.Transaction_toRecipient, "")
                    binding.txDescriptionValue.text = transaction.gift?.recipientName
                }
                transaction.isStaking -> {
                    binding.txDescriptionLabel.text =
                        context.getString(R.string.Transaction_stakingTo, "")
                    binding.txDescriptionValue.text = transaction.toAddress
                }
                commentString == null -> {
                    binding.txDescriptionLabel.text = ""
                    binding.txDescriptionValue.text = ""
                }
                commentString.isNotEmpty() -> {
                    binding.txDescriptionLabel.text = ""
                    binding.txDescriptionValue.text = commentString
                }
                transaction.isFeeForToken -> {
                    binding.txDescriptionLabel.text =
                        context.getString(R.string.Transaction_tokenTransfer, transaction.feeToken)
                    binding.txDescriptionValue.text = commentString
                }
                received -> {
                    if (transaction.isComplete) {
                        if (transaction.currencyCode.isBitcoinLike()) {
                            binding.txDescriptionLabel.text =
                                context.getString(R.string.TransactionDetails_receivedVia, "")
                            binding.txDescriptionValue.text = transaction.toAddress
                        } else {
                            binding.txDescriptionLabel.text =
                                context.getString(R.string.TransactionDetails_receivedFrom, "")
                            binding.txDescriptionValue.text = transaction.fromAddress
                        }
                    } else {
                        if (transaction.currencyCode.isBitcoinLike()) {
                            binding.txDescriptionLabel.text =
                                context.getString(R.string.TransactionDetails_receivingVia, "")
                            binding.txDescriptionValue.text = transaction.toAddress
                        } else {
                            binding.txDescriptionLabel.text =
                                context.getString(R.string.TransactionDetails_receivingFrom, "")
                            binding.txDescriptionValue.text = transaction.fromAddress
                        }
                    }
                }
                else -> if (transaction.isComplete) {
                    binding.txDescriptionLabel.text =
                        context.getString(R.string.Transaction_sentTo, "")
                    binding.txDescriptionValue.text = transaction.toAddress
                } else {
                    binding.txDescriptionLabel.text =
                        context.getString(R.string.Transaction_sendingTo, "")
                    binding.txDescriptionValue.text = transaction.toAddress
                }
            }

            val timeStamp = transaction.timeStamp
            binding.txDate.text = when {
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
            val context = itemView.context
            with(binding!!) {
                txDate.isVisible = true
                txAmount.isVisible = true
                imageTransferDirection.isVisible = true

                val textColor = context.getColor(R.color.total_assets_usd_color)
                txDate.setTextColor(textColor)
                txAmount.paintFlags = 0 // clear strike-through

                if (progress < PROGRESS_FULL) {
                    if (imageTransferDirection.progressDrawable == null) {
                        imageTransferDirection.progressDrawable =
                            ContextCompat.getDrawable(
                                context,
                                R.drawable.transfer_progress_drawable
                            )
                    }
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                        imageTransferDirection.setProgress(progress, true)
                    } else {
                        imageTransferDirection.progress = progress
                    }
                    txDescriptionValue.maxWidth = Utils.getPixelsFromDps(context, DP_120)
                } else {
                    imageTransferDirection.progressDrawable = null
                    imageTransferDirection.progress = 0
                }
            }
        }

        private fun showTransactionFailed() {
            val context = itemView.context
            with(binding!!) {
                imageTransferDirection.progressDrawable = null

                val errorColor = context.getColor(R.color.ui_error)
                txDate.setText(R.string.Transaction_failed)
                txDate.setTextColor(errorColor)
                txAmount.setTextColor(errorColor)
                txAmount.paintFlags = txAmount.paintFlags or Paint.STRIKE_THRU_TEXT_FLAG
            }
        }
    }
}
