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
package com.breadwallet.ui.txdetails

import android.animation.LayoutTransition
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import androidx.core.os.bundleOf
import androidx.core.view.isVisible
import com.breadwallet.R
import com.breadwallet.breadbox.formatCryptoForUi
import com.breadwallet.breadbox.formatFiatForUi
import com.breadwallet.tools.manager.BRClipboardManager
import com.breadwallet.tools.manager.BRSharedPrefs
import com.breadwallet.tools.util.BRDateUtil
import com.breadwallet.tools.util.Utils
import com.breadwallet.ui.BaseMobiusController
import com.breadwallet.ui.ViewEffect
import com.breadwallet.ui.changehandlers.DialogChangeHandler
import com.breadwallet.ui.flowbind.clicks
import com.breadwallet.ui.flowbind.textChanges
import com.breadwallet.ui.models.TransactionState
import com.breadwallet.ui.txdetails.TxDetails.E
import com.breadwallet.ui.txdetails.TxDetails.F
import com.breadwallet.ui.txdetails.TxDetails.M
import com.breadwallet.util.isBitcoinLike
import drewcarlson.mobius.flow.FlowTransformer
import kotlinx.android.synthetic.main.transaction_details.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.merge
import org.kodein.di.direct
import org.kodein.di.erased.instance
import java.math.BigDecimal
import java.text.NumberFormat
import java.util.Date

/**
 * TODO: Updated to support new APIs and improve general UX.
 *   Remaining Issues:
 *     - Expose formatted currencies in Model
 *     - Improve gas price handling
 *     - Validate state displays (handle deleted state?)
 *     - For received transactions, retrieve historical exchange rate at time of first confirmation
 */
class TxDetailsController(
    args: Bundle? = null
) : BaseMobiusController<M, E, F>(args) {

    companion object {
        private const val CURRENCY_CODE = "currencyCode"
        private const val TRANSFER_HASH = "transferHash"
    }

    constructor(currencyCode: String, transferHash: String) : this(
        bundleOf(
            CURRENCY_CODE to currencyCode,
            TRANSFER_HASH to transferHash
        )
    )

    private val currencyCode = arg<String>(CURRENCY_CODE)
    private val transactionHash = arg<String>(TRANSFER_HASH)

    init {
        overridePopHandler(DialogChangeHandler())
        overridePushHandler(DialogChangeHandler())
    }

    override val layoutId = R.layout.transaction_details
    override val init = TxDetailsInit
    override val update = TxDetailsUpdate
    override val defaultModel: M
        get() = M.createDefault(
            currencyCode,
            transactionHash,
            BRSharedPrefs.getPreferredFiatIso(),
            BRSharedPrefs.isCryptoPreferred()
        )

    override val flowEffectHandler: FlowTransformer<F, E>
        get() = createTxDetailsHandler(
            checkNotNull(applicationContext),
            direct.instance(),
            direct.instance()
        )

    override fun onCreateView(view: View) {
        super.onCreateView(view)
        // NOTE: This allows animateLayoutChanges to properly animate details show/hide
        (view as ViewGroup).layoutTransition.enableTransitionType(LayoutTransition.CHANGING)

        val color = tx_to_from_address.textColors.defaultColor
        memo_input.setTextColor(color)
        fee_primary_label.text = resources!!.getString(R.string.Send_fee, "")
        view.setOnClickListener {
            router.popCurrentController()
        }
    }

    override fun bindView(modelFlow: Flow<M>): Flow<E> {
        return merge(
            close_button.clicks().map { E.OnClosedClicked },
            show_hide_details.clicks().map { E.OnShowHideDetailsClicked },
            memo_input.textChanges().map { E.OnMemoChanged(it) },
            transaction_id.clicks().map { E.OnTransactionHashClicked },
            tx_to_from_address.clicks().map { E.OnAddressClicked }
        )
    }

    override fun onDetach(view: View) {
        super.onDetach(view)
        view.setOnClickListener(null)
        Utils.hideKeyboard(activity)
    }

    override fun handleViewEffect(effect: ViewEffect) {
        when (effect) {
            is F.CopyToClipboard -> copyToClipboard(effect.text)
        }
    }

    @Suppress("LongMethod", "ComplexMethod")
    override fun M.render() {
        val res = checkNotNull(resources)

        ifChanged(M::showDetails) {
            details_container.isVisible = showDetails
            show_hide_details.setText(
                when {
                    showDetails -> R.string.TransactionDetails_hideDetails
                    else -> R.string.TransactionDetails_showDetails
                }
            )
        }

        ifChanged(M::isFeeForToken) {
            if (isFeeForToken) { // it's a token transfer ETH tx
                memo_input.setText(
                    String.format(
                        res.getString(R.string.Transaction_tokenTransfer),
                        feeToken
                    )
                )
                memo_input.isFocusable = false
            }
        }

        ifChanged(M::isReceived) {
            showSentViews(!isReceived)

            tx_action.setText(
                when {
                    isReceived -> R.string.TransactionDetails_titleReceived
                    else -> R.string.TransactionDetails_titleSent
                }
            )

            tx_to_from.setText(
                when {
                    isReceived -> if (currencyCode.isBitcoinLike()) {
                        R.string.TransactionDetails_addressViaHeader
                    } else {
                        R.string.TransactionDetails_addressFromHeader
                    }
                    else -> R.string.TransactionDetails_addressToHeader
                }
            )

            tx_amount.setTextColor(
                res.getColor(
                    when {
                        isReceived -> R.color.transaction_amount_received_color
                        else -> R.color.total_assets_usd_color
                    }
                )
            )
        }

        if (!isReceived) {
            ifChanged(
                M::isEth,
                M::gasPrice,
                M::gasLimit
            ) {
                showEthViews(isEth)

                if (isEth) {
                    val formatter = NumberFormat.getIntegerInstance().apply {
                        maximumFractionDigits = 0
                        isGroupingUsed = false
                    }
                    gas_price.text = "%s %s".format(gasPrice.toBigInteger(), "gwei")
                    gas_limit.text = formatter.format(gasLimit).toString()
                }
            }

            ifChanged(M::transactionTotal) {
                fee_secondary.text =
                    transactionTotal.formatCryptoForUi(currencyCode, MAX_CRYPTO_DIGITS)
            }

            ifChanged(
                M::fee,
                M::isErc20
            ) {
                showTotalCost(!isErc20)
                fee_primary.text = fee.formatCryptoForUi(feeCurrency, MAX_CRYPTO_DIGITS)
            }
        }

        ifChanged(M::blockNumber) {
            showConfirmedView(blockNumber != 0)
        }

        ifChanged(
            M::fiatAmountWhenSent,
            M::exchangeCurrencyCode,
            M::fiatAmountNow,
            M::preferredFiatIso
        ) {
            val amountNow = fiatAmountNow.formatFiatForUi(preferredFiatIso)
            val showWhenSent = fiatAmountWhenSent.compareTo(BigDecimal.ZERO) != 0

            label_when_sent.text = res.getString(
                when {
                    isReceived -> R.string.TransactionDetails_amountWhenReceived
                    else -> R.string.TransactionDetails_amountWhenSent
                },
                fiatAmountWhenSent.formatFiatForUi(exchangeCurrencyCode),
                amountNow
            )
            amount_now.text = amountNow

            label_when_sent.visibility = if (showWhenSent) View.VISIBLE else View.INVISIBLE
            amount_now.visibility = if (!showWhenSent) View.VISIBLE else View.INVISIBLE
        }

        ifChanged(M::toOrFromAddress) {
            // TODO: Do we need a string res for no hash text?
            tx_to_from_address.text = when {
                toOrFromAddress.isNotBlank() -> toOrFromAddress
                else -> "<unknown>"
            }
        }

        ifChanged(M::cryptoTransferredAmount) {
            tx_amount.text = cryptoTransferredAmount.formatCryptoForUi(
                currencyCode,
                MAX_CRYPTO_DIGITS,
                !isReceived
            )
        }

        ifChanged(M::memoLoaded) {
            if (memoLoaded && !isFeeForToken) {
                memo_input.setText(memo)
            }
        }

        ifChanged(
            M::exchangeCurrencyCode,
            M::preferredFiatIso,
            M::exchangeRate,
            M::isReceived
        ) {
            if (isReceived) {
                groupExchangeRateSection.isVisible = false
            } else {
                exchange_rate_label.setText(R.string.Transaction_exchangeOnDaySent)
                exchange_rate.text = exchangeRate.formatFiatForUi(
                    when {
                        exchangeCurrencyCode.isNotBlank() -> exchangeCurrencyCode
                        else -> preferredFiatIso
                    }
                )
            }
        }

        ifChanged(M::confirmationDate) {
            tx_date.text = BRDateUtil.getFullDate((confirmationDate ?: Date()).time)
        }

        ifChanged(M::transactionHash) {
            transaction_id.text = transactionHash
        }

        ifChanged(M::confirmedInBlockNumber) {
            confirmed_in_block_number.text = confirmedInBlockNumber
        }

        ifChanged(M::confirmations) {
            confirmations_value.text = confirmations.toString()
        }

        ifChanged(M::transactionState) {
            when (transactionState) {
                TransactionState.CONFIRMED -> {
                    if (isCompleted) {
                        tx_status.setCompoundDrawablesRelativeWithIntrinsicBounds(
                            R.drawable.checkmark_circled,
                            0,
                            0,
                            0
                        )
                        tx_status.setText(R.string.Transaction_complete)
                    } else {
                        tx_status.setCompoundDrawablesWithIntrinsicBounds(0, 0, 0, 0)
                        tx_status.setText(R.string.Transaction_confirming)
                    }
                }
                TransactionState.FAILED -> {
                    tx_status.setCompoundDrawablesWithIntrinsicBounds(0, 0, 0, 0)
                    tx_status.setText(R.string.TransactionDetails_initializedTimestampHeader)
                }
                TransactionState.CONFIRMING -> {
                    tx_status.setCompoundDrawablesWithIntrinsicBounds(0, 0, 0, 0)
                    tx_status.setText(R.string.Transaction_confirming)
                }
            }
        }

        ifChanged(M::destinationTag) {
            if (destinationTag != null) {
                layoutDestinationTag.isVisible = true
                destination_tag_divider.isVisible = true
                if (destinationTag.value.isNullOrEmpty()) {
                    destination_tag_value.setText(R.string.TransactionDetails_destinationTag_EmptyHint)
                } else {
                    destination_tag_value.text = destinationTag.value
                }
            }
        }

        ifChanged(M::hederaMemo) {
            val hederaMemo = hederaMemo
            if (hederaMemo != null) {
                layoutHederaMemo.isVisible = true
                hedera_memo_divider.isVisible = true
                if (hederaMemo.value.isNullOrEmpty()) {
                    hedera_memo_value.setText(R.string.TransactionDetails_destinationTag_EmptyHint)
                } else {
                    hedera_memo_value.text = hederaMemo.value
                }
            }
        }
    }

    private fun showSentViews(show: Boolean) {
        fee_primary_container.isVisible = show
        fee_secondary_container.isVisible = show
        fee_primary_divider.isVisible = show
        fee_secondary_divider.isVisible = show
        showEthViews(show)
    }

    private fun showEthViews(show: Boolean) {
        gas_price_container.isVisible = show
        gas_limit_container.isVisible = show
        gas_price_divider.isVisible = show
        gas_limit_divider.isVisible = show
    }

    private fun showTotalCost(show: Boolean) {
        fee_secondary_container.isVisible = show
        fee_secondary_divider.isVisible = show
    }

    private fun showConfirmedView(show: Boolean) {
        confirmed_container.isVisible = show
        confirmed_divider.isVisible = show
        confirmations_divider.isVisible = show
        confirmations_container.isVisible = show
    }

    private fun copyToClipboard(text: String) {
        BRClipboardManager.putClipboard(text)
        toastLong(R.string.Receive_copied)
    }
}
