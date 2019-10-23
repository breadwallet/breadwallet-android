package com.breadwallet.ext

import java.math.BigDecimal

fun BigDecimal.isPositive() = compareTo(BigDecimal.ZERO) > 0
fun BigDecimal.isZero() = compareTo(BigDecimal.ZERO) == 0
fun BigDecimal.isNegative() = compareTo(BigDecimal.ZERO) < 0