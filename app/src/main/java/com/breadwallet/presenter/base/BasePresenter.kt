package com.breadwallet.presenter.base

import com.breadwallet.di.component.DaggerPresenterComponent
import com.breadwallet.presenter.spend.LoginPresenter
import com.breadwallet.presenter.spend.RegisterPresenter

/** Litewallet
 * Created by Mohamed Barry on 6/30/20
 * email: mosadialiou@gmail.com
 * Copyright © 2020 Litecoin Foundation. All rights reserved.
 */
abstract class BasePresenter<out V : BaseView>(var view: BaseView?) {

    private val injector = DaggerPresenterComponent.create()

    init {
        inject()
    }

    private fun inject() {
        when (this) {
            is LoginPresenter -> injector.inject(this)
            is RegisterPresenter -> injector.inject(this)
        }
    }

    abstract fun subscribe()
    abstract fun unsubscribe()

    fun detach() {
        view = null
    }

    fun isAttached() = view != null
}
