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
import com.breadwallet.tools.manager.SharedPreferencesManager;
import com.breadwallet.tools.util.Utils;

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

public class CurrencyListAdapter extends ArrayAdapter<CurrencyEntity> {
    public static final String TAG = CurrencyListAdapter.class.getName();

    private final Context mContext;
    private final int layoutResourceId;
    private TextView textViewItem;
    private final Point displayParameters = new Point();
    public static CurrencyListAdapter currencyListAdapter;

    public CurrencyListAdapter(Context mContext) {

        super(mContext, R.layout.currency_list_item);

        this.layoutResourceId = R.layout.currency_list_item;
        this.mContext = mContext;
        ((Activity) mContext).getWindowManager().getDefaultDisplay().getSize(displayParameters);
        currencyListAdapter = this;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {

        final int tmp = SharedPreferencesManager.getCurrencyListPosition(mContext);
        if (convertView == null) {
            // inflate the layout
            LayoutInflater inflater = ((Activity) mContext).getLayoutInflater();
            convertView = inflater.inflate(layoutResourceId, parent, false);
        }
        // get the TextView and then set the text (item name) and tag (item ID) values
        textViewItem = null;
        textViewItem = (TextView) convertView.findViewById(R.id.currency_item_text);
        Utils.overrideFonts(textViewItem);
        textViewItem.setText(this.getItem(position).codeAndName);
        ImageView checkMark = (ImageView) convertView.findViewById(R.id.currency_checkmark);

        if (position == tmp) {
            checkMark.setVisibility(View.VISIBLE);
        } else {
            checkMark.setVisibility(View.GONE);
        }
        normalizeTextView();
        return convertView;

    }

    @Override
    public int getItemViewType(int position) {
        return IGNORE_ITEM_VIEW_TYPE;
    }

    private boolean isTextSizeAcceptable(TextView textView) {
        textView.measure(0, 0);
        int textWidth = textView.getMeasuredWidth();
        int checkMarkWidth = 76 + 20;
        return (textWidth <= (displayParameters.x - checkMarkWidth));
    }

    private boolean normalizeTextView() {
        int count = 0;
//        Log.d(TAG, "Normalizing the text view !!!!!!");
        while (!isTextSizeAcceptable(textViewItem)) {
            count++;
            float textSize = textViewItem.getTextSize();
//            Log.e(TAG, "The text size is: " + String.valueOf(textSize));
            textViewItem.setTextSize(TypedValue.COMPLEX_UNIT_PX, textSize - 2);
            this.notifyDataSetChanged();
        }
        return (count > 0);
    }

}