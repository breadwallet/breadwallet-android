package com.breadwallet.tools.adapter;

import android.app.Activity;
import android.content.Context;
import android.support.v7.widget.RecyclerView;
import android.text.format.DateUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.breadwallet.R;
import com.breadwallet.presenter.entities.CurrencyEntity;
import com.breadwallet.presenter.entities.TransactionListItem;
import com.breadwallet.tools.manager.SharedPreferencesManager;
import com.breadwallet.tools.sqlite.CurrencyDataSource;
import com.breadwallet.tools.sqlite.TransactionDataSource;
import com.breadwallet.tools.util.BRCurrency;
import com.breadwallet.tools.util.BRExchange;
import com.breadwallet.wallet.BRWalletManager;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import static android.text.format.DateUtils.MINUTE_IN_MILLIS;
import static android.widget.Adapter.IGNORE_ITEM_VIEW_TYPE;


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

public class TransactionListAdapter extends RecyclerView.Adapter<TransactionListAdapter.CustomViewHolder> {
    public static final String TAG = TransactionListAdapter.class.getName();

    private final Context mContext;
    private final int layoutResourceId;
    private List<TransactionListItem> itemFeed;

    public TransactionListAdapter(Context mContext, List<TransactionListItem> items) {
        itemFeed = items;
        if (itemFeed == null) itemFeed = new ArrayList<>();
        this.layoutResourceId = R.layout.tx_list_item;
        this.mContext = mContext;
    }

    public TransactionListItem getItemAtPos(int pos){
        return itemFeed.get(pos);
    }
    public List<TransactionListItem> getItems(){
        return itemFeed;
    }

    @Override
    public CustomViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        // inflate the layout
        LayoutInflater inflater = ((Activity) mContext).getLayoutInflater();
        View convertView = inflater.inflate(layoutResourceId, parent, false);
        return new CustomViewHolder(convertView);
    }

    @Override
    public void onBindViewHolder(CustomViewHolder holder, int position) {
        setTexts(holder, position);
    }

    @Override
    public int getItemViewType(int position) {
        return IGNORE_ITEM_VIEW_TYPE;
    }

    @Override
    public int getItemCount() {
        return itemFeed.size();
    }

    private void setTexts(CustomViewHolder convertView, int position) {

        TransactionListItem item = itemFeed.get(position);

        boolean received = item.getSent() == 0;
        convertView.sentReceived.setText(received ? "Received" : "Sent");
        convertView.toFrom.setText(received ? "from" : "to");
        int blockHeight = item.getBlockHeight();
        int confirms = blockHeight == Integer.MAX_VALUE ? 0 : SharedPreferencesManager.getLastBlockHeight(mContext) - blockHeight + 1;
        convertView.confirmation.setText((confirms >= 6) ? "Completed" : "Waiting to be confirmed");

        long satoshisAmount = received ? item.getReceived() : (item.getSent() - item.getReceived()) * -1;

        boolean isBTCPreferred = SharedPreferencesManager.getPreferredBTC(mContext);
        String iso = isBTCPreferred ? "BTC" : SharedPreferencesManager.getIso(mContext);

        convertView.amount.setText(BRCurrency.getFormattedCurrencyString(mContext, iso, BRExchange.getAmountFromSatoshis(mContext, iso, new BigDecimal(satoshisAmount))));

        //if it's 0 we use the current time.
        long timeStamp = item.getTimeStamp() == 0 ? System.currentTimeMillis() : item.getTimeStamp() * 1000;
        CharSequence timeSpan = DateUtils.getRelativeTimeSpanString(timeStamp, System.currentTimeMillis(), MINUTE_IN_MILLIS);

        convertView.timestamp.setText(timeSpan);

    }

    class CustomViewHolder extends RecyclerView.ViewHolder {
        public TextView sentReceived;
        public TextView amount;
        public TextView toFrom;
        public TextView account;
        public TextView confirmation;
        public TextView timestamp;
        public TextView comment;

        public CustomViewHolder(View view) {
            super(view);
            sentReceived = (TextView) view.findViewById(R.id.sent_received);
            amount = (TextView) view.findViewById(R.id.amount);
            toFrom = (TextView) view.findViewById(R.id.to_from);
            account = (TextView) view.findViewById(R.id.account);
            confirmation = (TextView) view.findViewById(R.id.confirmation);
            timestamp = (TextView) view.findViewById(R.id.timestamp);
            comment = (TextView) view.findViewById(R.id.comment);
        }
    }

}