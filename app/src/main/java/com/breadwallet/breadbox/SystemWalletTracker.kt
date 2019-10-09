package com.breadwallet.breadbox

import com.breadwallet.crypto.System
import com.breadwallet.crypto.WalletManagerState
import com.breadwallet.ui.util.logDebug
import com.platform.interfaces.WalletProvider
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.onEach

// TODO: The responsibility of this class is still being fleshed out and thus, the name may change.
// The logic around connecting/disconnecting crypto wallet manager may end up here. Given this class
// is already handling the stream of enabled wallets from the Wallets Provider, perhaps that'll be
// too much responsibility for one class, but better here than in CoreBreadBox.
class SystemWalletTracker(
    private val walletProvider: WalletProvider,
    private val system: Flow<System>
) {

    fun trackedWallets() =
        walletProvider
            .enabledWallets()
            .mapLatest { currencyIds ->
                currencyIds.mapNotNull { currencyId ->
                    system.first()
                        .networks
                        .findByCurrencyId(currencyId)
                        ?.run { currency.code }
                }
            }

    fun monitorTrackedWallets() =
        trackedWallets()
            .onEach { wallets ->
                // Disconnect wallets not found in enabled wallets, if connected
                val systemWallets = system.first().wallets
                systemWallets.forEach { systemWallet ->
                    if (wallets.find {
                            systemWallet.currency.code.equals(
                                it,
                                true
                            )
                        } == null && systemWallet.walletManager.state.isTracked()) {
                        logDebug("Disconnecting Wallet Manager: ${systemWallet.currency.code}")
                        systemWallet.walletManager.disconnect()
                    }
                }
                // Enable wallets not found or otherwise disconnected in system wallets
                wallets.forEach { enabledWallet ->
                    val systemWallet =
                        systemWallets.find { it.currency.code.equals(enabledWallet, true) }
                    when (systemWallet?.walletManager?.state?.isTracked()) {
                        null -> {
                            // TODO: Implement for Add Wallets case (discover network, create wallet manager)
                        }
                        false -> {
                            logDebug("Connecting Wallet Manager: ${systemWallet.currency.code}")
                            systemWallet.walletManager.connect(null) //TODO: Support custom node
                        }
                    }
                }
            }
            .map { Unit }
}

fun WalletManagerState.isTracked() =
    type == WalletManagerState.Type.CREATED ||
        type == WalletManagerState.Type.CONNECTED ||
        type == WalletManagerState.Type.SYNCING