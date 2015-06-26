package com.breadwallet.adapter;

import android.support.v4.view.PagerAdapter;
import android.view.View;
import android.view.ViewGroup;

/**
 * Created by Mihail on 6/25/15.
 */
public class ParallaxPagerAdapter extends PagerAdapter {
    public static final String TAG = "ParallaxPagerAdapter";

    @Override
    public void destroyItem(ViewGroup container, int position, Object object) {
        super.destroyItem(container, position, object);
    }


    @Override
    public int getCount() {
        return 0;
    }

    @Override
    public boolean isViewFromObject(View view, Object object) {
        return false;
    }
}
