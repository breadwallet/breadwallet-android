package com.breadwallet.tools.manager;

import android.app.Activity;
import android.content.Context;
import android.os.Handler;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.helper.ItemTouchHelper;
import android.util.Log;
import android.view.View;

import com.breadwallet.R;
import com.breadwallet.presenter.activities.BreadActivity;
import com.breadwallet.presenter.entities.TxItem;
import com.breadwallet.tools.adapter.TransactionListAdapter;
import com.breadwallet.tools.animation.BRAnimator;
import com.breadwallet.tools.listeners.RecyclerItemClickListener;
import com.breadwallet.wallet.BRPeerManager;
import com.breadwallet.wallet.BRWalletManager;

import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

/**
 * BreadWallet
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
    public PromptManager.PromptItem currentPrompt;

    public PromptManager.PromptInfo promptInfo;
    public TransactionListAdapter.SyncingHolder syncingHolder;

    public static TxManager getInstance() {
        if (instance == null) instance = new TxManager();
        return instance;
    }

    public void init(final BreadActivity app) {
        txList = (RecyclerView) app.findViewById(R.id.tx_list);
        txList.setLayoutManager(new LinearLayoutManager(app));
        txList.addOnItemTouchListener(new RecyclerItemClickListener(app,
                txList, new RecyclerItemClickListener.OnItemClickListener() {
            @Override
            public void onItemClick(View view, int position, float x, float y) {
                if (currentPrompt == null || position > 0)
                    BRAnimator.showTransactionPager(app, adapter.getItems(), position);

            }

            @Override
            public void onLongItemClick(View view, int position) {

            }
        }));
        if (adapter == null)
            adapter = new TransactionListAdapter(app, null);
        setupSwipe(app);
    }

    private TxManager() {
    }

    public void onResume(final Activity app) {
        Log.e(TAG, "TxManager onResume: " + currentPrompt);
        new Thread(new Runnable() {
            @Override
            public void run() {
                final double progress = BRPeerManager.syncProgress(SharedPreferencesManager.getStartHeight(app));

                app.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        if (progress > 0 && progress < 1){
                            currentPrompt = PromptManager.PromptItem.SYNCING;
                        } else {
                            showNextPrompt(app);
                        }
                        updateCard(app);
                    }
                });

            }
        }).start();

    }

    public void showPrompt(Activity app, PromptManager.PromptItem item) {
        Log.e(TAG, "showPrompt: " + item);
        if (item == null) throw new RuntimeException("can't be null");
        if (currentPrompt != PromptManager.PromptItem.SYNCING) {
            currentPrompt = item;
        }
        Log.d(TAG, "setCurrentPrompt: try: " + item + ", SET to: " + currentPrompt);
        updateCard(app);
    }

    public void hidePrompt(final Activity app, final PromptManager.PromptItem item) {
        Log.e(TAG, "hidePrompt: " + item);

        app.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (item == PromptManager.PromptItem.SYNCING) {
                    new Handler().postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            showNextPrompt(app);
                        }
                    }, 1000);

                }
            }
        });

        currentPrompt = null;
        updateCard(app);
    }

    public void showInfoCard(boolean show, PromptManager.PromptInfo info) {
        if (show) {
            promptInfo = info;
        } else {
            promptInfo = null;
        }

    }

    private void showNextPrompt(Activity app) {

        PromptManager.PromptItem toShow = PromptManager.getInstance().nextPrompt(app);
        if (toShow != null) {
            Log.e(TAG, "showNextPrompt: " + toShow);
            currentPrompt = toShow;
            PromptManager.PromptInfo info = PromptManager.getInstance().promptInfo(app, currentPrompt);
            showInfoCard(true, info);
            updateCard(app);
        } else {
            Log.e(TAG, "showNextPrompt: nothing to show");
        }
    }

    //BLOCKS
    public void updateTxList(final Context app) {
        final TxItem[] arr = BRWalletManager.getInstance().getTransactions();
        ((Activity) app).runOnUiThread(new Runnable() {
            @Override
            public void run() {
                List<TxItem> items = arr == null ? null : new LinkedList<>(Arrays.asList(arr));
                adapter.setItems(items);
                txList.setAdapter(adapter);
                adapter.notifyDataSetChanged();
            }
        });

    }

    public void updateCard(Context app) {
//        Log.e(TAG, "updateTxList: ");
//        ((Activity) app).runOnUiThread(new Runnable() {
//            @Override
//            public void run() {
//                adapter.notifyItemChanged(0);
//            }
//        });
        updateTxList(app);
    }

    private void setupSwipe(final Activity app) {
        ItemTouchHelper.SimpleCallback simpleItemTouchCallback = new ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.RIGHT) {

            @Override
            public boolean onMove(RecyclerView recyclerView, RecyclerView.ViewHolder viewHolder, RecyclerView.ViewHolder target) {
//                Toast.makeText(BreadActivity.this, "on Move ", Toast.LENGTH_SHORT).show();
                return false;
            }

            @Override
            public void onSwiped(RecyclerView.ViewHolder viewHolder, int swipeDir) {
                hidePrompt(app, null);
                //Remove swiped item from list and notify the RecyclerView
            }

            @Override
            public int getSwipeDirs(RecyclerView recyclerView, RecyclerView.ViewHolder viewHolder) {
                if (!(viewHolder instanceof TransactionListAdapter.PromptHolder)) return 0;
                return super.getSwipeDirs(recyclerView, viewHolder);
            }
        };
        ItemTouchHelper itemTouchHelper = new ItemTouchHelper(simpleItemTouchCallback);
        itemTouchHelper.attachToRecyclerView(txList);
    }
}
