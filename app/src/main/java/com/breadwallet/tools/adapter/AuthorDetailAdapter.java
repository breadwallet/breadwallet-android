package com.breadwallet.tools.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import com.breadwallet.R;

import java.util.List;

public class AuthorDetailAdapter extends BaseAdapter {
    private LayoutInflater mInflater;
    private List<String> mData;
    

    public AuthorDetailAdapter(Context context, List<String> data){
        this.mData = data;
        mInflater = LayoutInflater.from(context);
    }

    @Override
    public int getCount() {
        return (mData!=null)?mData.size():0;
    }

    @Override
    public Object getItem(int position) {
        return (mData!=null)?mData.get(position):null;
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        ViewHolder holder = null;
        if(convertView == null){
            convertView = mInflater.inflate(R.layout.author_info_detail_item_layout, parent, false);
            holder = new ViewHolder();
            holder.nameTv = convertView.findViewById(R.id.author_detail_name_tv);

            convertView.setTag(holder);
        } else {
            holder = (ViewHolder) convertView.getTag();
        }

        String name = mData.get(position);
        holder.nameTv.setText(name);

        return convertView;
    }

    private class ViewHolder {
        TextView nameTv;
    }
}
