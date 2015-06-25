package com.breadwallet.listeners;

import android.support.v4.view.ViewPager;

import com.breadwallet.activities.MainActivity;

/**
 * Created by Mihail on 6/23/15.
 */
public class MyOnPageChangeListener implements ViewPager.OnPageChangeListener {
    public static final String TAG = "MyOnPageChangeListener";
    private MainActivity app;

    public MyOnPageChangeListener(){
        app = MainActivity.getApp();
    }
    @Override
    public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {

    }

    @Override
    public void onPageSelected(int position) {
        app.setPagerIndicator(position);
    }

    @Override
    public void onPageScrollStateChanged(int state) {

    }
}
