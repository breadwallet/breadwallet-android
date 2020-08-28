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
package com.breadwallet.ui.login

import com.spotify.mobius.Next

interface LoginScreenUpdateSpec {
    fun patch(model: LoginScreen.M, event: LoginScreen.E): Next<LoginScreen.M, LoginScreen.F> = when (event) {
        LoginScreen.E.OnFingerprintClicked -> onFingerprintClicked(model)
        LoginScreen.E.OnPinLocked -> onPinLocked(model)
        LoginScreen.E.OnUnlockAnimationEnd -> onUnlockAnimationEnd(model)
        LoginScreen.E.OnAuthenticationSuccess -> onAuthenticationSuccess(model)
        LoginScreen.E.OnAuthenticationFailed -> onAuthenticationFailed(model)
        is LoginScreen.E.OnFingerprintEnabled -> onFingerprintEnabled(model, event)
    }

    fun onFingerprintClicked(model: LoginScreen.M): Next<LoginScreen.M, LoginScreen.F>

    fun onPinLocked(model: LoginScreen.M): Next<LoginScreen.M, LoginScreen.F>

    fun onUnlockAnimationEnd(model: LoginScreen.M): Next<LoginScreen.M, LoginScreen.F>

    fun onAuthenticationSuccess(model: LoginScreen.M): Next<LoginScreen.M, LoginScreen.F>

    fun onAuthenticationFailed(model: LoginScreen.M): Next<LoginScreen.M, LoginScreen.F>

    fun onFingerprintEnabled(model: LoginScreen.M, event: LoginScreen.E.OnFingerprintEnabled): Next<LoginScreen.M, LoginScreen.F>
}