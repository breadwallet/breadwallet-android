package com.platform.interfaces

import kotlinx.coroutines.flow.Flow
import org.json.JSONObject

/** Provides access to a Key-Value Store serving [JSONObject]. */
interface KVStoreProvider {
    /** Get value for given [key]. */
    fun get(key: String): JSONObject?

    /** Put [value] for given [key] */
    fun put(key: String, value: JSONObject): Boolean

    /** Syncs the value for given [key] and returns it, null if sync failed. */
    suspend fun sync(key: String): JSONObject?

    /** Syncs entire data store. */
    suspend fun syncAll(): Boolean

    /** Returns a [Flow] for a given [key]. */
    fun keyFlow(key: String): Flow<JSONObject>
}