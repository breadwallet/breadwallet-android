package com.platform.util

import org.json.JSONObject

/**
 * Returns the value mapped by name of null if it doesn't exist.
 */
fun JSONObject.getStringOrNull(name: String): String? =
        if (this.has(name)) this.getString(name) else null