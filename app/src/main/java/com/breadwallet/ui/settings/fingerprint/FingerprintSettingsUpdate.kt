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
package com.breadwallet.ui.settings.fingerprint

import com.spotify.mobius.Next
import com.spotify.mobius.Next.next
import com.spotify.mobius.Update

object FingerprintSettingsUpdate :
    Update<FingerprintSettingsModel, FingerprintSettingsEvent, FingerprintSettingsEffect>,
    FingerprintSettingsUpdateSpec {

    override fun update(
        model: FingerprintSettingsModel,
        event: FingerprintSettingsEvent
    ): Next<FingerprintSettingsModel, FingerprintSettingsEffect> = patch(model, event)

    override fun onBackClicked(model: FingerprintSettingsModel): Next<FingerprintSettingsModel, FingerprintSettingsEffect> =
        Next.dispatch(
            setOf(FingerprintSettingsEffect.GoBack)
        )

    override fun onFaqClicked(model: FingerprintSettingsModel): Next<FingerprintSettingsModel, FingerprintSettingsEffect> =
        Next.dispatch(
            setOf(FingerprintSettingsEffect.GoToFaq)
        )

    override fun onAppUnlockChanged(
        model: FingerprintSettingsModel,
        event: FingerprintSettingsEvent.OnAppUnlockChanged
    ): Next<FingerprintSettingsModel, FingerprintSettingsEffect> {
        val updatedModel = model.copy(
            unlockApp = event.enable,
            sendMoneyEnable = event.enable,
            sendMoney = (event.enable && model.sendMoney)
        )
        return next(
            updatedModel,
            setOf(
                FingerprintSettingsEffect.UpdateFingerprintSetting(
                    updatedModel.unlockApp,
                    updatedModel.sendMoney
                )
            )
        )
    }

    override fun onSendMoneyChanged(
        model: FingerprintSettingsModel,
        event: FingerprintSettingsEvent.OnSendMoneyChanged
    ): Next<FingerprintSettingsModel, FingerprintSettingsEffect> {
        return next(
            model.copy(sendMoney = event.enable),
            setOf(
                FingerprintSettingsEffect.UpdateFingerprintSetting(
                    model.unlockApp,
                    event.enable
                )
            )
        )
    }

    override fun onSettingsLoaded(
        model: FingerprintSettingsModel,
        event: FingerprintSettingsEvent.OnSettingsLoaded
    ): Next<FingerprintSettingsModel, FingerprintSettingsEffect> {
        return next(
            model.copy(
                unlockApp = event.unlockApp,
                sendMoney = event.sendMoney,
                sendMoneyEnable = event.unlockApp
            )
        )
    }
}