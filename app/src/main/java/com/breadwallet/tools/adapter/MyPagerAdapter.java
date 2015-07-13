package com.breadwallet.tools.adapter;

import android.os.Handler;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.view.View;

import com.breadwallet.presenter.fragments.MainFragment;
import com.breadwallet.presenter.fragments.MainFragmentQR;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by Mihail on 5/23/15.
 */
public class MyPagerAdapter extends FragmentPagerAdapter {
    private MainFragment mainFragment;
    private MainFragmentQR mainFragmentQR;
    private List<Fragment> fragments;
    private boolean available = true;
    private View main;
    private View mainQR;
    private static MyPagerAdapter adapter;

    public MyPagerAdapter(FragmentManager fragmentManager) {
        super(fragmentManager);

        this.fragments = new ArrayList<>();
        mainFragment = new MainFragment();
        mainFragmentQR = new MainFragmentQR();
        fragments.add(mainFragment);
        fragments.add(mainFragmentQR);
        adapter = this;
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

    public void showFragments(boolean b) {
        if (main == null) main = mainFragment.getView();
        if (mainQR == null) mainQR = mainFragmentQR.getView();
        if (available) {
            available = false;
            if (b) {
                new Handler().postDelayed(new Runnable() {

                    @Override
                    public void run() {
                        main.setVisibility(View.VISIBLE);
                        mainQR.setVisibility(View.VISIBLE);
                        available = true;
                    }
                }, 200);

            } else {
                new Handler().postDelayed(new Runnable() {

                    @Override
                    public void run() {
                        main.setVisibility(View.GONE);
                        mainQR.setVisibility(View.GONE);
                        available = true;
                    }
                }, 200);

            }
        }
    }

    public static MyPagerAdapter getAdapter() {
        return adapter;
    }

    public View getFragmentViewByIndex(int index) {
        return index == 0 ? main : mainQR;
    }
}
