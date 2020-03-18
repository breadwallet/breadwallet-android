/**
 * BreadWallet
 *
 * Created by Pablo Budelli <pablo.budelli@breadwallet.com> on 10/25/19.
 * Copyright (c) 2019 breadwallet LLC
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package com.breadwallet.tools.security

import android.app.Activity
import android.content.Context
import android.text.format.DateUtils
import com.breadwallet.legacy.presenter.customviews.PinLayout
import com.breadwallet.tools.manager.BRSharedPrefs
import com.breadwallet.tools.util.Utils
import kotlin.math.pow

private const val MAX_UNLOCK_ATTEMPTS = 3

fun isWalletDisabled(activity: Activity): Boolean {
    val failCount = BRKeyStore.getFailCount(activity)
    return failCount >= MAX_UNLOCK_ATTEMPTS && disabledUntil(activity) > BRSharedPrefs.getSecureTime(
        activity
    )
}

fun disabledUntil(activity: Activity): Long {
    val failCount = BRKeyStore.getFailCount(activity)
    val failTimestamp = BRKeyStore.getFailTimeStamp(activity)
    val pow = PinLayout.MAX_PIN_DIGITS.toDouble()
        .pow((failCount - MAX_UNLOCK_ATTEMPTS).toDouble()) * DateUtils.MINUTE_IN_MILLIS
    return (failTimestamp + pow).toLong()
}

fun setPinCode(context: Context, pass: String) {
    BRKeyStore.putFailCount(0, context)
    BRKeyStore.putPinCode(pass, context)
    BRKeyStore.putLastPinUsedTime(System.currentTimeMillis(), context)
}

fun isFingerPrintAvailableAndSetup(context: Context): Boolean {
    return Utils.isFingerprintAvailable(context) && Utils.isFingerprintEnrolled(context)
}
