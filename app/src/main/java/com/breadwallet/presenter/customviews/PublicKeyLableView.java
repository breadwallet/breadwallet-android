package com.breadwallet.presenter.customviews;

import android.content.Context;
import android.view.LayoutInflater;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.breadwallet.R;

public class PublicKeyLableView extends LinearLayout {

    private TextView mText;
    private ImageView mFlag;

    public PublicKeyLableView(Context context) {
        super(context);

        LayoutInflater.from(context).inflate(R.layout.publickey_label, this);
        mText = findViewById(R.id.pb_lable_text);
        mFlag = findViewById(R.id.pb_lable_flag);
    }

    public void setText(String text) {
        mText.setText(text);
    }

    public void setText(int resid) {
        mText.setText(resid);
    }

    public void setFlagVisibility(int visibility) {
        mFlag.setVisibility(visibility);
    }
}
