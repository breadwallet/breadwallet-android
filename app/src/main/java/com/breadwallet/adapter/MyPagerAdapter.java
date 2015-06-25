package com.breadwallet.adapter;

import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;

import com.breadwallet.fragments.MainFragment;
import com.breadwallet.fragments.MainFragmentQR;

/**
 * Created by Mihail on 5/23/15.
 */
public class MyPagerAdapter extends FragmentPagerAdapter {
    final int PAGE_COUNT = 2;
    private MainFragment mainFragment;
    private MainFragmentQR mainFragmentQR;

    public MyPagerAdapter(FragmentManager fragmentManager) {
        super(fragmentManager);
    }

    // Returns total number of pages
    @Override
    public int getCount() {
        return PAGE_COUNT;
    }

    // Returns the walletFragment to display for that page
    @Override
    public Fragment getItem(int position) {
        switch (position) {
            case 0:
                mainFragment = new MainFragment();
                return mainFragment;
            case 1:
                mainFragmentQR = new MainFragmentQR();
                return mainFragmentQR;
            default:
                return null;
        }
    }

    public MainFragment getMainFragment() {
        return mainFragment;
    }

    public MainFragmentQR getMainFragmentQR() {
        return mainFragmentQR;
    }

}
