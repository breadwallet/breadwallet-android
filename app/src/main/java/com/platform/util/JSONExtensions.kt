package com.platform.util

import org.json.JSONObject

/**
 * Returns the value mapped by name of null if it doesn't exist.
 */
fun JSONObject.getStringOrNull(name: String): String? =
    if (this.has(name)) this.getString(name) else null

/** Returns the value mapped by name or [default] if it doesn't exist. */
fun JSONObject.getIntOrDefault(name: String, default: Int = 0) =
    if (has(name)) getInt(name) else default

/** Returns the value mapped by name or [default] if it doesn't exist. */
fun JSONObject.getDoubleOrDefault(name: String, default: Double = 0.0) =
    if (has(name)) getDouble(name) else default
