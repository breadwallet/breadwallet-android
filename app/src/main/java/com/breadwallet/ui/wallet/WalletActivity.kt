package com.breadwallet.ui.wallet

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.app.Activity
import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.support.constraint.ConstraintSet
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.text.format.DateUtils
import android.transition.TransitionManager
import android.util.Log
import android.util.TypedValue
import android.view.View
import android.view.animation.AnimationUtils
import android.widget.Button
import android.widget.ImageButton

import com.breadwallet.R
import com.breadwallet.presenter.activities.HomeActivity
import com.breadwallet.presenter.activities.util.BRActivity
import com.breadwallet.presenter.entities.CryptoRequest
import com.breadwallet.tools.animation.BRDialog
import com.breadwallet.tools.animation.UiUtils
import com.breadwallet.tools.manager.BRSharedPrefs
import com.breadwallet.tools.manager.FontManager
import com.breadwallet.tools.threads.executor.BRExecutor
import com.breadwallet.tools.util.BRConstants
import com.breadwallet.tools.util.CurrencyUtils
import com.breadwallet.tools.util.TokenUtil
import com.breadwallet.ui.global.effect.CryptoWalletEffectHandler
import com.breadwallet.ui.global.effect.NavigationEffect
import com.breadwallet.ui.global.effect.NavigationEffectHandler
import com.breadwallet.ui.global.effect.WalletEffect
import com.breadwallet.ui.global.event.WalletEvent
import com.breadwallet.ui.util.CompositeEffectHandler
import com.breadwallet.ui.util.nestedConnectable
import com.breadwallet.wallet.WalletsMaster
import com.breadwallet.wallet.wallets.ethereum.WalletEthManager
import com.spotify.mobius.*
import com.spotify.mobius.android.AndroidLogger
import com.spotify.mobius.android.MobiusAndroid
import com.spotify.mobius.android.runners.MainThreadWorkRunner
import com.spotify.mobius.disposables.Disposable
import com.spotify.mobius.functions.Consumer
import com.spotify.mobius.runners.WorkRunners
import kotlinx.android.synthetic.main.activity_wallet.*
import kotlinx.android.synthetic.main.wallet_sync_progress_view.*



import java.math.BigDecimal
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Locale

/**
 * Created by byfieldj on 1/16/18.
 *
 * This activity will display pricing and transaction information for any currency the user has access to
 * (BTC, BCH, ETH)
 */
open class WalletActivity : BRActivity(), EventSource<WalletScreenEvent> {

    private var mAdapter: TransactionListAdapter? = null
    var eventConsumer: Consumer<WalletScreenEvent> = QueuedConsumer()
        private set

    private var loopFactory: MobiusLoop.Factory<WalletScreenModel, WalletScreenEvent, WalletScreenEffect> = Mobius
            .loop(WalletUpdate,
                    CompositeEffectHandler.from(
                            Connectable { output -> WalletScreenEffectHandler(output) },
                            // Create nested handler, such that WalletScreenEffects are converted into WalletEffects and passed to WalletEffectHandler
                            // (events produced are converted into WalletScreenEvents)
                            nestedConnectable({ output: Consumer<WalletEvent> ->
                                CryptoWalletEffectHandler(output, this@WalletActivity, intent.getStringExtra(EXTRA_CURRENCY_CODE))
                            }, { effect: WalletScreenEffect ->
                                // Map incoming effect
                                when (effect) {
                                    is WalletScreenEffect.LoadWalletBalance -> WalletEffect.LoadWalletBalance(effect.currencyId)
                                    is WalletScreenEffect.LoadTransactions -> WalletEffect.LoadTransactions(effect.currencyId)
                                    else -> null
                                }
                            }, { event: WalletEvent ->
                                // Map outgoing event
                                when (event) {
                                    is WalletEvent.OnSyncProgressUpdated -> WalletScreenEvent.OnSyncProgressUpdated(event.progress, event.syncThroughMillis)
                                    is WalletEvent.OnBalanceUpdated -> WalletScreenEvent.OnBalanceUpdated(event.balance, event.fiatBalance)
                                    is WalletEvent.OnConnectionUpdated -> WalletScreenEvent.OnConnectionUpdated(event.isConnected)
                                    is WalletEvent.OnCurrencyNameUpdated -> WalletScreenEvent.OnCurrencyNameUpdated(event.name)
                                    is WalletEvent.OnTransactionAdded -> WalletScreenEvent.OnTransactionAdded(event.walletTransaction)
                                    is WalletEvent.OnTransactionRemoved -> WalletScreenEvent.OnTransactionRemoved(event.walletTransaction)
                                    is WalletEvent.OnTransactionUpdated -> WalletScreenEvent.OnTransactionUpdated(event.walletTransaction)
                                    is WalletEvent.OnTransactionsUpdated -> WalletScreenEvent.OnTransactionsUpdated(event.walletTransactions)
                                }
                            }),
                            Connectable { output -> WalletReviewPromptHandler(output, this@WalletActivity, intent.getStringExtra(EXTRA_CURRENCY_CODE)) },
                            Connectable { output -> WalletRatesHandler(output, this@WalletActivity, intent.getStringExtra(EXTRA_CURRENCY_CODE)) },
                            nestedConnectable({ NavigationEffectHandler(this@WalletActivity) }, { effect ->
                                when (effect) {
                                    is WalletScreenEffect.GoToSend -> NavigationEffect.GoToSend(effect.currencyId, effect.cryptoRequest)
                                    is WalletScreenEffect.GoToReceive -> NavigationEffect.GoToReceive(effect.currencyId)
                                    is WalletScreenEffect.GoToTransaction -> NavigationEffect.GoToTransaction(effect.currencyId, effect.txHash)
                                    WalletScreenEffect.GoBack -> NavigationEffect.GoBack
                                    WalletScreenEffect.GoToBrdRewards -> NavigationEffect.GoToBrdRewards
                                    WalletScreenEffect.GoToReview -> NavigationEffect.GoToReview
                                    else -> null
                                }
                            })
                    )
            )
            .eventSource(this)
            .init(WalletInit)
            .effectRunner {
                // TODO: The wallet manager and repositories are not thread safe,
                //   to avoid concurrent modifications.  In the future this should
                //   be changed to a background worker.
                MainThreadWorkRunner.create()
            }
            .eventRunner { WorkRunners.cachedThreadPool() }
            .logger(AndroidLogger.tag("WalletLoop"))

    private var controller: MobiusLoop.Controller<WalletScreenModel, WalletScreenEvent> = MobiusAndroid.controller(
            loopFactory,
            WalletScreenModel.createDefault(
                    // TODO: Do not rely on shared prefs
                    BRSharedPrefs.getCurrentWalletCurrencyCode(this)
            )
    )

    override fun subscribe(newConsumer: Consumer<WalletScreenEvent>): Disposable {
        (eventConsumer as? QueuedConsumer)?.dequeueAll(newConsumer)
        eventConsumer = newConsumer
        return Disposable {
            eventConsumer = QueuedConsumer()
        }
    }

    /**
     * Check the back stack to see if send or receive fragment are present and look for FragmentTxDetail by tag.
     */
    private val isFragmentOnTop: Boolean
        get() = supportFragmentManager.backStackEntryCount > 0 || supportFragmentManager.findFragmentByTag(FragmentTxDetails.TAG) != null

    protected enum class FlipperViewState {
        BALANCE, SEARCH, NETWORK
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_wallet)

        BRSharedPrefs.putIsNewWallet(this, false)

        tool_bar_flipper.inAnimation = AnimationUtils.loadAnimation(this, R.anim.flipper_enter)
        tool_bar_flipper.outAnimation = AnimationUtils.loadAnimation(this, R.anim.flipper_exit)

        // TODO: Add sell click event sell_button.setOnClickListener(view -> UiUtils.startPlatformBrowser(WalletActivity.this, HTTPServer.getPlatformUrl(HTTPServer.URL_SELL)));

        updateUi()

        // TODO: Migrate delisted token check and "more info" button to new framework (effect for checking if delisted, event for showing fragment etc.)
        // Not sure, if Generic Core has a notion of delisted token and whether it manages this
        val moreInfoButton = delisted_token_layout!!.findViewById<Button>(R.id.more_info_button)
        moreInfoButton.setOnClickListener { view -> UiUtils.showSupportFragment(this@WalletActivity, BRConstants.FAQ_UNSUPPORTED_TOKEN, null) }

        controller.connect { this.connectViews(it) }
    }

    override fun onDestroy() {
        // Note: disconnect the loop first because the super
        //   may dispose of some required resources.
        controller.disconnect()
        super.onDestroy()
    }

    /**
     * This token is no longer supported by the BRD app, notify the user.
     */
    private fun showDelistedTokenBanner() {
        delisted_token_layout!!.visibility = View.VISIBLE
    }

    private fun updateUi() {
        // TODO: Not 100% sure where to move this but it harms nothing by remaining here at this time
        val walletManager = WalletsMaster.getInstance().getCurrentWallet(this)
        if (walletManager == null) {
            Log.e(TAG, "updateUi: wallet is null")
            return
        }

        val startColor = walletManager.uiConfiguration.startColor
        val endColor = walletManager.uiConfiguration.endColor
        val currentTheme = UiUtils.getThemeId(this)

        if (currentTheme == R.style.AppTheme_Dark) {
            send_button!!.setColor(getColor(R.color.wallet_footer_button_color_dark))
            receive_button!!.setColor(getColor(R.color.wallet_footer_button_color_dark))
            sell_button!!.setColor(getColor(R.color.wallet_footer_button_color_dark))

            if (endColor != null) {
                bottom_toolbar_layout1!!.setBackgroundColor(Color.parseColor(endColor))
            }
        } else if (endColor != null) {
            send_button!!.setColor(Color.parseColor(endColor))
            receive_button!!.setColor(Color.parseColor(endColor))
            sell_button!!.setColor(Color.parseColor(endColor))
        }

        if (endColor != null) {
            //it's a gradient
            val gd = GradientDrawable(
                    GradientDrawable.Orientation.LEFT_RIGHT,
                    intArrayOf(Color.parseColor(startColor), Color.parseColor(endColor)))
            gd.cornerRadius = 0f
            bread_bar!!.background = gd
        } else {
            //it's a solid color
            bread_bar!!.setBackgroundColor(Color.parseColor(startColor))
        }
    }

    private fun setPriceTags(cryptoPreferred: Boolean, animate: Boolean) {
        val set = ConstraintSet()
        set.clone(bread_toolbar!!)
        if (animate) {
            TransitionManager.beginDelayedTransition(bread_toolbar)
        }
        val balanceTextMargin = resources.getDimension(R.dimen.balance_text_margin).toInt()
        val balancePrimaryPadding = resources.getDimension(R.dimen.balance_primary_padding).toInt()
        val balanceSecondaryPadding = resources.getDimension(R.dimen.balance_secondary_padding).toInt()
        val primaryTextSize = TypedValue()
        resources.getValue(R.dimen.wallet_balance_primary_text_size, primaryTextSize, true)
        val secondaryTextSize = TypedValue()
        resources.getValue(R.dimen.wallet_balance_secondary_text_size, secondaryTextSize, true)
        // CRYPTO on RIGHT
        if (cryptoPreferred) {
            // Align crypto balance to the right parent
            set.connect(R.id.balance_secondary, ConstraintSet.END,
                    ConstraintSet.PARENT_ID, ConstraintSet.END, balanceTextMargin)
            balance_primary!!.setPadding(0, balancePrimaryPadding, 0, 0)
            balance_secondary!!.setPadding(0, balanceSecondaryPadding, 0, 0)

            // Align swap icon to left of crypto balance
            set.connect(R.id.swap, ConstraintSet.END, R.id.balance_secondary, ConstraintSet.START, balanceTextMargin)

            // Align usd balance to left of swap icon
            set.connect(R.id.balance_primary, ConstraintSet.END, R.id.swap, ConstraintSet.START, balanceTextMargin)

            balance_secondary!!.textSize = primaryTextSize.float
            balance_primary!!.textSize = secondaryTextSize.float

            set.applyTo(bread_toolbar!!)

        } else {
            // CRYPTO on LEFT
            // Align primary to right of parent
            set.connect(R.id.balance_primary, ConstraintSet.END, ConstraintSet.PARENT_ID,
                    ConstraintSet.END, balanceTextMargin)

            // Align swap icon to left of usd balance
            set.connect(R.id.swap, ConstraintSet.END, R.id.balance_primary, ConstraintSet.START, balanceTextMargin)


            // Align secondary currency to the left of swap icon
            set.connect(R.id.balance_secondary, ConstraintSet.END, R.id.swap, ConstraintSet.START, balanceTextMargin)
            balance_primary!!.setPadding(0, balanceSecondaryPadding, 0, 0)
            balance_secondary!!.setPadding(0, balancePrimaryPadding, 0, 0)

            balance_secondary!!.textSize = secondaryTextSize.float
            balance_primary!!.textSize = primaryTextSize.float

            set.applyTo(bread_toolbar!!)
        }
        balance_secondary!!.setTextColor(resources.getColor(if (cryptoPreferred)
            R.color.white
        else
            R.color.currency_subheading_color, null))
        balance_primary!!.setTextColor(resources.getColor(if (cryptoPreferred)
            R.color.currency_subheading_color
        else
            R.color.white, null))
        val circularBoldFont = getString(R.string.Font_CircularPro_Bold)
        val circularBookFont = getString(R.string.Font_CircularPro_Book)
        balance_secondary!!.typeface = FontManager.get(this, if (cryptoPreferred) circularBoldFont else circularBookFont)
        balance_primary!!.typeface = FontManager.get(this, if (cryptoPreferred) circularBookFont else circularBoldFont)
    }

    override fun onResume() {
        super.onResume()
        controller.start()

        if (!TokenUtil.isTokenSupported(controller.model.currencyCode)) {
            showDelistedTokenBanner()
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        val request = intent.getSerializableExtra(EXTRA_CRYPTO_REQUEST) as? CryptoRequest
        intent.removeExtra(EXTRA_CRYPTO_REQUEST)
        if (request != null) {
            eventConsumer.accept(WalletScreenEvent.OnSendRequestGiven(request))
        }
    }


    override fun onPause() {
        super.onPause()
        controller.stop()
    }

    override fun onBackPressed() {
        val entryCount = fragmentManager.backStackEntryCount
        if (entryCount > 0) {
            super.onBackPressed()
        } else {
            val intent = Intent(this, HomeActivity::class.java)
            startActivity(intent)
            overridePendingTransition(R.anim.enter_from_left, R.anim.exit_to_right)
            if (!isDestroyed) {
                finish()
            }
        }
    }

    private fun connectViews(output: Consumer<WalletScreenEvent>): Connection<WalletScreenModel> {
        // Tx Action buttons
        send_button.setHasShadow(false)
        send_button.setOnClickListener { view -> output.accept(WalletScreenEvent.OnSendClicked) }
        receive_button.setHasShadow(false)
        receive_button.setOnClickListener { view -> output.accept(WalletScreenEvent.OnReceiveClicked) }

        // Tx List
        val txList = findViewById<RecyclerView>(R.id.tx_list)
        txList.layoutManager = LinearLayoutManager(this)
        mAdapter = TransactionListAdapter(this, null) { (txHash) -> output.accept(WalletScreenEvent.OnTransactionClicked(txHash)) }
        txList.adapter = mAdapter

        // Search button
        val searchIcon = findViewById<ImageButton>(R.id.search_icon)
        searchIcon.setOnClickListener { view -> output.accept(WalletScreenEvent.OnSearchClicked) }
        search_bar.setEventOutput(output)

        // Display currency buttons
        val displayCurrencyListener = View.OnClickListener {
            output.accept(WalletScreenEvent.OnChangeDisplayCurrencyClicked)
        }
        balance_primary.setOnClickListener(displayCurrencyListener)
        balance_secondary.setOnClickListener(displayCurrencyListener)

        // Back button
        back_icon.setOnClickListener { output.accept(WalletScreenEvent.OnBackClicked) }

        val connection = object : Connection<WalletScreenModel> {

            private var previousIsCryptoPreferred: Boolean? = !controller.model.isCryptoPreferred // opposite of initial state to force an initial render
            private var previousFiatBalance: BigDecimal? = null // null to force an initial render
            private var previousCryptoBalance: BigDecimal? = null // null to force an initial render
            private var previousSyncProgress = controller.model.syncProgress

            override fun accept(model: WalletScreenModel) {
                currency_label.text = model.currencyName

                val switchingIsCryptoPreferred = previousIsCryptoPreferred != model.isCryptoPreferred
                previousIsCryptoPreferred = model.isCryptoPreferred

                // Update fiat balance
                if (previousFiatBalance == null || previousFiatBalance != model.fiatBalance) {
                    previousFiatBalance = model.fiatBalance
                    val formattedFiatBalance = CurrencyUtils.getFormattedFiatAmount(
                            // TODO: Move preferred fiat iso to model
                            BRSharedPrefs.getPreferredFiatIso(this@WalletActivity), model.fiatBalance)

                    balance_primary.text = formattedFiatBalance
                }

                // Update crypto balance
                if (previousCryptoBalance == null || previousCryptoBalance != model.balance) {
                    previousCryptoBalance = model.balance
                    val formattedCryptoBalance = CurrencyUtils.getFormattedCryptoAmount(model.currencyCode, model.balance)

                    balance_secondary.text = formattedCryptoBalance
                }

                if (switchingIsCryptoPreferred)
                    setPriceTags(model.isCryptoPreferred, true)

                // Update price per unit
                currency_usd_price.text = String.format(getString(R.string.Account_exchangeRate),
                        model.fiatPricePerUnit, model.currencyCode)
                if (model.hasPricePerUnit) {
                    currency_usd_price.visibility = View.VISIBLE
                } else {
                    currency_usd_price.visibility = View.INVISIBLE
                }

                // Refresh adapter if tx list is updated or display fiat changed
                var adapterHasChanged = false
                if (model.isFilterApplied) {
                    if (mAdapter!!.items !== model.filteredTransactions) {
                        mAdapter!!.setItems(model.filteredTransactions)
                        adapterHasChanged = true
                    }
                } else if (mAdapter!!.items !== model.transactions) {
                    mAdapter!!.setItems(model.transactions)
                    adapterHasChanged = true
                }

                if (switchingIsCryptoPreferred) {
                    mAdapter!!.setIsCryptoPreferred(model.isCryptoPreferred)
                    adapterHasChanged = true
                }

                if (adapterHasChanged)
                    mAdapter!!.notifyDataSetChanged()

                // Update header area
                val displayedChild = tool_bar_flipper.displayedChild
                if (model.hasInternet) {
                    if (model.isShowingSearch) {
                        if (displayedChild != FlipperViewState.SEARCH.ordinal) {
                            tool_bar_flipper.displayedChild = FlipperViewState.SEARCH.ordinal
                        }
                        search_bar.onShow(true)
                        search_bar.render(model)
                    } else {
                        if (displayedChild != FlipperViewState.BALANCE.ordinal) {
                            tool_bar_flipper.displayedChild = FlipperViewState.BALANCE.ordinal
                        }
                        search_bar.onShow(false)
                    }
                } else {
                    if (displayedChild != FlipperViewState.NETWORK.ordinal) {
                        tool_bar_flipper.displayedChild = FlipperViewState.NETWORK.ordinal
                    }
                }

                // Update sync progress
                if (previousSyncProgress != model.syncProgress) {
                    previousSyncProgress = model.syncProgress

                    if (model.isSyncing) {
                        val labelText = StringBuffer(getString(R.string.SyncingView_syncing))
                        labelText.append(' ')
                                .append(NumberFormat.getPercentInstance().format(model.syncProgress))
                        syncing_label.text = labelText
                        progress_layout.visibility = View.VISIBLE

                        if (model.hasSyncTime) {
                            val syncedThroughDate = SimpleDateFormat(SYNCED_THROUGH_DATE_FORMAT,
                                    Locale.getDefault()).format(model.syncingThroughMillis)
                            sync_status_label.text = String.format(getString(R.string.SyncingView_syncedThrough),
                                    syncedThroughDate)
                        }
                    } else {
                        progress_layout.animate()
                                .translationY((-progress_layout.height).toFloat())
                                .alpha(SYNC_PROGRESS_LAYOUT_ANIMATION_ALPHA)
                                .setDuration(DateUtils.SECOND_IN_MILLIS)
                                .setListener(object : AnimatorListenerAdapter() {
                                    override fun onAnimationEnd(animation: Animator) {
                                        super.onAnimationEnd(animation)
                                        progress_layout.visibility = View.GONE
                                    }
                                })
                    }
                }

                // Show Review Prompt Dialog
                if (model.showReviewPrompt && !model.isShowingReviewPrompt) {
                    if (!isFragmentOnTop) {
                        // Show dialog
                        BRDialog.showCustomDialog(
                                this@WalletActivity,
                                getString(R.string.RateAppPrompt_Title_Android),
                                getString(R.string.RateAppPrompt_Body_Android),
                                getString(R.string.RateAppPrompt_Button_RateApp_Android),
                                getString(R.string.RateAppPrompt_Button_Dismiss_Android),
                                { // positiveButton
                                    brDialogView ->
                                    output.accept(WalletScreenEvent.OnReviewPromptAccepted)
                                    brDialogView.dismiss() // NOTE: This causes us to over-count dismisses, as each positive button click also calls dismiss handler (known issue)
                                },
                                { // negativeButton
                                    brDialogView ->
                                    output.accept(WalletScreenEvent.OnHideReviewPrompt(false))
                                    brDialogView.dismiss() // NOTE: see above comment about over-counting
                                },
                                { // onDismiss
                                    brDialogView ->
                                    output.accept(WalletScreenEvent.OnHideReviewPrompt(true))
                                },
                                0
                        )

                        output.accept(WalletScreenEvent.OnIsShowingReviewPrompt)
                    } else {
                        // dispatch showReviewPrompt not shown evt (turn both showReviewPrompt and isShowing == false)
                    }
                }
            }

            override fun dispose() {
                search_bar.setEventOutput(null)
            }
        }
        // TODO: This fixes janky animations when using animateLayoutChanges
        //  something for Drew to investigate later :)
        connection.accept(controller.model)
        return connection
    }

    companion object {
        private val TAG = WalletActivity::class.java.name

        const val EXTRA_CRYPTO_REQUEST = "com.breadwallet.ui.wallet.WalletActivity.EXTRA_CRYPTO_REQUEST"
        protected const val EXTRA_CURRENCY_CODE = "com.breadwallet.ui.wallet.WalletActivity.EXTRA_CURRENCY_CODE"
        private const val SYNCED_THROUGH_DATE_FORMAT = "MM/dd/yy HH:mm"
        private const val SYNC_PROGRESS_LAYOUT_ANIMATION_ALPHA = 0.0f

        /**
         * Start the wallet activity for the given currency.
         *
         * @param callerActivity Activity from where WalletActivity is started.
         * @param currencyCode   The currency code of the wallet to be shown.
         */
        fun start(callerActivity: Activity, currencyCode: String) {
            val intent = Intent(callerActivity, WalletActivity::class.java)
            intent.putExtra(EXTRA_CURRENCY_CODE, currencyCode)
            callerActivity.startActivity(intent)
        }
    }
}
