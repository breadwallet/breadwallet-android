package com.platform.jsbridge

import android.content.Context
import android.webkit.JavascriptInterface
import com.platform.APIClient
import com.platform.kvstore.RemoteKVStore
import com.platform.kvstore.ReplicatedKVStore
import com.platform.sqlite.KVItem
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Locale

class KVStoreJs(
    private val nativePromiseFactory: NativePromiseFactory,
    private val context: Context
) : JsApi {
    private val store: ReplicatedKVStore
        get() {
            val remote = RemoteKVStore.getInstance(APIClient.getInstance(context))
            return ReplicatedKVStore.getInstance(context, remote)
        }

    companion object {
        const val KEY_VERSION = "version"
        const val KEY_LAST_MODIFIED = "lastModified"
    }

    @JavascriptInterface
    fun get(
        key: String
    ) = nativePromiseFactory.create {
        val completionObj = store.get(getKey(key), 0)
        val kv = completionObj.kv

        check(kv != null && kv.deleted == 0) {
            "KVStore does not contain an entry for $key"
        }

        try {
            val jsonVal = kv.value.toString(Charsets.UTF_8)
            JSONObject(jsonVal).decorate(kv.version, kv.time)
        } catch (e: Exception) {
            store.delete(getKey(key), kv.version)
            throw e
        }
    }

    @JavascriptInterface
    fun put(
        key: String,
        valueStr: String,
        version: Long
    ) = nativePromiseFactory.create {
        val completionObj = store.set(
            KVItem(
                version,
                0,
                getKey(key),
                valueStr.toByteArray(Charsets.UTF_8),
                System.currentTimeMillis(),
                0
            )
        )

        check(completionObj.err == null) {
            "KVStore error on setting the key: $key, err: ${completionObj.err}"
        }

        JSONObject(valueStr).decorate(completionObj.version, completionObj.time)
    }

    @JavascriptInterface
    fun delete(
        key: String,
        version: Long
    ) = nativePromiseFactory.create {
        val completionObj = store.delete(getKey(key), version)
        check(completionObj != null && completionObj.err == null) {
            "KVStore error deleting the key: $key, err: ${completionObj.err}"
        }
        JSONObject().decorate(completionObj.version, completionObj.time)
    }

    private fun JSONObject.decorate(version: Long, time: Long): JSONObject {
        put(KEY_VERSION, version)
        val dateTime = SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss z", Locale.US).format(time)
        return put(KEY_LAST_MODIFIED, dateTime)
    }

    private fun getKey(key: String) = "plat-$key"
}