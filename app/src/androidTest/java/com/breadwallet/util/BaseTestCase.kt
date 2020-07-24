package com.breadwallet.util

import android.content.Intent
import androidx.test.rule.ActivityTestRule
import com.breadwallet.app.BreadApp
import com.breadwallet.ui.MainActivity
import com.kaspersky.kaspresso.testcases.api.testcase.TestCase
import org.junit.Rule

open class BaseTestCase : TestCase() {

    @get:Rule
    val activityTestRule = ActivityTestRule(MainActivity::class.java, true, false)

    fun launchActivity(intent: Intent? = null) {
        activityTestRule.launchActivity(intent)
    }

    fun clearData() {
        activityTestRule.activity.runOnUiThread {
            (activityTestRule.activity.application as BreadApp).clearApplicationData()
        }
    }
}
