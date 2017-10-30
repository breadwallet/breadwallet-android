package com.breadwallet.tools.manager;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.app.Activity;
import android.content.Context;
import android.os.Handler;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.helper.ItemTouchHelper;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;

import com.breadwallet.R;
import com.breadwallet.presenter.activities.BreadActivity;
import com.breadwallet.presenter.entities.CurrencyEntity;
import com.breadwallet.presenter.entities.TxItem;
import com.breadwallet.tools.adapter.TransactionListAdapter;
import com.breadwallet.tools.animation.BRAnimator;
import com.breadwallet.tools.listeners.RecyclerItemClickListener;
import com.breadwallet.tools.sqlite.CurrencyDataSource;
import com.breadwallet.wallet.BRPeerManager;
import com.breadwallet.wallet.BRWalletManager;
import com.platform.entities.TxMetaData;
import com.platform.tools.KVStoreManager;

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
    public RecyclerView txList;
    public TransactionListAdapter adapter;
    public PromptManager.PromptItem currentPrompt;
    private boolean isMetaDataUpdating;

    public PromptManager.PromptInfo promptInfo;
    public TransactionListAdapter.SyncingHolder syncingHolder;

    public static TxManager getInstance() {
        if (instance == null) instance = new TxManager();
        return instance;
    }

    public void init(final BreadActivity app) {
        txList = (RecyclerView) app.findViewById(R.id.tx_list);
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
                        view.animate().setDuration(150).translationX(BreadActivity.screenParametersPoint.x).setListener(new AnimatorListenerAdapter() {
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
                            setPrompt(null);
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
        setupSwipe(app);
    }

    private TxManager() {
    }

    public void onResume(final Activity app) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                final double progress = BRPeerManager.syncProgress(BRSharedPrefs.getStartHeight(app));

                app.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        if (progress > 0 && progress < 1) {
                            setPrompt(PromptManager.PromptItem.SYNCING);
                        } else {
                            showNextPrompt(app);
                        }
                        updateCard(app);
                    }
                });

            }
        }).start();

    }

    private void setPrompt(PromptManager.PromptItem item) {
        currentPrompt = item;
        adapter.setPromptItem(currentPrompt);
    }

    public void showPrompt(Activity app, PromptManager.PromptItem item) {
        if (item == null) throw new RuntimeException("can't be null");
        BREventManager.getInstance().pushEvent("prompt." + PromptManager.getInstance().getPromptName(item) + ".displayed");
        if (currentPrompt != PromptManager.PromptItem.SYNCING) {
            setPrompt(item);
        }
        updateCard(app);
    }

    public void hidePrompt(final Activity app, final PromptManager.PromptItem item) {
        setPrompt(null);
        updateCard(app);
        if (item == PromptManager.PromptItem.SYNCING) {
            app.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    new Handler().postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            showNextPrompt(app);
                            updateCard(app);
                        }
                    }, 1000);

                }
            });
        } else {
            if (item != null)
                BREventManager.getInstance().pushEvent("prompt." + PromptManager.getInstance().getPromptName(item) + ".dismissed");
            if (currentPrompt == PromptManager.PromptItem.SHARE_DATA) {
                BRSharedPrefs.putShareDataDismissed(app, true);
            }
        }

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
            Log.d(TAG, "showNextPrompt: " + toShow);
            setPrompt(toShow);
            PromptManager.PromptInfo info = PromptManager.getInstance().promptInfo(app, currentPrompt);
            showInfoCard(true, info);
            updateCard(app);
        } else {
            Log.i(TAG, "showNextPrompt: nothing to show");
        }
    }

    //BLOCKS
    public void updateTxList(final Context app) {
        final TxItem[] arr = BRWalletManager.getInstance().getTransactions();
        updateTxMetaData(app, arr);
        List<TxItem> items = arr == null ? null : new LinkedList<>(Arrays.asList(arr));
        if (adapter != null) {
            adapter.setItems(items);
            ((Activity) app).runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    txList.swapAdapter(adapter, true);

                    adapter.notifyDataSetChanged();
                }
            });
        }

    }

    private void updateTxMetaData(final Context app, final TxItem[] arr) {
        if (isMetaDataUpdating) return;
        isMetaDataUpdating = true;
        new Thread(new Runnable() {
            @Override
            public void run() {
                if (arr != null) {
                    for (TxItem item : arr) {
                        KVStoreManager kvM = KVStoreManager.getInstance();
                        String iso = BRSharedPrefs.getIso(app);
                        CurrencyEntity currencyEntity = CurrencyDataSource.getInstance().getCurrencyByIso(iso);
                        double rate = currencyEntity == null ? 0f: currencyEntity.rate;
                        TxMetaData temp = kvM.getTxMetaData(app, item.getTxHash());
                        String comment = temp == null ? "" : temp.comment;

                        TxMetaData tx = new TxMetaData();
                        tx.exchangeCurrency = iso;
                        tx.exchangeRate = rate;
                        tx.fee = item.getFee();
                        tx.creationTime = (int) (item.getTimeStamp() / 1000);
                        tx.blockHeight = item.getBlockHeight();
                        tx.deviceId = BRSharedPrefs.getDeviceId(app);
                        tx.txSize = item.getTxSize();
                        tx.comment = comment == null ? "" : comment;
//                        tx.classVersion = ...
                        kvM.putTxMetaData(app, tx, item.getTxHash());
                    }
                }
                isMetaDataUpdating = false;
            }
        }).start();
    }

    public void updateCard(Context app) {
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

}
