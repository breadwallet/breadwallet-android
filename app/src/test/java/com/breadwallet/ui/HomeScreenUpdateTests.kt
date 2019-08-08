package com.breadwallet.ui

import com.breadwallet.tools.manager.PromptManager
import com.breadwallet.ui.home.*
import com.spotify.mobius.test.NextMatchers.*
import com.spotify.mobius.test.UpdateSpec
import com.spotify.mobius.test.UpdateSpec.assertThatNext
import org.junit.Test
import java.math.BigDecimal

class HomeScreenUpdateTests {

    private val WALLET_BITCOIN = Wallet("Bitcoin", "btc")
    private val WALLET_ETHEREUM = Wallet("Ethereum", "eth")

    private val spec = UpdateSpec(HomeScreenUpdate)

    @Test
    fun addAndUpdateWallets() {

        val initState = HomeScreenModel.createDefault()

        // Add initial list of wallets
        val wallets = mapOf(WALLET_BITCOIN.currencyCode to WALLET_BITCOIN.copy())

        spec.given(initState)
            .`when`(
                HomeScreenEvent.OnWalletsAdded(wallets.values.toList()))
            .then(
                assertThatNext(
                    hasModel(initState.copy(wallets = wallets)),
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
                HomeScreenEvent.OnWalletAdded(walletToAdd))
            .then(
                assertThatNext(
                        hasModel(initialWalletsAddedState.copy(wallets = expectedWallets)),
                        hasNoEffects()
                )
            )

        // Update ETH wallet balance
        val ethWalletAddedState = initialWalletsAddedState.copy(wallets = expectedWallets)
        val updatedWallet = walletToAdd.copy(balance = BigDecimal.valueOf(1), fiatBalance = BigDecimal.valueOf(1000), fiatPricePerUnit = BigDecimal.valueOf(1000))


        expectedWallets = expectedWallets.toMutableMap()
        expectedWallets[updatedWallet.currencyCode] = updatedWallet

        spec.given(ethWalletAddedState)
            .`when`(
                HomeScreenEvent.OnWalletBalanceUpdated(updatedWallet.currencyCode, updatedWallet.balance, updatedWallet.fiatBalance, updatedWallet.fiatPricePerUnit))
            .then(
                assertThatNext(
                        hasModel(ethWalletAddedState.copy(wallets = expectedWallets)),
                        hasNoEffects()
                )
            )
    }

    @Test
    fun syncProgressUpdate() {
        val wallets = mutableMapOf(WALLET_BITCOIN.currencyCode to WALLET_BITCOIN.copy())
        val initState = HomeScreenModel.createDefault().copy(wallets = wallets)

        val progress = 0.15
        val expectedWallet = WALLET_BITCOIN.copy(syncProgress = progress)
        wallets[expectedWallet.currencyCode] = expectedWallet

        spec.given(initState)
            .`when`(
                HomeScreenEvent.OnWalletSyncProgressUpdated(currencyCode = WALLET_BITCOIN.currencyCode, progress = progress, syncThroughMillis = 0L))
            .then(
                assertThatNext(
                        hasModel(initState.copy(wallets = wallets)),
                        hasNoEffects()
                )
            )
    }

    @Test
    fun connectivityUpdate() {
        val initState = HomeScreenModel.createDefault()

        spec.given(initState)
            .`when`(
                HomeScreenEvent.OnConnectionUpdated(isConnected = false))
            .then(
                assertThatNext(
                        hasModel(initState.copy(hasInternet = false)),
                        hasNoEffects()
                )
            )
    }

    @Test
    fun addWalletClick() {
        val initState = HomeScreenModel.createDefault()

        spec.given(initState)
            .`when`(
                HomeScreenEvent.OnAddWalletClicked)
            .then(
                assertThatNext(
                    hasEffects(HomeScreenEffect.GoToAddWallet as HomeScreenEffect)
                )
            )
    }

    @Test
    fun walletClick() {
        val initState = HomeScreenModel.createDefault().copy(wallets = mutableMapOf(WALLET_BITCOIN.currencyCode to WALLET_BITCOIN.copy()))

        val expectedEffect = HomeScreenEffect.GoToWallet(WALLET_BITCOIN.currencyCode)

        spec.given(initState)
            .`when`(
                HomeScreenEvent.OnWalletClicked(currencyCode = WALLET_BITCOIN.currencyCode))
            .then(
                assertThatNext(
                        hasEffects(expectedEffect as HomeScreenEffect)
                )
            )
    }

    @Test
    fun buyClick() {
        val initState = HomeScreenModel.createDefault()

        spec.given(initState)
            .`when`(
                HomeScreenEvent.OnBuyClicked)
            .then(
                assertThatNext(
                        hasEffects(HomeScreenEffect.GoToBuy as HomeScreenEffect)
                )
            )
    }

    @Test
    fun tradeClick() {
        val initState = HomeScreenModel.createDefault()

        spec.given(initState)
            .`when`(
                HomeScreenEvent.OnTradeClicked)
            .then(
                assertThatNext(
                        hasEffects(HomeScreenEffect.GoToTrade as HomeScreenEffect)
                )
            )
    }

    @Test
    fun menuClick() {
        val initState = HomeScreenModel.createDefault()

        spec.given(initState)
            .`when`(
                HomeScreenEvent.OnMenuClicked)
            .then(
                assertThatNext(
                        hasEffects(HomeScreenEffect.GoToMenu as HomeScreenEffect)
                )
            )
    }

    @Test
    fun promptLoaded() {
        val initState = HomeScreenModel.createDefault()

        spec.given(initState)
            .`when`(
                HomeScreenEvent.OnPromptLoaded(promptId = PromptManager.PromptItem.NO_PASSCODE))
            .then(
                assertThatNext(
                        hasModel(initState.copy(promptId = PromptManager.PromptItem.NO_PASSCODE))
                )
            )
    }
}