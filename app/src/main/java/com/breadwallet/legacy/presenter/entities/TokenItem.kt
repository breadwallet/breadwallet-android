package com.breadwallet.legacy.presenter.entities

data class TokenItem(
    val address: String?,
    val symbol: String,
    val name: String,
    var image: String?,
    val isSupported: Boolean
) {
    var startColor: String? = null
    var endColor: String? = null
    var currencyId: String? = null
}
