/**
 * BreadWallet
 *
 * Created by Ahsan Butt <ahsan.butt@breadwallet.com> on 5/05/20.
 * Copyright (c) 2020 breadwallet LLC
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

import android.view.View
import com.breadwallet.R
import com.breadwallet.databinding.StakingViewBinding
import com.breadwallet.tools.util.TokenUtil
import com.breadwallet.util.CurrencyCode
import com.mikepenz.fastadapter.FastAdapter
import com.mikepenz.fastadapter.items.AbstractItem
import com.squareup.picasso.Picasso
import java.io.File

class StakingItem(
    val currencyCode: CurrencyCode
) : AbstractItem<StakingItem.ViewHolder>() {

    override val type: Int = R.id.staking_item
    override val layoutRes: Int = R.layout.staking_view
    override fun getViewHolder(v: View) = ViewHolder(v)

    class ViewHolder(v: View) : FastAdapter.ViewHolder<StakingItem>(v) {

        override fun bindView(item: StakingItem, payloads: List<Any>) {
            with(StakingViewBinding.bind(itemView)) {
                val tokenIconPath = TokenUtil.getTokenIconPath(item.currencyCode, true)

                if (!tokenIconPath.isNullOrBlank()) {
                    val iconFile = File(tokenIconPath)
                    Picasso.get().load(iconFile).into(stakingTokenIcon)
                }
            }
        }

        override fun unbindView(item: StakingItem) = Unit
    }
}