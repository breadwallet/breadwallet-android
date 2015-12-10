package com.breadwallet.tools.adapter;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Point;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.breadwallet.R;
import com.breadwallet.presenter.activities.MainActivity;
import com.breadwallet.presenter.entities.CurrencyEntity;
import com.breadwallet.presenter.fragments.FragmentCurrency;

/**
 * BreadWallet
 * <p/>
 * Created by Mihail on 7/27/15.
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

public class TransactionListAdapter extends ArrayAdapter<CurrencyEntity> {
    public static final String TAG = TransactionListAdapter.class.getName();

    Context mContext;
    int layoutResourceId;
    public TextView textViewItem;
    public Point displayParameters = new Point();
//    public static TransactionListAdapter currencyListAdapter;

    public TransactionListAdapter(Context mContext, int layoutResourceId) {

        super(mContext, layoutResourceId);

        this.layoutResourceId = layoutResourceId;
        this.mContext = mContext;
        ((Activity)mContext).getWindowManager().getDefaultDisplay().getSize(displayParameters);
//        currencyListAdapter = this;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {

        SharedPreferences settings = mContext.getSharedPreferences(MainActivity.PREFS_NAME, 0);
        final int tmp = settings.getInt(FragmentCurrency.POSITION, 0);
        if (convertView == null) {
            // inflate the layout
            LayoutInflater inflater = ((Activity) mContext).getLayoutInflater();
            convertView = inflater.inflate(layoutResourceId, parent, false);
        }
        // get the TextView and then set the text (item name) and tag (item ID) values
        textViewItem = null;
        textViewItem = (TextView) convertView.findViewById(R.id.currency_item_text);
        textViewItem.setText(this.getItem(position).codeAndName);
        ImageView checkMark = (ImageView) convertView.findViewById(R.id.currency_checkmark);

        if (position == tmp) {
            checkMark.setVisibility(View.VISIBLE);
        } else {
            checkMark.setVisibility(View.GONE);
        }
        return convertView;

    }

    @Override
    public int getItemViewType(int position) {
        return IGNORE_ITEM_VIEW_TYPE;
    }


}