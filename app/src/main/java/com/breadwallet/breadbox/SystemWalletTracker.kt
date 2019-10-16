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
import com.breadwallet.logger.logDebug
import com.breadwallet.logger.logError
import com.platform.interfaces.WalletProvider
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
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
    private val isMainnet: Boolean,
    private val walletManagerMode: WalletManagerMode
) : SystemListener,
    SystemEventVisitor<Unit> {

    /**
     * On each emission of enabled wallets, connects any [WalletManager]s that are newly needed
     * and registers for the new currencies, and disconnects [WalletManager]s no longer needed.
     */
    fun monitorTrackedWallets() =
        walletProvider
            .enabledWallets()
            .onEach { wallets ->
                // Disconnect wallet managers that have no tracked currencies, if connected
                val systemWallets = system.first().wallets
                systemWallets.forEach { systemWallet ->
                    val walletManager = systemWallet.walletManager
                    val disconnectWalletManager =
                        walletManager.state.isTracked() &&
                            wallets.none { walletManager.networkContainsCurrency(it) }
                    if (disconnectWalletManager) {
                        logDebug("Disconnecting Wallet Manager: ${systemWallet.currency.code}")
                        systemWallet.walletManager.disconnect()
                    }
                }
                // Enable wallets not found or otherwise disconnected in system wallets
                wallets.forEach { enabledWallet ->
                    val systemWallet =
                        systemWallets.find { it.currencyId.equals(enabledWallet, true) }

                    if (systemWallet == null) {
                        val walletManager = system.first()
                            .walletManagers
                            .find {
                                it.networkContainsCurrency(enabledWallet)
                            }

                        when (walletManager) {
                            null -> logError("Wallet Manager for $enabledWallet not found.")
                            else -> {
                                if (walletManager.state.isTracked()) {
                                    logDebug("Connecting Wallet Manager: ${walletManager.network}")
                                    walletManager.connect(null) //TODO: Support custom node
                                }
                                logDebug("Registering wallet for: $enabledWallet")
                                walletManager.registerWalletFor(
                                    walletManager.findCurrency(
                                        enabledWallet
                                    )
                                )
                            }
                        }
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
        event.accept(object : DefaultWalletManagerEventVisitor<Unit>() {
            override fun visit(event: WalletManagerCreatedEvent) {
                logDebug("Wallet Manager Created: '${manager.name}'")

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
                        manager.connect(null) //TODO: Support custom node
                        logDebug("Wallet Manager Connected: '${manager.name}'")
                        wallets.forEach {
                            logDebug("Registering wallet for: $it")
                            manager.registerWalletFor(manager.findCurrency(it))
                        }
                    }
                    .launchIn(scope)
            }

            override fun visit(event: WalletManagerDeletedEvent) = Unit

            override fun visit(event: WalletManagerSyncProgressEvent) = Unit

            override fun visit(event: WalletManagerChangedEvent) = Unit
        })
    }

    @Synchronized
    override fun handleSystemEvent(system: System, event: SystemEvent) {
        event.accept(this)
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

    /**
     * Creates a [WalletManager] if the newly added [Network] contains a currency that should
     * be tracked.
     */
    override fun visit(event: SystemNetworkAddedEvent) {
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

                val system = system.first()
                val wmMode = when {
                    system.supportsWalletManagerMode(network, walletManagerMode) ->
                        walletManagerMode
                    else -> system.getDefaultWalletManagerMode(network)
                }

                val addressScheme = system.getDefaultAddressScheme(network)
                system.createWalletManager(event.network, wmMode, addressScheme, emptySet())
            }
            .launchIn(scope)
    }

    override fun visit(event: SystemDiscoveredNetworksEvent) = Unit

    override fun visit(event: SystemCreatedEvent) = Unit

    override fun visit(event: SystemManagerAddedEvent) = Unit
}