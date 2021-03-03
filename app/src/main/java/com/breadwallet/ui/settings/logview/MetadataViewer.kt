/**
 * BreadWallet
 *
 * Created by Drew Carlson <drew.carlson@breadwallet.com> on 12/3/20.
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
package com.breadwallet.ui.settings.logview

import android.content.Context
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.FrameLayout
import android.widget.FrameLayout.LayoutParams
import android.widget.FrameLayout.LayoutParams.MATCH_PARENT
import android.widget.FrameLayout.LayoutParams.WRAP_CONTENT
import android.widget.LinearLayout
import android.widget.LinearLayout.LayoutParams.MATCH_PARENT as LL_MATCH_PARENT
import android.widget.LinearLayout.LayoutParams.WRAP_CONTENT as LL_WRAP_CONTENT
import android.widget.TextView
import androidx.core.view.get
import androidx.core.view.setMargins
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bluelinelabs.conductor.changehandler.FadeChangeHandler
import com.breadwallet.ui.BaseController
import com.breadwallet.ui.flowbind.clicks
import com.platform.interfaces.KVStoreProvider
import com.platform.util.getJSONObjectOrNull
import kotlinx.coroutines.Dispatchers.Default
import kotlinx.coroutines.Dispatchers.Main
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combineTransform
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import org.json.JSONObject
import org.kodein.di.erased.instance
import android.widget.LinearLayout.LayoutParams as LinearLayoutParams

private const val LOG_LINE_MARGIN = 8

class MetadataViewer(args: Bundle? = null) : BaseController(args) {

    init {
        overridePopHandler(FadeChangeHandler(false))
        overridePushHandler(FadeChangeHandler(false))
    }

    private val kvStore: KVStoreProvider by instance()
    private val metadataKeys = MutableStateFlow<List<String>>(emptyList())
    private val filterFlow = MutableStateFlow(Filter.NON_TX)
    private val adapter = object : RecyclerView.Adapter<MetadataHolder>() {
        override fun getItemCount(): Int = metadataKeys.value.size
        override fun onBindViewHolder(holder: MetadataHolder, position: Int) {
            val key = metadataKeys.value[position]
            holder.setData(key, kvStore.get(key), isAlt = position % 2 == 0)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MetadataHolder {
            return MetadataHolder(parent.context)
        }
    }

    enum class Filter {
        NON_TX, ALL_TX, GIFT_TX
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup, savedViewState: Bundle?): View {
        return LinearLayout(container.context).apply {
            isClickable = true
            orientation = LinearLayout.VERTICAL
            val recycler = RecyclerView(context).apply {
                background = ColorDrawable(Color.BLACK)
                adapter = this@MetadataViewer.adapter
                layoutManager = LinearLayoutManager(context)
            }
            val actions = LinearLayout(context).apply {
                background = ColorDrawable(Color.BLACK)
                orientation = LinearLayout.HORIZONTAL
                weightSum = 4f

                val showTxnsButton = Button(context).apply {
                    filterFlow
                        .onEach { filter ->
                            text = when (filter) {
                                Filter.NON_TX -> "Show Txns"
                                Filter.ALL_TX -> "Show Gifts"
                                Filter.GIFT_TX -> "Show Non Txns"
                            }
                        }
                        .flowOn(Main)
                        .launchIn(viewAttachScope)
                    clicks()
                        .map {
                            val next = filterFlow.value.ordinal + 1
                            filterFlow.value = Filter.values()[next % Filter.values().size]
                        }
                        .launchIn(viewAttachScope)
                }
                addView(showTxnsButton, LinearLayoutParams(LL_WRAP_CONTENT, LL_WRAP_CONTENT).apply { weight = 1f })
            }
            addView(recycler, LinearLayoutParams(LL_MATCH_PARENT, LL_MATCH_PARENT).apply { weight = .8f })
            addView(actions, LinearLayoutParams(LL_MATCH_PARENT, LL_WRAP_CONTENT).apply { weight = .2f })
        }
    }

    override fun onAttach(view: View) {
        super.onAttach(view)
        kvStore.keysFlow()
            .combineTransform(filterFlow) { keys, filter ->
                emit(when (filter) {
                    Filter.ALL_TX -> keys.filter { it.startsWith("txn2") }
                    Filter.NON_TX -> keys.filterNot { it.startsWith("txn2") }
                    Filter.GIFT_TX -> keys.filter { it.startsWith("txn2") }
                        .filter { key ->
                            (kvStore.get(key) ?: JSONObject())
                                .getJSONObjectOrNull("gift") != null
                        }
                })
            }
            .flowOn(Default)
            .onEach { keys ->
                metadataKeys.value = keys
                adapter.notifyDataSetChanged()
            }
            .flowOn(Main)
            .launchIn(viewAttachScope)
    }

    class MetadataHolder(context: Context) : RecyclerView.ViewHolder(
        FrameLayout(context).apply {
            layoutParams = LayoutParams(MATCH_PARENT, WRAP_CONTENT)
            addView(
                TextView(context),
                LayoutParams(MATCH_PARENT, WRAP_CONTENT).apply {
                    setMargins(LOG_LINE_MARGIN)
                }
            )
        }
    ) {
        fun setData(key: String, metadata: JSONObject?, isAlt: Boolean) {
            val textView = ((itemView as ViewGroup)[0] as TextView)
            val value = metadata?.toString(2)
            textView.text = "KEY: $key\nVALUE: ${value}\n"
        }
    }
}
