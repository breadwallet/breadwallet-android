/**
 * BreadWallet
 *
 * Created by Pablo Budelli <pablo.budelli@breadwallet.com> on 5/31/19.
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
package com.breadwallet.ui.browser

import android.content.Context
import android.content.Intent
import com.breadwallet.tools.util.BRConstants

/**
 * Platform content web browser.
 */
class PlatformBrowserActivity : WebViewActivity() {

    companion object {
        /**
         * Start a new BrdBrowserActivity at the given url. Optionally you can include animation to
         * transition the activity in and out.
         */
        @JvmOverloads fun start(context: Context,
                  url: String,
                  returnEnterAnimation: Int? = 0,
                  returnExitAnimation: Int? = 0) {
            val intent = Intent(context, PlatformBrowserActivity::class.java)
            intent.putExtra(BRConstants.EXTRA_URL, url)
            if (returnEnterAnimation != 0 && returnExitAnimation != 0) {
                intent.putExtra(EXTRA_ENTER_TRANSITION, returnEnterAnimation)
                intent.putExtra(EXTRA_EXIT_TRANSITION, returnExitAnimation)
            }
            context.startActivity(intent)
        }
    }
}
