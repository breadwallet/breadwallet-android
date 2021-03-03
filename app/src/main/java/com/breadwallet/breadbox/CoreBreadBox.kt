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

import com.breadwallet.app.BreadApp
import com.breadwallet.crypto.Account
import com.breadwallet.crypto.Key
import com.breadwallet.crypto.Network
import com.breadwallet.crypto.System
import com.breadwallet.crypto.Transfer
import com.breadwallet.crypto.Wallet
import com.breadwallet.crypto.WalletManager
import com.breadwallet.crypto.WalletManagerState
import com.breadwallet.crypto.blockchaindb.BlockchainDb
import com.breadwallet.crypto.events.network.NetworkEvent
import com.breadwallet.crypto.events.system.SystemDiscoveredNetworksEvent
import com.breadwallet.crypto.events.system.SystemEvent
import com.breadwallet.crypto.events.system.SystemListener
import com.breadwallet.crypto.events.system.SystemNetworkAddedEvent
import com.breadwallet.crypto.events.transfer.TranferEvent
import com.breadwallet.crypto.events.wallet.WalletEvent
import com.breadwallet.crypto.events.wallet.WalletTransferAddedEvent
import com.breadwallet.crypto.events.wallet.WalletTransferChangedEvent
import com.breadwallet.crypto.events.wallet.WalletTransferDeletedEvent
import com.breadwallet.crypto.events.wallet.WalletTransferSubmittedEvent
import com.breadwallet.crypto.events.walletmanager.WalletManagerChangedEvent
import com.breadwallet.crypto.events.walletmanager.WalletManagerCreatedEvent
import com.breadwallet.crypto.events.walletmanager.WalletManagerEvent
import com.breadwallet.crypto.events.walletmanager.WalletManagerSyncProgressEvent
import com.breadwallet.crypto.events.walletmanager.WalletManagerSyncRecommendedEvent
import com.breadwallet.ext.throttleLatest
import com.breadwallet.logger.logDebug
import com.breadwallet.logger.logInfo
import com.breadwallet.tools.manager.BRSharedPrefs
import com.breadwallet.tools.security.BrdUserManager
import com.breadwallet.tools.util.Bip39Reader
import com.breadwallet.tools.util.TokenUtil
import com.breadwallet.util.errorHandler
import com.platform.interfaces.WalletProvider
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.cancel
import kotlinx.coroutines.channels.BroadcastChannel
import kotlinx.coroutines.channels.Channel.Factory.BUFFERED
import kotlinx.coroutines.channels.Channel.Factory.CONFLATED
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.dropWhile
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.flow.transform
import kotlinx.coroutines.launch
import kotlinx.coroutines.plus
import java.io.File
import java.util.Locale
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicBoolean

private const val DEFAULT_THROTTLE_MS = 500L
private const val AGGRESSIVE_THROTTLE_MS = 800L

@Suppress("TooManyFunctions")
internal class CoreBreadBox(
    private val storageFile: File,
    override val isMainnet: Boolean = false,
    private val walletProvider: WalletProvider,
    private val blockchainDb: BlockchainDb,
    private val userManager: BrdUserManager
) : BreadBox,
    SystemListener {

    init {
        // Set default words list
        val context = BreadApp.getBreadContext()
        val words = Bip39Reader.getBip39Words(context, BRSharedPrefs.recoveryKeyLanguage)
        Key.setDefaultWordList(words)
    }

    @Volatile
    private var system: System? = null
    private val systemExecutor = Executors.newSingleThreadScheduledExecutor()

    private var openScope = CoroutineScope(
        SupervisorJob() + Dispatchers.Default + errorHandler("openScope")
    )

    private val systemChannel = BroadcastChannel<Unit>(CONFLATED)
    private val accountChannel = BroadcastChannel<Unit>(CONFLATED)
    private val walletsChannel = BroadcastChannel<Unit>(CONFLATED)
    private val walletSyncStateChannel = BroadcastChannel<WalletSyncState>(BUFFERED)
    private val walletTransfersChannelMap = BroadcastChannel<String>(BUFFERED)
    private val transferUpdatedChannelMap = BroadcastChannel<Transfer>(BUFFERED)

    private var networkManager: NetworkManager? = null

    private val isDiscoveryComplete = AtomicBoolean(false)
    private val _isOpen = AtomicBoolean(false)
    override var isOpen: Boolean
        get() = _isOpen.get()
        set(value) {
            _isOpen.set(value)
        }

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
            logDebug("Dispatching initial System values")

            system.resume()

            networkManager = NetworkManager(
                system,
                openScope + systemExecutor.asCoroutineDispatcher(),
                listOf(DefaultNetworkInitializer(userManager))
            )
            systemChannel.offer(Unit)
            accountChannel.offer(Unit)
            system.wallets?.let { wallets ->
                walletsChannel.offer(Unit)
                wallets.forEach { wallet ->
                    walletTransfersChannelMap.offer(wallet.eventKey())
                }
            }
        }

        isOpen = true

        walletProvider
            .enabledWallets()
            .onEach { enabledWallets ->
                networkManager?.enabledWallets = enabledWallets
                system?.wallets?.let {
                    walletsChannel.offer(Unit)
                }
            }
            .launchIn(openScope)

        walletProvider
            .walletModes()
            .onEach { modes ->
                networkManager?.managerModes = modes
                system?.wallets?.let {
                    walletsChannel.offer(Unit)
                }
            }
            .launchIn(openScope)

        logInfo("BreadBox opened successfully")
    }

    @Synchronized
    override fun close(wipe: Boolean) {
        logDebug("Closing BreadBox")

        check(isOpen) { "BreadBox must be opened before calling close()." }

        openScope.cancel()
        openScope = CoroutineScope(
            SupervisorJob() + Dispatchers.Default + errorHandler("openScope")
        )

        checkNotNull(system).pause()

        if (wipe) {
            System.wipe(system)
            system = null
        }

        isOpen = false

        networkManager = null
    }

    override fun system(): Flow<System> =
        systemChannel
            .asFlow()
            .dropWhile { !isOpen }
            .mapNotNull { system }

    override fun account() =
        accountChannel
            .asFlow()
            .mapNotNull { system?.account }

    override fun wallets(filterByTracked: Boolean) =
        walletsChannel
            .asFlow()
            .throttleLatest(AGGRESSIVE_THROTTLE_MS)
            .mapNotNull {
                val wallets = system?.wallets
                when {
                    filterByTracked -> {
                        wallets?.filterByCurrencyIds(
                            walletProvider.enabledWallets().first()
                        )
                    }
                    else -> wallets
                }
            }

    override fun wallet(currencyCode: String) =
        walletsChannel
            .asFlow()
            .throttleLatest(DEFAULT_THROTTLE_MS)
            .run {
                if (currencyCode.contains(":")) {
                    mapNotNull {
                        system?.wallets?.firstOrNull {
                            it.currency.uids.equals(currencyCode, true)
                        }
                    }
                } else {
                    mapNotNull {
                        system?.wallets?.firstOrNull {
                            it.currency.code.equals(currencyCode, true)
                        }
                    }
                }
            }

    override fun currencyCodes(): Flow<List<String>> =
        combine(
            walletProvider.enabledWallets().throttleLatest(AGGRESSIVE_THROTTLE_MS),
            wallets()
        ) { enabledWallets, wallets ->
            enabledWallets
                .associateWith { wallets.findByCurrencyId(it) }
                .mapValues { (currencyId, wallet) ->
                    wallet?.currency?.code ?: TokenUtil.tokenForCurrencyId(currencyId)
                        ?.symbol?.toLowerCase(Locale.ROOT)
                }.values
                .filterNotNull()
                .toList()
        }.throttleLatest(AGGRESSIVE_THROTTLE_MS)
            .distinctUntilChanged()

    override fun walletSyncState(currencyCode: String) =
        walletSyncStateChannel
            .asFlow()
            .filter { it.currencyCode.equals(currencyCode, true) }
            .throttleLatest(DEFAULT_THROTTLE_MS)
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

    override fun walletTransfers(currencyCode: String) =
        walletTransfersChannelMap
            .asFlow()
            .onStart { emit(currencyCode) }
            .filter { currencyCode.equals(it, true) }
            .throttleLatest(AGGRESSIVE_THROTTLE_MS)
            .mapNotNull {
                system?.wallets
                    ?.find { it.currency.code.equals(currencyCode, true) }
                    ?.transfers
            }

    override fun walletTransfer(currencyCode: String, transferHash: String): Flow<Transfer> {
        return transferUpdatedChannelMap
            .asFlow()
            .filter { transfer ->
                transfer.wallet.currency.code.equals(currencyCode, true) &&
                    transfer.hash.isPresent &&
                    transfer.hashString().equals(transferHash, true)
            }
            .onStart {
                system?.wallets
                    ?.find { it.currency.code.equals(currencyCode, true) }
                    ?.transfers
                    ?.firstOrNull {
                        it.hash.isPresent && it.hashString() == transferHash
                    }
                    ?.also { emit(it) }
            }
    }

    override fun walletTransfer(currencyCode: String, transfer: Transfer): Flow<Transfer> {
        val targetWallet = { wallet: Wallet -> wallet.currency.code.equals(currencyCode, true) }
        val targetTransfer = { updatedTransfer: Transfer ->
                (transfer == updatedTransfer || (transfer.hash.isPresent && transfer.hash == updatedTransfer.hash))
        }
        return transferUpdatedChannelMap
            .asFlow()
            .filter { updatedTransfer ->
                updatedTransfer.wallet.currency.code.equals(currencyCode, true) &&
                    targetTransfer(updatedTransfer)
            }
            .onStart {
                emit(
                    system?.wallets
                        ?.firstOrNull(targetWallet)
                        ?.transfers
                        ?.firstOrNull(targetTransfer)
                        ?: return@onStart
                )
            }
    }

    override fun initializeWallet(currencyCode: String) {
        check(isOpen) { "initializeWallet cannot be called before open." }
        val system = checkNotNull(system)
        val networkManager = checkNotNull(networkManager)
        val network = system.networks.find { it.containsCurrencyCode(currencyCode) }
        checkNotNull(network) {
            "Network with currency code '$currencyCode' not found."
        }
        openScope.launch {
            networkManager.completeNetworkInitialization(network.currency.uids)
        }
    }

    override fun walletState(currencyCode: String): Flow<WalletState> =
        system()
            .map { system -> system.networks.find { it.containsCurrencyCode(currencyCode) } }
            .mapNotNull { network ->
                network?.currency?.uids ?: TokenUtil.tokenForCode(currencyCode)?.currencyId
            }
            .take(1)
            .flatMapLatest { uids ->
                checkNotNull(networkManager).networkState(uids).map { networkState ->
                    when (networkState) {
                        is NetworkState.Initialized -> WalletState.Initialized
                        is NetworkState.Loading -> WalletState.Loading
                        is NetworkState.ActionNeeded -> WalletState.WaitingOnAction
                        is NetworkState.Error -> WalletState.Error
                    }
                }
            }

    override fun networks(whenDiscoveryComplete: Boolean): Flow<List<Network>> =
        system().transform {
            if (whenDiscoveryComplete) {
                if (isDiscoveryComplete.get()) {
                    emit(it.networks)
                }
            } else {
                emit(it.networks)
            }
        }

    override fun getSystemUnsafe(): System? = system

    override fun handleWalletEvent(
        system: System,
        manager: WalletManager,
        wallet: Wallet,
        event: WalletEvent
    ) {
        walletsChannel.offer(Unit)

        fun updateTransfer(transfer: Transfer) {
            transferUpdatedChannelMap.offer(transfer)
            walletTransfersChannelMap.offer(wallet.eventKey())
        }

        when (event) {
            is WalletTransferSubmittedEvent ->
                updateTransfer(event.transfer)
            is WalletTransferDeletedEvent ->
                updateTransfer(event.transfer)
            is WalletTransferAddedEvent ->
                updateTransfer(event.transfer)
            is WalletTransferChangedEvent ->
                updateTransfer(event.transfer)
        }
    }

    override fun handleManagerEvent(
        system: System,
        manager: WalletManager,
        event: WalletManagerEvent
    ) {
        walletsChannel.offer(Unit)

        when (event) {
            is WalletManagerCreatedEvent -> {
                logDebug("Wallet Manager Created: '${manager.name}' mode ${manager.mode}")
                networkManager?.connectManager(manager)
            }
            is WalletManagerSyncProgressEvent -> {
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
            is WalletManagerChangedEvent -> {
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

                if (fromStateType != WalletManagerState.Type.CONNECTED &&
                    toStateType == WalletManagerState.Type.CONNECTED
                ) {
                    logDebug("Wallet Manager Connected: '${manager.name}'")
                    networkManager?.registerCurrencies(manager)
                }
            }
            is WalletManagerSyncRecommendedEvent -> {
                logDebug("Syncing '${manager.currency.code}' to ${event.depth}")
                manager.syncToDepth(event.depth)
            }
        }
    }

    override fun handleNetworkEvent(system: System, network: Network, event: NetworkEvent) = Unit

    override fun handleSystemEvent(system: System, event: SystemEvent) {
        when (event) {
            is SystemNetworkAddedEvent -> {
                logDebug("Network '${event.network.name}' added.")
                networkManager?.initializeNetwork(event.network)
            }
            is SystemDiscoveredNetworksEvent -> {
                isDiscoveryComplete.set(true)
            }
        }
        systemChannel.offer(Unit)
    }

    override fun handleTransferEvent(
        system: System,
        manager: WalletManager,
        wallet: Wallet,
        transfer: Transfer,
        event: TranferEvent
    ) {
        transferUpdatedChannelMap.offer(transfer)
        walletTransfersChannelMap.offer(wallet.eventKey())
    }

    private fun Wallet.eventKey() = currency.code.toLowerCase(Locale.ROOT)
}
