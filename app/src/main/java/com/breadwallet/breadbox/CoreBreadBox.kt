/**
 * BreadWallet
 *
 * Created by Drew Carlson <drew.carlson@breadwallet.com> on 9/10/19.
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
package com.breadwallet.breadbox

import com.breadwallet.BreadApp
import com.breadwallet.corecrypto.CryptoApiProvider
import com.breadwallet.crypto.Account
import com.breadwallet.crypto.CryptoApi
import com.breadwallet.crypto.Key
import com.breadwallet.crypto.Network
import com.breadwallet.crypto.System
import com.breadwallet.crypto.Transfer
import com.breadwallet.crypto.Wallet
import com.breadwallet.crypto.WalletManager
import com.breadwallet.crypto.WalletManagerMode
import com.breadwallet.crypto.WalletManagerState
import com.breadwallet.crypto.blockchaindb.BlockchainDb
import com.breadwallet.crypto.events.network.NetworkEvent
import com.breadwallet.crypto.events.system.SystemCreatedEvent
import com.breadwallet.crypto.events.system.SystemDiscoveredNetworksEvent
import com.breadwallet.crypto.events.system.SystemEvent
import com.breadwallet.crypto.events.system.SystemEventVisitor
import com.breadwallet.crypto.events.system.SystemListener
import com.breadwallet.crypto.events.system.SystemManagerAddedEvent
import com.breadwallet.crypto.events.system.SystemNetworkAddedEvent
import com.breadwallet.crypto.events.transfer.TranferEvent
import com.breadwallet.crypto.events.wallet.WalletEvent
import com.breadwallet.crypto.events.walletmanager.DefaultWalletManagerEventVisitor
import com.breadwallet.crypto.events.walletmanager.WalletManagerChangedEvent
import com.breadwallet.crypto.events.walletmanager.WalletManagerCreatedEvent
import com.breadwallet.crypto.events.walletmanager.WalletManagerDeletedEvent
import com.breadwallet.crypto.events.walletmanager.WalletManagerEvent
import com.breadwallet.crypto.events.walletmanager.WalletManagerSyncProgressEvent
import com.breadwallet.tools.manager.BRSharedPrefs
import com.breadwallet.tools.util.Bip39Reader
import com.breadwallet.ui.util.logDebug
import com.breadwallet.ui.util.logError
import com.breadwallet.ui.util.logInfo
import com.platform.tools.KVStoreManager
import kotlinx.coroutines.channels.BroadcastChannel
import kotlinx.coroutines.channels.Channel.Factory.BUFFERED
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.distinctUntilChangedBy
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.cancelChildren
import okhttp3.OkHttpClient
import java.io.File
import java.util.concurrent.Executors

@UseExperimental(ExperimentalCoroutinesApi::class, FlowPreview::class)
@Suppress("TooManyFunctions")
internal class CoreBreadBox(
    private val storageFile: File,
    private val isMainnet: Boolean = false,
    // TODO: Allow reconfiguring manager mode
    private val walletManagerMode: WalletManagerMode = WalletManagerMode.API_ONLY
) : BreadBox,
    SystemListener,
    SystemEventVisitor<Unit>,
    CoroutineScope {

    companion object {
        init {
            CryptoApi.initialize(CryptoApiProvider.getInstance())
        }

        // TODO: Used until auth flow is implemented
        const val BDB_AUTH_TOKEN =
            "eyJ0eXAiOiJKV1QiLCJhbGciOiJIUzUxMiJ9.eyJzdWIiOiJkZWI2M2UyOC0wMzQ1LTQ4ZjYtOWQxNy1jZTgwY2JkNjE3Y2IiLCJicmQ6Y3QiOiJjbGkiLCJleHAiOjkyMjMzNzIwMzY4NTQ3NzUsImlhdCI6MTU2Njg2MzY0OX0.FvLLDUSk1p7iFLJfg2kA-vwhDWTDulVjdj8YpFgnlE62OBFCYt4b3KeTND_qAhLynLKbGJ1UDpMMihsxtfvA0A"
    }

    init {
        // Set default words list
        val context = BreadApp.getBreadContext()
        val words = Bip39Reader.getBip39Words(context, BRSharedPrefs.recoveryKeyLanguage)
        Key.setDefaultWordList(words)
    }

    private var system: System? = null
    private val systemExecutor = Executors.newSingleThreadScheduledExecutor()
    // TODO: Use createForTest until auth flow is implemented.
    //  expect an overload of createForTest without URL params
    private val blockchainDb =
        BlockchainDb.createForTest(OkHttpClient(), BDB_AUTH_TOKEN)

    override val coroutineContext = SupervisorJob() + systemExecutor.asCoroutineDispatcher()

    private val systemChannel = BroadcastChannel<System>(BUFFERED)
    private val accountChannel = BroadcastChannel<Account>(BUFFERED)
    private val walletsChannel = BroadcastChannel<List<Wallet>>(BUFFERED)
    private val walletSyncStateChannel = BroadcastChannel<WalletSyncState>(BUFFERED)
    private val walletTransfersChannelMap =
        mutableMapOf<String, BroadcastChannel<List<Transfer>>>().run {
            withDefault { key -> getOrPut(key) { BroadcastChannel(BUFFERED) } }
        }

    override var isOpen: Boolean = false
        @Synchronized get
        @Synchronized private set

    @Synchronized
    override fun open(account: Account) {
        logDebug("Opening CoreBreadBox")

        check(!isOpen) { "open() called while BreadBox was open." }
        check(account.serialize().isNotEmpty()) { "Account.serialize() contains 0 bytes" }

        if (!storageFile.exists()) {
            logDebug("Making storage directories")
            check(storageFile.mkdirs()) {
                "Failed to create storage directory: ${storageFile.absolutePath}"
            }
        }

        fun newSystem() = System.create(
            systemExecutor,
            this@CoreBreadBox,
            account,
            isMainnet,
            storageFile.absolutePath,
            blockchainDb
        ).apply {
            logDebug("Created new System instance")
            configure(emptyList())
        }

        system = (system ?: newSystem()).also { system ->
            check(system.account.serialize()!!.contentEquals(account.serialize())) {
                "Provided Account does not match existing System Account, " +
                    "CoreBreadBox does not support swapping accounts at runtime."
            }

            logDebug("Dispatching initial System values")

            systemChannel.offer(system)
            accountChannel.offer(system.account)
            system.wallets?.let { wallets ->
                walletsChannel.offer(wallets)
                wallets.forEach {
                    walletTransfersChannelMap
                        .getValue(it.currency.code)
                        .offer(it.transfers)
                }
            }
        }

        isOpen = true

        logInfo("BreadBox opened successfully")
    }

    @Synchronized
    override fun close() {
        logDebug("Closing BreadBox")

        check(isOpen) { "BreadBox must be opened before calling close()." }

        try {
            checkNotNull(system).stop()
            logDebug("System stopped successfully")
        } catch (e: Exception) {
            logError("Failed calling System.stop()", e)
        }

        isOpen = false
    }

    @Synchronized
    override fun empty() {
        check(!isOpen) { "empty() called while BreadBox was open." }
        coroutineContext.cancelChildren()
    }

    override fun system() =
        systemChannel
            .asFlow()
            .onStart {
                system?.let { emit(it) }
            }

    override fun account() =
        accountChannel
            .asFlow()
            .onStart {
                system?.account?.let { emit(it) }
            }

    override fun wallets() =
        walletsChannel
            .asFlow()
            .onStart { system?.wallets?.let { emit(it) } }
            .distinctUntilChangedBy { wallets ->
                wallets.map { it.currency.code }
            }

    override fun wallet(currencyCode: String) =
        walletsChannel
            .asFlow()
            .onStart { system?.wallets?.let { emit(it) } }
            .mapLatest { wallets ->
                wallets.firstOrNull { it.currency.code == currencyCode }
            }
            .filterNotNull()

    override fun currencyCodes() =
        walletsChannel
            .asFlow()
            .onStart { system?.wallets?.let { emit(it) } }
            .mapLatest { wallets -> wallets.map { it.currency.code } }
            .distinctUntilChanged()

    override fun walletSyncState(currencyCode: String) =
        walletSyncStateChannel
            .asFlow()
            .distinctUntilChanged()
            .onStart {
                // Dispatch initial sync state
                val isSyncing = wallet(currencyCode)
                    .map { it.walletManager.state == WalletManagerState.SYNCING }
                    .first()
                emit(
                    WalletSyncState(
                        currencyCode = currencyCode,
                        percentComplete = if (isSyncing) 0f else 1f,
                        timestamp = 0,
                        isSyncing = isSyncing
                    )
                )
            }

    @Synchronized
    override fun walletTransfers(currencyCode: String) =
        walletTransfersChannelMap.getValue(currencyCode)
            .asFlow()
            .onStart {
                system?.wallets
                    ?.find { it.currency.code == currencyCode }
                    ?.transfers
                    ?.let { emit(it) }
            }

    @Synchronized
    override fun getSystemUnsafe(): System? = system

    @Synchronized
    override fun handleWalletEvent(
        system: System,
        manager: WalletManager,
        wallet: Wallet,
        event: WalletEvent
    ) {
        walletsChannel.offer(system.wallets)
        walletTransfersChannelMap
            .getValue(wallet.currency.code)
            .offer(wallet.transfers)
    }

    @Synchronized
    override fun handleManagerEvent(
        system: System,
        manager: WalletManager,
        event: WalletManagerEvent
    ) {
        event.accept(object : DefaultWalletManagerEventVisitor<Unit>() {
            override fun visit(event: WalletManagerCreatedEvent) {
                logDebug("Wallet Manager Created: '${manager.name}'")

                KVStoreManager.walletMetaData(manager.currency.code)
                    .take(1)
                    .onEach {
                        when (it.isEnabled) {
                            true -> {
                                manager.connect()
                                logDebug("Wallet Manager Connected: '${manager.name}'")
                            }
                            else -> logDebug("Wallet Manager Not Tracked: '${manager.name}'")
                        }
                    }
                    .launchIn(this@CoreBreadBox)
            }

            override fun visit(event: WalletManagerDeletedEvent) {
                logDebug("Wallet Manager Deleted: '${manager.name}'")
                manager.disconnect()
            }

            override fun visit(event: WalletManagerSyncProgressEvent) {
                logDebug("(${manager.currency.code}) Sync Progress progress=${event.percentComplete} time=${event.timestamp?.get()?.time}")
                // NOTE: Fulfill percentComplete fractional expectation of consumers
                walletSyncStateChannel.offer(
                    WalletSyncState(
                        currencyCode = manager.currency.code,
                        percentComplete = event.percentComplete / 100,
                        timestamp = event.timestamp.orNull()?.time ?: 0L,
                        isSyncing = true
                    )
                )
            }

            override fun visit(event: WalletManagerChangedEvent) {
                logDebug("(${manager.currency.code}) State Changed from='${event.oldState.name}' to='${event.newState.name}'")
                // Syncing is complete, manually signal change to observers
                if (event.oldState == WalletManagerState.SYNCING) {
                    walletSyncStateChannel.offer(
                        WalletSyncState(
                            currencyCode = manager.currency.code,
                            percentComplete = 1f,
                            timestamp = 0L,
                            isSyncing = false
                        )
                    )
                }

                if (event.newState == WalletManagerState.SYNCING) {
                    walletSyncStateChannel.offer(
                        WalletSyncState(
                            currencyCode = manager.currency.code,
                            percentComplete = 0f,
                            timestamp = 0L,
                            isSyncing = true
                        )
                    )
                }
            }
        })
    }

    @Synchronized
    override fun handleNetworkEvent(system: System, network: Network, event: NetworkEvent) = Unit

    @Synchronized
    override fun handleSystemEvent(system: System, event: SystemEvent) {
        event.accept(this)
    }

    @Synchronized
    override fun handleTransferEvent(
        system: System,
        manager: WalletManager,
        wallet: Wallet,
        transfer: Transfer,
        event: TranferEvent
    ) = Unit

    override fun visit(event: SystemDiscoveredNetworksEvent) = Unit

    override fun visit(event: SystemCreatedEvent) = Unit

    override fun visit(event: SystemManagerAddedEvent) {
        event.walletManager.apply {
            logDebug("Wallet Manager Added: '$name'")
            connect()
            logDebug("Wallet Manager Connected: '$name'")
        }
    }

    @Synchronized
    override fun visit(event: SystemNetworkAddedEvent) {
        val system = checkNotNull(system)
        val network = event.network

        logDebug("Network '${network.name}' added.")

        KVStoreManager.walletsMetaData()
            .take(1)
            .onEach{ wallets ->
                val enableNetwork = wallets
                    .filter { it.isEnabled }
                    .any { network.getCurrencyByCode(it.currencyCode.toLowerCase()).isPresent }
                if (enableNetwork && network.isMainnet == isMainnet) {
                    logDebug("Creating wallet manager for network '${network.name}'.")

                    val wmMode = when {
                        system.supportsWalletManagerModes(network, walletManagerMode) ->
                            walletManagerMode
                        else -> system.getDefaultWalletManagerMode(network)
                    }

                    val addressScheme = system.getDefaultAddressScheme(network)
                    system.createWalletManager(event.network, wmMode, addressScheme)
                }
            }
            .launchIn(this@CoreBreadBox)
    }
}