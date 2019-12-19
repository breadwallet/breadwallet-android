package com.platform.interfaces

import kotlinx.coroutines.flow.Flow
import org.json.JSONObject

/** Provides access to a Key-Value Store serving [JSONObject]. */
interface KVStoreProvider {
    /** Get value for [key]. */
    fun get(key: String): JSONObject?

    /** Put [value] for [key]. */
    fun put(key: String, value: JSONObject): Boolean

    /** Syncs the value for [key] and returns it, null if sync failed. */
    suspend fun sync(key: String): JSONObject?

    /** Syncs entire data store. If [migrateEncryption] is true, will migrate KV entries encryption. */
    suspend fun syncAll(migrateEncryption: Boolean, syncOrder: List<String> = listOf()): Boolean

    /** Returns a [Flow] for a given [key]. */
    fun keyFlow(key: String): Flow<JSONObject>
}