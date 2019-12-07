package com.breadwallet.model

import com.breadwallet.crypto.WalletManagerMode

enum class SyncMode(val walletManagerMode: WalletManagerMode) {
    API_ONLY(WalletManagerMode.API_ONLY),
    P2P_ONLY(WalletManagerMode.P2P_ONLY);

    companion object {
        private val map = values().associateBy(SyncMode::walletManagerMode)
        fun fromWalletManagerMode(mode: WalletManagerMode) = checkNotNull(map[mode])
    }
}
