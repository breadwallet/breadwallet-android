package com.breadwallet.util

typealias CurrencyCode = String

fun CurrencyCode.isBitcoin(): Boolean = equals("btc", true)
fun CurrencyCode.isBitcoinCash(): Boolean = equals("bch", true)
fun CurrencyCode.isEth(): Boolean = equals("eth", true)
fun CurrencyCode.isBrd(): Boolean = equals("brd", true)
fun CurrencyCode.isDai(): Boolean = equals("dai", true)
fun CurrencyCode.isTusd(): Boolean = equals("tusd", true)


