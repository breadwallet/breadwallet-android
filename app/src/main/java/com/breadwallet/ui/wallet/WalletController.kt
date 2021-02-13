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
import android.widget.TextView
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
import com.breadwallet.databinding.ControllerWalletBinding
import com.breadwallet.effecthandler.metadata.MetaDataEffectHandler
import com.breadwallet.ui.formatFiatForUi
import com.breadwallet.logger.logDebug
import com.breadwallet.model.PriceDataPoint
import com.breadwallet.tools.animation.UiUtils
import com.breadwallet.tools.manager.BRSharedPrefs
import com.breadwallet.tools.util.BRConstants
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
import com.breadwallet.util.isTezos
import com.google.android.material.appbar.AppBarLayout
import com.mikepenz.fastadapter.FastAdapter
import com.mikepenz.fastadapter.GenericFastAdapter
import com.mikepenz.fastadapter.adapters.GenericModelAdapter
import com.mikepenz.fastadapter.adapters.ItemAdapter
import com.mikepenz.fastadapter.adapters.ModelAdapter
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

    override val defaultModel = M.createDefault(currencyCode)
    override val init = WalletInit
    override val update = WalletUpdate
    override val flowEffectHandler
        get() = WalletScreenHandler.createEffectHandler(
            checkNotNull(applicationContext),
            direct.instance(),
            { output ->
                MetaDataEffectHandler(output, direct.instance(), direct.instance())
            },
            direct.instance(),
            direct.instance()
        )

    protected val binding by viewBinding(ControllerWalletBinding::inflate)

    private var fastAdapter: GenericFastAdapter? = null
    private var txAdapter: GenericModelAdapter<WalletTransaction>? = null
    private var syncAdapter: ItemAdapter<SyncingItem>? = null
    private var stakingAdapter: ItemAdapter<StakingItem>? = null
    private var mPriceDataAdapter = SparkAdapter()
    private val mIntervalButtons: List<TextView>
        get() = with(binding.chartContainer) {
            listOf<TextView>(
                oneDay,
                oneWeek,
                oneMonth,
                threeMonths,
                oneYear,
                threeYears
            )
        }

    override fun onCreateView(view: View) {
        super.onCreateView(view)
        with(binding) {
            updateUi()
            setPriceTags(BRSharedPrefs.isCryptoPreferred(), false)
            delistedTokenLayout.moreInfoButton.setOnClickListener {
                val url = NavigationTarget.SupportPage(BRConstants.FAQ_UNSUPPORTED_TOKEN).asSupportUrl()
                router.pushController(
                    RouterTransaction.with(
                        WebController(url)
                    )
                )
            }

            txAdapter = ModelAdapter { TransactionListItem(it, currentModel.isCryptoPreferred) }
            syncAdapter = ItemAdapter()
            stakingAdapter = ItemAdapter()
            fastAdapter = FastAdapter.with(listOf(syncAdapter!!, stakingAdapter!!, txAdapter!!))
            checkNotNull(fastAdapter).onClickListener = { _, _, item, _ ->
                when (item) {
                    is TransactionListItem ->
                        eventConsumer.accept(E.OnTransactionClicked(item.model.txHash))
                    is StakingItem -> eventConsumer.accept(E.OnStakingCellClicked)
                }
                true
            }

            // TODO: When we get another staking currency, should re-assess how to perform this check
            if (currencyCode.isTezos()) {
                stakingAdapter!!.setNewList(listOf(StakingItem(currencyCode)))
            }

            txList.adapter = fastAdapter
            txList.itemAnimator = DefaultItemAnimator()
            txList.layoutManager = object : LinearLayoutManager(applicationContext) {
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

            chartContainer.sparkLine.setAdapter(mPriceDataAdapter)
            chartContainer.sparkLine.sparkAnimator = LineSparkAnimator().apply {
                duration = MARKET_CHART_ANIMATION_DURATION
                interpolator = AccelerateInterpolator(MARKET_CHART_ANIMATION_ACCELERATION)
            }

            appbar.addOnOffsetChangedListener(this@WalletController)
        }
    }

    override fun onDestroyView(view: View) {
        binding.appbar.removeOnOffsetChangedListener(this)
        txAdapter = null
        fastAdapter = null
        mPriceDataAdapter = SparkAdapter()
        super.onDestroyView(view)
    }

    override fun bindView(modelFlow: Flow<M>): Flow<E> {
        // Tx Action buttons
        return with(binding) {
            sendButton.setHasShadow(false)
            receiveButton.setHasShadow(false)

            merge(
                sendButton.clicks().map { E.OnSendClicked },
                receiveButton.clicks().map { E.OnReceiveClicked },
                buttonCreateAccount.clicks().map { E.OnCreateAccountClicked },
                breadBar.searchIcon.clicks().map { E.OnSearchClicked },
                breadBar.backIcon.clicks().map { E.OnBackClicked },
                bindTxList(),
                bindIntervalClicks(),
                bindSparkLineScrubbing(),
                merge(
                    balancePrimary.clicks(),
                    balanceSecondary.clicks()
                ).map { E.OnChangeDisplayCurrencyClicked },
                callbackFlow {
                    searchBar.setEventOutput(channel)
                    awaitClose { searchBar.setEventOutput(null) }
                }
            )
        }
    }

    @Suppress("ComplexMethod")
    private fun bindIntervalClicks(): Flow<E> =
        callbackFlow {
            val intervalClickListener = View.OnClickListener { v ->
                with(binding.chartContainer) {
                    E.OnChartIntervalSelected(
                        when (v.id) {
                            oneDay.id -> Interval.ONE_DAY
                            oneWeek.id -> Interval.ONE_WEEK
                            oneMonth.id -> Interval.ONE_MONTH
                            threeMonths.id -> Interval.THREE_MONTHS
                            oneYear.id -> Interval.ONE_YEAR
                            threeYears.id -> Interval.THREE_YEARS
                            else -> error("Unknown button pressed")
                        }
                    ).run(::offer)
                }
            }
            mIntervalButtons.forEach { it.setOnClickListener(intervalClickListener) }
            awaitClose {
                mIntervalButtons.forEach { it.setOnClickListener(null) }
            }
        }

    private fun bindSparkLineScrubbing(): Flow<E> =
        callbackFlow {
            val channel = channel
            binding.chartContainer.sparkLine.scrubListener = object : SparkView.OnScrubListener {
                override fun onScrubbed(value: Any?) {
                    if (value == null) {
                        channel.offer(E.OnChartDataPointReleased)
                    } else {
                        val dataPoint = value as PriceDataPoint
                        logDebug("dataPoint: $dataPoint")
                        channel.offer(E.OnChartDataPointSelected(dataPoint))
                    }
                }
            }
            awaitClose {
                binding.chartContainer.sparkLine.scrubListener = null
            }
        }

    private fun bindTxList(): Flow<E> =
        callbackFlow {
            // Tx List
            val channel = channel
            binding.txList.addOnScrollListener(
                object : RecyclerView.OnScrollListener() {
                    override fun onScrollStateChanged(recyclerView: RecyclerView, newState: Int) {
                        super.onScrollStateChanged(recyclerView, newState)

                        when (newState) {
                            RecyclerView.SCROLL_STATE_DRAGGING -> {
                                channel.offer(E.OnVisibleTransactionsChanged(emptyList()))
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
                binding.txList.clearOnScrollListeners()
            }
        }

    private fun updateVisibleTransactions(
        adapter: GenericModelAdapter<WalletTransaction>,
        layoutManager: LinearLayoutManager,
        output: SendChannel<E>
    ) {
        val syncItemCount = syncAdapter!!.adapterItemCount
        val stakingItemCount = stakingAdapter!!.adapterItemCount
        val firstIndex = layoutManager.findFirstVisibleItemPosition() - syncItemCount - stakingItemCount
        val lastIndex = layoutManager.findLastVisibleItemPosition() - syncItemCount - stakingItemCount
        if (firstIndex > RecyclerView.NO_POSITION) {
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

        with(binding) {

            ifChanged(M::currencyName, breadBar.currencyLabel::setText)

            ifChanged(M::balance) {
                balanceSecondary.text = it.formatCryptoForUi(currencyCode, scale = MAX_CRYPTO_DIGITS)
            }

            ifChanged(M::fiatBalance) {
                // TODO: Move preferredFiatIso to model
                val preferredFiatIso = BRSharedPrefs.getPreferredFiatIso()
                balancePrimary.text = it.formatFiatForUi(preferredFiatIso)
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
                        notificationBar.isVisible = false
                        // TODO revisit this animation with conductor
                        searchBar.onShow(true)
                        if (searchBar.y != 0f) {
                            val searchBarAnimation =
                                AnimatorInflater.loadAnimator(applicationContext, R.animator.from_top)
                            searchBarAnimation.setTarget(searchBar)
                            searchBarAnimation.start()
                        }
                        searchBar.render(this@render)
                    }
                    hasInternet && !isShowingSearch -> {
                        notificationBar.isVisible = false
                        if (searchBar.y == 0f) {
                            val searchBarAnimation =
                                AnimatorInflater.loadAnimator(applicationContext, R.animator.to_top)
                            searchBarAnimation.setTarget(searchBar)
                            searchBarAnimation.start()
                        }
                        searchBar.onShow(false)
                    }
                    else -> {
                        notificationBar.isVisible = true
                    }
                }
            }

            ifChanged(
                M::filterComplete,
                M::filterPending,
                M::filterReceived,
                M::filterSent
            ) {
                searchBar.render(this@render)
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
                        layoutSendReceive.isVisible = true
                        layoutCreateAccount.isVisible = false
                    }
                    WalletState.Error, WalletState.Loading -> {
                        layoutSendReceive.isVisible = false
                        layoutCreateAccount.isVisible = true
                        buttonCreateAccount.isVisible = false
                        progressCreateAccount.isVisible = true
                    }
                    WalletState.WaitingOnAction -> {
                        layoutSendReceive.isVisible = false
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
                if (!it && priceChartDataPoints.isEmpty()) toolbarLayout.isVisible = false
            }

            ifChanged(M::priceChartInterval) {
                val deselectedColor = resources.getColor(R.color.trans_white)
                val selectedColor = resources.getColor(R.color.white)
                mIntervalButtons.forEachIndexed { index, TextView ->
                    TextView.setTextColor(
                        when (priceChartInterval.ordinal) {
                            index -> selectedColor
                            else -> deselectedColor
                        }
                    )
                }
            }

            with(chartContainer) {
                ifChanged(
                    M::selectedPriceDataPoint,
                    M::fiatPricePerUnit,
                    M::priceChartInterval
                ) {
                    if (selectedPriceDataPoint == null) {
                        chartLabel.text = priceChange?.toString().orEmpty()
                        currencyUsdPrice.text = fiatPricePerUnit
                    } else {
                        currencyUsdPrice.text = selectedPriceDataPoint.closePrice
                            .toBigDecimal()
                            .formatFiatForUi(BRSharedPrefs.getPreferredFiatIso())

                        val format = when (priceChartInterval) {
                            Interval.ONE_DAY, Interval.ONE_WEEK ->
                                MARKET_CHART_DATE_WITH_HOUR
                            else -> MARKET_CHART_DATE_WITH_YEAR
                        }
                        val dateFormat = SimpleDateFormat(format, Locale.ROOT)
                        chartLabel.text = dateFormat.format(selectedPriceDataPoint.time)
                    }
                }
            }

            ifChanged(M::isShowingDelistedBanner) {
                if (isShowingDelistedBanner) showDelistedTokenBanner()
            }

            with(marketData) {
                ifChanged(M::marketDataState) {
                    marketDataCard.isVisible = marketDataState != MarketDataState.ERROR
                    marketDataCard.layoutParams.height = if (marketDataState == MarketDataState.LOADED) {
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
                    marketCap.text = this@render.marketCap?.formatFiatForUi(preferredFiat, 0) ?: ""
                    totalVolume.text = this@render.totalVolume?.formatFiatForUi(preferredFiat, 0) ?: ""
                    twentyFourHigh.text = high24h?.formatFiatForUi(preferredFiat) ?: ""
                    twentyFourLow.text = low24h?.formatFiatForUi(preferredFiat) ?: ""
                }
            }
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
        binding.delistedTokenLayout.root.visibility = View.VISIBLE
    }

    private fun updateUi() {
        val resources = checkNotNull(resources)
        val token = TokenUtil.tokenForCode(currencyCode) ?: return

        val startColor = token.startColor
        val endColor = token.endColor
        val currentTheme = UiUtils.getThemeId(activity)

        with(binding) {
            if (currentTheme == R.style.AppTheme_Dark) {
                val buttonColor = resources.getColor(R.color.wallet_footer_button_color_dark)
                sendButton.setColor(buttonColor)
                receiveButton.setColor(buttonColor)
                buttonCreateAccount.setColor(buttonColor)
                progressCreateAccount.indeterminateDrawable.setTint(buttonColor)

                if (endColor != null) {
                    val color = Color.parseColor(endColor)
                    layoutCreateAccount.setBackgroundColor(color)
                    layoutSendReceive.setBackgroundColor(color)
                }
            } else if (endColor != null) {
                Color.parseColor(endColor).let {
                    sendButton.setColor(it)
                    receiveButton.setColor(it)
                    buttonCreateAccount.setColor(it)
                }
            }

            if (endColor != null) {
                //it's a gradient
                val gd = GradientDrawable(
                    GradientDrawable.Orientation.LEFT_RIGHT,
                    intArrayOf(Color.parseColor(startColor), Color.parseColor(endColor))
                )
                gd.cornerRadius = 0f
                mainContainer.background = gd
                appbar.background = gd
            } else {
                //it's a solid color
                mainContainer.setBackgroundColor(Color.parseColor(startColor))
                appbar.setBackgroundColor(Color.parseColor(startColor))
            }
        }
    }

    @Suppress("LongMethod", "ComplexMethod") // TODO: This function should not exist
    private fun setPriceTags(cryptoPreferred: Boolean, animate: Boolean) {
        val resources = checkNotNull(resources)
        with(binding) {
            if (animate) {
                TransitionManager.beginDelayedTransition(balanceValues)
            }

            val primaryTextSize = resources.getDimension(R.dimen.wallet_balance_primary_text_size)
            val secondaryTextSize = resources.getDimension(R.dimen.wallet_balance_secondary_text_size)

            // Reverse balance value labels
            fun reverseLabels() {
                val newChildren = balanceValues.children.toList().reversed()
                balanceValues.removeAllViews()
                newChildren.forEach { view ->
                    balanceValues.addView(view)
                    balanceValues.recomputeViewAttributes(view)
                }
            }

            if (cryptoPreferred) {
                if (balanceValues.children.last().id == R.id.balance_primary) {
                    reverseLabels()
                }
                // CRYPTO on RIGHT
                balanceSecondary.setTextSize(TypedValue.COMPLEX_UNIT_PX, primaryTextSize)
                balancePrimary.setTextSize(TypedValue.COMPLEX_UNIT_PX, secondaryTextSize)
            } else {
                if (balanceValues.children.last().id == R.id.balance_secondary) {
                    reverseLabels()
                }
                // CRYPTO on LEFT
                balanceSecondary.setTextSize(TypedValue.COMPLEX_UNIT_PX, secondaryTextSize)
                balancePrimary.setTextSize(TypedValue.COMPLEX_UNIT_PX, primaryTextSize)
            }

            balanceSecondary.setTextColor(
                resources.getColor(
                    if (cryptoPreferred)
                        R.color.white
                    else
                        R.color.currency_subheading_color, null
                )
            )
            balancePrimary.setTextColor(
                resources.getColor(
                    if (cryptoPreferred)
                        R.color.currency_subheading_color
                    else
                        R.color.white, null
                )
            )
        }
    }

    override fun onOffsetChanged(appBarLayout: AppBarLayout, verticalOffset: Int) {
        // Disable layout transitions while collapsed or collapsing
        with(binding) {
            when {
                abs(verticalOffset) == appBarLayout.totalScrollRange -> {
                    appBarLayout.layoutTransition.disableTransitionType(LayoutTransition.CHANGING)
                    marketInfo.layoutTransition.disableTransitionType(LayoutTransition.CHANGING)
                    sparkviewContainer.layoutTransition.disableTransitionType(LayoutTransition.CHANGING)
                }
                verticalOffset == 0 -> {
                    appBarLayout.layoutTransition.enableTransitionType(LayoutTransition.CHANGING)
                    marketInfo.layoutTransition.enableTransitionType(LayoutTransition.CHANGING)
                    sparkviewContainer.layoutTransition.enableTransitionType(LayoutTransition.CHANGING)
                }
                else -> {
                    appBarLayout.layoutTransition.disableTransitionType(LayoutTransition.CHANGING)
                    marketInfo.layoutTransition.disableTransitionType(LayoutTransition.CHANGING)
                    sparkviewContainer.layoutTransition.disableTransitionType(LayoutTransition.CHANGING)
                    TransitionManager.endTransitions(marketInfo)
                    TransitionManager.endTransitions(sparkviewContainer)
                    TransitionManager.endTransitions(balanceValues)
                }
            }
        }
    }
}
