package com.breadwallet.presenter.activities;

import android.content.Context;
import android.support.annotation.NonNull;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.breadwallet.R;

import java.util.List;

public class PublicKeyAdapter extends ArrayAdapter<PublicKeyAdapter.PublicKey> {

    private int mResourceId;

    PublicKeyAdapter(@NonNull Context context, int resource, @NonNull List<PublicKey> objects) {
        super(context, resource, objects);
        mResourceId = resource;
    }

    @NonNull
    @Override
    public View getView(int position, View convertView, @NonNull ViewGroup parent) {
        PublicKey publicKey = getItem(position);
        View view;
        ViewHolder viewHolder;

        if (convertView == null) {
            view = LayoutInflater.from(getContext()).inflate(mResourceId, null);

            viewHolder = new ViewHolder();
            viewHolder.flag = view.findViewById(R.id.pb_lable_flag);
            viewHolder.publicKey = view.findViewById(R.id.pb_lable_text);
            view.setTag(viewHolder);
        } else {
            view = convertView;
            viewHolder = (ViewHolder) view.getTag();
        }

        assert publicKey != null;
        viewHolder.flag.setVisibility(publicKey.mSigned ? View.VISIBLE : View.GONE);
        viewHolder.publicKey.setText(publicKey.mPublicKey);

        return view;
    }

    class ViewHolder{
        ImageView flag;
        TextView publicKey;
    }

    static public class PublicKey {
        String mPublicKey;
        boolean mSigned;
    }
}
