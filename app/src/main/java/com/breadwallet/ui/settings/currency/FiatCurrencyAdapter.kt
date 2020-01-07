package com.breadwallet.ui.settings.currency

import android.annotation.SuppressLint
import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.core.view.isVisible
import com.breadwallet.R
import com.breadwallet.legacy.presenter.customviews.BaseTextView
import kotlinx.android.synthetic.main.currency_list_item.view.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.SendChannel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import java.util.Currency

class FiatCurrencyAdapter(
    private val currenciesFlow: Flow<List<FiatCurrency>>,
    private val selectedCurrencyFlow: Flow<String>,
    private val sendChannel: SendChannel<DisplayCurrency.E>
) : RecyclerView.Adapter<FiatCurrencyAdapter.CurrencyViewHolder>() {

    private var currencies: List<FiatCurrency> = emptyList()
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
        viewHolder.check.isVisible = currency.code.equals(selectedCurrencyCode, true)
        try {
            viewHolder.label.text =
                "${currency.code}  (${Currency.getInstance(currency.code).symbol})"
        } catch (ignored: IllegalArgumentException) {
            viewHolder.label.text = currency.code
        }
        viewHolder.itemView.setOnClickListener {
            sendChannel.offer(DisplayCurrency.E.OnCurrencySelected(currencyCode = currency.code))
        }
    }

    class CurrencyViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val label: BaseTextView = view.currency_item_text
        val check: ImageView = view.currency_checkmark
    }
}