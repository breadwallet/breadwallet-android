package com.breadwallet.ui.wallet

import com.spotify.mobius.Effects.effects
import com.spotify.mobius.First.first
import com.spotify.mobius.Init

val WalletInit = Init<WalletScreenModel, WalletScreenEffect> { model ->
    first(model, effects(
            WalletScreenEffect.LoadWalletBalance(model.currencyCode),
            WalletScreenEffect.LoadTransactions(model.currencyCode),
            WalletScreenEffect.LoadFiatPricePerUnit(model.currencyCode),
            WalletScreenEffect.LoadCryptoPreferred
    ))
}