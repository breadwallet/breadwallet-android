package com.breadwallet.ui.addwallets

import com.breadwallet.ui.navigation.NavEffectHolder
import com.breadwallet.ui.navigation.NavigationEffect

sealed class AddWalletsEffect {
    data class SearchTokens(val query: String) : AddWalletsEffect()
    data class AddWallet(val token: Token) : AddWalletsEffect()
    data class RemoveWallet(val token: Token) : AddWalletsEffect()

    object GoBack : AddWalletsEffect(), NavEffectHolder {
        override val navigationEffect = NavigationEffect.GoBack
    }
}
