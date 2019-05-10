package com.breadwallet.tools.adapter;

import android.content.Context;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import com.breadwallet.R;
import com.breadwallet.wallet.wallets.ela.data.TxProducerEntity;

import java.util.List;

public class TxProducerAdapter extends BaseAdapter {
    private Context mContext;
    private List<TxProducerEntity> mData;

    public TxProducerAdapter(Context context, List<TxProducerEntity> data){
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
        final VoteNodeAdapter.ViewHolder holder;
        if(convertView == null){
            convertView = View.inflate(mContext, R.layout.vote_node_item_layout, null);
            holder = new VoteNodeAdapter.ViewHolder();
            holder.mNameTv = convertView.findViewById(R.id.item_name_tv);
            convertView.setTag(holder);
        } else {
            holder = (VoteNodeAdapter.ViewHolder) convertView.getTag();
        }

        holder.mNameTv.setText(mData.get(position).Nickname);

        return convertView;
    }

    static class ViewHolder {
        TextView mNameTv;
    }
}
