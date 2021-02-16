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
import androidx.core.view.isGone
import androidx.core.view.isVisible
import com.breadwallet.R
import com.breadwallet.breadbox.formatCryptoForUi
import com.breadwallet.databinding.TransactionDetailsBinding
import com.breadwallet.ui.formatFiatForUi
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

    private val binding by viewBinding(TransactionDetailsBinding::inflate)

    override fun onCreateView(view: View) {
        super.onCreateView(view)
        // NOTE: This allows animateLayoutChanges to properly animate details show/hide
        (view as ViewGroup).layoutTransition.enableTransitionType(LayoutTransition.CHANGING)

        with(binding) {
            val color = txToFromAddress.textColors.defaultColor
            memoInput.setTextColor(color)
            feePrimaryLabel.text = resources!!.getString(R.string.Send_fee, "")
            view.setOnClickListener {
                router.popCurrentController()
            }
        }
    }

    override fun bindView(modelFlow: Flow<M>): Flow<E> {
        return with(binding) {
            merge(
                closeButton.clicks().map { E.OnClosedClicked },
                showHideDetails.clicks().map { E.OnShowHideDetailsClicked },
                memoInput.textChanges().map { E.OnMemoChanged(it) },
                transactionId.clicks().map { E.OnTransactionHashClicked },
                txToFromAddress.clicks().map { E.OnAddressClicked },
            )
        }
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

        with(binding) {
            ifChanged(M::showDetails) {
                detailsContainer.isVisible = showDetails
                showHideDetails.setText(
                    when {
                        showDetails -> R.string.TransactionDetails_hideDetails
                        else -> R.string.TransactionDetails_showDetails
                    }
                )
            }

            ifChanged(M::isFeeForToken) {
                if (isFeeForToken) { // it's a token transfer ETH tx
                    memoInput.setText(
                        String.format(
                            res.getString(R.string.Transaction_tokenTransfer),
                            feeToken
                        )
                    )
                    memoInput.isFocusable = false
                }
            }

            ifChanged(M::isReceived) {
                showSentViews(!isReceived)

                txAction.setText(
                    when {
                        isReceived -> R.string.TransactionDetails_titleReceived
                        else -> R.string.TransactionDetails_titleSent
                    }
                )

                txToFrom.setText(
                    when {
                        isReceived -> if (currencyCode.isBitcoinLike()) {
                            R.string.TransactionDetails_addressViaHeader
                        } else {
                            R.string.TransactionDetails_addressFromHeader
                        }
                        else -> R.string.TransactionDetails_addressToHeader
                    }
                )

                txAmount.setTextColor(
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
                        gasPrice.text = "%s %s".format(this@render.gasPrice.toBigInteger(), "gwei")
                        gasLimit.text = formatter.format(this@render.gasLimit).toString()
                    }
                }

                ifChanged(M::transactionTotal) {
                    feeSecondary.text =
                        transactionTotal.formatCryptoForUi(currencyCode, MAX_CRYPTO_DIGITS)
                }

                ifChanged(
                    M::fee,
                    M::isErc20
                ) {
                    showTotalCost(!isErc20)
                    feePrimary.text = fee.formatCryptoForUi(feeCurrency, MAX_CRYPTO_DIGITS)
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
                val formattedAmountNow = fiatAmountNow.formatFiatForUi(preferredFiatIso)
                val showWhenSent = fiatAmountWhenSent.compareTo(BigDecimal.ZERO) != 0

                labelWhenSent.text = res.getString(
                    when {
                        isReceived -> R.string.TransactionDetails_amountWhenReceived
                        else -> R.string.TransactionDetails_amountWhenSent
                    },
                    fiatAmountWhenSent.formatFiatForUi(exchangeCurrencyCode),
                    formattedAmountNow
                )
                amountNow.text = formattedAmountNow

                labelWhenSent.visibility = if (showWhenSent) View.VISIBLE else View.INVISIBLE
                amountNow.visibility = if (!showWhenSent) View.VISIBLE else View.INVISIBLE
            }

            ifChanged(M::toOrFromAddress) {
                // TODO: Do we need a string res for no hash text?
                txToFromAddress.text = when {
                    toOrFromAddress.isNotBlank() -> toOrFromAddress
                    else -> "<unknown>"
                }
            }

            ifChanged(M::cryptoTransferredAmount) {
                txAmount.text = cryptoTransferredAmount.formatCryptoForUi(
                    currencyCode,
                    MAX_CRYPTO_DIGITS,
                    !isReceived
                )
            }

            ifChanged(M::memoLoaded) {
                if (memoLoaded && !isFeeForToken) {
                    memoInput.setText(
                        if (gift?.recipientName.isNullOrBlank()) {
                            memo
                        } else {
                            String.format(res.getString(R.string.TransactionDetails_giftedTo, gift?.recipientName))
                        }
                    )
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
                    exchangeRateLabel.setText(R.string.Transaction_exchangeOnDaySent)
                    exchangeRate.text = this@render.exchangeRate.formatFiatForUi(
                        when {
                            exchangeCurrencyCode.isNotBlank() -> exchangeCurrencyCode
                            else -> preferredFiatIso
                        }
                    )
                }
            }

            ifChanged(M::confirmationDate) {
                txDate.text = BRDateUtil.getFullDate((confirmationDate ?: Date()).time)
            }

            ifChanged(M::transactionHash) {
                transactionId.text = transactionHash
            }

            ifChanged(M::confirmedInBlockNumber) {
                confirmedInBlockNumber.text = this@render.confirmedInBlockNumber
            }

            ifChanged(M::confirmations) {
                confirmationsValue.text = confirmations.toString()
            }

            ifChanged(M::transactionState) {
                when (transactionState) {
                    TransactionState.CONFIRMED -> {
                        if (isCompleted) {
                            txStatus.setCompoundDrawablesRelativeWithIntrinsicBounds(
                                R.drawable.checkmark_circled,
                                0,
                                0,
                                0
                            )
                            txStatus.setText(R.string.Transaction_complete)
                        } else {
                            txStatus.setCompoundDrawablesWithIntrinsicBounds(0, 0, 0, 0)
                            txStatus.setText(R.string.Transaction_confirming)
                        }
                    }
                    TransactionState.FAILED -> {
                        txStatus.setCompoundDrawablesWithIntrinsicBounds(0, 0, 0, 0)
                        txStatus.setText(R.string.TransactionDetails_initializedTimestampHeader)
                    }
                    TransactionState.CONFIRMING -> {
                        txStatus.setCompoundDrawablesWithIntrinsicBounds(0, 0, 0, 0)
                        txStatus.setText(R.string.Transaction_confirming)
                    }
                }
            }

            ifChanged(M::destinationTag) {
                if (destinationTag != null) {
                    layoutDestinationTag.isVisible = true
                    destinationTagDivider.isVisible = true
                    if (destinationTag.value.isNullOrEmpty()) {
                        destinationTagValue.setText(R.string.TransactionDetails_destinationTag_EmptyHint)
                    } else {
                        destinationTagValue.text = destinationTag.value
                    }
                }
            }

            ifChanged(M::hederaMemo) {
                val hederaMemo = hederaMemo
                if (hederaMemo != null) {
                    layoutHederaMemo.isVisible = true
                    hederaMemoDivider.isVisible = true
                    if (hederaMemo.value.isNullOrEmpty()) {
                        hederaMemoValue.setText(R.string.TransactionDetails_destinationTag_EmptyHint)
                    } else {
                        hederaMemoValue.text = hederaMemo.value
                    }
                }
            }

            ifChanged(M::gift) { gift ->
                val gone = gift?.keyData.isNullOrBlank() ||
                    gift?.reclaimed == true ||
                    gift?.claimed == true

                layoutGift.isGone = gone
                giftDivider.isGone = gone
            }
        }
    }

    private fun showSentViews(show: Boolean) {
        with(binding) {
            feePrimaryContainer.isVisible = show
            feeSecondaryContainer.isVisible = show
            feePrimaryDivider.isVisible = show
            feeSecondaryDivider.isVisible = show
        }
        showEthViews(show)
    }

    private fun showEthViews(show: Boolean) {
        with(binding) {
            gasPriceContainer.isVisible = show
            gasLimitContainer.isVisible = show
            gasPriceDivider.isVisible = show
            gasLimitDivider.isVisible = show
        }
    }

    private fun showTotalCost(show: Boolean) {
        with(binding) {
            feeSecondaryContainer.isVisible = show
            feeSecondaryDivider.isVisible = show
        }
    }

    private fun showConfirmedView(show: Boolean) {
        with(binding) {
            confirmedContainer.isVisible = show
            confirmedDivider.isVisible = show
            confirmationsDivider.isVisible = show
            confirmationsContainer.isVisible = show
        }
    }

    private fun copyToClipboard(text: String) {
        BRClipboardManager.putClipboard(text)
        toastLong(R.string.Receive_copied)
    }
}
