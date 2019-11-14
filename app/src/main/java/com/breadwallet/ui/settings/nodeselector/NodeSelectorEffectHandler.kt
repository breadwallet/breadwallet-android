package com.breadwallet.ui.settings.nodeselector

import com.breadwallet.breadbox.BreadBox
import com.breadwallet.breadbox.currencyId
import com.breadwallet.breadbox.findNetwork
import com.breadwallet.breadbox.getPeerOrNull
import com.breadwallet.ext.bindConsumerIn
import com.breadwallet.legacy.wallet.wallets.bitcoin.BaseBitcoinWalletManager.BITCOIN_CURRENCY_CODE
import com.breadwallet.logger.logError
import com.breadwallet.tools.manager.BRSharedPrefs
import com.spotify.mobius.Connection
import com.spotify.mobius.functions.Consumer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

class NodeSelectorEffectHandler(
    private val output: Consumer<NodeSelectorEvent>,
    private val breadBox: BreadBox,
    private val showNodeDialog: () -> Unit
) : Connection<NodeSelectorEffect>,
    CoroutineScope {

    override val coroutineContext = SupervisorJob() + Dispatchers.Default

    init {
        breadBox.wallet(BITCOIN_CURRENCY_CODE)
            .map { it.walletManager.state }
            .distinctUntilChanged()
            .map { NodeSelectorEvent.OnConnectionStateUpdated(it) }
            .bindConsumerIn(output, this)
    }

    override fun accept(effect: NodeSelectorEffect) {
        when (effect) {
            NodeSelectorEffect.LoadConnectionInfo -> loadConnectionInfo()
            NodeSelectorEffect.SetToAutomatic -> setToAutomatic()
            NodeSelectorEffect.ShowNodeDialog -> launch(Dispatchers.Main) { showNodeDialog() }
            is NodeSelectorEffect.SetCustomNode -> setToManual(effect)
        }
    }

    override fun dispose() {
        coroutineContext.cancel()
    }

    private fun loadConnectionInfo() {
        val node = BRSharedPrefs.getTrustNode(iso = BITCOIN_CURRENCY_CODE)
        val event = if (node.isNullOrBlank()) {
            NodeSelectorEvent.OnConnectionInfoLoaded(NodeSelectorModel.Mode.AUTOMATIC)
        } else {
            NodeSelectorEvent.OnConnectionInfoLoaded(NodeSelectorModel.Mode.MANUAL, node)
        }
        output.accept(event)
    }

    private fun setToAutomatic() {
        BRSharedPrefs.putTrustNode(
            iso = BITCOIN_CURRENCY_CODE,
            trustNode = ""
        )
        launch {
            val walletManager = breadBox.wallet(BITCOIN_CURRENCY_CODE).first().walletManager
            walletManager.connect(null)
        }
    }

    private fun setToManual(effect: NodeSelectorEffect.SetCustomNode) {
        BRSharedPrefs.putTrustNode(
            iso = BITCOIN_CURRENCY_CODE,
            trustNode = effect.node
        )
        launch {
            val wallet = breadBox
                .wallet(BITCOIN_CURRENCY_CODE)
                .first()
            val network = breadBox
                .system()
                .findNetwork(wallet.currencyId)
                .first()
            val networkPeer = network.getPeerOrNull(effect.node)
            if (networkPeer != null) {
                wallet.walletManager.connect(networkPeer)
            } else {
                logError("Failed to create network peer")
            }
        }
    }
}