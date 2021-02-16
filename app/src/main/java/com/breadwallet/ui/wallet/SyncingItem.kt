/**
 * BreadWallet
 *
 * Created by Drew Carlson <drew.carlson@breadwallet.com> on 4/23/20.
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
import com.breadwallet.databinding.WalletSyncProgressViewBinding
import com.mikepenz.fastadapter.FastAdapter
import com.mikepenz.fastadapter.items.AbstractItem
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Locale

private const val SYNCED_THROUGH_DATE_FORMAT = "MM/dd/yy HH:mm"

class SyncingItem : AbstractItem<SyncingItem.ViewHolder>() {

    var syncProgress = 1f
    var syncThroughMillis = 0L
    val hasSyncTime
        get() = syncThroughMillis != 0L

    override val type: Int = R.id.syncing_item
    override val layoutRes: Int = R.layout.wallet_sync_progress_view
    override fun getViewHolder(v: View) = ViewHolder(v)

    class ViewHolder(
        v: View
    ) : FastAdapter.ViewHolder<SyncingItem>(v) {

        private val resources = v.resources
        private val dateFormat = SimpleDateFormat(SYNCED_THROUGH_DATE_FORMAT, Locale.US)
        private val numberFormat = NumberFormat.getPercentInstance()

        override fun bindView(item: SyncingItem, payloads: List<Any>) {
            val syncingText = resources.getString(R.string.SyncingView_syncing)
            val binding = WalletSyncProgressViewBinding.bind(itemView)
            val syncingPercentText = numberFormat.format(item.syncProgress)
            binding.syncingLabel.text = "%s %s".format(syncingText, syncingPercentText)

            if (item.hasSyncTime) {
                val syncedThroughDate = dateFormat.format(item.syncThroughMillis)
                val syncedThroughText = resources.getString(R.string.SyncingView_syncedThrough)
                binding.syncStatusLabel.text = syncedThroughText.format(syncedThroughDate)
            }
        }

        override fun unbindView(item: SyncingItem) = Unit
    }
}