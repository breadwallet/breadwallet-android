/**
 * BreadWallet
 *
 * Created by Drew Carlson <drew.carlson@breadwallet.com> on 8/14/20.
 * Copyright (c) 2020 breadwallet LLC
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

import com.spotify.mobius.Next

interface OnBoardingUpdateSpec {
    fun patch(model: OnBoarding.M, event: OnBoarding.E): Next<OnBoarding.M, OnBoarding.F> = when (event) {
        OnBoarding.E.OnSkipClicked -> onSkipClicked(model)
        OnBoarding.E.OnBackClicked -> onBackClicked(model)
        OnBoarding.E.OnBuyClicked -> onBuyClicked(model)
        OnBoarding.E.OnBrowseClicked -> onBrowseClicked(model)
        OnBoarding.E.OnWalletCreated -> onWalletCreated(model)
        is OnBoarding.E.OnPageChanged -> onPageChanged(model, event)
        is OnBoarding.E.SetupError -> setupError(model, event)
    }

    fun onSkipClicked(model: OnBoarding.M): Next<OnBoarding.M, OnBoarding.F>

    fun onBackClicked(model: OnBoarding.M): Next<OnBoarding.M, OnBoarding.F>

    fun onBuyClicked(model: OnBoarding.M): Next<OnBoarding.M, OnBoarding.F>

    fun onBrowseClicked(model: OnBoarding.M): Next<OnBoarding.M, OnBoarding.F>

    fun onWalletCreated(model: OnBoarding.M): Next<OnBoarding.M, OnBoarding.F>

    fun onPageChanged(model: OnBoarding.M, event: OnBoarding.E.OnPageChanged): Next<OnBoarding.M, OnBoarding.F>

    fun setupError(model: OnBoarding.M, event: OnBoarding.E.SetupError): Next<OnBoarding.M, OnBoarding.F>
}