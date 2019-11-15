package com.breadwallet.util

import java.util.regex.Pattern

private val EMAIL_REGEX =
    "^[a-zA-Z0-9_+&*-]+(?:\\.[a-zA-Z0-9_+&*-]+)*@(?:[a-zA-Z0-9-]+\\.)+[a-zA-Z]{2,7}$"

/** Return true if the string has a valid email format */
fun String?.isValidEmail(): Boolean {
    return !isNullOrBlank() && Pattern.compile(EMAIL_REGEX).matcher(this).matches()
}