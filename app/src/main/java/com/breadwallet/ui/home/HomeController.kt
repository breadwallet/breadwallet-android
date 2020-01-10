/**
 * BreadWallet
 *
 * Created by Pablo Budelli <pablo.budelli@breadwallet.com> on 9/10/19.
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
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.ItemTouchHelper
import android.view.View
import android.widget.Button
import android.widget.ImageButton
import android.widget.ImageView
import androidx.core.view.isGone
import androidx.core.view.isVisible
import com.breadwallet.BuildConfig
import com.breadwallet.R
import com.breadwallet.legacy.presenter.activities.util.BRActivity
import com.breadwallet.legacy.presenter.customviews.BRButton
import com.breadwallet.legacy.presenter.customviews.BREdit
import com.breadwallet.legacy.presenter.customviews.BaseTextView
import com.breadwallet.mobius.CompositeEffectHandler
import com.breadwallet.mobius.nestedConnectable
import com.breadwallet.tools.animation.SimpleItemTouchHelperCallback
import com.breadwallet.tools.animation.SpringAnimator
import com.breadwallet.tools.manager.BRSharedPrefs
import com.breadwallet.tools.util.CurrencyUtils
import com.breadwallet.ui.BaseMobiusController
import com.breadwallet.ui.navigation.NavigationEffect
import com.breadwallet.ui.navigation.NavigationEffectHandler
import com.breadwallet.ui.navigation.OnCompleteAction
import com.breadwallet.ui.navigation.RouterNavigationEffectHandler
import com.breadwallet.ui.settings.SettingsSection
import com.breadwallet.util.isValidEmail
import com.spotify.mobius.Connectable
import com.spotify.mobius.disposables.Disposable
import com.spotify.mobius.functions.Consumer
import kotlinx.android.synthetic.main.activity_home.*
import kotlinx.coroutines.Dispatchers.Main
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.kodein.di.direct
import org.kodein.di.erased.instance

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
            Connectable { output ->
                HomeScreenEffectHandler(
                    output,
                    activity as BRActivity,
                    direct.instance(),
                    direct.instance(),
                    direct.instance()
                )
            },
            Connectable { output ->
                PromptEffectHandler(output, activity!!)
            },
            nestedConnectable({ direct.instance<NavigationEffectHandler>() }, { effect ->
                when (effect) {
                    is HomeScreenEffect.GoToDeepLink -> NavigationEffect.GoToDeepLink(effect.url)
                    is HomeScreenEffect.GoToInappMessage ->
                        NavigationEffect.GoToInAppMessage(effect.inAppMessage)
                    HomeScreenEffect.GoToBuy -> NavigationEffect.GoToBuy
                    HomeScreenEffect.GoToTrade -> NavigationEffect.GoToTrade
                    else -> null
                }
            }),
            nestedConnectable({ direct.instance<RouterNavigationEffectHandler>() }) { effect ->
                when (effect) {
                    HomeScreenEffect.GoToMenu -> NavigationEffect.GoToMenu(SettingsSection.HOME)
                    HomeScreenEffect.GoToWriteDownKey -> NavigationEffect.GoToWriteDownKey(
                        OnCompleteAction.GO_HOME
                    )
                    is HomeScreenEffect.GoToWallet ->
                        NavigationEffect.GoToWallet(effect.currencyCode)
                    is HomeScreenEffect.GoToAddWallet ->
                        NavigationEffect.GoToAddWallet
                    else -> null
                }
            }
        )

    private var walletAdapter: WalletListAdapter? = null

    override fun bindView(output: Consumer<HomeScreenEvent>): Disposable {
        buy_layout.setOnClickListener { output.accept(HomeScreenEvent.OnBuyClicked) }
        trade_layout.setOnClickListener { output.accept(HomeScreenEvent.OnTradeClicked) }
        menu_layout.setOnClickListener { output.accept(HomeScreenEvent.OnMenuClicked) }

        walletAdapter = WalletListAdapter({
            output.accept(HomeScreenEvent.OnWalletClicked(it.currencyCode))
        }, {
            output.accept(HomeScreenEvent.OnAddWalletsClicked)
        }) { displayOrder ->
            output.accept(HomeScreenEvent.OnWalletDisplayOrderUpdated(displayOrder))
        }

        rv_wallet_list.adapter = walletAdapter
        rv_wallet_list.layoutManager = LinearLayoutManager(activity)

        ItemTouchHelper(object : SimpleItemTouchHelperCallback(walletAdapter) {
            override fun isItemViewSwipeEnabled() = false
        }).attachToRecyclerView(rv_wallet_list)
        return Disposable {}
    }

    override fun onCreateView(view: View) {
        super.onCreateView(view)
        setUpBuildInfoLabel()
    }

    override fun HomeScreenModel.render() {
        ifChanged(HomeScreenModel::displayOrder) {
            walletAdapter!!.setDisplayOrder(displayOrder)
        }

        ifChanged(HomeScreenModel::wallets) {
            walletAdapter!!.setWallets(wallets.values.toList())
        }

        ifChanged(HomeScreenModel::aggregatedFiatBalance) {
            total_assets_usd.text = CurrencyUtils.getFormattedFiatAmount(
                BRSharedPrefs.getPreferredFiatIso(activity),
                aggregatedFiatBalance
            )
        }

        ifChanged(HomeScreenModel::showPrompt) {
            if (prompt_container.childCount > 0) {
                prompt_container.removeAllViews()
            }
            if (showPrompt) {
                val promptView = getPromptView(promptId!!)
                prompt_container.addView(promptView, 0)
            }
        }

        ifChanged(HomeScreenModel::hasInternet) {
            notification_bar.apply {
                isGone = hasInternet
                if (hasInternet) bringToFront()
            }
        }

        ifChanged(HomeScreenModel::isBuyBellNeeded) {
            buy_bell.isVisible = isBuyBellNeeded
        }

        ifChanged(HomeScreenModel::hasInternet) {
            buy_text_view.setText(
                when {
                    showBuyAndSell -> R.string.HomeScreen_buyAndSell
                    else -> R.string.HomeScreen_buy
                }
            )
        }
    }

    private fun setUpBuildInfoLabel() {
        val network = if (BuildConfig.BITCOIN_TESTNET) NETWORK_TESTNET else NETWORK_MAINNET
        val buildInfo = "$network ${BuildConfig.VERSION_NAME} build ${BuildConfig.BUILD_VERSION}"
        testnet_label.text = buildInfo
        testnet_label.isVisible = BuildConfig.BITCOIN_TESTNET || BuildConfig.DEBUG
    }

    private fun getPromptView(promptItem: PromptItem): View {
        val act = checkNotNull(activity)

        val baseLayout = act.layoutInflater.inflate(R.layout.base_prompt, prompt_container, false)
        val title = baseLayout.findViewById<BaseTextView>(R.id.prompt_title)
        val description = baseLayout.findViewById<BaseTextView>(R.id.prompt_description)
        val continueButton = baseLayout.findViewById<Button>(R.id.continue_button)
        val dismissButton = baseLayout.findViewById<ImageButton>(R.id.dismiss_button)
        dismissButton.setOnClickListener {
            eventConsumer.accept(HomeScreenEvent.OnPromptDismissed(promptItem))
        }
        when (promptItem) {
            PromptItem.FINGER_PRINT -> {
                title.text = act.getString(R.string.Prompts_TouchId_title_android)
                description.text = act.getString(R.string.Prompts_TouchId_body_android)
                continueButton.setOnClickListener {
                    eventConsumer.accept(HomeScreenEvent.OnFingerprintPromptClicked)
                }
            }
            PromptItem.PAPER_KEY -> {
                title.text = act.getString(R.string.Prompts_PaperKey_title)
                description.text = act.getString(R.string.Prompts_PaperKey_Body_Android)
                continueButton.setOnClickListener {
                    eventConsumer.accept(HomeScreenEvent.OnPaperKeyPromptClicked)
                }
            }
            PromptItem.UPGRADE_PIN -> {
                title.text = act.getString(R.string.Prompts_UpgradePin_title)
                description.text = act.getString(R.string.Prompts_UpgradePin_body)
                continueButton.setOnClickListener {
                    eventConsumer.accept(HomeScreenEvent.OnUpgradePinPromptClicked)
                }
            }
            PromptItem.RECOMMEND_RESCAN -> {
                title.text = act.getString(R.string.Prompts_RecommendRescan_title)
                description.text = act.getString(R.string.Prompts_RecommendRescan_body)
                continueButton.setOnClickListener {
                    eventConsumer.accept(HomeScreenEvent.OnRescanPromptClicked)
                }
            }
            PromptItem.EMAIL_COLLECTION -> {
                return getEmailPrompt()
            }
        }
        return baseLayout
    }

    private fun getEmailPrompt(): View {
        val act = checkNotNull(activity)
        val customLayout = act.layoutInflater.inflate(R.layout.email_prompt, null)
        val customTitle = customLayout.findViewById<BaseTextView>(R.id.prompt_title)
        val customDescription =
            customLayout.findViewById<BaseTextView>(R.id.prompt_description)
        val footNote = customLayout.findViewById<BaseTextView>(R.id.prompt_footnote)
        val submitButton = customLayout.findViewById<BRButton>(R.id.submit_button)
        val closeButton = customLayout.findViewById<ImageView>(R.id.close_button)
        val emailEditText = customLayout.findViewById<BREdit>(R.id.email_edit)
        submitButton.setColor(act.getColor(R.color.create_new_wallet_button_dark))
        customTitle.text = act.getString(R.string.Prompts_Email_title)
        customDescription.text = act.getString(R.string.Prompts_Email_body)
        closeButton.setOnClickListener {
            eventConsumer.accept(HomeScreenEvent.OnPromptDismissed(PromptItem.EMAIL_COLLECTION))
        }
        submitButton.setOnClickListener {
            val email = emailEditText.text.toString().trim { it <= ' ' }
            if (email.isValidEmail()) {
                eventConsumer.accept(HomeScreenEvent.OnEmailPromptClicked(email))
                emailEditText.visibility = View.INVISIBLE
                submitButton.visibility = View.INVISIBLE
                footNote.visibility = View.VISIBLE
                customTitle.text = act.getString(R.string.Prompts_Email_successTitle)
                customDescription.text = act.getString(R.string.Prompts_Email_successBody)
                viewAttachScope.launch(Main) {
                    delay(3_000L)
                    prompt_container.removeAllViews()
                }
            } else {
                SpringAnimator.failShakeAnimation(act, emailEditText)
            }

        }
        return customLayout
    }
}

