/**
 * BreadWallet
 *
 * Created by Ahsan Butt <ahsan.butt@breadwallet.com> on 11/6/19.
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
package com.breadwallet.ui

import com.breadwallet.ui.home.HomeScreen.M
import com.breadwallet.ui.home.HomeScreen.E
import com.breadwallet.ui.home.HomeScreen.F
import com.breadwallet.ui.home.HomeScreenUpdate
import com.breadwallet.ui.home.PromptItem
import com.breadwallet.ui.home.Wallet
import com.spotify.mobius.test.NextMatchers.hasEffects
import com.spotify.mobius.test.NextMatchers.hasModel
import com.spotify.mobius.test.NextMatchers.hasNoEffects
import com.spotify.mobius.test.UpdateSpec
import com.spotify.mobius.test.UpdateSpec.assertThatNext
import org.junit.Test

@Suppress("LongMethod")
class HomeScreenUpdateTests {

    private val WALLET_BITCOIN = Wallet("bitcoin-mainnet:__native__", "Bitcoin", "btc")
    private val WALLET_ETHEREUM = Wallet("ethereum-mainnet:__native__", "Ethereum", "eth")

    private val spec = UpdateSpec(HomeScreenUpdate)

    @Test
    fun addAndUpdateWallets() {

        val initState = M.createDefault()

        // Add initial list of wallets
        val wallets = mapOf(WALLET_BITCOIN.currencyCode to WALLET_BITCOIN.copy())

        spec.given(initState)
            .`when`(
                E.OnEnabledWalletsUpdated(wallets.values.toList())
            )
            .then(
                assertThatNext(
                    hasModel(initState.copy(
                        wallets = wallets,
                        displayOrder = wallets.values.map { it.currencyId }
                    )),
                    hasEffects<M, F>(F.LoadWallets)
                )
            )

        spec.given(initState)
            .`when`(
                E.OnWalletsUpdated(wallets.values.toList())
            )
            .then(
                assertThatNext(
                    hasModel(
                        initState.copy(
                            wallets = wallets
                        )
                    ),
                    hasNoEffects()
                )
            )

        // Add ETH wallet
        val initialWalletsAddedState = initState.copy(wallets = wallets)
        val walletToAdd = WALLET_ETHEREUM.copy()

        var expectedWallets = wallets.toMutableMap()
        expectedWallets[walletToAdd.currencyCode] = walletToAdd

        spec.given(initialWalletsAddedState)
            .`when`(
                E.OnWalletsUpdated(expectedWallets.values.toList())
            )
            .then(
                assertThatNext(
                    hasModel(
                        initialWalletsAddedState.copy(
                            wallets = expectedWallets
                        )
                    ),
                    hasNoEffects()
                )
            )
    }

    @Test
    fun syncProgressUpdate() {
        val wallets = mutableMapOf(WALLET_BITCOIN.currencyCode to WALLET_BITCOIN.copy())
        val initState = M.createDefault().copy(wallets = wallets)

        val progress = 0.15f
        val expectedWallet =
            WALLET_BITCOIN.copy(
                syncProgress = progress,
                isSyncing = true,
                state = Wallet.State.READY
            )
        wallets[expectedWallet.currencyCode] = expectedWallet

        spec.given(initState)
            .`when`(
                E.OnWalletSyncProgressUpdated(
                    currencyCode = WALLET_BITCOIN.currencyCode,
                    progress = progress,
                    syncThroughMillis = 0L,
                    isSyncing = true
                )
            )
            .then(
                assertThatNext(
                    hasModel(initState.copy(wallets = wallets)),
                    hasNoEffects()
                )
            )
    }

    @Test
    fun connectivityUpdate() {
        val initState = M.createDefault()

        spec.given(initState)
            .`when`(
                E.OnConnectionUpdated(isConnected = false)
            )
            .then(
                assertThatNext(
                    hasModel(initState.copy(hasInternet = false)),
                    hasNoEffects()
                )
            )
    }

    @Test
    fun walletClick() {
        val initState = M.createDefault()
            .copy(wallets = mutableMapOf(WALLET_BITCOIN.currencyCode to WALLET_BITCOIN.copy(state = Wallet.State.LOADING)))

        spec.given(initState)
            .`when`(
                E.OnWalletClicked(currencyCode = WALLET_BITCOIN.currencyCode)
            )
            .then(
                assertThatNext(
                    hasNoEffects()
                )
            )

        val initializedWallet = WALLET_BITCOIN.copy(state = Wallet.State.READY)
        val updatedState =
            initState.copy(wallets = mutableMapOf(initializedWallet.currencyCode to initializedWallet))
        val expectedEffect = F.GoToWallet(WALLET_BITCOIN.currencyCode)

        spec.given(updatedState)
            .`when`(
                E.OnWalletClicked(currencyCode = initializedWallet.currencyCode)
            )
            .then(
                assertThatNext(
                    hasEffects(expectedEffect as F)
                )
            )
    }

    @Test
    fun buyClick() {
        val initState = M.createDefault()

        spec.given(initState)
            .`when`(
                E.OnBuyClicked
            )
            .then(
                assertThatNext(
                    hasEffects(F.GoToBuy as F)
                )
            )
    }

    @Test
    fun tradeClick() {
        val initState = M.createDefault()

        spec.given(initState)
            .`when`(
                E.OnTradeClicked
            )
            .then(
                assertThatNext(
                    hasEffects(F.GoToTrade as F)
                )
            )
    }

    @Test
    fun menuClick() {
        val initState = M.createDefault()

        spec.given(initState)
            .`when`(
                E.OnMenuClicked
            )
            .then(
                assertThatNext(
                    hasEffects(F.GoToMenu as F)
                )
            )
    }

    @Test
    fun promptLoaded() {
        val initState = M.createDefault()

        spec.given(initState)
            .`when`(
                E.OnPromptLoaded(promptId = PromptItem.EMAIL_COLLECTION)
            )
            .then(
                assertThatNext(
                    hasModel(initState.copy(promptId = PromptItem.EMAIL_COLLECTION))
                )
            )
    }
}
