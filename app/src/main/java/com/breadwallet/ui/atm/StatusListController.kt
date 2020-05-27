package com.breadwallet.ui.atm

import android.annotation.SuppressLint
import android.content.Context
import android.os.Bundle
import android.view.View
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.ContextCompat
import cash.just.wac.Wac
import cash.just.wac.WacSDK
import cash.just.wac.model.CashCodeStatusResponse
import cash.just.wac.model.CashStatus
import cash.just.wac.model.CodeStatus
import com.bluelinelabs.conductor.RouterTransaction
import com.breadwallet.R
import com.breadwallet.ui.BaseController
import com.breadwallet.ui.formatTo
import com.breadwallet.ui.platform.PlatformConfirmTransactionController
import com.breadwallet.ui.toDate
import com.platform.PlatformTransactionBus
import kotlinx.android.synthetic.main.fragment_request_list.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.withContext
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class StatusListController(args: Bundle) : BaseController(args) {

    override val layoutId = R.layout.fragment_request_list
    var statusList = ArrayList<CashStatus>()
    var size = 0
    companion object {
        const val SEVER_TIME_FORMAT = "yyyy-MM-dd'T'HH:mm:ss'Z'"
        const val DISPLAY_TIME_FORMAT = "dd MMM, hh:mm"
        const val HTTP_OK = 200
    }
    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreateView(view: View) {
        super.onCreateView(view)
        no_requests.visibility = View.GONE
        size = 0
        val context = view.context
        if (!WacSDK.isSessionCreated()) {
            WacSDK.createSession(BitcoinServer.getServer(), object: Wac.SessionCallback {
                override fun onSessionCreated(sessionKey: String) {
                    proceed(context)
                }

                override fun onError(errorMessage: String?) {
                    Toast.makeText(applicationContext, errorMessage, Toast.LENGTH_SHORT).show()
                }
            })
        } else {
            proceed(context)
        }

        handlePlatformMessages().launchIn(viewCreatedScope)
    }

    private fun proceed(context: Context) {
        statusList = ArrayList()
        val requests = AtmSharedPreferencesManager.getWithdrawalRequests(context)
        requests?.let {
            size = it.size
            it.forEach { string ->
                loadRequest(context, string)
            }
        }

        if(requests == null || requests.isEmpty()) {
            no_requests.visibility = View.VISIBLE
        }
    }

    private fun loadRequest(context: Context, secureCode:String) {
        WacSDK.checkCashCodeStatus(secureCode).enqueue(object: Callback<CashCodeStatusResponse> {
            override fun onResponse(call: Call<CashCodeStatusResponse>, response: Response<CashCodeStatusResponse>) {
                if (response.isSuccessful && response.code() == HTTP_OK) {
                    statusList.add(response.body()?.data!!.items[0])
                }
                if (statusList.size == size) {
                    createStatusRows(context)
                }
            }

            override fun onFailure(call: Call<CashCodeStatusResponse>, t: Throwable) {
                Toast.makeText(applicationContext, "Failed to load $secureCode status", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun createStatusRows(context:Context){
        statusList.sortBy {
            it.expiration
        }
        statusList.forEach { status ->
            if (requestGroup != null) {
                val view = View.inflate(context, R.layout.item_list_cash_out_request, null)
                populateCashCodeStatus(view, status)
                requestGroup.addView(view)
            }
        }
    }

    private fun populateCashCodeStatus(view:View, response: CashStatus) {
        view.findViewById<TextView>(R.id.date).text =
            response.expiration.toDate(SEVER_TIME_FORMAT).formatTo(DISPLAY_TIME_FORMAT)
        view.findViewById<TextView>(R.id.addressLocation).text = response.description
        val status = CodeStatus.resolve(response.status)
        val stateView = view.findViewById<TextView>(R.id.stateMessage)
        when (status) {
            CodeStatus.NEW_CODE -> {
                stateView.text = "Awaiting funds"
                stateView.setOnClickListener {
                    router.pushController(RouterTransaction.with(CashOutStatusController(response)))
                }
                val drawable = ContextCompat.getDrawable(view.context, R.drawable.ic_eye)
                stateView.setCompoundDrawablesWithIntrinsicBounds(null, null, drawable, null)
            }
            CodeStatus.FUNDED -> {
                stateView.text = "Funded"
            }
            else -> {
                val state = CodeStatus.resolve(response.status).toString().toLowerCase()
                val capitalizeFirst = state.substring(0, 1).toUpperCase() + state.substring(1)
                stateView.text = capitalizeFirst
            }
        }
    }

    private fun handlePlatformMessages() = PlatformTransactionBus.requests().onEach {
        withContext(Dispatchers.Main) {
            val transaction = RouterTransaction.with(PlatformConfirmTransactionController(it))
            router.pushController(transaction)
        }
    }
}
