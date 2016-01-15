package com.breadwallet.presenter.fragments;

import android.app.Fragment;
import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.support.v4.graphics.drawable.DrawableCompat;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.breadwallet.R;
import com.breadwallet.presenter.activities.MainActivity;
import com.breadwallet.presenter.entities.TransactionListItem;
import com.breadwallet.tools.adapter.MiddleViewAdapter;
import com.breadwallet.tools.animation.FragmentAnimator;

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
    private static final String TAG = "MainFragmentSettings";

    private static MainActivity app;
    private FragmentSettingsAll fragmentSettingsAll;
    private static TransactionListItem[] transactionObjects;
    //    public TransactionListAdapter transactionListAdapter;
    public static LinearLayout transactionList;

//    private RelativeLayout relativeLayout;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        // The last two arguments ensure LayoutParams are inflated
        // properly.
        final View rootView = inflater.inflate(R.layout.fragment_settings_all, container, false);
        app = MainActivity.app;
        fragmentSettingsAll = this;

        RelativeLayout importPrivateKeys = (RelativeLayout) rootView.findViewById(R.id.import_private_key);
        RelativeLayout settings = (RelativeLayout) rootView.findViewById(R.id.settings);
//        relativeLayout = (RelativeLayout) rootView.findViewById(R.id.transactions);
        TextView noTransactions = (TextView) rootView.findViewById(R.id.text_no_transactions);
        transactionList = (LinearLayout) rootView.findViewById(R.id.transactions_list);
        refreshTransactions(getActivity());
        Log.e(TAG, "transactionObjects: " + transactionObjects);
//        transactionListAdapter = new TransactionListAdapter(getActivity());

//        transactionObjects = null; //to test the no transaction text
        if (transactionObjects != null) {
            noTransactions.setVisibility(View.GONE);
            refreshUI(getActivity());
//            transactionListAdapter.addAll(transactionObjects);
//            transactionList.setAdapter(transactionListAdapter);
//            transactionListAdapter.notifyDataSetChanged();
        }
//        setListViewHeightBasedOnChildren(transactionList);
        importPrivateKeys.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (FragmentAnimator.checkTheMultipressingAvailability()) {
                    FragmentAnimator.animateDecoderFragment();
                }
            }
        });
        settings.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (FragmentAnimator.checkTheMultipressingAvailability()) {
                    FragmentAnimator.animateSlideToLeft(app, new FragmentSettings(), fragmentSettingsAll);
                    Log.d(TAG, "Starting:   showBouncySlideHorizontal()");
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
        MiddleViewAdapter.resetMiddleView(null);
    }

    public static void refreshTransactions(Context ctx) {
        Log.e(TAG, "REFRESH TRANSACTIONS: 1");
        if (app != null) {

            //TODO put back the original method
//            transactionObjects = BRWalletManager.getInstance(app).getTransactions();
            transactionObjects = getTestTransactions();
            Log.e(TAG, "REFRESH TRANSACTIONS: " + transactionObjects);

        }
        refreshUI(ctx);
    }

    private static TransactionListItem[] getTestTransactions() {
        TransactionListItem[] transactionListItems = new TransactionListItem[10];
        for (int i = 0; i < transactionListItems.length; i++) {
            transactionListItems[i] = new TransactionListItem(i * 81, i * 23, "something".getBytes(),
                    i * 4, i * 9, i * 17, "some address", "some other address");
        }
        return transactionListItems;
    }

    public static void refreshUI(Context ctx) {
        if (transactionObjects == null) return;
        if (transactionObjects.length == 0) return;
        transactionList.removeAllViews();


        transactionList.addView(getSeparationLine(0, ctx));
        for (int i = 0; i < transactionObjects.length; i++) {
            View tmpView = getViewFromTransactionObject(transactionObjects[i]);
            Drawable drawable = tmpView.findViewById(R.id.transaction_sent_received_label).getBackground();

            drawable = DrawableCompat.wrap(drawable);

            if (i % 2 == 0) {
                DrawableCompat.setTint(drawable, Color.GREEN);
            } else {
                DrawableCompat.setTint(drawable, Color.RED);
            }

            transactionList.addView(tmpView);
            if (i != transactionObjects.length - 1)
                transactionList.addView(getSeparationLine(1, ctx));
        }

        transactionList.addView(getSeparationLine(0, ctx));

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

    public static View getViewFromTransactionObject(TransactionListItem item) {
        if (app == null) return null;
        LayoutInflater inflater = (LayoutInflater) app.getApplicationContext().getSystemService
                (Context.LAYOUT_INFLATER_SERVICE);
        RelativeLayout tmpLayout = (RelativeLayout) inflater.inflate(R.layout.transaction_list_item, null);
        Button sentReceive = (Button) tmpLayout.findViewById(R.id.transaction_sent_received_label);
        sentReceive.setText(item.getTo());
        sentReceive.setTextColor(Color.RED);
        return tmpLayout;
    }

//    private void setListViewHeightBasedOnChildren(ListView listView) {
//        ListAdapter listAdapter = listView.getAdapter();
//        if (listAdapter == null) {
//            return;
//        }
//
//        int totalHeight = 0;
//        for (int i = 0; i < listAdapter.getCount(); i++) {
//            View listItem = listAdapter.getView(i, null, listView);
//            if (listItem != null) {
//                listItem.measure(
//                        View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED),
//                        View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED));
//            }
//
//            totalHeight += listItem.getMeasuredHeight();
//        }
//
//        ViewGroup.LayoutParams params = listView.getLayoutParams();
//        params.height = totalHeight
//                + (listView.getDividerHeight() * (listAdapter.getCount() - 1));
//        listView.setLayoutParams(params);
//        listView.requestLayout();
//    }

}
