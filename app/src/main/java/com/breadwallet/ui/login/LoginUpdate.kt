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
import com.breadwallet.ui.login.LoginScreen.E
import com.breadwallet.ui.login.LoginScreen.F
import com.breadwallet.ui.login.LoginScreen.M
import com.spotify.mobius.Next
import com.spotify.mobius.Next.dispatch
import com.spotify.mobius.Next.next
import com.spotify.mobius.Update

object LoginUpdate : Update<M, E, F>, LoginScreenUpdateSpec {

    override fun update(model: M, event: E): Next<M, F> =
        patch(model, event)

    override fun onFingerprintClicked(model: M): Next<M, F> =
        dispatch(setOf(F.ShowFingerprintController))

    override fun onAuthenticationSuccess(model: M): Next<M, F> =
        next(
            model.copy(isUnlocked = true),
            setOf(
                F.AuthenticationSuccess,
                F.UnlockBrdUser,
                F.TrackEvent(EventUtils.EVENT_LOGIN_SUCCESS)
            )
        )

    override fun onAuthenticationFailed(model: M): Next<M, F> =
        dispatch(
            setOf(
                F.AuthenticationFailed,
                F.TrackEvent(EventUtils.EVENT_LOGIN_FAILED)
            )
        )

    override fun onPinLocked(model: M): Next<M, F> =
        dispatch(setOf(F.GoToDisableScreen))

    override fun onUnlockAnimationEnd(model: M): Next<M, F> {
        val effect = when {
            model.extraUrl.isNotBlank() ->
                F.GoToDeepLink(model.extraUrl)
            model.showHomeScreen -> F.GoToHome
            else -> F.GoBack
        } as F
        return dispatch(setOf(effect))
    }

    override fun onFingerprintEnabled(
        model: M,
        event: E.OnFingerprintEnabled
    ): Next<M, F> = next(
        model.copy(fingerprintEnable = event.enabled),
        if (event.enabled) {
            setOf(F.ShowFingerprintController)
        } else {
            emptySet()
        }
    )
}
