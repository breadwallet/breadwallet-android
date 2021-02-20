package com.breadwallet.presenter.spend

import android.content.Intent
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AlertDialog
import androidx.core.text.buildSpannedString
import androidx.core.text.underline
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.FragmentTransaction
import com.breadwallet.R
import com.breadwallet.presenter.activities.BreadActivity
import com.breadwallet.presenter.base.BaseFragment
import com.breadwallet.tools.util.BRConstants
import com.breadwallet.tools.util.addFragment
import com.github.razir.progressbutton.bindProgressButton
import com.github.razir.progressbutton.hideProgress
import com.github.razir.progressbutton.showProgress
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import kotlinx.android.synthetic.main.fragment_login.*
import kotlinx.android.synthetic.main.view_login.*
import timber.log.Timber

/** Litewallet
 * Created by Mohamed Barry on 6/10/20
 * email: mosadialiou@gmail.com
 * Copyright © 2020 Litecoin Foundation. All rights reserved.
 */

class LoginFragment : BaseFragment<LoginPresenter>(), LoginView {

    override fun onDetach() {
        super.onDetach()
        presenter.detach()
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_login, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        toolBar.setNavigationOnClickListener { (parentFragment as DialogFragment?)?.dismiss() }
        loginBut.setOnClickListener { handleLogin() }
        registerBut.setOnClickListener { handleRegister() }
        forgetPwdBut.text = buildSpannedString {
            underline { append(forgetPwdBut.text) }
        }
        forgetPwdBut.setOnClickListener { handleForgotPassword() }
        bindProgressButton(loginBut)
    }

    private fun handleForgotPassword() {
        AlertDialog.Builder(requireContext())
                .setTitle(R.string.Login_Dialog_forgotPassword_reset)
                .setMessage(R.string.Login_Dialog_forgotPasswordMessage)
                .setPositiveButton(R.string.Login_Dialog_forgotPassword_visit)
                { _, _ -> openWebPage() }
                .setNegativeButton(
                    android.R.string.cancel,
                    null
                )
                .show()
    }

    private fun openWebPage() {
        val intent = Intent(
            Intent.ACTION_VIEW,
            Uri.parse(BRConstants.RESET_CARD_PWD_LINK)
        )
        if (intent.resolveActivity(requireContext().packageManager) != null) {
            startActivity(intent)
        } else {
            Timber.w("Web Browser is not installed")
        }
    }

    private fun handleRegister() {
        parentFragment?.addFragment(
            RegisterFragment(),
            transition = FragmentTransaction.TRANSIT_FRAGMENT_OPEN
        )
    }

    private fun handleLogin() {
        if (validateFields()) {
            presenter.login(
                emailField.editText!!.text.toString(),
                passwordField.editText!!.text.toString()
            )
        }
    }

    override fun showProgress() {
        loginBut.showProgress { progressColor = Color.WHITE }
        loginBut.isEnabled = false
    }

    override fun hideProgress() {
        loginBut.hideProgress( R.string.Login_login)
        loginBut.isEnabled = true
    }

    override fun show2faView() {
    }

    override fun showTransferView() {
        (parentFragment as BottomSheetDialogFragment?)?.dismiss()
        (activity as BreadActivity?)?.handleNavigationItemSelected(R.id.nav_card)
    }

    private fun validateFields(): Boolean {
        return true
    }

    override fun initPresenter() = LoginPresenter(this)
}
