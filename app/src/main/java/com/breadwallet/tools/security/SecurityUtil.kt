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