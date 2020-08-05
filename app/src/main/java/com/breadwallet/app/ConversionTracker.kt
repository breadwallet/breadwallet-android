/**
 * BreadWallet
 *
 * Created by Ahsan Butt <ahsan.butt@breadwallet.com> on 7/22/2020.
 * Copyright (c) 2020 breadwallet LLC
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
package com.breadwallet.app

import com.breadwallet.breadbox.BreadBox
import com.breadwallet.tools.manager.BRSharedPrefs
import com.platform.util.AppReviewPromptManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.flow.flatMapMerge
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach

class ConversionTracker(
    private val breadBox: BreadBox
) {

    fun start(scope: CoroutineScope) {
        if (!shouldTrackConversions()) return

        BRSharedPrefs.trackedConversionChanges
            .flatMapMerge {
                it.entries.asFlow()
            }
            .flatMapMerge { (currencyCode, trackedConversions) ->
                breadBox.walletTransfers(currencyCode).onEach { transfers ->
                    trackedConversions.forEach { conversion ->
                        val transfer = transfers.find { conversion.isTriggered(it) }
                        if (transfer != null) {
                            BRSharedPrefs.appRatePromptShouldPrompt = true
                            BRSharedPrefs.removeTrackedConversion(conversion)
                        }
                    }
                }
            }
            .launchIn(scope)
    }

    fun track(conversion: Conversion) {
        if (!shouldTrackConversions()) return
        BRSharedPrefs.putTrackedConversion(conversion)
    }

    private fun shouldTrackConversions(): Boolean = AppReviewPromptManager.shouldTrackConversions()
}
