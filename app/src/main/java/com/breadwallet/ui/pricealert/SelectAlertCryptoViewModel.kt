/**
 * BreadWallet
 * <p/>
 * Created by Drew Carlson <drew.carlson@breadwallet.com> on 6/2/2019.
 * Copyright (c) 2019 breadwallet LLC
 * <p/>
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 * <p/>
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 * <p/>
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package com.breadwallet.ui.pricealert

import android.arch.lifecycle.LiveData
import android.arch.lifecycle.ViewModel
import android.content.Context
import com.breadwallet.BreadApp
import com.breadwallet.presenter.entities.TokenItem
import com.breadwallet.tools.manager.BRReportsManager
import com.breadwallet.tools.threads.executor.BRExecutor
import com.breadwallet.ui.util.map
import com.breadwallet.ui.util.mutableLiveData
import com.breadwallet.ui.util.switchMap
import com.breadwallet.wallet.wallets.bitcoin.WalletBchManager
import com.breadwallet.wallet.wallets.bitcoin.WalletBitcoinManager
import com.breadwallet.wallet.wallets.ethereum.WalletEthManager
import com.breadwallet.wallet.wallets.ethereum.WalletTokenManager
import com.platform.entities.TokenListMetaData
import com.platform.tools.KVStoreManager

class SelectAlertCryptoViewModel : ViewModel() {

    // TODO: Inject classes that require a Context
    private val context: Context
        get() = BreadApp.getBreadContext().applicationContext

    private val walletEthManager
        get() = WalletEthManager.getInstance(context)

    private val selectedCrypto = mutableLiveData<TokenItem>()
    private val filterQuery = mutableLiveData("")
    private val tokenItems = mutableLiveData<List<TokenItem>>()

    /**
     * Returns a list of the user's active tokens
     * filtered by the current value of [filterQuery].
     */
    fun getTokenItems(): LiveData<List<TokenItem>> {
        if (tokenItems.value == null) {
            BRExecutor.getInstance().forLightWeightBackgroundTasks().execute {
                tokenItems.postValue(
                        KVStoreManager.getTokenListMetaData(context)
                                .enabledCurrencies
                                .filterMissingErc20Tokens()
                                .mapToTokenItem())
            }
        }

        return filterQuery.switchMap { queryString ->
            if (queryString.isBlank()) tokenItems
            else tokenItems.map { tokenItems ->
                tokenItems.filter {
                    it.name.equals(queryString, false) ||
                            it.symbol.equals(queryString, false)
                }
            }
        }
    }


    /**
     * Set the filter query used by [getTokenItems].
     */
    fun setFilterQuery(query: String?) {
        filterQuery.value = query ?: ""
    }

    /**
     * Returns the user's chosen item.
     */
    fun getSelectedCrypto(): LiveData<TokenItem> = selectedCrypto

    /**
     * Set the user selected item.
     */
    fun setSelectedCrypto(tokenInfo: TokenItem) {
        selectedCrypto.value = tokenInfo
    }

    /**
     * Filter out erc20 missing tokens from a list of
     * [TokenListMetaData.TokenInfo].
     */
    private fun List<TokenListMetaData.TokenInfo>.filterMissingErc20Tokens() =
            filter { tokenInfo ->
                if (tokenInfo.erc20 && tokenInfo.symbol != WalletTokenManager.BRD_CURRENCY_CODE) {
                    val tk = walletEthManager.node.lookupToken(tokenInfo.contractAddress)
                    if (tk == null) {
                        BRReportsManager.reportBug(NullPointerException("No token for contract: ${tokenInfo.contractAddress}"))
                        false
                    } else true
                } else true
            }

    /**
     * Transform a list of [TokenListMetaData.TokenInfo] into
     * a list of [TokenItem]s for the UI.
     */
    private fun List<TokenListMetaData.TokenInfo>.mapToTokenItem() =
            map { tokenInfo ->
                when (tokenInfo.symbol) {
                    WalletBitcoinManager.BITCOIN_CURRENCY_CODE ->
                        TokenItem(null, WalletBitcoinManager.BITCOIN_CURRENCY_CODE, WalletBitcoinManager.NAME, null, true)
                    WalletBchManager.BITCASH_CURRENCY_CODE ->
                        TokenItem(null, WalletBchManager.BITCASH_CURRENCY_CODE, WalletBchManager.NAME, null, true)
                    WalletEthManager.ETH_CURRENCY_CODE ->
                        TokenItem(null, WalletEthManager.ETH_CURRENCY_CODE, WalletEthManager.NAME, null, true)
                    WalletTokenManager.BRD_CURRENCY_CODE ->
                        TokenItem(null, WalletTokenManager.BRD_CURRENCY_CODE, WalletTokenManager.BRD_CURRENCY_CODE, null, true)
                    else -> {
                        walletEthManager.node
                                .lookupToken(tokenInfo.contractAddress)!!
                                .run { TokenItem(address, symbol, name, null, true) }
                    }
                }
            }
}