package com.breadwallet.presenter.fragments;

import android.annotation.SuppressLint;
import android.app.Fragment;
import android.content.Context;
import android.graphics.Color;
import android.os.Bundle;
import android.support.v4.content.ContextCompat;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.breadwallet.R;
import com.breadwallet.presenter.BreadWalletApp;
import com.breadwallet.presenter.activities.MainActivity;
import com.breadwallet.presenter.entities.TransactionListItem;
import com.breadwallet.tools.BRConstants;
import com.breadwallet.tools.CurrencyManager;
import com.breadwallet.tools.CustomLogger;
import com.breadwallet.tools.adapter.MiddleViewAdapter;
import com.breadwallet.tools.animation.FragmentAnimator;
import com.breadwallet.wallet.BRPeerManager;
import com.breadwallet.wallet.BRWalletManager;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/**
 * BreadWallet
 * <p/>
 * Created by Mihail on 8/4/15.
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

public class FragmentSettingsAll extends Fragment {
    private static final String TAG = FragmentSettingsAll.class.getName();
    private static TransactionListItem[] transactionObjects;
    public static LinearLayout transactionList;
    public static LinearLayout transactionHistory;
    public static TextView noTransactions;
    private static int unconfirmedColor;
    private static int sentColor;
    private static int receivedColor;

    //    private RelativeLayout relativeLayout;
    //RED FF5454 GREEN 00BF00 BLUE 0080FF
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        // The last two arguments ensure LayoutParams are inflated
        // properly.
        final View rootView = inflater.inflate(R.layout.fragment_settings_all, container, false);

        RelativeLayout importPrivateKeys = (RelativeLayout) rootView.findViewById(R.id.import_private_key);
        RelativeLayout settings = (RelativeLayout) rootView.findViewById(R.id.settings);
        RelativeLayout rescan = (RelativeLayout) rootView.findViewById(R.id.rescan_blockchain);
        noTransactions = (TextView) rootView.findViewById(R.id.text_no_transactions);
        transactionHistory = (LinearLayout) rootView.findViewById(R.id.layout_transaction_history);
        transactionList = (LinearLayout) rootView.findViewById(R.id.transactions_list);

        unconfirmedColor = ContextCompat.getColor(getActivity(), R.color.white);
        sentColor = Color.parseColor("#FF5454");
        receivedColor = Color.parseColor("#00BF00");

        transactionHistory.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ((BreadWalletApp) getActivity().getApplicationContext()).
                        promptForAuthentication(getActivity(), BRConstants.AUTH_FOR_GENERAL, null);
            }
        });
        refreshTransactions(getActivity());
        if (transactionObjects != null) {
            if (transactionObjects.length == 0) transactionObjects = null;
        }
        importPrivateKeys.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (FragmentAnimator.checkTheMultipressingAvailability()) {
                    FragmentAnimator.animateDecoderFragment();
                }
            }
        });
        rescan.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (FragmentAnimator.checkTheMultipressingAvailability()) {
                    //TODO broken, fix then use!
                    ((BreadWalletApp) getActivity().getApplication()).showCustomToast(getActivity(), "MIKE, STOP WORKING, GO GRAB SOME FUN", MainActivity.screenParametersPoint.y / 2, Toast.LENGTH_LONG, 0);
//                    BRWalletManager.getInstance(getActivity()).rescan();
                }
            }
        });
        settings.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (FragmentAnimator.checkTheMultipressingAvailability()) {
                    MainActivity app = MainActivity.app;
                    if (app == null) return;
                    FragmentSettingsAll fragmentSettingsAll = (FragmentSettingsAll) getActivity().
                            getFragmentManager().findFragmentByTag(FragmentSettingsAll.class.getName());
                    FragmentAnimator.animateSlideToLeft(app, new FragmentSettings(), fragmentSettingsAll);
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
//        refreshTransactions(getActivity());
    }

    public static void refreshTransactions(Context ctx) {
        transactionObjects = BRWalletManager.getInstance(ctx).getTransactions();

//        transactionObjects = getTestTransactions(); //TODO this is a test
//        transactionObjects = new TransactionListItem[0];
//            transactionObjects = getTestTransactions();
//        Log.e(TAG, "REFRESH TRANSACTIONS: " + transactionObjects.length);
        if (ctx != null && ctx instanceof MainActivity)
            refreshUI(ctx);
    }

    public static void refreshUI(Context ctx) {
        if (transactionList == null || transactionHistory == null)
            return;
        if (!BreadWalletApp.unlocked) {
            transactionList.setVisibility(View.GONE);
            transactionHistory.setVisibility(View.VISIBLE);
//            Log.e(TAG, "inside, NO auth");
        } else {
            transactionList.setVisibility(View.VISIBLE);
            transactionHistory.setVisibility(View.GONE);
//            Log.e(TAG, "inside, YES auth");
        }
        if (transactionObjects == null) {
            if (BreadWalletApp.unlocked) {
                noTransactions.setVisibility(View.VISIBLE);
            } else {
                noTransactions.setVisibility(View.GONE);
            }
            return;
        }
        noTransactions.setVisibility(View.GONE);
        transactionList.removeAllViews();

        transactionList.addView(getSeparationLine(0, ctx));

        int limit = transactionObjects.length > 5 ? 5 : transactionObjects.length;
        Log.e(TAG, "transactionObjects.length: " + transactionObjects.length);

        for (int i = 0; i < limit; i++) {
            View tmpView = getViewFromTransactionObject(transactionObjects[i]);
            if (tmpView != null) {
                transactionList.addView(tmpView);
                if (i != transactionObjects.length - 1)
                    transactionList.addView(getSeparationLine(1, ctx));
            }
        }
        if (transactionObjects.length > 5) {
            transactionList.addView(getSeparationLine(0, ctx));
            transactionList.addView(getMore(ctx));
        }
//        transactionList.addView(getSeparationLine(0, ctx));

    }

    public static LinearLayout getMore(final Context context) {
        LayoutInflater inflater = (LayoutInflater) context.getApplicationContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        LinearLayout more = (LinearLayout) inflater.inflate(R.layout.transaction_list_item_more, null);
        more.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                transactionList.removeView(v);
                for (int i = 5; i < transactionObjects.length; i++) {
                    View tmpView = getViewFromTransactionObject(transactionObjects[i]);
                    if (tmpView != null)
                        transactionList.addView(tmpView);
                    if (i != transactionObjects.length - 1)
                        transactionList.addView(getSeparationLine(1, context));
                }
                transactionList.addView(getSeparationLine(0, context));
            }
        });
        return more;
    }

    public static RelativeLayout getSeparationLine(int MODE, Context ctx) {
        //0 - regular , 1 - with left padding
        RelativeLayout line = new RelativeLayout(ctx);
        line.setMinimumHeight(1);
        line.setBackgroundColor(R.color.grey);
        if (MODE == 1)
            line.setPadding(40, 0, 0, 0);
        return line;
    }

    public static View getViewFromTransactionObject(final TransactionListItem item) {
        //TODO FIX THE BUG ,,does not go more then 10 000 000 bits"
        final MainActivity app = MainActivity.app;
        if (app == null || item == null) return null;
        CurrencyManager m = CurrencyManager.getInstance(app);
        LayoutInflater inflater = (LayoutInflater) app.getApplicationContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        final LinearLayout tmpLayout = (LinearLayout) inflater.inflate(R.layout.transaction_list_item, null);
        TextView sentReceivedTextView = (TextView) tmpLayout.findViewById(R.id.transaction_sent_received_label);
        TextView dateTextView = (TextView) tmpLayout.findViewById(R.id.transaction_date);
        TextView bitsTextView = (TextView) tmpLayout.findViewById(R.id.transaction_amount_bits);
        TextView dollarsTextView = (TextView) tmpLayout.findViewById(R.id.transaction_amount_dollars);
        TextView bitsTotalTextView = (TextView) tmpLayout.findViewById(R.id.transaction_amount_bits_total);
        TextView dollarsTotalTextView = (TextView) tmpLayout.findViewById(R.id.transaction_amount_dollars_total);
        tmpLayout.setBackgroundResource(R.drawable.clickable_layout);

        tmpLayout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
//                Log.e(TAG, "clicked: " + ((TextView) tmpLayout.findViewById(R.id.transaction_date)).getText().toString());
                if (FragmentAnimator.checkTheMultipressingAvailability()) {
                    FragmentSettingsAll fragmentSettingsAll = (FragmentSettingsAll) app.
                            getFragmentManager().findFragmentByTag(FragmentSettingsAll.class.getName());
                    FragmentTransactionExpanded fragmentTransactionExpanded = new FragmentTransactionExpanded();
                    fragmentTransactionExpanded.setCurrentObject(item);
                    FragmentAnimator.animateSlideToLeft(app, fragmentTransactionExpanded, fragmentSettingsAll);
                }
            }
        });

        boolean received = item.getSent() == 0;
        CustomLogger.LogThis("TX getReceived", String.valueOf(item.getReceived()), "TX getSent", String.valueOf(item.getSent()),
                "TX getBalanceAfterTx", String.valueOf(item.getBalanceAfterTx()));
        int blockHeight = item.getBlockHeight();
        int estimatedBlockHeight = BRPeerManager.getEstimatedBlockHeight();
        if (blockHeight != Integer.MAX_VALUE && blockHeight + 5 < estimatedBlockHeight) {
            sentReceivedTextView.setBackgroundResource(received ? R.drawable.received_label : R.drawable.sent_label);
            sentReceivedTextView.setText(received ? "received" : "sent");
            sentReceivedTextView.setTextColor(received ? receivedColor : sentColor);
        } else {
            sentReceivedTextView.setBackgroundResource(R.drawable.unconfirmed_label);
            sentReceivedTextView.setTextColor(unconfirmedColor);

            int confirms = blockHeight == Integer.MAX_VALUE ? 0 : estimatedBlockHeight - blockHeight;
            Log.e(TAG, "item.getBlockHeight(): " + blockHeight + ", confirms: " + confirms + ", lastBlock: " + estimatedBlockHeight);
            sentReceivedTextView.setText(String.format("%d confirmations", confirms >= 0 && confirms < 6 ? confirms : -1));

        }

        long itemTimeStamp = item.getTimeStamp();
        Log.e(TAG, "item.getTimeStamp(): " + itemTimeStamp);
        dateTextView.setText(itemTimeStamp != 0 ? getFormattedDateFromLong(itemTimeStamp * 1000) : "");

        long bitsAmount = m.getBitsFromSatoshi(received ? item.getReceived() : (item.getSent() - item.getReceived()) * -1);

        bitsTextView.setText(m.getFormattedCurrencyString("BTC", String.valueOf(bitsAmount)));
        dollarsTextView.setText(String.format("(%s)", m.getExchangeForAmount(m.getRateFromPrefs(), m.getISOFromPrefs(), String.valueOf(bitsAmount))));
        long bitsAfterTx = m.getBitsFromSatoshi(item.getBalanceAfterTx());

        bitsTotalTextView.setText(m.getFormattedCurrencyString("BTC", String.valueOf(bitsAfterTx)));
        dollarsTotalTextView.setText(String.format("(%s)", m.getExchangeForAmount(m.getRateFromPrefs(), m.getISOFromPrefs(), String.valueOf(bitsAfterTx))));

        return tmpLayout;
    }

    @SuppressLint("SimpleDateFormat")
    public static String getFormattedDateFromLong(long time) {
        MainActivity app = MainActivity.app;
        SimpleDateFormat sdf;
        if (app != null) {
            Locale current = app.getResources().getConfiguration().locale;
            sdf = new SimpleDateFormat("MM/dd@ha", current);
        } else {
            sdf = new SimpleDateFormat("MM/dd@ha");
        }
        Date resultDate = new Date(time);
        return sdf.format(resultDate);
    }

}
