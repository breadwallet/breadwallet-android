package com.breadwallet.ui.atm

import android.graphics.Color
import android.view.View
import android.widget.TextView
import cash.just.sdk.model.AtmMachine
import com.breadwallet.R
import com.mikepenz.fastadapter.FastAdapter
import com.mikepenz.fastadapter.items.AbstractItem

open class AtmItem(var atmMachine: AtmMachine) : AbstractItem<AtmItem.ViewHolder>() {

    /** defines the type defining this item. must be unique. preferably an id */
    override val type: Int
        get() = R.id.addressLocation

    /** defines the layout which will be used for this item in the list */
    override val layoutRes: Int
        get() = R.layout.atm_item

    override fun getViewHolder(v: View): ViewHolder {
        return ViewHolder(v)
    }

    class ViewHolder(view: View) : FastAdapter.ViewHolder<AtmItem>(view) {
        var root: View = view.findViewById(R.id.root_view)
        var name: TextView = view.findViewById(R.id.item_date)
        var description: TextView = view.findViewById(R.id.item_description)
        var btc: View = view.findViewById(R.id.btcIcon)
        var cash: View = view.findViewById(R.id.cashIcon)

        override fun unbindView(item: AtmItem) {
            name.text = null
            description.text = null
        }

        override fun bindView(item: AtmItem, payloads: MutableList<Any>) {
            name.text = item.atmMachine.addressDesc
            description.text = "${item.atmMachine.city} ${item.atmMachine.zip}"

            if (item.atmMachine.redemption == 0) {
                cash.visibility = View.GONE
                root.setBackgroundColor(Color.parseColor("#f0eeee"))
            } else {
                root.setBackgroundColor(Color.WHITE)
                cash.visibility = View.VISIBLE
            }
        }
    }
}