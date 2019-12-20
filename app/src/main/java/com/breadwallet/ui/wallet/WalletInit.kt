package com.breadwallet.ui.wallet

import com.breadwallet.tools.util.EventUtils
import com.spotify.mobius.Effects.effects
import com.spotify.mobius.First.first
import com.spotify.mobius.Init

val WalletInit = Init<WalletScreenModel, WalletScreenEffect> { model ->
    first(
        model, effects(
            WalletScreenEffect.LoadWalletBalance(model.currencyCode),
            WalletScreenEffect.LoadTransactions(model.currencyCode),
            WalletScreenEffect.LoadFiatPricePerUnit(model.currencyCode),
            WalletScreenEffect.LoadCryptoPreferred,
            WalletScreenEffect.LoadCurrencyName(model.currencyCode),
            WalletScreenEffect.LoadSyncState(model.currencyCode),
            WalletScreenEffect.LoadChartInterval(model.priceChartInterval, model.currencyCode),
            WalletScreenEffect.TrackEvent(
                String.format(
                    EventUtils.EVENT_WALLET_APPEARED,
                    model.currencyCode
                )
            ),
            WalletScreenEffect.LoadIsTokenSupported(model.currencyCode)
        )
    )
}
