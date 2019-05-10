package com.breadwallet.tools.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.breadwallet.R;
import com.breadwallet.did.AuthorInfo;
import com.breadwallet.tools.util.BRConstants;
import com.breadwallet.tools.util.StringUtil;

import java.util.List;

public class AuthorAdapter extends BaseAdapter {
    private LayoutInflater mInflater;
    private List<AuthorInfo>  mData;
    private Context mContext;

    public AuthorAdapter(Context context, List<AuthorInfo> data) {
        mData = data;
        mInflater = LayoutInflater.from(context);
        this.mContext = context;
    }

    @Override
    public int getCount() {
        return (mData!=null)?mData.size():0;
    }

    @Override
    public AuthorInfo getItem(int position) {
        return mData.get(position);
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

        AuthorInfo info = mData.get(i);
        holder.nameTv.setText(info.getAppName());
        String appId = info.getAppId();
        int iconResourceId = mContext.getResources().getIdentifier("unknow", BRConstants.DRAWABLE, mContext.getPackageName());
        if(!StringUtil.isNullOrEmpty(appId)) {
            if(appId.equals(BRConstants.REA_PACKAGE_ID)){
                iconResourceId = mContext.getResources().getIdentifier("redpackage", BRConstants.DRAWABLE, mContext.getPackageName());
            } else if(appId.equals(BRConstants.DEVELOPER_WEBSITE)){
                iconResourceId = mContext.getResources().getIdentifier("developerweb", BRConstants.DRAWABLE, mContext.getPackageName());
            } else if(appId.equals(BRConstants.HASH_ID)){
                iconResourceId = mContext.getResources().getIdentifier("hash", BRConstants.DRAWABLE, mContext.getPackageName());
            }
        }
        holder.iconTv.setImageDrawable(mContext.getDrawable(iconResourceId));

        return convertView;
    }

    private class ViewHolder {
        ImageView iconTv;
        TextView nameTv;
    }

}
