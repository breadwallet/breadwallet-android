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

import android.arch.lifecycle.LifecycleOwner
import android.arch.lifecycle.Observer
import android.content.Context
import android.content.Intent
import android.os.Bundle
import com.breadwallet.R
import com.breadwallet.presenter.activities.util.BRActivity
import com.breadwallet.ui.pricealert.SelectAlertCryptoActivity.Companion.OUTPUT_TOKEN_SYMBOL
import com.breadwallet.ui.pricealert.SelectForCurrencyActivity.Companion.OUTPUT_CURRENCY_ISO
import com.breadwallet.ui.util.DefaultTextWatcher
import com.breadwallet.ui.util.viewModel
import kotlinx.android.synthetic.main.activity_new_price_alert.*

/**
 * Displays a configurable form for creating [PriceAlert]s.
 */
class NewPriceAlertActivity : BRActivity() {

    companion object {
        private const val DEFAULT_CURRENCY = "DEFAULT_CURRENCY"

        private const val SELECT_ALERT_CRYPTO_REQUEST = 0
        private const val SELECT_FOR_CURRENCY_REQUEST = 1

        private const val ALPHA_VISIBLE = 1f
        private const val ALPHA_INVISIBLE = 0f
        private const val ANIMATION_TRANSLATION_ORIGINAL = 0f
        private const val ANIMATION_TRANSLATION_OFFSET = -10f

        @JvmStatic
        fun open(context: Context, defaultCurrency: String? = null) =
                context.startActivity(Intent(context, NewPriceAlertActivity::class.java).apply {
                    if (!defaultCurrency.isNullOrBlank()) {
                        putExtra(DEFAULT_CURRENCY, defaultCurrency)
                    }
                })
    }

    private val viewModel by viewModel {
        NewPriceAlertViewModel(intent.getStringExtra(DEFAULT_CURRENCY))
    }

    private val valueTextWatcher = object : DefaultTextWatcher() {
        override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
            viewModel.setAlertValue(s?.toString()?.toFloatOrNull() ?: 0f)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_new_price_alert)

        back_button.setOnClickListener { onBackPressed() }
        select_crypto_button.setOnClickListener {
            SelectAlertCryptoActivity.openForResult(this, SELECT_ALERT_CRYPTO_REQUEST)
            overridePendingTransition(R.anim.enter_from_right, R.anim.exit_to_left)
        }
        save_alert_button.setOnClickListener {
            viewModel.saveAlert()
        }
        select_to_currency.setOnClickListener {
            SelectForCurrencyActivity.openForResult(this, SELECT_FOR_CURRENCY_REQUEST)
            overridePendingTransition(R.anim.enter_from_right, R.anim.exit_to_left)
        }
        select_alert_when.setOnClickListener {
            viewModel.goToNextAlert()
        }
        select_window_button.setOnClickListener {
            viewModel.goToNextWindow()
        }

        value_input.addTextChangedListener(valueTextWatcher)

        val owner: LifecycleOwner = this
        viewModel.apply {
            getSelectedCurrency().observe(owner, Observer { selected ->
                select_crypto_button.text = selected
            })

            getToCurrency().observe(owner, Observer { selected ->
                select_to_currency.text = selected
            })

            getExchangeRate().observe(owner, Observer { currentRate ->
                current_rate_label.text = currentRate?.toString()
            })

            getSelectedAlertName().observe(owner, Observer { label ->
                select_alert_when.text = label
            })

            getSelectedWindowName().observe(owner, Observer { label ->
                select_window_button.text = label
            })

            getAlertSaved().observe(owner, Observer { saved ->
                if (saved == true) {
                    finish()
                    overridePendingTransition(R.anim.enter_from_left, R.anim.exit_to_right)
                }
            })

            getWindowSelectVisible().observe(owner, Observer { visible ->
                if (visible!!) {
                    select_window_button.apply {
                        translationY = ANIMATION_TRANSLATION_OFFSET
                        animate().alpha(ALPHA_VISIBLE)
                                .translationY(ANIMATION_TRANSLATION_ORIGINAL)
                    }
                    window_label.apply {
                        translationY = ANIMATION_TRANSLATION_OFFSET
                        animate().alpha(ALPHA_VISIBLE)
                                .translationY(ANIMATION_TRANSLATION_ORIGINAL)
                    }
                } else {
                    select_window_button.animate()
                            .alpha(ALPHA_INVISIBLE)
                            .translationY(ANIMATION_TRANSLATION_OFFSET)
                    window_label.animate()
                            .alpha(ALPHA_INVISIBLE)
                            .translationY(ANIMATION_TRANSLATION_OFFSET)
                }
            })
        }
    }

    override fun onDestroy() {
        value_input.removeTextChangedListener(valueTextWatcher)
        super.onDestroy()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode == RESULT_OK) {
            when (requestCode) {
                SELECT_ALERT_CRYPTO_REQUEST -> {
                    viewModel.setSelectedCurrency(data!!.getStringExtra(OUTPUT_TOKEN_SYMBOL))
                }
                SELECT_FOR_CURRENCY_REQUEST -> {
                    viewModel.setToCurrency(data!!.getStringExtra(OUTPUT_CURRENCY_ISO))
                }
            }
        }
    }

    override fun onBackPressed() {
        super.onBackPressed()
        overridePendingTransition(R.anim.enter_from_left, R.anim.exit_to_right)
    }
}