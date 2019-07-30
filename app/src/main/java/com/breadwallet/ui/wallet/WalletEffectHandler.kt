package com.breadwallet.ui.wallet

import android.arch.lifecycle.Observer
import android.content.Context
import android.text.format.DateUtils
import com.breadwallet.BuildConfig
import com.breadwallet.model.Wallet
import com.breadwallet.presenter.entities.TxUiHolder
import com.breadwallet.repository.WalletRepository
import com.breadwallet.tools.manager.BRSharedPrefs
import com.breadwallet.tools.manager.InternetManager
import com.breadwallet.tools.util.BRConstants
import com.breadwallet.tools.util.SyncTestLogger
import com.breadwallet.wallet.WalletsMaster
import com.breadwallet.wallet.abstracts.BalanceUpdateListener
import com.breadwallet.wallet.abstracts.BaseWalletManager
import com.breadwallet.wallet.abstracts.OnTxListModified
import com.breadwallet.wallet.abstracts.SyncListener
import com.breadwallet.wallet.util.SyncUpdateHandler
import com.breadwallet.wallet.wallets.CryptoTransaction
import com.breadwallet.wallet.wallets.bitcoin.BaseBitcoinWalletManager
import com.platform.tools.KVStoreManager
import com.spotify.mobius.Connection
import com.spotify.mobius.functions.Consumer
import java.math.BigDecimal

class WalletEffectHandler(
        private val output: Consumer<WalletEvent>,
        private val context: Context,
        private val currencyCode: String
) : Connection<WalletEffect>,
        SyncListener,
        BalanceUpdateListener,
        OnTxListModified,
        InternetManager.ConnectionReceiverListener {

    companion object {
        private const val RUN_LOGGER = false
    }

    private val walletManager: BaseWalletManager =
            WalletsMaster.getInstance().getCurrentWallet(context)
    private val walletRepository: WalletRepository =
            WalletRepository.getInstance(context)

    private val testLogger by lazy { SyncTestLogger(context) }

    private val walletsObserver = Observer<List<Wallet>> { wallets ->
        val wallet = wallets?.find { it.currencyCode == currencyCode } ?: return@Observer
        output.accept(WalletEvent.OnSyncProgressUpdated(
                progress = wallet.syncProgress,
                syncThroughMillis = when (wallet) {
                    is BaseBitcoinWalletManager ->
                        wallet.peerManager.lastBlockTimestamp * DateUtils.SECOND_IN_MILLIS
                    else -> 0L
                }
        ))
    }

    init {
        if (BuildConfig.DEBUG && RUN_LOGGER) {
            testLogger.start()
        }
        InternetManager.registerConnectionReceiver(context, this)
        walletManager.addSyncListener(this)
        walletManager.addBalanceChangedListener(this)
        walletManager.addTxListModifiedListener(this)

        SyncUpdateHandler.startWalletSync(context, currencyCode)

        walletRepository
                .walletsLiveData
                .observeForever(walletsObserver)
    }

    override fun accept(value: WalletEffect) {
        when (value) {
            is WalletEffect.LoadTransactions ->
                output.accept(WalletEvent.OnTransactionsUpdated(
                        (walletManager.getTxUiHolders(context) ?: emptyList())
                                .map {
                                    it.updateMetaData()
                                    it.asWalletTransaction()
                                }
                ))
            is WalletEffect.LoadWalletBalance -> {
                val fiatBalance = walletManager.getFiatBalance(context)
                output.accept(WalletEvent.OnCurrencyNameUpdated(walletManager.name))
                output.accept(WalletEvent.OnBalanceUpdated(walletManager.balance, fiatBalance))
            }
        }
    }

    override fun dispose() {
        InternetManager.unregisterConnectionReceiver(context, this)

        walletRepository
                .walletsLiveData
                .removeObserver(walletsObserver)

        walletManager.removeSyncListener(this)
        walletManager.removeBalanceChangedListener(this)
        walletManager.removeTxListModifiedListener(this)
    }

    override fun syncStarted() {
        //SyncUpdateHandler.startWalletSync(context, currencyCode) // TODO: Copied from WalletActivity, why was this here?
    }

    override fun syncStopped(err: String?) {
    }

    override fun onBalanceChanged(updatedCurrencyCode: String, newBalance: BigDecimal) {
        // TODO: Still not receiving all balance updates
        if (updatedCurrencyCode == currencyCode) {
            val fiatBalance = walletManager.getFiatBalance(context)
            output.accept(WalletEvent.OnBalanceUpdated(newBalance, fiatBalance))
        }
    }

    override fun onBalancesChanged(balanceMap: MutableMap<String, BigDecimal>?) = Unit

    override fun txListModified(hash: String?) {
        // TODO: A better txlist observable source is needed
        // TODO: Still not receiving the final "INCLUDED" event, so txns hang in 'in-progress' unless screen is re-loaded
        hash ?: return
        val updatedTx = walletManager.getTxUiHolders(context)
                .find { it.txHash.toString(Charsets.UTF_8) == hash } ?: return
        updatedTx.updateMetaData()
        output.accept(WalletEvent.OnTransactionUpdated(updatedTx.asWalletTransaction()))
    }

    override fun onConnectionChanged(isConnected: Boolean) {
        output.accept(WalletEvent.OnConnectionUpdated(isConnected))
        if (isConnected) {
            SyncUpdateHandler.startWalletSync(context, currencyCode)
        }
    }

    private fun TxUiHolder.updateMetaData() {
        var txMetaData = KVStoreManager.getTxMetaData(context, txHash)
        if (System.currentTimeMillis() - timeStamp < DateUtils.HOUR_IN_MILLIS) {
            if (txMetaData == null) {
                txMetaData = KVStoreManager.createMetadata(context, walletManager,
                        CryptoTransaction(transaction))
                KVStoreManager.putTxMetaData(context, txMetaData, txHash)
            } else if (txMetaData.exchangeRate == 0.0) {
                txMetaData.exchangeRate = walletManager.getFiatExchangeRate(context).toDouble()
                txMetaData.exchangeCurrency = BRSharedPrefs.getPreferredFiatIso(context)
                KVStoreManager.putTxMetaData(context, txMetaData, txHash)
            }
        }
        metaData = txMetaData
    }

    private fun TxUiHolder.asWalletTransaction() : WalletTransaction {
        val confirmations = when (blockHeight) {
            Integer.MAX_VALUE -> 0
            0 -> 0
            else -> BRSharedPrefs.getLastBlockHeight(context, currencyCode) - blockHeight + 1
        }

        val levels = when {
            confirmations > 4 -> BRConstants.CONFIRMED_BLOCKS_NUMBER
            confirmations <= 0 -> {
                val relayCount = walletManager.getRelayCount(txHash)
                when {
                    relayCount <= 0L -> 0
                    relayCount == 1L -> 1
                    else -> 2
                }
            }
            else -> confirmations + 2
        }

        return WalletTransaction(
                txHash = txHash.toString(Charsets.UTF_8),
                amount = amount,
                fiatWhenSent = 0f, // TODO: Rates info
                toAddress = to,
                fromAddress = from,
                isReceived = isReceived,
                isErrored = isErrored,
                memo = metaData?.comment.orEmpty(),
                isValid = isValid,
                fee = fee,
                blockHeight = blockHeight,
                confirmations = confirmations,
                timeStamp = timeStamp,
                levels = levels,
                currencyCode = currencyCode
        )
    }
}