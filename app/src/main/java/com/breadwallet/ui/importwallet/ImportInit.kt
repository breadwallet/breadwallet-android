package com.breadwallet.ui.importwallet

import com.spotify.mobius.First.first
import com.spotify.mobius.Init

val ImportInit = Init<Import.M, Import.F> { model ->
    if (model.privateKey != null && !model.isKeyValid) {
        first(
            model, setOf(
                Import.F.ValidateKey(model.privateKey, model.keyPassword)
            )
        )
    } else first(model)
}
