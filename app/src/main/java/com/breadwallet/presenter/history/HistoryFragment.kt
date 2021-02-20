package com.breadwallet.presenter.history

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.breadwallet.R
import com.breadwallet.presenter.activities.BreadActivity
import com.breadwallet.presenter.base.BaseFragment
import com.breadwallet.tools.manager.BRSharedPrefs
import com.breadwallet.tools.manager.BRSharedPrefs.OnIsoChangedListener
import com.breadwallet.tools.manager.TxManager
import com.breadwallet.tools.sqlite.TransactionDataSource.OnTxAddedListener
import com.breadwallet.tools.threads.BRExecutor
import com.breadwallet.wallet.BRPeerManager
import com.breadwallet.wallet.BRPeerManager.OnTxStatusUpdate
import com.breadwallet.wallet.BRWalletManager
import com.breadwallet.wallet.BRWalletManager.OnBalanceChanged
import kotlinx.android.synthetic.main.fragment_history.*

/** Litewallet
 * Created by Mohamed Barry on 6/1/20
 * email: mosadialiou@gmail.com
 * Copyright © 2020 Litecoin Foundation. All rights reserved.
 */
class HistoryFragment :
    BaseFragment<HistoryPresenter>(),
    OnBalanceChanged,
    OnTxStatusUpdate,
    OnIsoChangedListener,
    OnTxAddedListener,
    HistoryView {

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_history, container, false)
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        TxManager.getInstance().init(requireActivity() as BreadActivity, recyclerView)
    }

    private fun addObservers() {
        BRWalletManager.getInstance().addBalanceChangedListener(this)
        BRPeerManager.getInstance().addStatusUpdateListener(this)
        BRSharedPrefs.addIsoChangedListener(this)
    }

    private fun removeObservers() {
        BRWalletManager.getInstance().removeListener(this)
        BRPeerManager.getInstance().removeListener(this)
        BRSharedPrefs.removeListener(this)
    }

    override fun onResume() {
        super.onResume()
        addObservers()
        TxManager.getInstance().onResume(requireActivity() as BreadActivity)
    }

    override fun onPause() {
        super.onPause()
        removeObservers()
    }

    override fun onBalanceChanged(balance: Long) {
        updateUI()
    }

    override fun onStatusUpdate() {
        BRExecutor.getInstance().forBackgroundTasks().execute {
            TxManager.getInstance().updateTxList(requireActivity() as BreadActivity)
        }
    }

    override fun onIsoChanged(iso: String) {
        updateUI()
    }

    override fun onTxAdded() {
        BRExecutor.getInstance().forBackgroundTasks().execute {
            TxManager.getInstance().updateTxList(requireActivity() as BreadActivity)
        }
    }

    private fun updateUI() {
        BRExecutor.getInstance().forLightWeightBackgroundTasks().execute {
            Thread.currentThread().name = Thread.currentThread().name + "HistoryFragment:updateUI"
            TxManager.getInstance().updateTxList(requireActivity() as BreadActivity)
        }
    }

    override fun initPresenter() = HistoryPresenter(this)
}
