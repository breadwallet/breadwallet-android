package com.breadwallet.tools.adapter;

import android.app.Activity;
import android.content.Context;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.breadwallet.R;
import com.breadwallet.presenter.customviews.BRButton;
import com.breadwallet.tools.util.BRCurrency;
import com.breadwallet.tools.util.BRExchange;

import java.util.ArrayList;
import java.util.List;

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

public class CurAdapter extends RecyclerView.Adapter<CurAdapter.CustomViewHolder> {
    public static final String TAG = CurAdapter.class.getName();

    private final Context mContext;
    private final int layoutResourceId;
    private List<String> itemFeed;

    public CurAdapter(Context mContext, List<String> items) {
        itemFeed = items;
        if (itemFeed == null) itemFeed = new ArrayList<>();
        this.layoutResourceId = R.layout.spinner_item;
        this.mContext = mContext;
    }

    public String getItemAtPos(int pos) {
        return itemFeed.get(pos);
    }

    public List<String> getItems() {
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

        String item = itemFeed.get(position);
        convertView.button.setText(String.format("%s(%s)", item, BRCurrency.getSymbolByIso(mContext, item)));

    }

    class CustomViewHolder extends RecyclerView.ViewHolder {
        public BRButton button;

        public CustomViewHolder(View view) {
            super(view);
            button = (BRButton) view.findViewById(R.id.watch_list_layout);
        }
    }

}