/**
 * BreadWallet
 *
 * Created by Drew Carlson <drew.carlson@breadwallet.com> on 9/24/19.
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

import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.support.v7.widget.RecyclerView
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isGone
import androidx.core.view.isVisible
import com.breadwallet.R
import com.breadwallet.breadbox.formatCryptoForUi
import com.breadwallet.breadbox.formatFiatForUi
import com.breadwallet.ext.swap
import com.breadwallet.legacy.presenter.customviews.BaseTextView
import com.breadwallet.legacy.presenter.customviews.ShimmerLayout
import com.breadwallet.tools.animation.ItemTouchHelperAdapter
import com.breadwallet.tools.animation.ItemTouchHelperViewHolder
import com.breadwallet.tools.manager.BRSharedPrefs
import com.breadwallet.tools.util.TokenUtil
import com.breadwallet.tools.util.Utils
import com.breadwallet.util.WalletDisplayUtils
import com.breadwallet.util.isBrd
import com.squareup.picasso.Picasso
import kotlinx.android.extensions.LayoutContainer
import kotlinx.android.synthetic.main.wallet_list_item.*
import java.io.File
import java.math.BigDecimal
import java.text.NumberFormat
import java.util.ArrayList

class WalletListAdapter(
    private val onWalletClicked: (Wallet) -> Unit,
    private val onAddWalletClicked: () -> Unit,
    private val onWalletDisplayOrderUpdated: (List<String>) -> Unit
) : RecyclerView.Adapter<WalletListAdapter.WalletItemViewHolder>(),
    ItemTouchHelperAdapter {

    companion object {
        const val VIEW_TYPE_WALLET = 0
        const val VIEW_TYPE_ADD_WALLET = 1
    }

    private var walletList: List<Wallet> = ArrayList()
    private var displayOrderList: MutableList<String> = mutableListOf()

    /**
     * Sets the wallets that the adapter is responsible for rendering.
     *
     * @param wallets The wallets to render.
     */
    fun setWallets(wallets: List<Wallet>) {
        walletList =
            displayOrderList.mapNotNull { currencyId -> wallets.find { it.currencyId == currencyId } }
        notifyDataSetChanged()
    }

    fun setDisplayOrder(displayOrder: List<String>) {
        displayOrderList = displayOrder.toMutableList()
    }

    /**
     * Creates a view holder that will display a list item (e.g., a wallet or 'Add Wallets' button display).
     *
     * @param parent   The parent view group.
     * @param viewType The view type for the holder (indicates either wallet or 'Add Wallets')
     * @return The created wallet-specific view holder.
     */
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): WalletItemViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        return when (viewType) {
            VIEW_TYPE_WALLET ->
                DecoratedWalletItemViewHolder(
                    inflater.inflate(
                        R.layout.wallet_list_item,
                        parent,
                        false
                    )
                )
            VIEW_TYPE_ADD_WALLET ->
                WalletItemViewHolder(inflater.inflate(R.layout.add_wallets_item, parent, false))
            else -> throw IllegalArgumentException("Invalid ViewHolder type: $viewType")
        }
    }

    /**
     * Returns a list item's view type. In the case of the last item in the list, the view type
     * will correspond to the 'Add Wallets' type.
     *
     * @param position The index of list item in question.
     * @return The view type of given list item.
     */
    override fun getItemViewType(position: Int): Int {
        return if (position < walletList.size) {
            VIEW_TYPE_WALLET
        } else {
            VIEW_TYPE_ADD_WALLET
        }
    }

    /**
     * Returns the wallet at the given index.
     *
     * @param position The index of the wallet to return.
     * @return The wallet at the given index.
     */
    fun getItemAt(position: Int): Wallet? {
        return walletList[position]
    }

    /**
     * Binds the wallet data, specified by the given index, to the view holder, as well as rendering
     * any other display elements (icons, colours, etc.).
     *
     * @param holderView   The view holder to be bound and rendered.
     * @param position The index of the view holder (and wallet).
     */
    override fun onBindViewHolder(holderView: WalletItemViewHolder, position: Int) {
        if (getItemViewType(position) == VIEW_TYPE_WALLET && holderView is DecoratedWalletItemViewHolder) {
            holderView.renderWallet(checkNotNull(getItemAt(position)))
        } else {
            val context = holderView.itemView.context
            holderView.itemView.setOnClickListener { onAddWalletClicked() }
            val addWalletLabel = holderView.itemView.findViewById<BaseTextView>(R.id.add_wallets)
            addWalletLabel.text = "+ ${context.getString(R.string.TokenList_addTitle)}"
        }
    }

    /**
     * Returns the number of display items (*not* just wallets) in the list.
     *
     * @return The number of display items.
     */
    override fun getItemCount(): Int {
        // Number of wallets plus 1 for the 'Add Wallets' item.
        return walletList.size + 1
    }

    override fun onItemMove(fromPosition: Int, toPosition: Int) {
        // Prevent moving wallets below the add wallet button
        if (getItemViewType(toPosition) == VIEW_TYPE_ADD_WALLET) return
        displayOrderList.swap(fromPosition, toPosition)
        notifyItemMoved(fromPosition, toPosition)
    }

    override fun onItemDrop(fromPosition: Int, toPosition: Int) {
        onWalletDisplayOrderUpdated(displayOrderList)
    }

    // Swipe events are disabled
    override fun onItemDismiss(position: Int) = Unit

    /**
     * Generic [RecyclerView.ViewHolder] for item in the home screen wallet list.
     * Used for the "Add Wallets" item.
     */
    open class WalletItemViewHolder(view: View) : RecyclerView.ViewHolder(view)

    /**
     * [RecyclerView.ViewHolder] for each wallet in the home screen wallet list.
     */
    inner class DecoratedWalletItemViewHolder(
        override val containerView: View
    ) : WalletItemViewHolder(containerView),
        ItemTouchHelperViewHolder,
        LayoutContainer {

        override fun onItemSelected() = Unit
        override fun onItemClear() = Unit

        @Suppress("LongMethod", "ComplexMethod")
        fun renderWallet(wallet: Wallet) {
            val context = containerView.context
            val currencyCode = wallet.currencyCode
            containerView.setOnClickListener { onWalletClicked(wallet) }

            if (currencyCode.isBrd() && !BRSharedPrefs.getRewardsAnimationShown(context)) {
                (containerView as ShimmerLayout).startShimmerAnimation()
            } else {
                (containerView as ShimmerLayout).stopShimmerAnimation()
            }

            // Format numeric data
            val preferredFiatIso = BRSharedPrefs.getPreferredFiatIso(context)
            val exchangeRate = wallet.fiatPricePerUnit.formatFiatForUi(preferredFiatIso)
            val fiatBalance = wallet.fiatBalance.formatFiatForUi(preferredFiatIso)
            val cryptoBalance = wallet.balance.formatCryptoForUi(currencyCode)

            if (wallet.fiatPricePerUnit == BigDecimal.ZERO) {
                wallet_balance_fiat.visibility = View.INVISIBLE
                wallet_trade_price.visibility = View.INVISIBLE
            } else {
                wallet_balance_fiat.visibility = View.VISIBLE
                wallet_trade_price.visibility = View.VISIBLE
            }

            val isSyncing = wallet.isSyncing
            // Set wallet fields
            wallet_name.text = wallet.currencyName
            wallet_trade_price.text = exchangeRate
            wallet_balance_fiat.text = fiatBalance
            wallet_balance_fiat.setTextColor(
                context.getColor(
                    when {
                        isSyncing -> R.color.wallet_balance_fiat_syncing
                        else -> R.color.wallet_balance_fiat
                    }
                )
            )
            wallet_balance_currency.text = cryptoBalance
            wallet_balance_currency.isGone = isSyncing
            sync_progress.isVisible = isSyncing
            syncing_label.isVisible = isSyncing
            if (isSyncing) {
                val syncProgress = wallet.syncProgress
                var labelText = context.getString(R.string.SyncingView_syncing)
                if (syncProgress > 0) {
                    labelText += " ${NumberFormat.getPercentInstance().format(syncProgress.toDouble())}"
                }
                syncing_label.text = labelText
            }

            val priceChange = wallet.priceChange
            price_change.visibility = if (priceChange != null) View.VISIBLE else View.INVISIBLE
            if (priceChange != null) {
                price_change.text = priceChange.getPercentageChange()
            }

            // Get icon for currency
            val tokenIconPath =
                TokenUtil.getTokenIconPath(context, currencyCode.toUpperCase(), false)

            if (!Utils.isNullOrEmpty(tokenIconPath)) {
                val iconFile = File(tokenIconPath)
                Picasso.get().load(iconFile).into(currency_icon_white)
                icon_letter.visibility = View.GONE
                currency_icon_white.visibility = View.VISIBLE
            } else {
                // If no icon is present, then use the capital first letter of the token currency code instead.
                icon_letter.visibility = View.VISIBLE
                currency_icon_white.visibility = View.GONE
                icon_letter.text = currencyCode.substring(0, 1).toUpperCase()
            }

            val uiConfiguration = WalletDisplayUtils.getUIConfiguration(currencyCode, context)
            val startColor = uiConfiguration.startColor
            val endColor = uiConfiguration.endColor
            val drawable =
                context.resources.getDrawable(R.drawable.crypto_card_shape, null).mutate()

            val isTokenSupported = TokenUtil.isTokenSupported(currencyCode)
            if (isTokenSupported) {
                // Create gradient if 2 colors exist.
                (drawable as GradientDrawable).colors =
                    intArrayOf(Color.parseColor(startColor), Color.parseColor(endColor))
                drawable.orientation = GradientDrawable.Orientation.LEFT_RIGHT
                wallet_card.background = drawable
                setWalletItemColors(R.dimen.token_background_no_alpha)
            } else {
                // To ensure that the unsupported wallet card has the same shape as
                // the supported wallet card, we reuse the drawable.
                (drawable as GradientDrawable).colors = intArrayOf(
                    context.getColor(R.color.wallet_delisted_token_background),
                    context.getColor(R.color.wallet_delisted_token_background)
                )
                wallet_card.background = drawable
                setWalletItemColors(R.dimen.token_background_with_alpha)
            }
        }

        private fun setWalletItemColors(dimenRes: Int) {
            val typedValue = TypedValue()
            containerView.context.resources.getValue(dimenRes, typedValue, true)
            val alpha = typedValue.float
            currency_icon_white.alpha = alpha
            wallet_name.alpha = alpha
            wallet_trade_price.alpha = alpha
            wallet_balance_fiat.alpha = alpha
            wallet_balance_currency.alpha = alpha
        }
    }
}
