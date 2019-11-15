package com.breadwallet.ui

import com.breadwallet.model.PriceChange
import com.breadwallet.ui.home.HomeScreenEffect
import com.breadwallet.ui.home.HomeScreenEvent
import com.breadwallet.ui.home.HomeScreenModel
import com.breadwallet.ui.home.HomeScreenUpdate
import com.breadwallet.ui.home.PromptItem
import com.breadwallet.ui.home.Wallet
import com.spotify.mobius.test.NextMatchers.hasEffects
import com.spotify.mobius.test.NextMatchers.hasModel
import com.spotify.mobius.test.NextMatchers.hasNoEffects
import com.spotify.mobius.test.UpdateSpec
import com.spotify.mobius.test.UpdateSpec.assertThatNext
import org.junit.Test
import java.math.BigDecimal

@Suppress("LongMethod")
class HomeScreenUpdateTests {

    private val WALLET_BITCOIN = Wallet("bitcoin-mainnet:__native__", "Bitcoin", "btc")
    private val WALLET_ETHEREUM = Wallet("ethereum-mainnet:__native__", "Ethereum", "eth")

    private val spec = UpdateSpec(HomeScreenUpdate)

    @Test
    fun addAndUpdateWallets() {

        val initState = HomeScreenModel.createDefault()

        // Add initial list of wallets
        val wallets = mapOf(WALLET_BITCOIN.currencyCode to WALLET_BITCOIN.copy())

        spec.given(initState)
            .`when`(
                HomeScreenEvent.OnWalletsUpdated(wallets.values.toList())
            )
            .then(
                assertThatNext(
                    hasModel(initState.copy(
                        wallets = wallets,
                        displayOrder = wallets.values.map { it.currencyId }
                    )),
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
                HomeScreenEvent.OnWalletsUpdated(expectedWallets.values.toList())
            )
            .then(
                assertThatNext(
                    hasModel(initialWalletsAddedState.copy(
                        wallets = expectedWallets,
                        displayOrder = expectedWallets.values.map { it.currencyId }
                    )),
                    hasNoEffects()
                )
            )

        // Update ETH wallet balance
        val ethWalletAddedState = initialWalletsAddedState.copy(wallets = expectedWallets)
        val updatedWallet = walletToAdd.copy(
            balance = BigDecimal.valueOf(1),
            fiatBalance = BigDecimal.valueOf(1000),
            fiatPricePerUnit = BigDecimal.valueOf(1000),
            priceChange = PriceChange(0.1, 10.0)
        )


        expectedWallets = expectedWallets.toMutableMap()
        expectedWallets[updatedWallet.currencyCode] = updatedWallet

        spec.given(ethWalletAddedState)
            .`when`(
                HomeScreenEvent.OnWalletBalanceUpdated(
                    updatedWallet.currencyCode,
                    updatedWallet.balance,
                    updatedWallet.fiatBalance,
                    updatedWallet.fiatPricePerUnit,
                    updatedWallet.priceChange!!
                )
            )
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

        val progress = 0.15f
        val expectedWallet = WALLET_BITCOIN.copy(syncProgress = progress, isSyncing = true)
        wallets[expectedWallet.currencyCode] = expectedWallet

        spec.given(initState)
            .`when`(
                HomeScreenEvent.OnWalletSyncProgressUpdated(
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
        val initState = HomeScreenModel.createDefault()

        spec.given(initState)
            .`when`(
                HomeScreenEvent.OnConnectionUpdated(isConnected = false)
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
        val initState = HomeScreenModel.createDefault()
            .copy(wallets = mutableMapOf(WALLET_BITCOIN.currencyCode to WALLET_BITCOIN.copy()))

        val expectedEffect = HomeScreenEffect.GoToWallet(WALLET_BITCOIN.currencyCode)

        spec.given(initState)
            .`when`(
                HomeScreenEvent.OnWalletClicked(currencyCode = WALLET_BITCOIN.currencyCode)
            )
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
                HomeScreenEvent.OnBuyClicked
            )
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
                HomeScreenEvent.OnTradeClicked
            )
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
                HomeScreenEvent.OnMenuClicked
            )
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
                HomeScreenEvent.OnPromptLoaded(promptId = PromptItem.EMAIL_COLLECTION)
            )
            .then(
                assertThatNext(
                    hasModel(initState.copy(promptId = PromptItem.EMAIL_COLLECTION))
                )
            )
    }
}
