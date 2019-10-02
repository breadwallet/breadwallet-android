package com.platform.interfaces

import kotlinx.coroutines.flow.Flow

interface WalletsProvider {

    fun enabledWallets(): Flow<List<String>>

    //fun allAvailableWallets(): Flow<List<WalletMetaData>>

    //fun enableWallet(currencyCode: String)

    //fun disableWallet(currencyCode: String)
}

data class WalletMetaData(
    val name: String,
    val currencyCode: String,
    val isErc20: Boolean
)