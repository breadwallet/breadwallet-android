package com.breadwallet.ui.managewallets

import com.spotify.mobius.Init
import com.spotify.mobius.Effects.effects
import com.spotify.mobius.First.first

val ManageWalletsInit = Init<ManageWalletsModel, ManageWalletsEffect> { model ->
    first(
        model, effects(
            ManageWalletsEffect.LoadEnabledWallets,
            ManageWalletsEffect.DoNothing
        )
    )
}