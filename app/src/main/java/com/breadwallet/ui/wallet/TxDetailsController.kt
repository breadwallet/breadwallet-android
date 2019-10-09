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

import android.animation.LayoutTransition
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.widget.RelativeLayout
import androidx.core.os.bundleOf
import androidx.core.view.isVisible
import com.breadwallet.R
import com.breadwallet.app.BreadApp
import com.breadwallet.breadbox.formatCryptoForUi
import com.breadwallet.breadbox.formatFiatForUi
import com.breadwallet.breadbox.isErc20
import com.breadwallet.breadbox.toBigDecimal
import com.breadwallet.breadbox.toSanitizedString
import com.breadwallet.crypto.Transfer
import com.breadwallet.crypto.TransferDirection
import com.breadwallet.crypto.TransferState
import com.breadwallet.legacy.presenter.entities.CurrencyEntity
import com.breadwallet.tools.manager.BRClipboardManager
import com.breadwallet.tools.manager.BRSharedPrefs
import com.breadwallet.tools.util.BRDateUtil
import com.breadwallet.ui.BaseController
import com.breadwallet.ui.changehandlers.DialogChangeHandler
import com.platform.entities.TxMetaData
import com.platform.interfaces.AccountMetaDataProvider
import kotlinx.android.synthetic.main.transaction_details.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancelChildren
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.withContext
import org.kodein.di.erased.instance
import java.math.BigDecimal
import java.text.NumberFormat
import java.util.Date

/**
 * TODO: Updated to support new APIs and improve general UX.
 *   Remaining Issues:
 *     - Convert to mobius
 *     - Expose formatted currencies in Model
 *     - Improve gas price handling
 *     - Support exchange rate data and fiat first display
 *     - Support memo when KVStore functions
 *     - Validate state displays (handle deleted state?)
 */
@UseExperimental(ExperimentalCoroutinesApi::class)
class TxDetailsController(
    args: Bundle? = null
) : BaseController(args), CoroutineScope {

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
    private val transferHash = arg<String>(TRANSFER_HASH)

    init {
        overridePopHandler(DialogChangeHandler())
        overridePushHandler(DialogChangeHandler())
    }

    override val coroutineContext = SupervisorJob() + Dispatchers.Default
    override val layoutId = R.layout.transaction_details

    private var mTxMetaData: TxMetaData? = null

    private val metaDataProvider: AccountMetaDataProvider by instance()

    /** The gas unit for Ethereum transactions. */
    private val weiUnit by lazy {
        // TODO: Cleanup Unit lookup. Can we get gwei from core?
        BreadApp.getBreadBox()
            .getSystemUnsafe()!!
            .networks
            .first { it.currency.code == "eth" }
            .run { unitsFor(currency).get() }
            .first { it.symbol == "wei" }
    }

    override fun onCreateView(view: View) {
        super.onCreateView(view)
        // NOTE: This allows animateLayoutChanges to properly animate details show/hide
        (view as ViewGroup).layoutTransition.enableTransitionType(LayoutTransition.CHANGING)

        view.setOnClickListener { router.popCurrentController() }
        close_button.setOnClickListener { router.popCurrentController() }

        // Allow the transaction id to be copy-able
        transaction_id.setOnClickListener {
            BRClipboardManager.putClipboard(applicationContext, transaction_id.text?.toString())
            toastLong(R.string.Receive_copied)
        }

        show_hide_details.setOnClickListener {
            val newVisibility = !details_container.isVisible
            details_container.isVisible = newVisibility
            show_hide_details.setText(
                when {
                    newVisibility -> R.string.TransactionDetails_titleFailed
                    else -> R.string.TransactionDetails_showDetails
                }
            )
        }

        memo_input.imeOptions = EditorInfo.IME_ACTION_DONE
        val color = tx_to_from_address.textColors.defaultColor
        memo_input.setTextColor(color)
    }

    override fun onAttach(view: View) {
        super.onAttach(view)
        observeTransfer(currencyCode, transferHash)
    }

    override fun onDetach(view: View) {
        super.onDetach(view)
        coroutineContext.cancelChildren()
        val activity = checkNotNull(activity)

        // TODO: Cleanup when memo is functional
        // Update the memo field on the transaction and save it
        val txHash = transferHash.toByteArray()
        metaDataProvider.getTxMetaData(txHash)?.let { txMetaData ->
            val memo = memo_input.text.toString()
            if (memo.isNotEmpty()) {
                txMetaData.comment = memo
                metaDataProvider.putTxMetaData(txMetaData, txHash)
            }
        }

        // Hide softkeyboard if it's visible
        val imm = view.context.getSystemService(InputMethodManager::class.java)
        imm.hideSoftInputFromWindow(memo_input.windowToken, 0)
    }

    private fun observeTransfer(currencyCode: String, transferHash: String) {
        BreadApp.getBreadBox()
            .walletTransfer(currencyCode, transferHash)
            .onEach { transfer ->
                withContext(Dispatchers.Main) {
                    updateUi(transfer)
                }
            }
            .launchIn(this)
    }

    @Suppress("LongMethod", "ComplexMethod")
    private fun updateUi(transfer: Transfer) {
        val app = checkNotNull(activity)
        val resources = checkNotNull(resources)

        val currencyCode = transfer.wallet.currency.code
        val transferred = transfer.amount.toBigDecimal()
        val fee = transfer.fee.toBigDecimal()
        val transactionTotal = transferred + fee

        //user prefers crypto (or fiat)
        val isCryptoPreferred = BRSharedPrefs.isCryptoPreferred(app)
        val fiatIso = BRSharedPrefs.getPreferredFiatIso()

        val iso = if (isCryptoPreferred) currencyCode else fiatIso

        val received = transfer.direction == TransferDirection.RECEIVED

        val amountWhenSent: String
        val amountNow: String
        val exchangeRateFormatted: String

        if (received) {
            hideSentViews()
        } else {
            //var rawFee = transaction.fee
            if (transfer.amount.currency.run { code == "eth" && !isErc20() }) {
                val formatter = NumberFormat.getIntegerInstance().apply {
                    maximumFractionDigits = 0
                    isGroupingUsed = false
                }
                val feeBasis = transfer.run {
                    confirmedFeeBasis.orNull() ?: estimatedFeeBasis.get()
                }

                val gas = feeBasis.pricePerCostFactor
                    .convert(weiUnit)
                    .get()
                    .toBigDecimal()

                // TODO: Can we get gwei Unit from core?
                gas_price.text = "%s %s".format(gas / BigDecimal("1000000000"), "gwei")
                gas_limit.text = formatter.format(feeBasis.costFactor).toString()
            } else {
                hideEthViews()
            }

            fee_secondary.text = transactionTotal.formatCryptoForUi(currencyCode)
            fee_primary.text = fee.formatCryptoForUi(currencyCode)
            fee_primary_label.text = resources.getString(R.string.Send_fee, "")

            //erc20s
            if (transfer.amount.currency.isErc20()) {
                hideTotalCost()
                fee_primary.text =
                    String.format(
                        "%s %s",
                        transfer.fee.toBigDecimal().stripTrailingZeros().toPlainString(),
                        "gwei"
                    )
            }
        }

        val blockNumber = transfer.confirmation.orNull()?.blockNumber?.toInt() ?: 0
        if (blockNumber == 0) {
            hideConfirmedView()
        }

        when (transfer.state.type!!) {
            TransferState.Type.INCLUDED -> {
                tx_status.setText(R.string.Transaction_complete)
            }
            TransferState.Type.FAILED -> {
                tx_status.setCompoundDrawablesWithIntrinsicBounds(0, 0, 0, 0)
                tx_status.setText(R.string.TransactionDetails_initializedTimestampHeader)
            }
            TransferState.Type.CREATED,
            TransferState.Type.SIGNED,
            TransferState.Type.SUBMITTED,
            TransferState.Type.PENDING -> {
                tx_status.setText(R.string.Transaction_confirming)
            }
            TransferState.Type.DELETED -> Unit
        }

        val fiatAmountNow = BigDecimal.ZERO// TODO: get Fiat now

        val fiatAmountWhenSent: BigDecimal
        val metaData = metaDataProvider.getTxMetaData(transferHash.toByteArray())
        val hasData = metaData?.run {
            exchangeRate == 0.0 || exchangeCurrency.isNullOrBlank()
        } ?: false

        if (metaData != null && hasData) {
            val ent = CurrencyEntity(
                metaData.exchangeCurrency,
                null,
                metaData.exchangeRate.toFloat(),
                transfer.wallet.currency.code
            )
            fiatAmountWhenSent = BigDecimal.ZERO// TODO: get fiat when sent
            //always fiat amount
            amountWhenSent = fiatAmountWhenSent.formatFiatForUi(ent.code)
        } else {
            fiatAmountWhenSent = BigDecimal.ZERO
            //always fiat amount
            amountWhenSent = fiatAmountWhenSent.formatFiatForUi(fiatIso)
        }

        //always fiat amount
        amountNow = fiatAmountNow.formatFiatForUi(fiatIso)

        amount_when_sent.text = amountWhenSent
        amount_now.text = amountNow

        // If 'amount when sent' is 0 or unavailable, show fiat tx amount on its own
        if (fiatAmountWhenSent.compareTo(BigDecimal.ZERO) == 0) {
            amount_when_sent.visibility = View.INVISIBLE
            label_when_sent.visibility = View.INVISIBLE
            label_now.visibility = View.INVISIBLE

            val params = RelativeLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
            params.addRule(RelativeLayout.CENTER_HORIZONTAL)
            params.addRule(RelativeLayout.BELOW, tx_amount.id)
            amount_now.layoutParams = params
        }

        when {
            received -> {
                tx_action.setText(R.string.TransactionDetails_titleReceived)
                tx_to_from.setText(R.string.TransactionDetails_addressViaHeader)
            }
            else -> {
                tx_action.setText(R.string.TransactionDetails_titleSent)
                tx_to_from.setText(R.string.TransactionDetails_addressToHeader)
            }
        }

        tx_to_from_address.text = when {
            received -> transfer.source
            else -> transfer.target
        }.orNull()?.toSanitizedString()
            ?: "<unknown>" // TODO: Do we need a string res for no hash text?

        // Allow the to/from address to be copyable
        tx_to_from_address.setOnClickListener {
            val address = tx_to_from_address.text.toString()
            BRClipboardManager.putClipboard(applicationContext, address)
            toastLong(R.string.Receive_copied)
        }

        //this is always crypto amount
        tx_amount.text = transferred.formatCryptoForUi(
            transfer.wallet.currency.code,
            negate = !received
        )

        if (received) {
            tx_amount.setTextColor(resources.getColor(R.color.transaction_amount_received_color))
        }

        // Set the memo text if one is available
        mTxMetaData = metaDataProvider.getTxMetaData(transferHash.toByteArray())
        mTxMetaData?.let { txMetaData ->
            memo_input.setText(txMetaData.comment ?: "")

            val exchangeCurrency = txMetaData.exchangeCurrency
            val metaIso = when {
                exchangeCurrency.isNullOrBlank() -> BRSharedPrefs.getPreferredFiatIso()
                else -> exchangeCurrency
            }
            exchange_rate.text = txMetaData.exchangeRate.toBigDecimal().formatFiatForUi(metaIso)
        }
        /* TODO: Token support
            if (tkn != null) { // it's a token transfer ETH tx
            memo_input.setText(
                String.format(
                    app.getString(R.string.Transaction_tokenTransfer),
                    tkn.symbol
                )
            )
            memo_input.isFocusable = false
        }*/

        // timestamp is 0 if it's not confirmed in a block yet so make it now
        val confirmationDate = transfer
            .confirmation
            .transform { it?.confirmationTime }
            .or { Date() }

        tx_date.text = BRDateUtil.getFullDate(confirmationDate.time)

        // Set the transaction id
        transaction_id.text = transfer.hash.orNull()?.toString() ?: ""

        // Set the transaction block number
        transfer.confirmation
            .transform { it?.blockNumber?.toString() }
            .orNull()
            ?.let { confirmed_in_block_number.text = it }
    }

    private fun hideSentViews() {
        details_container.apply {
            removeView(fee_primary_container)
            removeView(fee_secondary_container)
            removeView(fee_primary_divider)
            removeView(fee_secondary_divider)
        }
        hideEthViews()
    }

    private fun hideEthViews() {
        details_container.apply {
            removeView(gas_price_container)
            removeView(gas_limit_container)
            removeView(gas_price_divider)
            removeView(gas_limit_divider)
        }
    }

    private fun hideTotalCost() {
        details_container.removeView(fee_secondary_container)
        details_container.removeView(fee_secondary_divider)
    }

    private fun hideConfirmedView() {
        details_container.removeView(confirmed_container)
        details_container.removeView(confirmed_divider)
    }
}
