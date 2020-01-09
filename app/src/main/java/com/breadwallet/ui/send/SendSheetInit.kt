package com.breadwallet.ui.send

import com.spotify.mobius.First
import com.spotify.mobius.Init


object SendSheetInit : Init<SendSheetModel, SendSheetEffect> {
    override fun init(model: SendSheetModel): First<SendSheetModel, SendSheetEffect> {
        val effects = mutableSetOf<SendSheetEffect>()

        if (model.targetAddress.isNotBlank()) {
            effects.add(SendSheetEffect.ValidateAddress(
                model.currencyCode,
                model.targetAddress
            ))
        }

        return First.first(
            model,
            effects + setOf(
                SendSheetEffect.LoadBalance(model.currencyCode),
                SendSheetEffect.LoadExchangeRate(model.currencyCode, model.fiatCode),
                SendSheetEffect.LoadAuthenticationSettings
            )
        )
    }
}
