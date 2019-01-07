package com.breadwallet.presenter.activities.did;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.breadwallet.R;

import java.util.List;

public class AuthorAdapter extends BaseAdapter {
    private LayoutInflater mInflater;
    private Context mContext;
    private List  mData;

    public AuthorAdapter(Context context) {
        this.mContext = context;
        mInflater = LayoutInflater.from(context);
    }

    @Override
    public int getCount() {
        return 3;
    }

    @Override
    public Object getItem(int position) {
        return null;
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(int i, View convertView, ViewGroup parent) {
        ViewHolder holder = null;
        if(convertView == null){
            convertView = mInflater.inflate(R.layout.did_auth_item_layout, parent, false);
            holder = new ViewHolder();
            holder.iconTv = convertView.findViewById(R.id.auth_app_icon);
            holder.nameTv = convertView.findViewById(R.id.auth_app_name);

            convertView.setTag(holder);
        } else {
            holder = (ViewHolder) convertView.getTag();
        }

        holder.iconTv.setImageResource(R.mipmap.ic_launcher);
        holder.nameTv.setText("Elastos Developer website");

        return convertView;
    }

    private class ViewHolder {
        ImageView iconTv;
        TextView nameTv;
    }

}
