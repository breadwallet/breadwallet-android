package com.breadwallet.ui.settings.currency

import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class FiatCurrency(val code: String, val name: String)