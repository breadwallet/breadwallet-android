
package com.breadwallet.presenter.fragments;

import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.breadwallet.R;
import com.breadwallet.presenter.BreadWalletApp;
import com.breadwallet.presenter.activities.MainActivity;
import com.breadwallet.tools.adapter.CurrencyListAdapter;
import com.breadwallet.tools.others.CurrencyManager;


/**
 * BreadWallet
 * <p/>
 * Created by Mihail on 7/14/15.
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

public class FragmentCurrency extends Fragment {
    public static final String TAG = "FragmentCurrency";
    public static final String CURRENT_CURRENCY = "currentCurrency";
    public static final String POSITION = "position";
    private ListView currencyList;
    private MainActivity app;
    private Button currencyRefresh;
    private TextView noInternetConnection;
    private CurrencyListAdapter adapter;
    private TextView currencyItemText;
    private ProgressBar currencyProgressBar;
    SharedPreferences settings;

    @Override
    public View onCreateView(LayoutInflater inflater,
                             final ViewGroup container, Bundle savedInstanceState) {
        // The last two arguments ensure LayoutParams are inflated
        // properly.
        final View rootView = inflater.inflate(
                R.layout.fragment_local_currency, container, false);
        app = MainActivity.app;
        currencyList = (ListView) rootView.findViewById(R.id.currency_list_view);
        currencyList.setChoiceMode(ListView.CHOICE_MODE_SINGLE);
        currencyProgressBar = (ProgressBar) rootView.findViewById(R.id.currency_progress_barr);
        currencyRefresh = (Button) rootView.findViewById(R.id.currencyRefresh);
        noInternetConnection = (TextView) rootView.findViewById(R.id.noInternetConnectionText);
        settings = getActivity().getSharedPreferences(MainActivity.PREFS_NAME, 0);
        new ListInitiatorTask().execute();

        return rootView;
    }

    public void tryAndSetAdapter() {
        if (adapter.getCount() > 0) {
            currencyList.setAdapter(adapter);
            currencyRefresh.setVisibility(View.GONE);
            noInternetConnection.setVisibility(View.GONE);
            currencyProgressBar.setVisibility(View.GONE);
        } else {
            ((BreadWalletApp) app.getApplicationContext()).showCustomToast(getActivity(),
                    getResources().getString(R.string.no_internet_connection), 500, Toast.LENGTH_SHORT);
            currencyRefresh.setVisibility(View.VISIBLE);
            noInternetConnection.setVisibility(View.VISIBLE);
            currencyProgressBar.setVisibility(View.GONE);
        }
    }

    private class ListInitiatorTask extends AsyncTask {

        @Override
        protected Object doInBackground(Object[] params) {
            try {
                Thread.sleep(400);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            adapter = CurrencyManager.getCurrencyAddapterIfReady();
            return null;
        }

        @Override
        protected void onPostExecute(Object o) {
            super.onPostExecute(o);
            tryAndSetAdapter();
            currencyRefresh.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    adapter = CurrencyManager.getCurrencyAddapterIfReady();
                    tryAndSetAdapter();
                }
            });

            currencyList.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                @Override
                public void onItemClick(AdapterView<?> parent, View view,
                                        int position, long id) {
                    adapter.notifyDataSetChanged();
                    currencyItemText = (TextView) view.findViewById(R.id.currency_item_text);
                    final String selectedCurrency = currencyItemText.getText().toString();
                    SharedPreferences.Editor editor = settings.edit();
                    editor.putString(CURRENT_CURRENCY, selectedCurrency.substring(0, 3));
                    editor.putInt(POSITION, position);
                    editor.commit();
                }
            });
        }
    }
}
