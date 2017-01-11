package com.breadwallet.tools.adapter;

import android.app.ActionBar;
import android.app.Activity;
import android.content.Context;
import android.graphics.Color;
import android.os.AsyncTask;
import android.support.v4.content.ContextCompat;
import android.text.Layout;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.breadwallet.BreadWalletApp;
import com.breadwallet.R;
import com.breadwallet.presenter.activities.MainActivity;
import com.breadwallet.presenter.entities.TransactionListItem;
import com.breadwallet.presenter.fragments.FragmentSettings;
import com.breadwallet.presenter.fragments.FragmentSettingsAll;
import com.breadwallet.presenter.fragments.FragmentTransactionExpanded;
import com.breadwallet.presenter.fragments.FragmentWebView;
import com.breadwallet.tools.animation.BRAnimator;
import com.breadwallet.tools.manager.SharedPreferencesManager;
import com.breadwallet.tools.util.BRConstants;
import com.breadwallet.tools.util.BRStringFormatter;
import com.breadwallet.tools.util.Utils;
import com.breadwallet.wallet.BRWalletManager;
import com.platform.APIClient;


import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;


/**
 * BreadWallet
 * <p>
 * Created by Mihail Gutan <mihail@breadwallet.com> on 7/7/16.
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

public class TransactionListAdapter extends BaseAdapter {
    public static final String TAG = TransactionListAdapter.class.getName();

    private Activity activity;
    private ArrayList<TransactionListItem> data;
    private static LayoutInflater inflater = null;
    private static int unconfirmedColor;
    private static int sentColor;
    private static int receivedColor;
    public static boolean showAllTx = false;
    public boolean buyBitcoinEnabled = false;
//    private List<Layout> extraItems = new LinkedList<>();

    public TransactionListAdapter(Activity a, TransactionListItem[] d) {
        activity = a;
        data = new ArrayList<>();
        if (d != null)
            Collections.addAll(data, d);
        inflater = (LayoutInflater) activity.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        unconfirmedColor = ContextCompat.getColor(a, R.color.white);
        sentColor = Color.parseColor("#FF5454");
        receivedColor = Color.parseColor("#00BF00");
        buyBitcoinEnabled = APIClient.getInstance(a).isFeatureEnabled(APIClient.FeatureFlags.BUY_BITCOIN.toString());
        buyBitcoinEnabled = BRConstants.PLATFORM_ON; //todo delete
    }

    public void updateData(TransactionListItem[] d) {
        if (d != null) {
            if (d.length != data.size()) {
                data.clear();
                Collections.addAll(data, d);
            }
        }
        showAllTx = false;
        notifyDataSetChanged();
    }

    @Override
    public int getCount() {
        final int EXTRA_ITEMS = buyBitcoinEnabled ? 5 : 4;
        if (!BreadWalletApp.unlocked) {
            return getUnconfirmedCount(data) == 0 ? (EXTRA_ITEMS + 1) : getUnconfirmedCount(data) == data.size()
                    ? (getUnconfirmedCount(data) + EXTRA_ITEMS) : (getUnconfirmedCount(data) + EXTRA_ITEMS + 1);
        }
        if (data.size() == 0) return EXTRA_ITEMS + 1;
        return showAllTx ? (data.size() + EXTRA_ITEMS) : (data.size() > 5) ? (6 + EXTRA_ITEMS) : (data.size() + EXTRA_ITEMS);
    }

    @Override
    public Object getItem(int position) {
        return data.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup viewGroup) {
        View tmpLayout = inflater.inflate(R.layout.transaction_list_item, null);
        if (data.size() == 0 && position == 0) {
            RelativeLayout noTransactions = (RelativeLayout) inflater.inflate(R.layout.button_no_transactions, null);
            noTransactions.setLayoutParams(new ActionBar.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, Utils.getPixelsFromDps(activity, 70)));
            return noTransactions;
        } else if (position == getCount() - (buyBitcoinEnabled ? 5 : 4)) {
            View separator = new View(activity);
            separator.setLayoutParams(new ActionBar.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, Utils.getPixelsFromDps(activity, 30)));
            separator.setBackgroundResource(android.R.color.transparent);
            return separator;
        } else if (buyBitcoinEnabled && position == getCount() - 4) {
            RelativeLayout buyBitcoinLayout = (RelativeLayout) inflater.inflate(R.layout.button_buy_bitcoin, null);
            buyBitcoinLayout.setLayoutParams(new ActionBar.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, Utils.getPixelsFromDps(activity, 50)));
            buyBitcoinLayout.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    if (BRAnimator.checkTheMultipressingAvailability()) {
                        FragmentWebView fragmentWebView = new FragmentWebView();
                        fragmentWebView.setMode(1);
                        BRAnimator.animateSlideToLeft((MainActivity) activity, fragmentWebView, FragmentSettingsAll.instantiate(activity, FragmentSettingsAll.class.getName()));
                    }
                }
            });
            return buyBitcoinLayout;
        } else if (position == getCount() - 3) {
            RelativeLayout importPrivateKeys = (RelativeLayout) inflater.inflate(R.layout.button_import_privkey, null);
            importPrivateKeys.setLayoutParams(new ActionBar.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, Utils.getPixelsFromDps(activity, 50)));
            importPrivateKeys.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    if (BRAnimator.checkTheMultipressingAvailability()) {
                        BRAnimator.animateDecoderFragment();
                    }
                }
            });
            return importPrivateKeys;

        } else if (position == getCount() - 2) {
            RelativeLayout settings = (RelativeLayout) inflater.inflate(R.layout.button_settings, null);
            settings.setLayoutParams(new ActionBar.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, Utils.getPixelsFromDps(activity, 50)));
            settings.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (BRAnimator.checkTheMultipressingAvailability()) {
                        MainActivity app = MainActivity.app;
                        if (app == null) return;
                        FragmentSettingsAll fragmentSettingsAll = (FragmentSettingsAll) activity.
                                getFragmentManager().findFragmentByTag(FragmentSettingsAll.class.getName());
                        BRAnimator.animateSlideToLeft(app, new FragmentSettings(), fragmentSettingsAll);
                    }
                }
            });
            return settings;
        } else if (position == getCount() - 1) {
            View separator = new View(activity);
            separator.setLayoutParams(new ActionBar.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, Utils.getPixelsFromDps(activity, 30)));
            separator.setBackgroundResource(android.R.color.transparent);
            separator.setClickable(false);
            return separator;
        }

        if (!BreadWalletApp.unlocked) {
            if (getUnconfirmedCount(data) == 0 && position == 0) {
                RelativeLayout txHistory = (RelativeLayout) inflater.inflate(R.layout.button_transaction_history, null);
                txHistory.setLayoutParams(new RelativeLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, Utils.getPixelsFromDps(activity, 40)));
                txHistory.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        if (BRAnimator.checkTheMultipressingAvailability()) {
                            ((BreadWalletApp) activity.getApplicationContext()).
                                    promptForAuthentication(activity, BRConstants.AUTH_FOR_GENERAL, null, null, null, null, false);
                        }
                    }
                });
                return txHistory;
            } else {
                if (position == getUnconfirmedCount(data)) {
                    RelativeLayout moreAuth = (RelativeLayout) inflater.inflate(R.layout.button_more, null);
                    moreAuth.setLayoutParams(new RelativeLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, Utils.getPixelsFromDps(activity, 40)));
                    moreAuth.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            if (BRAnimator.checkTheMultipressingAvailability()) {
                                ((BreadWalletApp) activity.getApplicationContext()).
                                        promptForAuthentication(activity, BRConstants.AUTH_FOR_GENERAL, null, null, null, null, false);
                            }
                        }
                    });
                    return moreAuth;
                } else {
                    return getTxView(tmpLayout, position);
                }
            }
        } else {
            if (showAllTx) return getTxView(tmpLayout, position);
            if (data.size() > 5) {
                if (position < 5) {
                    return getTxView(tmpLayout, position);
                } else {
                    RelativeLayout more = (RelativeLayout) inflater.inflate(R.layout.button_more, null);
                    more.setLayoutParams(new RelativeLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, Utils.getPixelsFromDps(activity, 40)));
                    more.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            showAllTx = true;
                            notifyDataSetChanged();
                        }
                    });
                    return more;
                }
            } else {
                return getTxView(tmpLayout, position);
            }
        }
    }

    public int getUnconfirmedCount(List<TransactionListItem> items) {
        int count = 0;
        for (TransactionListItem t : items) {
            if (t == null) continue;
            int blockHeight = t.getBlockHeight();
            int confirms = blockHeight == Integer.MAX_VALUE ? 0 : SharedPreferencesManager.getLastBlockHeight(activity) - blockHeight + 1;
            if (blockHeight != Integer.MAX_VALUE && confirms < 6) {
                count++;
            }
        }
        return count;
    }

    @Override
    public int getViewTypeCount() {
        return 2;
    }

    public View getTxView(View tmpLayout, int position) {
        TextView sentReceivedTextView = (TextView) tmpLayout.findViewById(R.id.transaction_sent_received_label);
        TextView dateTextView = (TextView) tmpLayout.findViewById(R.id.transaction_date);
        TextView bitsTextView = (TextView) tmpLayout.findViewById(R.id.transaction_amount_bits);
        TextView dollarsTextView = (TextView) tmpLayout.findViewById(R.id.transaction_amount_dollars);
        TextView bitsTotalTextView = (TextView) tmpLayout.findViewById(R.id.transaction_amount_bits_total);
        TextView dollarsTotalTextView = (TextView) tmpLayout.findViewById(R.id.transaction_amount_dollars_total);
        Utils.overrideFonts(sentReceivedTextView, dateTextView, bitsTextView, dollarsTextView, bitsTotalTextView, dollarsTotalTextView);
        tmpLayout.setBackgroundResource(R.drawable.clickable_layout);

        bitsTextView.setVisibility(BreadWalletApp.unlocked ? View.VISIBLE : View.INVISIBLE);
        bitsTotalTextView.setVisibility(BreadWalletApp.unlocked ? View.VISIBLE : View.INVISIBLE);
        dollarsTextView.setVisibility(BreadWalletApp.unlocked ? View.VISIBLE : View.INVISIBLE);
        dollarsTotalTextView.setVisibility(BreadWalletApp.unlocked ? View.VISIBLE : View.INVISIBLE);

        final TransactionListItem item = data.get(position);

        tmpLayout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (BRAnimator.checkTheMultipressingAvailability()) {
                    FragmentSettingsAll fragmentSettingsAll = (FragmentSettingsAll) activity.
                            getFragmentManager().findFragmentByTag(FragmentSettingsAll.class.getName());
                    FragmentTransactionExpanded fragmentTransactionExpanded = new FragmentTransactionExpanded();
                    fragmentTransactionExpanded.setCurrentObject(item);
                    BRAnimator.animateSlideToLeft((MainActivity) activity, fragmentTransactionExpanded, fragmentSettingsAll);
                }
            }
        });

        boolean received = item.getSent() == 0;
        int blockHeight = item.getBlockHeight();

        int confirms = blockHeight == Integer.MAX_VALUE ? 0 : SharedPreferencesManager.getLastBlockHeight(activity) - blockHeight + 1;

        if (item.getSent() > 0 && item.getSent() == item.getReceived()) {
            sentReceivedTextView.setBackgroundResource(R.drawable.unconfirmed_label);
            sentReceivedTextView.setText(R.string.moved);
            sentReceivedTextView.setTextColor(unconfirmedColor);
        } else if (blockHeight != Integer.MAX_VALUE && confirms >= 6) {
            sentReceivedTextView.setBackgroundResource(received ? R.drawable.received_label : R.drawable.sent_label);
            sentReceivedTextView.setText(received ? R.string.received : R.string.sent);
            sentReceivedTextView.setTextColor(received ? receivedColor : sentColor);
        } else {
            sentReceivedTextView.setBackgroundResource(R.drawable.unconfirmed_label);
            sentReceivedTextView.setTextColor(unconfirmedColor);
            if (!BRWalletManager.getInstance(activity).transactionIsVerified(item.getHexId())) {
                sentReceivedTextView.setText(R.string.unverified);
            } else {
                int confsNr = confirms >= 0 && confirms <= 5 ? confirms : 0;
                String message = confsNr == 0 ? activity.getString(R.string.nr_confirmations0) :
                        (confsNr == 1 ? activity.getString(R.string.nr_confirmations1) : String.format(activity.getString(R.string.nr_confirmations), confsNr));

                sentReceivedTextView.setText(message);
            }
        }

        long itemTimeStamp = item.getTimeStamp();
        dateTextView.setText(itemTimeStamp != 0 ? Utils.getFormattedDateFromLong(itemTimeStamp * 1000) : Utils.getFormattedDateFromLong(System.currentTimeMillis()));

        long satoshisAmount = received ? item.getReceived() : (item.getSent() - item.getReceived()) * -1;

        bitsTextView.setText(BRStringFormatter.getFormattedCurrencyString("BTC", satoshisAmount));
        dollarsTextView.setText(String.format("(%s)", BRStringFormatter.getExchangeForAmount(SharedPreferencesManager.getRate(activity),
                SharedPreferencesManager.getIso(activity), new BigDecimal(satoshisAmount), activity)));
        long satoshisAfterTx = item.getBalanceAfterTx();

        bitsTotalTextView.setText(BRStringFormatter.getFormattedCurrencyString("BTC", satoshisAfterTx));
        dollarsTotalTextView.setText(String.format("(%s)", BRStringFormatter.getExchangeForAmount(SharedPreferencesManager.getRate(activity),
                SharedPreferencesManager.getIso(activity), new BigDecimal(satoshisAfterTx), activity)));
        return tmpLayout;
    }
}