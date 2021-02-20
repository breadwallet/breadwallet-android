package com.breadwallet.presenter.spend

import com.breadwallet.presenter.base.BaseView

/** Litewallet
 * Created by Mohamed Barry on 6/30/20
 * email: mosadialiou@gmail.com
 * Copyright © 2020 Litecoin Foundation. All rights reserved.
 */
interface RegisterView : BaseView {
    fun onWrongFirstName(errorResId: Int)
    fun onWrongLastName(errorResId: Int)
    fun onWrongAddress1(errorResId: Int)
    fun onWrongCity(errorResId: Int)
    fun onWrongState(errorResId: Int)
    fun onWrongPostalCode(errorResId: Int)
    fun onWrongCountry(errorResId: Int)
    fun onWrongPhone(errorResId: Int)
    fun onWrongEmail(errorResId: Int)
    fun onWrongPassword(errorResId: Int)
    fun onWrongConfirmPassword(errorResId: Int)

    fun onRegisteredSuccessful()
}
