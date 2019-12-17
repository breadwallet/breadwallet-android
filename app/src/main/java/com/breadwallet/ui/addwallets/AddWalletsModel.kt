package com.breadwallet.ui.addwallets

data class AddWalletsModel(
    val tokens: List<Token> = emptyList(),
    val searchQuery: String = ""
) {
    companion object {
        fun createDefault() = AddWalletsModel()
    }

    override fun toString(): String {
        return "AddWalletsModel(" +
            "tokens=(size:${tokens.size}), " +
            "searchQuery='$searchQuery')"
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
