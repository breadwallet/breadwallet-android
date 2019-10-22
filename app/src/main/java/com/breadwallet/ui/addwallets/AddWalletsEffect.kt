package com.breadwallet.ui.addwallets

sealed class AddWalletsEffect {
    object LoadTokens : AddWalletsEffect()

    data class SearchTokens(val query: String) : AddWalletsEffect()
    data class AddWallet(val token: Token) : AddWalletsEffect()
    data class RemoveWallet(val token: Token) : AddWalletsEffect()

    object GoBack : AddWalletsEffect()

    object DoNothing : AddWalletsEffect() // TODO: Remove when issue with effects() is fixed
}