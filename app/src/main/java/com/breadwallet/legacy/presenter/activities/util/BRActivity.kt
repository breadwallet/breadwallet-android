/**
 * BreadWallet
 *
 * Created by Mihail Gutan <mihail@breadwallet.com> on 5/23/17.
 * Copyright (c) 2017 breadwallet LLC
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
package com.breadwallet.legacy.presenter.activities.util

import android.graphics.Point
import android.os.Bundle
import android.util.Log
import android.view.MotionEvent
import androidx.appcompat.app.AppCompatActivity
import com.breadwallet.R
import com.breadwallet.app.BreadApp
import com.breadwallet.app.BreadApp.Companion.setBreadContext
import com.breadwallet.tools.animation.BRDialog
import com.breadwallet.tools.manager.BRSharedPrefs.getScreenHeight
import com.breadwallet.tools.manager.BRSharedPrefs.putScreenHeight
import com.breadwallet.tools.manager.BRSharedPrefs.putScreenWidth
import com.breadwallet.tools.security.BrdUserState.Disabled
import com.breadwallet.tools.security.BrdUserState.Locked
import com.breadwallet.tools.security.BrdUserManager
import com.breadwallet.util.errorHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers.Default
import kotlinx.coroutines.Dispatchers.Main
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.cancelChildren
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.take
import org.kodein.di.erased.instance

private const val TAG = "BRActivity"

@Suppress("TooManyFunctions")
abstract class BRActivity : AppCompatActivity() {

    protected val userManager: BrdUserManager by lazy {
        BreadApp.getKodeinInstance().instance<BrdUserManager>()
    }

    private val resumedScope = CoroutineScope(
        Default + SupervisorJob() + errorHandler("resumedScope")
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        saveScreenSizesIfNeeded()
    }

    override fun onDestroy() {
        super.onDestroy()
        resumedScope.cancel()
    }

    override fun onResume() {
        super.onResume()

        userManager.stateChanges()
            .filter { it is Disabled || it is Locked }
            .take(1)
            .onEach { finish() }
            .flowOn(Main)
            .launchIn(resumedScope)

        setBreadContext(this)
    }

    override fun onPause() {
        super.onPause()
        resumedScope.coroutineContext.cancelChildren()

        setBreadContext(null)
    }

    private fun saveScreenSizesIfNeeded() {
        if (getScreenHeight() == 0) {
            Log.d(TAG, "saveScreenSizesIfNeeded: saving screen sizes.")
            val display = windowManager.defaultDisplay
            val size = Point()
            display.getSize(size)
            putScreenHeight(size.y)
            putScreenWidth(size.x)
        }
    }

    /**
     * Check if there is an overlay view over the screen, if an
     * overlay view is found the event won't be dispatched and
     * a dialog with a warning will be shown.
     *
     * @param event The touch screen event.
     * @return boolean Return true if this event was consumed or if an overlay view was found.
     */
    protected fun checkOverlayAndDispatchTouchEvent(event: MotionEvent): Boolean {
        // Filter obscured touches by consuming them.
        if (event.flags and MotionEvent.FLAG_WINDOW_IS_OBSCURED != 0) {
            if (event.action == MotionEvent.ACTION_UP) {
                BRDialog.showSimpleDialog(
                    this, getString(R.string.Android_screenAlteringTitle),
                    getString(R.string.Android_screenAlteringMessage)
                )
            }
            return true
        }
        return super.dispatchTouchEvent(event)
    }
}
