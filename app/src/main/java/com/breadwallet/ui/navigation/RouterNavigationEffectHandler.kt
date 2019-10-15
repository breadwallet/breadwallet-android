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
package com.breadwallet.ui.navigation

import com.bluelinelabs.conductor.Router
import com.bluelinelabs.conductor.RouterTransaction
import com.bluelinelabs.conductor.changehandler.HorizontalChangeHandler
import com.bluelinelabs.conductor.changehandler.VerticalChangeHandler
import com.breadwallet.ui.addwallets.AddWalletsController
import com.breadwallet.ui.home.HomeController
import com.breadwallet.ui.login.LoginController
import com.breadwallet.ui.managewallets.ManageWalletsController
import com.breadwallet.ui.pin.InputPinController
import com.breadwallet.ui.provekey.PaperKeyProveController
import com.breadwallet.ui.send.SendSheetController
import com.breadwallet.ui.showkey.ShowPaperKeyController
import com.breadwallet.ui.wallet.BrdWalletController
import com.breadwallet.ui.wallet.TxDetailsController
import com.breadwallet.ui.wallet.WalletController
import com.breadwallet.ui.writedownkey.WriteDownKeyController
import com.breadwallet.util.isBrd
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

    override fun goToWallet(effect: NavigationEffect.GoToWallet) {
        val walletController = when {
            effect.currencyCode.isBrd() -> BrdWalletController()
            else -> WalletController(effect.currencyCode)
        }
        router.pushController(
            RouterTransaction.with(walletController)
                .popChangeHandler(HorizontalChangeHandler())
                .pushChangeHandler(HorizontalChangeHandler())
        )
    }

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

    override fun goToAddWallet() {
        router.pushController(
            RouterTransaction.with(AddWalletsController())
                .popChangeHandler(HorizontalChangeHandler())
                .pushChangeHandler(HorizontalChangeHandler())
        )
    }

    override fun goToSend(effect: NavigationEffect.GoToSend) {
        val controller = when {
            effect.cryptoRequest != null -> SendSheetController(effect.cryptoRequest)
            else -> SendSheetController(effect.currencyId)
        }
        router.pushController(RouterTransaction.with(controller))
    }

    override fun goToReceive(effect: NavigationEffect.GoToReceive) = Unit

    override fun goToTransaction(effect: NavigationEffect.GoToTransaction) {
        val controller = TxDetailsController(effect.currencyId, effect.txHash)
        router.pushController(RouterTransaction.with(controller))
    }

    override fun goToDeepLink(effect: NavigationEffect.GoToDeepLink) = Unit

    override fun goToInAppMessage(effect: NavigationEffect.GoToInAppMessage) = Unit

    override fun goToFaq(effect: NavigationEffect.GoToFaq) = Unit

    override fun goToSetPin(effect: NavigationEffect.GoToSetPin) {
        val transaction =
            RouterTransaction.with(InputPinController(effect.onComplete, !effect.onboarding))
                .pushChangeHandler(HorizontalChangeHandler())
                .popChangeHandler(HorizontalChangeHandler())
        if (effect.onboarding) {
            router.setBackstack(listOf(transaction), HorizontalChangeHandler())
        } else {
            router.pushController(transaction)
        }
    }

    override fun goToHome() {
        router.setBackstack(
            listOf(RouterTransaction.with(HomeController())), HorizontalChangeHandler()
        )
    }

    override fun goToLogin() {
        router.pushController(
            RouterTransaction.with(LoginController())
                .popChangeHandler(HorizontalChangeHandler())
                .pushChangeHandler(HorizontalChangeHandler())
        )
    }

    override fun goToErrorDialog(effect: NavigationEffect.GoToErrorDialog) = Unit

    override fun goToManageWallets() {
        router.pushController(
            RouterTransaction.with(ManageWalletsController())
                .popChangeHandler(HorizontalChangeHandler())
                .pushChangeHandler(HorizontalChangeHandler())
        )
    }

    override fun goToDisabledScreen() = Unit

    override fun goToQrScan() = Unit

    override fun goToWriteDownKey(effect: NavigationEffect.GoToWriteDownKey) {
        router.pushController(
            RouterTransaction.with(
                WriteDownKeyController(effect.onComplete)
            ).pushChangeHandler(VerticalChangeHandler()).popChangeHandler(VerticalChangeHandler())
        )
    }

    override fun goToPaperKey(effect: NavigationEffect.GoToPaperKey) {
        router.pushController(
            RouterTransaction.with(
                ShowPaperKeyController(effect.phrase, effect.onComplete)
            ).pushChangeHandler(VerticalChangeHandler()).popChangeHandler(VerticalChangeHandler())
        )
    }

    override fun goToPaperKeyProve(effect: NavigationEffect.GoToPaperKeyProve) {
        router.pushController(
            RouterTransaction.with(
                PaperKeyProveController(effect.phrase, effect.onComplete)
            ).pushChangeHandler(VerticalChangeHandler()).popChangeHandler(VerticalChangeHandler())
        )
    }
}
