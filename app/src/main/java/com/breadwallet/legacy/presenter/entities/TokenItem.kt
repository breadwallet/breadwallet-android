package com.breadwallet.legacy.presenter.entities

// todo are we planning to remove this class? if not move out of legacy
data class TokenItem(
    val address: String?,
    val symbol: String,
    val name: String,
    var image: String?,
    val isSupported: Boolean,
    val startColor: String? = null,
    val endColor: String? = null,
    val currencyId: String? = null
)
