/**
 * BreadWallet
 * <p/>
 * Created by Ahsan Butt on <ahsan.butt@breadwallet.com> 7/30/19.
 * Copyright (c) 2019 breadwallet LLC
 * <p/>
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 * <p/>
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 * <p/>
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package com.breadwallet

import android.util.Log

import com.breadwallet.crypto.Network
import com.breadwallet.crypto.System
import com.breadwallet.crypto.Transfer
import com.breadwallet.crypto.Wallet
import com.breadwallet.crypto.WalletManager
import com.breadwallet.crypto.WalletManagerMode
import com.breadwallet.crypto.events.network.NetworkEvent
import com.breadwallet.crypto.events.system.DefaultSystemEventVisitor
import com.breadwallet.crypto.events.system.SystemEvent
import com.breadwallet.crypto.events.system.SystemListener
import com.breadwallet.crypto.events.system.SystemManagerAddedEvent
import com.breadwallet.crypto.events.system.SystemNetworkAddedEvent
import com.breadwallet.crypto.events.transfer.TranferEvent
import com.breadwallet.crypto.events.transfer.TransferListener
import com.breadwallet.crypto.events.wallet.DefaultWalletEventVisitor
import com.breadwallet.crypto.events.wallet.WalletCreatedEvent
import com.breadwallet.crypto.events.wallet.WalletEvent
import com.breadwallet.crypto.events.wallet.WalletListener
import com.breadwallet.crypto.events.walletmanager.WalletManagerEvent
import com.breadwallet.crypto.events.walletmanager.WalletManagerListener

import java.util.Collections
import java.util.HashSet
import java.util.WeakHashMap

class CryptoSystemListener(private val mode: WalletManagerMode, currencyCodesNeeded: List<String>) : SystemListener {

    private val walletManagerListeners = Collections.newSetFromMap(WeakHashMap<WalletManagerListener, Boolean>())
    private val walletListeners = Collections.newSetFromMap(WeakHashMap<WalletListener, Boolean>())
    private val transferListeners = Collections.newSetFromMap(WeakHashMap<TransferListener, Boolean>())
    private val currencyCodesNeeded = currencyCodesNeeded.toList()


    fun addListener(listener: WalletManagerListener) {
        synchronized(walletManagerListeners) {
            walletManagerListeners.add(listener)
        }
    }

    fun removeListener(listener: WalletManagerListener) {
        synchronized(walletManagerListeners) {
            walletManagerListeners.remove(listener)
        }
    }

    fun addListener(listener: WalletListener) {
        synchronized(walletListeners) {
            walletListeners.add(listener)
        }
    }

    fun removeListener(listener: WalletListener) {
        synchronized(walletListeners) {
            walletListeners.remove(listener)
        }
    }

    fun addListener(listener: TransferListener) {
        synchronized(transferListeners) {
            transferListeners.add(listener)
        }
    }

    fun removeListener(listener: TransferListener) {
        synchronized(transferListeners) {
            transferListeners.remove(listener)
        }
    }

    private fun copyWalletListeners(): Set<WalletListener> {
        return synchronized(walletListeners) {
            HashSet(walletListeners)
        }
    }

    private fun copyWalletManagerListeners(): Set<WalletManagerListener> {
        return synchronized(walletManagerListeners) {
            HashSet(walletManagerListeners)
        }
    }

    private fun copyTransferListeners(): Set<TransferListener> {
        return synchronized(transferListeners) {
            HashSet(transferListeners)
        }
    }

    override fun handleSystemEvent(system: System, event: SystemEvent) {
        Log.d(TAG, "System: $event")
        event.accept(object : DefaultSystemEventVisitor<Void>() {
            override fun visit(event: SystemManagerAddedEvent): Void? {
                val manager = event.walletManager
                manager.connect()
                Log.d(TAG, "Manager connect: " + manager.name)
                return null
            }

            override fun visit(event: SystemNetworkAddedEvent): Void? {
                val network = event.network

                var isNetworkNeeded = false
                for (currencyCode in currencyCodesNeeded) {
                    val currency = network.getCurrencyByCode(currencyCode)
                    if (currency.isPresent) {
                        isNetworkNeeded = true
                        break
                    }
                }

                // TODO: Check mainnet/testnet?
                if (!network.isMainnet && isNetworkNeeded) {
                    val wmMode = if (system.supportsWalletManagerModes(network, mode))
                        mode
                    else
                        system.getDefaultWalletManagerMode(network)

                    val addressScheme = system.getDefaultAddressScheme(network)
                    system.createWalletManager(event.network, wmMode, addressScheme)
                }
                return null
            }
        })
    }

    override fun handleNetworkEvent(system: System, network: Network, event: NetworkEvent) {
        Log.d(TAG, "Network: $event")
    }

    override fun handleManagerEvent(system: System, manager: WalletManager, event: WalletManagerEvent) {
        Log.d(TAG, String.format("Manager (%s): %s", manager.name, event))
        for (listener in copyWalletManagerListeners()) {
            listener.handleManagerEvent(system, manager, event)
        }
    }

    override fun handleWalletEvent(system: System, manager: WalletManager, wallet: Wallet, event: WalletEvent) {
        Log.d(TAG, String.format("Wallet (%s:%s): %s", manager.name, wallet.name, event))
        for (listener in copyWalletListeners()) {
            listener.handleWalletEvent(system, manager, wallet, event)
        }

        event.accept(object : DefaultWalletEventVisitor<Void>() {
            override fun visit(event: WalletCreatedEvent): Void? {
                val wallet = manager.primaryWallet
                Log.d(TAG, String.format("Wallet addresses: %s <--> %s", wallet.source, wallet.target))
                return null
            }
        })
    }

    override fun handleTransferEvent(system: System, manager: WalletManager, wallet: Wallet, transfer: Transfer, event: TranferEvent) {
        Log.d(TAG, String.format("Transfer (%s:%s): %s", manager.name, wallet.name, event))
        for (listener in copyTransferListeners()) {
            listener.handleTransferEvent(system, manager, wallet, transfer, event)
        }
    }

    companion object {
        private val TAG = CryptoSystemListener::class.simpleName
    }
}
