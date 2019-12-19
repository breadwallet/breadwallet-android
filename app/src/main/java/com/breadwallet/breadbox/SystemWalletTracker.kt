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

import com.breadwallet.crypto.Network
import com.breadwallet.crypto.System
import com.breadwallet.crypto.Transfer
import com.breadwallet.crypto.Wallet
import com.breadwallet.crypto.WalletManager
import com.breadwallet.crypto.WalletManagerMode
import com.breadwallet.crypto.WalletManagerState
import com.breadwallet.crypto.events.network.NetworkEvent
import com.breadwallet.crypto.events.system.SystemEvent
import com.breadwallet.crypto.events.system.SystemListener
import com.breadwallet.crypto.events.system.SystemNetworkAddedEvent
import com.breadwallet.crypto.events.transfer.TranferEvent
import com.breadwallet.crypto.events.wallet.WalletEvent
import com.breadwallet.crypto.events.walletmanager.WalletManagerChangedEvent
import com.breadwallet.crypto.events.walletmanager.WalletManagerCreatedEvent
import com.breadwallet.crypto.events.walletmanager.WalletManagerEvent
import com.breadwallet.logger.logDebug
import com.breadwallet.logger.logError
import com.breadwallet.tools.manager.BRSharedPrefs
import com.platform.interfaces.WalletProvider
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.take

/**
 * Responsible for tracking (and un-tracking) [Wallet]s and handles Core [System] events related to created
 * and connecting [WalletManager].
 */
class SystemWalletTracker(
    private val walletProvider: WalletProvider,
    private val system: Flow<System>,
    private val scope: CoroutineScope,
    private val isMainnet: Boolean
) : SystemListener {

    /**
     * On each emission of enabled wallets or change to a wallet connection mode,
     * connects any [WalletManager]s that are newly needed and registers for the new currencies,
     * disconnects [WalletManager]s no longer needed, or restarts [WalletManager]s in new mode.
     */
    fun monitorTrackedWallets() =
        walletProvider
            .enabledWallets()
            .combine(walletProvider.walletModes()) { wallets, walletModeMap ->
                // Disconnect unneeded wallet managers
                val system = system.first()
                val systemWallets = system.wallets
                systemWallets.forEach { disconnectWalletManager(it, wallets) }

                // Enable wallets by creating wallet managers and/or connecting as necessary
                wallets.forEach { enabledWallet ->
                    val systemWallet =
                        systemWallets.find { it.currencyId.equals(enabledWallet, true) }
                    val walletManager = systemWallet?.walletManager ?: system
                        .walletManagers
                        .find { it.networkContainsCurrency(enabledWallet) }

                    when (walletManager) {
                        null -> createWalletManager(system, enabledWallet, walletModeMap)
                        else -> connectAndRegister(
                            walletManager,
                            enabledWallet,
                            walletModeMap
                        )
                    }
                }
            }
            .map { Unit }

    @Synchronized
    /** Connects a newly created [WalletManager] if its [Network] contains a tracked currency. */
    override fun handleManagerEvent(
        system: System,
        manager: WalletManager,
        event: WalletManagerEvent
    ) {
        when (event) {
            is WalletManagerCreatedEvent -> {
                logDebug("Wallet Manager Created: '${manager.name}' mode ${manager.mode}")
                walletProvider
                    .enabledWallets()
                    .take(1)
                    .filter { wallets ->
                        wallets.any { manager.networkContainsCurrency(it) }
                    }
                    .onEach {
                        val address = BRSharedPrefs
                            .getTrustNode(iso = manager.currency.code.capitalize())
                            .orEmpty()
                        val networkPeer = manager
                            .network
                            .getPeerOrNull(address)
                        manager.connect(networkPeer)
                    }
                    .launchIn(scope)
            }
            is WalletManagerChangedEvent -> {
                if (event.oldState.type != WalletManagerState.Type.CONNECTED &&
                    event.newState.type == WalletManagerState.Type.CONNECTED
                ) {
                    logDebug("Wallet Manager Connected: '${manager.name}'")
                    walletProvider
                        .enabledWallets()
                        .take(1)
                        .filter { wallets ->
                            wallets.any { manager.networkContainsCurrency(it) }
                        }
                        .map { wallets ->
                            wallets.filter {
                                manager.networkContainsCurrency(it)
                            }
                        }
                        .onEach { wallets ->
                            wallets.forEach {
                                logDebug("Registering wallet for: $it")
                                manager.registerWalletFor(manager.findCurrency(it))
                            }
                        }
                        .launchIn(scope)
                }
            }
        }
    }

    @Synchronized
    /**
     * Creates a [WalletManager] if the newly added [Network] contains a currency that should
     * be tracked.
     */
    override fun handleSystemEvent(systemEvt: System, event: SystemEvent) {
        when (event) {
            is SystemNetworkAddedEvent -> {
                val network = event.network

                logDebug("Network '${network.name}' added.")

                walletProvider
                    .enabledWallets()
                    .take(1)
                    .filter { wallets ->
                        network.isMainnet == isMainnet && wallets.any { network.containsCurrency(it) }
                    }
                    .onEach {
                        logDebug("Creating wallet manager for network '${network.name}'.")
                        val modeMap = walletProvider.walletModes().first()
                        system
                            .first()
                            .createWalletManager(
                                network,
                                modeMap[network.currency.uids],
                                emptySet()
                            )
                    }
                    .launchIn(scope)
            }
        }
    }

    override fun handleWalletEvent(
        system: System,
        manager: WalletManager,
        wallet: Wallet,
        event: WalletEvent
    ) = Unit

    override fun handleTransferEvent(
        system: System,
        manager: WalletManager,
        wallet: Wallet,
        transfer: Transfer,
        event: TranferEvent
    ) = Unit

    override fun handleNetworkEvent(
        system: System,
        network: Network,
        event: NetworkEvent
    ) = Unit

    private fun createWalletManager(
        system: System,
        currencyId: String,
        walletModeMap: Map<String, WalletManagerMode>
    ) {
        val network = system
            .networks
            .find {
                it.containsCurrency(currencyId)
            }
        when (network) {
            null -> logError("Network for $currencyId not found.")
            else -> {
                val currency = network.findCurrency(currencyId)
                logDebug("Creating wallet manager for $currencyId.")
                system.createWalletManager(
                    network,
                    walletModeMap[network.currency.uids],
                    when (currency) {
                        null -> emptySet()
                        else -> setOf(currency)
                    }
                )
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
        currencyId: String,
        walletModeMap: Map<String, WalletManagerMode>
    ) {
        val mode = walletModeMap[walletManager.network.currency.uids] ?: walletManager.mode
        val address = BRSharedPrefs
            .getTrustNode(iso = walletManager.currency.code.capitalize())
            .orEmpty()
        val networkPeer = walletManager
            .network
            .getPeerOrNull(address)
        if (!walletManager.state.isTracked()) {
            logDebug("Connecting Wallet Manager: ${walletManager.network}")
            walletManager.mode = mode
            walletManager.connect(networkPeer)
        } else if (walletManager.mode != mode) {
            logDebug("Restarting Wallet Manager with new mode: ${walletManager.network} $mode")
            walletManager.disconnect()
            walletManager.mode = mode
            walletManager.connect(networkPeer)
        }
        logDebug("Registering wallet for: $currencyId")
        walletManager.registerWalletFor(
            walletManager.findCurrency(
                currencyId
            )
        )
    }
}