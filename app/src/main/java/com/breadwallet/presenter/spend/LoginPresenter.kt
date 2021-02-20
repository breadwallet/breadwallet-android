package com.breadwallet.presenter.spend

import com.breadwallet.BreadApp
import com.breadwallet.R
import com.breadwallet.presenter.base.BasePresenter
import com.breadwallet.tools.manager.BRSharedPrefs
import org.litecoin.partnerapi.callback.LoginCallback
import org.litecoin.partnerapi.network.AuthClient
import javax.inject.Inject

/** Litewallet
 * Created by Mohamed Barry on 6/30/20
 * email: mosadialiou@gmail.com
 * Copyright © 2020 Litecoin Foundation. All rights reserved.
 */
class LoginPresenter(view: LoginView) : BasePresenter<LoginView>(view) {

    @Inject
    lateinit var authClient: AuthClient

    override fun subscribe() {
    }

    override fun unsubscribe() {
    }

    fun login(email: String, password: String) {
        view?.showProgress()
        authClient.login(
            email, password,
            object : LoginCallback {
                override fun on2faRequired() {
                    view?.hideProgress()
                    (view as LoginView?)?.show2faView()
                }

                override fun onEmailNotVerified() {
                    view?.hideProgress()
                    view?.showError(R.string.Error_emailNotVerified)
                }

                override fun onUserNotFound(message: String) {
                    view?.hideProgress()
                    view?.showError(message)
                }

                override fun onLoggedIn(id: String) {
                    view?.hideProgress()
                    BRSharedPrefs.putLitecoinCardId(BreadApp.getBreadContext(), id)
                    (view as LoginView?)?.showTransferView()
                }

                override fun onWrong2faTokenProvided() {
                    view?.hideProgress()
                    view?.showError(R.string.Error_wrong2faTokenProvided)
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
                }
            }
        )
    }
}
