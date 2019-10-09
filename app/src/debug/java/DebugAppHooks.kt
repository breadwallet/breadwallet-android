package com.breadwallet

import com.github.anrwatchdog.ANRWatchDog

internal fun BreadApp.installHooks() {
    ANRWatchDog().start()
}
