
package com.breadwallet.presenter.fragments;

import android.app.Fragment;
import android.os.Bundle;
import android.os.Handler;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.breadwallet.R;
import com.breadwallet.presenter.activities.MainActivity;
import com.breadwallet.tools.util.BRStringFormatter;
import com.breadwallet.tools.manager.CurrencyManager;
import com.breadwallet.tools.manager.SharedPreferencesManager;
import com.breadwallet.tools.adapter.CurrencyListAdapter;
import com.breadwallet.tools.adapter.MiddleViewAdapter;
import com.breadwallet.tools.animation.SpringAnimator;


/**
 * BreadWallet
 * <p/>
 * Created by Mihail Gutan on 7/14/15.
 * Copyright (c) 2016 breadwallet llc <mihail@breadwallet.com>
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

public class FragmentCurrency extends Fragment {
    private static final String TAG = FragmentCurrency.class.getName();


    private ListView currencyList;
    private MainActivity app;
    private Button currencyRefresh;
    private TextView noInternetConnection;
    private CurrencyListAdapter adapter;
    private TextView currencyItemText;
    private ProgressBar currencyProgressBar;
    private String ISO;
    private float rate;
    public static int lastItemsPosition = 0;

    @Override
    public View onCreateView(LayoutInflater inflater,
                             final ViewGroup container, Bundle savedInstanceState) {
        // The last two arguments ensure LayoutParams are inflated
        // properly.
        final View rootView = inflater.inflate(
                R.layout.fragment_currency, container, false);

        app = MainActivity.app;
        currencyList = (ListView) rootView.findViewById(R.id.currency_list_view);
        currencyProgressBar = (ProgressBar) rootView.findViewById(R.id.currency_progress_barr);
        currencyRefresh = (Button) rootView.findViewById(R.id.currencyRefresh);
        noInternetConnection = (TextView) rootView.findViewById(R.id.noInternetConnectionText);
        currencyList.setChoiceMode(ListView.CHOICE_MODE_SINGLE);
        currencyRefresh.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                tryAndSetAdapter();
                SpringAnimator.showAnimation(v);
            }
        });
        currencyList.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view,
                                    int position, long id) {
                currencyItemText = (TextView) view.findViewById(R.id.currency_item_text);
                final String selectedCurrency = currencyItemText.getText().toString();
                ISO = selectedCurrency.substring(0, 3);
                lastItemsPosition = position;
                rate = adapter.getItem(position).rate;
                SharedPreferencesManager.putIso(getActivity(), ISO);
                SharedPreferencesManager.putCurrencyListPosition(getActivity(), lastItemsPosition);
                SharedPreferencesManager.putRate(getActivity(), rate);
                String finalExchangeRate = BRStringFormatter.getMiddleTextExchangeString(rate, ISO, getActivity());

                MiddleViewAdapter.resetMiddleView(getActivity(), finalExchangeRate);
                adapter.notifyDataSetChanged();

            }

        });
        return rootView;
    }

    @Override
    public void onResume() {
        super.onResume();
        final String iso = SharedPreferencesManager.getIso(getActivity());
        float tmpRate;
        tmpRate = (adapter != null && !adapter.isEmpty()) ?
                adapter.getItem(SharedPreferencesManager.getCurrencyListPosition(getActivity())).rate
                : SharedPreferencesManager.getRate(getActivity());
        String readyText = BRStringFormatter.getMiddleTextExchangeString(tmpRate, iso, getActivity());
        MiddleViewAdapter.resetMiddleView(getActivity(), readyText);
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                tryAndSetAdapter();
            }
        }, 500);
    }

    @Override
    public void onPause() {
        super.onPause();
    }

    private void tryAndSetAdapter() {
        adapter = CurrencyManager.getInstance(getActivity()).getCurrencyAdapterIfReady();
        if (adapter.getCount() > 0) {
            currencyList.setAdapter(adapter);
            currencyRefresh.clearAnimation();
            currencyRefresh.setVisibility(View.GONE);
            noInternetConnection.setVisibility(View.GONE);
            currencyProgressBar.setVisibility(View.GONE);
        } else {
            currencyRefresh.setVisibility(View.VISIBLE);
            noInternetConnection.setVisibility(View.VISIBLE);
            currencyProgressBar.setVisibility(View.GONE);
        }
    }
}
