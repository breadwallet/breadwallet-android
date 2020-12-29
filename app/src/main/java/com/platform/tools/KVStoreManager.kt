/**
 * BreadWallet
 *
 * Created by Mihail Gutan <mihail@breadwallet.com> on 6/22/17.
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
package com.platform.tools

import android.content.Context
import com.breadwallet.logger.logDebug
import com.breadwallet.logger.logError
import com.breadwallet.tools.manager.BRReportsManager
import com.breadwallet.tools.util.BRCompressor
import com.breadwallet.tools.util.netRetry
import com.platform.APIClient
import com.platform.interfaces.KVStoreProvider
import com.platform.kvstore.CompletionObject
import com.platform.kvstore.RemoteKVStore
import com.platform.kvstore.ReplicatedKVStore
import com.platform.sqlite.KVItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.BroadcastChannel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.IOException
import java.util.concurrent.ConcurrentHashMap

@Suppress("TooManyFunctions", "TooGenericExceptionCaught")
class KVStoreManager(
    private val context: Context
) : KVStoreProvider {

    // TODO: Compose with data store interface passed in constructor

    companion object {
        private const val SYNC_KEY_NUM_RETRY = 3
        private const val SYNC_KEY_TIMEOUT_MS = 15_000L
        private val mutex = Mutex()
    }

    private val keyChannelMap = ConcurrentHashMap<String, BroadcastChannel<JSONObject>>()

    override fun get(key: String): JSONObject? =
        getData(context, key)?.run { JSONObject(String(this)) }

    override fun put(key: String, value: JSONObject): Boolean {
        logDebug("put $key -> $value")
        val valueStr = value.toString().toByteArray()

        if (valueStr.isEmpty()) {
            return false
        }
        val completionObject = setData(context, valueStr, key)
        return when (completionObject?.err) {
            null -> {
                keyChannelMap.getSafe(key).offer(value)
                true
            }
            else -> {
                logError("Error setting value for key: $key, err: ${completionObject.err}")
                false
            }
        }
    }

    override fun getKeys(): List<String> {
        return getReplicatedKvStore(context).rawKVs.map(KVItem::key)
    }

    override suspend fun sync(key: String): JSONObject? {
        var value: JSONObject? = null
        try {
            // TODO: ReplicatedKVStore swallows exceptions, so retry logic is moot
            // At some point, need to reconsider the interface between these two layers
            withContext(Dispatchers.IO) {
                mutex.withLock {
                    netRetry(SYNC_KEY_NUM_RETRY, SYNC_KEY_TIMEOUT_MS) {
                        getReplicatedKvStore(context).syncKey(key)
                    }
                }
            }
            value = get(key)?.also { keyChannelMap.getSafe(key).offer(it) }
        } catch (ex: Exception) {
            logError("Sync exception for key: $key", ex)
        }
        return value
    }

    override suspend fun syncAll(syncOrder: List<String>): Boolean =
        withContext(Dispatchers.IO) {
            mutex.withLock {
                getReplicatedKvStore(context).syncAllKeys(syncOrder)
            }
        }.also { syncSuccess ->
            if (syncSuccess) {
                keyChannelMap.keys.forEach { key ->
                    get(key)?.let { value ->
                        keyChannelMap.getSafe(key).offer(value)
                    }
                }
            }
        }

    override fun keyFlow(key: String): Flow<JSONObject> =
        keyChannelMap.getSafe(key).asFlow().onStart { get(key)?.let { emit(it) } }

    override fun keysFlow(): Flow<List<String>> {
        val kvstore = getReplicatedKvStore(context)
        return if (mutex.isLocked) {
            flow {
                mutex.withLock {  }
                emit(kvstore.rawKVs.map(KVItem::key))
            }
        } else {
            flowOf(kvstore.rawKVs.map(KVItem::key))
        }
    }

    private fun setData(context: Context, data: ByteArray, key: String?): CompletionObject? =
        try {
            val compressed = BRCompressor.bz2Compress(data)
            val kvStore = getReplicatedKvStore(context)
            val localVer = kvStore.localVersion(key).version
            val removeVer = kvStore.remoteVersion(key)

            kvStore.set(localVer, removeVer, key, compressed, System.currentTimeMillis(), 0)
        } catch (e: IOException) {
            BRReportsManager.reportBug(e)
            null
        }

    private fun getData(context: Context, key: String): ByteArray? {
        val kvStore = getReplicatedKvStore(context)
        val ver = kvStore.localVersion(key).version
        val obj = kvStore.get(key, ver)
        if (obj.kv == null) {
            return null
        }
        return when (val decompressed = BRCompressor.bz2Extract(obj.kv.value)) {
            null -> {
                logError("Decompression failed for $key: ${obj.kv.value?.let { String(it) }}")
                null
            }
            else -> decompressed
        }
    }

    private fun getReplicatedKvStore(context: Context): ReplicatedKVStore {
        val remoteKVStore = RemoteKVStore.getInstance(APIClient.getInstance(context))
        return ReplicatedKVStore.getInstance(context, remoteKVStore)
    }

    private fun ConcurrentHashMap<String, BroadcastChannel<JSONObject>>.getSafe(key: String) = run {
        getOrElse(key, { BroadcastChannel(Channel.BUFFERED) }).also { putIfAbsent(key, it) }
    }
}
