package com.breadwallet.ui.home

import android.content.Context
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.util.TypedValue
import android.view.View
import androidx.core.content.ContextCompat
import androidx.core.view.isGone
import androidx.core.view.isVisible
import com.breadwallet.R
import com.breadwallet.breadbox.formatCryptoForUi
import com.breadwallet.databinding.WalletListItemBinding
import com.breadwallet.legacy.presenter.customviews.ShimmerLayout
import com.breadwallet.ui.formatFiatForUi
import com.breadwallet.tools.manager.BRSharedPrefs
import com.breadwallet.tools.util.TokenUtil
import com.breadwallet.util.isBrd
import com.mikepenz.fastadapter.FastAdapter
import com.mikepenz.fastadapter.drag.IDraggable
import com.mikepenz.fastadapter.items.ModelAbstractItem
import com.squareup.picasso.Picasso
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Dispatchers.Default
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancelChildren
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.invoke
import kotlinx.coroutines.launch
import java.io.File
import java.math.BigDecimal
import java.text.NumberFormat
import java.util.Locale

class WalletListItem(
    wallet: Wallet
) : ModelAbstractItem<Wallet, WalletListItem.ViewHolder>(wallet), IDraggable {

    override val type: Int = R.id.wallet_list_item
    override val layoutRes: Int = R.layout.wallet_list_item
    override var identifier: Long = wallet.currencyId.hashCode().toLong()
    override val isDraggable: Boolean = true

    override fun getViewHolder(v: View) = ViewHolder(v)

    class ViewHolder(
        v: View
    ) : FastAdapter.ViewHolder<WalletListItem>(v) {

        private val boundScope = CoroutineScope(Dispatchers.Main + SupervisorJob())

        override fun bindView(item: WalletListItem, payloads: List<Any>) {
            val wallet = item.model
            val context = itemView.context
            val currencyCode = wallet.currencyCode

            if (currencyCode.isBrd() && !BRSharedPrefs.getRewardsAnimationShown()) {
                (itemView as ShimmerLayout).startShimmerAnimation()
            } else {
                (itemView as ShimmerLayout).stopShimmerAnimation()
            }

            // Format numeric data
            val preferredFiatIso = BRSharedPrefs.getPreferredFiatIso()
            val exchangeRate = wallet.fiatPricePerUnit.formatFiatForUi(preferredFiatIso)
            val fiatBalance = wallet.fiatBalance.formatFiatForUi(preferredFiatIso)
            val cryptoBalance = wallet.balance.formatCryptoForUi(currencyCode, MAX_CRYPTO_DIGITS)

            with(WalletListItemBinding.bind(itemView)) {
                if (wallet.fiatPricePerUnit == BigDecimal.ZERO) {
                    walletBalanceFiat.visibility = View.INVISIBLE
                    walletTradePrice.visibility = View.INVISIBLE
                } else {
                    walletBalanceFiat.visibility = View.VISIBLE
                    walletTradePrice.visibility = View.VISIBLE
                }

                val isSyncing = wallet.isSyncing
                val isLoading = wallet.state == Wallet.State.LOADING
                // Set wallet fields
                walletName.text = wallet.currencyName
                walletTradePrice.text = exchangeRate
                walletBalanceFiat.text = fiatBalance
                walletBalanceFiat.setTextColor(
                    context.getColor(
                        when {
                            isSyncing -> R.color.wallet_balance_fiat_syncing
                            else -> R.color.wallet_balance_fiat
                        }
                    )
                )
                walletBalanceCurrency.text = cryptoBalance
                walletBalanceCurrency.isGone = isSyncing || isLoading
                syncProgress.isVisible = isSyncing || isLoading
                syncingLabel.isVisible = isSyncing || isLoading
                if (isSyncing) {
                    val syncProgress = wallet.syncProgress
                    var labelText = context.getString(R.string.SyncingView_syncing)
                    if (syncProgress > 0) {
                        labelText += " ${NumberFormat.getPercentInstance()
                            .format(syncProgress.toDouble())}"
                    }
                    syncingLabel.text = labelText
                } else if (isLoading) {
                    syncingLabel.setText(R.string.Account_loadingMessage)
                }

                val priceChange2 = wallet.priceChange
                priceChange.visibility = if (priceChange2 != null) View.VISIBLE else View.INVISIBLE
                divider.visibility = if (priceChange2 != null) View.VISIBLE else View.INVISIBLE
                if (priceChange2 != null) {
                    priceChange.text = priceChange2.getPercentageChange()
                }

                if (itemView.tag == wallet.currencyCode) {
                    return
                }

                loadTokenIcon(this, currencyCode)
                setBackground(this, wallet, context)

                item.tag = wallet.currencyCode
            }
        }

        override fun unbindView(item: WalletListItem) {
            item.tag = null
            boundScope.coroutineContext.cancelChildren()
        }

        private fun loadTokenIcon(binding: WalletListItemBinding, currencyCode: String) {
            boundScope.launch {
                // Get icon for currency
                val tokenIconPath = Default {
                    TokenUtil.getTokenIconPath(currencyCode, false)
                }
                ensureActive()

                with(binding) {
                    if (tokenIconPath.isNullOrBlank()) {
                        iconLetter.visibility = View.VISIBLE
                        currencyIconWhite.visibility = View.GONE
                        iconLetter.text = currencyCode.take(1).toUpperCase(Locale.ROOT)
                    } else {
                        val iconFile = File(tokenIconPath)
                        Picasso.get().load(iconFile).into(currencyIconWhite)
                        iconLetter.visibility = View.GONE
                        currencyIconWhite.visibility = View.VISIBLE
                    }
                }
            }
        }

        private fun setBackground(binding: WalletListItemBinding, wallet: Wallet, context: Context) {
            val drawable = ContextCompat
                .getDrawable(context, R.drawable.crypto_card_shape)!!
                .mutate()
            if (wallet.isSupported) {
                // Create gradient if 2 colors exist.
                val startColor = Color.parseColor(wallet.startColor ?: return)
                val endColor = Color.parseColor(wallet.endColor ?: return)
                (drawable as GradientDrawable).colors = intArrayOf(startColor, endColor)
                drawable.orientation = GradientDrawable.Orientation.LEFT_RIGHT
                binding.walletCard.background = drawable
                setWalletItemColors(binding, R.dimen.token_background_no_alpha)
            } else {
                // To ensure that the unsupported wallet card has the same shape as
                // the supported wallet card, we reuse the drawable.
                (drawable as GradientDrawable).colors = intArrayOf(
                    context.getColor(R.color.wallet_delisted_token_background),
                    context.getColor(R.color.wallet_delisted_token_background)
                )
                binding.walletCard.background = drawable
                setWalletItemColors(binding, R.dimen.token_background_with_alpha)
            }
        }

        private fun setWalletItemColors(binding: WalletListItemBinding, dimenRes: Int) {
            val typedValue = TypedValue()
            itemView.context.resources.getValue(dimenRes, typedValue, true)
            val alpha = typedValue.float
            with(binding) {
                currencyIconWhite.alpha = alpha
                walletName.alpha = alpha
                walletTradePrice.alpha = alpha
                walletBalanceFiat.alpha = alpha
                walletBalanceCurrency.alpha = alpha
            }
        }
    }
}
