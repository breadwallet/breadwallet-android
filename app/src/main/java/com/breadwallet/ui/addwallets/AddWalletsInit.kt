package com.breadwallet.ui.addwallets

import com.spotify.mobius.Init
import com.spotify.mobius.Effects.effects
import com.spotify.mobius.First.first

val AddWalletsInit = Init<AddWalletsModel, AddWalletsEffect> { model ->
    first(model, setOf<AddWalletsEffect>(AddWalletsEffect.SearchTokens(model.searchQuery)))
}
