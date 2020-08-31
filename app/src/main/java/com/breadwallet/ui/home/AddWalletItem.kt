package com.breadwallet.ui.home

import android.view.View
import com.breadwallet.R
import com.mikepenz.fastadapter.FastAdapter
import com.mikepenz.fastadapter.drag.IDraggable
import com.mikepenz.fastadapter.items.AbstractItem
import kotlinx.android.extensions.LayoutContainer
import kotlinx.android.synthetic.main.add_wallets_item.*

class AddWalletItem : AbstractItem<AddWalletItem.ViewHolder>(), IDraggable {

    override val type: Int = R.id.add_wallet_item
    override val layoutRes: Int = R.layout.add_wallets_item
    override var identifier: Long = 0

    override val isDraggable: Boolean = false

    override fun getViewHolder(v: View) = ViewHolder(v)

    class ViewHolder(
        override val containerView: View
    ) : FastAdapter.ViewHolder<AddWalletItem>(containerView),
        LayoutContainer{

        init {
            val res = containerView.resources
            add_wallets.text = "+ ${res.getString(R.string.TokenList_addTitle)}"
        }

        override fun bindView(
            item: AddWalletItem,
            payloads: List<Any>
        ) = Unit

        override fun unbindView(item: AddWalletItem) = Unit
    }
}
