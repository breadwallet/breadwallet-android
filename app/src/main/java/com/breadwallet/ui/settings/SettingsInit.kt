package com.breadwallet.ui.settings

import com.spotify.mobius.First
import com.spotify.mobius.First.first
import com.spotify.mobius.Init

object SettingsInit : Init<SettingsModel, SettingsEffect> {
    override fun init(model: SettingsModel): First<SettingsModel, SettingsEffect> =
        first(model, setOf(SettingsEffect.LoadOptions(model.section)))
}