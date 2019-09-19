/**
 * BreadWallet
 *
 * Created by Ahsan Butt <ahsan.butt@breadwallet.com> 8/1/19.
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
package com.breadwallet.ui.global.effect

import com.bluelinelabs.conductor.Router
import com.bluelinelabs.conductor.RouterTransaction
import com.bluelinelabs.conductor.changehandler.HorizontalChangeHandler
import com.breadwallet.ui.home.HomeController
import com.breadwallet.ui.login.LoginController
import com.spotify.mobius.Connection
import com.spotify.mobius.android.runners.MainThreadWorkRunner

@Suppress("TooManyFunctions")
class RouterNavigationEffectHandler(
        private val router: Router
) : Connection<NavigationEffect>,
        NavigationEffectHandlerSpec {

    private val workRunner = MainThreadWorkRunner.create()

    override fun accept(value: NavigationEffect) {
        // We must run the navigation code on the MainThread.
        // Being an implementation detail we internalize the
        // MainThread usage and ignore the EffectRunner thread.
        workRunner.post {
            patch(value)
        }
    }

    override fun dispose() {
        workRunner.dispose()
    }

    override fun goToWallet(effect: NavigationEffect.GoToWallet) = Unit

    override fun goBack() {
        if (!router.handleBack()) {
            router.activity?.onBackPressed()
        }
    }

    override fun goToBrdRewards() = Unit

    override fun goToReview() = Unit

    override fun goToBuy() = Unit

    override fun goToTrade() = Unit

    override fun goToMenu() = Unit

    override fun goToAddWallet() = Unit

    override fun goToSend(effect: NavigationEffect.GoToSend) = Unit

    override fun goToReceive(effect: NavigationEffect.GoToReceive) = Unit

    override fun goToTransaction(effect: NavigationEffect.GoToTransaction) = Unit

    override fun goToDeepLink(effect: NavigationEffect.GoToDeepLink) = Unit

    override fun goToInAppMessage(effect: NavigationEffect.GoToInAppMessage) = Unit

    override fun goToFaq(effect: NavigationEffect.GoToFaq) = Unit

    override fun goToSetPin(effect: NavigationEffect.GoToSetPin) = Unit

    override fun goToHome() {
        // TODO clear backstack
        router.pushController(RouterTransaction.with(HomeController())
                .popChangeHandler(HorizontalChangeHandler())
                .pushChangeHandler(HorizontalChangeHandler()))
    }

    override fun goToLogin() {
        router.pushController(RouterTransaction.with(LoginController())
                .popChangeHandler(HorizontalChangeHandler())
                .pushChangeHandler(HorizontalChangeHandler()))
    }

    override fun goToErrorDialog(effect: NavigationEffect.GoToErrorDialog) = Unit
}