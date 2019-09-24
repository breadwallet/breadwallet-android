/**
 * BreadWallet
 *
 * Created by Ahsan Butt <ahsan.butt@breadwallet.com> on 9/17/19.
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
package com.platform.interfaces

import kotlinx.coroutines.flow.Flow
import com.platform.entities.TokenListMetaData

/** Manages access to metadata for an [com.breadwallet.crypto.Account]. */
interface AccountMetaDataManager {
    /** Syncs metadata for the given account. */
    //fun open()

    /** Clean up. */
    //fun close()

    /** Emits a list of wallet meta-data. */
    fun walletsMetaData(): Flow<List<WalletMetaData>>

    /** Emits wallet meta-data for the given currency code. */
    fun walletMetaData(currencyCode: String): Flow<WalletMetaData>

    /** Enables the wallet for this Account. */
    //fun enableWallet(currencyCode: String): Flow<Unit>

    /** Disables the wallet for this Account. */
    //fun disableWallet(currencyCode: String): Flow<Unit>

    /** Sets the creation date of this Account. */
    //fun setAccountCreationDate(timestamp: Long): Flow<Unit>
}

data class WalletMetaData(
    val currencyCode: String,
    val isErc20: Boolean,
    val contractAddress: String?,
    val isEnabled: Boolean
)

fun TokenListMetaData.TokenInfo.toWalletMetaData(isEnabled: Boolean): WalletMetaData =
    WalletMetaData(symbol, erc20, contractAddress, isEnabled)

fun TokenListMetaData.getWalletMetaData(): List<WalletMetaData> =
        enabledCurrencies.map { it.toWalletMetaData(true) } +
        hiddenCurrencies.map { it.toWalletMetaData(false) }
