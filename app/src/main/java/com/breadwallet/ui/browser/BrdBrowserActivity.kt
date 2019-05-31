package com.breadwallet.ui.browser

import android.content.Context
import android.content.Intent
import com.breadwallet.tools.util.BRConstants
import com.platform.middlewares.plugins.LinkPlugin
import org.json.JSONObject

/**
 * General purpose web browser activity.
 */
class BrdBrowserActivity : WebViewActivity() {

    companion object {
        /**
         * Start a new BrdBrowserActivity at the given url.
         */
        fun startWithUrl(context: Context, url: String) {
            val getInt = Intent(context, BrdBrowserActivity::class.java)
            getInt.putExtra(BRConstants.EXTRA_URL, url)
            context.startActivity(getInt)
        }

        /**
         * Start a new BrdBrowserActivity with a JSONObject containing required data to perform
         * POST or GET such as url, headers and request's body (optional only for POST).
         */
        fun startJson(context: Context, jsonParams: JSONObject) {
            val getInt = Intent(context, BrdBrowserActivity::class.java)
            getInt.putExtra(EXTRA_JSON_PARAM, jsonParams.toString())
            context.startActivity(getInt)
        }
    }

    override fun onPause() {
        super.onPause()
        LinkPlugin.hasBrowser = false
    }
}