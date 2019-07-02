/**
 * BreadWallet
 * <p/>
 * Created by Drew Carlson <drew.carlson@breadwallet.com> on 6/6/2019.
 * Copyright (c) 2019 breadwallet LLC
 * <p/>
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 * <p/>
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 * <p/>
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package com.breadwallet.ui.pricealert

import android.app.Activity
import android.arch.lifecycle.LifecycleOwner
import android.arch.lifecycle.Observer
import android.content.Intent
import android.os.Bundle
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import com.breadwallet.R
import com.breadwallet.presenter.activities.util.BRActivity
import com.breadwallet.presenter.entities.CurrencyEntity
import com.breadwallet.ui.util.bindCreated
import com.breadwallet.ui.util.viewModel
import kotlinx.android.synthetic.main.activity_select_for_currency.*
import java.util.*

/**
 * Displays a list of currencies and finishes with a result
 * containing [OUTPUT_CURRENCY_ISO] upon user selection.
 */
class SelectForCurrencyActivity : BRActivity() {

    companion object {
        private val TAG = SelectForCurrencyActivity::class.java.simpleName

        const val OUTPUT_CURRENCY_ISO = "CURRENCY_ISO"

        fun openForResult(activity: Activity, requestCode: Int) {
            val intent = Intent(activity, SelectForCurrencyActivity::class.java)
            activity.startActivityForResult(intent, requestCode)
        }
    }

    private val viewModel by viewModel { SelectForCurrencyViewModel() }

    private val adapter by bindCreated { CurrencyAdapter(viewModel::selectCurrency) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_select_for_currency)

        back_arrow.setOnClickListener { onBackPressed() }

        currency_list.layoutManager = LinearLayoutManager(this)
        currency_list.adapter = adapter
    }

    override fun onResume() {
        super.onResume()
        val owner: LifecycleOwner = this
        viewModel.apply {
            getCurrencies().observe(owner, Observer { currencies ->
                adapter.currencies = currencies!!
            })
            getSelectedCurrency().observe(owner, Observer { selection ->
                setResult(RESULT_OK, Intent().apply {
                    putExtra(OUTPUT_CURRENCY_ISO, selection!!.code)
                })
                finish()
                overridePendingTransition(R.anim.enter_from_left, R.anim.exit_to_right)
            })
        }
    }

    override fun onBackPressed() {
        super.onBackPressed()
        overridePendingTransition(R.anim.enter_from_left, R.anim.exit_to_right)
    }

    private class CurrencyAdapter(
            private val onSelectListener: (CurrencyEntity) -> Unit
    ) : RecyclerView.Adapter<CurrencyAdapter.ViewHolder>() {

        var currencies: List<CurrencyEntity> = emptyList()
            set(value) {
                field = value
                notifyDataSetChanged()
            }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val inflater = LayoutInflater.from(parent.context)
            val view = inflater.inflate(R.layout.currency_item, parent, false)
            return ViewHolder(view)
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val item = currencies[position]
            val currency: Currency? = try {
                Currency.getInstance(item.code)
            } catch (ignored: IllegalArgumentException) {
                null
            }

            holder.currencyLabel.text = currency?.run { "${item.code} ($symbol)" } ?: item.code
            holder.itemView.setOnClickListener {
                onSelectListener.invoke(currencies[holder.adapterPosition])
            }
        }

        override fun getItemCount(): Int = currencies.size

        class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
            val currencyLabel: TextView = view.findViewById(R.id.currency_item_text)
        }
    }
}