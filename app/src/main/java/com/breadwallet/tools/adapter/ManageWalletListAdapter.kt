package com.breadwallet.tools.adapter

import android.app.Activity
import android.content.Context
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.support.v4.view.MotionEventCompat
import android.support.v7.widget.RecyclerView
import android.util.TypedValue
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageButton
import android.widget.ImageView

import com.breadwallet.R
import com.breadwallet.presenter.customviews.BaseTextView
import com.breadwallet.tools.animation.ItemTouchHelperAdapter
import com.breadwallet.tools.animation.ItemTouchHelperViewHolder
import com.breadwallet.tools.listeners.OnStartDragListener
import com.breadwallet.tools.util.TokenUtil
import com.breadwallet.ui.managewallets.Wallet
import com.breadwallet.ui.util.swap
import com.squareup.picasso.Picasso

import java.io.File
import java.util.Collections

class ManageWalletListAdapter(
    private val context: Context,
    private val showOrHideListener: OnWalletShowOrHideListener,
    private val addWalletListener: () -> Unit,
    private val startDragListener: OnStartDragListener,
    private val moveWalletListener: (wallets: List<Wallet>) -> Unit
) : RecyclerView.Adapter<RecyclerView.ViewHolder>(),
    ItemTouchHelperAdapter {

    interface OnWalletShowOrHideListener {
        fun onShow(wallet: Wallet)
        fun onHide(wallet: Wallet)
    }

    var wallets: List<Wallet> = emptyList()
        set(value) {
            field = value
            notifyDataSetChanged()
        }

    override fun onBindViewHolder(
        holder: RecyclerView.ViewHolder,
        position: Int
    ) {
        when (getItemViewType(position)) {
            VIEW_TYPE_WALLET -> bindWalletHolder(holder as ManageWalletViewHolder, position)
            else -> bindAddWalletHolder(holder as AddWalletItemViewHolder)
        }
    }

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): RecyclerView.ViewHolder {

        val inflater = (context as Activity).layoutInflater
        val convertView: View

        return when (viewType) {
            VIEW_TYPE_WALLET -> {
                convertView = inflater.inflate(R.layout.manage_wallets_list_item, parent, false)
                ManageWalletViewHolder(convertView)
            }
            else -> {
                convertView = inflater.inflate(R.layout.add_wallets_item, parent, false)
                AddWalletItemViewHolder(convertView)
            }
        }
    }

    override fun getItemViewType(position: Int): Int {
        return if (position < wallets.size) {
            VIEW_TYPE_WALLET
        } else {
            VIEW_TYPE_ADD_WALLET
        }
    }

    override fun getItemCount(): Int {
        // We add 1 here because this adapter has an "extra" item at the bottom, which is the
        // "Add Wallets" footer
        return wallets.size + 1
    }

    open inner class ManageWalletViewHolder(view: View) : RecyclerView.ViewHolder(view),
        ItemTouchHelperViewHolder {

        var dragHandle: ImageButton? = null
        val tokenTicker: BaseTextView
        val tokenName: BaseTextView
        val tokenBalance: BaseTextView
        val showHide: Button
        val tokenIcon: ImageView
        val tokenLetter: BaseTextView
        val iconParent: View

        init {
            dragHandle = view.findViewById(R.id.drag_icon)
            tokenTicker = view.findViewById(R.id.token_symbol)
            tokenName = view.findViewById(R.id.token_name)
            tokenBalance = view.findViewById(R.id.token_balance)
            showHide = view.findViewById(R.id.show_hide_button)
            tokenIcon = view.findViewById(R.id.token_icon)
            tokenLetter = view.findViewById(R.id.icon_letter)
            iconParent = view.findViewById(R.id.icon_parent)
        }

        override fun onItemClear() = Unit

        override fun onItemSelected() = Unit
    }

    inner class AddWalletItemViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        private val mAddWalletsLabel: BaseTextView = view.findViewById(R.id.add_wallets)
        val mParent: View = view.findViewById(R.id.wallet_card)

        init {
            mAddWalletsLabel.text = "+ ${context.getString(R.string.TokenList_addTitle)}"
        }
    }

    override fun onItemDismiss(position: Int) = Unit

    override fun onItemMove(fromPosition: Int, toPosition: Int) {
        if (toPosition >= wallets.size) return

        notifyItemMoved(fromPosition, toPosition)
        wallets.swap(fromPosition, toPosition)
        moveWalletListener(wallets)
    }

    private fun bindWalletHolder(walletHolder: ManageWalletViewHolder, position: Int) {
        val wallet = wallets[position]
        val currencyCode = wallet.currencyCode.toLowerCase()

        val tokenIconPath = TokenUtil.getTokenIconPath(context, currencyCode, true)
        val iconDrawable = walletHolder.iconParent.background as GradientDrawable

        if (!tokenIconPath.isNullOrEmpty()) {
            walletHolder.tokenIcon.visibility = View.VISIBLE
            walletHolder.tokenLetter.visibility = View.GONE
            val iconFile = File(tokenIconPath)
            Picasso.get().load(iconFile).into(walletHolder.tokenIcon)
            iconDrawable.setColor(Color.TRANSPARENT)
        } else {
            // If no icon is present, then use the capital first letter of the token currency code instead.
            walletHolder.tokenIcon.visibility = View.GONE
            walletHolder.tokenLetter.visibility = View.VISIBLE
            walletHolder.tokenLetter.text = currencyCode.substring(0, 1).toUpperCase()
            iconDrawable.setColor(Color.parseColor(TokenUtil.getTokenStartColor(currencyCode)))
        }

        walletHolder.tokenName.text = wallets[position].name
        walletHolder.tokenTicker.text = wallets[position].currencyCode

        val typeface =
            Typeface.createFromAsset(context.assets, FONT_ASSET_PATH)
        walletHolder.showHide.typeface = typeface

        val showWalletTypedValue = TypedValue()
        val hideWalletTypedValue = TypedValue()

        context.theme.resolveAttribute(
            R.attr.show_wallet_button_background,
            showWalletTypedValue,
            true
        )
        context.theme.resolveAttribute(
            R.attr.hide_wallet_button_background,
            hideWalletTypedValue,
            true
        )

        when {
            wallet.enabled -> {
                walletHolder.showHide.background =
                    context.getDrawable(hideWalletTypedValue.resourceId)
                walletHolder.showHide.text = context.getString(R.string.TokenList_hide)
                walletHolder.showHide.setTextColor(context.getColor(R.color.button_cancel_add_wallet_text))
                walletHolder.showHide.setOnClickListener { showOrHideListener.onHide(wallet) }
            }
            else -> {
                walletHolder.showHide.background =
                    context.getDrawable(showWalletTypedValue.resourceId)
                walletHolder.showHide.text = context.getString(R.string.TokenList_show)
                walletHolder.showHide.setTextColor(context.getColor(R.color.button_add_wallet_text))
                walletHolder.showHide.setOnClickListener { showOrHideListener.onShow(wallet) }
            }
        }

        walletHolder.dragHandle!!.setOnTouchListener { v, event ->
            if (MotionEventCompat.getActionMasked(event) == MotionEvent.ACTION_DOWN) {
                startDragListener.onStartDrag(walletHolder)
            }
            false
        }
    }

    private fun bindAddWalletHolder(holder: AddWalletItemViewHolder) =
        holder.mParent.setOnClickListener { addWalletListener() }

    companion object {
        private const val VIEW_TYPE_WALLET = 0
        private const val VIEW_TYPE_ADD_WALLET = 1
        private const val FONT_ASSET_PATH = "fonts/CircularPro-Book.otf"
    }
}
