package com.breadwallet.presenter.fragments;

import android.app.Activity;
import android.app.Fragment;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListView;
import android.widget.RelativeLayout;

import com.breadwallet.R;
import com.breadwallet.presenter.activities.MainActivity;
import com.breadwallet.presenter.entities.TransactionListItem;
import com.breadwallet.tools.adapter.TransactionListAdapter;
import com.breadwallet.tools.animation.BRAnimator;
import com.breadwallet.tools.adapter.MiddleViewAdapter;
import com.breadwallet.wallet.BRWalletManager;


/**
 * BreadWallet
 * <p/>
 * Created by Mihail Gutan <mihail@breadwallet.com> on 8/4/15.
 * Copyright (c) 2016 breadwallet LLC
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
    public static TransactionListAdapter adapter;
    private static boolean refreshTransactionAvailable = true;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        // The last two arguments ensure LayoutParams are inflated
        // properly.
        final View rootView = inflater.inflate(R.layout.fragment_settings_all, container, false);

        transactionList = (ListView) rootView.findViewById(R.id.transactions_list);

        adapter = new TransactionListAdapter(getActivity(), transactionObjects);
        if (transactionObjects != null) {
            if (transactionObjects.length == 0) transactionObjects = null;
        }
        if (transactionList != null)
            transactionList.setAdapter(adapter);
        refreshTransactions(getActivity());
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
        refreshUI();
    }

    public static void refreshTransactions(final Activity ctx) {
        if (refreshTransactionAvailable) {
            refreshTransactionAvailable = false;
            new Thread(new Runnable() {
                @Override
                public void run() {
                    transactionObjects = BRWalletManager.getInstance(ctx).getTransactions();
                    if (ctx != null && ctx instanceof MainActivity) {
                        ctx.runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                refreshTransactionAvailable = true;
                                refreshUI();
                            }
                        });
                    }
                }
            }).start();
        }

    }

    public static void refreshUI() {
        if (BRAnimator.level != 1) return;
        if (adapter != null) {
            adapter.updateData(transactionObjects);
        }
    }

    public static RelativeLayout getSeparationLine(int MODE, Activity ctx) {
        //0 - regular , 1 - with left padding
        RelativeLayout line = new RelativeLayout(ctx);
        line.setMinimumHeight(1);
        line.setBackgroundColor(ctx.getColor(R.color.gray));
        if (MODE == 1)
            line.setPadding(40, 0, 0, 0);
        return line;
    }

}
