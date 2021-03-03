/**
 * BreadWallet
 *
 * Created by Pablo Budelli <pablo.budelli@breadwallet.com> on 1/7/20.
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
package com.breadwallet.ui.settings.currency

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.core.view.isVisible
import androidx.recyclerview.widget.RecyclerView
import com.breadwallet.R
import com.breadwallet.databinding.CurrencyListItemBinding
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.SendChannel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import java.util.Currency

class FiatCurrencyAdapter(
    private val currenciesFlow: Flow<List<String>>,
    private val selectedCurrencyFlow: Flow<String>,
    private val sendChannel: SendChannel<DisplayCurrency.E>
) : RecyclerView.Adapter<FiatCurrencyAdapter.CurrencyViewHolder>() {

    private var currencies: List<String> = emptyList()
    private var selectedCurrencyCode: String = ""

    override fun onAttachedToRecyclerView(recyclerView: RecyclerView) {
        super.onAttachedToRecyclerView(recyclerView)
        currenciesFlow
            .onEach { currencies ->
                this.currencies = currencies
                notifyDataSetChanged()
            }
            .launchIn(CoroutineScope(Dispatchers.Main))

        selectedCurrencyFlow
            .onEach { currency ->
                this.selectedCurrencyCode = currency
                notifyDataSetChanged()
            }
            .launchIn(CoroutineScope(Dispatchers.Main))
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CurrencyViewHolder {
        val view = LayoutInflater
            .from(parent.context)
            .inflate(R.layout.currency_list_item, parent, false)
        return CurrencyViewHolder(view)
    }

    override fun getItemCount() = currencies.size

    @SuppressLint("SetTextI18n")
    override fun onBindViewHolder(viewHolder: CurrencyViewHolder, position: Int) {
        val currency = currencies[position]
        viewHolder.check.isVisible = currency.equals(selectedCurrencyCode, true)
        try {
            viewHolder.label.text = "$currency  (${Currency.getInstance(currency).symbol})"
        } catch (ignored: IllegalArgumentException) {
            viewHolder.label.text = currency
        }
        viewHolder.itemView.setOnClickListener {
            sendChannel.offer(DisplayCurrency.E.OnCurrencySelected(currencyCode = currency))
        }
    }

    class CurrencyViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val binding = CurrencyListItemBinding.bind(view)
        val label: TextView = binding.currencyItemText
        val check: ImageView = binding.currencyCheckmark
    }
}
