package com.breadwallet.tools.adapter;

import android.app.Fragment;
import android.app.FragmentManager;
import android.os.Handler;
import android.support.v13.app.FragmentPagerAdapter;
import android.view.View;

import com.breadwallet.presenter.activities.MainActivity;
import com.breadwallet.presenter.fragments.MainFragment;
import com.breadwallet.presenter.fragments.MainFragmentQR;

import java.util.ArrayList;
import java.util.List;

/**
 * BreadWallet
 * <p/>
 * Created by Mihail on 5/23/15.
 * Copyright (c) 2015 Mihail Gutan <mihail@breadwallet.com>
 * <p/>
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 * <p/>
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 * <p/>
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */

public class CustomPagerAdapter extends FragmentPagerAdapter {
    private static final String TAG = CustomPagerAdapter.class.getName();
    public final MainFragment mainFragment;
    public final MainFragmentQR mainFragmentQR;
    private final List<Fragment> fragments;
    private View main;
    private View mainQR;
    public static CustomPagerAdapter adapter;

    public CustomPagerAdapter(FragmentManager fragmentManager) {
        super(fragmentManager);

        this.fragments = new ArrayList<>();
        mainFragment = new MainFragment();
        mainFragmentQR = new MainFragmentQR();
        fragments.add(mainFragment);
        fragments.add(mainFragmentQR);
        adapter = this;

    }

//    public void swipe(int leftRight){
//        if(leftRight == 0){
////            this.
//        } else {
//
//        }
//    }

//    on

    @Override
    public int getCount() {
        return fragments.size();
    }

    @Override
    public Fragment getItem(int position) {
        return fragments.get(position);
    }


    /**
     * Show the fragments or hide, which is specified by the boolean parameter b
     *
     * @param b parameter that specifies to show or to hide the fragments
     */
    public void showFragments(boolean b) {
//        Log.w(TAG, "Warning showFragments called with variable: " + b);
        if (main == null) main = mainFragment.getView();
        if (mainQR == null) mainQR = mainFragmentQR.getView();
        if (b) {
            new Handler().postDelayed(new Runnable() {

                @Override
                public void run() {
                    if (main != null)
                        main.setVisibility(View.VISIBLE);
                    if (mainQR != null)
                        mainQR.setVisibility(View.VISIBLE);
                    MainActivity.pageIndicator.setVisibility(View.VISIBLE);
                }
            }, 200);

        } else {
            new Handler().postDelayed(new Runnable() {

                @Override
                public void run() {
                    if (main != null)
                        main.setVisibility(View.GONE);
                    if (mainQR != null)
                        mainQR.setVisibility(View.GONE);
                    MainActivity.pageIndicator.setVisibility(View.GONE);
                }
            }, 200);

        }
    }

}

