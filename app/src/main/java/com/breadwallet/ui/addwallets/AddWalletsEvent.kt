package com.breadwallet.ui.addwallets

import io.hypno.switchboard.MobiusUpdateSpec

@MobiusUpdateSpec(
    baseEffect = AddWalletsEffect::class,
    baseModel = AddWalletsModel::class
)
sealed class AddWalletsEvent {
    data class OnSearchQueryChanged(val query: String) : AddWalletsEvent()
    data class OnTokensChanged(val tokens: List<Token>) : AddWalletsEvent()

    data class OnAddWalletClicked(val token: Token) : AddWalletsEvent()
    object OnBackClicked : AddWalletsEvent()
}
