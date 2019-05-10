package com.breadwallet.tools.adapter;

import android.content.Context;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import com.breadwallet.R;
import com.breadwallet.vote.ProducerEntity;
import com.breadwallet.vote.ProducersEntity;

import java.util.List;

public class VoteNodeAdapter extends BaseAdapter {

    private Context mContext;
    private List<ProducerEntity> mData;

    public VoteNodeAdapter(Context context, List<ProducerEntity> data){
        mContext = context;
        mData = data;
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
        final ViewHolder holder;
        if(convertView == null){
            convertView = View.inflate(mContext, R.layout.vote_node_item_layout, null);
            holder = new ViewHolder();
            holder.mNameTv = convertView.findViewById(R.id.item_name_tv);
            convertView.setTag(holder);
        } else {
            holder = (ViewHolder) convertView.getTag();
        }

        holder.mNameTv.setText(mData.get(position).Nickname);

        return convertView;
    }

    static class ViewHolder {
        TextView mNameTv;
    }
}
