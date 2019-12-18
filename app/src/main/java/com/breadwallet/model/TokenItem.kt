package com.breadwallet.model

data class TokenItem(
    val address: String?,
    val symbol: String,
    val name: String,
    var image: String?,
    val isSupported: Boolean,
    val startColor: String? = null,
    val endColor: String? = null,
    val currencyId: String? = null,
    val cryptocompareAlias: String? = null
) {

    val exchangeRateCurrencyCode: String
        get() {
            return if (cryptocompareAlias.isNullOrBlank()) {
                symbol
            } else {
                cryptocompareAlias
            }
        }
}
