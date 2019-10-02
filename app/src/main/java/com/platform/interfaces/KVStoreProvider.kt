package com.platform.interfaces

import kotlinx.coroutines.flow.Flow
import org.json.JSONObject

/** Provides access to a Key-Value Store serving [JSONObject]. */
interface KVStoreProvider {
    /** Get value for given [key]. */
    fun get(key: String): JSONObject?

    /** Put [value] for given [key] */
    fun put(key: String, value: JSONObject): Boolean

    /** Syncs the value for given [key]. */
    fun sync(key: String)

    /** Syncs entire data store. */
    fun syncAll(): Boolean

    /** Returns a [Flow] for a given [key]. */
    fun keyFlow(key: String): Flow<JSONObject>
}