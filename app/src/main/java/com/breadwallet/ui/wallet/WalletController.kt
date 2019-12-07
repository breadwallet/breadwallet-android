package com.breadwallet.ui.wallet

import android.animation.Animator
import android.animation.AnimatorInflater
import android.animation.AnimatorListenerAdapter
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.support.constraint.ConstraintSet
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.text.format.DateUtils
import android.transition.TransitionManager
import android.util.TypedValue
import android.view.View
import android.view.animation.AccelerateInterpolator
import androidx.core.os.bundleOf
import androidx.core.view.isVisible
import com.bluelinelabs.conductor.RouterTransaction
import com.breadwallet.R
import com.breadwallet.breadbox.BreadBoxEffect
import com.breadwallet.breadbox.BreadBoxEffectHandler
import com.breadwallet.breadbox.BreadBoxEvent
import com.breadwallet.breadbox.formatCryptoForUi
import com.breadwallet.breadbox.formatFiatForUi
import com.breadwallet.effecthandler.metadata.MetaDataEffect
import com.breadwallet.effecthandler.metadata.MetaDataEffectHandler
import com.breadwallet.effecthandler.metadata.MetaDataEvent
import com.breadwallet.legacy.presenter.customviews.BaseTextView
import com.breadwallet.logger.logDebug
import com.breadwallet.mobius.CompositeEffectHandler
import com.breadwallet.mobius.nestedConnectable
import com.breadwallet.model.PriceDataPoint
import com.breadwallet.tools.animation.UiUtils
import com.breadwallet.tools.manager.BRSharedPrefs
import com.breadwallet.tools.util.BRConstants
import com.breadwallet.tools.util.CurrencyUtils
import com.breadwallet.tools.util.TokenUtil
import com.breadwallet.ui.BaseMobiusController
import com.breadwallet.ui.navigation.NavigationEffect
import com.breadwallet.ui.navigation.NavigationEffectHandler
import com.breadwallet.ui.navigation.RouterNavigationEffectHandler
import com.breadwallet.ui.wallet.spark.SparkAdapter
import com.breadwallet.ui.wallet.spark.SparkView
import com.breadwallet.ui.wallet.spark.animation.LineSparkAnimator
import com.breadwallet.ui.web.WebController
import com.breadwallet.util.WalletDisplayUtils
import com.spotify.mobius.Connectable
import com.spotify.mobius.disposables.Disposable
import com.spotify.mobius.functions.Consumer
import kotlinx.android.synthetic.main.activity_wallet.*
import kotlinx.android.synthetic.main.chart_view.*
import kotlinx.android.synthetic.main.view_delisted_token.*
import kotlinx.android.synthetic.main.wallet_sync_progress_view.*
import kotlinx.android.synthetic.main.wallet_toolbar.*
import org.kodein.di.direct
import org.kodein.di.erased.instance
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Locale

/**
 * TODO: Remaining work: Make review prompt a controller.
 */
open class WalletController(
    args: Bundle
) : BaseMobiusController<WalletScreenModel, WalletScreenEvent, WalletScreenEffect>(args) {

    constructor(currencyCode: String) : this(
        bundleOf(EXTRA_CURRENCY_CODE to currencyCode)
    )

    private val currencyCode = arg<String>(EXTRA_CURRENCY_CODE)

    companion object {
        const val EXTRA_CURRENCY_CODE =
            "com.breadwallet.ui.wallet.WalletController.EXTRA_CURRENCY_CODE"
        private const val SYNCED_THROUGH_DATE_FORMAT = "MM/dd/yy HH:mm"
        private const val MARKET_CHART_DATE_WITH_HOUR = "MMM d, h:mm"
        private const val MARKET_CHART_DATE_WITH_YEAR = "MMM d, YYYY"
        private const val SYNC_PROGRESS_LAYOUT_ANIMATION_ALPHA = 0.0f
        private const val MARKET_CHART_ANIMATION_DURATION = 500L
        private const val MARKET_CHART_ANIMATION_ACCELERATION = 1.2f
    }

    override val layoutId = R.layout.activity_wallet

    override val defaultModel = WalletScreenModel.createDefault(currencyCode)
    override val init = WalletInit
    override val update = WalletUpdate
    override val effectHandler: Connectable<WalletScreenEffect, WalletScreenEvent> =
        CompositeEffectHandler.from(
            Connectable { output -> WalletScreenEffectHandler(output) },
            nestedConnectable({ output: Consumer<BreadBoxEvent> ->
                BreadBoxEffectHandler(output, currencyCode, direct.instance())
            }, { effect: WalletScreenEffect ->
                // Map incoming effect
                when (effect) {
                    is WalletScreenEffect.LoadWalletBalance ->
                        BreadBoxEffect.LoadWalletBalance(effect.currencyId)
                    is WalletScreenEffect.LoadTransactions ->
                        BreadBoxEffect.LoadTransactions(effect.currencyId)
                    else -> null
                }
            }, { event: BreadBoxEvent ->
                // Map outgoing event
                when (event) {
                    is BreadBoxEvent.OnSyncProgressUpdated ->
                        WalletScreenEvent.OnSyncProgressUpdated(
                            event.progress,
                            event.syncThroughMillis,
                            event.isSyncing
                        )
                    is BreadBoxEvent.OnBalanceUpdated ->
                        WalletScreenEvent.OnBalanceUpdated(event.balance, event.fiatBalance)
                    is BreadBoxEvent.OnConnectionUpdated ->
                        WalletScreenEvent.OnConnectionUpdated(event.isConnected)
                    is BreadBoxEvent.OnCurrencyNameUpdated ->
                        WalletScreenEvent.OnCurrencyNameUpdated(event.name)
                    is BreadBoxEvent.OnTransactionAdded ->
                        WalletScreenEvent.OnTransactionAdded(event.walletTransaction)
                    is BreadBoxEvent.OnTransactionRemoved ->
                        WalletScreenEvent.OnTransactionRemoved(event.walletTransaction)
                    is BreadBoxEvent.OnTransactionsUpdated ->
                        WalletScreenEvent.OnCryptoTransactionsUpdated(event.transactions)
                    else -> null
                }
            }),
            nestedConnectable({ output: Consumer<MetaDataEvent> ->
                MetaDataEffectHandler(output, direct.instance(), direct.instance())
            }, { effect: WalletScreenEffect ->
                when (effect) {
                    is WalletScreenEffect.LoadTransactionMetaData ->
                        MetaDataEffect.LoadTransactionMetaData(effect.transactionHashes)
                    else -> null
                }
            }, { event: MetaDataEvent ->
                when (event) {
                    is MetaDataEvent.OnTransactionMetaDataUpdated ->
                        WalletScreenEvent.OnTransactionMetaDataUpdated(
                            event.transactionHash,
                            event.txMetaData
                        ) as WalletScreenEvent
                    else -> null
                }
            }),
            Connectable { output ->
                WalletReviewPromptHandler(output, applicationContext!!, currencyCode)
            },
            Connectable { output ->
                WalletRatesHandler(output, applicationContext!!, currencyCode)
            },
            Connectable { output ->
                WalletHistoricalPriceIntervalHandler(output, applicationContext!!, currencyCode)
            },
            nestedConnectable(
                { direct.instance<NavigationEffectHandler>() },
                { effect: WalletScreenEffect ->
                    when (effect) {
                        WalletScreenEffect.GoBack -> NavigationEffect.GoBack
                        WalletScreenEffect.GoToBrdRewards -> NavigationEffect.GoToBrdRewards
                        WalletScreenEffect.GoToReview -> NavigationEffect.GoToReview
                        else -> null
                    }
                }),
            nestedConnectable(
                { direct.instance<RouterNavigationEffectHandler>() },
                { effect ->
                    when (effect) {
                        is WalletScreenEffect.GoToSend ->
                            NavigationEffect.GoToSend(effect.currencyId, effect.cryptoRequest)
                        is WalletScreenEffect.GoToReceive ->
                            NavigationEffect.GoToReceive(effect.currencyId)
                        is WalletScreenEffect.GoToTransaction ->
                            NavigationEffect.GoToTransaction(effect.currencyId, effect.txHash)
                        else -> null
                    }
                })
        )

    private var mAdapter: TransactionListAdapter? = null
    private var mPriceDataAdapter = SparkAdapter()
    private val mIntervalButtons: List<BaseTextView>
        get() = listOf<BaseTextView>(
            one_day,
            one_week,
            one_month,
            three_months,
            one_year,
            three_years
        )

    override fun onCreateView(view: View) {
        super.onCreateView(view)

        BRSharedPrefs.putIsNewWallet(newWallet = false) // TODO: What does this do?

        // TODO: Add sell click event
        //  sell_button.setOnClickListener(
        //  UiUtils.startPlatformBrowser(WalletController.this, HTTPServer.getPlatformUrl(HTTPServer.URL_SELL)));

        updateUi()

        // TODO: Migrate delisted token check and "more info" button to new framework
        //  (effect for checking if delisted, event for showing fragment etc.)
        // Not sure, if Generic Core has a notion of delisted token and whether it manages this
        more_info_button.setOnClickListener {
            val controller = WebController(BRConstants.FAQ_UNSUPPORTED_TOKEN)
            router.pushController(RouterTransaction.with(controller))
        }
    }

    override fun onAttach(view: View) {
        super.onAttach(view)

        // TODO: Remove?
        if (!TokenUtil.isTokenSupported(currentModel.currencyCode)) {
            showDelistedTokenBanner()
        }
    }

    override fun bindView(output: Consumer<WalletScreenEvent>): Disposable {
        // Tx Action buttons
        send_button.setHasShadow(false)
        send_button.setOnClickListener { output.accept(WalletScreenEvent.OnSendClicked) }
        receive_button.setHasShadow(false)
        receive_button.setOnClickListener { output.accept(WalletScreenEvent.OnReceiveClicked) }

        // Tx List
        tx_list.layoutManager = object : LinearLayoutManager(applicationContext) {
            override fun onLayoutCompleted(state: RecyclerView.State?) {
                super.onLayoutCompleted(state)
                val adapter = checkNotNull(mAdapter)
                updateVisibleTransactions(adapter, this, output)
            }
        }
        mAdapter = TransactionListAdapter(applicationContext!!, null) { (txHash) ->
            output.accept(WalletScreenEvent.OnTransactionClicked(txHash))
        }
        tx_list.adapter = mAdapter

        tx_list.addOnScrollListener(
            object : RecyclerView.OnScrollListener() {
                override fun onScrollStateChanged(recyclerView: RecyclerView, newState: Int) {
                    super.onScrollStateChanged(recyclerView, newState)

                    when (newState) {
                        RecyclerView.SCROLL_STATE_DRAGGING -> {
                            output.accept(WalletScreenEvent.OnVisibleTransactionsChanged(emptyList()))
                        }
                        RecyclerView.SCROLL_STATE_IDLE -> {
                            val adapter = checkNotNull(mAdapter)
                            val layoutManager =
                                (checkNotNull(recyclerView.layoutManager) as LinearLayoutManager)
                            updateVisibleTransactions(adapter, layoutManager, output)
                        }
                        else -> return
                    }
                }
            }
        )

        // Search button
        search_icon.setOnClickListener { output.accept(WalletScreenEvent.OnSearchClicked) }
        search_bar.setEventOutput(output)

        // Display currency buttons
        val displayCurrencyListener = View.OnClickListener {
            output.accept(WalletScreenEvent.OnChangeDisplayCurrencyClicked)
        }
        balance_primary.setOnClickListener(displayCurrencyListener)
        balance_secondary.setOnClickListener(displayCurrencyListener)

        // Back button
        back_icon.setOnClickListener { output.accept(WalletScreenEvent.OnBackClicked) }

        val intervalClickListener = View.OnClickListener { v ->
            output.accept(
                WalletScreenEvent.OnChartIntervalSelected(
                    when (v.id) {
                        one_day.id -> Interval.ONE_DAY
                        one_week.id -> Interval.ONE_WEEK
                        one_month.id -> Interval.ONE_MONTH
                        three_months.id -> Interval.THREE_MONTHS
                        one_year.id -> Interval.ONE_YEAR
                        three_years.id -> Interval.THREE_YEARS
                        else -> error("Unknown button pressed")
                    }
                )
            )
        }
        arrayOf(one_day, one_week, one_month, three_months, one_year, three_years)
            .forEach { it.setOnClickListener(intervalClickListener) }
        spark_line.setAdapter(mPriceDataAdapter)
        spark_line.sparkAnimator = LineSparkAnimator().apply {
            duration = MARKET_CHART_ANIMATION_DURATION
            interpolator = AccelerateInterpolator(MARKET_CHART_ANIMATION_ACCELERATION)
        }
        spark_line.scrubListener = object : SparkView.OnScrubListener {
            override fun onScrubbed(value: Any?) {
                val event = if (value == null) {
                    WalletScreenEvent.OnChartDataPointReleased
                } else {
                    val dataPoint = value as PriceDataPoint
                    logDebug("dataPoint: $dataPoint")
                    WalletScreenEvent.OnChartDataPointSelected(dataPoint)
                }
                output.accept(event)
            }
        }

        return Disposable {
            search_bar.setEventOutput(null)
            tx_list.clearOnScrollListeners()
        }
    }

    private fun updateVisibleTransactions(
        adapter: TransactionListAdapter,
        layoutManager: LinearLayoutManager,
        output: Consumer<WalletScreenEvent>
    ) {
        val firstIndex = layoutManager.findFirstVisibleItemPosition()
        val lastIndex = layoutManager.findLastVisibleItemPosition()
        if (firstIndex != RecyclerView.NO_POSITION) {
            output.accept(
                WalletScreenEvent.OnVisibleTransactionsChanged(
                    adapter.items
                        .slice(firstIndex..lastIndex)
                        .map { it.txHash }
                )
            )
        }
    }

    @Suppress("LongMethod", "ComplexMethod")
    override fun WalletScreenModel.render() {
        val adapter = checkNotNull(mAdapter)
        val resources = checkNotNull(resources)
        var adapterHasChanged = false

        ifChanged(WalletScreenModel::currencyName, currency_label::setText)

        ifChanged(WalletScreenModel::balance) {
            balance_secondary.text = it.formatCryptoForUi(currencyCode)
        }

        ifChanged(WalletScreenModel::fiatBalance) {
            // TODO: Move preferredFiatIso to model
            val preferredFiatIso = BRSharedPrefs.getPreferredFiatIso()
            balance_primary.text = CurrencyUtils.getFormattedFiatAmount(preferredFiatIso, it)
        }

        ifChanged(WalletScreenModel::isCryptoPreferred) {
            setPriceTags(it, true)

            adapter.setIsCryptoPreferred(isCryptoPreferred)
            adapterHasChanged = true
        }

        ifChanged(
            WalletScreenModel::isFilterApplied,
            WalletScreenModel::filteredTransactions,
            WalletScreenModel::transactions
        ) {
            if (isFilterApplied) {
                adapter.items = filteredTransactions
                adapterHasChanged = true
            } else {
                adapter.items = transactions
                adapterHasChanged = true
            }
        }

        if (adapterHasChanged) {
            adapter.notifyDataSetChanged()
        }

        // Update header area
        ifChanged(
            WalletScreenModel::hasInternet,
            WalletScreenModel::isShowingSearch
        ) {
            when {
                hasInternet && isShowingSearch -> {
                    notification_bar.isVisible = false
                    // TODO revisit this animation with conductor
                    search_bar.onShow(true)
                    val searchBarAnimation =
                        AnimatorInflater.loadAnimator(applicationContext, R.animator.from_top)
                    searchBarAnimation.setTarget(search_bar)
                    searchBarAnimation.start()
                    search_bar.render(this)
                }
                hasInternet && !isShowingSearch -> {
                    notification_bar.isVisible = false
                    val searchBarAnimation =
                        AnimatorInflater.loadAnimator(applicationContext, R.animator.to_top)
                    searchBarAnimation.setTarget(search_bar)
                    searchBarAnimation.start()
                    search_bar.onShow(false)
                }
                else -> {
                    notification_bar.isVisible = true
                }
            }
        }

        ifChanged(
            WalletScreenModel::filterComplete,
            WalletScreenModel::filterPending,
            WalletScreenModel::filterReceived,
            WalletScreenModel::filterSent
        ) {
            search_bar.render(this)
        }

        // Update sync progress
        ifChanged(
            WalletScreenModel::syncProgress,
            WalletScreenModel::isSyncing,
            WalletScreenModel::hasSyncTime
        ) {
            if (isSyncing) {
                progress_layout.isVisible = true
                progress_layout.alpha = 1f
                val syncingText = resources.getString(R.string.SyncingView_syncing)
                val syncingPercentText = NumberFormat.getPercentInstance().format(syncProgress)
                syncing_label.text = "%s %s".format(syncingText, syncingPercentText)

                if (hasSyncTime) {
                    val syncedThroughDate = syncingThroughMillis
                        .run(SimpleDateFormat(SYNCED_THROUGH_DATE_FORMAT)::format)
                    sync_status_label.text =
                        resources.getString(R.string.SyncingView_syncedThrough)
                            .format(syncedThroughDate)
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
                            progress_layout.alpha = 1f
                        }
                    })
            }
        }

        /* TODO: Make review prompt a controller
        // Show Review Prompt Dialog
        if (model.showReviewPrompt && !model.isShowingReviewPrompt) {
            if (!isFragmentOnTop) {
                // Show dialog
                BRDialog.showCustomDialog(
                    this@WalletController,
                    getString(R.string.RateAppPrompt_Title_Android),
                    getString(R.string.RateAppPrompt_Body_Android),
                    getString(R.string.RateAppPrompt_Button_RateApp_Android),
                    getString(R.string.RateAppPrompt_Button_Dismiss_Android),
                    { // positiveButton
                            brDialogView ->
                        output.accept(WalletScreenEvent.OnReviewPromptAccepted)
                        // NOTE: This causes us to over-count dismisses, as each positive button
                        // click also calls dismiss handler (known issue)
                        brDialogView.dismiss()
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
        }*/

        ifChanged(WalletScreenModel::priceChartDataPoints) {
            mPriceDataAdapter.dataSet = it
            mPriceDataAdapter.notifyDataSetChanged()
        }

        ifChanged(WalletScreenModel::priceChartInterval) {
            val deselectedColor = resources.getColor(R.color.trans_white)
            val selectedColor = resources.getColor(R.color.white)
            mIntervalButtons.forEachIndexed { index, baseTextView ->
                baseTextView.setTextColor(
                    when (priceChartInterval.ordinal) {
                        index -> selectedColor
                        else -> deselectedColor
                    }
                )
            }
        }

        ifChanged(
            WalletScreenModel::selectedPriceDataPoint,
            WalletScreenModel::fiatPricePerUnit,
            WalletScreenModel::priceChartInterval
        ) {
            if (selectedPriceDataPoint == null) {
                chart_label.text = priceChange?.toString().orEmpty()
                currency_usd_price.text = fiatPricePerUnit
            } else {
                currency_usd_price.text = selectedPriceDataPoint.closePrice
                    .toBigDecimal()
                    .formatFiatForUi(BRSharedPrefs.getPreferredFiatIso())

                val format = when (priceChartInterval) {
                    Interval.ONE_DAY, Interval.ONE_WEEK ->
                        MARKET_CHART_DATE_WITH_HOUR
                    else -> MARKET_CHART_DATE_WITH_YEAR
                }
                val dateFormat = SimpleDateFormat(format, Locale.getDefault())
                chart_label.text = dateFormat.format(selectedPriceDataPoint.time)
            }
        }
    }

    /**
     * This token is no longer supported by the BRD app, notify the user.
     */
    private fun showDelistedTokenBanner() {
        delisted_token_layout.visibility = View.VISIBLE
    }

    private fun updateUi() {
        val resources = checkNotNull(resources)
        val uiConfiguration = WalletDisplayUtils.getUIConfiguration(currencyCode)

        val startColor = uiConfiguration.startColor
        val endColor = uiConfiguration.endColor
        val currentTheme = UiUtils.getThemeId(activity)

        if (currentTheme == R.style.AppTheme_Dark) {
            send_button.setColor(resources.getColor(R.color.wallet_footer_button_color_dark))
            receive_button.setColor(resources.getColor(R.color.wallet_footer_button_color_dark))
            sell_button.setColor(resources.getColor(R.color.wallet_footer_button_color_dark))

            if (endColor != null) {
                bottom_toolbar_layout1.setBackgroundColor(Color.parseColor(endColor))
            }
        } else if (endColor != null) {
            send_button.setColor(Color.parseColor(endColor))
            receive_button.setColor(Color.parseColor(endColor))
            sell_button.setColor(Color.parseColor(endColor))
        }

        if (endColor != null) {
            //it's a gradient
            val gd = GradientDrawable(
                GradientDrawable.Orientation.LEFT_RIGHT,
                intArrayOf(Color.parseColor(startColor), Color.parseColor(endColor))
            )
            gd.cornerRadius = 0f
            bread_bar.background = gd
            appbar.background = gd
        } else {
            //it's a solid color
            bread_bar.setBackgroundColor(Color.parseColor(startColor))
            appbar.setBackgroundColor(Color.parseColor(startColor))
        }
    }

    @Suppress("LongMethod", "ComplexMethod") // TODO: This function should not exist
    private fun setPriceTags(cryptoPreferred: Boolean, animate: Boolean) {
        val resources = checkNotNull(resources)
        val set = ConstraintSet()
        set.clone(balance_row)
        if (animate) {
            TransitionManager.beginDelayedTransition(balance_row)
        }
        val balanceTextMargin = resources.getDimension(R.dimen.balance_text_margin).toInt()
        val primaryTextSize = TypedValue()
        resources.getValue(R.dimen.wallet_balance_primary_text_size, primaryTextSize, true)
        val secondaryTextSize = TypedValue()
        resources.getValue(R.dimen.wallet_balance_secondary_text_size, secondaryTextSize, true)
        // CRYPTO on RIGHT
        if (cryptoPreferred) {
            // Align crypto balance to the right parent
            set.connect(
                R.id.balance_secondary, ConstraintSet.END,
                ConstraintSet.PARENT_ID, ConstraintSet.END, balanceTextMargin
            )
            set.centerVertically(R.id.balance_secondary, ConstraintSet.PARENT_ID)

            // Align swap icon to left of crypto balance
            set.connect(
                R.id.swap,
                ConstraintSet.END,
                R.id.balance_secondary,
                ConstraintSet.START,
                balanceTextMargin
            )
            set.centerVertically(R.id.swap, ConstraintSet.PARENT_ID)

            // Align usd balance to left of swap icon
            set.connect(
                R.id.balance_primary,
                ConstraintSet.END,
                R.id.swap,
                ConstraintSet.START,
                balanceTextMargin
            )
            set.centerVertically(R.id.balance_primary, ConstraintSet.PARENT_ID)

            balance_secondary.textSize = primaryTextSize.float
            balance_primary.textSize = secondaryTextSize.float
        } else {
            // CRYPTO on LEFT
            // Align primary to right of parent
            set.connect(
                R.id.balance_primary, ConstraintSet.END, ConstraintSet.PARENT_ID,
                ConstraintSet.END, balanceTextMargin
            )
            set.centerVertically(R.id.balance_primary, ConstraintSet.PARENT_ID)

            // Align swap icon to left of usd balance
            set.connect(
                R.id.swap,
                ConstraintSet.END,
                R.id.balance_primary,
                ConstraintSet.START,
                balanceTextMargin
            )
            set.centerVertically(R.id.swap, ConstraintSet.PARENT_ID)

            // Align secondary currency to the left of swap icon
            set.connect(
                R.id.balance_secondary,
                ConstraintSet.END,
                R.id.swap,
                ConstraintSet.START,
                balanceTextMargin
            )
            set.centerVertically(R.id.balance_secondary, ConstraintSet.PARENT_ID)

            balance_secondary.textSize = secondaryTextSize.float
            balance_primary.textSize = primaryTextSize.float
        }
        set.applyTo(balance_row)
        balance_secondary.setTextColor(
            resources.getColor(
                if (cryptoPreferred)
                    R.color.white
                else
                    R.color.currency_subheading_color, null
            )
        )
        balance_primary.setTextColor(
            resources.getColor(
                if (cryptoPreferred)
                    R.color.currency_subheading_color
                else
                    R.color.white, null
            )
        )
    }
}
