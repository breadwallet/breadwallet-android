/**
 * BreadWallet
 *
 * Created by Drew Carlson <drew.carlson@breadwallet.com> on 8/12/19.
 * Copyright (c) 2019 breadwallet LLC
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package com.breadwallet.ui.onboarding

import android.view.View
import cash.just.support.CashSupport
import cash.just.support.pages.GeneralSupportPage
import cash.just.ui.CashUI
import com.bluelinelabs.conductor.RouterTransaction
import com.bluelinelabs.conductor.changehandler.HorizontalChangeHandler
import com.breadwallet.R
import com.breadwallet.tools.animation.UiUtils
import com.breadwallet.tools.util.EventUtils
import com.breadwallet.ui.BaseController
import com.breadwallet.ui.send.fragmentManager
import kotlinx.android.synthetic.main.controller_intro.*

/**
 * Activity shown when there is no wallet, here the user can pick
 * between creating new wallet or recovering one with the paper key.
 */
class IntroController : BaseController() {

    override val layoutId: Int = R.layout.controller_intro

    override fun onCreateView(view: View) {
        super.onCreateView(view)
        button_new_wallet.setOnClickListener {
            EventUtils.pushEvent(EventUtils.EVENT_LANDING_PAGE_GET_STARTED)
            router.pushController(RouterTransaction.with(OnBoardingController())
                    .popChangeHandler(HorizontalChangeHandler())
                    .pushChangeHandler(HorizontalChangeHandler()))
        }
        button_recover_wallet.setOnClickListener {
            EventUtils.pushEvent(EventUtils.EVENT_LANDING_PAGE_RESTORE_WALLET)
            router.pushController(RouterTransaction.with(IntroRecoveryController())
                    .popChangeHandler(HorizontalChangeHandler())
                    .pushChangeHandler(HorizontalChangeHandler()))
        }
        faq_button.setOnClickListener {
            if (!UiUtils.isClickAllowed()) return@setOnClickListener
            CashUI.showSupportPage(CashSupport.Builder().detail(GeneralSupportPage.GET_STARTED), router.fragmentManager()!!)
        }
    }

    override fun onAttach(view: View) {
        super.onAttach(view)
        EventUtils.pushEvent(EventUtils.EVENT_LANDING_PAGE_APPEARED)
    }
}
