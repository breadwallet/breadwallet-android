/**
 * BreadWallet
 *
 * Created by Pablo Budelli on <pablo.budelli@breadwallet.com> 10/25/19.
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
package com.breadwallet.ui.login

import io.hypno.switchboard.MobiusUpdateSpec

@MobiusUpdateSpec(
    baseModel = LoginModel::class,
    baseEffect = LoginEffect::class
)
sealed class LoginEvent {
    object OnFingerprintClicked : LoginEvent()
    object OnPinLocked : LoginEvent()
    object OnUnlockAnimationEnd : LoginEvent()
    data class OnFingerprintEnabled(val enabled: Boolean) : LoginEvent()
    data class OnLoginPreferencesLoaded(
        val showHomeScreen: Boolean,
        val currentCurrencyCode: String
    ) : LoginEvent()

    object OnAuthenticationSuccess : LoginEvent()
    object OnAuthenticationFailed : LoginEvent()
}