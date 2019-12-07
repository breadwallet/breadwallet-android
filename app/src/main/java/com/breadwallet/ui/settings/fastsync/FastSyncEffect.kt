package com.breadwallet.ui.settings.fastsync

import com.breadwallet.model.SyncMode
import com.breadwallet.ui.navigation.NavEffectHolder
import com.breadwallet.ui.navigation.NavigationEffect

sealed class FastSyncEffect {
    object LoadCurrencyIds : FastSyncEffect()
    object ShowDisableFastSyncDialog : FastSyncEffect()
    object GoBack : FastSyncEffect(), NavEffectHolder {
        override val navigationEffect = NavigationEffect.GoBack
    }
    sealed class MetaData : FastSyncEffect() {
        data class SetSyncMode(
            val currencyId: String,
            val mode: SyncMode
        ) : MetaData()
        object LoadSyncModes : MetaData()
    }
}
