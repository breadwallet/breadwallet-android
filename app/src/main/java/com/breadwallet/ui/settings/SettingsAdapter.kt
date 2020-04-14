/**
 * BreadWallet
 *
 * Created by Pablo Budelli <pablo.budelli@breadwallet.com> on 10/17/19.
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
package com.breadwallet.ui.settings

import android.graphics.Color
import androidx.recyclerview.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.TextView
import androidx.core.view.isGone
import androidx.core.view.isVisible
import com.breadwallet.R
import com.breadwallet.legacy.presenter.customviews.BaseTextView
import kotlinx.android.synthetic.main.settings_list_item.view.*

class SettingsAdapter(
    private val items: List<SettingsItem>,
    private val onClick: (SettingsOption) -> Unit
) :
    RecyclerView.Adapter<SettingsAdapter.SettingsViewHolder>() {

    override fun onCreateViewHolder(viewGroup: ViewGroup, viewType: Int): SettingsViewHolder {
        val inflater = LayoutInflater.from(viewGroup.context)
        val view = inflater.inflate(R.layout.settings_list_item, viewGroup, false)
        return SettingsViewHolder(view)
    }

    override fun onBindViewHolder(viewHolder: SettingsViewHolder, position: Int) {
        viewHolder.bindView(items[position])
        viewHolder.itemView.setOnClickListener { onClick(items[viewHolder.adapterPosition].option) }
    }

    override fun getItemCount() = items.size

    class SettingsViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        private val title: BaseTextView get() = itemView.item_title
        private val icon: ImageButton get() = itemView.setting_icon
        private val addon: TextView get() = itemView.item_addon
        private val subHeader: TextView get() = itemView.item_sub_header
        private val rootView:View = view
        fun bindView(item: SettingsItem) {
            title.text = item.title
            icon.isVisible = item.iconResId != null
            item.iconResId?.let { icon.setBackgroundResource(it) }
            addon.text = item.addOn
            subHeader.isGone = item.subHeader.isBlank()
            subHeader.text = item.subHeader

            if (item.option == SettingsOption.ATM_FINDER) {
                rootView.setBackgroundColor(Color.BLACK)
            }
        }
    }
}
