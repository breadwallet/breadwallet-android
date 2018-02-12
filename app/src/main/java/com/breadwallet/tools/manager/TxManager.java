package com.breadwallet.tools.manager;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.app.Activity;
import android.content.Context;
import android.os.Looper;
import android.support.annotation.WorkerThread;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.helper.ItemTouchHelper;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;

import com.breadwallet.R;
import com.breadwallet.presenter.activities.WalletActivity;
import com.breadwallet.presenter.activities.util.BRActivity;
import com.breadwallet.presenter.entities.TxUiHolder;
import com.breadwallet.tools.adapter.TransactionListAdapter;
import com.breadwallet.tools.animation.BRAnimator;
import com.breadwallet.tools.listeners.RecyclerItemClickListener;
import com.breadwallet.tools.threads.BRExecutor;
import com.breadwallet.wallet.WalletsMaster;
import com.breadwallet.wallet.abstracts.BaseWallet;

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

    public void init(final WalletActivity app) {
        txList = app.findViewById(R.id.tx_list);
        txList.setLayoutManager(new CustomLinearLayoutManager(app));
        txList.addOnItemTouchListener(new RecyclerItemClickListener(app,
                txList, new RecyclerItemClickListener.OnItemClickListener() {
            @Override
            public void onItemClick(View view, int position, float x, float y) {
                if (currentPrompt == null || position > 0)
                    BRAnimator.showTransactionPager(app, adapter.getItems(), currentPrompt == null ? position : position - 1);
                else {
                    //clicked on the  x (close)
                    if (x > view.getWidth() - 100 && y < 100) {
                        view.animate().setDuration(150).translationX(BRActivity.screenParametersPoint.x).setListener(new AnimatorListenerAdapter() {
                            @Override
                            public void onAnimationEnd(Animator animation) {
                                super.onAnimationEnd(animation);
                                hidePrompt(app, null);
                            }
                        });

                    } else { //clicked on the prompt
                        BREventManager.getInstance().pushEvent("prompt." + PromptManager.getInstance().getPromptName(currentPrompt) + ".trigger");
                        if (currentPrompt != PromptManager.PromptItem.SYNCING) {
                            PromptManager.PromptInfo info = PromptManager.getInstance().promptInfo(app, currentPrompt);
                            if (info != null)
                                info.listener.onClick(view);
                            currentPrompt = null;
                        }
                    }
                }
            }

            @Override
            public void onLongItemClick(View view, int position) {

            }
        }));
        if (adapter == null)
            adapter = new TransactionListAdapter(app, null);
        txList.setAdapter(adapter);
        adapter.notifyDataSetChanged();
        setupSwipe(app);
    }

    private TxManager() {
    }

    public void onResume(final Activity app) {
        crashIfNotMain();
        BRExecutor.getInstance().forLightWeightBackgroundTasks().execute(new Runnable() {
            @Override
            public void run() {
                String currentIso = BRSharedPrefs.getCurrentWalletIso(app);
                BaseWallet wallet = WalletsMaster.getInstance().getCurrentWallet(app);
                final double progress = wallet.getPeerManager().getSyncProgress(BRSharedPrefs.getStartHeight(app, currentIso));
                BRExecutor.getInstance().forMainThreadTasks().execute(new Runnable() {
                    @Override
                    public void run() {
                        if (progress > 0 && progress < 1) {
                            currentPrompt = PromptManager.PromptItem.SYNCING;
                            updateCard(app);
                        } else {
                            showNextPrompt(app);
                        }
                    }
                });
            }
        });

    }

    public void showPrompt(Context app, PromptManager.PromptItem item) {
        crashIfNotMain();
        if (item == null) throw new RuntimeException("can't be null");
        BREventManager.getInstance().pushEvent("prompt." + PromptManager.getInstance().getPromptName(item) + ".displayed");
        if (currentPrompt != PromptManager.PromptItem.SYNCING) {
            currentPrompt = item;
        }
        updateCard(app);
    }

    public void hidePrompt(final Context app, final PromptManager.PromptItem item) {
        crashIfNotMain();
        if (currentPrompt == PromptManager.PromptItem.SHARE_DATA) {
            BRSharedPrefs.putShareDataDismissed(app, true);
        }
        currentPrompt = null;
        if (txList != null && txList.getAdapter() != null)
            txList.getAdapter().notifyItemRemoved(0);
        if (item == PromptManager.PromptItem.SYNCING) {
            showNextPrompt(app);
            updateCard(app);
        } else {
            if (item != null)
                BREventManager.getInstance().pushEvent("prompt." + PromptManager.getInstance().getPromptName(item) + ".dismissed");

        }

    }

    private void showNextPrompt(Context app) {
        crashIfNotMain();
        PromptManager.PromptItem toShow = PromptManager.getInstance().nextPrompt(app);
        if (toShow != null) {
//            Log.d(TAG, "showNextPrompt: " + toShow);
            currentPrompt = toShow;
            promptInfo = PromptManager.getInstance().promptInfo((Activity) app, currentPrompt);
            updateCard(app);
        } else {
            Log.i(TAG, "showNextPrompt: nothing to show");
        }
    }

    @WorkerThread
    public synchronized void updateTxList(final Context app) {
        long start = System.currentTimeMillis();
        BaseWallet wallet = WalletsMaster.getInstance().getCurrentWallet(app);

        final List<TxUiHolder> items = wallet.getTxUiHolders();

        long took = (System.currentTimeMillis() - start);
        if (took > 500)
            Log.e(TAG, "updateTxList: took: " + took);
        if (adapter != null) {
            ((Activity) app).runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    adapter.setItems(items);
                    txList.setAdapter(adapter);
                    adapter.notifyDataSetChanged();
                }
            });
        }

    }

    public void updateCard(final Context app) {
        BRExecutor.getInstance().forLightWeightBackgroundTasks().execute(new Runnable() {
            @Override
            public void run() {
                updateTxList(app);
            }
        });
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
