package com.breadwallet.ui.importwallet

import android.os.Bundle
import android.view.View
import androidx.core.os.bundleOf
import com.bluelinelabs.conductor.RouterTransaction
import com.breadwallet.R
import com.breadwallet.tools.util.Link
import com.breadwallet.ui.BaseController
import com.breadwallet.ui.scanner.ScannerController
import kotlinx.android.synthetic.main.controller_import_wallet.*

private const val PRIVATE_KEY = "private_key"

class ImportWalletController(
    args: Bundle? = null
) : BaseController(args),
    ScannerController.Listener {

    constructor(privateKey: String) : this(
        bundleOf(
            PRIVATE_KEY to privateKey
        )
    )

    override val layoutId = R.layout.controller_import_wallet

    override fun onAttach(view: View) {
        super.onAttach(view)
        close_button.setOnClickListener {
            router.popController(this)
        }
        faq_button.setOnClickListener {
            // val wm = WalletsMaster.getInstance().getCurrentWallet(this@ImportActivity)
            // TODO: UiUtils.showSupportFragment(, BRConstants.FAQ_IMPORT_WALLET, wm)
        }

        scan_button.setOnClickListener {
            router.pushController(
                RouterTransaction.with(ScannerController())
            )
        }

        argOptional<String>(PRIVATE_KEY)?.run(this::sweepWallet)
    }

    override fun onLinkScanned(link: Link) {
        when (link) {
            is Link.ImportWallet -> sweepWallet(link.privateKey)
        }
    }

    private fun sweepWallet(privateKey: String) {
        // TODO: ImportPrivKeyTask.trySweepWallet()
    }
}
