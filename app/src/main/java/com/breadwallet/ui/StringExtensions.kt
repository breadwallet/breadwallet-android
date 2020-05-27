package com.breadwallet.ui

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

fun String.toDate(dateFormat: String = "yyyy-MM-dd HH:mm:ss", timeZone: TimeZone = TimeZone.getTimeZone("UTC")): Date {
    val parser = SimpleDateFormat(dateFormat, Locale.getDefault())
    parser.timeZone = timeZone
    return parser.parse(this)
}

fun Date.formatTo(dateFormat: String, timeZone: TimeZone = TimeZone.getDefault()): String {
    val formatter = SimpleDateFormat(dateFormat, Locale.getDefault())
    formatter.timeZone = timeZone
    return formatter.format(this)
}