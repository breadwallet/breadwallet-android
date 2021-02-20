package com.breadwallet.di.component

import com.breadwallet.BreadApp
import com.breadwallet.di.module.AppModule
import dagger.Component

/** Litewallet
 * Created by Mohamed Barry on 6/30/20
 * email: mosadialiou@gmail.com
 * Copyright © 2020 Litecoin Foundation. All rights reserved.
 */
@Component(modules = [AppModule::class])
interface AppComponent {
    fun inject(app: BreadApp)
}
