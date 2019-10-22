package com.breadwallet.ui.addwallets

data class AddWalletsModel(
    val tokens: List<Token> = emptyList(),
    val searchQuery: String = ""
) {
    companion object {
        fun createDefault() = AddWalletsModel()
    }
}

data class Token(
    val name: String,
    val currencyCode: String,
    val currencyId: String,
    val startColor: String,
    val enabled: Boolean,
    val removable: Boolean
)