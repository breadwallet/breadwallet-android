package com.breadwallet.tools.manager;

import android.app.Activity;
import android.content.Context;
import android.os.Looper;
import android.support.annotation.WorkerThread;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;

import com.breadwallet.R;
import com.breadwallet.presenter.activities.WalletActivity;
import com.breadwallet.presenter.entities.TxUiHolder;
import com.breadwallet.tools.adapter.TransactionListAdapter;
import com.breadwallet.tools.animation.UiUtils;
import com.breadwallet.tools.listeners.RecyclerItemClickListener;
import com.breadwallet.tools.threads.executor.BRExecutor;
import com.breadwallet.wallet.WalletsMaster;
import com.breadwallet.wallet.abstracts.BaseWalletManager;

import java.util.List;


/**
 * BreadWalletP
 * <p/>
 * Created by Mihail Gutan on <mihail@breadwallet.com> 7/19/17.
 * Copyright (c) 2017 breadwallet LLC
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
public class TxManager {

    private static final String TAG = TxManager.class.getName();
    private static TxManager instance;
    private RecyclerView txList;
    public TransactionListAdapter adapter;
    private RecyclerItemClickListener mItemListener;


    public static TxManager getInstance() {
        if (instance == null) instance = new TxManager();
        return instance;
    }

    public void init(final WalletActivity app) {
        mItemListener = new RecyclerItemClickListener(app,
                txList, new RecyclerItemClickListener.OnItemClickListener() {
            @Override
            public void onItemClick(View view, int position, float x, float y) {

                if (position == -1) return;
                TxUiHolder item = adapter.getItems().get(position);
                UiUtils.showTransactionDetails(app, item, position);
            }

            @Override
            public void onLongItemClick(View view, int position) {

            }
        });
        txList = app.findViewById(R.id.tx_list);
        txList.setLayoutManager(new CustomLinearLayoutManager(app));
        txList.addOnItemTouchListener(mItemListener);
        if (adapter == null)
            adapter = new TransactionListAdapter(app, null);
        if (txList.getAdapter() == null)
            txList.setAdapter(adapter);
        adapter.notifyDataSetChanged();
    }

    private TxManager() {
    }

    public void onResume(final Activity app) {
        crashIfNotMain();
    }

    @WorkerThread
    public synchronized void updateTxList(final Context app) {
        BaseWalletManager wallet = WalletsMaster.getInstance(app).getCurrentWallet(app);
        if (wallet == null) {
            Log.e(TAG, "updateTxList: wallet is null");
            return;
        }
        if (TxManager.getInstance().adapter != null)
            TxManager.getInstance().adapter.updateData();
        final List<TxUiHolder> items = wallet.getTxUiHolders(app);

        if (adapter != null) {
            BRExecutor.getInstance().forMainThreadTasks().execute(new Runnable() {
                @Override
                public void run() {
                    adapter.setItems(items);
                    adapter.notifyDataSetChanged();
                }
            });
        }

    }

    private class CustomLinearLayoutManager extends LinearLayoutManager {

        public CustomLinearLayoutManager(Context context) {
            super(context);
        }

        /**
         * Disable predictive animations. There is a bug in RecyclerView which causes views that
         * are being reloaded to pull invalid ViewHolders from the internal recycler stack if the
         * adapter size has decreased since the ViewHolder was recycled.
         */
        @Override
        public boolean supportsPredictiveItemAnimations() {
            return false;
        }

        public CustomLinearLayoutManager(Context context, int orientation, boolean reverseLayout) {
            super(context, orientation, reverseLayout);
        }

        public CustomLinearLayoutManager(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
            super(context, attrs, defStyleAttr, defStyleRes);
        }
    }

    private void crashIfNotMain() {
        if (Looper.myLooper() != Looper.getMainLooper()) {
            throw new IllegalAccessError("Can only call from main thread");
        }
    }

}
