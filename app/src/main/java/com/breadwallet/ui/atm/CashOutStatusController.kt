package com.breadwallet.ui.atm

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.core.os.bundleOf
import cash.just.wac.WacSDK
import cash.just.wac.model.CashCodeStatusResponse
import cash.just.wac.model.CashStatus
import cash.just.wac.model.CodeStatus
import com.bluelinelabs.conductor.RouterTransaction
import com.breadwallet.R
import com.breadwallet.legacy.presenter.entities.CryptoRequest
import com.breadwallet.tools.qrcode.QRUtils
import com.breadwallet.ui.BaseController
import com.breadwallet.ui.platform.PlatformConfirmTransactionController
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

class CashOutStatusController(args: Bundle) : BaseController(args) {

    constructor(status: CashStatus) : this(
        bundleOf(cashStatus to status)
    )

    constructor(code: String) : this(
        bundleOf(secureCode to code)
    )

    companion object {
        private const val cashStatus = "CashOutStatusController.Status"
        private const val secureCode = "CashOutStatusController.SecureCode"
    }

    override val layoutId = R.layout.fragment_request_cash_out_status
    private val cryptoUriParser by instance<CryptoUriParser>()

    enum class ViewState {
        LOADING,
        AWAITING,
        FUNDED
    }

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreateView(view: View) {
        super.onCreateView(view)
        changeUiState(ViewState.LOADING)

        handlePlatformMessages().launchIn(viewCreatedScope)

        val status:CashStatus? = argOptional(cashStatus)
        val code:String? = argOptional(secureCode)

        status?.let {
            if (CodeStatus.resolve(it.status) == CodeStatus.NEW_CODE) {
                populateAwaitingView(it.address, it.description, it.usdAmount, it.btc_amount)
            } else if (CodeStatus.resolve(it.status) == CodeStatus.FUNDED) {
                populateFundedView(view.context, it.code!!, it.usdAmount, it.description)
            }
        } ?:run {
            val safeCode = code ?: throw IllegalArgumentException("Missing arguments $cashStatus and $secureCode")
            WacSDK.checkCashCodeStatus(safeCode).enqueue(object: Callback<CashCodeStatusResponse> {
                override fun onResponse(call: Call<CashCodeStatusResponse>,
                    response: Response<CashCodeStatusResponse>) {
                    if (response.isSuccessful && response.code() == 200) {

                        response.body()?.let { it ->
                            val cashStatus = it.data!!.items[0]

                            if (CodeStatus.resolve(cashStatus.status) == CodeStatus.NEW_CODE) {
                                populateAwaitingView(cashStatus.address, cashStatus.description,
                                    cashStatus.usdAmount, cashStatus.btc_amount)
                            } else if (CodeStatus.resolve(cashStatus.status) == CodeStatus.FUNDED) {
                                populateFundedView(view.context, cashStatus.code!!,
                                    cashStatus.usdAmount, cashStatus.description)
                            }
                        }
                    }
                }

                override fun onFailure(call: Call<CashCodeStatusResponse>, t: Throwable) {
                    Toast.makeText(applicationContext, "Failed to load $safeCode status", Toast.LENGTH_SHORT).show()
                }
            })
        }
    }

    private fun changeUiState(state: ViewState){
        when(state) {
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
                fundedCard.visibility = View.GONE
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

    private fun populateAwaitingView(address:String, details:String, usdAmount:String, btcAmount:String) {
        changeUiState(ViewState.AWAITING)
        awaitingAddress.text = address
        awaitingAddress.isSelected = true

        awaitingBTCAmount.text = "Amount: $btcAmount BTC"
        awaitingLocationAddress.text = "Location: $details"
        awaitingUSDAmount.text = "Amount (USD): $$usdAmount"

        val request = CryptoRequest.Builder()
            .setAddress(address)
            .setAmount(btcAmount.toFloat().toBigDecimal())
            .build()

        val uri = cryptoUriParser.createUrl("BTC", request)

        if (!QRUtils.generateQR(activity, uri.toString(), qr_image)) {
            error("failed to generate qr image for address")
        }
    }

    private fun populateFundedView(context: Context, code:String, usdAmount:String, address:String){
        changeUiState(ViewState.FUNDED)

        cashCode.text = code
        amountFunded.text = "Amount (USD):  \$$usdAmount"
        locationFunded.text = "Location: $address"
        locationFunded.setOnClickListener {
            val geoUri = "http://maps.google.com/maps?q=loc:$address"
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(geoUri))
            context.startActivity(intent)
        }
    }
}
