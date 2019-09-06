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
package com.breadwallet.ui.home

import com.breadwallet.tools.util.EventUtils
import com.spotify.mobius.Effects.effects
import com.spotify.mobius.Next.*
import com.spotify.mobius.Update

val HomeScreenUpdate = Update<HomeScreenModel, HomeScreenEvent, HomeScreenEffect> { model, event ->

    when (event) {
        is HomeScreenEvent.OnWalletSyncProgressUpdated -> {
            when (val wallet = model.wallets[event.currencyCode]) {
                null -> noChange<HomeScreenModel, HomeScreenEffect>()
                else -> {
                    val wallets = model.wallets.toMutableMap()
                    wallets[event.currencyCode] = wallet.copy(
                            syncProgress = event.progress,
                            syncingThroughMillis = event.syncThroughMillis
                    )
                    next(model.copy(wallets = wallets))
                }
            }
        }
        is HomeScreenEvent.OnBuyBellNeededLoaded -> next(model.copy(isBuyBellNeeded = event.isBuyBellNeeded))
        is HomeScreenEvent.OnWalletsAdded -> {
            next(model.copy(
                    wallets = model.wallets + event.wallets.associateBy { it.currencyCode }
            ))
        }
        is HomeScreenEvent.OnWalletAdded -> {
            val wallets = model.wallets.toMutableMap()
            wallets[event.wallet.currencyCode] = event.wallet
            next(model.copy(wallets = wallets))
        }
        is HomeScreenEvent.OnWalletBalanceUpdated -> {
            when (val wallet = model.wallets[event.currencyCode]) {
                null -> noChange<HomeScreenModel, HomeScreenEffect>()
                else -> {
                    when (wallet.balance == event.balance && wallet.fiatBalance == event.fiatBalance
                            && wallet.priceChange == event.priceChange && wallet.fiatPricePerUnit == event.fiatPricePerUnit) {
                        true -> noChange<HomeScreenModel, HomeScreenEffect>()
                        else -> {
                            val wallets = model.wallets.toMutableMap()
                            wallets[event.currencyCode] = wallet.copy(
                                    balance = event.balance,
                                    fiatBalance = event.fiatBalance,
                                    fiatPricePerUnit = event.fiatPricePerUnit,
                                    priceChange = event.priceChange
                            )
                            next(model.copy(wallets = wallets))
                        }
                    }
                }
            }
        }
        is HomeScreenEvent.OnConnectionUpdated -> next(model.copy(hasInternet = event.isConnected))
        HomeScreenEvent.OnAddWalletClicked -> dispatch(effects(HomeScreenEffect.GoToAddWallet))
        is HomeScreenEvent.OnWalletClicked -> dispatch(effects(HomeScreenEffect.GoToWallet(event.currencyCode)))
        HomeScreenEvent.OnBuyClicked -> dispatch(effects(HomeScreenEffect.GoToBuy))
        HomeScreenEvent.OnTradeClicked -> dispatch(effects(HomeScreenEffect.GoToTrade))
        HomeScreenEvent.OnMenuClicked -> dispatch(effects(HomeScreenEffect.GoToMenu))
        is HomeScreenEvent.OnPromptLoaded -> next(model.copy(promptId = event.promptId))
        is HomeScreenEvent.OnDeepLinkProvided -> dispatch(effects(HomeScreenEffect.GoToDeepLink(event.url)))
        is HomeScreenEvent.OnInAppNotificationProvided -> dispatch(effects(HomeScreenEffect.GoToInappMessage(event.inAppMessage)))
        is HomeScreenEvent.OnPushNotificationOpened -> dispatch(effects(HomeScreenEffect.RecordPushNotificationOpened(event.campaignId)))
        is HomeScreenEvent.OnShowBuyAndSell -> {
            val clickAttributes = mapOf(EventUtils.EVENT_ATTRIBUTE_BUY_AND_SELL to model.showBuyAndSell.toString())
            next<HomeScreenModel, HomeScreenEffect>(
                    model.copy(showBuyAndSell = event.showBuyAndSell),
                    effects(HomeScreenEffect.TrackEvent(EventUtils.EVENT_HOME_DID_TAP_BUY, clickAttributes))
            )
        }
    }
}