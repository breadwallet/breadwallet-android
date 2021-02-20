package com.breadwallet.presenter.spend

import com.breadwallet.presenter.base.BaseView

/** Litewallet
 * Created by Mohamed Barry on 6/30/20
 * email: mosadialiou@gmail.com
 * Copyright © 2020 Litecoin Foundation. All rights reserved.
 */
interface LoginView : BaseView {
    fun showTransferView()
    fun show2faView()
}
