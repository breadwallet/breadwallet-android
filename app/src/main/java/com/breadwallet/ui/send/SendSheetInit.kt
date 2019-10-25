package com.breadwallet.ui.send

import com.spotify.mobius.First
import com.spotify.mobius.Init


object SendSheetInit : Init<SendSheetModel, SendSheetEffect> {
    override fun init(model: SendSheetModel): First<SendSheetModel, SendSheetEffect> {
        return First.first(
            model, setOf(
                SendSheetEffect.LoadBalance,
                SendSheetEffect.LoadExchangeRate(model.currencyCode, model.fiatCode),
                SendSheetEffect.LoadAuthenticationSettings
            )
        )
    }
}
