package com.breadwallet.presenter.transfer

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.breadwallet.R
import com.breadwallet.presenter.activities.BreadActivity
import com.breadwallet.presenter.base.BaseFragment
import kotlinx.android.synthetic.main.fragment_transfer.*

/** Litewallet
 * Created by Mohamed Barry on 6/14/20
 * email: mosadialiou@gmail.com
 * Copyright © 2020 Litecoin Foundation. All rights reserved.
 */
class TransferFragment : BaseFragment<TransferPresenter>(), TransferView {

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_transfer, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        logoutBut.setOnClickListener {
            presenter.logout()
            (requireActivity() as BreadActivity?)?.recreate()
        }
    }

    override fun initPresenter() = TransferPresenter(this)
}
