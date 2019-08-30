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

import com.breadwallet.tools.util.EventUtils
import com.breadwallet.ui.onboarding.OnBoardingEvent.SetupError
import com.spotify.mobius.Next
import com.spotify.mobius.Next.dispatch
import com.spotify.mobius.Next.next
import com.spotify.mobius.Next.noChange
import com.spotify.mobius.Update

object OnBoardingUpdate : Update<OnBoardingModel, OnBoardingEvent, OnBoardingEffect>,
    OnBoardingUpdateSpec {

    override fun update(model: OnBoardingModel, event: OnBoardingEvent) = patch(model, event)

    override fun onSkipClicked(model: OnBoardingModel): Next<OnBoardingModel, OnBoardingEffect> {
        return when {
            model.isLoading -> noChange()
            else -> next(
                model.copy(
                    isLoading = true,
                    pendingTarget = OnBoardingModel.Target.SKIP
                ),
                setOf(
                    OnBoardingEffect.CreateWallet,
                    OnBoardingEffect.TrackEvent(EventUtils.EVENT_SKIP_BUTTON),
                    OnBoardingEffect.TrackEvent(EventUtils.EVENT_FINAL_PAGE_BROWSE_FIRST)
                )
            )
        }
    }

    override fun onBackClicked(model: OnBoardingModel): Next<OnBoardingModel, OnBoardingEffect> {
        return when {
            model.isLoading -> noChange()
            else -> dispatch(
                setOf(
                    OnBoardingEffect.Cancel,
                    OnBoardingEffect.TrackEvent(EventUtils.EVENT_BACK_BUTTON)
                )
            )
        }
    }

    override fun onBuyClicked(model: OnBoardingModel): Next<OnBoardingModel, OnBoardingEffect> {
        return when {
            model.isLoading -> noChange()
            else -> next(
                model.copy(
                    isLoading = true,
                    pendingTarget = OnBoardingModel.Target.BUY
                ),
                setOf(
                    OnBoardingEffect.CreateWallet,
                    OnBoardingEffect.TrackEvent(EventUtils.EVENT_FINAL_PAGE_BUY_COIN)
                )
            )
        }
    }

    override fun onBrowseClicked(model: OnBoardingModel): Next<OnBoardingModel, OnBoardingEffect> {
        return when {
            model.isLoading -> noChange()
            else -> next(
                model.copy(
                    isLoading = true,
                    pendingTarget = OnBoardingModel.Target.BROWSE
                ),
                setOf(
                    OnBoardingEffect.CreateWallet,
                    OnBoardingEffect.TrackEvent(EventUtils.EVENT_FINAL_PAGE_BROWSE_FIRST)
                )
            )
        }
    }

    override fun onPageChanged(
        model: OnBoardingModel,
        event: OnBoardingEvent.OnPageChanged
    ): Next<OnBoardingModel, OnBoardingEffect> {
        return next(
            model.copy(page = event.page),
            setOf(
                OnBoardingEffect.TrackEvent(when (event.page) {
                    1 -> EventUtils.EVENT_GLOBE_PAGE_APPEARED
                    2 -> EventUtils.EVENT_COINS_PAGE_APPEARED
                    3 -> EventUtils.EVENT_FINAL_PAGE_APPEARED
                    else -> error("Invalid page, expected 1-3")
                })
            )
        )
    }

    override fun onWalletCreated(model: OnBoardingModel): Next<OnBoardingModel, OnBoardingEffect> {
        return when (model.pendingTarget) {
            OnBoardingModel.Target.NONE -> noChange()
            OnBoardingModel.Target.BROWSE -> dispatch(setOf(OnBoardingEffect.Browse))
            OnBoardingModel.Target.BUY -> dispatch(setOf(OnBoardingEffect.Buy))
            OnBoardingModel.Target.SKIP -> dispatch(setOf(OnBoardingEffect.Skip))
        }
    }

    override fun setupError(
        model: OnBoardingModel,
        event: SetupError
    ): Next<OnBoardingModel, OnBoardingEffect> {
        // TODO: For now, we collapse all errors into a
        //  single message to replicate previous behavior.
        val error = when (event) {
            SetupError.PhraseCreationFailed,
            SetupError.PhraseStoreFailed,
            SetupError.PhraseLoadFailed,
            SetupError.AccountKeyCreationFailed,
            SetupError.ApiKeyCreationFailed,
            SetupError.StoreWalletFailed,
            SetupError.CryptoSystemBootError ->
                "Failed to generate wallet, please try again."
        }
        return next(
            model.copy(
                isLoading = false,
                pendingTarget = OnBoardingModel.Target.NONE
            ),
            setOf(OnBoardingEffect.ShowError(error))
        )
    }
}