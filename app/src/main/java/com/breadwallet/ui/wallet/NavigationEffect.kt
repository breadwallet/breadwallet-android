package com.breadwallet.ui.wallet

import com.breadwallet.presenter.entities.CryptoRequest


sealed class NavigationEffect {
    data class GoToSend(val currencyId: String, val cryptoRequest: CryptoRequest? = null ) : NavigationEffect()
    data class GoToReceive(val currencyId: String) : NavigationEffect()
    data class GoToTransaction(val currencyId: String, val txHash: String) : NavigationEffect()

    object GoBack : NavigationEffect()
    object GoToBrdRewards : NavigationEffect()
    object GoToReview : NavigationEffect()
}
