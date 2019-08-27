/**
 * BreadWallet
 *
 *
 * Created by Ahsan Butt <ahsan.butt@breadwallet.com> 8/1/19.
 * Copyright (c) 2019 breadwallet LLC
 *
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package com.breadwallet.ui.home

import android.content.Context
import com.breadwallet.BreadApp
import com.breadwallet.crypto.Amount
import com.breadwallet.crypto.System
import com.breadwallet.crypto.Wallet as CryptoWallet
import com.breadwallet.crypto.WalletManager
import com.breadwallet.crypto.events.wallet.*
import com.breadwallet.crypto.events.walletmanager.DefaultWalletManagerEventVisitor
import com.breadwallet.crypto.events.walletmanager.WalletManagerEvent
import com.breadwallet.crypto.events.walletmanager.WalletManagerListener
import com.breadwallet.crypto.events.walletmanager.WalletManagerSyncProgressEvent
import com.breadwallet.repository.RatesRepository
import com.breadwallet.repository.MessagesRepository
import com.breadwallet.tools.manager.BRSharedPrefs
import com.breadwallet.tools.manager.PromptManager
import com.breadwallet.tools.sqlite.RatesDataSource
import com.breadwallet.tools.util.CurrencyUtils
import com.breadwallet.tools.util.EventUtils
import com.platform.APIClient
import com.spotify.mobius.Connection
import com.spotify.mobius.functions.Consumer
import com.squareup.picasso.Callback
import com.squareup.picasso.Picasso
import java.lang.Exception
import java.math.BigDecimal

class HomeScreenEffectHandler(
    private val output: Consumer<HomeScreenEvent>,
    private val context: Context
) : Connection<HomeScreenEffect>, WalletListener, WalletManagerListener, RatesDataSource.OnDataChanged {

    private val enabledWallets = listOf("btc", "eth", "brd") // TODO: this should be replaced with a wallet management store -- also, should be listening for updates to this

    init {
        BreadApp.getCryptoSystemListener()?.apply {
            addListener(this as WalletListener)
            addListener(this as WalletManagerListener)
        }

        RatesDataSource.getInstance(context).addOnDataChangedListener(this)
    }

    override fun accept(value: HomeScreenEffect) {
        when (value) {
            HomeScreenEffect.LoadWallets -> loadWallets()
            HomeScreenEffect.LoadPrompt -> loadPrompt()
            HomeScreenEffect.LoadIsBuyBellNeeded -> loadIsBuyBellNeeded()
            HomeScreenEffect.CheckInAppNotification -> checkInAppNotification()
            is HomeScreenEffect.RecordPushNotificationOpened -> recordPushNotificationOpened(value.campaignId)
        }
    }

    override fun dispose() {
        BreadApp.getCryptoSystemListener()?.apply {
            removeListener(this as WalletListener)
            removeListener(this as WalletManagerListener)
        }

        RatesDataSource.getInstance(context).removeOnDataChangedListener(this)
    }

    private fun loadWallets() {
        output.accept(HomeScreenEvent.OnWalletsAdded(
                wallets = BreadApp.getCryptoSystem().wallets
                            .filter { isEnabledWallet(it.currency.code) }
                            .map { it.asWallet() }
        ))
    }

    private fun loadPrompt() {
        // TODO: refactor to its own generic effect handler
        val promptId = PromptManager.nextPrompt(context)
        if (promptId != null) {
            EventUtils.pushEvent(EventUtils.EVENT_PROMPT_PREFIX +
                    PromptManager.getPromptName(promptId) + EventUtils.EVENT_PROMPT_SUFFIX_DISPLAYED)
        }
        output.accept(HomeScreenEvent.OnPromptLoaded(promptId))
    }

    private fun loadIsBuyBellNeeded() {
        val isBuyBellNeeded = BRSharedPrefs.getFeatureEnabled(context, APIClient.FeatureFlags.BUY_NOTIFICATION.toString()) && CurrencyUtils.isBuyNotificationNeeded(context)
        output.accept(HomeScreenEvent.OnBuyBellNeededLoaded(isBuyBellNeeded))
    }

    private fun checkInAppNotification() {
        val notification = MessagesRepository.getInAppNotification(context) ?: return

        // If the notification contains an image we need to pre fetch it to avoid showing the image space empty
        // while we fetch the image while the notification is shown.
        when (notification.imageUrl == null) {
            true -> output.accept(HomeScreenEvent.OnInAppNotificationProvided(notification))
            false -> {
                Picasso.get().load(notification.imageUrl).fetch(object : Callback {

                    override fun onSuccess() {
                        output.accept(HomeScreenEvent.OnInAppNotificationProvided(notification))
                    }

                    override fun onError(exception: Exception) {
                    }
                })
            }
        }
    }

    private fun recordPushNotificationOpened(campaignId: String) {
        val attributes = HashMap<String, String>()
        attributes[EventUtils.EVENT_ATTRIBUTE_CAMPAIGN_ID] = campaignId
        EventUtils.pushEvent(EventUtils.EVENT_MIXPANEL_APP_OPEN, attributes)
        EventUtils.pushEvent(EventUtils.EVENT_PUSH_NOTIFICATION_OPEN)
    }

    private fun isEnabledWallet(currencyCode: String): Boolean {
        return enabledWallets.indexOfFirst { currencyCode.equals(it, true) } != -1
    }

    override fun handleWalletEvent(system: System, manager: WalletManager, wallet: CryptoWallet, event: WalletEvent) {
        if (!isEnabledWallet(wallet.currency.code))
            return

        event.accept(object : DefaultWalletEventVisitor<Void>() {
            override fun visit(event: WalletBalanceUpdatedEvent): Void? {
                val balance = event.balance
                updateBalance(wallet.currency.code, balance)
                return null
            }

            override fun visit(event: WalletCreatedEvent): Void? {
                output.accept(HomeScreenEvent.OnWalletAdded(
                        wallet = wallet.asWallet()
                ))
                return null
            }
        })
    }

    override fun handleManagerEvent(system: System?, manager: WalletManager, event: WalletManagerEvent) {
        if (!isEnabledWallet(manager.currency.code))
            return

        event.accept(object : DefaultWalletManagerEventVisitor<Void>() {
            override fun visit(event: WalletManagerSyncProgressEvent): Void? {
                output.accept(HomeScreenEvent.OnWalletSyncProgressUpdated(
                        currencyCode = manager.currency.code,
                        progress = event.percentComplete.toDouble(), // TODO: Use float to match core.
                        syncThroughMillis = event.timestamp.transform { it?.time }.or(0L)
                ))
                return null
            }
        })
    }

    override fun onChanged() {
        BreadApp.getCryptoSystem().wallets
            .filter { isEnabledWallet(it.currency.code) }
            .forEach {
                updateBalance(it.currency.code, it.balance)
            }
    }

    private fun getFiatPerPriceUnit(currencyCode: String): BigDecimal {
        return RatesRepository.getInstance(context).getFiatForCrypto(BigDecimal.ONE, currencyCode, BRSharedPrefs.getPreferredFiatIso(context))
    }

    private fun updateBalance(currencyCode: String, balanceAmt: Amount) {
        val balance = getBalance(balanceAmt)
        val balanceInFiat = getBalanceInFiat(balanceAmt)
        val fiatPricePerUnit = getFiatPerPriceUnit(currencyCode)

        output.accept(HomeScreenEvent.OnWalletBalanceUpdated(currencyCode, balance, balanceInFiat, fiatPricePerUnit))
    }

    private fun getBalanceAmtInBase(balance: Amount): BigDecimal {
        return balance.doubleAmount(balance.unit.base).or(0.0).toBigDecimal()
    }

    private fun getBalance(balanceAmt: Amount) : BigDecimal {
        return balanceAmt.doubleAmount(balanceAmt.unit).or(0.0).toBigDecimal()
    }

    private fun getBalanceInFiat(balanceAmt: Amount): BigDecimal {
        val balance = getBalance(balanceAmt)
        return RatesRepository.getInstance(context).getFiatForCrypto(balance, balanceAmt.currency.code, BRSharedPrefs.getPreferredFiatIso(context)) ?: BigDecimal.ZERO
    }

    private fun CryptoWallet.asWallet(): Wallet {
        return Wallet(
                currencyName = currency.name,
                currencyCode = currency.code,
                fiatPricePerUnit = getFiatPerPriceUnit(currency.code),
                balance = getBalance(balance),
                fiatBalance = getBalanceInFiat(balance),
                syncProgress = 0.0, // will update via sync events
                syncingThroughMillis = 0L // will update via sync events
        )
    }
}
