package com.breadwallet.ui.atm

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.View
import androidx.core.os.bundleOf
import cash.just.wac.model.CashStatus
import com.bluelinelabs.conductor.RouterTransaction
import com.breadwallet.R
import com.breadwallet.ui.BaseController
import com.breadwallet.ui.platform.PlatformConfirmTransactionController
import com.platform.PlatformTransactionBus
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.withContext

class StatusListController(
    args: Bundle
) : BaseController(args) {

    constructor(status: CashStatus) : this(
        bundleOf(cashStatus to status)
    )

    companion object {
        private const val cashStatus = "CashOutStatusController.Status"
    }

    override val layoutId = R.layout.fragment_request_list

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreateView(view: View) {
        super.onCreateView(view)

        handlePlatformMessages().launchIn(viewCreatedScope)
    }

    private fun handlePlatformMessages() = PlatformTransactionBus.requests().onEach {
        withContext(Dispatchers.Main) {
            val transaction = RouterTransaction.with(PlatformConfirmTransactionController(it))
            router.pushController(transaction)
        }
    }
}
