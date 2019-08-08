package com.breadwallet.ui.browser

import android.content.Context
import android.content.Intent
import com.breadwallet.BreadApp
import com.breadwallet.tools.util.BRConstants

/**
 * Platform content web browser.
 */
class PlatformBrowserActivity: WebViewActivity() {

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