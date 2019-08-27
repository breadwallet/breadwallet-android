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
package com.breadwallet.ui.global.effect

import com.breadwallet.crypto.*

import com.breadwallet.BreadApp

import android.content.Context
import com.breadwallet.BuildConfig
import com.breadwallet.crypto.events.wallet.*
import com.breadwallet.crypto.events.wallet.WalletEvent as CoreWalletEvent
import com.breadwallet.crypto.events.walletmanager.DefaultWalletManagerEventVisitor
import com.breadwallet.crypto.events.walletmanager.WalletManagerEvent
import com.breadwallet.crypto.events.walletmanager.WalletManagerListener
import com.breadwallet.crypto.events.walletmanager.WalletManagerSyncProgressEvent
import com.breadwallet.repository.RatesRepository
import com.breadwallet.tools.manager.BRSharedPrefs
import com.breadwallet.tools.util.BRConstants
import com.breadwallet.tools.util.SyncTestLogger
import com.breadwallet.ui.global.event.WalletEvent
import com.breadwallet.ui.wallet.WalletTransaction
import com.google.common.primitives.UnsignedLong
import com.platform.tools.KVStoreManager
import com.spotify.mobius.Connection
import com.spotify.mobius.functions.Consumer
import java.math.BigDecimal

class CryptoWalletEffectHandler(
        private val output: Consumer<WalletEvent>,
        private val context: Context,
        private val currencyCode: String
) : Connection<WalletEffect>, WalletListener, WalletManagerListener {

    companion object {
        private const val RUN_LOGGER = false
    }

    private val wallet = BreadApp.getCryptoSystem()
            .wallets
            .single { currencyCode.equals(it.currency.code, true) }

    private val testLogger by lazy { SyncTestLogger(context) }

    init {
        if (BuildConfig.DEBUG && RUN_LOGGER) {
            testLogger.start()
        }

        BreadApp.getCryptoSystemListener()?.apply {
            addListener(this as WalletListener)
            addListener(this as WalletManagerListener)
        }
    }

    override fun accept(value: WalletEffect) {
        when (value) {
            is WalletEffect.LoadTransactions -> {
                output.accept(WalletEvent.OnTransactionsUpdated(
                        (wallet.transfers ?: emptyList())
                                .map {
                                    it.asWalletTransaction()
                                }.sortedByDescending(WalletTransaction::timeStamp)
                ))
            }
            is WalletEffect.LoadWalletBalance -> {
                val balanceAmt = wallet.balance

                val balance = getBalance(balanceAmt)
                val balanceInFiat = getBalanceInFiat(balanceAmt)

                output.accept(WalletEvent.OnCurrencyNameUpdated(wallet.walletManager.network.name)) // TODO: should be wallet.name, but instead of 'Bitcoin', gives us 'btc' (discuss with CORE)
                output.accept(WalletEvent.OnBalanceUpdated(balance, balanceInFiat))
            }
        }
    }

    override fun dispose() {
        BreadApp.getCryptoSystemListener()?.apply {
            removeListener(this as WalletListener)
            removeListener(this as WalletManagerListener)
        }
    }

    override fun handleWalletEvent(system: System, manager: WalletManager, wallet: Wallet, event: CoreWalletEvent) {
        event.accept(object : DefaultWalletEventVisitor<Void>() {
            override fun visit(event: WalletBalanceUpdatedEvent): Void? {
                if (event.balance.currency.code.toLowerCase() != currencyCode.toLowerCase())
                    return null

                val balanceAmt = event.balance
                val balance = getBalance(balanceAmt)
                val balanceInFiat = getBalanceInFiat(balanceAmt)

                output.accept(WalletEvent.OnBalanceUpdated(balance, balanceInFiat))

                return null
            }

            override fun visit(event: WalletTransferAddedEvent): Void? {
                updateTransfer(event.transfer)
                return null
            }

            override fun visit(event: WalletTransferChangedEvent): Void? {
                updateTransfer(event.transfer)
                return null
            }

            override fun visit(event: WalletTransferSubmittedEvent): Void? {
                updateTransfer(event.transfer)
                return null
            }
        })
    }

    override fun handleManagerEvent(system: System?, manager: WalletManager, event: WalletManagerEvent) {
        event.accept(object : DefaultWalletManagerEventVisitor<Void>() {
            override fun visit(event: WalletManagerSyncProgressEvent): Void? {
                output.accept(WalletEvent.OnSyncProgressUpdated(
                        progress = event.percentComplete.toDouble(), // TODO: Use float to match core.
                        syncThroughMillis = event.timestamp.transform { it?.time }.or(0L)
                ))
                return null
            }
        })
    }

    private fun getBalance(balanceAmt : Amount) : BigDecimal {
        return balanceAmt.doubleAmount(balanceAmt.unit).or(0.0).toBigDecimal()
    }

    private fun getBalanceInFiat(balanceAmt : Amount) : BigDecimal {
        val balance = getBalance(balanceAmt)
        return RatesRepository.getInstance(context).getFiatForCrypto( balance, balanceAmt.currency.code, BRSharedPrefs.getPreferredFiatIso(context)) ?: BigDecimal.ZERO
    }

    private fun updateTransfer(transfer : Transfer) {
        val updatedTx = wallet.transfers
                .find { it.getHashAsString() == transfer.getHashAsString()} ?: return

        output.accept(WalletEvent.OnTransactionUpdated(updatedTx.asWalletTransaction()))
    }

    private fun Transfer.getHashAsString() : String {
        return hash.transform { it.toString() }.or("")
    }

    private fun Transfer.asWalletTransaction() : WalletTransaction {

        val txHash = getHashAsString()
        val metaData = KVStoreManager.getTxMetaData(context, txHash.toByteArray())

        val confirmations = getConfirmations().or(UnsignedLong.ZERO).toInt()

        val levels = when {
            confirmations > 4 -> BRConstants.CONFIRMED_BLOCKS_NUMBER
            confirmations <= 0 -> 2 //TODO: how to replace relay count logic?
            else -> confirmations + 2
        }

        val blockHeight = when (confirmation.isPresent) {
            true -> confirmation.get().blockNumber.toInt()
            false -> 0
        }

        val timeStamp = when (confirmation.isPresent) {
            true -> confirmation.get().confirmationTime.time
            false -> 0L
        }

        return WalletTransaction(
                txHash = txHash,
                amount = getBalance(amount),
                amountInFiat = getBalanceInFiat(amount),
                fiatWhenSent = 0f, // TODO: Rates info
                toAddress = target.transform { it.toString() }.or("<unknown>"),
                fromAddress = source.transform { it.toString() }.or("<unknown>"),
                isReceived = direction == TransferDirection.RECEIVED,
                isErrored = state.type == TransferState.Type.FAILED,
                memo = metaData?.comment.orEmpty(),
                isValid = true, // TODO: do we have this info?
                fee = fee.doubleAmount(unitForFee.base).or(0.0).toBigDecimal(),
                blockHeight = blockHeight,
                confirmations = confirmations,
                timeStamp = timeStamp,
                levels = levels,
                currencyCode = currencyCode,
                feeForToken = null // TODO: Either establish this via meta-data (like iOS) or compare toAddress with token addresses as in pre-Generic Core
        )
    }
}