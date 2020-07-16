package com.breadwallet.ui.atm

import android.R.attr.label
import android.annotation.SuppressLint
import android.content.ClipData
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.core.os.bundleOf
import cash.just.sdk.CashSDK
import cash.just.sdk.model.CashCodeStatusResponse
import cash.just.sdk.model.CashStatus
import cash.just.sdk.model.CodeStatus
import com.bluelinelabs.conductor.RouterTransaction
import com.breadwallet.R
import com.breadwallet.legacy.presenter.entities.CryptoRequest
import com.breadwallet.legacy.wallet.wallets.bitcoin.WalletBitcoinManager
import com.breadwallet.tools.qrcode.QRUtils
import com.breadwallet.ui.BaseController
import com.breadwallet.ui.atm.model.RetryableCashStatus
import com.breadwallet.ui.changehandlers.BottomSheetChangeHandler
import com.breadwallet.ui.platform.PlatformConfirmTransactionController
import com.breadwallet.ui.send.SendSheetController
import com.breadwallet.util.CryptoUriParser
import com.platform.PlatformTransactionBus
import kotlinx.android.synthetic.main.controller_receive.qr_image
import kotlinx.android.synthetic.main.fragment_request_cash_out_status.*
import kotlinx.android.synthetic.main.request_status_awaiting.*
import kotlinx.android.synthetic.main.request_status_funded.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.withContext
import org.kodein.di.erased.instance
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.net.HttpURLConnection.HTTP_OK
import java.util.Locale

class CashOutStatusController(args: Bundle) : BaseController(args) {

    constructor(status: RetryableCashStatus) : this(
        bundleOf(cashStatus to status)
    )

    constructor(secureCode: String) : this(
        bundleOf(secureCode to secureCode)
    )

    companion object {
        private const val cashStatus = "CashOutStatusController.Status"
        private const val secureCode = "CashOutStatusController.SecureCode"
        private const val clipboardLabel = "coinsquare_wallet"
    }

    override val layoutId = R.layout.fragment_request_cash_out_status

    private val cryptoUriParser by instance<CryptoUriParser>()
    private lateinit var clipboard: android.content.ClipboardManager
    private lateinit var safeCode: String

    enum class ViewState {
        LOADING,
        AWAITING,
        FUNDED
    }

    init {
        overridePushHandler(BottomSheetChangeHandler())
        overridePopHandler(BottomSheetChangeHandler())
    }

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreateView(view: View) {
        super.onCreateView(view)
        changeUiState(ViewState.LOADING)

        val context = view.context

        clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager

        handlePlatformMessages().launchIn(viewCreatedScope)

        val retryableCashStatus: RetryableCashStatus? = argOptional(cashStatus)
        val code:String? = argOptional(secureCode)

        retryableCashStatus?.let {
            safeCode = it.secureCode
            val cashStatus = it.cashStatus
            if (CodeStatus.resolve(cashStatus.status) == CodeStatus.NEW_CODE) {
                populateAwaitingView(context, cashStatus)
            } else if (CodeStatus.resolve(it.cashStatus.status) == CodeStatus.FUNDED) {
                refreshCodeStatus(safeCode, context)
            }
        } ?: run {
            safeCode = code ?: throw IllegalArgumentException("Missing arguments $cashStatus and $secureCode")

            refreshCodeStatus(code, context)
        }
    }

    private fun refreshCodeStatus(code:String, context:Context) {
        CashSDK.checkCashCodeStatus(code).enqueue(object: Callback<CashCodeStatusResponse> {
            override fun onResponse(call: Call<CashCodeStatusResponse>,
                response: Response<CashCodeStatusResponse>) {
                if (response.isSuccessful && response.code() == HTTP_OK) {

                    response.body()?.let { it ->
                        val cashStatus = it.data!!.items[0]

                        if (CodeStatus.resolve(cashStatus.status) == CodeStatus.NEW_CODE) {
                            populateAwaitingView(context, cashStatus)
                        } else if (CodeStatus.resolve(cashStatus.status) == CodeStatus.FUNDED) {
                            populateFundedView(context, cashStatus)
                        }
                    }
                }
            }

            override fun onFailure(call: Call<CashCodeStatusResponse>, t: Throwable) {
                Toast.makeText(context.applicationContext,
                    "Failed to load $safeCode status", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun changeUiState(state: ViewState){
        when (state) {
            ViewState.LOADING -> {
                loadingView.visibility = View.VISIBLE
                fundedCard.visibility = View.GONE
                awaitingCard.visibility = View.GONE
            }
            ViewState.AWAITING -> {
                loadingView.visibility = View.GONE
                fundedCard.visibility = View.GONE
                awaitingCard.visibility = View.VISIBLE
            }
            ViewState.FUNDED -> {
                loadingView.visibility = View.GONE
                fundedCard.visibility = View.VISIBLE
                awaitingCard.visibility = View.GONE
            }
        }
    }
    private fun handlePlatformMessages() = PlatformTransactionBus.requests().onEach {
        withContext(Dispatchers.Main) {
            val transaction = RouterTransaction.with(PlatformConfirmTransactionController(it))
            router.pushController(transaction)
        }
    }

    private fun populateAwaitingView(context:Context, cashStatus: CashStatus) {

        changeUiState(ViewState.AWAITING)

        sendAction.setOnClickListener {
            goToSend(cashStatus.btc_amount, cashStatus.address)
        }

        refreshAction.setOnClickListener {
            changeUiState(ViewState.LOADING)
            refreshCodeStatus(safeCode, it.context)
        }

        awaitingAddress.text = cashStatus.address
        awaitingAddress.isSelected = true
        awaitingAddress.setOnClickListener {
            copyToClipboard(context, cashStatus.address)
        }
        awaitingBTCAmount.text = "Amount: ${cashStatus.btc_amount} BTC"
        awaitingBTCAmount.setOnClickListener {
            copyToClipboard(context, cashStatus.btc_amount)
        }

        awaitingLocationAddress.text = "Location: ${cashStatus.description}"

        awaitingLocationAddress.setOnClickListener {
            openMaps(context, cashStatus)
        }

        awaitingUSDAmount.text = "Amount (USD): $${cashStatus.usdAmount}"

        qr_image.setOnClickListener {
            copyToClipboard(context, cashStatus.address)
        }

        val request = CryptoRequest.Builder()
            .setAddress(cashStatus.address)
            .setAmount(cashStatus.btc_amount.toFloat().toBigDecimal())
            .build()

        val uri = cryptoUriParser.createUrl("BTC", request)

        if (!QRUtils.generateQR(activity, uri.toString(), qr_image)) {
            error("failed to generate qr image for address")
        }
    }

    private fun goToSend(btc:String, address:String) {
        val builder = CryptoRequest.Builder()
        builder.address = address
        builder.amount = btc.toFloat().toBigDecimal()
        builder.currencyCode = WalletBitcoinManager.BITCOIN_CURRENCY_CODE
        val request = builder.build()
        router.pushController(RouterTransaction.with(SendSheetController(request)))
    }

    private fun populateFundedView(context: Context, cashStatus: CashStatus){
        changeUiState(ViewState.FUNDED)

        cashCode.text = cashStatus.code!!
        cashCode.setOnClickListener {
            copyToClipboard(context, cashStatus.code!!)
        }
        amountFunded.text = "Amount (USD):  \$${cashStatus.usdAmount}"
        locationFunded.text = "Location: ${cashStatus.description}"
        locationFunded.setOnClickListener {
            openMaps(context, cashStatus)
        }
    }

    private fun copyToClipboard(context:Context, data: String){
        val clip = ClipData.newPlainText(clipboardLabel, data)
        clipboard.setPrimaryClip(clip)
        Toast.makeText(context, "Copied to the clipboard!", Toast.LENGTH_SHORT).show()
    }

    private fun openMaps(context:Context, cashStatus: CashStatus) {
        val uri: String = java.lang.String.format(
            Locale.ENGLISH, "geo:%f,%f?z=%d&q=%f,%f (%s)",
            cashStatus.latitude, cashStatus.longitude, 15, cashStatus.latitude, cashStatus.longitude, cashStatus.description
        )
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(uri))
        context.startActivity(intent)
    }
}
