package com.breadwallet.util

import com.agoda.kakao.screen.Screen.Companion.onScreen
import com.breadwallet.R
import com.kaspersky.kaspresso.testcases.core.testcontext.TestContext

const val APPLICATION_PIN = "000000"

fun <T> TestContext<T>.setApplicationPin() {
    step("Set pin") {
        onScreen<InputPinScreen> {
            title.hasText(R.string.UpdatePin_createTitle)
            keyboard.input(APPLICATION_PIN)
        }
    }

    step("Confirm pin") {
        onScreen<InputPinScreen> {
            title.hasText(R.string.UpdatePin_createTitleConfirm)
            keyboard.input(APPLICATION_PIN)
        }
    }
}
