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
 *
 * @author Mihail Gutan
 */
public class CustomPagerAdapter extends FragmentPagerAdapter {
    public static final String TAG = "MyPagerAdapter";
    private MainFragment mainFragment;
    private MainFragmentQR mainFragmentQR;
    private List<Fragment> fragments;
    private boolean available = true;
    private View main;
    private View mainQR;
    private static CustomPagerAdapter adapter;

    public CustomPagerAdapter(FragmentManager fragmentManager) {
        super(fragmentManager);

        this.fragments = new ArrayList<>();
        mainFragment = new MainFragment();
        mainFragmentQR = new MainFragmentQR();
        fragments.add(mainFragment);
        fragments.add(mainFragmentQR);
        adapter = this;
    }

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

    public static CustomPagerAdapter getAdapter() {
        return adapter;
    }

    public View getFragmentViewByIndex(int index) {
        return index == 0 ? main : mainQR;
    }

    public MainFragment getMainFragment() {
        return mainFragment;
    }

    public MainFragmentQR getMainFragmentQR() {
        return mainFragmentQR;
    }
}
