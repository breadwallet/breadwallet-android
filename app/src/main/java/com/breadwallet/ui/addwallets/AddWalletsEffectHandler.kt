/**
 * BreadWallet
 *
 * Created by Ahsan Butt <ahsan.butt@breadwallet.com> on 10/11/19.
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
package com.breadwallet.ui.addwallets

import android.content.Context
import com.breadwallet.breadbox.BreadBox
import com.breadwallet.breadbox.containsCurrency
import com.breadwallet.breadbox.currencyId
import com.breadwallet.breadbox.findByCurrencyId
import com.breadwallet.breadbox.findCurrency
import com.breadwallet.breadbox.isNative
import com.breadwallet.breadbox.networkContainsCurrency
import com.breadwallet.crypto.Wallet
import com.breadwallet.logger.logDebug
import com.breadwallet.logger.logError
import com.breadwallet.model.TokenItem
import com.breadwallet.tools.util.TokenUtil
import com.breadwallet.ui.navigation.NavEffectTransformer
import com.platform.interfaces.AccountMetaDataProvider
import com.spotify.mobius.flow.flowTransformer
import com.spotify.mobius.flow.subtypeEffectHandler
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapLatest

@UseExperimental(ExperimentalCoroutinesApi::class, FlowPreview::class)
object AddWalletsEffectHandler {

    fun createEffectHandler(
        context: Context,
        breadBox: BreadBox,
        acctMetaDataProvider: AccountMetaDataProvider,
        navEffectHandler: NavEffectTransformer
    ) = subtypeEffectHandler<AddWalletsEffect, AddWalletsEvent> {
        addTransformer(searchTokens(breadBox, context))
        addConsumer(addWallet(breadBox, acctMetaDataProvider))
        addConsumer(removeWallet(acctMetaDataProvider))
        addTransformer<AddWalletsEffect.GoBack>(navEffectHandler)
    }

    private fun searchTokens(breadBox: BreadBox, context: Context) =
        flowTransformer<AddWalletsEffect.SearchTokens, AddWalletsEvent> { effects ->
            effects
                .flatMapLatest { effect ->
                    breadBox.wallets().map { wallets ->
                        wallets to effect.query
                    }
                }
                .mapLatest { (trackedWallets, query) ->
                    TokenUtil.getTokenItems(context)
                        .filter { it.isSupported }
                        .applyFilter(query)
                        .map { tokenItem ->
                            val currencyId = tokenItem.currencyId ?: ""
                            tokenItem.asToken(
                                enabled = trackedWallets.containsCurrency(currencyId),
                                removable = isRemovable(
                                    trackedWallets.findByCurrencyId(currencyId),
                                    trackedWallets
                                )
                            )
                        }
                }
                .map { tokens -> AddWalletsEvent.OnTokensChanged(tokens) }
        }

    /** Adds a [Wallet] for the given [currencyId] and its native wallet, if not already tracked. */
    private fun addWallet(
        breadBox: BreadBox,
        acctMetaDataProvider: AccountMetaDataProvider
    ): suspend (AddWalletsEffect.AddWallet) -> Unit = { addWallet ->
        val currencyId = addWallet.token.currencyId
        val system = checkNotNull(breadBox.getSystemUnsafe())
        val network = system.networks.find { it.containsCurrency(currencyId) }

        when (network?.findCurrency(currencyId)?.isNative()) {
            null -> logError("No network or currency found for $currencyId.")
            false -> {
                val trackedWallets = breadBox.wallets().first()
                if (!trackedWallets.containsCurrency(network.currency.uids)) {
                    logDebug("Adding native wallet ${network.currency.uids} for $currencyId.")
                    acctMetaDataProvider.enableWallet(network.currency.uids)
                }
            }
        }

        logDebug("Adding wallet '$currencyId'")
        acctMetaDataProvider.enableWallet(currencyId)
    }

    private fun removeWallet(
        acctMetaDataProvider: AccountMetaDataProvider
    ): suspend (AddWalletsEffect.RemoveWallet) -> Unit = { removeWallet ->
        val currencyId = removeWallet.token.currencyId
        logDebug("Removing wallet '$currencyId'")
        acctMetaDataProvider.disableWallet(currencyId)
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


