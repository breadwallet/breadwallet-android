package com.breadwallet.presenter.customviews;

import android.content.Context;
import android.content.res.TypedArray;
import android.util.AttributeSet;
import android.widget.ListView;

import com.breadwallet.R;

public class MaxHeightLv extends ListView {
    private float mMaxHeight = 0;

    public MaxHeightLv(Context context) {
        super(context);
    }

    public MaxHeightLv(Context context, AttributeSet attrs) {
        super(context, attrs);
        initHeight(context, attrs, -1);
    }

    public MaxHeightLv(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        initHeight(context, attrs, defStyleAttr);
    }

    private void initHeight(Context context, AttributeSet attrs, int defStyleAttr){
        TypedArray array = context.obtainStyledAttributes(attrs, R.styleable.ConstraintHeightListView, 0, defStyleAttr);
        int count = array.getIndexCount();
        for (int i = 0; i < count; i++) {
            int type = array.getIndex(i);
            if (type == R.styleable.ConstraintHeightListView_maxHeight) {
                mMaxHeight = array.getDimension(type, -1);
            }
        }

    }

    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int specSize = MeasureSpec.getSize(heightMeasureSpec);
        if (mMaxHeight <= specSize && mMaxHeight > -1) {
            heightMeasureSpec = MeasureSpec.makeMeasureSpec(Float.valueOf(mMaxHeight).intValue(),
                    MeasureSpec.AT_MOST);
        }
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
    }

}
