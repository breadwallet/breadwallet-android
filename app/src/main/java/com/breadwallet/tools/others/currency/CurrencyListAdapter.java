package com.breadwallet.tools.others.currency;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.breadwallet.R;
import com.breadwallet.presenter.activities.MainActivity;
import com.breadwallet.presenter.fragments.allsettings.settings.FragmentCurrency;

/**
 * Created by Mihail on 7/27/15.
 */
public class CurrencyListAdapter extends ArrayAdapter<String> {
    public static final String TAG = "CurrencyListAdapter";

    Context mContext;
    int layoutResourceId;
    public int selectedIndex = -1;

    public CurrencyListAdapter(Context mContext, int layoutResourceId) {

        super(mContext, layoutResourceId);

        this.layoutResourceId = layoutResourceId;
        this.mContext = mContext;
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
        // get the TextView and then set the text (item name) and tag (item ID) values
        TextView textViewItem = (TextView) convertView.findViewById(R.id.currency_item_text);
        textViewItem.setText(this.getItem(position));

        return convertView;

    }

    public void setSelectedIndex(int index) {
        selectedIndex = index;
    }

}