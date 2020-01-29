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
import android.view.inputmethod.InputMethodManager
import androidx.core.os.bundleOf
import androidx.core.view.isVisible
import com.breadwallet.R
import com.breadwallet.breadbox.BreadBoxEffect
import com.breadwallet.breadbox.BreadBoxEffectHandler
import com.breadwallet.breadbox.BreadBoxEvent
import com.breadwallet.breadbox.formatCryptoForUi
import com.breadwallet.breadbox.formatFiatForUi
import com.breadwallet.effecthandler.metadata.MetaDataEffect
import com.breadwallet.effecthandler.metadata.MetaDataEffectHandler
import com.breadwallet.effecthandler.metadata.MetaDataEvent
import com.breadwallet.mobius.CompositeEffectHandler
import com.breadwallet.mobius.nestedConnectable
import com.breadwallet.tools.manager.BRClipboardManager
import com.breadwallet.tools.manager.BRSharedPrefs
import com.breadwallet.tools.util.BRDateUtil
import com.breadwallet.ui.BaseMobiusController
import com.breadwallet.ui.changehandlers.DialogChangeHandler
import com.breadwallet.ui.models.TransactionState
import com.breadwallet.ui.navigation.NavigationEffect
import com.breadwallet.ui.navigation.RouterNavigationEffectHandler
import com.breadwallet.ui.txdetails.TxDetails.E
import com.breadwallet.ui.txdetails.TxDetails.F
import com.breadwallet.ui.txdetails.TxDetails.M
import com.breadwallet.ui.view
import com.spotify.mobius.Connectable
import com.spotify.mobius.functions.Consumer
import kotlinx.android.synthetic.main.transaction_details.*
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

    override val effectHandler: Connectable<F, E> =
        CompositeEffectHandler.from(
            Connectable { output: Consumer<E> ->
                TxDetailsHandler(output, checkNotNull(activity))
            },
            nestedConnectable({ output: Consumer<BreadBoxEvent> ->
                BreadBoxEffectHandler(output, currencyCode, direct.instance())
            }, { effect: F ->
                when (effect) {
                    is F.LoadTransaction ->
                        BreadBoxEffect.LoadTransaction(transactionHash)
                    else -> null
                }
            }, { event: BreadBoxEvent ->
                when (event) {
                    is BreadBoxEvent.OnTransactionUpdated ->
                        E.OnTransactionUpdated(
                            event.transaction,
                            event.gasPrice,
                            event.gasLimit
                        )
                    else -> null
                } as? E
            }),
            nestedConnectable({ output: Consumer<MetaDataEvent> ->
                MetaDataEffectHandler(output, direct.instance(), direct.instance())
            }, { effect: F ->
                when (effect) {
                    is F.LoadTransactionMetaData ->
                        MetaDataEffect.LoadTransactionMetaData(effect.transactionHash)
                    is F.UpdateMemo ->
                        MetaDataEffect.UpdateTransactionComment(
                            effect.transactionHash,
                            effect.memo
                        )
                    else -> null
                }
            }, { event: MetaDataEvent ->
                when (event) {
                    is MetaDataEvent.OnTransactionMetaDataUpdated ->
                        E.OnMetaDataUpdated(event.txMetaData)
                    else -> null
                } as? E
            }),
            nestedConnectable({
                direct.instance<RouterNavigationEffectHandler>()
            }) { effect ->
                when (effect) {
                    F.Close -> NavigationEffect.GoBack
                    else -> null
                }
            }
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

    override fun bindView(output: Consumer<E>) = output.view {
        close_button.onClick(E.OnClosedClicked)
        show_hide_details.onClick(E.OnShowHideDetailsClicked)
        memo_input.onTextChanged { E.OnMemoChanged(it) }

        transaction_id.setOnClickListener {
            transaction_id.text?.toString()?.run(::copyToClipboard)
        }

        tx_to_from_address.setOnClickListener {
            tx_to_from_address.text?.toString()?.run(::copyToClipboard)
        }

        onDispose {
            transaction_id.setOnClickListener(null)
            tx_to_from_address.setOnClickListener(null)
        }
    }

    override fun onDetach(view: View) {
        super.onDetach(view)
        view.setOnClickListener(null)

        // Hide softkeyboard if it's visible
        val imm = view.context.getSystemService(InputMethodManager::class.java)
        imm.hideSoftInputFromWindow(memo_input.windowToken, 0)
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
                    isReceived -> R.string.TransactionDetails_addressViaHeader
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

                fee_primary.text = when {
                    isErc20 -> {
                        String.format(
                            "%s %s",
                            fee.stripTrailingZeros().toPlainString(),
                            "gwei"
                        )
                    }
                    else -> fee.formatCryptoForUi(currencyCode, MAX_CRYPTO_DIGITS)
                }
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

        ifChanged(M::memo) {
            if (memo_input.text.isNullOrEmpty() && !isFeeForToken) memo_input.setText(memo)
        }

        ifChanged(
            M::exchangeCurrencyCode,
            M::preferredFiatIso,
            M::exchangeRate,
            M::isReceived
        ) {
            exchange_rate_label.setText(
                when {
                    isReceived -> R.string.Transaction_exchangeOnDayReceived
                    else -> R.string.Transaction_exchangeOnDaySent
                }
            )
            exchange_rate.text = exchangeRate.formatFiatForUi(
                when {
                    exchangeCurrencyCode.isNotBlank() -> exchangeCurrencyCode
                    else -> preferredFiatIso
                }
            )
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
                if (destinationTag.value == null) {
                    destination_tag_value.setText(R.string.TransactionDetails_destinationTag_EmptyHint)
                } else {
                    destination_tag_value.text = destinationTag.value
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
        BRClipboardManager.putClipboard(
            applicationContext,
            text
        )
        toastLong(R.string.Receive_copied)
    }
}
