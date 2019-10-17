/**
 * BreadWallet
 *
 *
 * Created by Ahsan Butt <ahsan.butt@breadwallet.com> 8/1/19.
 * Copyright (c) 2019 breadwallet LLC
 *
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
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

import com.breadwallet.legacy.presenter.entities.CryptoRequest
import com.breadwallet.model.InAppMessage
import io.hypno.switchboard.MobiusHandlerSpec

@MobiusHandlerSpec
sealed class NavigationEffect {
    data class GoToSend(
        val currencyId: String,
        val cryptoRequest: CryptoRequest? = null
    ) : NavigationEffect()

    data class GoToReceive(val currencyCode: String) : NavigationEffect()
    data class GoToTransaction(
        val currencyId: String,
        val txHash: String
    ) : NavigationEffect()

    object GoBack : NavigationEffect()
    object GoToBrdRewards : NavigationEffect()
    object GoToReview : NavigationEffect()
    object GoToQrScan : NavigationEffect()

    data class GoToDeepLink(val url: String) : NavigationEffect()
    data class GoToInAppMessage(val inAppMessage: InAppMessage) : NavigationEffect()
    data class GoToWallet(val currencyCode: String) : NavigationEffect()
    data class GoToFaq(
        val articleId: String,
        val currencyCode: String? = null
    ) : NavigationEffect()

    data class GoToSetPin(
        val onboarding: Boolean = false,
        val onComplete: OnCompleteAction = OnCompleteAction.GO_HOME
    ) : NavigationEffect()

    data class GoToErrorDialog(
        val title: String,
        val message: String
    ) : NavigationEffect()

    object GoToLogin : NavigationEffect()
    object GoToHome : NavigationEffect()
    object GoToBuy : NavigationEffect()
    object GoToTrade : NavigationEffect()
    object GoToMenu : NavigationEffect()
    object GoToAddWallet : NavigationEffect()
    object GoToManageWallets : NavigationEffect()
    object GoToDisabledScreen : NavigationEffect()

    data class GoToWriteDownKey(val onComplete: OnCompleteAction) : NavigationEffect()

    data class GoToPaperKey(
        val phrase: List<String>,
        val onComplete: OnCompleteAction
    ) : NavigationEffect() {
        override fun toString() = "GoToPaperKey()"
    }

    data class GoToPaperKeyProve(
        val phrase: List<String>,
        val onComplete: OnCompleteAction
    ) : NavigationEffect() {
        override fun toString() = "GoToPaperKeyProve()"
    }
}
