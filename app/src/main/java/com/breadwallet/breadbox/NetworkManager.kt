/**
 * BreadWallet
 *
 * Created by Ahsan Butt <ahsan.butt@breadwallet.com> on 10/01/19.
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

import com.breadwallet.crypto.AddressScheme
import com.breadwallet.crypto.Network
import com.breadwallet.crypto.NetworkPeer
import com.breadwallet.crypto.System
import com.breadwallet.crypto.Wallet
import com.breadwallet.crypto.WalletManager
import com.breadwallet.crypto.WalletManagerMode
import com.breadwallet.logger.logDebug
import com.breadwallet.tools.manager.BRSharedPrefs
import com.breadwallet.util.isBitcoin
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.BroadcastChannel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.onStart

import kotlinx.coroutines.launch
import java.util.concurrent.ConcurrentHashMap

/**
 * Responsible for tracking (and un-tracking) [Wallet]s and all related hook-up.
 */
class NetworkManager(
    private val system: System,
    private val scope: CoroutineScope,
    private val networkInitializers: List<NetworkInitializer>
) {

    var enabledWallets: List<String> = emptyList()
        set(value) {
            if (field == value) return

            field = value
            scope.launch {
                updateWallets()
            }
        }

    var managerModes: Map<String, WalletManagerMode> = emptyMap()
        set(value) {
            if (field == value) return

            field = value
            scope.launch { updateManagerModes() }
        }

    private val initializeActionChannel = ConcurrentHashMap<String, Channel<Unit>>()
    private val networkState = ConcurrentHashMap<String, NetworkState>()
    private val networkStateChannel = BroadcastChannel<Pair<String, NetworkState>>(Channel.BUFFERED)

    init {
        system.networks.forEach(::initializeNetwork)
    }

    /**
     * Initializes a [Network].
     */
    fun initializeNetwork(network: Network) {
        if (system.walletManagers.any { it.network == network }) {
            logDebug("WalletManager for network (${network.uids}) already exists.")
            networkState[network.currency.uids] = NetworkState.Initialized
            return
        }
        scope.launch {
            initializeNetwork(network, false)
        }
    }

    /**
     * Emits the [NetworkState] for a [Network] specified by its [currencyId]
     */
    fun networkState(currencyId: String) =
        networkStateChannel
            .asFlow()
            .filter { it.first.equals(currencyId, true) }
            .mapLatest { it.second }
            .onStart {
                networkState[currencyId]?.let { emit(it) }
                    ?: emit(NetworkState.Loading)
            }

    /**
     * Completes the initialization process for a [Network] specified by
     * its [currencyId].
     */
    fun completeNetworkInitialization(currencyId: String) {
        val channel = checkNotNull(initializeActionChannel[currencyId]) {
            "Network for $currencyId not waiting on initialize action."
        }
        channel.offer(Unit)
    }

    /**
     * Connects a [WalletManager] if needed.
     */
    fun connectManager(manager: WalletManager) {
        scope.launch {
            enabledWallets
                .filter { manager.networkContainsCurrency(it) }
                .onEach {
                    val networkPeer = getNetworkPeer(manager)
                    val network = manager.network
                    manager.mode = network.getManagerMode(managerModes[network.currency.uids])
                    manager.connect(networkPeer)
                }
        }
    }

    /**
     * Registers all needed currencies for the given [WalletManager].
     */
    fun registerCurrencies(manager: WalletManager) {
        scope.launch {
            enabledWallets
                .filter { manager.networkContainsCurrency(it) }
                .onEach {
                    logDebug("Registering wallet for: $it")
                    manager.registerWalletFor(manager.findCurrency(it))
                }
        }
    }

    private suspend fun initializeNetwork(network: Network, createIfNeeded: Boolean) {
        val initializer = networkInitializers.get(network.currency.uids)

        if (initializer == null) {
            logDebug("No initializer found for network '${network.name}'")
            return
        }

        if (createIfNeeded) {
            emitNetworkState(network.currency.uids, NetworkState.Loading)
        }
        val state = initializer.initialize(system, network, createIfNeeded)
        emitNetworkState(network.currency.uids, state)

        when (state) {
            is NetworkState.Initialized -> {
                logDebug("Network '${network.name}' initialized.")

                val wmMode = network.getManagerMode(managerModes[network.currency.uids])
                system.createWalletManager(
                    network,
                    wmMode,
                    network.getAddressScheme(),
                    emptySet()
                )
            }
            is NetworkState.ActionNeeded -> {
                logDebug("Network '${network.name}' requires further action.")
                waitForInitializeAction(network)
            }
            is NetworkState.Error -> {
                logDebug(
                    "Network '${network.name}' failed to initialize. ${state.message}",
                    state.error
                )
                emitNetworkState(network.currency.uids, NetworkState.ActionNeeded)
                waitForInitializeAction(network)
            }
        }
    }

    private suspend fun waitForInitializeAction(network: Network) {
        initializeActionChannel.getSafe(network.currency.uids).receive()
        initializeNetwork(network, true)
    }

    private fun updateWallets() {
        // Disconnect unneeded wallet managers
        val systemWallets = system.wallets
        systemWallets.forEach { disconnectWalletManager(it, enabledWallets) }

        // Enable wallets by connecting and registering as necessary
        enabledWallets.forEach { enabledWallet ->
            val systemWallet = systemWallets.find { it.currencyId.equals(enabledWallet, true) }
            val walletManager = systemWallet?.walletManager ?: system
                .walletManagers
                .find { it.networkContainsCurrency(enabledWallet) }

            if (walletManager != null) {
                connectAndRegister(
                    walletManager,
                    enabledWallet
                )
            }
        }
    }

    private fun updateManagerModes() {
        managerModes.entries.forEach { modeEntry: Map.Entry<String, WalletManagerMode> ->
            val wallet = system.wallets.find { it.currencyId.equals(modeEntry.key, true) }
            val walletManager = wallet?.walletManager ?: system
                .walletManagers
                .find { it.networkContainsCurrency(modeEntry.key) }
            val mode = walletManager?.network?.getManagerMode(modeEntry.value)

            if (mode != null && walletManager.mode != mode) {
                logDebug("Restarting Wallet Manager with new mode: ${walletManager.network} ${modeEntry.value}")
                walletManager.mode = walletManager.network.getManagerMode(modeEntry.value)
                walletManager.disconnect()
                walletManager.connect(getNetworkPeer(walletManager))
            }
        }
    }

    /**
     * Disconnect [WalletManager] for [systemWallet], if connected and not needed for
     * another tracked wallet.
     */
    private fun disconnectWalletManager(systemWallet: Wallet, enabledWallets: List<String>) {
        val walletManager = systemWallet.walletManager
        val disconnectWalletManager =
            walletManager.state.isTracked() &&
                enabledWallets.none { walletManager.networkContainsCurrency(it) }
        if (disconnectWalletManager) {
            logDebug("Disconnecting Wallet Manager: ${systemWallet.currency.code}")
            systemWallet.walletManager.disconnect()
        }
    }

    /**
     * Connect [WalletManager] for [currencyId], if not already connected, and register for
     * [currencyId].
     */
    private fun connectAndRegister(
        walletManager: WalletManager,
        currencyId: String
    ) {
        val network = walletManager.network
        val mode = network.getManagerMode(managerModes[network.currency.uids])
        val networkPeer = getNetworkPeer(walletManager)
        if (!walletManager.state.isTracked()) {
            logDebug("Connecting Wallet Manager: ${walletManager.network}")
            walletManager.mode = mode
            walletManager.connect(networkPeer)
        }
        // TODO: Once CORE-871 is fixed, calling registerWalletFor() here will be redundant, as
        // we should get another WalletManagerChangedEvent after connecting above
        logDebug("Registering wallet for: $currencyId")
        walletManager.registerWalletFor(
            walletManager.findCurrency(
                currencyId
            )
        )
    }

    private fun getNetworkPeer(manager: WalletManager): NetworkPeer? {
        val address = BRSharedPrefs
            .getTrustNode(iso = manager.currency.code)
            .orEmpty()
        return manager
            .network
            .getPeerOrNull(address)
    }

    private fun emitNetworkState(currencyId: String, state: NetworkState) {
        networkState[currencyId] = state
        networkStateChannel.offer(Pair(currencyId, state))
    }
}

private fun Network.getManagerMode(preferredMode: WalletManagerMode?): WalletManagerMode = when {
    currency.isBitcoinCash() -> WalletManagerMode.API_ONLY
    preferredMode == null -> defaultWalletManagerMode
    supportsWalletManagerMode(preferredMode) -> preferredMode
    else -> defaultWalletManagerMode
}

private fun Network.getAddressScheme(): AddressScheme {
    return when {
        currency.code.isBitcoin() -> {
            if (BRSharedPrefs.getIsSegwitEnabled()) {
                AddressScheme.BTC_SEGWIT
            } else {
                AddressScheme.BTC_LEGACY
            }
        }
        else -> defaultAddressScheme
    }
}

fun List<NetworkInitializer>.get(currencyId: String) =
    firstOrNull { it !is DefaultNetworkInitializer && it.isSupported(currencyId) }
        ?: firstOrNull { it.isSupported(currencyId) }

private fun ConcurrentHashMap<String, Channel<Unit>>.getSafe(key: String) = run {
    getOrElse(key, { Channel() }).also { putIfAbsent(key, it) }
}
