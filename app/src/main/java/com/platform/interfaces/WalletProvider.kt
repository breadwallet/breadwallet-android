package com.platform.interfaces

import com.breadwallet.crypto.WalletManagerMode
import kotlinx.coroutines.flow.Flow

interface WalletProvider {

    fun enabledWallets(): Flow<List<String>>
    fun walletModes(): Flow<Map<String, WalletManagerMode>>
}