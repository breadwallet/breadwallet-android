/**
 * BreadWallet
 * <p/>
 * Created by Drew Carlson <drew.carlson@breadwallet.com> on 6/2/2019.
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

import android.content.Context
import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import com.breadwallet.R
import com.breadwallet.model.PriceAlert
import com.breadwallet.presenter.customviews.BaseTextView
import com.breadwallet.tools.util.CurrencyUtils
import java.math.RoundingMode

class PriceAlertAdapter(
        private val context: Context,
        private val onDeleteListener: (@ParameterName("priceAlert") PriceAlert) -> Unit
) : RecyclerView.Adapter<PriceAlertAdapter.ViewHolder>() {

    var priceAlerts: List<PriceAlert> = emptyList()
        set(value) {
            field = value
            notifyDataSetChanged()
        }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val alert = priceAlerts[position]
        holder.alertLabel.text = alert.asLabelString()
        holder.deleteAlert.setOnClickListener {
            onDeleteListener.invoke(priceAlerts[position])
        }
    }

    override fun getItemCount(): Int = priceAlerts.size

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val inflater = LayoutInflater.from(context)
        val view = inflater.inflate(R.layout.price_alert_list_item, parent, false)

        return ViewHolder(view).apply { setIsRecyclable(true) }
    }

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val alertLabel: BaseTextView = view.findViewById(R.id.alert_label)
        val deleteAlert: ImageButton = view.findViewById(R.id.delete_alert_button)
    }

    private fun PriceAlert.asLabelString(): String {
        val roundedValue = value.toBigDecimal()
                .setScale(2, RoundingMode.HALF_EVEN)
                .toString()
        val formattedValue by lazy {
            CurrencyUtils.getFormattedAmount(context, toCurrencyCode, value.toBigDecimal())
        }
        return when {
            isPriceTargetType() -> when (direction) {
                PriceAlert.Direction.INCREASE ->
                    context.getString(R.string.PriceAlertList_priceIncreasedByValue, forCurrencyCode, formattedValue)
                PriceAlert.Direction.DECREASE ->
                    context.getString(R.string.PriceAlertList_priceDecreasedByValue, forCurrencyCode, formattedValue)
                else -> error("Unsupported direction $direction")
            }
            else -> context.getString(when (type) {
                PriceAlert.Type.PERCENTAGE_CHANGE -> when (direction) {
                    PriceAlert.Direction.BOTH -> R.string.PriceAlertList_percentChanged
                    PriceAlert.Direction.INCREASE -> R.string.PriceAlertList_percentIncreased
                    PriceAlert.Direction.DECREASE -> R.string.PriceAlertList_percentDecreased
                }
                PriceAlert.Type.PERCENTAGE_CHANGE_DAY -> when (direction) {
                    PriceAlert.Direction.BOTH -> R.string.PriceAlertList_percentChangedInDay
                    PriceAlert.Direction.INCREASE -> R.string.PriceAlertList_percentIncreasedInDay
                    PriceAlert.Direction.DECREASE -> R.string.PriceAlertList_percentDecreasedInDay
                }
                PriceAlert.Type.PERCENTAGE_CHANGE_DAY_WEEK -> when (direction) {
                    PriceAlert.Direction.BOTH -> R.string.PriceAlertList_percentChangedInDayOrWeek
                    PriceAlert.Direction.INCREASE -> R.string.PriceAlertList_percentIncreasedInDayOrWeek
                    PriceAlert.Direction.DECREASE -> R.string.PriceAlertList_percentDecreasedInDayOrWeek
                }
                else -> error("Unsupported type $type")
            }, forCurrencyCode, roundedValue)
        }
    }
}