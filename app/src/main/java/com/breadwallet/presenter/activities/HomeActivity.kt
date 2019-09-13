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

package com.breadwallet.presenter.activities

import android.content.Intent
import android.os.Bundle
import android.support.v7.widget.LinearLayoutManager
import android.view.View
import androidx.core.view.isGone
import androidx.core.view.isVisible

import com.breadwallet.BuildConfig
import com.breadwallet.R
import com.breadwallet.presenter.activities.util.BRActivity
import com.breadwallet.tools.adapter.WalletListAdapter
import com.breadwallet.tools.listeners.RecyclerItemClickListener
import com.breadwallet.tools.manager.BRSharedPrefs
import com.breadwallet.tools.manager.PromptManager
import com.breadwallet.tools.util.CurrencyUtils
import com.breadwallet.ui.global.effect.NavigationEffect
import com.breadwallet.ui.global.effect.NavigationEffectHandler
import com.breadwallet.ui.global.event.InternetEvent
import com.breadwallet.ui.global.eventsource.InternetConnectivityEventSource
import com.breadwallet.ui.home.*
import com.breadwallet.ui.util.CompositeEffectHandler
import com.breadwallet.ui.util.nestedConnectable
import com.breadwallet.ui.util.nestedEventSource
import com.breadwallet.ui.util.QueuedConsumer
import com.spotify.mobius.*
import com.spotify.mobius.android.AndroidLogger
import com.spotify.mobius.android.MobiusAndroid
import com.spotify.mobius.android.runners.MainThreadWorkRunner
import com.spotify.mobius.disposables.Disposable
import com.spotify.mobius.functions.Consumer
import com.spotify.mobius.runners.WorkRunner
import com.spotify.mobius.runners.WorkRunners
import kotlinx.android.synthetic.main.activity_home.*
import kotlinx.android.synthetic.main.activity_home.notification_bar

/**
 * Home activity that will show a list of a user's wallets
 */

class HomeActivity : BRActivity(), EventSource<HomeScreenEvent> {

    private var mAdapter: WalletListAdapter? = null
    var eventConsumer: Consumer<HomeScreenEvent> = QueuedConsumer()
        private set

    private var loopFactory: MobiusLoop.Factory<HomeScreenModel, HomeScreenEvent, HomeScreenEffect> = Mobius
            .loop(HomeScreenUpdate,
                    CompositeEffectHandler.from(
                            Connectable { output -> HomeScreenEffectHandler(output, this@HomeActivity) },
                            nestedConnectable({ NavigationEffectHandler(this@HomeActivity) }, { effect ->
                                when (effect) {
                                    is HomeScreenEffect.GoToDeepLink -> NavigationEffect.GoToDeepLink(effect.url)
                                    is HomeScreenEffect.GoToInappMessage -> NavigationEffect.GoToInAppMessage(effect.inAppMessage)
                                    is HomeScreenEffect.GoToWallet -> NavigationEffect.GoToWallet(effect.currencyCode)
                                    HomeScreenEffect.GoToBuy -> NavigationEffect.GoToBuy
                                    HomeScreenEffect.GoToTrade -> NavigationEffect.GoToTrade
                                    HomeScreenEffect.GoToMenu -> NavigationEffect.GoToMenu
                                    HomeScreenEffect.GoToAddWallet -> NavigationEffect.GoToAddWallet
                                    else -> null
                                }
                            })
                    )
            )
            .eventSource(this)
            .eventSources(
                    this,
                    nestedEventSource(InternetConnectivityEventSource(this@HomeActivity)) { event ->
                        when (event) {
                            is InternetEvent.OnConnectionUpdated -> HomeScreenEvent.OnConnectionUpdated(event.isConnected)
                        }
                    }
            )
            .init(HomeScreenInit)
            .effectRunner { WorkRunners.cachedThreadPool() }
            .eventRunner { WorkRunners.cachedThreadPool() }
            .logger(AndroidLogger.tag("HomeScreenLoop"))

    private var controller: MobiusLoop.Controller<HomeScreenModel, HomeScreenEvent> = MobiusAndroid.controller(
            loopFactory,
            HomeScreenModel.createDefault()
    )

    override fun subscribe(newConsumer: Consumer<HomeScreenEvent>): Disposable {
        (eventConsumer as? QueuedConsumer)?.dequeueAll(newConsumer)
        eventConsumer = newConsumer
        return Disposable {
            eventConsumer = QueuedConsumer()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_home)

        // Show build info as a watermark on non prod builds like: TESTNET 3.10.1 build 1
        setUpBuildInfoLabel()

        rv_wallet_list.layoutManager = LinearLayoutManager(this)

        processIntentData(intent)

        mAdapter = WalletListAdapter(this)
        rv_wallet_list.adapter = mAdapter

        controller.connect { this.connectViews(it) }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        processIntentData(intent)
    }

    @Synchronized
    private fun processIntentData(intent: Intent) {
        if (intent.hasExtra(EXTRA_PUSH_NOTIFICATION_CAMPAIGN_ID)) {
            eventConsumer.accept(HomeScreenEvent.OnPushNotificationOpened(intent.getStringExtra(EXTRA_PUSH_NOTIFICATION_CAMPAIGN_ID)))
        }

        var data: String? = intent.getStringExtra(EXTRA_DATA)
        if (data.isNullOrBlank())
            data = intent.dataString

        if (data != null)
            eventConsumer.accept(HomeScreenEvent.OnDeepLinkProvided(data))
    }

    override fun onResume() {
        super.onResume()
        controller.start()
    }

    override fun onPause() {
        super.onPause()
        controller.stop()
    }

    override fun onDestroy() {
        // Note: disconnect the loop first because the super
        //   may dispose of some required resources.
        controller.disconnect()
        super.onDestroy()
    }

    fun closeNotificationBar() {
        notification_bar!!.visibility = View.INVISIBLE
    }

    private fun setUpBuildInfoLabel() {
        val network = if (BuildConfig.BITCOIN_TESTNET) NETWORK_TESTNET else NETWORK_MAINNET
        val buildInfo = "$network ${BuildConfig.VERSION_NAME} build ${BuildConfig.BUILD_VERSION}"
        testnet_label.text = buildInfo
        testnet_label.visibility = if (BuildConfig.BITCOIN_TESTNET || BuildConfig.DEBUG) View.VISIBLE else View.GONE
    }

    private fun connectViews(output: Consumer<HomeScreenEvent>): Connection<HomeScreenModel> {
        buy_layout.setOnClickListener { output.accept(HomeScreenEvent.OnBuyClicked) }
        trade_layout.setOnClickListener { output.accept(HomeScreenEvent.OnTradeClicked) }
        menu_layout.setOnClickListener { output.accept(HomeScreenEvent.OnMenuClicked) }

        rv_wallet_list.addOnItemTouchListener(RecyclerItemClickListener(this, rv_wallet_list, object : RecyclerItemClickListener.OnItemClickListener {
            override fun onItemClick(view: View, position: Int, x: Float, y: Float) {
                if (position >= mAdapter!!.itemCount || position < 0)
                    return

                if (mAdapter!!.getItemViewType(position) == 0) {
                    val currencyCode = mAdapter!!.getItemAt(position)!!.currencyCode
                    output.accept(HomeScreenEvent.OnWalletClicked(currencyCode))
                } else {
                    output.accept(HomeScreenEvent.OnAddWalletClicked)
                }
            }

            override fun onLongItemClick(view: View, position: Int) {}
        }))

        val connection = object : Connection<HomeScreenModel> {

            private var previousHasInternet = !controller.model.hasInternet // opposite of initial state to force an initial render
            private var previousShowPrompt = !controller.model.showPrompt // ditto

            override fun accept(model: HomeScreenModel) {
                mAdapter!!.setWallets(model.wallets.values.toList())

                total_assets_usd.text = CurrencyUtils.getFormattedFiatAmount(BRSharedPrefs.getPreferredFiatIso(this@HomeActivity), model.aggregatedFiatBalance)

                if (previousShowPrompt != model.showPrompt) {
                    previousShowPrompt = model.showPrompt

                    if (model.showPrompt) {
                        val promptView = PromptManager.promptInfo(this@HomeActivity, model.promptId)
                        if (list_group_layout.childCount >= MAX_NUMBER_OF_CHILDREN) {
                            list_group_layout.removeViewAt(0)
                        }
                        list_group_layout.addView(promptView, 0)
                    }
                }

                buy_bell.isVisible = model.isBuyBellNeeded

                if (model.hasInternet != previousHasInternet && notification_bar != null) {
                    previousHasInternet = model.hasInternet

                    when (model.hasInternet) {
                        true -> notification_bar.isGone = true
                        false -> {
                            notification_bar.isGone = false
                            notification_bar.bringToFront()
                        }
                    }
                }

                buy_text_view.text = if (model.showBuyAndSell) {
                    getString(R.string.HomeScreen_buyAndSell)
                } else {
                    getString(R.string.HomeScreen_buy)
                }
            }

            override fun dispose() {
            }
        }

        connection.accept(controller.model)
        return connection
    }

    companion object {
        private val TAG = HomeActivity::class.java.simpleName
        const val EXTRA_DATA = "com.breadwallet.presenter.activities.WalletActivity.EXTRA_DATA"
        const val EXTRA_PUSH_NOTIFICATION_CAMPAIGN_ID = "com.breadwallet.presenter.activities.HomeActivity.EXTRA_PUSH_CAMPAIGN_ID"
        private const val MAX_NUMBER_OF_CHILDREN = 2
        private const val NETWORK_TESTNET = "TESTNET"
        private const val NETWORK_MAINNET = "MAINNET"
    }
}
