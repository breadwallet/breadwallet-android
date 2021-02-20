package com.breadwallet.presenter.base

/** Litewallet
 * Created by Mohamed Barry on 6/30/20
 * email: mosadialiou@gmail.com
 * Copyright © 2020 Litecoin Foundation. All rights reserved.
 */
interface BaseView {
    fun showProgress() {}
    fun hideProgress() {}
    fun showError(error: String)
    fun showError(errorId: Int) {}
}
