package com.breadwallet.presenter.fragments;

import android.app.Activity;
import android.app.Fragment;
import android.content.Context;
import android.content.res.Resources;
import android.graphics.Color;
import android.os.Bundle;
import android.support.v4.content.ContextCompat;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.breadwallet.R;
import com.breadwallet.BreadWalletApp;
import com.breadwallet.presenter.activities.MainActivity;
import com.breadwallet.presenter.entities.TransactionListItem;
import com.breadwallet.tools.adapter.TransactionListAdapter;
import com.breadwallet.tools.animation.BRAnimator;
import com.breadwallet.tools.util.BRConstants;
import com.breadwallet.tools.util.BRStringFormatter;
import com.breadwallet.tools.manager.CurrencyManager;
import com.breadwallet.tools.util.CustomLogger;
import com.breadwallet.tools.manager.SharedPreferencesManager;
import com.breadwallet.tools.util.Utils;
import com.breadwallet.tools.adapter.MiddleViewAdapter;
import com.breadwallet.wallet.BRPeerManager;
import com.breadwallet.wallet.BRWalletManager;

import java.math.BigDecimal;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;

/**
 * BreadWallet
 * <p/>
 * Created by Mihail gutan on 8/4/15.
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

public class FragmentSettingsAll extends Fragment {
    private static final String TAG = FragmentSettingsAll.class.getName();
    public static TransactionListItem[] transactionObjects;
    public static ListView transactionList;
    public static LinearLayout transactionHistory;
    public static TextView noTransactions;


    //    private RelativeLayout relativeLayout;
    //RED FF5454 GREEN 00BF00 BLUE 0080FF
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        // The last two arguments ensure LayoutParams are inflated
        // properly.
        final View rootView = inflater.inflate(R.layout.fragment_settings_all, container, false);

        RelativeLayout importPrivateKeys = (RelativeLayout) rootView.findViewById(R.id.import_private_key);
        RelativeLayout settings = (RelativeLayout) rootView.findViewById(R.id.settings);


        noTransactions = (TextView) rootView.findViewById(R.id.text_no_transactions);
        transactionHistory = (LinearLayout) rootView.findViewById(R.id.layout_transaction_history);
        transactionList = (ListView) rootView.findViewById(R.id.transactions_list);

        transactionHistory.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (BRAnimator.checkTheMultipressingAvailability()) {
                    ((BreadWalletApp) getActivity().getApplicationContext()).
                            promptForAuthentication(getActivity(), BRConstants.AUTH_FOR_GENERAL, null, null, null, null);
                }
            }
        });
        refreshTransactions(getActivity());
        Button btnLoadMore = new Button(getActivity());
        btnLoadMore.setText(getString(R.string.more));

        // Adding Load More button to lisview at bottom
//        transactionList.addFooterView(btnLoadMore);

        // Getting adapter
        TransactionListAdapter adapter = new TransactionListAdapter(getActivity(), transactionObjects);
        transactionList.setAdapter(adapter);
        adapter.notifyDataSetChanged();
        if (transactionObjects != null) {
            if (transactionObjects.length == 0) transactionObjects = null;
        }
        importPrivateKeys.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (BRAnimator.checkTheMultipressingAvailability()) {
                    BRAnimator.animateDecoderFragment();
                }
            }
        });

        settings.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (BRAnimator.checkTheMultipressingAvailability()) {
                    MainActivity app = MainActivity.app;
                    if (app == null) return;
                    FragmentSettingsAll fragmentSettingsAll = (FragmentSettingsAll) getActivity().
                            getFragmentManager().findFragmentByTag(FragmentSettingsAll.class.getName());
                    BRAnimator.animateSlideToLeft(app, new FragmentSettings(), fragmentSettingsAll);
                }
            }
        });

        return rootView;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
    }

    @Override
    public void onResume() {
        super.onResume();
        MiddleViewAdapter.resetMiddleView(getActivity(), null);
        refreshUI(getActivity());
//        refreshTransactions(getActivity());
    }

    public static void refreshTransactions(final Activity ctx) {
        transactionObjects = BRWalletManager.getInstance(ctx).getTransactions();

        if (ctx != null && ctx instanceof MainActivity) {
            ctx.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    refreshUI(ctx);
                }
            });
        }


    }

    public static void refreshUI(Activity ctx) {
        if (BRAnimator.level != 1) return;
        Log.e(TAG, "refreshUI");
        if (transactionList == null || transactionHistory == null)
            return;
        if (transactionObjects == null) {
            noTransactions.setVisibility(View.VISIBLE);
            transactionHistory.setVisibility(View.GONE);
            transactionList.setVisibility(View.GONE);
            return;
        }

        noTransactions.setVisibility(View.GONE);
//        transactionList.removeAllViews();

        if (!BreadWalletApp.unlocked) {
            boolean addLine = false;
            int unconfirmedTxCount = getUnconfirmedCount(transactionObjects);
            if (unconfirmedTxCount == 0) {
                transactionList.setVisibility(View.GONE);
                transactionHistory.setVisibility(View.VISIBLE);
                return;
            }
            transactionHistory.setVisibility(View.GONE);
            transactionList.setVisibility(View.VISIBLE);
//            int estimatedBlockHeight = BRPeerManager.getEstimatedBlockHeight();
//            for (TransactionListItem transactionObject : transactionObjects) {
//                int blockHeight = transactionObject.getBlockHeight();
//                int confirms = blockHeight == Integer.MAX_VALUE ? 0 : estimatedBlockHeight - blockHeight + 1;
//                if (blockHeight != Integer.MAX_VALUE && confirms < 6) {
//                    if (addLine) {
//                        transactionList.addView(getSeparationLine(1, ctx));
//                    } else {
//                        addLine = true;
//                    }
//                    transactionList.addView(getViewFromTransactionObject(transactionObject));
//
//                }
//            }
//            if (unconfirmedTxCount != transactionObjects.length) {
//                transactionList.addView(getSeparationLine(1, ctx));
//                transactionList.addView(getMore(ctx, false));
//            }


        } else {
            transactionList.setVisibility(View.VISIBLE);
            transactionHistory.setVisibility(View.GONE);

//            int limit = transactionObjects.length > 5 ? 5 : transactionObjects.length;
//            Log.e(TAG, "transactionObjects.length: " + transactionObjects.length);
//
//            for (int i = 0; i < limit; i++) {
//                View tmpView = getViewFromTransactionObject(transactionObjects[i]);
//                if (tmpView != null) {
//                    transactionList.addView(tmpView);
//                    if (i != transactionObjects.length - 1)
//                        transactionList.addView(getSeparationLine(1, ctx));
//                }
//            }
//            if (transactionObjects.length > 5) {
//                transactionList.addView(getMore(ctx, true));
//            }
        }

    }

    private static int getUnconfirmedCount(TransactionListItem[] items) {
        int count = 0;
        int estimatedBlockHeight = BRPeerManager.getEstimatedBlockHeight();
        for (TransactionListItem t : items) {
            int blockHeight = t.getBlockHeight();
            int confirms = blockHeight == Integer.MAX_VALUE ? 0 : estimatedBlockHeight - blockHeight + 1;
            if (blockHeight != Integer.MAX_VALUE && confirms < 6) {
                count++;
            }
        }
        return count;
    }

//    public static LinearLayout getMore(final Activity context, final boolean auth) {
//        LayoutInflater inflater = (LayoutInflater) context.getApplicationContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
//        LinearLayout more = (LinearLayout) inflater.inflate(R.layout.transaction_list_item_more, null);
//        TextView moreText = (TextView) more.findViewById(R.id.more_text);
//        Utils.overrideFonts(moreText);
//        more.setBackgroundResource(R.drawable.clickable_layout);
//
//        more.setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View v) {
//                if (auth) {
//                    transactionList.removeView(v);
//                    for (int i = 5; i < transactionObjects.length; i++) {
//                        View tmpView = getViewFromTransactionObject(transactionObjects[i]);
//                        if (tmpView != null)
//                            transactionList.addView(tmpView);
//                        if (i != transactionObjects.length - 1)
//                            transactionList.addView(getSeparationLine(1, context));
//                    }
////                    transactionList.addView(getSeparationLine(0, context));
//                } else {
//                    if (BRAnimator.checkTheMultipressingAvailability()) {
//                        ((BreadWalletApp) context.getApplicationContext()).
//                                promptForAuthentication(context, BRConstants.AUTH_FOR_GENERAL, null, null, null, null);
//                    }
//                }
//            }
//        });
//        return more;
//    }

    public static RelativeLayout getSeparationLine(int MODE, Activity ctx) {
        //0 - regular , 1 - with left padding
        RelativeLayout line = new RelativeLayout(ctx);
        line.setMinimumHeight(1);
        line.setBackgroundColor(ctx.getColor(R.color.gray));
        if (MODE == 1)
            line.setPadding(40, 0, 0, 0);
        return line;
    }

//    public static View getViewFromTransactionObject(final TransactionListItem item) {
//        final MainActivity app = MainActivity.app;
//        if (app == null || item == null) return null;
//        CurrencyManager m = CurrencyManager.getInstance(app);
//        LayoutInflater inflater = (LayoutInflater) app.getApplicationContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
//        final LinearLayout tmpLayout = (LinearLayout) inflater.inflate(R.layout.transaction_list_item, null);
//        TextView sentReceivedTextView = (TextView) tmpLayout.findViewById(R.id.transaction_sent_received_label);
//        TextView dateTextView = (TextView) tmpLayout.findViewById(R.id.transaction_date);
//        TextView bitsTextView = (TextView) tmpLayout.findViewById(R.id.transaction_amount_bits);
//        TextView dollarsTextView = (TextView) tmpLayout.findViewById(R.id.transaction_amount_dollars);
//        TextView bitsTotalTextView = (TextView) tmpLayout.findViewById(R.id.transaction_amount_bits_total);
//        TextView dollarsTotalTextView = (TextView) tmpLayout.findViewById(R.id.transaction_amount_dollars_total);
//        Utils.overrideFonts(sentReceivedTextView, dateTextView, bitsTextView, dollarsTextView, bitsTotalTextView, dollarsTotalTextView);
//        tmpLayout.setBackgroundResource(R.drawable.clickable_layout);
//
//        tmpLayout.setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View v) {
////                Log.e(TAG, "clicked: " + ((TextView) tmpLayout.findViewById(R.id.transaction_date)).getText().toString());
//                if (BRAnimator.checkTheMultipressingAvailability()) {
//                    FragmentSettingsAll fragmentSettingsAll = (FragmentSettingsAll) app.
//                            getFragmentManager().findFragmentByTag(FragmentSettingsAll.class.getName());
//                    FragmentTransactionExpanded fragmentTransactionExpanded = new FragmentTransactionExpanded();
//                    fragmentTransactionExpanded.setCurrentObject(item);
//                    BRAnimator.animateSlideToLeft(app, fragmentTransactionExpanded, fragmentSettingsAll);
//                }
//            }
//        });
//
//        boolean received = item.getSent() == 0;
//        CustomLogger.logThis("TX getReceived", String.valueOf(item.getReceived()), "TX getSent", String.valueOf(item.getSent()),
//                "TX getBalanceAfterTx", String.valueOf(item.getBalanceAfterTx()));
//        int blockHeight = item.getBlockHeight();
//        int estimatedBlockHeight = BRPeerManager.getEstimatedBlockHeight();
//        int confirms = blockHeight == Integer.MAX_VALUE ? 0 : estimatedBlockHeight - blockHeight + 1;
//        Log.e(TAG, "confirms: " + confirms);
//
//        if (item.getSent() > 0 && item.getSent() == item.getReceived()) {
//            sentReceivedTextView.setBackgroundResource(R.drawable.unconfirmed_label);
//            sentReceivedTextView.setText(R.string.moved);
//            sentReceivedTextView.setTextColor(unconfirmedColor);
//        } else if (blockHeight != Integer.MAX_VALUE && confirms >= 6) {
//            sentReceivedTextView.setBackgroundResource(received ? R.drawable.received_label : R.drawable.sent_label);
//            sentReceivedTextView.setText(received ? R.string.received : R.string.sent);
//            sentReceivedTextView.setTextColor(received ? receivedColor : sentColor);
//        } else {
//            sentReceivedTextView.setBackgroundResource(R.drawable.unconfirmed_label);
//            sentReceivedTextView.setTextColor(unconfirmedColor);
//            if (!BRWalletManager.getInstance(app).transactionIsVerified(item.getHexId())) {
//                sentReceivedTextView.setText(R.string.unverified);
//            } else {
//                Log.e(TAG, "item.getBlockHeight(): " + blockHeight + ", confirms: " + confirms + ", lastBlock: " + estimatedBlockHeight);
//                Resources res = app.getResources();
//                int confsNr = confirms >= 0 && confirms <= 5 ? confirms : 0;
//                String confs = res.getQuantityString(R.plurals.nr_confirmations, confsNr);
//                String message = String.format(confs, confsNr);
//                sentReceivedTextView.setText(message);
//            }
//        }
//
//        long itemTimeStamp = item.getTimeStamp();
//        Log.e(TAG, "item.getTimeStamp(): " + itemTimeStamp);
//        dateTextView.setText(itemTimeStamp != 0 ? getFormattedDateFromLong(itemTimeStamp * 1000) : getFormattedDateFromLong(System.currentTimeMillis()));
//
//        long satoshisAmount = received ? item.getReceived() : (item.getSent() - item.getReceived()) * -1;
//
//        bitsTextView.setText(BRStringFormatter.getFormattedCurrencyString("BTC", satoshisAmount));
//        dollarsTextView.setText(String.format("(%s)", BRStringFormatter.getExchangeForAmount(SharedPreferencesManager.getRate(app), SharedPreferencesManager.getIso(app), new BigDecimal(satoshisAmount), app)));
//        long satoshisAfterTx = item.getBalanceAfterTx();
//
//        bitsTotalTextView.setText(BRStringFormatter.getFormattedCurrencyString("BTC", satoshisAfterTx));
//        dollarsTotalTextView.setText(String.format("(%s)", BRStringFormatter.getExchangeForAmount(SharedPreferencesManager.getRate(app), SharedPreferencesManager.getIso(app), new BigDecimal(satoshisAfterTx), app)));
//
//        return tmpLayout;
//    }

}
