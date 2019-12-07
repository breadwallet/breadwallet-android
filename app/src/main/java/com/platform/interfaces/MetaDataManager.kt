package com.platform.interfaces

import com.breadwallet.app.BreadApp
import com.breadwallet.crypto.WalletManagerMode
import com.breadwallet.logger.logDebug
import com.breadwallet.logger.logError
import com.breadwallet.logger.logInfo
import com.breadwallet.protocols.messageexchange.entities.PairingMetaData
import com.breadwallet.tools.crypto.CryptoHelper
import com.breadwallet.tools.util.TokenUtil
import com.breadwallet.tools.util.Utils
import com.platform.entities.TxMetaData
import com.platform.entities.TokenListMetaData
import com.platform.entities.WalletInfoData
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.onStart
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import java.util.Date

@Suppress("TooManyFunctions")
@UseExperimental(ExperimentalCoroutinesApi::class, FlowPreview::class)
class MetaDataManager(
    private val storeProvider: KVStoreProvider
) : WalletProvider, AccountMetaDataProvider {

    companion object {
        private const val KEY_WALLET_INFO = "wallet-info"
        private const val KEY_TOKEN_LIST_META_DATA = "token-list-metadata"
        private const val KEY_MSG_INBOX = "encrypted-message-inbox-metadata"
        private const val KEY_ASSET_INDEX = "asset-index"
        private const val CURSOR = "cursor"
        private const val ENABLED_ASSET_IDS = "enabledAssetIds"
        private const val CLASS_VERSION = "classVersion"
        private const val CLASS_VERSION_ASSET_IDS = 2
        private const val CLASS_VERSION_MSG_INBOX = 1
        private const val TX_META_DATA_KEY_PREFIX = "txn2-"
        private const val PAIRING_META_DATA_KEY_PREFIX = "pwd-"
    }

    override fun create(accountCreationDate: Date) {
        storeProvider.put(
            KEY_WALLET_INFO,
            WalletInfoData(
                creationDate = accountCreationDate.time.toInt()
            ).toJSON()
        )
        putEnabledWallets(BreadApp.getDefaultEnabledWallets())
        logInfo("MetaDataManager created successfully")
    }

    override fun recoverAll(migrate: Boolean) = flow {
        val syncResult = storeProvider.syncAll(migrate)
        if (migrate) {
            migrateTokenList()
        }
        emit(syncResult)
    }

    override fun walletInfo(): Flow<WalletInfoData> =
        storeProvider.keyFlow(KEY_WALLET_INFO)
            .mapLatest { WalletInfoData.fromJsonObject(it) }
            .onStart {
                emit(
                    getOrSync(
                        KEY_WALLET_INFO
                    )
                    { WalletInfoData().toJSON() }
                    !!.run { WalletInfoData.fromJsonObject(this) }
                )
            }
            .distinctUntilChanged()

    override fun getWalletInfoUnsafe(): WalletInfoData? =
        try {
            storeProvider.get(KEY_WALLET_INFO)
                ?.run { WalletInfoData.fromJsonObject(this) }
        } catch (ex: JSONException) {
            logError("$ex")
            null
        }

    override fun enabledWallets(): Flow<List<String>> =
        storeProvider.keyFlow(KEY_ASSET_INDEX)
            .mapLatest {
                jsonToEnabledWallets(it)
            }
            .onStart {
                emit(
                    getOrSync(
                        KEY_ASSET_INDEX
                    )
                    { enabledWalletsToJSON(BreadApp.getDefaultEnabledWallets()) }
                    !!.run {
                        jsonToEnabledWallets(this)
                    }
                )
            }
            .distinctUntilChanged()

    override fun enableWallet(currencyId: String): Flow<Unit> = flow {
        val enabledWallets = getEnabledWallets()?.toMutableList() ?: mutableListOf()
        if (!enabledWallets.contains(currencyId)) {
            putEnabledWallets(
                enabledWallets.apply { add(currencyId) }
            )
        }
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

    override fun reorderWallets(currencyIds: List<String>) = flow {
        putEnabledWallets(currencyIds)
        emit(Unit)
    }

    override fun walletModes(): Flow<Map<String, WalletManagerMode>> =
        walletInfo()
            .mapLatest { it.connectionModes }
            .distinctUntilChanged()

    override suspend fun putWalletMode(currencyId: String, mode: WalletManagerMode) {
        var walletInfo = walletInfo().first()
        if (walletInfo.connectionModes[currencyId] == mode) return
        val connectionModes = walletInfo.connectionModes.toMutableMap().apply {
            put(currencyId, mode)
        }

        walletInfo = walletInfo.copy(connectionModes = connectionModes)
        storeProvider.put(
            KEY_WALLET_INFO,
            walletInfo.toJSON()
        )
    }

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
                val rawPubKey = CryptoHelper.hexDecode(pairingData.publicKeyHex)
                storeProvider.put(pairingKey(rawPubKey), pairingData.toJSON())
            }
        }

    override fun getLastCursor(): String? =
        try {
            storeProvider.get(KEY_MSG_INBOX)?.getString(CURSOR)
        } catch (ex: JSONException) {
            logError("Error getting last cursor", ex)
            null
        }

    override fun putLastCursor(lastCursor: String): Boolean =
        storeProvider.put(
            KEY_MSG_INBOX,
            JSONObject(
                mapOf(
                    CLASS_VERSION to CLASS_VERSION_MSG_INBOX,
                    CURSOR to lastCursor
                )
            )
        )

    override fun txMetaData(transactionHash: String): Flow<TxMetaData> =
        storeProvider.keyFlow(getTxMetaDataKey(transactionHash))
            .mapLatest {
                TxMetaData.fromJsonObject(it)
            }
            .onStart {
                val key = getTxMetaDataKey(transactionHash)
                var metaData = getOrSync(key)
                if (metaData == null) {
                    metaData = getOrSync(getTxMetaDataKey(transactionHash, true))
                        ?.apply {
                            storeProvider.put(key, this)
                        }
                }
                metaData?.run {
                    emit(
                        TxMetaData.fromJsonObject(this)
                    )
                }
            }
            .distinctUntilChanged()

    override fun getTxMetaData(txHash: String): TxMetaData? {
        val key = getTxMetaDataKey(txHash)
        var metaData = storeProvider.get(key)
        if (metaData == null) {
            metaData = storeProvider.get(getTxMetaDataKey(txHash, true))
                ?.apply {
                    storeProvider.put(key, this)
                }
        }
        return metaData?.run { TxMetaData.fromJsonObject(this) }
    }

    override suspend fun putTxMetaData(newTxMetaData: TxMetaData, txHash: String) {
        var txMetaData = getTxMetaData(txHash)

        var needsUpdate = false
        if (txMetaData == null) {
            needsUpdate = true
            txMetaData = newTxMetaData
        } else {
            if (newTxMetaData.comment != txMetaData.comment && newTxMetaData != null) {
                txMetaData = txMetaData.copy(
                    comment = newTxMetaData.comment
                )
                needsUpdate = true
            }
            if (txMetaData.exchangeRate == 0.0 && newTxMetaData.exchangeRate != 0.0) {
                txMetaData = txMetaData.copy(
                    exchangeRate = newTxMetaData.exchangeRate
                )
                needsUpdate = true
            }
        }

        if (needsUpdate) {
            val key = getTxMetaDataKey(txHash)
            storeProvider.put(key, txMetaData.toJSON())
        }
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
        enabledWalletsToJSON(enabledWallets)
            .also {
                storeProvider.put(KEY_ASSET_INDEX, it)
            }

    private fun migrateTokenList() {
        if (storeProvider.get(KEY_ASSET_INDEX) != null) {
            logDebug("Metadata for $KEY_ASSET_INDEX found, no migration needed.")
            return
        }
        logDebug("Migrating $KEY_TOKEN_LIST_META_DATA to $KEY_ASSET_INDEX.")
        try {
            val tokenListMetaData = storeProvider.get(KEY_TOKEN_LIST_META_DATA)
                ?.run(::TokenListMetaData)

            if (tokenListMetaData == null) {
                logDebug("No value for $KEY_TOKEN_LIST_META_DATA found.")
                return
            }

            val currencyCodeToToken =
                TokenUtil.getTokenItems(BreadApp.getBreadContext())
                    ?.associateBy { it.symbol.toLowerCase() } ?: emptyMap()

            tokenListMetaData.enabledCurrencies
                .mapNotNull {
                    currencyCodeToToken[it.symbol.toLowerCase()]?.currencyId
                }
                .apply {
                    if (isNotEmpty()) {
                        putEnabledWallets(this)
                        logDebug("Migration to $KEY_ASSET_INDEX completed.")
                    }
                }
        } catch (ex: JSONException) {
            logError("$ex")
        }
    }

    private suspend fun getOrSync(
        key: String,
        defaultProducer: (() -> JSONObject)? = null
    ): JSONObject? {
        var value = storeProvider.get(key) ?: storeProvider.sync(key)
        return when (value) {
            null -> {
                defaultProducer
                    ?.invoke()
                    ?.also {
                        logDebug("Sync returned null. Putting default value: $key -> $it")
                        storeProvider.put(key, it)
                    }
            }
            else -> value
        }
    }

    private fun enabledWalletsToJSON(wallets: List<String>) =
        JSONObject(
            mapOf(
                CLASS_VERSION to CLASS_VERSION_ASSET_IDS,
                ENABLED_ASSET_IDS to wallets.toJSON()
            )
        )

    private fun jsonToEnabledWallets(walletsJSON: JSONObject) =
        walletsJSON
            .getJSONArray(ENABLED_ASSET_IDS)
            .run {
                List(length(), this::getString)
            }

    private fun List<String>.toJSON(): JSONArray {
        val json = JSONArray()
        forEach { json.put(it) }
        return json
    }

    private fun getTxMetaDataKey(txHash: String, legacy: Boolean = false): String =
        TX_META_DATA_KEY_PREFIX + if (legacy) {
            Utils.bytesToHex(CryptoHelper.sha256(txHash.toByteArray())!!)
        } else {
            CryptoHelper.hexDecode(txHash)
                .apply {
                    reverse()
                }
                .run(CryptoHelper::sha256)
                ?.run(CryptoHelper::hexEncode)
                ?: ""
        }

    private fun pairingKey(pubKey: ByteArray): String =
        PAIRING_META_DATA_KEY_PREFIX + CryptoHelper.hexEncode(CryptoHelper.sha256(pubKey)!!)
}
