package com.breadwallet.tools.adapter;

import android.app.Activity;
import android.content.Context;
import android.graphics.Point;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.breadwallet.R;
import com.breadwallet.presenter.entities.CurrencyEntity;
import com.breadwallet.presenter.entities.TransactionListItem;
import com.breadwallet.tools.manager.SharedPreferencesManager;
import com.breadwallet.tools.util.BRStringFormatter;
import com.breadwallet.tools.util.Utils;
import com.breadwallet.wallet.BRWalletManager;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Currency;
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

public class TransactionListAdapter extends ArrayAdapter<TransactionListItem> {
    public static final String TAG = TransactionListAdapter.class.getName();

    private final Context mContext;
    private final int layoutResourceId;
    private TextView textViewItem;
    private List<TransactionListItem> itemFeed;

    public TransactionListAdapter(Context mContext, List<TransactionListItem> items) {
        super(mContext, R.layout.tx_list_item);
        itemFeed = items;
        if (itemFeed == null) itemFeed = new ArrayList<>();
        this.layoutResourceId = R.layout.tx_list_item;
        this.mContext = mContext;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {

        if (convertView == null) {
            // inflate the layout
            LayoutInflater inflater = ((Activity) mContext).getLayoutInflater();
            convertView = inflater.inflate(layoutResourceId, parent, false);
        }
        setTexts(convertView, position);

        return convertView;

    }

    @Override
    public int getCount() {
        return itemFeed.size();
    }

    @Override
    public int getItemViewType(int position) {
        return IGNORE_ITEM_VIEW_TYPE;
    }

    private void setTexts(View convertView, int position) {
        TextView sentReceived = (TextView) convertView.findViewById(R.id.sent_received);
        TextView amount = (TextView) convertView.findViewById(R.id.amount);
        TextView toFrom = (TextView) convertView.findViewById(R.id.to_from);
        TextView account = (TextView) convertView.findViewById(R.id.account);
        TextView confirmation = (TextView) convertView.findViewById(R.id.confirmation);
        TextView timestamp = (TextView) convertView.findViewById(R.id.timestamp);
        TextView comment = (TextView) convertView.findViewById(R.id.comment);

        TransactionListItem item = itemFeed.get(position);

        boolean received = item.getSent() == 0;
        sentReceived.setText(received ? "Received" : "Sent");
        int blockHeight = item.getBlockHeight();
        int confirms = blockHeight == Integer.MAX_VALUE ? 0 : SharedPreferencesManager.getLastBlockHeight(mContext) - blockHeight + 1;
        confirmation.setText((confirms >= 6) ? "Completed" : "Waiting to be confirmed");


//        if (item.getSent() > 0 && item.getSent() == item.getReceived()) {
//            sentReceivedTextView.setBackgroundResource(R.drawable.unconfirmed_label);
//            sentReceivedTextView.setText(R.string.moved);
//            sentReceivedTextView.setTextColor(unconfirmedColor);
//        } else if (blockHeight != Integer.MAX_VALUE && confirms >= 6) {
//            sentReceivedTextView.setBackgroundResource(received ? R.drawable.received_label : R.drawable.sent_label);
//            sentReceivedTextView.setText(received ? R.string.received : R.string.sent);
//            sentReceivedTextView.setTextColor(received ? receivedColor : sentColor);
//        } else {
//            sentReceivedTextView.setBackgroundResource(R.drawable.unconfirmed_label);
//            sentReceivedTextView.setTextColor(unconfirmedColor);
//            if (!BRWalletManager.getInstance(activity).transactionIsVerified(item.getHexId())) {
//                sentReceivedTextView.setText(R.string.unverified);
//            } else {
//                int confsNr = confirms >= 0 && confirms <= 5 ? confirms : 0;
//                String message = confsNr == 0 ? activity.getString(R.string.nr_confirmations0) :
//                        (confsNr == 1 ? activity.getString(R.string.nr_confirmations1) : String.format(activity.getString(R.string.nr_confirmations), confsNr));
//
//                sentReceivedTextView.setText(message);
//            }
//        }

//        long itemTimeStamp = item.getTimeStamp();
//        dateTextView.setText(itemTimeStamp != 0 ? Utils.getFormattedDateFromLong(itemTimeStamp * 1000) : Utils.getFormattedDateFromLong(System.currentTimeMillis()));
//
//        long satoshisAmount = received ? item.getReceived() : (item.getSent() - item.getReceived()) * -1;
//
//        bitsTextView.setText(BRStringFormatter.getFormattedCurrencyString("BTC", satoshisAmount));
//        dollarsTextView.setText(String.format("(%s)", BRStringFormatter.getExchangeForAmount(SharedPreferencesManager.getRate(activity),
//                SharedPreferencesManager.getIso(activity), new BigDecimal(satoshisAmount), activity)));
//        long satoshisAfterTx = item.getBalanceAfterTx();
//
//        bitsTotalTextView.setText(BRStringFormatter.getFormattedCurrencyString("BTC", satoshisAfterTx));
//        dollarsTotalTextView.setText(String.format("(%s)", BRStringFormatter.getExchangeForAmount(SharedPreferencesManager.getRate(activity),
//                SharedPreferencesManager.getIso(activity), new BigDecimal(satoshisAfterTx), activity)));

    }

}