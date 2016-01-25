
package com.breadwallet.presenter.fragments;

import android.app.Fragment;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.breadwallet.R;
import com.breadwallet.presenter.activities.MainActivity;
import com.breadwallet.presenter.entities.TransactionListItem;
import com.breadwallet.tools.CurrencyManager;
import com.breadwallet.tools.adapter.MiddleViewAdapter;

/**
 * BreadWallet
 * <p/>
 * Created by Mihail on 6/29/15.
 * Copyright (c) 2015 Mihail Gutan <mihail@breadwallet.com>
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

public class FragmentTransactionExpanded extends Fragment {
    private static final String TAG = FragmentTransactionExpanded.class.getName();
    private TextView hashText;
    private TextView statusText;
    private TextView amountText;
    private TextView exchangeText;
    private TextView fromText;
    private TextView fromDescription;
    private TextView toText;
    private TextView toDescription;
    private TextView toAmountText;
    private TextView toExchangeText;
    private TransactionListItem item;


    @Override
    public View onCreateView(LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        // The last two arguments ensure LayoutParams are inflated
        // properly.
        View rootView = null;
        if (item.getSent() == 0) {
            rootView = inflater.inflate(R.layout.transaction_item_expanded_received, container, false);
        } else {
            rootView = inflater.inflate(R.layout.transaction_item_expanded_sent, container, false);
        }

        hashText = (TextView) rootView.findViewById(R.id.tx_hash_text);
        statusText = (TextView) rootView.findViewById(R.id.tx_status_text);
        amountText = (TextView) rootView.findViewById(R.id.tx_amount_text);
        exchangeText = (TextView) rootView.findViewById(R.id.tx_exchange_text);
        fromText = (TextView) rootView.findViewById(R.id.tx_from_text);
        fromDescription = (TextView) rootView.findViewById(R.id.tx_from_description);
        toText = (TextView) rootView.findViewById(R.id.tx_to_text);
        toDescription = (TextView) rootView.findViewById(R.id.tx_to_description);
        toAmountText = (TextView) rootView.findViewById(R.id.tx_to_amount_text);
        toExchangeText = (TextView) rootView.findViewById(R.id.tx_to_exchange_text);

        if (item != null) {
            CurrencyManager m = CurrencyManager.getInstance(getActivity());
            SharedPreferences settings = getActivity().getSharedPreferences(MainActivity.PREFS_NAME, 0);
            final double rate = settings.getFloat(FragmentCurrency.RATE, 0);
            final String iso = settings.getString(FragmentCurrency.CURRENT_CURRENCY, "USD");
            long amount = item.getSent() == 0 ? item.getReceived() : item.getSent() - item.getReceived();

            hashText.setText(item.getHexId());
            statusText.setText(String.format("confirmed in block #%d\n%s", item.getBlockHeight(),
                    FragmentSettingsAll.getFormattedDateFromLong(item.getTimeStamp())));
            amountText.setText(m.getFormattedCurrencyString("BTC", String.valueOf(m.getBitsFromSatoshi(amount))));
            exchangeText.setText(String.format("(%s)", m.getExchangeForAmount(rate, iso, String.valueOf(amount))));
            fromText.setText(item.getFrom());
//            fromDescription.setText("spent address");//TODO ask Aaron what here
            toText.setText(item.getTo());
//            toDescription.setText("wallet address"); //TODO ask Aaron what here
            toAmountText.setText(m.getFormattedCurrencyString("BTC", String.valueOf(m.getBitsFromSatoshi(amount))));
            toExchangeText.setText(String.format("(%s)", m.getExchangeForAmount(rate, iso, String.valueOf(amount))));

        }

        return rootView;
    }

    @Override
    public void onResume() {
        super.onResume();
        Log.e(TAG, "In onResume");
        MiddleViewAdapter.resetMiddleView(null);
    }

    @Override
    public void onPause() {
        super.onPause();
        Log.e(TAG, "In onPause");
    }

    public void setCurrentObject(TransactionListItem item) {
        this.item = item;
    }


}
