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
import com.breadwallet.crypto.Account
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
import com.breadwallet.crypto.events.wallet.DefaultWalletEventVisitor
import com.breadwallet.crypto.events.wallet.WalletEvent
import com.breadwallet.crypto.events.wallet.WalletTransferAddedEvent
import com.breadwallet.crypto.events.wallet.WalletTransferChangedEvent
import com.breadwallet.crypto.events.wallet.WalletTransferDeletedEvent
import com.breadwallet.crypto.events.wallet.WalletTransferSubmittedEvent
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
import com.platform.interfaces.WalletsProvider
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.BroadcastChannel
import kotlinx.coroutines.channels.Channel.Factory.BUFFERED
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.distinctUntilChangedBy
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.cancelChildren
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.take
import okhttp3.OkHttpClient
import java.io.File
import java.util.concurrent.Executors

@UseExperimental(ExperimentalCoroutinesApi::class, FlowPreview::class)
@Suppress("TooManyFunctions")
internal class CoreBreadBox(
    private val storageFile: File,
    private val isMainnet: Boolean = false,
    private val walletsProvider: WalletsProvider,
    // TODO: Allow reconfiguring manager mode
    private val walletManagerMode: WalletManagerMode = WalletManagerMode.API_ONLY
) : BreadBox,
    SystemListener,
    SystemEventVisitor<Unit> {

    companion object {
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
    private val blockchainDb = BlockchainDb.createForTest(OkHttpClient(), BDB_AUTH_TOKEN)

    private val openScope = CoroutineScope(
        SupervisorJob() +
            systemExecutor.asCoroutineDispatcher() +
            CoroutineExceptionHandler { _, throwable ->
                logError("Error in coroutine", throwable)
            })

    private val systemChannel = BroadcastChannel<System>(BUFFERED)
    private val accountChannel = BroadcastChannel<Account>(BUFFERED)
    private val walletsChannel = BroadcastChannel<List<Wallet>>(BUFFERED)
    private val walletSyncStateChannel = BroadcastChannel<WalletSyncState>(BUFFERED)

    private val walletTransfersChannelMap = createChannelMap<String, List<Transfer>>()
    private val transferUpdatedChannelMap = createChannelMap<String, Transfer>()

    private val walletTracker = SystemWalletTracker(walletsProvider, system())

    private fun <K, V> createChannelMap(): MutableMap<K, BroadcastChannel<V>> =
        mutableMapOf<K, BroadcastChannel<V>>().run {
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

        walletTracker
            .monitorTrackedWallets()
            .launchIn(openScope)

        logInfo("BreadBox opened successfully")
    }

    @Synchronized
    override fun close() {
        logDebug("Closing BreadBox")

        check(isOpen) { "BreadBox must be opened before calling close()." }

        openScope.coroutineContext.cancelChildren()

        try {
            checkNotNull(system).stop()
            logDebug("System stopped successfully")
        } catch (e: Exception) {
            logError("Failed calling System.stop()", e)
        }

        isOpen = false
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

    override fun wallets(filterByTracked: Boolean) =
        walletsChannel
            .asFlow()
            .onStart { system?.wallets?.let { emit(it) } }
            .mapLatest { wallets ->
                when {
                    filterByTracked -> {
                        wallets.filter { it.walletManager.state.isTracked() }
                    }
                    else -> wallets
                }
            }
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
            .filter { it.currencyCode == currencyCode }
            .onStart {
                // Dispatch initial sync state
                val isSyncing = wallet(currencyCode)
                    .map { it.walletManager.state.type == WalletManagerState.Type.SYNCING }
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
            .distinctUntilChanged()

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

    override fun walletTransfer(currencyCode: String, transferHash: String) =
        transferUpdatedChannelMap.getValue(currencyCode)
            .asFlow()
            .filter { it.hashString() == transferHash }
            .onStart {
                system
                    ?.wallets
                    ?.find { it.currency.code == currencyCode }
                    ?.transfers
                    ?.singleOrNull { it.hashString() == transferHash }
                    ?.let { emit(it) }
            }
            .distinctUntilChanged()

    override fun walletTransferUpdates(currencyCode: String): Flow<Transfer> =
        transferUpdatedChannelMap.getValue(currencyCode)
            .asFlow()

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

        fun updateTransfer(transfer: Transfer) {
            walletTransfersChannelMap
                .getValue(wallet.currency.code)
                .offer(wallet.transfers)
            transferUpdatedChannelMap
                .getValue(wallet.currency.code)
                .offer(transfer)
        }

        event.accept(object : DefaultWalletEventVisitor<Unit>() {
            override fun visit(event: WalletTransferChangedEvent) {
                updateTransfer(event.transfer)
            }

            override fun visit(event: WalletTransferSubmittedEvent) {
                updateTransfer(event.transfer)
            }

            override fun visit(event: WalletTransferDeletedEvent) {
                updateTransfer(event.transfer)
            }

            override fun visit(event: WalletTransferAddedEvent) {
                updateTransfer(event.transfer)
            }
        })
    }

    @Synchronized
    override fun handleManagerEvent(
        system: System,
        manager: WalletManager,
        event: WalletManagerEvent
    ) {
        walletsChannel.offer(system.wallets)

        event.accept(object : DefaultWalletManagerEventVisitor<Unit>() {
            override fun visit(event: WalletManagerCreatedEvent) {
                logDebug("Wallet Manager Created: '${manager.name}'")

                walletTracker
                    .trackedWallets()
                    .take(1)
                    .onEach { wallets ->
                        val connectManager =
                            wallets.any { it.equals(manager.currency.code, true) }
                        when {
                            connectManager -> {
                                manager.connect(null) //TODO: Support custom node
                                logDebug("Wallet Manager Connected: '${manager.name}'")
                            }
                            else -> logDebug("Wallet Manager Not Tracked: '${manager.name}'")
                        }
                    }
                    .launchIn(openScope)
            }

            override fun visit(event: WalletManagerDeletedEvent) {
                logDebug("Wallet Manager Deleted: '${manager.name}'")
                manager.disconnect()
            }

            override fun visit(event: WalletManagerSyncProgressEvent) {
                val timeStamp = event.timestamp?.get()?.time
                logDebug("(${manager.currency.code}) Sync Progress progress=${event.percentComplete} time=$timeStamp")
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
                val fromStateType = event.oldState.type
                val toStateType = event.newState.type
                logDebug("(${manager.currency.code}) State Changed from='$fromStateType' to='$toStateType'")

                // Syncing is complete, manually signal change to observers
                if (fromStateType == WalletManagerState.Type.SYNCING) {
                    walletSyncStateChannel.offer(
                        WalletSyncState(
                            currencyCode = manager.currency.code,
                            percentComplete = 1f,
                            timestamp = 0L,
                            isSyncing = false
                        )
                    )
                }

                if (toStateType == WalletManagerState.Type.SYNCING) {
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

    override fun handleNetworkEvent(system: System, network: Network, event: NetworkEvent) =
        Unit

    @Synchronized
    override fun handleSystemEvent(system: System, event: SystemEvent) {
        event.accept(this)
    }

    override fun handleTransferEvent(
        system: System,
        manager: WalletManager,
        wallet: Wallet,
        transfer: Transfer,
        event: TranferEvent
    ) = Unit

    override fun visit(event: SystemDiscoveredNetworksEvent) = Unit

    override fun visit(event: SystemCreatedEvent) = Unit

    override fun visit(event: SystemManagerAddedEvent) = Unit

    override fun visit(event: SystemNetworkAddedEvent) {
        val system = checkNotNull(system)
        val network = event.network

        logDebug("Network '${network.name}' added.")

        walletTracker
            .trackedWallets()
            .take(1)
            .onEach { wallets ->
                val enableNetwork = wallets
                    .any { network.getCurrencyByCode(it.toLowerCase()).isPresent }
                if (enableNetwork && network.isMainnet == isMainnet) {
                    logDebug("Creating wallet manager for network '${network.name}'.")

                    val wmMode = when {
                        system.supportsWalletManagerMode(network, walletManagerMode) ->
                            walletManagerMode
                        else -> system.getDefaultWalletManagerMode(network)
                    }

                    val addressScheme = system.getDefaultAddressScheme(network)
                    system.createWalletManager(event.network, wmMode, addressScheme, emptySet())
                }
            }
            .launchIn(openScope)
    }
}