package com.breadwallet.ui.addwallets

import android.content.Context
import com.breadwallet.breadbox.BreadBox
import com.breadwallet.breadbox.containsCurrency
import com.breadwallet.breadbox.currencyId

import com.breadwallet.breadbox.findByCurrencyId
import com.breadwallet.breadbox.findCurrency
import com.breadwallet.breadbox.findNetwork
import com.breadwallet.breadbox.isNative
import com.breadwallet.breadbox.networkContainsCurrency
import com.breadwallet.crypto.Wallet
import com.breadwallet.ext.bindConsumerIn
import com.breadwallet.legacy.presenter.entities.TokenItem
import com.breadwallet.logger.logDebug
import com.breadwallet.logger.logError
import com.breadwallet.tools.util.TokenUtil
import com.platform.interfaces.AccountMetaDataProvider
import com.spotify.mobius.Connection
import com.spotify.mobius.functions.Consumer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancelChildren
import kotlinx.coroutines.channels.BroadcastChannel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.launch

@UseExperimental(ExperimentalCoroutinesApi::class, FlowPreview::class)
class AddWalletsEffectHandler(
    private val output: Consumer<AddWalletsEvent>,
    private val breadBox: BreadBox,
    private val acctMetaDataProvider: AccountMetaDataProvider,
    private val contextProvider: () -> Context
) : Connection<AddWalletsEffect>, CoroutineScope {

    override val coroutineContext = SupervisorJob() + Dispatchers.Default

    private val searchChannel = BroadcastChannel<String>(Channel.BUFFERED)
    private val search =
        searchChannel
            .asFlow()
            .onStart {
                emit("")
            }

    override fun accept(value: AddWalletsEffect) {
        when (value) {
            AddWalletsEffect.LoadTokens -> loadTokens()
            is AddWalletsEffect.SearchTokens -> searchTokens(value.query)
            is AddWalletsEffect.AddWallet -> addWallet(value.token.currencyId)
            is AddWalletsEffect.RemoveWallet -> removeWallet(value.token.currencyId)
        }
    }

    override fun dispose() {
        coroutineContext.cancelChildren()
    }

    private fun loadTokens() {
        breadBox.wallets()
            .combine(search) { trackedWallets, query ->
                TokenUtil.getTokenItems(contextProvider())
                    .filter { it.isSupported }
                    .applyFilter(query)
                    .map {
                        it.asToken(
                            trackedWallets.containsCurrency(it.currencyId ?: ""),
                            isRemovable(
                                trackedWallets.findByCurrencyId(it.currencyId ?: ""),
                                trackedWallets
                            )
                        )
                    }
            }
            .map { AddWalletsEvent.OnTokensChanged(it) }
            .bindConsumerIn(output, this)
    }

    private fun searchTokens(query: String) = searchChannel.offer(query)

    /** Adds a [Wallet] for the given [currencyId] and its native wallet, if not already tracked. */
    private fun addWallet(currencyId: String) {
        launch {
            val network = breadBox
                .system()
                .findNetwork(currencyId)
                .first()
            when (network.findCurrency(currencyId)?.isNative()) {
                null -> logError("No network or currency found for $currencyId.")
                false -> {
                    val trackedWallets = breadBox.wallets().first()
                    if (!trackedWallets.containsCurrency(network.currency.uids)) {
                        logDebug("Adding native wallet ${network.currency.uids} for $currencyId.")
                        acctMetaDataProvider.enableWallet(network.currency.uids).collect()
                    }
                }
            }
            logDebug("Adding wallet $currencyId.")
            acctMetaDataProvider.enableWallet(currencyId).collect()
        }
    }

    private fun removeWallet(currencyId: String) {
        logDebug("Removing wallet $currencyId.")
        acctMetaDataProvider.disableWallet(currencyId).launchIn(this)
    }

    /**
     * Returns true if the [Wallet] exists, it's not the last remaining enabled [Wallet], and
     * it's not a [Wallet] another enabled [Wallet] depends on.
     */
    private fun isRemovable(wallet: Wallet?, trackedWallets: List<Wallet>) =
        wallet != null &&
            trackedWallets.size > 1 &&
            !walletIsNeeded(wallet, trackedWallets)

    private fun walletIsNeeded(wallet: Wallet, trackedWallets: List<Wallet>) =
        wallet.currency.isNative() &&
            trackedWallets.filter { !it.currencyId.equals(wallet.currencyId, true) }
                .any {
                    wallet.walletManager.networkContainsCurrency(it.currencyId)
                }

    private fun List<TokenItem>.applyFilter(query: String) =
        filter { token ->
            token.name.contains(query, true) || token.symbol.contains(query, true)
        }

    private fun TokenItem.asToken(enabled: Boolean, removable: Boolean): Token {
        return Token(
            name,
            symbol,
            checkNotNull(currencyId),
            checkNotNull(startColor),
            enabled,
            removable
        )
    }
}


