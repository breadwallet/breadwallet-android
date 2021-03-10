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

import com.breadwallet.BuildConfig
import com.breadwallet.app.BreadApp
import com.breadwallet.breadbox.hashString
import com.breadwallet.breadbox.isErc20
import com.breadwallet.crypto.Transfer
import com.breadwallet.crypto.WalletManagerMode
import com.breadwallet.logger.logDebug
import com.breadwallet.logger.logError
import com.breadwallet.logger.logInfo
import com.breadwallet.platform.entities.TxMetaData
import com.breadwallet.platform.entities.TxMetaDataEmpty
import com.breadwallet.platform.entities.TxMetaDataValue
import com.breadwallet.platform.entities.WalletInfoData
import com.breadwallet.platform.interfaces.AccountMetaDataProvider
import com.breadwallet.protocols.messageexchange.entities.PairingMetaData
import com.breadwallet.tools.crypto.CryptoHelper
import com.breadwallet.tools.util.TokenUtil
import com.breadwallet.tools.util.Utils
import com.platform.entities.TokenListMetaData
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.onStart
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import java.util.Date
import java.util.Locale

@Suppress("TooManyFunctions")
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
        private const val TK_META_DATA_KEY_PREFIX = "tkxf-"
        private const val PAIRING_META_DATA_KEY_PREFIX = "pwd-"
        private val ORDERED_KEYS =
            listOf(KEY_WALLET_INFO, KEY_ASSET_INDEX, KEY_TOKEN_LIST_META_DATA)
    }

    override fun create(accountCreationDate: Date) {
        val walletInfoJson = WalletInfoData(
            creationDate = accountCreationDate.time,
            connectionModes = BreadApp.getDefaultWalletModes()
        ).toJSON()

        storeProvider.put(KEY_WALLET_INFO, walletInfoJson)
        putEnabledWallets(BreadApp.getDefaultEnabledWallets())
        logInfo("MetaDataManager created successfully")
    }

    override suspend fun recoverAll(migrate: Boolean): Boolean {
        // Sync essential metadata first, migrate enabled wallets ASAP
        storeProvider.sync(KEY_WALLET_INFO)
        storeProvider.sync(KEY_ASSET_INDEX)
        if (migrate) {
            storeProvider.sync(KEY_TOKEN_LIST_META_DATA)
            migrateTokenList()
        }
        // Not redundant to prioritize the above in ordered keys
        // if above was successful, will be a no-op, and if failed, it's a retry
        val syncResult = storeProvider.syncAll(ORDERED_KEYS)
        if (storeProvider.get(KEY_ASSET_INDEX) == null) {
            // Something went wrong, put default wallets
            storeProvider.put(
                KEY_ASSET_INDEX,
                enabledWalletsToJSON(BreadApp.getDefaultEnabledWallets())
            )
        }
        return syncResult
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
                getOrSync(KEY_ASSET_INDEX)?.run {
                    emit(jsonToEnabledWallets(this))
                }
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

    override fun enableWallets(currencyIds: List<String>) {
        val enabledWallets = getEnabledWalletsUnsafe().orEmpty()
        putEnabledWallets(enabledWallets.union(currencyIds).toList())
    }

    override fun disableWallet(currencyId: String) {
        getEnabledWalletsUnsafe()
            ?.toMutableList()
            ?.filter { !it.equals(currencyId, true) }
            ?.run(::putEnabledWallets)
    }

    override fun reorderWallets(currencyIds: List<String>) {
        putEnabledWallets(currencyIds)
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
                val rawPubKey = CryptoHelper.hexDecode(pairingData.publicKeyHex) ?: pairingData.publicKeyHex.toByteArray(Charsets.UTF_8)
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

    override fun txMetaData(onlyGifts: Boolean): Map<String, TxMetaData> {
        return storeProvider.getKeys()
            .filter { it.startsWith(TX_META_DATA_KEY_PREFIX) }
            .mapNotNull { key ->
                storeProvider.get(key)?.run { key to this }
            }
            .toMap()
            .run {
                if (onlyGifts) filterValues { TxMetaDataValue.hasGift(it) }
                else this
            }
            .mapValues { (_, v) -> TxMetaDataValue.fromJsonObject(v) }
    }

    override fun txMetaData(key: String, isErc20: Boolean): Flow<TxMetaData> {
        return storeProvider.keyFlow(key)
            .mapLatest {
                TxMetaDataValue.fromJsonObject(it)
            }
            .onStart {
                val metaData = storeProvider.get(key)
                if (metaData != null) emit(TxMetaDataValue.fromJsonObject(metaData))
                else emit(TxMetaDataEmpty)
            }
            .distinctUntilChanged()
    }

    override fun txMetaData(transaction: Transfer): Flow<TxMetaData> {
        val key = getTxMetaDataKey(transaction)
        return txMetaData(key, isErc20 = transaction.wallet.currency.isErc20())
    }

    override suspend fun putTxMetaData(
        transaction: Transfer,
        newTxMetaData: TxMetaDataValue
    ) {
        val key = getTxMetaDataKey(transaction)
        putTxMetaData(key, transaction.wallet.currency.isErc20(), newTxMetaData)
    }

    override suspend fun putTxMetaData(key: String, isErc20: Boolean, newTxMetaData: TxMetaDataValue) {
        var txMetaData = txMetaData(key, isErc20 = isErc20).first()

        var needsUpdate = false
        when (txMetaData) {
            is TxMetaDataEmpty -> {
                needsUpdate =
                    !newTxMetaData.comment.isNullOrBlank() ||
                        newTxMetaData.exchangeRate != 0.0 ||
                        !newTxMetaData.gift?.keyData.isNullOrBlank()
                txMetaData = newTxMetaData
            }
            is TxMetaDataValue -> {
                val newGift = newTxMetaData.gift
                val oldGift = txMetaData.gift
                if (newGift != oldGift && newGift != null) {
                    // Don't overwrite gift unless keyData is provided
                    if (oldGift?.keyData == null && newGift.keyData != null) {
                        txMetaData = txMetaData.copy(
                            gift = newTxMetaData.gift
                        )
                        needsUpdate = true
                    } else if (
                        oldGift?.keyData != null &&
                        (oldGift.claimed != newGift.claimed ||
                            oldGift.reclaimed != newGift.reclaimed)
                    ) {
                        txMetaData = txMetaData.copy(
                            gift = oldGift.copy(
                                claimed = newGift.claimed,
                                reclaimed = newGift.reclaimed
                            )
                        )
                        needsUpdate = true
                    }
                }
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
            .also { storeProvider.put(KEY_ASSET_INDEX, it) }

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

            TokenUtil.initialize(BreadApp.getBreadContext(), true, !BuildConfig.BITCOIN_TESTNET)
            val currencyCodeToToken = TokenUtil.getTokenItems()
                .associateBy { it.symbol.toLowerCase(Locale.ROOT) }

            tokenListMetaData.enabledCurrencies
                .filter { enabledToken ->
                    // Need to also ensure not in hidden currencies list
                    tokenListMetaData.hiddenCurrencies.find {
                        it.symbol.equals(enabledToken.symbol, true)
                    } == null
                }
                .mapNotNull {
                    currencyCodeToToken[it.symbol.toLowerCase(Locale.ROOT)]?.currencyId
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
        val value = storeProvider.get(key) ?: storeProvider.sync(key)
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

    private fun getTxMetaDataKey(transaction: Transfer, legacy: Boolean = false): String {
        return getTxMetaDataKey(
            transferHash = transaction.hashString(),
            isErc20 = transaction.wallet.currency.isErc20(),
            legacy = legacy
        )
    }

    private fun getTxMetaDataKey(
        transferHash: String,
        isErc20: Boolean,
        legacy: Boolean = false
    ): String =
        if (legacy) {
            TX_META_DATA_KEY_PREFIX + Utils.bytesToHex(
                CryptoHelper.sha256(transferHash.toByteArray())!!
            )
        } else {
            val hashString = transferHash.removePrefix("0x")
            val sha256hash =
                (CryptoHelper.hexDecode(hashString) ?: hashString.toByteArray(Charsets.UTF_8))
                    .apply {
                        reverse()
                    }
                    .run(CryptoHelper::sha256)
                    ?.run(CryptoHelper::hexEncode)
                    ?: ""
            if (isErc20) TK_META_DATA_KEY_PREFIX + sha256hash
            else TX_META_DATA_KEY_PREFIX + sha256hash
        }

    private fun pairingKey(pubKey: ByteArray): String =
        PAIRING_META_DATA_KEY_PREFIX + CryptoHelper.hexEncode(CryptoHelper.sha256(pubKey)!!)
}
