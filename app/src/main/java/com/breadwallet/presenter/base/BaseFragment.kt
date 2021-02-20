package com.breadwallet.presenter.base

import android.os.Bundle
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import timber.log.Timber

/** Litewallet
 * Created by Mohamed Barry on 6/1/20
 * email: mosadialiou@gmail.com
 * Copyright © 2020 Litecoin Foundation. All rights reserved.
 */
abstract class BaseFragment<P : BasePresenter<BaseView>> : Fragment() {
    lateinit var presenter: P

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        presenter = initPresenter()
    }

    override fun onDetach() {
        super.onDetach()
        if (this::presenter.isInitialized) {
            presenter.detach()
        } else {
            Timber.w("presenter is not yet initialized")
        }
    }

    fun showError(error: String) {
        AlertDialog.Builder(requireContext()).setMessage(error).setPositiveButton(
            android.R.string.ok,
            null
        ).show()
    }

    abstract fun initPresenter(): P
}
