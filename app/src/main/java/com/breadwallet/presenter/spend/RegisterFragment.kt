package com.breadwallet.presenter.spend

import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AlertDialog
import com.breadwallet.R
import com.breadwallet.entities.Country
import com.breadwallet.presenter.base.BaseFragment
import com.breadwallet.tools.util.CountryHelper
import com.breadwallet.tools.util.onError
import com.breadwallet.tools.util.text
import com.github.razir.progressbutton.bindProgressButton
import com.github.razir.progressbutton.hideProgress
import com.github.razir.progressbutton.showProgress
import kotlinx.android.synthetic.main.fragment_register.*
import kotlinx.android.synthetic.main.view_register.*

/** Litewallet
 * Created by Mohamed Barry on 6/3/20
 * email: mosadialiou@gmail.com
 * Copyright © 2020 Litecoin Foundation. All rights reserved.
 */
class RegisterFragment : BaseFragment<RegisterPresenter>(), RegisterView {

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_register, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        toolBar.setNavigationOnClickListener {
            goToLogin()
        }
        countryField.editText?.keyListener = null
        countryField.editText?.run {
            val country = CountryHelper.countries.find { it.name == "United States" }
            setText(country?.name)
            tag = country
        }

        submitBut.setOnClickListener { handleSubmit() }
        bindProgressButton(submitBut)
    }

    private fun goToLogin() {
        (parentFragment as AuthBottomSheetDialogFragment?)?.onBackPressed()
    }

    private fun handleSubmit() {
        presenter.register(
            firstNameField.text(),
            lastNameField.text(),
            emailField.text(),
            passwordField.text(),
            confirmPasswordField.text(),
            addressField.text(),
            null,
            cityField.text(),
            stateField.text(),
            postalCodeField.text(),
            countryField.editText?.tag as Country,
            mobileNumberField.text()
        )
    }

    override fun onWrongFirstName(errorResId: Int) {
        firstNameField.onError(errorResId)
    }

    override fun onWrongLastName(errorResId: Int) {
        lastNameField.onError(errorResId)
    }

    override fun onWrongAddress1(errorResId: Int) {
        addressField.onError(errorResId)
    }

    override fun onWrongCity(errorResId: Int) {
        cityField.onError(errorResId)
    }

    override fun onWrongState(errorResId: Int) {
        stateField.onError(errorResId)
    }

    override fun onWrongPostalCode(errorResId: Int) {
        postalCodeField.onError(errorResId)
    }

    override fun onWrongCountry(errorResId: Int) {
        countryField.onError(errorResId)
    }

    override fun onWrongPhone(errorResId: Int) {
        mobileNumberField.onError(errorResId)
    }

    override fun onWrongEmail(errorResId: Int) {
        emailField.onError(errorResId)
    }

    override fun onWrongPassword(errorResId: Int) {
        passwordField.onError(errorResId)
    }

    override fun onWrongConfirmPassword(errorResId: Int) {
        confirmPasswordField.onError(errorResId)
    }

    override fun showProgress() {
        submitBut.showProgress { progressColor = Color.WHITE }
        submitBut.isEnabled = false
    }

    override fun hideProgress() {
        submitBut.hideProgress(R.string.Button_submit)
        submitBut.isEnabled = true
    }

    override fun onRegisteredSuccessful() {
        AlertDialog.Builder(requireContext()).setMessage(R.string.Register_Dialog_registeredSuccessMessage).setPositiveButton(
            android.R.string.ok
        ) { _, _ ->
            goToLogin()
        }.show()
    }

    override fun initPresenter() = RegisterPresenter(this)
}
