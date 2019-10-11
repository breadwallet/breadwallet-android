package com.breadwallet.ui.addwallets

import android.content.Context
import com.breadwallet.breadbox.BreadBox
import com.breadwallet.ext.bindConsumerIn
import com.breadwallet.legacy.presenter.entities.TokenItem
import com.breadwallet.tools.util.TokenUtil
import com.platform.interfaces.AccountMetaDataProvider
import com.spotify.mobius.Connection
import com.spotify.mobius.functions.Consumer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancelChildren
import kotlinx.coroutines.channels.BroadcastChannel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.onStart

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
        }
    }

    override fun dispose() {
        coroutineContext.cancelChildren()
    }

    private fun loadTokens() {
        breadBox.wallets()
            .combine(search) { trackedWallets, query ->
                TokenFilter(trackedWallets.map { it.currency.code }, query)
            }
            .mapLatest { tokenFilter ->
                TokenUtil.getTokenItems(contextProvider())
                    .applyFilter(tokenFilter)
                    .map { it.asToken() }
            }
            .map { AddWalletsEvent.OnTokensChanged(it) }
            .bindConsumerIn(output, this)
    }

    private fun searchTokens(query: String) = searchChannel.offer(query)

    private fun addWallet(currencyId: String) =
        acctMetaDataProvider.enableWallet(currencyId).launchIn(this)

    data class TokenFilter(val trackedTokens: List<String>, val query: String)

    private fun List<TokenItem>.applyFilter(filter: TokenFilter) =
        filter { token ->
            !filter.trackedTokens.any { it.equals(token.symbol, true) }
        }.filter { token ->
            token.name.contains(filter.query, true) || token.symbol.contains(filter.query, true)
        }

    private fun TokenItem.asToken(): Token {
        return Token(name, symbol, checkNotNull(currencyId), checkNotNull(startColor))
    }
}


