package com.breadwallet.ui.atm

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.View
import androidx.core.os.bundleOf
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
import kotlinx.android.synthetic.main.controller_receive.*
import kotlinx.android.synthetic.main.fragment_request_cash_out_status.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.withContext
import org.kodein.di.erased.instance

class CashOutStatusController(
    args: Bundle
) : BaseController(args) {

    constructor(status: CashStatus) : this(
        bundleOf(cashStatus to status)
    )

    companion object {
        private const val cashStatus = "CashOutStatusController.Status"
    }

    override val layoutId = R.layout.fragment_request_cash_out_status
    private val cryptoUriParser by instance<CryptoUriParser>()

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreateView(view: View) {
        super.onCreateView(view)

        handlePlatformMessages().launchIn(viewCreatedScope)

        val status:CashStatus = arg(cashStatus)

        if (CodeStatus.resolve(status.status) == CodeStatus.NEW_CODE) {
            awaitingCard.visibility = View.VISIBLE
            fundedCard.visibility = View.GONE

            val request = CryptoRequest.Builder()
                .setAddress(status.address)
                .setAmount(status.btc_amount.toFloat().toBigDecimal())
                .build()
            val uri = cryptoUriParser.createUrl("BTC", request)
            if (!QRUtils.generateQR(activity, uri.toString(), qr_image)) {
                error("failed to generate qr image for address")
            }

        } else if (CodeStatus.resolve(status.status) == CodeStatus.FUNDED) {
            awaitingCard.visibility = View.GONE
            fundedCard.visibility = View.VISIBLE
        }
    }

    private fun handlePlatformMessages() = PlatformTransactionBus.requests().onEach {
        withContext(Dispatchers.Main) {
            val transaction = RouterTransaction.with(PlatformConfirmTransactionController(it))
            router.pushController(transaction)
        }
    }
}
