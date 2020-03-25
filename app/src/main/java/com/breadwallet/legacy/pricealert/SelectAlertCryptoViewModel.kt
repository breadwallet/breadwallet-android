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
package com.breadwallet.legacy.pricealert

import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import com.breadwallet.breadbox.BreadBox
import com.breadwallet.ext.map
import com.breadwallet.ext.mutableLiveData
import com.breadwallet.ext.switchMap
import com.breadwallet.model.TokenItem
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancelChildren
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.take
import java.util.Locale
import com.breadwallet.crypto.Wallet as CryptoWallet

class SelectAlertCryptoViewModel(
    private val breadBox: BreadBox
) : ViewModel(), CoroutineScope {

    override val coroutineContext = SupervisorJob() + Dispatchers.Default

    private val selectedCrypto = mutableLiveData<TokenItem>()
    private val filterQuery = mutableLiveData("")
    private val tokenItems = mutableLiveData<List<TokenItem>>()

    /**
     * Returns a list of the user's active tokens
     * filtered by the current value of [filterQuery].
     */
    fun getTokenItems(): LiveData<List<TokenItem>> {
        if (tokenItems.value == null) {
            breadBox.wallets()
                .take(1)
                .map { wallets -> wallets.mapToTokenItem() }
                .onEach { tokenItems.postValue(it) }
                .launchIn(this@SelectAlertCryptoViewModel)
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

    override fun onCleared() {
        super.onCleared()
        coroutineContext.cancelChildren()
    }

    /**
     * Transform a list of [CryptoWallet] into
     * a list of [TokenItem]s for the UI.
     */
    private fun List<CryptoWallet>.mapToTokenItem() =
        map { wallet ->
            TokenItem(
                address = wallet.target.toString(),
                symbol = wallet.currency.code.toUpperCase(Locale.ROOT),
                name = wallet.currency.name,
                image = null,
                isSupported = true
            )
        }
}
