/**
 * BreadWallet
 *
 *
 * Created by Mihail Gutan on <mihail></mihail>@breadwallet.com> 6/22/17.
 * Copyright (c) 2017 breadwallet LLC
 *
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
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
import android.text.format.DateUtils
import com.breadwallet.BreadApp

import com.breadwallet.core.BRCoreKey
import com.breadwallet.protocols.messageexchange.entities.PairingMetaData
import com.breadwallet.repository.RatesRepository
import com.breadwallet.tools.crypto.CryptoHelper
import com.breadwallet.tools.manager.BRReportsManager
import com.breadwallet.tools.manager.BRSharedPrefs
import com.breadwallet.tools.util.BRCompressor
import com.breadwallet.tools.util.BRConstants
import com.breadwallet.tools.util.Utils
import com.breadwallet.ui.util.logDebug
import com.breadwallet.ui.util.logError
import com.breadwallet.ui.util.logWarning
import com.breadwallet.wallet.abstracts.BaseWalletManager
import com.breadwallet.wallet.wallets.CryptoTransaction
import com.breadwallet.wallet.wallets.bitcoin.WalletBchManager
import com.breadwallet.wallet.wallets.bitcoin.WalletBitcoinManager
import com.breadwallet.wallet.wallets.ethereum.WalletEthManager
import com.breadwallet.wallet.wallets.ethereum.WalletTokenManager
import com.platform.APIClient
import com.platform.entities.TokenListMetaData
import com.platform.entities.TokenListMetaData.TokenInfo
import com.platform.entities.TxMetaData
import com.platform.entities.WalletInfoData
import com.platform.interfaces.AccountMetaDataManager
import com.platform.interfaces.WalletMetaData
import com.platform.interfaces.getWalletMetaData
import com.platform.kvstore.CompletionObject
import com.platform.kvstore.RemoteKVStore
import com.platform.kvstore.ReplicatedKVStore
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.channels.BroadcastChannel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.filterNotNull

import org.json.JSONException
import org.json.JSONObject

import java.io.IOException
import java.math.BigDecimal

@UseExperimental(ExperimentalCoroutinesApi::class, FlowPreview::class)
@Suppress("TooManyFunctions")
internal object KVStoreManager : AccountMetaDataManager {
    private const val KEY_WALLET_INFO = "wallet-info"
    private const val KEY_SEGWIT_META_DATA = "segwit-metadata"
    private const val KEY_TOKEN_LIST_META_DATA = "token-list-metadata"
    private const val KEY_CURSOR = "lastCursor"
    private const val MY_BREAD = "My Bread"
    private const val CURSOR = "cursor"
    private const val TX_META_DATA_KEY_PREFIX = "txn2-"
    private const val PAIRING_META_DATA_KEY_PREFIX = "pwd-"
    private const val EXCHANGE_RATE_SCALE = 8

    // TODO: Default currencies should be elsewhere
    private val DEFAULT_WALLETS = listOf(
        TokenInfo(WalletBitcoinManager.BITCOIN_CURRENCY_CODE, false, null),
        TokenInfo(WalletBchManager.BITCASH_CURRENCY_CODE, false, null),
        TokenInfo(WalletEthManager.ETH_CURRENCY_CODE, false, null),
        TokenInfo(WalletTokenManager.BRD_CURRENCY_CODE, true, WalletTokenManager.BRD_CONTRACT_ADDRESS),
        TokenInfo(WalletTokenManager.DAI_CURRENCY_CODE, true, WalletTokenManager.DAI_CONTRACT_ADDRESS),
        TokenInfo(WalletTokenManager.TUSD_CURRENCY_CODE, true, WalletTokenManager.TUSD_CONTRACT_ADDRESS)
    )

    private val walletMetaDataChannel = BroadcastChannel<List<WalletMetaData>>(Channel.BUFFERED)

    // TODO: If we have some form of dependency injection:
    // 1. Expose this as a top-level resource
    // 2. Compose with a kvstore interface passed into constructor (e.g., replicated data store)
    // 3. Convert all clients to use via injection + interface

    override fun walletsMetaData(): Flow<List<WalletMetaData>> =
        walletMetaDataChannel
            .asFlow()
            .onStart{
                getTokenListMetaData(BreadApp.getBreadContext())?.getWalletMetaData()?.let { emit(it) }
            }

    override fun walletMetaData(currencyCode: String): Flow<WalletMetaData> =
        walletMetaDataChannel
                .asFlow()
                .onStart{
                    getTokenListMetaData(BreadApp.getBreadContext())?.getWalletMetaData()?.let { emit(it) }
                }
                .mapLatest { wallets ->
                    wallets.firstOrNull { it.currencyCode.equals(currencyCode, true) }
                }
                .filterNotNull()

    /** Retrieves [WalletInfoData] */
    fun getWalletInfo(context: Context): WalletInfoData? =
        try {
            getJSONData(context, KEY_WALLET_INFO)
                ?.run(::WalletInfoData)
                ?.also { logDebug("${it.creationDate}, name: ${it.name}") }
        } catch (ex: JSONException) {
            logError("$ex")
            null
        }

    /** Persists [WalletInfoData] */
    fun putWalletInfo(app: Context, newInfo: WalletInfoData) {
        var old = getWalletInfo(app) ?: WalletInfoData()

        // Add all the params that we want to change
        if (newInfo.classVersion != 0) {
            old.classVersion = newInfo.classVersion
        }
        if (newInfo.creationDate != 0) {
            old.creationDate = newInfo.creationDate
        }
        if (newInfo.name != null) {
            old.name = newInfo.name
        }

        // Sanity check
        if (old.classVersion == 0) {
            old.classVersion = 1
        }
        if (old.name != null) {
            old.name = MY_BREAD
        }

        putJSONData(app, KEY_WALLET_INFO, old.toJSON())
    }

    /** Synchronize wallet info with KV store. */
    fun syncWalletInfo(context: Context) = getReplicatedKvStore(context).syncKey(KEY_WALLET_INFO)

    /** Synchronize token list with KV store. */
    fun syncTokenList(context: Context) = getReplicatedKvStore(context).syncKey(KEY_TOKEN_LIST_META_DATA)

    /** Retrieves [PairingMetaData] given a public key. */
    fun getPairingMetadata(context: Context, pubKey: ByteArray): PairingMetaData? =
        try {
            val key = pairingKey(pubKey)
            getJSONData(context, key)?.run(::PairingMetaData)
        } catch (ex: JSONException) {
            logError("$ex")
            null
        }

    /** Persists the given [PairingMetaData]. */
    fun putPairingMetadata(app: Context, pairingData: PairingMetaData): Boolean =
        when (pairingData.publicKeyHex) {
            null -> {
                logError("pairingData.getPublicKeyHex() is null!")
                false
            }
            else -> {
                val rawPubKey = BRCoreKey.decodeHex(pairingData.publicKeyHex)
                putJSONData(app, pairingKey(rawPubKey), pairingData.toJSON())
            }
        }

    /** Retrieves the last cursor (used in retrieving inbox messages). */
    fun getLastCursor(context: Context): String? =
        try {
            getJSONData(context, KEY_CURSOR)?.getString(CURSOR)
        } catch (ex: JSONException) {
            logError("$ex")
            null
        }

    /** Persists the last cursor (used in retrieving inbox messages). */
    fun putLastCursor(app: Context, lastCursor: String): Boolean =
            putJSONData(app, KEY_CURSOR, JSONObject(mapOf(CURSOR to lastCursor)))

    // Expensive, takes ~ 20 milliseconds
    @Synchronized
    /** Retrieves [TokenListMetaData]. */
    fun getTokenListMetaData(context: Context): TokenListMetaData? =
        try {
            getJSONData(context, KEY_TOKEN_LIST_META_DATA)
                ?.run(::TokenListMetaData)
                ?: TokenListMetaData(DEFAULT_WALLETS)
        } catch (ex: JSONException) {
            logError("$ex")
            null
        }

    @Synchronized
    /** Persists given [TokenListMetaData]. */
    fun putTokenListMetaData(app: Context, newMetaData: TokenListMetaData): Boolean {
        var old = getTokenListMetaData(app) ?: TokenListMetaData()

        // Add all the params that we want to change
        if (newMetaData.enabledCurrencies.size > 0) {
            old.enabledCurrencies = newMetaData.enabledCurrencies
        }
        old.hiddenCurrencies = newMetaData.hiddenCurrencies

        if (putJSONData(app, KEY_TOKEN_LIST_META_DATA, old.toJSON())) {
            walletMetaDataChannel.offer(newMetaData.getWalletMetaData())
            return true
        }
        return false
    }

    /** Retrieves [TxMetaData] for given transaction hash. */
    fun getTxMetaData(context: Context, txHash: ByteArray): TxMetaData? =
        getJSONData(context, getTxMetaDataKey(txHash))?.run(::TxMetaData)

    /** Persist given [TxMetaData] for transaction hash, but ONLY if the comment or exchange rate has changed. */
    fun putTxMetaData(context: Context, newTxMetaData: TxMetaData, txHash: ByteArray) {
        var oldTxMetaData = getTxMetaData(context, txHash)

        var needsUpdate = false
        if (oldTxMetaData == null) {
            needsUpdate = true
            oldTxMetaData = newTxMetaData
        } else {
            val finalComment = getFinalValue(newTxMetaData.comment, oldTxMetaData.comment)
            if (finalComment != null) {
                oldTxMetaData.comment = finalComment
                needsUpdate = true
            }
            // If rate wasn't persisted at time of creating the txn, need to update it
            if (oldTxMetaData.exchangeRate == 0.0 && newTxMetaData.exchangeRate != 0.0) {
                oldTxMetaData.exchangeRate = newTxMetaData.exchangeRate
                needsUpdate = true
            }
        }

        if (needsUpdate) {
            val key = getTxMetaDataKey(txHash)
            logDebug("updating txMetadata for : $key")
            putJSONData(context, key, oldTxMetaData.toJSON())
        }
    }

    fun getAllTxMD(context: Context): Map<String, TxMetaData?> {
        val kvStore = getReplicatedKvStore(context)
        return kvStore.allTxMdKv
            .filter{ BRCompressor.bz2Extract(it.value) != null }
            .associate{ it.key to TxMetaData(JSONObject(String(BRCompressor.bz2Extract(it.value))))}
    }

    private fun getFinalValue(newVal: String?, oldVal: String?): String? =
        when {
            newVal == null || newVal == oldVal  -> null
            else -> newVal
        }

    // TODO: refactor to avoid wallet manager
    fun createMetadata(app: Context, wm: BaseWalletManager, tx: CryptoTransaction): TxMetaData {
        return TxMetaData().apply{
            exchangeCurrency = BRSharedPrefs.getPreferredFiatIso(app)
            val ent = RatesRepository
                                    .getInstance(app)
                                    .getCurrencyByCode(wm.currencyCode, exchangeCurrency)
            exchangeRate = when (ent == null) {
                true -> 0.0
                false -> {
                    BigDecimal(ent.rate.toDouble())
                        .setScale(EXCHANGE_RATE_SCALE, BRConstants.ROUNDING_MODE)
                        .stripTrailingZeros()
                        .toDouble()
                }
            }
            fee = wm.getTxFee(tx).toPlainString()
            txSize = tx.txSize!!.toInt()
            blockHeight = BRSharedPrefs.getLastBlockHeight(app, wm.currencyCode)
            creationTime = (System.currentTimeMillis() / DateUtils.SECOND_IN_MILLIS).toInt() // seconds
            deviceId = BRSharedPrefs.getDeviceId(app)
            classVersion = 1
        }
    }

    private fun getTxMetaDataKey(txHash: ByteArray): String =
        TX_META_DATA_KEY_PREFIX + Utils.bytesToHex(CryptoHelper.sha256(txHash)!!)

    private fun pairingKey(pubKey: ByteArray): String =
        PAIRING_META_DATA_KEY_PREFIX + BRCoreKey.encodeHex(CryptoHelper.sha256(pubKey)!!)

    private fun getReplicatedKvStore(context: Context): ReplicatedKVStore {
        val remoteKVStore = RemoteKVStore.getInstance(APIClient.getInstance(context))
        return ReplicatedKVStore.getInstance(context, remoteKVStore)
    }

    /* TODO: Hide JSON and get/set Data fns behind ReplicatedKVStore abstraction? Or at least, byte compression logic */
    private fun getJSONData(context: Context, key: String): JSONObject? =
        when (val data = getData(context, key)) {
            null -> {
                logError("Data value is null")
                null
            }
            else -> JSONObject(String(data))
        }

    private fun putJSONData(context: Context, key: String, value: JSONObject): Boolean {
        logDebug("put $key -> $value")
        val valueStr = value.toString().toByteArray()

        if (valueStr.isEmpty()) {
            logError("FAILED: result is empty")
            return false
        }
        val completionObject = setData(context, valueStr, key)
        return when (completionObject?.err) {
            null -> true
            else -> {
                logError("Error setting value for key: $key, err: ${completionObject.err}")
                false
            }
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
            logWarning("getData: value is null for key: $key")
            return null
        }
        return when(val decompressed = BRCompressor.bz2Extract(obj.kv.value)) {
            null -> {
                logError("getData: decompressed value is null")
                null
            }
            else -> decompressed
        }
    }
}

/* unused code
 public static SegWitMetaData getSegwit(Context context) {
     JSONObject json;
     byte[] data = getData(context, KEY_SEGWIT_META_DATA);
     try {
         if (data == null) {
             Log.e(TAG, "getSegwit: data value is null");
             return null;
         }
         json = new JSONObject(new String(data));
     } catch (JSONException e) {
         Log.e(TAG, "getSegwit: ", e);
         return null;
     }

     int classVersion = 0;
     int enabledAtBlockHeight = 0;

     try {
         classVersion = json.getInt(CLASS_VERSION);
         enabledAtBlockHeight = json.getInt(ENABLED_AT_BLOCK_HEIGHT);
     } catch (JSONException e) {
         Log.e(TAG, "getSegwit: ", e);
     }
     Log.d(TAG, "getSegwit: " + KEY_SEGWIT_META_DATA);
     return new SegWitMetaData(classVersion, enabledAtBlockHeight);
 }

 public static void putSegwit(Context app, SegWitMetaData segwitData) {
     JSONObject obj = new JSONObject();
     byte[] result;
     try {
         obj.put(CLASS_VERSION, segwitData.getClassVersion());
         obj.put(ENABLED_AT_BLOCK_HEIGHT, segwitData.getEnabledAtBlockHeight());
         result = obj.toString().getBytes();

     } catch (JSONException e) {
         Log.e(TAG, "putSegwit: ", e);
         return;
     }

     if (result.length == 0) {
         Log.e(TAG, "putSegwit: FAILED: result is empty");
         return;
     }
     CompletionObject completionObject = setData(app, result, KEY_SEGWIT_META_DATA);
     if (completionObject != null && completionObject.err != null) {
         Log.e(TAG, "putSegwit: Error setting value for key: " + KEY_SEGWIT_META_DATA + ",
         err: " + completionObject.err);
     }
 }
*/