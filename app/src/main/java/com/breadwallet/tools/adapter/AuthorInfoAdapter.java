package com.breadwallet.tools.adapter;

import android.content.Context;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;

import com.breadwallet.R;
import com.breadwallet.presenter.customviews.BaseTextView;
import com.breadwallet.presenter.customviews.SwitchButton;
import com.breadwallet.presenter.entities.AuthorInfoItem;
import com.breadwallet.tools.util.StringUtil;

import java.util.List;

public class AuthorInfoAdapter extends BaseAdapter {

    private Context mContext;
    private List<AuthorInfoItem> mData;

    public AuthorInfoAdapter(Context context, List<AuthorInfoItem> data){
        this.mContext = context;
        this.mData = data;
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
    public View getView(final int position, View convertView, ViewGroup parent) {
        final ViewHolder holder;
        if(convertView == null){
            convertView = View.inflate(mContext, R.layout.author_info_item_layout, null);
            holder = new ViewHolder();
            holder.mNameTv = convertView.findViewById(R.id.item_name_tv);
            holder.mRequireTv = convertView.findViewById(R.id.item_require_tv);
            holder.mCheckSb = convertView.findViewById(R.id.item_switch_btn);
            convertView.setTag(holder);
        } else {
            holder = (ViewHolder) convertView.getTag();
        }

        String name = mData.get(position).getName();
        holder.mNameTv.setText(name);
        String flag = mData.get(position).getFlag();
        if(!StringUtil.isNullOrEmpty(flag)){
            if(flag.equals("required")){
                holder.mRequireTv.setVisibility(View.VISIBLE);
                holder.mCheckSb.setVisibility(View.GONE);
            } else if(flag.equals("check")){
                holder.mRequireTv.setVisibility(View.GONE);
                holder.mCheckSb.setVisibility(View.VISIBLE);
            } else {
                holder.mRequireTv.setVisibility(View.GONE);
                holder.mCheckSb.setVisibility(View.VISIBLE);
            }
        }

        holder.mCheckSb.setOnCheckedChangeListener(null);
        holder.mCheckSb.setChecked(mData.get(position).isChecked());
        holder.mCheckSb.setOnCheckedChangeListener(new SwitchButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(SwitchButton view, boolean isChecked) {
                Log.d("checkedChanged", "position:"+position+" isChecked:"+isChecked);
                mData.get(position).setChecked(isChecked);
            }
        });

        return convertView;
    }

    static class ViewHolder {
        BaseTextView mNameTv;
        BaseTextView mRequireTv;
        SwitchButton mCheckSb;
    }


}
