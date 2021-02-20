package com.breadwallet.di.component

import com.breadwallet.presenter.spend.LoginPresenter
import com.breadwallet.presenter.spend.RegisterPresenter
import dagger.Component
import org.litecoin.partnerapi.di.module.NetworkModule
import javax.inject.Singleton

/** Litewallet
 * Created by Mohamed Barry on 6/30/20
 * email: mosadialiou@gmail.com
 * Copyright © 2020 Litecoin Foundation. All rights reserved.
 */
@Singleton
@Component(modules = [NetworkModule::class])
interface PresenterComponent {
    fun inject(presenter: LoginPresenter)
    fun inject(presenter: RegisterPresenter)
}
