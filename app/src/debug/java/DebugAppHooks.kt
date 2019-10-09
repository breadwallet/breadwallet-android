package com.breadwallet

import com.breadwallet.app.BreadApp
import com.github.anrwatchdog.ANRWatchDog

internal fun BreadApp.installHooks() {
    ANRWatchDog().start()
}
