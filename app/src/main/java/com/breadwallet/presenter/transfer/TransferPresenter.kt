package com.breadwallet.presenter.transfer

import com.breadwallet.BreadApp
import com.breadwallet.presenter.base.BasePresenter
import com.breadwallet.tools.manager.BRSharedPrefs

/** Litewallet
 * Created by Mohamed Barry on 6/30/20
 * email: mosadialiou@gmail.com
 * Copyright © 2020 Litecoin Foundation. All rights reserved.
 */
class TransferPresenter(view: TransferView) : BasePresenter<TransferView>(view) {
    override fun subscribe() {
    }

    override fun unsubscribe() {
    }

    fun logout() {
        BRSharedPrefs.logoutFromLitecoinCard(BreadApp.getBreadContext())
    }
}
