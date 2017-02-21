
package com.breadwallet.presenter.fragments;

import android.app.Activity;
import android.app.Fragment;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.GestureDetector;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.breadwallet.BreadWalletApp;
import com.breadwallet.R;
import com.breadwallet.presenter.activities.MainActivity;
import com.breadwallet.presenter.entities.CurrencyEntity;
import com.breadwallet.tools.util.BRConstants;
import com.breadwallet.tools.util.BRStringFormatter;
import com.breadwallet.tools.manager.CurrencyManager;
import com.breadwallet.tools.manager.SharedPreferencesManager;
import com.breadwallet.tools.adapter.CurrencyListAdapter;
import com.breadwallet.tools.adapter.MiddleViewAdapter;
import com.breadwallet.tools.animation.SpringAnimator;
import com.google.firebase.crash.FirebaseCrash;

/**
 * BreadWallet
 * <p>
 * Created by Mihail Gutan <mihail@breadwallet.com> on 7/14/15.
 * Copyright (c) 2016 breadwallet LLC
 * <p>
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 * <p>
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 * <p>
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
    private Button currencyRefresh;
    private TextView noInternetConnection;
    private CurrencyListAdapter adapter;
    private TextView currencyItemText;
    private ProgressBar currencyProgressBar;
    private String ISO;
    private float rate;
    public static int lastItemsPosition = 0;

    @SuppressWarnings("deprecation")
    final GestureDetector gdt = new GestureDetector(new GestureListener());

    @Override
    public View onCreateView(LayoutInflater inflater,
                             final ViewGroup container, Bundle savedInstanceState) {
        // The last two arguments ensure LayoutParams are inflated
        // properly.
        final View rootView = inflater.inflate(
                R.layout.fragment_currency, container, false);
        currencyList = (ListView) rootView.findViewById(R.id.currency_list_view);
        currencyProgressBar = (ProgressBar) rootView.findViewById(R.id.currency_progress_barr);
        currencyRefresh = (Button) rootView.findViewById(R.id.currencyRefresh);
        noInternetConnection = (TextView) rootView.findViewById(R.id.noInternetConnectionText);
        currencyList.setChoiceMode(ListView.CHOICE_MODE_SINGLE);
        currencyRefresh.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                SpringAnimator.showAnimation(v);
                tryAndSetAdapter();
            }
        });
        currencyList.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view,
                                    int position, long id) {
                Activity app = getActivity();
                if (app == null) return;
                currencyItemText = (TextView) view.findViewById(R.id.currency_item_text);
                final String selectedCurrency = currencyItemText.getText().toString();
                ISO = selectedCurrency.substring(0, 3);
                lastItemsPosition = position;
                CurrencyEntity item = adapter.getItem(position);
                rate = item == null ? 0 : item.rate;
                SharedPreferencesManager.putIso(app, ISO);
                SharedPreferencesManager.putCurrencyListPosition(app, lastItemsPosition);
                SharedPreferencesManager.putRate(app, rate);
                String finalExchangeRate = BRStringFormatter.getMiddleTextExchangeString(rate, ISO, app);

                MiddleViewAdapter.resetMiddleView(app, finalExchangeRate);
                adapter.notifyDataSetChanged();

            }

        });
        return rootView;
    }

    @Override
    public void onResume() {
        super.onResume();
        setMiddleExchangeRate();
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                tryAndSetAdapter();
            }
        }, 500);
        Activity app = getActivity();
        if (app != null)
            allowChangeDisplayUnits((MainActivity) app, true);
    }

    private void setMiddleExchangeRate() {
        Activity app = getActivity();
        if (app == null) return;
        final String iso = SharedPreferencesManager.getIso(getActivity());
        float tmpRate;
        CurrencyEntity item = (adapter != null && !adapter.isEmpty()) ? adapter.getItem(SharedPreferencesManager.getCurrencyListPosition(app)) : null;
        tmpRate = item == null ? SharedPreferencesManager.getRate(app) : item.rate;
        String readyText = BRStringFormatter.getMiddleTextExchangeString(tmpRate, iso, app);
        MiddleViewAdapter.resetMiddleView(app, readyText);
    }

    @Override
    public void onPause() {
        super.onPause();
        Activity app = getActivity();
        if (app != null)
            allowChangeDisplayUnits((MainActivity) app, false);
    }

    private void allowChangeDisplayUnits(MainActivity app, boolean allow) {
        try {
            if (allow)
                app.viewFlipper.setOnTouchListener(new View.OnTouchListener() {
                    @Override
                    public boolean onTouch(final View view, final MotionEvent event) {
                        gdt.onTouchEvent(event);
                        return true;
                    }
                });
            else
                app.viewFlipper.setOnTouchListener(new View.OnTouchListener() {
                    @Override
                    public boolean onTouch(final View view, final MotionEvent event) {
                        return false;
                    }
                });
        } catch (Exception e) {
            FirebaseCrash.report(new RuntimeException("allowChangeDisplayUnits: " + e.getMessage()));
            e.printStackTrace();
        }
    }

    //0 - left, 1 - right
    private void changeUnitWithDirection() {
        MainActivity app = MainActivity.app;
        if (app == null) return;
        int unit = SharedPreferencesManager.getCurrencyUnit(app);
        switch (unit) {
            case BRConstants.CURRENT_UNIT_BITS:
                SharedPreferencesManager.putCurrencyUnit(app, BRConstants.CURRENT_UNIT_MBITS);
                break;
            case BRConstants.CURRENT_UNIT_MBITS:
                SharedPreferencesManager.putCurrencyUnit(app, BRConstants.CURRENT_UNIT_BITCOINS);
                break;
            case BRConstants.CURRENT_UNIT_BITCOINS:
                SharedPreferencesManager.putCurrencyUnit(app, BRConstants.CURRENT_UNIT_BITS);
                break;
        }

    }

    private static final int SWIPE_MIN_DISTANCE = 120;
    private static final int SWIPE_THRESHOLD_VELOCITY = 200;

    private class GestureListener extends GestureDetector.SimpleOnGestureListener {
        @Override
        public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
            if (e1.getX() - e2.getX() > SWIPE_MIN_DISTANCE && Math.abs(velocityX) > SWIPE_THRESHOLD_VELOCITY) {
                changeUnitWithDirection();
                setMiddleExchangeRate();
                return false; // Right to left
            }
            return false;
        }
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
