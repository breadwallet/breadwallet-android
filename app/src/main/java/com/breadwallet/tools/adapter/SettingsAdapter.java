package com.breadwallet.tools.adapter;

import android.app.Activity;
import android.content.Context;
import android.support.annotation.LayoutRes;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import com.breadwallet.R;
import com.breadwallet.presenter.entities.BRSettingsItem;

import java.util.List;

import static com.breadwallet.R.layout.settings_list_item;
import static com.breadwallet.R.layout.settings_list_section;

/**
 * BreadWallet
 * <p/>
 * Created by Mihail Gutan on <mihail@breadwallet.com> 4/1/18.
 * Copyright (c) 2018 breadwallet LLC
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
public class SettingsAdapter extends ArrayAdapter<String> {

    private List<BRSettingsItem> mItems;
    private Context mContext;

    public SettingsAdapter(@NonNull Context context, @LayoutRes int resource, @NonNull List<BRSettingsItem> items) {
        super(context, resource);
        this.mItems = items;
        this.mContext = context;
    }

    @NonNull
    @Override
    public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {

        View v;
        BRSettingsItem item = mItems.get(position);
        LayoutInflater inflater = ((Activity) mContext).getLayoutInflater();

        if (item.isSection) {
            v = inflater.inflate(settings_list_section, parent, false);
        } else {
            v = inflater.inflate(settings_list_item, parent, false);
            TextView addon = v.findViewById(R.id.item_addon);

            if (!addon.getText().toString().isEmpty()) {
                addon.setVisibility(View.VISIBLE);
                addon.setText(item.addonText);
            }
            ImageButton icon = v.findViewById(R.id.setting_icon);
            if (item.iconResId != 0) {
                icon.setVisibility(View.VISIBLE);
                icon.setBackgroundResource(item.iconResId);
            }

            v.setOnClickListener(item.listener);
        }

        TextView title = v.findViewById(R.id.item_title);
        title.setText(item.title);

        return v;

    }

    @Override
    public int getCount() {
        return mItems == null ? 0 : mItems.size();
    }

    @Override
    public int getItemViewType(int position) {
        return super.getItemViewType(position);
    }
}