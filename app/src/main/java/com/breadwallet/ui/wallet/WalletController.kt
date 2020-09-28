/**
 * BreadWallet
 *
 * Created by Ahsan Butt <ahsan.butt@breadwallet.com> on 7/26/19.
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
package com.breadwallet.ui.wallet

import android.animation.AnimatorInflater
import android.animation.LayoutTransition
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.transition.TransitionManager
import android.util.TypedValue
import android.view.View
import android.view.ViewGroup
import android.view.animation.AccelerateInterpolator
import androidx.core.os.bundleOf
import androidx.core.view.children
import androidx.core.view.isVisible
import androidx.recyclerview.widget.DefaultItemAnimator
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bluelinelabs.conductor.RouterTransaction
import com.breadwallet.R
import com.breadwallet.breadbox.WalletState
import com.breadwallet.breadbox.formatCryptoForUi
import com.breadwallet.breadbox.formatFiatForUi
import com.breadwallet.effecthandler.metadata.MetaDataEffectHandler
import com.breadwallet.legacy.presenter.customviews.BaseTextView
import com.breadwallet.logger.logDebug
import com.breadwallet.model.PriceDataPoint
import com.breadwallet.tools.animation.UiUtils
import com.breadwallet.tools.manager.BRSharedPrefs
import com.breadwallet.tools.util.BRConstants
import com.breadwallet.tools.util.CurrencyUtils
import com.breadwallet.tools.util.TokenUtil
import com.breadwallet.ui.BaseMobiusController
import com.breadwallet.ui.controllers.AlertDialogController
import com.breadwallet.ui.flowbind.clicks
import com.breadwallet.ui.home.MAX_CRYPTO_DIGITS
import com.breadwallet.ui.navigation.NavigationTarget
import com.breadwallet.ui.navigation.asSupportUrl
import com.breadwallet.ui.wallet.WalletScreen.DIALOG_CREATE_ACCOUNT
import com.breadwallet.ui.wallet.WalletScreen.E
import com.breadwallet.ui.wallet.WalletScreen.F
import com.breadwallet.ui.wallet.WalletScreen.M
import com.breadwallet.ui.wallet.spark.SparkAdapter
import com.breadwallet.ui.wallet.spark.SparkView
import com.breadwallet.ui.wallet.spark.animation.LineSparkAnimator
import com.breadwallet.ui.web.WebController
import com.google.android.material.appbar.AppBarLayout
import com.mikepenz.fastadapter.FastAdapter
import com.mikepenz.fastadapter.GenericFastAdapter
import com.mikepenz.fastadapter.adapters.GenericModelAdapter
import com.mikepenz.fastadapter.adapters.ItemAdapter
import com.mikepenz.fastadapter.adapters.ModelAdapter
import com.spotify.mobius.Connectable
import kotlinx.android.synthetic.main.chart_view.*
import kotlinx.android.synthetic.main.controller_wallet.*
import kotlinx.android.synthetic.main.market_data_view.*
import kotlinx.android.synthetic.main.view_delisted_token.*
import kotlinx.android.synthetic.main.wallet_toolbar.*
import kotlinx.coroutines.channels.SendChannel
import kotlinx.coroutines.channels.actor
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.merge
import org.kodein.di.direct
import org.kodein.di.erased.instance
import java.text.SimpleDateFormat
import java.util.Locale
import kotlin.math.abs

private const val EXTRA_CURRENCY_CODE = "currency_code"
private const val MARKET_CHART_DATE_WITH_HOUR = "MMM d, h:mm"
private const val MARKET_CHART_DATE_WITH_YEAR = "MMM d, yyyy"
private const val MARKET_CHART_ANIMATION_DURATION = 500L
private const val MARKET_CHART_ANIMATION_ACCELERATION = 1.2f

/**
 * TODO: Remaining work: Make review prompt a controller.
 */
open class WalletController(args: Bundle) : BaseMobiusController<M, E, F>(args),
    AlertDialogController.Listener,
    AppBarLayout.OnOffsetChangedListener {

    constructor(currencyCode: String) : this(
        bundleOf(EXTRA_CURRENCY_CODE to currencyCode)
    )

    private val currencyCode = arg<String>(EXTRA_CURRENCY_CODE)

    override val layoutId = R.layout.controller_wallet

    override val defaultModel = M.createDefault(currencyCode)
    override val init = WalletInit
    override val update = WalletUpdate
    override val flowEffectHandler
        get() = WalletScreenHandler.createEffectHandler(
            checkNotNull(applicationContext),
            direct.instance(),
            Connectable { output ->
                MetaDataEffectHandler(output, direct.instance(), direct.instance())
            },
            direct.instance(),
            direct.instance()
        )

    private var fastAdapter: GenericFastAdapter? = null
    private var txAdapter: GenericModelAdapter<WalletTransaction>? = null
    private var syncAdapter: ItemAdapter<SyncingItem>? = null
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

        updateUi()
        setPriceTags(BRSharedPrefs.isCryptoPreferred(), false)

        more_info_button.setOnClickListener {
            val url = NavigationTarget.SupportPage(BRConstants.FAQ_UNSUPPORTED_TOKEN).asSupportUrl()
            router.pushController(
                RouterTransaction.with(
                    WebController(url)
                )
            )
        }

        txAdapter = ModelAdapter { TransactionListItem(it, currentModel.isCryptoPreferred) }
        syncAdapter = ItemAdapter()
        fastAdapter = FastAdapter.with(listOf(syncAdapter!!, txAdapter!!))
        checkNotNull(fastAdapter).onClickListener = { _, _, item, _ ->
            when (item) {
                is TransactionListItem ->
                    eventConsumer.accept(E.OnTransactionClicked(item.model.txHash))
            }
            true
        }
        tx_list.adapter = fastAdapter
        tx_list.itemAnimator = DefaultItemAnimator()
        tx_list.layoutManager = object : LinearLayoutManager(applicationContext) {
            override fun onLayoutCompleted(state: RecyclerView.State?) {
                super.onLayoutCompleted(state)
                val adapter = checkNotNull(txAdapter)
                updateVisibleTransactions(adapter, this, viewCreatedScope.actor {
                    for (event in channel) {
                        eventConsumer.accept(event)
                    }
                })
            }
        }

        spark_line.setAdapter(mPriceDataAdapter)
        spark_line.sparkAnimator = LineSparkAnimator().apply {
            duration = MARKET_CHART_ANIMATION_DURATION
            interpolator = AccelerateInterpolator(MARKET_CHART_ANIMATION_ACCELERATION)
        }

        appbar.addOnOffsetChangedListener(this)
    }

    override fun onDestroyView(view: View) {
        appbar.removeOnOffsetChangedListener(this)
        txAdapter = null
        fastAdapter = null
        mPriceDataAdapter = SparkAdapter()
        super.onDestroyView(view)
    }

    override fun bindView(modelFlow: Flow<M>): Flow<E> {
        // Tx Action buttons
        send_button.setHasShadow(false)
        receive_button.setHasShadow(false)

        return merge(
            send_button.clicks().map { E.OnSendClicked },
            receive_button.clicks().map { E.OnReceiveClicked },
            buttonCreateAccount.clicks().map { E.OnCreateAccountClicked },
            search_icon.clicks().map { E.OnSearchClicked },
            back_icon.clicks().map { E.OnBackClicked },
            bindTxList(),
            bindIntervalClicks(),
            bindSparkLineScrubbing(),
            merge(
                balance_primary.clicks(),
                balance_secondary.clicks()
            ).map { E.OnChangeDisplayCurrencyClicked },
            callbackFlow {
                search_bar.setEventOutput(channel)
                awaitClose { search_bar.setEventOutput(null) }
            }
        )
    }

    @Suppress("ComplexMethod")
    private fun bindIntervalClicks(): Flow<E> =
        callbackFlow {
            val intervalClickListener = View.OnClickListener { v ->
                E.OnChartIntervalSelected(
                    when (v.id) {
                        one_day.id -> Interval.ONE_DAY
                        one_week.id -> Interval.ONE_WEEK
                        one_month.id -> Interval.ONE_MONTH
                        three_months.id -> Interval.THREE_MONTHS
                        one_year.id -> Interval.ONE_YEAR
                        three_years.id -> Interval.THREE_YEARS
                        else -> error("Unknown button pressed")
                    }
                ).run(::offer)
            }
            val inputs = arrayOf(one_day, one_week, one_month, three_months, one_year, three_years)
            inputs.forEach { it.setOnClickListener(intervalClickListener) }
            awaitClose {
                inputs.forEach { it.setOnClickListener(null) }
            }
        }

    private fun bindSparkLineScrubbing(): Flow<E> =
        callbackFlow {
            spark_line.scrubListener = object : SparkView.OnScrubListener {
                override fun onScrubbed(value: Any?) {
                    if (value == null) {
                        offer(E.OnChartDataPointReleased)
                    } else {
                        val dataPoint = value as PriceDataPoint
                        logDebug("dataPoint: $dataPoint")
                        offer(E.OnChartDataPointSelected(dataPoint))
                    }
                }
            }
            awaitClose {
                spark_line.scrubListener = null
            }
        }

    private fun bindTxList(): Flow<E> =
        callbackFlow {
            // Tx List
            tx_list.addOnScrollListener(
                object : RecyclerView.OnScrollListener() {
                    override fun onScrollStateChanged(recyclerView: RecyclerView, newState: Int) {
                        super.onScrollStateChanged(recyclerView, newState)

                        when (newState) {
                            RecyclerView.SCROLL_STATE_DRAGGING -> {
                                offer(E.OnVisibleTransactionsChanged(emptyList()))
                            }
                            RecyclerView.SCROLL_STATE_IDLE -> {
                                val adapter = checkNotNull(txAdapter)
                                val layoutManager =
                                    (checkNotNull(recyclerView.layoutManager) as LinearLayoutManager)
                                updateVisibleTransactions(adapter, layoutManager, channel)
                            }
                            else -> return
                        }
                    }
                }
            )

            awaitClose {
                tx_list.clearOnScrollListeners()
            }
        }

    private fun updateVisibleTransactions(
        adapter: GenericModelAdapter<WalletTransaction>,
        layoutManager: LinearLayoutManager,
        output: SendChannel<E>
    ) {
        val syncItemCount = syncAdapter!!.adapterItemCount
        val firstIndex = layoutManager.findFirstVisibleItemPosition() - syncItemCount
        val lastIndex = layoutManager.findLastVisibleItemPosition() - syncItemCount
        if (firstIndex != RecyclerView.NO_POSITION) {
            output.offer(
                E.OnVisibleTransactionsChanged(
                    adapter.models
                        .slice(firstIndex..lastIndex)
                        .map { it.txHash }
                )
            )
        }
    }

    @Suppress("LongMethod", "ComplexMethod")
    override fun M.render() {
        val adapter = checkNotNull(txAdapter)
        val resources = checkNotNull(resources)

        ifChanged(M::currencyName, currency_label::setText)

        ifChanged(M::balance) {
            balance_secondary.text = it.formatCryptoForUi(currencyCode, scale = MAX_CRYPTO_DIGITS)
        }

        ifChanged(M::fiatBalance) {
            // TODO: Move preferredFiatIso to model
            val preferredFiatIso = BRSharedPrefs.getPreferredFiatIso()
            balance_primary.text = CurrencyUtils.getFormattedFiatAmount(preferredFiatIso, it)
        }

        ifChanged(M::isCryptoPreferred) {
            setPriceTags(it, true)

            adapter.itemList.items
                .filterIsInstance<TransactionListItem>()
                .forEach { item ->
                    item.isCryptoPreferred = isCryptoPreferred
                }
            checkNotNull(fastAdapter).notifyAdapterDataSetChanged()
        }

        ifChanged(
            M::isFilterApplied,
            M::filteredTransactions,
            M::transactions
        ) {
            if (isFilterApplied) {
                adapter.setNewList(filteredTransactions)
            } else {
                adapter.setNewList(transactions)
            }
        }

        // Update header area
        ifChanged(
            M::hasInternet,
            M::isShowingSearch
        ) {
            when {
                hasInternet && isShowingSearch -> {
                    notification_bar.isVisible = false
                    // TODO revisit this animation with conductor
                    search_bar.onShow(true)
                    if (search_bar.y != 0f) {
                        val searchBarAnimation =
                            AnimatorInflater.loadAnimator(applicationContext, R.animator.from_top)
                        searchBarAnimation.setTarget(search_bar)
                        searchBarAnimation.start()
                    }
                    search_bar.render(this)
                }
                hasInternet && !isShowingSearch -> {
                    notification_bar.isVisible = false
                    if (search_bar.y == 0f) {
                        val searchBarAnimation =
                            AnimatorInflater.loadAnimator(applicationContext, R.animator.to_top)
                        searchBarAnimation.setTarget(search_bar)
                        searchBarAnimation.start()
                    }
                    search_bar.onShow(false)
                }
                else -> {
                    notification_bar.isVisible = true
                }
            }
        }

        ifChanged(
            M::filterComplete,
            M::filterPending,
            M::filterReceived,
            M::filterSent
        ) {
            search_bar.render(this)
        }

        // Update sync progress
        ifChanged(
            M::syncProgress,
            M::isSyncing,
            M::syncingThroughMillis
        ) {
            val syncAdapter = syncAdapter!!
            if (isSyncing) {
                val item = syncAdapter.adapterItems.firstOrNull() ?: SyncingItem()
                item.syncProgress = syncProgress
                item.syncThroughMillis = syncingThroughMillis

                if (syncAdapter.adapterItemCount == 0) {
                    syncAdapter.setNewList(listOf(item))
                } else {
                    fastAdapter?.notifyAdapterDataSetChanged()
                }
            } else {
                syncAdapter.setNewList(emptyList())
            }
        }
        ifChanged(M::state) {
            when (state) {
                WalletState.Initialized -> {
                    layout_send_receive.isVisible = true
                    layoutCreateAccount.isVisible = false
                }
                WalletState.Error, WalletState.Loading -> {
                    layout_send_receive.isVisible = false
                    layoutCreateAccount.isVisible = true
                    buttonCreateAccount.isVisible = false
                    progressCreateAccount.isVisible = true
                }
                WalletState.WaitingOnAction -> {
                    layout_send_receive.isVisible = false
                    layoutCreateAccount.isVisible = true
                    buttonCreateAccount.isVisible = true
                    progressCreateAccount.isVisible = false
                }
            }
        }

        ifChanged(M::priceChartDataPoints) {
            mPriceDataAdapter.dataSet = it
            mPriceDataAdapter.notifyDataSetChanged()
        }

        ifChanged(M::priceChartIsLoading) {
            if (!it && priceChartDataPoints.isEmpty()) toolbar_layout.isVisible = false
        }

        ifChanged(M::priceChartInterval) {
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
            M::selectedPriceDataPoint,
            M::fiatPricePerUnit,
            M::priceChartInterval
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
                val dateFormat = SimpleDateFormat(format, Locale.ROOT)
                chart_label.text = dateFormat.format(selectedPriceDataPoint.time)
            }
        }

        ifChanged(M::isShowingDelistedBanner) {
            if (isShowingDelistedBanner) showDelistedTokenBanner()
        }

        ifChanged(M::marketDataState) {
            market_data.isVisible = marketDataState != MarketDataState.ERROR
            market_data.layoutParams.height = if (marketDataState == MarketDataState.LOADED) {
                ViewGroup.LayoutParams.WRAP_CONTENT
            } else {
                0
            }
        }

        ifChanged(
            M::marketCap,
            M::totalVolume,
            M::high24h,
            M::low24h
        ) {
            val preferredFiat = BRSharedPrefs.getPreferredFiatIso().toUpperCase(Locale.ROOT)

            market_cap.text = marketCap?.formatFiatForUi(preferredFiat, 0) ?: ""
            total_volume.text = totalVolume?.formatFiatForUi(preferredFiat, 0) ?: ""
            twenty_four_high.text = high24h?.formatFiatForUi(preferredFiat) ?: ""
            twenty_four_low.text = low24h?.formatFiatForUi(preferredFiat) ?: ""
        }
    }

    override fun onPositiveClicked(
        dialogId: String,
        controller: AlertDialogController,
        result: AlertDialogController.DialogInputResult
    ) {
        if (dialogId == DIALOG_CREATE_ACCOUNT) {
            eventConsumer.accept(E.OnCreateAccountConfirmationClicked)
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
        val token = TokenUtil.tokenForCode(currencyCode) ?: return

        val startColor = token.startColor
        val endColor = token.endColor
        val currentTheme = UiUtils.getThemeId(activity)

        if (currentTheme == R.style.AppTheme_Dark) {
            val buttonColor = resources.getColor(R.color.wallet_footer_button_color_dark)
            send_button.setColor(buttonColor)
            receive_button.setColor(buttonColor)
            buttonCreateAccount.setColor(buttonColor)
            progressCreateAccount.indeterminateDrawable.setTint(buttonColor)

            if (endColor != null) {
                val color = Color.parseColor(endColor)
                layoutCreateAccount.setBackgroundColor(color)
                layout_send_receive.setBackgroundColor(color)
            }
        } else if (endColor != null) {
            Color.parseColor(endColor).let {
                send_button.setColor(Color.parseColor(endColor))
                receive_button.setColor(Color.parseColor(endColor))
                buttonCreateAccount.setColor(Color.parseColor(endColor))
            }
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
        if (animate) {
            TransitionManager.beginDelayedTransition(balance_values)
        }

        val primaryTextSize = resources.getDimension(R.dimen.wallet_balance_primary_text_size)
        val secondaryTextSize = resources.getDimension(R.dimen.wallet_balance_secondary_text_size)

        // Reverse balance value labels
        fun reverseLabels() {
            val newChildren = balance_values.children.toList().reversed()
            balance_values.removeAllViews()
            newChildren.forEach { view ->
                balance_values.addView(view)
                balance_values.recomputeViewAttributes(view)
            }
        }

        if (cryptoPreferred) {
            if (balance_values.children.last().id == R.id.balance_primary) {
                reverseLabels()
            }
            // CRYPTO on RIGHT
            balance_secondary.setTextSize(TypedValue.COMPLEX_UNIT_PX, primaryTextSize)
            balance_primary.setTextSize(TypedValue.COMPLEX_UNIT_PX, secondaryTextSize)
        } else {
            if (balance_values.children.last().id == R.id.balance_secondary) {
                reverseLabels()
            }
            // CRYPTO on LEFT
            balance_secondary.setTextSize(TypedValue.COMPLEX_UNIT_PX, secondaryTextSize)
            balance_primary.setTextSize(TypedValue.COMPLEX_UNIT_PX, primaryTextSize)
        }

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

    override fun onOffsetChanged(appBarLayout: AppBarLayout, verticalOffset: Int) {
        // Disable layout transitions while collapsed or collapsing
        when {
            abs(verticalOffset) == appBarLayout.totalScrollRange -> {
                appBarLayout.layoutTransition.disableTransitionType(LayoutTransition.CHANGING)
                market_info.layoutTransition.disableTransitionType(LayoutTransition.CHANGING)
                sparkview_container.layoutTransition.disableTransitionType(LayoutTransition.CHANGING)
            }
            verticalOffset == 0 -> {
                appBarLayout.layoutTransition.enableTransitionType(LayoutTransition.CHANGING)
                market_info.layoutTransition.enableTransitionType(LayoutTransition.CHANGING)
                sparkview_container.layoutTransition.enableTransitionType(LayoutTransition.CHANGING)
            }
            else -> {
                appBarLayout.layoutTransition.disableTransitionType(LayoutTransition.CHANGING)
                market_info.layoutTransition.disableTransitionType(LayoutTransition.CHANGING)
                sparkview_container.layoutTransition.disableTransitionType(LayoutTransition.CHANGING)
                TransitionManager.endTransitions(market_info)
                TransitionManager.endTransitions(sparkview_container)
                TransitionManager.endTransitions(balance_values)
            }
        }
    }
}
