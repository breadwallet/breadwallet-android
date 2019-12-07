package com.breadwallet.ui.settings.fastsync

import com.spotify.mobius.First
import com.spotify.mobius.Init

object FastSyncInit : Init<FastSyncModel, FastSyncEffect> {
    override fun init(model: FastSyncModel): First<FastSyncModel, FastSyncEffect> =
        First.first(
            model,
            setOf(FastSyncEffect.LoadCurrencyIds)
        )
}