/**
 * BreadWallet
 * <p/>
 * Created by Pablo Budelli on <pablo.budelli@breadwallet.com> 6/18/19.
 * Copyright (c) 2019 breadwallet LLC
 * <p/>
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 * <p/>
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 * <p/>
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package com.platform.util

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.text.format.DateUtils
import com.breadwallet.R
import com.breadwallet.presenter.entities.TxUiHolder
import com.breadwallet.tools.manager.BRSharedPrefs
import com.google.android.gms.common.ConnectionResult
import com.google.android.gms.common.GoogleApiAvailability


/**
 * Responsible of handling the rules for showing the dialog to request to user to submit
 */
object AppReviewPromptManager {

    private const val MIN_FOREGROUNDED_TIMES_FOR_FEEDBACK = 10
    private const val GOOGLE_PLAY_APP_URI = "market://details?id=com.breadwallet"
    private const val GOOGLE_PLAY_URI = "https://play.google.com/store/apps/details?id=com.breadwallet"
    private const val GOOGLE_PLAY_PACKAGE = "com.android.vending"

    /**
     * Check if the rules to display the review prompt are met.
     *
     * Rules:
     * - App foregrounded more than [MIN_FOREGROUNDED_TIMES_FOR_FEEDBACK].
     * - Dialog hasn't been shown.
     * - There is at least one transaction in the [transactionsList] from the [currencyCode] that
     * has been received and completed in the last 24 hours.
     */
    fun showReview(context: Context, currencyCode: String, transactionsList: List<TxUiHolder>): Boolean {
        val foregroundedTimes = BRSharedPrefs.getInt(context, BRSharedPrefs.APP_FOREGROUNDED_COUNT, 0)
        return if (foregroundedTimes >= MIN_FOREGROUNDED_TIMES_FOR_FEEDBACK
                // check if we didn't already show the prompt
                && !BRSharedPrefs.getBoolean(context, BRSharedPrefs.APP_RATE_PROMPT_HAS_RATED, false)
                && !BRSharedPrefs.getBoolean(context, BRSharedPrefs.APP_RATE_PROMPT_HAS_DISMISSED, false)) {
            // Check if it has received a transaction in the last 24 hours.
            val tx = transactionsList.firstOrNull {
                it.isReceived && it.isComplete(context, currencyCode)
                        && (System.currentTimeMillis() - it.timeStamp * DateUtils.SECOND_IN_MILLIS) < DateUtils.DAY_IN_MILLIS
            }
            return tx != null
        } else {
            false
        }
    }

    /**
     * Save that the user dismissed the review prompt to avoid showing the prompt again.
     */
    fun onReviewPromptDismissed(context: Context) =
            BRSharedPrefs.putBoolean(context, BRSharedPrefs.APP_RATE_PROMPT_HAS_DISMISSED, true)

    /**
     * Open Google Play from [activity] for the user to submit a review of the app.
     */
    fun openGooglePlay(activity: Activity) {
        BRSharedPrefs.putBoolean(activity, BRSharedPrefs.APP_RATE_PROMPT_HAS_RATED, true)
        // Try to send an intent to google play and if that fails open google play in the browser.
        try {
            val googlePlayIntent = Intent(Intent.ACTION_VIEW, Uri.parse(GOOGLE_PLAY_APP_URI))
                    .setPackage(GOOGLE_PLAY_PACKAGE)
            activity.startActivity(googlePlayIntent)
        } catch (exception: android.content.ActivityNotFoundException) {
            activity.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(GOOGLE_PLAY_URI)))
        }
        activity.overridePendingTransition(R.anim.enter_from_right, R.anim.exit_to_left)
    }
}