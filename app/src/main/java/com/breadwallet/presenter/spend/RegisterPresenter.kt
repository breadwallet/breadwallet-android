package com.breadwallet.presenter.spend

import com.breadwallet.R
import com.breadwallet.entities.Country
import com.breadwallet.presenter.base.BasePresenter
import com.breadwallet.tools.util.NO_ERROR
import com.breadwallet.tools.util.noError
import org.litecoin.partnerapi.callback.RegisterCallback
import org.litecoin.partnerapi.model.User
import org.litecoin.partnerapi.network.UserClient
import javax.inject.Inject

/** Litewallet
 * Created by Mohamed Barry on 6/30/20
 * email: mosadialiou@gmail.com
 * Copyright © 2020 Litecoin Foundation. All rights reserved.
 */
class RegisterPresenter(view: RegisterView) : BasePresenter<RegisterView>(view) {

    @Inject
    lateinit var userClient: UserClient

    override fun subscribe() {
    }

    override fun unsubscribe() {
    }

    fun register(
        firstname: CharSequence,
        lastname: CharSequence,
        email: CharSequence,
        password: CharSequence,
        passwordConfirmation: CharSequence,
        address1: CharSequence,
        address2: CharSequence?,
        city: CharSequence,
        state: CharSequence,
        postalCode: CharSequence,
        country: Country,
        phone: CharSequence
    ) {
        if (validateFields(
            firstname,
            lastname,
            email,
            password,
            passwordConfirmation,
            address1,
            city,
            state,
            postalCode,
            country.name,
            phone
        )
        ) {
            view?.showProgress()
            userClient.register(
                firstname.toString(),
                lastname.toString(),
                email.toString(),
                password.toString(),
                passwordConfirmation.toString(),
                address1.toString(),
                address2?.toString(),
                city.toString(),
                state.toString(),
                postalCode.toString(),
                country.code,
                phone.toString(),
                callback = object : RegisterCallback {
                    override fun onRegistered(user: User) {
                        view?.hideProgress()
                        (view as RegisterView?)?.onRegisteredSuccessful()
                    }

                    override fun onEmailAlreadyUsed(message: String) {
                        view?.hideProgress()
                        view?.showError(message)
                    }

                    override fun onValidationError(message: String) {
                        view?.hideProgress()
                        view?.showError(message)
                    }

                    override fun onUnknownSystemError() {
                        view?.hideProgress()
                        view?.showError(R.string.Error_unknownSystem)
                    }

                    override fun onTokenExpired() {
                        view?.hideProgress()
                    }

                    override fun onFailure(error: Int) {
                        view?.hideProgress()
                        view?.showError(error)
                    }
                }
            )
        }
    }

    private fun validateFields(
        firstName: CharSequence,
        lastName: CharSequence,
        email: CharSequence,
        password: CharSequence,
        confirmPassword: CharSequence,
        address1: CharSequence,
        city: CharSequence,
        state: CharSequence,
        postalCode: CharSequence,
        country: CharSequence,
        phone: CharSequence

    ): Boolean {
        val firstNameResId = errorResId(firstName)
        val lastNameResId = errorResId(lastName)
        val address1ResId = errorResId(address1)
        val cityResId = errorResId(city)
        val stateResId = errorResId(state)
        val postalCodeResId = errorResId(postalCode)
        val countryResId = errorResId(country)
        val phoneResId = errorResId(phone)

        val emailResId = emailErrorId(email)
        val pwdResId = passwordErrorId(password)
        val confirmPasswordResId = confirmPasswordErrorId(password, confirmPassword)

        (view as RegisterView?)?.run {
            onWrongFirstName(firstNameResId)
            onWrongLastName(lastNameResId)
            onWrongAddress1(address1ResId)
            onWrongCity(cityResId)
            onWrongState(stateResId)
            onWrongPostalCode(postalCodeResId)
            onWrongCountry(countryResId)
            onWrongPhone(phoneResId)

            onWrongEmail(emailResId)
            onWrongPassword(pwdResId)
            onWrongConfirmPassword(confirmPasswordResId)
        }

        return firstNameResId.noError() && lastNameResId.noError() &&
            address1ResId.noError() && cityResId.noError() && stateResId.noError() && postalCodeResId.noError() &&
            countryResId.noError() && phoneResId.noError() &&
            emailResId.noError() && pwdResId.noError() && confirmPasswordResId.noError()
    }

    private fun confirmPasswordErrorId(password: CharSequence, confirmPassword: CharSequence): Int {
        var resId = Int.NO_ERROR
        if (password.toString() != confirmPassword.toString()) {
            resId = R.string.mismatch_password
        }
        return resId
    }

    private fun emailErrorId(email: CharSequence): Int {
        var resId = Int.NO_ERROR
        if (email.isEmpty()) {
            resId = R.string.required
        } else if (!email.isEmail()) {
            resId = R.string.wrong_email_format
        }
        return resId
    }

    private fun passwordErrorId(pwd: CharSequence): Int {
        var resId = Int.NO_ERROR
        if (pwd.isEmpty()) {
            resId = R.string.required
        } else if (!pwd.hasPwdMinLength()) {
            resId = R.string.error_pwd_6chars_min
        }
        return resId
    }

    private fun errorResId(field: CharSequence) =
        if (field.isEmpty()) R.string.required else Int.NO_ERROR
}
