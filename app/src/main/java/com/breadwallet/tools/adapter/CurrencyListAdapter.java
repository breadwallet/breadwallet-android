package com.breadwallet.tools.adapter;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Point;
import android.util.Log;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.breadwallet.R;
import com.breadwallet.presenter.activities.MainActivity;
import com.breadwallet.presenter.fragments.allsettings.settings.FragmentCurrency;

import java.util.ArrayList;
import java.util.List;

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

public class CurrencyListAdapter extends ArrayAdapter<String> {
    public static final String TAG = "CurrencyListAdapter";

    Context mContext;
    int layoutResourceId;
    public int selectedIndex = -1;
    public TextView textViewItem;
    public Point displayParameters = new Point();
    public List<Integer> scaledItemsIDs = new ArrayList<>();

    public CurrencyListAdapter(Context mContext, int layoutResourceId) {

        super(mContext, layoutResourceId);

        this.layoutResourceId = layoutResourceId;
        this.mContext = mContext;
        MainActivity.app.getWindowManager().getDefaultDisplay().getSize(displayParameters);
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {

        /*
         * The convertView argument is essentially a "ScrapView" as described is Lucas post
         * http://lucasr.org/2012/04/05/performance-tips-for-androids-listview/
         * It will have a non-null value when ListView is asking you recycle the row layout.
         * So, when convertView is not null, you should simply update its contents instead of inflating a new row layout.
         */
        SharedPreferences settings = MainActivity.app.getSharedPreferences(MainActivity.PREFS_NAME, 0);
        final int tmp = settings.getInt(FragmentCurrency.POSITION, 0);
        if (convertView == null) {
            // inflate the layout
            LayoutInflater inflater = ((Activity) mContext).getLayoutInflater();
            convertView = inflater.inflate(layoutResourceId, parent, false);
        }
        // get the TextView and then set the text (item name) and tag (item ID) values
        textViewItem = null;
        textViewItem = (TextView) convertView.findViewById(R.id.currency_item_text);
        textViewItem.setText(this.getItem(position));
        ImageView checkMark = (ImageView) convertView.findViewById(R.id.currency_checkmark);

        if (selectedIndex >= 0) {
            if (selectedIndex == position) {
                checkMark.setVisibility(View.VISIBLE);
            } else {
                checkMark.setVisibility(View.GONE);
            }
        } else if (position == tmp) {
            checkMark.setVisibility(View.VISIBLE);
        } else {
            checkMark.setVisibility(View.GONE);
        }
        if (scaledItemsIDs.contains(new Integer(position))) {
            normalizeTextView();
            Log.w(TAG, "The list contains the item and is being normalized!");
        }

        return convertView;

    }

    @Override
    public int getItemViewType(int position) {
        return IGNORE_ITEM_VIEW_TYPE;
    }

    public void setSelectedIndex(int index) {
        selectedIndex = index;
    }

    public boolean isTextSizeAcceptable(TextView textView) {
        textView.measure(0, 0);
        int textWidth = textView.getMeasuredWidth();
        int checkMarkWidth = 76 + 20;
//        Log.e(TAG, "______________________________________");
//        Log.w(TAG, "The pointer x: " + displayParameters.x);
//        Log.w(TAG, "The pointer y: " + displayParameters.y);
//        Log.w(TAG, "The text width: " + textWidth);
//        Log.w(TAG, "The checkMarkWidth: " + checkMarkWidth);
//        Log.e(TAG, "^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^");

        return (textWidth > (displayParameters.x - checkMarkWidth)) ? false : true;
    }

    public boolean normalizeTextView() {
        int count = 0;
//        Log.d(TAG, "Normalizing the text view !!!!!!");
        while (!isTextSizeAcceptable(textViewItem)) {
            count++;
            float textSize = textViewItem.getTextSize();
//            Log.e(TAG, "The text size is: " + String.valueOf(textSize));
            textViewItem.setTextSize(TypedValue.COMPLEX_UNIT_PX, textSize - 2);
            this.notifyDataSetChanged();
        }
        return (count > 0) ? true : false;
    }

}