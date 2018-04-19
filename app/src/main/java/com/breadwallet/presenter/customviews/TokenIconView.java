package com.breadwallet.presenter.customviews;

import android.content.Context;
import android.util.AttributeSet;
import android.widget.RelativeLayout;

import com.breadwallet.R;

public class TokenIconView extends RelativeLayout {

    public TokenIconView(Context context) {
        super(context);
        init();
    }

    public TokenIconView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();

    }

    public TokenIconView(Context context, AttributeSet attrs, int defaultStyle) {
        super(context, attrs, defaultStyle);
        init();
    }


    private void init() {
        inflate(getContext(), R.layout.token_icon_view, this);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
    }
}
