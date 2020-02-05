/**
 * BreadWallet
 *
 * Created by Ahsan Butt <ahsan.butt@breadwallet.com> on 9/17/19.
 * Copyright (c) 2019 breadwallet LLC
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
import com.platform.entities.TxMetaDataEmpty
import com.platform.entities.TxMetaDataValue
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
        private val ORDERED_KEYS =
            listOf(KEY_WALLET_INFO, KEY_ASSET_INDEX, KEY_TOKEN_LIST_META_DATA)
    }

    override fun create(accountCreationDate: Date) {
        val walletInfoJson = WalletInfoData(
            creationDate = accountCreationDate.time
        ).toJSON()

        storeProvider.put(KEY_WALLET_INFO, walletInfoJson)
        putEnabledWallets(BreadApp.getDefaultEnabledWallets())
        logInfo("MetaDataManager created successfully")
    }

    override fun recoverAll(migrate: Boolean) = flow {
        // Sync essential metadata first, migrate enabled wallets ASAP
        storeProvider.sync(KEY_WALLET_INFO)
        storeProvider.sync(KEY_ASSET_INDEX)
        if (migrate) {
            storeProvider.sync(KEY_TOKEN_LIST_META_DATA)
            migrateTokenList()
        }
        // Not redundant to prioritize the above in ordered keys
        // if above was successful, will be a no-op, and if failed, it's a retry
        val syncResult = storeProvider.syncAll(migrate, ORDERED_KEYS)
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

    override fun enableWallet(currencyId: String) {
        val enabledWallets = getEnabledWalletsUnsafe()?.toMutableList() ?: mutableListOf()
        if (!enabledWallets.contains(currencyId)) {
            putEnabledWallets(
                enabledWallets.apply { add(currencyId) }
            )
        }
    }

    override fun disableWallet(currencyId: String) {
        getEnabledWalletsUnsafe()
            ?.toMutableList()
            ?.filter { !it.equals(currencyId, true) }
            ?.run(::putEnabledWallets)
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

    override fun txMetaData(transactionHash: String): Flow<TxMetaData> {
        return storeProvider.keyFlow(getTxMetaDataKey(transactionHash))
            .mapLatest {
                TxMetaDataValue.fromJsonObject(it)
            }
            .onStart {
                val key = getTxMetaDataKey(transactionHash)
                var metaData = storeProvider.get(key)
                if (metaData == null) {
                    // Emit none for now, attempt sync (both new and legacy key)
                    emit(TxMetaDataEmpty)
                    metaData = storeProvider.sync(key) ?: getOrSync(
                        getTxMetaDataKey(
                            transactionHash,
                            true
                        )
                    )
                    metaData?.run {
                        storeProvider.put(key, this)
                        emit(TxMetaDataValue.fromJsonObject(this))
                    }
                } else {
                    emit(TxMetaDataValue.fromJsonObject(metaData))
                }
            }
            .distinctUntilChanged()
    }

    override suspend fun putTxMetaData(newTxMetaData: TxMetaDataValue, rawTxHash: String) {
        var txMetaData = txMetaData(rawTxHash).first()

        var needsUpdate = false
        when (txMetaData) {
            is TxMetaDataEmpty -> {
                needsUpdate = true
                txMetaData = newTxMetaData
            }
            is TxMetaDataValue -> {
                if (newTxMetaData.comment != txMetaData.comment) {
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
        }

        if (needsUpdate) {
            val key = getTxMetaDataKey(rawTxHash)
            storeProvider.put(key, txMetaData.toJSON())
        }
    }

    override fun getEnabledWalletsUnsafe(): List<String>? =
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

    override fun resetDefaultWallets() {
        putEnabledWallets(BreadApp.getDefaultEnabledWallets())
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
            CryptoHelper.hexDecode(txHash.removePrefix("0x"))
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
