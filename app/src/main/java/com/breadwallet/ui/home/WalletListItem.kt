package com.breadwallet.ui.home

import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.util.TypedValue
import android.view.View
import androidx.core.view.isGone
import androidx.core.view.isVisible
import com.breadwallet.R
import com.breadwallet.breadbox.formatCryptoForUi
import com.breadwallet.breadbox.formatFiatForUi
import com.breadwallet.legacy.presenter.customviews.ShimmerLayout
import com.breadwallet.tools.manager.BRSharedPrefs
import com.breadwallet.tools.util.TokenUtil
import com.breadwallet.tools.util.Utils
import com.breadwallet.util.WalletDisplayUtils
import com.breadwallet.util.isBrd
import com.mikepenz.fastadapter.FastAdapter
import com.mikepenz.fastadapter.drag.IDraggable
import com.mikepenz.fastadapter.items.ModelAbstractItem
import com.squareup.picasso.Picasso
import kotlinx.android.extensions.LayoutContainer
import kotlinx.android.synthetic.main.wallet_list_item.*
import java.io.File
import java.math.BigDecimal
import java.text.NumberFormat

class WalletListItem(
    wallet: Wallet
) : ModelAbstractItem<Wallet, WalletListItem.ViewHolder>(wallet), IDraggable {

    override val type: Int = R.id.wallet_list_item
    override val layoutRes: Int = R.layout.wallet_list_item
    override var identifier: Long = wallet.currencyId.hashCode().toLong()
    override val isDraggable: Boolean = true

    override fun getViewHolder(v: View) = ViewHolder(v)

    class ViewHolder(
        override val containerView: View
    ) : FastAdapter.ViewHolder<WalletListItem>(containerView),
        LayoutContainer {

        override fun bindView(item: WalletListItem, payloads: MutableList<Any>) {
            val wallet = item.model
            val context = containerView.context
            val currencyCode = wallet.currencyCode

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

            if (itemView.tag == wallet.currencyCode) {
                return
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

            item.tag = wallet.currencyCode
        }

        override fun unbindView(item: WalletListItem) {
            item.tag = null
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
