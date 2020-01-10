/**
 * BreadWallet
 *
 * Created by Pablo Budelli <pablo.budelli@breadwallet.com> on 10/25/19.
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

import com.breadwallet.tools.util.EventUtils
import com.spotify.mobius.Next
import com.spotify.mobius.Next.dispatch
import com.spotify.mobius.Next.next
import com.spotify.mobius.Update

object LoginUpdate : Update<LoginModel, LoginEvent, LoginEffect>, LoginUpdateSpec {

    override fun update(model: LoginModel, event: LoginEvent): Next<LoginModel, LoginEffect> =
        patch(model, event)

    override fun onFingerprintClicked(model: LoginModel): Next<LoginModel, LoginEffect> =
        dispatch(setOf(LoginEffect.ShowFingerprintController))

    override fun onAuthenticationSuccess(model: LoginModel): Next<LoginModel, LoginEffect> =
        dispatch(
            setOf(
                LoginEffect.AuthenticationSuccess,
                LoginEffect.TrackEvent(EventUtils.EVENT_LOGIN_SUCCESS)
            )
        )

    override fun onAuthenticationFailed(model: LoginModel): Next<LoginModel, LoginEffect> =
        dispatch(
            setOf(
                LoginEffect.AuthenticationFailed,
                LoginEffect.TrackEvent(EventUtils.EVENT_LOGIN_FAILED)
            )
        )

    override fun onPinLocked(model: LoginModel): Next<LoginModel, LoginEffect> =
        dispatch(setOf(LoginEffect.GoToDisableScreen))

    override fun onUnlockAnimationEnd(model: LoginModel): Next<LoginModel, LoginEffect> {
        val effect = when {
            model.extraUrl.isNotBlank() ->
                LoginEffect.GoToDeepLink(model.extraUrl)
            model.showHomeScreen -> LoginEffect.GoToHome
            model.currentCurrencyCode.isNotBlank() ->
                LoginEffect.GoToWallet(model.currentCurrencyCode)
            else -> LoginEffect.GoToHome
        }
        return dispatch(setOf(effect))
    }

    override fun onFingerprintEnabled(
        model: LoginModel,
        event: LoginEvent.OnFingerprintEnabled
    ): Next<LoginModel, LoginEffect> =
        next(model.copy(fingerprintEnable = event.enabled))

    override fun onLoginPreferencesLoaded(
        model: LoginModel,
        event: LoginEvent.OnLoginPreferencesLoaded
    ): Next<LoginModel, LoginEffect> =
        next(
            model.copy(
                showHomeScreen = event.showHomeScreen,
                currentCurrencyCode = event.currentCurrencyCode
            )
        )
}
