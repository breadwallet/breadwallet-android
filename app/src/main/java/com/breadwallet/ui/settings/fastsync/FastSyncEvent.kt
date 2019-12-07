package com.breadwallet.ui.settings.fastsync

import com.breadwallet.model.SyncMode
import io.hypno.switchboard.MobiusUpdateSpec

@MobiusUpdateSpec(
    baseModel = FastSyncModel::class,
    baseEffect = FastSyncEffect::class
)
sealed class FastSyncEvent {
    object OnBackClicked : FastSyncEvent()
    object OnDisableFastSyncConfirmed : FastSyncEvent()
    object OnDisableFastSyncCanceled : FastSyncEvent()
    data class OnFastSyncChanged(val enable: Boolean) : FastSyncEvent()
    data class OnSyncModesUpdated(val modeMap: Map<String, SyncMode>) : FastSyncEvent()
    data class OnCurrencyIdsUpdated(val currencyMap: Map<String, String>) : FastSyncEvent()
}