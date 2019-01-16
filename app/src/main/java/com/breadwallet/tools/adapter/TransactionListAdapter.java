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

package com.breadwallet.tools.adapter;

import android.app.Activity;
import android.content.Context;
import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;
import android.text.format.DateUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;

import com.breadwallet.R;
import com.breadwallet.core.ethereum.BREthereumToken;
import com.breadwallet.presenter.customviews.BaseTextView;
import com.breadwallet.presenter.entities.TxUiHolder;
import com.breadwallet.tools.manager.BRSharedPrefs;
import com.breadwallet.tools.threads.executor.BRExecutor;
import com.breadwallet.tools.util.BRDateUtil;
import com.breadwallet.tools.util.CurrencyUtils;
import com.breadwallet.tools.util.Utils;
import com.breadwallet.wallet.WalletsMaster;
import com.breadwallet.wallet.abstracts.BaseWalletManager;
import com.breadwallet.wallet.wallets.CryptoTransaction;
import com.breadwallet.wallet.wallets.ethereum.WalletEthManager;
import com.platform.entities.TxMetaData;
import com.platform.tools.KVStoreManager;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class TransactionListAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
    public static final String TAG = TransactionListAdapter.class.getName();
    private static final int DP_120 = 120;
    private static final int DP_36 = 36;
    private static final int DP_16 = 16;
    private static final int CONFIRMED_BLOCKS_NUMBER = 6;
    private static final int PROGRESS_FULL = 100;
    private static final int PROGRESS_PACE = 20;
    private static final int FOUR_CONFIRMATIONS = 4;
    private static final int FIVE_CONFIRMATIONS = 5;
    private final Context mContext;
    private final int mTxResourceId;
    private List<TxUiHolder> mBackUpFeed;
    private List<TxUiHolder> mItemFeed;
    private Map<Integer, TxMetaData> mMetaDatas;
    private static final int TX_TYPE = 0;
    private boolean mIsUpdatingData;

    public TransactionListAdapter(Context context, List<TxUiHolder> items) {
        this.mTxResourceId = R.layout.tx_item;
        this.mContext = context;
        mBackUpFeed = items;
        mItemFeed = items;
        mMetaDatas = new HashMap<>();
        items = new ArrayList<>();
        init(items);
    }

    public void setItems(List<TxUiHolder> items) {
        init(items);
    }

    private void init(List<TxUiHolder> items) {
        if (items == null) {
            items = new ArrayList<>();
        }
        if (mItemFeed == null) {
            mItemFeed = new ArrayList<>();
        }
        if (mBackUpFeed == null) {
            mBackUpFeed = new ArrayList<>();
        }
        //In case we're in the middle of filtering transactions.
        if (mBackUpFeed.size() == mItemFeed.size()) {
            this.mItemFeed = items;
        }
        this.mBackUpFeed = items;
    }

    public void updateData() {
        if (!mIsUpdatingData) {
            Map<Integer, TxMetaData> localMDs = new HashMap<>();
            BaseWalletManager wm = WalletsMaster.getInstance(mContext).getCurrentWallet(mContext);
            for (int i = 0; i < mBackUpFeed.size(); i++) {
                TxUiHolder item = mBackUpFeed.get(i);
                TxMetaData md = KVStoreManager.getTxMetaData(mContext, item.getTxHash());
                if (System.currentTimeMillis() - item.getTimeStamp() < DateUtils.HOUR_IN_MILLIS) {
                    if (md == null) {
                        md = KVStoreManager.createMetadata(mContext, wm,
                                new CryptoTransaction(item.getTransaction()));
                        KVStoreManager.putTxMetaData(mContext, md, item.getTxHash());
                    } else if (md.exchangeRate == 0) {
                        md.exchangeRate = wm.getFiatExchangeRate(mContext).doubleValue();
                        md.exchangeCurrency = BRSharedPrefs.getPreferredFiatIso(mContext);
                        Log.d(TAG, "MetaData not null");
                        KVStoreManager.putTxMetaData(mContext, md, item.getTxHash());
                    }
                }
                localMDs.put(i, md);
            }
            mMetaDatas.clear();
            mMetaDatas.putAll(localMDs);
            mIsUpdatingData = false;
            BRExecutor.getInstance().forMainThreadTasks().execute(new Runnable() {
                @Override
                public void run() {
                    notifyDataSetChanged();
                }
            });
        }
    }

    public List<TxUiHolder> getItems() {
        return mItemFeed;
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        // inflate the layout
        LayoutInflater inflater = ((Activity) mContext).getLayoutInflater();
        return new TxHolder(inflater.inflate(mTxResourceId, parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        if (holder.getItemViewType() == TX_TYPE) {
            holder.setIsRecyclable(false);
            setTexts((TxHolder) holder, position);
        }
    }

    @Override
    public int getItemViewType(int position) {
        return TX_TYPE;
    }

    @Override
    public int getItemCount() {
        return mItemFeed.size();
    }

    private void setTexts(final TxHolder convertView, int position) {
        BaseWalletManager wm = WalletsMaster.getInstance(mContext).getCurrentWallet(mContext);
        TxUiHolder item = mItemFeed.get(position);

        String commentString = "";
        TxMetaData md = mMetaDatas.size() > position ? mMetaDatas.get(position) : null;
        if (md != null) {
            if (md.comment != null) {
                commentString = md.comment;
            }
        }

        boolean received = item.isReceived();
        int amountColor = received ? R.color.transaction_amount_received_color : R.color.total_assets_usd_color;

        convertView.getTransactionAmount().setTextColor(mContext.getResources().getColor(amountColor, null));

        // If this transaction failed, show the "FAILED" indicator in the cell
        if (!item.isValid()) {
            showTransactionFailed(convertView, item, received);
        }

        BigDecimal cryptoAmount = item.getAmount().abs();

        BREthereumToken tkn = null;
        if (wm.getCurrencyCode().equalsIgnoreCase(WalletEthManager.ETH_CURRENCY_CODE) && wm.isAddressValid(item.getTo())) {
            tkn = WalletEthManager.getInstance(mContext).node.lookupToken(item.getTo());
        }
        // it's a token transfer ETH tx
        if (tkn != null) {
            cryptoAmount = item.getFee();
        }
        boolean isCryptoPreferred = BRSharedPrefs.isCryptoPreferred(mContext);
        String preferredCurrencyCode = isCryptoPreferred ? wm.getCurrencyCode() : BRSharedPrefs.getPreferredFiatIso(mContext);
        BigDecimal amount = isCryptoPreferred ? cryptoAmount : wm.getFiatForSmallestCrypto(mContext, cryptoAmount, null);
        if (!received && amount != null) {
            amount = amount.negate();
        }
        String formattedAmount = CurrencyUtils.getFormattedAmount(mContext, preferredCurrencyCode, amount, wm.getUiConfiguration().getMaxDecimalPlacesForUi());
        convertView.getTransactionAmount().setText(formattedAmount);
        int blockHeight = item.getBlockHeight();
        int lastBlockHeight = BRSharedPrefs.getLastBlockHeight(mContext, wm.getCurrencyCode());
        int confirms = blockHeight == Integer.MAX_VALUE ? 0 : lastBlockHeight - blockHeight + 1;
        int level;
        if (confirms <= 0) {
            long relayCount = wm.getRelayCount(item.getTxHash());
            if (relayCount <= 0) {
                level = 0;
            } else if (relayCount == 1) {
                level = 1;
            } else {
                level = 2;
            }
        } else {
            if (confirms >= FOUR_CONFIRMATIONS) {
                level = CONFIRMED_BLOCKS_NUMBER;
            } else {
                level = confirms + 2;
            }
        }
        if (level > 0 && level < FIVE_CONFIRMATIONS) {
            showTransactionProgress(convertView, level * PROGRESS_PACE);
        }
        String sentTo = String.format(mContext.getString(R.string.Transaction_sentTo), wm.decorateAddress(item.getTo()));
        String receivedVia = String.format(mContext.getString(R.string.TransactionDetails_receivedVia), wm.decorateAddress(item.getTo()));

        String sendingTo = String.format(mContext.getString(R.string.Transaction_sendingTo), wm.decorateAddress(item.getTo()));
        String receivingVia = String.format(mContext.getString(R.string.TransactionDetails_receivingVia), wm.decorateAddress(item.getTo()));

        if (level > FOUR_CONFIRMATIONS) {
            convertView.getTransactionDetail().setText(!commentString.isEmpty() ? commentString : (!received ? sentTo : receivedVia));
        } else {
            convertView.getTransactionDetail().setText(!commentString.isEmpty() ? commentString : (!received ? sendingTo : receivingVia));
        }
        // it's a token transfer ETH tx
        if (tkn != null) {
            convertView.getTransactionDetail()
                    .setText(String.format(mContext.getString(R.string.Transaction_tokenTransfer), tkn.getSymbol()));
        }

        //if it's 0 we use the current time.
        long timeStamp = item.getTimeStamp() == 0 ? System.currentTimeMillis() : item.getTimeStamp() * DateUtils.SECOND_IN_MILLIS;

        String shortDate = BRDateUtil.getShortDate(timeStamp);

        convertView.getTransactionDate().setText(shortDate);
    }

    private void showTransactionProgress(TxHolder holder, int progress) {
        if (progress < PROGRESS_FULL) {
            holder.getTransactionProgress().setVisibility(View.VISIBLE);
            holder.getTransactionDate().setVisibility(View.GONE);
            holder.getTransactionProgress().setProgress(progress);
            RelativeLayout.LayoutParams detailParams = new RelativeLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.MATCH_PARENT);
            detailParams.addRule(RelativeLayout.RIGHT_OF, holder.getTransactionProgress().getId());
            detailParams.addRule(RelativeLayout.CENTER_VERTICAL);
            detailParams.setMargins(Utils.getPixelsFromDps(mContext, DP_16), Utils.getPixelsFromDps(mContext, DP_36), 0, 0);
            holder.getTransactionDetail().setLayoutParams(detailParams);
            holder.getTransactionDetail().setMaxWidth(Utils.getPixelsFromDps(mContext, DP_120));
        } else {
            holder.getTransactionProgress().setVisibility(View.INVISIBLE);
            holder.getTransactionDate().setVisibility(View.VISIBLE);
            RelativeLayout.LayoutParams startingParams = new RelativeLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            startingParams.addRule(RelativeLayout.ALIGN_PARENT_LEFT);
            startingParams.addRule(RelativeLayout.CENTER_VERTICAL);
            startingParams.setMargins(Utils.getPixelsFromDps(mContext, DP_16), 0, 0, 0);
            holder.getTransactionDetail().setLayoutParams(startingParams);
            holder.setIsRecyclable(true);
        }
    }

    private void showTransactionFailed(TxHolder holder, TxUiHolder tx, boolean received) {
        holder.getTransactionDate().setVisibility(View.INVISIBLE);
        RelativeLayout.LayoutParams params = new RelativeLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        params.addRule(RelativeLayout.RIGHT_OF, holder.getTransactionFailed().getId());
        params.setMargins(DP_16, 0, 0, 0);
        params.addRule(RelativeLayout.CENTER_VERTICAL, holder.getTransactionFailed().getId());
        holder.getTransactionDetail().setLayoutParams(params);
        BaseWalletManager wm = WalletsMaster.getInstance(mContext).getCurrentWallet(mContext);

        if (!received) {
            holder.getTransactionDetail().setText(String.format(mContext.getString(R.string.Transaction_sendingTo),
                    wm.decorateAddress(tx.getTo())));
        }
    }

    public void resetFilter() {
        mItemFeed = mBackUpFeed;
        notifyDataSetChanged();
    }

    public void filterBy(final String query, final boolean[] switches) {
        String lowerQuery = query.toLowerCase().trim();
        if (Utils.isNullOrEmpty(lowerQuery) && !switches[0] && !switches[1] && !switches[2] && !switches[3]) {
            return;
        }
        int switchesON = 0;
        for (boolean i : switches) {
            if (i) {
                switchesON++;
            }
        }

        final List<TxUiHolder> filteredList = new ArrayList<>();
        TxUiHolder item;
        for (int i = 0; i < mBackUpFeed.size(); i++) {
            item = mBackUpFeed.get(i);
            boolean matchesHash = item.getHashReversed() != null && item.getHashReversed().contains(lowerQuery);
            boolean matchesAddress = item.getFrom().contains(lowerQuery) || item.getTo().contains(lowerQuery);
            boolean matchesMemo = item.metaData != null && item.metaData.comment != null
                    && item.metaData.comment.toLowerCase().contains(lowerQuery);
            if (matchesHash || matchesAddress || matchesMemo) {
                if (switchesON == 0) {
                    filteredList.add(item);
                } else {
                    boolean willAdd = true;
                    //filter by sent and this is received
                    if (switches[0] && item.isReceived()) {
                        willAdd = false;
                    }
                    //filter by received and this is sent
                    if (switches[1] && !item.isReceived()) {
                        willAdd = false;
                    }
                    BaseWalletManager wallet = WalletsMaster.getInstance(mContext).getCurrentWallet(mContext);

                    int confirms = item.getBlockHeight() == Integer.MAX_VALUE ? 0
                            : BRSharedPrefs.getLastBlockHeight(mContext, wallet.getCurrencyCode()) - item.getBlockHeight() + 1;
                    //complete
                    if (switches[2] && confirms >= CONFIRMED_BLOCKS_NUMBER) {
                        willAdd = false;
                    }

                    //pending
                    if (switches[3] && confirms < CONFIRMED_BLOCKS_NUMBER) {
                        willAdd = false;
                    }

                    if (willAdd) {
                        filteredList.add(item);
                    }
                }
            }
        }
        mItemFeed = filteredList;
        notifyDataSetChanged();
    }

    private class TxHolder extends RecyclerView.ViewHolder {
        private BaseTextView mTransactionDate;
        private BaseTextView mTransactionAmount;
        private BaseTextView mTransactionDetail;
        private Button mTransactionFailed;
        private ProgressBar mTransactionProgress;

        BaseTextView getTransactionDate() {
            return mTransactionDate;
        }

        BaseTextView getTransactionAmount() {
            return mTransactionAmount;
        }

        BaseTextView getTransactionDetail() {
            return mTransactionDetail;
        }

        Button getTransactionFailed() {
            return mTransactionFailed;
        }

        ProgressBar getTransactionProgress() {
            return mTransactionProgress;
        }

        TxHolder(View view) {
            super(view);
            mTransactionDate = view.findViewById(R.id.tx_date);
            mTransactionAmount = view.findViewById(R.id.tx_amount);
            mTransactionDetail = view.findViewById(R.id.tx_description);
            mTransactionFailed = view.findViewById(R.id.tx_failed_button);
            mTransactionProgress = view.findViewById(R.id.tx_progress);
        }
    }

}