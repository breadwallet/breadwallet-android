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

import io.hypno.switchboard.MobiusUpdateSpec

@MobiusUpdateSpec(
    baseEffect = OnBoardingEffect::class,
    baseModel = OnBoardingModel::class
)
sealed class OnBoardingEvent {
    data class OnPageChanged(val page: Int) : OnBoardingEvent()
    object OnSkipClicked : OnBoardingEvent()
    object OnBackClicked : OnBoardingEvent()
    object OnBuyClicked : OnBoardingEvent()
    object OnBrowseClicked : OnBoardingEvent()

    object OnWalletCreated : OnBoardingEvent()

    sealed class SetupError : OnBoardingEvent() {
        object PhraseCreationFailed : SetupError()
        object PhraseStoreFailed : SetupError()
        object PhraseLoadFailed : SetupError()

        object AccountKeyCreationFailed : SetupError()
        object ApiKeyCreationFailed : SetupError()

        object StoreWalletFailed : SetupError()

        object CryptoSystemBootError : SetupError()
    }
}