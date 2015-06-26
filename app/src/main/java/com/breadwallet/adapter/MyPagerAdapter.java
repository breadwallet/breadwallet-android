package com.breadwallet.adapter;

import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;

import com.breadwallet.fragments.MainFragment;
import com.breadwallet.fragments.MainFragmentQR;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by Mihail on 5/23/15.
 */
public class MyPagerAdapter extends FragmentPagerAdapter {
    private MainFragment mainFragment;
    private MainFragmentQR mainFragmentQR;
    private List<Fragment> fragments;

    public MyPagerAdapter(FragmentManager fragmentManager) {
        super(fragmentManager);

        this.fragments = new ArrayList<>();
        mainFragment = new MainFragment();
        mainFragmentQR = new MainFragmentQR();
        fragments.add(mainFragment);
        fragments.add(mainFragmentQR);
    }

    // Returns total number of pages
    @Override
    public int getCount() {
        return fragments.size();
    }

    // Returns the walletFragment to display for that page
    @Override
    public Fragment getItem(int position) {
        return fragments.get(position);
    }

    public MainFragment getMainFragment() {
        return mainFragment;
    }

    public MainFragmentQR getMainFragmentQR() {
        return mainFragmentQR;
    }

}
