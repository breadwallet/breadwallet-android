package com.breadwallet.presenter.spend

import java.util.regex.Pattern

/** Litewallet
 * Created by Mohamed Barry on 8/3/20
 * email: mosadialiou@gmail.com
 * Copyright © 2020 Litecoin Foundation. All rights reserved.
 */

val emailRegex: Pattern = Pattern.compile(
    "[a-zA-Z0-9\\+\\.\\_\\%\\-\\+]{1,256}" +
        "\\@" +
        "[a-zA-Z0-9][a-zA-Z0-9\\-]{0,64}" +
        "(" +
        "\\." +
        "[a-zA-Z0-9][a-zA-Z0-9\\-]{0,25}" +
        ")+"
)

fun CharSequence.isEmail(): Boolean {
    return emailRegex.matcher(this).matches()
}

fun CharSequence.hasPwdMinLength() = this.length >= 6
