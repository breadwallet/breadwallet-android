/**
 * BreadWallet
 *
 * Created by Pablo Budelli on <pablo.budelli@breadwallet.com> 9/10/19.
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
package com.breadwallet.ui.home

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
import com.breadwallet.ui.BaseMobiusController
import com.breadwallet.ui.global.effect.NavigationEffect
import com.breadwallet.ui.global.effect.NavigationEffectHandler
import com.breadwallet.ui.util.CompositeEffectHandler
import com.breadwallet.ui.util.nestedConnectable
import com.spotify.mobius.Connectable
import com.spotify.mobius.disposables.Disposable
import com.spotify.mobius.functions.Consumer
import kotlinx.android.synthetic.main.activity_home.*

class HomeController(
        args: Bundle? = null
) : BaseMobiusController<HomeScreenModel, HomeScreenEvent, HomeScreenEffect>(args) {

    companion object {
        private const val NETWORK_TESTNET = "TESTNET"
        private const val NETWORK_MAINNET = "MAINNET"
    }

    override val layoutId = R.layout.activity_home
    override val defaultModel = HomeScreenModel.createDefault()
    override val update = HomeScreenUpdate
    override val init = HomeScreenInit
    override val effectHandler: Connectable<HomeScreenEffect, HomeScreenEvent> =
            CompositeEffectHandler.from(
                    Connectable { output -> HomeScreenEffectHandler(output, activity as BRActivity) },
                    nestedConnectable({ NavigationEffectHandler(activity as BRActivity) }, { effect ->
                        when (effect) {
                            is HomeScreenEffect.GoToDeepLink -> NavigationEffect.GoToDeepLink(effect.url)
                            is HomeScreenEffect.GoToInappMessage ->
                                NavigationEffect.GoToInAppMessage(effect.inAppMessage)
                            is HomeScreenEffect.GoToWallet -> NavigationEffect.GoToWallet(effect.currencyCode)
                            HomeScreenEffect.GoToBuy -> NavigationEffect.GoToBuy
                            HomeScreenEffect.GoToTrade -> NavigationEffect.GoToTrade
                            HomeScreenEffect.GoToMenu -> NavigationEffect.GoToMenu
                            HomeScreenEffect.GoToAddWallet -> NavigationEffect.GoToAddWallet
                            else -> null
                        }
                    })
            )
    private var walletAdapter: WalletListAdapter? = null

    override fun bindView(output: Consumer<HomeScreenEvent>): Disposable {
        buy_layout.setOnClickListener { output.accept(HomeScreenEvent.OnBuyClicked) }
        trade_layout.setOnClickListener { output.accept(HomeScreenEvent.OnTradeClicked) }
        menu_layout.setOnClickListener { output.accept(HomeScreenEvent.OnMenuClicked) }

        walletAdapter = WalletListAdapter(activity)
        rv_wallet_list.adapter = walletAdapter
        rv_wallet_list.layoutManager = LinearLayoutManager(activity)
        rv_wallet_list.addOnItemTouchListener(RecyclerItemClickListener(activity, rv_wallet_list,
                object : RecyclerItemClickListener.OnItemClickListener {
                    override fun onItemClick(view: View, position: Int, x: Float, y: Float) {
                        if (position >= walletAdapter!!.itemCount || position < 0)
                            return

                        if (walletAdapter!!.getItemViewType(position) == 0) {
                            val currencyCode = walletAdapter!!.getItemAt(position)!!.currencyCode
                            output.accept(HomeScreenEvent.OnWalletClicked(currencyCode))
                        } else {
                            output.accept(HomeScreenEvent.OnAddWalletClicked)
                        }
                    }

                    override fun onLongItemClick(view: View, position: Int) = Unit
                }))
        return Disposable {}
    }

    override fun onCreateView(view: View) {
        super.onCreateView(view)
        setUpBuildInfoLabel()
    }

    override fun render(model: HomeScreenModel) {
        with(model) {
            ifChanged(HomeScreenModel::wallets) {
                walletAdapter!!.setWallets(wallets.values.toList())
            }

            ifChanged(HomeScreenModel::aggregatedFiatBalance) {
                total_assets_usd.text = CurrencyUtils.getFormattedFiatAmount(
                        BRSharedPrefs.getPreferredFiatIso(activity),
                        aggregatedFiatBalance)
            }

            ifChanged(HomeScreenModel::showPrompt) {
                if (model.showPrompt) {
                    val promptView = PromptManager.promptInfo(activity, model.promptId)
                    if (list_group_layout.childCount > 0) {
                        list_group_layout.removeAllViews()
                    }
                    list_group_layout.addView(promptView, 0)
                }
            }

            ifChanged(HomeScreenModel::hasInternet) {
                when (hasInternet) {
                    true -> notification_bar.isGone = true
                    false -> {
                        notification_bar.isGone = false
                        notification_bar.bringToFront()
                    }
                }
            }

            ifChanged(HomeScreenModel::isBuyBellNeeded) {
                buy_bell.isVisible = isBuyBellNeeded
            }

            ifChanged(HomeScreenModel::hasInternet) {
                buy_text_view.setText(when {
                    showBuyAndSell -> R.string.HomeScreen_buyAndSell
                    else -> R.string.HomeScreen_buy
                })
            }
        }
    }

    private fun setUpBuildInfoLabel() {
        val network = if (BuildConfig.BITCOIN_TESTNET) NETWORK_TESTNET else NETWORK_MAINNET
        val buildInfo = "$network ${BuildConfig.VERSION_NAME} build ${BuildConfig.BUILD_VERSION}"
        testnet_label.text = buildInfo
        testnet_label.visibility = if (BuildConfig.BITCOIN_TESTNET || BuildConfig.DEBUG) View.VISIBLE else View.GONE
    }
}