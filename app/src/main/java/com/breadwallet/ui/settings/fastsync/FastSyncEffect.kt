package com.breadwallet.ui.settings.fastsync

import com.breadwallet.model.SyncMode
import com.breadwallet.tools.util.BRConstants.FAQ_FASTSYNC
import com.breadwallet.ui.navigation.NavEffectHolder
import com.breadwallet.ui.navigation.NavigationEffect

sealed class FastSyncEffect {
    object LoadCurrencyIds : FastSyncEffect()
    object ShowDisableFastSyncDialog : FastSyncEffect()

    sealed class Nav : FastSyncEffect(), NavEffectHolder {
        object GoBack : Nav() {
            override val navigationEffect = NavigationEffect.GoBack
        }

        object GoToFaq : Nav() {
            override val navigationEffect = NavigationEffect.GoToFaq(FAQ_FASTSYNC)
        }
    }

    sealed class MetaData : FastSyncEffect() {
        data class SetSyncMode(
            val currencyId: String,
            val mode: SyncMode
        ) : MetaData()

        object LoadSyncModes : MetaData()
    }
}
