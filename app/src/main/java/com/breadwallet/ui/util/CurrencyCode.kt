package com.breadwallet.ui.util

typealias CurrencyCode = String

fun CurrencyCode.isBitcoin() : Boolean = equals("btc", true)
fun CurrencyCode.isBitcash() : Boolean = equals("bch", true)
fun CurrencyCode.isEth() : Boolean = equals("eth", true)
fun CurrencyCode.isBrd() : Boolean = equals("brd", true)


