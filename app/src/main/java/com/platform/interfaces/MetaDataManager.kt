package com.platform.interfaces

import android.content.Context
import android.text.format.DateUtils
import com.breadwallet.BreadApp
import com.breadwallet.core.BRCoreKey
import com.breadwallet.protocols.messageexchange.entities.PairingMetaData
import com.breadwallet.repository.RatesRepository
import com.breadwallet.tools.crypto.CryptoHelper
import com.breadwallet.tools.manager.BRSharedPrefs
import com.breadwallet.tools.util.BRConstants
import com.breadwallet.tools.util.Utils
import com.breadwallet.ui.util.logDebug
import com.breadwallet.ui.util.logError
import com.breadwallet.ui.util.logInfo
import com.breadwallet.wallet.abstracts.BaseWalletManager
import com.breadwallet.wallet.wallets.CryptoTransaction
import com.platform.entities.TxMetaData
import com.platform.entities.WalletInfoData
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.flow.flow

import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.onStart
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import java.math.BigDecimal
import java.util.Date

@Suppress("TooManyFunctions")
@UseExperimental(ExperimentalCoroutinesApi::class, FlowPreview::class)
class MetaDataManager(
    private val storeProvider: KVStoreProvider
) : WalletsProvider, AccountMetaDataProvider {

    // TODO: Decompose further

    companion object {
        private const val KEY_WALLET_INFO = "wallet-info"
        private const val KEY_SEGWIT_META_DATA = "segwit-metadata"
        private const val KEY_TOKEN_LIST_META_DATA = "token-list-metadata"
        private const val KEY_CURSOR = "lastCursor"
        private const val KEY_ASSET_INDEX = "asset-index"
        private const val MY_BREAD = "My Bread"
        private const val CURSOR = "cursor"
        private const val ENABLED_ASSET_IDS = "enabledAssetIds"
        private const val TX_META_DATA_KEY_PREFIX = "txn2-"
        private const val PAIRING_META_DATA_KEY_PREFIX = "pwd-"
        private const val EXCHANGE_RATE_SCALE = 8
    }

    override fun create(accountCreationDate: Date, defaultEnabledWallets: List<String>?) {
        WalletInfoData().apply {
            creationDate = accountCreationDate.time.toInt()
            putWalletInfo(this)
        }

        defaultEnabledWallets?.let { putEnabledWallets(it) }

        logInfo("MetaDataManager created successfully")
    }

    override fun walletInfo(): Flow<WalletInfoData> =
        storeProvider.keyFlow(KEY_WALLET_INFO)
            .mapLatest { WalletInfoData(it) }
            .onStart {
                getWalletInfoUnsafe()?.let { emit(it) }
            }

    override fun getWalletInfoUnsafe(): WalletInfoData? =
        try {
            storeProvider.get(KEY_WALLET_INFO)
                ?.run(::WalletInfoData)
                ?.also { logDebug("creationDate: ${it.creationDate}, name: ${it.name}") }
        } catch (ex: JSONException) {
            logError("$ex")
            null
        }

    // TODO: Until KVStore backend is working and network failure semantics are worked out, always
    // return default wallets
    override fun enabledWallets(): Flow<List<String>> =
        listOf(BreadApp.getDefaultEnabledWallets()).asFlow()
    /*
    storeProvider.keyFlow(KEY_ASSET_INDEX)
        .mapLatest {
            it.getJSONArray(ENABLED_ASSET_IDS).run {
                List(length(), this::getString)
            }
        }
        .onStart {
            getEnabledWallets()?.let { emit(it) } ?: emit(emptyList())
        }
     */

    override fun enableWallet(currencyId: String): Flow<Unit> = flow {
        putEnabledWallets(
            getEnabledWallets()
                ?.toMutableList()
                ?.apply { add(currencyId) }
                ?: listOf(currencyId)
        )
        emit(Unit)
    }

    override fun disableWallet(currencyId: String): Flow<Unit> = flow {
        getEnabledWallets()
            ?.toMutableList()
            ?.filter { !it.equals(currencyId, true) }
            ?.let {
                putEnabledWallets(it)
                emit(Unit)
            }
    }

    override fun syncWalletInfo() = storeProvider.sync(KEY_WALLET_INFO)

    override fun syncAssetIndex() = storeProvider.sync(KEY_ASSET_INDEX)

    override fun getPairingMetadata(pubKey: ByteArray): PairingMetaData? =
        try {
            val key = pairingKey(pubKey)
            storeProvider.get(key)?.run(::PairingMetaData)
        } catch (ex: JSONException) {
            logError("Error getting pairing metadata", ex)
            null
        }

    override fun putPairingMetadata(pairingData: PairingMetaData): Boolean =
        when (pairingData.publicKeyHex) {
            null -> {
                logError("pairingData.getPublicKeyHex() is null!")
                false
            }
            else -> {
                val rawPubKey = BRCoreKey.decodeHex(pairingData.publicKeyHex)
                storeProvider.put(pairingKey(rawPubKey), pairingData.toJSON())
            }
        }

    override fun getLastCursor(): String? =
        try {
            storeProvider.get(KEY_CURSOR)?.getString(CURSOR)
        } catch (ex: JSONException) {
            logError("Error getting last cursor", ex)
            null
        }

    override fun putLastCursor(lastCursor: String): Boolean =
        storeProvider.put(
            KEY_CURSOR, JSONObject(mapOf(CURSOR to lastCursor))
        )

    override fun getTxMetaData(txHash: ByteArray): TxMetaData? =
        storeProvider.get(getTxMetaDataKey(txHash))?.run(::TxMetaData)

    override fun putTxMetaData(newTxMetaData: TxMetaData, txHash: ByteArray) {
        var oldTxMetaData = getTxMetaData(txHash)

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
            storeProvider.put(key, oldTxMetaData.toJSON())
        }
    }

    fun List<String>.toJSON(): JSONArray {
        val json = JSONArray()
        forEach { json.put(it) }
        return json
    }

    private fun getEnabledWallets(): List<String>? =
        try {
            storeProvider.get(KEY_ASSET_INDEX)
                ?.getJSONArray(ENABLED_ASSET_IDS)
                ?.run {
                    List(length()) { getString(it) }
                }
        } catch (ex: JSONException) {
            logError("$ex")
            null
        }

    private fun putEnabledWallets(enabledWallets: List<String>) =
        JSONObject()
            .put(ENABLED_ASSET_IDS, enabledWallets.toJSON())
            .also {
                storeProvider.put(KEY_ASSET_INDEX, it)
            }

    private fun putWalletInfo(newInfo: WalletInfoData) {
        var old = getWalletInfoUnsafe() ?: WalletInfoData()

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

        storeProvider.put(KEY_WALLET_INFO, old.toJSON())
    }

    private fun getFinalValue(newVal: String?, oldVal: String?): String? =
        when (newVal) {
            null, oldVal -> null
            else -> newVal
        }

    // TODO: refactor to avoid wallet manager
    fun createMetadata(app: Context, wm: BaseWalletManager, tx: CryptoTransaction): TxMetaData {
        return TxMetaData().apply {
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
            creationTime =
                (System.currentTimeMillis() / DateUtils.SECOND_IN_MILLIS).toInt() // seconds
            deviceId = BRSharedPrefs.getDeviceId(app)
            classVersion = 1
        }
    }

    private fun getTxMetaDataKey(txHash: ByteArray): String =
        TX_META_DATA_KEY_PREFIX + Utils.bytesToHex(CryptoHelper.sha256(txHash)!!)

    private fun pairingKey(pubKey: ByteArray): String =
        PAIRING_META_DATA_KEY_PREFIX + BRCoreKey.encodeHex(CryptoHelper.sha256(pubKey)!!)
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
/*

    // Expensive, takes ~ 20 milliseconds
    //@Synchronized
    /** Retrieves [TokenListMetaData]. */
    /*
    fun getTokenListMetaData(context: Context): TokenListMetaData? =
        try {
            getJSONData(context, KEY_TOKEN_LIST_META_DATA)
                ?.run(::TokenListMetaData)
                ?: TokenListMetaData(DEFAULT_WALLETS)
        } catch (ex: JSONException) {
            logError("$ex")
            null
        }
    */


*/