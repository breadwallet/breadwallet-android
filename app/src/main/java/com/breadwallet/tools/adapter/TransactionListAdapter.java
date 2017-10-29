package com.breadwallet.tools.adapter;

import android.app.Activity;
import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.support.constraint.ConstraintLayout;
import android.support.constraint.ConstraintSet;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.breadwallet.R;
import com.breadwallet.presenter.customviews.BRText;
import com.breadwallet.presenter.entities.TxItem;
import com.breadwallet.tools.manager.BRSharedPrefs;
import com.breadwallet.tools.manager.PromptManager;
import com.breadwallet.tools.manager.TxManager;
import com.breadwallet.tools.security.BRKeyStore;
import com.breadwallet.tools.util.BRCurrency;
import com.breadwallet.tools.util.BRDateUtil;
import com.breadwallet.tools.util.BRExchange;
import com.breadwallet.tools.util.Utils;
import com.breadwallet.wallet.BRPeerManager;
import com.platform.entities.TxMetaData;
import com.platform.tools.KVStoreManager;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;


/**
 * BreadWallet
 * <p>
 * Created by Mihail Gutan <mihail@breadwallet.com> on 7/27/15.
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

public class TransactionListAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
    public static final String TAG = TransactionListAdapter.class.getName();

    private final Context mContext;
    private final int txResId;
    private final int syncingResId;
    private final int promptResId;
    private List<TxItem> backUpFeed;
    private List<TxItem> itemFeed;
    private final int txType = 0;
    private final int promptType = 1;
    private final int syncingType = 2;
    private byte[] tempAuthKey;
    private PromptManager.PromptItem currPromptItem;

    public TransactionListAdapter(Context mContext, List<TxItem> items) {
        itemFeed = items;
        backUpFeed = items;
        if (itemFeed == null) itemFeed = new ArrayList<>();
        this.txResId = R.layout.tx_item;
        this.syncingResId = R.layout.syncing_item;
        this.promptResId = R.layout.prompt_item;
        this.mContext = mContext;
    }

    public void setItems(List<TxItem> items) {
        if (items == null) items = new ArrayList<>();
        this.itemFeed = items;
        this.backUpFeed = items;
    }

    public List<TxItem> getItems() {
        return itemFeed;
    }

    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        // inflate the layout
        LayoutInflater inflater = ((Activity) mContext).getLayoutInflater();
        if (viewType == txType)
            return new TxHolder(inflater.inflate(txResId, parent, false));
        else if (viewType == promptType)
            return new PromptHolder(inflater.inflate(promptResId, parent, false));
        else if (viewType == syncingType)
            return new SyncingHolder(inflater.inflate(syncingResId, parent, false));
        return null;
    }


    @Override
    public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {
        switch (holder.getItemViewType()) {
            case txType:
                setTexts((TxHolder) holder, position);
                break;
            case promptType:
                setPrompt((PromptHolder) holder);
                break;
            case syncingType:
                setSyncing((SyncingHolder) holder);
                break;
        }

    }

    @Override
    public int getItemViewType(int position) {
        if (position == 0 && currPromptItem == PromptManager.PromptItem.SYNCING) {
            return syncingType;
        } else if (position == 0 && currPromptItem != null) {
            return promptType;
        } else {
            return txType;
        }
    }

    @Override
    public int getItemCount() {
        return currPromptItem == null ? itemFeed.size() : itemFeed.size() + 1;
    }

    public void setPromptItem(final PromptManager.PromptItem newPromptItem) {
        if (newPromptItem == null && TransactionListAdapter.this.currPromptItem == null) {
            // Nothing to do
            return;
        }
        // Wait for a layout pass to complete before adding/changing a prompt
        new Handler(Looper.getMainLooper()).post(new Runnable() {
            @Override
            public void run() {
                boolean removing = newPromptItem == null;
                boolean updatingExisting = TransactionListAdapter.this.currPromptItem != null && newPromptItem != null;
                TransactionListAdapter.this.currPromptItem = newPromptItem;
                if (removing) {
                    Log.d(TAG, "notifyItemRemoved");
                    notifyItemRemoved(0);
                } else {
                    if (updatingExisting) {
                        Log.d(TAG, "notifyItemChanged: " + newPromptItem.name());
                        notifyItemChanged(0);
                    } else {
                        Log.d(TAG, "notifyItemInserted: " + newPromptItem.name());
                        notifyItemInserted(0);
                    }
                }
            }
        });
    }

    private void setTexts(final TxHolder convertView, int position) {

        TxItem item = itemFeed.get(currPromptItem == null ? position : position - 1);
        if (Utils.isNullOrEmpty(tempAuthKey)) {
            tempAuthKey = BRKeyStore.getAuthKey(mContext);
            new Handler().postDelayed(new Runnable() {
                @Override
                public void run() {
                    Log.e(TAG, "clearing out tempAuthKey: ");
                    Arrays.fill(tempAuthKey, (byte) 0);
                    tempAuthKey = null;
                }
            }, 10000);
        }
        TxMetaData txMetaData = KVStoreManager.getInstance().getTxMetaData(mContext, item.getTxHash(), tempAuthKey);
        String commentString = (txMetaData == null || txMetaData.comment == null) ? "" : txMetaData.comment;
        convertView.comment.setText(commentString);
        if (commentString.isEmpty()) {
            convertView.constraintLayout.removeView(convertView.comment);
            ConstraintSet set = new ConstraintSet();
            set.clone(convertView.constraintLayout);
            int px = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 8, mContext.getResources().getDisplayMetrics());

            set.connect(R.id.status, ConstraintSet.TOP, convertView.toFrom.getId(), ConstraintSet.BOTTOM, px);
            // Apply the changes
            set.applyTo(convertView.constraintLayout);
        } else {
            if (convertView.constraintLayout.indexOfChild(convertView.comment) == -1)
                convertView.constraintLayout.addView(convertView.comment);
            ConstraintSet set = new ConstraintSet();
            set.clone(convertView.constraintLayout);
            int px = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 8, mContext.getResources().getDisplayMetrics());

            set.connect(R.id.status, ConstraintSet.TOP, convertView.comment.getId(), ConstraintSet.BOTTOM, px);
            // Apply the changes
            set.applyTo(convertView.constraintLayout);
        }

        boolean received = item.getSent() == 0;
        convertView.arrowIcon.setImageResource(received ? R.drawable.arrow_down_bold_circle : R.drawable.arrow_up_bold_circle);
        convertView.mainLayout.setBackgroundResource(getResourceByPos(position));
        convertView.sentReceived.setText(received ? mContext.getString(R.string.TransactionDetails_received, "") : mContext.getString(R.string.TransactionDetails_sent, ""));
        convertView.toFrom.setText(received ? String.format(mContext.getString(R.string.TransactionDetails_from), "") : String.format(mContext.getString(R.string.TransactionDetails_to), ""));
        final String addr = item.getTo()[0];
        convertView.account.setText(addr);
        int blockHeight = item.getBlockHeight();
        int confirms = blockHeight == Integer.MAX_VALUE ? 0 : BRSharedPrefs.getLastBlockHeight(mContext) - blockHeight + 1;
        int relayCount = BRPeerManager.getRelayCount(item.getTxHash());

        int level = 0;
        if (confirms <= 0) {
            if (relayCount <= 0)
                level = 0;
            else if (relayCount == 1)
                level = 1;
            else
                level = 2;
        } else {
            if (confirms == 1)
                level = 3;
            else if (confirms == 2)
                level = 4;
            else if (confirms == 3)
                level = 5;
            else
                level = 6;
        }
        boolean availableForSpend = false;
        String sentReceived = received ? "Receiving" : "Sending";
        String percentage = "";
        switch (level) {
            case 0:
                percentage = "0%";
                break;
            case 1:
                percentage = "20%";
                break;
            case 2:
                percentage = "40%";
                availableForSpend = true;
                break;
            case 3:
                percentage = "60%";
                availableForSpend = true;
                break;
            case 4:
                percentage = "80%";
                availableForSpend = true;
                break;
            case 5:
                percentage = "100%";
                availableForSpend = true;
                break;
        }
        if (availableForSpend && received) {
            convertView.status_2.setText(mContext.getString(R.string.Transaction_available));
        } else {
            convertView.constraintLayout.removeView(convertView.status_2);
            ConstraintSet set = new ConstraintSet();
            set.clone(convertView.constraintLayout);
            int px = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 16, mContext.getResources().getDisplayMetrics());

            set.connect(R.id.status, ConstraintSet.BOTTOM, convertView.constraintLayout.getId(), ConstraintSet.BOTTOM, px);
            // Apply the changes
            set.applyTo(convertView.constraintLayout);
        }
        if (level == 6) {
            convertView.status.setText(mContext.getString(R.string.Transaction_complete));
        } else {
            convertView.status.setText(String.format("%s - %s", sentReceived, percentage));
        }

        if (!item.isValid())
            convertView.status.setText(mContext.getString(R.string.Transaction_invalid));

        long satoshisAmount = received ? item.getReceived() : (item.getSent() - item.getReceived());

        boolean isBTCPreferred = BRSharedPrefs.getPreferredBTC(mContext);
        String iso = isBTCPreferred ? "LTC" : BRSharedPrefs.getIso(mContext);
        convertView.amount.setText(BRCurrency.getFormattedCurrencyString(mContext, iso, BRExchange.getAmountFromSatoshis(mContext, iso, new BigDecimal(satoshisAmount))));

        //if it's 0 we use the current time.
        long timeStamp = item.getTimeStamp() == 0 ? System.currentTimeMillis() : item.getTimeStamp() * 1000;
        CharSequence timeSpan = BRDateUtil.getCustomSpan(new Date(timeStamp));

        convertView.timestamp.setText(timeSpan);

    }

    private void setPrompt(final PromptHolder prompt) {

        if (TxManager.getInstance().promptInfo == null) {
            throw new RuntimeException("can't happen, showing prompt with null PromptInfo");
        }

        prompt.mainLayout.setOnClickListener(TxManager.getInstance().promptInfo.listener);
        prompt.mainLayout.setBackgroundResource(R.drawable.tx_rounded);
        prompt.title.setText(TxManager.getInstance().promptInfo.title);
        prompt.description.setText(TxManager.getInstance().promptInfo.description);

    }

    private void setSyncing(final SyncingHolder syncing) {
//        Log.e(TAG, "setSyncing: " + syncing);
        TxManager.getInstance().syncingHolder = syncing;
        syncing.mainLayout.setBackgroundResource(R.drawable.tx_rounded);
    }

    private int getResourceByPos(int pos) {
        if (currPromptItem != null) pos--;
        if (itemFeed != null && itemFeed.size() == 1) {
            return R.drawable.tx_rounded;
        } else if (pos == 0) {
            return R.drawable.tx_rounded_up;
        } else if (itemFeed != null && pos == itemFeed.size() - 1) {
            return R.drawable.tx_rounded_down;
        } else {
            return R.drawable.tx_not_rounded;
        }
    }

    public void filterBy(String query, boolean[] switches) {
        filter(query, switches);
    }

    private void filter(final String query, final boolean[] switches) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                String lowerQuery = query.toLowerCase().trim();

                int switchesON = 0;
                for (boolean i : switches) if (i) switchesON++;

                final List<TxItem> filteredList = new ArrayList<>();
                TxMetaData metaData;
                for (TxItem item : backUpFeed) {
                    metaData = KVStoreManager.getInstance().getTxMetaData(mContext, item.getTxHash());
                    if (item.getTxHashHexReversed().toLowerCase().contains(lowerQuery)
                            || item.getFrom()[0].toLowerCase().contains(lowerQuery)
                            || item.getTo()[0].toLowerCase().contains(lowerQuery) ||
                            (metaData.comment != null && metaData.comment.toLowerCase().contains(lowerQuery))) {
                        if (switchesON == 0) {
                            filteredList.add(item);
                        } else {
                            boolean willAdd = true;
                            //filter by sent and this is received
                            if (switches[0] && (item.getSent() - item.getReceived() <= 0)) {
                                willAdd = false;
                            }
                            //filter by received and this is sent
                            if (switches[1] && (item.getSent() - item.getReceived() > 0)) {
                                willAdd = false;
                            }

                            int confirms = item.getBlockHeight() == Integer.MAX_VALUE ? 0 : BRSharedPrefs.getLastBlockHeight(mContext) - item.getBlockHeight() + 1;
                            //filter by pending and this is complete
                            if (switches[2] && confirms >= 6) {
                                willAdd = false;
                            }

                            //filter by completed and this is pending
                            if (switches[3] && confirms < 6) {
                                willAdd = false;
                            }

                            if (willAdd) filteredList.add(item);
                        }

                    }

                }
                ((Activity) mContext).runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        itemFeed = filteredList;
                        notifyDataSetChanged();
                    }
                });
            }
        }).start();

    }

    private class TxHolder extends RecyclerView.ViewHolder {
        public RelativeLayout mainLayout;
        public ConstraintLayout constraintLayout;
        public TextView sentReceived;
        public TextView amount;
        public TextView toFrom;
        public TextView account;
        public TextView status;
        public TextView status_2;
        public TextView timestamp;
        public TextView comment;
        public ImageView arrowIcon;

        public TxHolder(View view) {
            super(view);
            mainLayout = (RelativeLayout) view.findViewById(R.id.main_layout);
            constraintLayout = (ConstraintLayout) view.findViewById(R.id.constraintLayout);
            sentReceived = (TextView) view.findViewById(R.id.sent_received);
            amount = (TextView) view.findViewById(R.id.amount);
            toFrom = (TextView) view.findViewById(R.id.to_from);
            account = (TextView) view.findViewById(R.id.account);
            status = (TextView) view.findViewById(R.id.status);
            status_2 = (TextView) view.findViewById(R.id.status_2);
            timestamp = (TextView) view.findViewById(R.id.timestamp);
            comment = (TextView) view.findViewById(R.id.comment);
            arrowIcon = (ImageView) view.findViewById(R.id.arrow_icon);
        }
    }

    public class PromptHolder extends RecyclerView.ViewHolder {
        public RelativeLayout mainLayout;
        public ConstraintLayout constraintLayout;
        public BRText title;
        public BRText description;
        public ImageButton close;

        public PromptHolder(View view) {
            super(view);
            mainLayout = (RelativeLayout) view.findViewById(R.id.main_layout);
            constraintLayout = (ConstraintLayout) view.findViewById(R.id.prompt_layout);
            title = (BRText) view.findViewById(R.id.info_title);
            description = (BRText) view.findViewById(R.id.info_description);
            close = (ImageButton) view.findViewById(R.id.info_close_button);
        }
    }

    public class SyncingHolder extends RecyclerView.ViewHolder {
        public RelativeLayout mainLayout;
        public ConstraintLayout constraintLayout;
        public BRText date;
        public BRText label;
        public ProgressBar progress;

        public SyncingHolder(View view) {
            super(view);
            mainLayout = (RelativeLayout) view.findViewById(R.id.main_layout);
            constraintLayout = (ConstraintLayout) view.findViewById(R.id.syncing_layout);
            date = (BRText) view.findViewById(R.id.sync_date);
            label = (BRText) view.findViewById(R.id.syncing_label);
            progress = (ProgressBar) view.findViewById(R.id.sync_progress);
        }
    }


}