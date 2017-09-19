package com.breadwallet.presenter.fragments;

import android.app.Activity;
import android.app.Fragment;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.view.ViewPager;
import android.util.Log;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.breadwallet.R;
import com.breadwallet.presenter.entities.TxItem;
import com.breadwallet.tools.adapter.TransactionPagerAdapter;
import com.breadwallet.tools.animation.BRAnimator;

import java.util.List;


/**
 * BreadWallet
 * <p>
 * Created by Mihail Gutan <mihail@breadwallet.com> on 6/29/15.
 * Copyright (c) 2016 breadwallet LLC
 * <p>
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 * <p>
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 * <p>
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */

public class FragmentTransactionDetails extends Fragment {
    private static final String TAG = FragmentTransactionDetails.class.getName();

    public TextView mTitle;
    public LinearLayout backgroundLayout;
    private ViewPager txViewPager;
    private TransactionPagerAdapter txPagerAdapter;
    private List<TxItem> items;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        // The last two arguments ensure LayoutParams are inflated
        // properly.
        View rootView = inflater.inflate(R.layout.fragment_transaction_details, container, false);
        mTitle = (TextView) rootView.findViewById(R.id.title);
        backgroundLayout = (LinearLayout) rootView.findViewById(R.id.background_layout);
        txViewPager = (ViewPager) rootView.findViewById(R.id.tx_list_pager);
        txViewPager.setOnPageChangeListener(new ViewPager.OnPageChangeListener() {
            public void onPageScrollStateChanged(int state) {
            }

            public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {
            }

            public void onPageSelected(int position) {
                // Check if this is the page you want.
            }
        });
        txPagerAdapter = new TransactionPagerAdapter(getChildFragmentManager(), items);
        txViewPager.setAdapter(txPagerAdapter);
        txViewPager.setOffscreenPageLimit(5);
        int margin = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 16 * 2, getResources().getDisplayMetrics());
        txViewPager.setPageMargin(-margin);
        int pos = getArguments().getInt("pos");
        txViewPager.setCurrentItem(pos, false);

        return rootView;
    }


    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        final ViewTreeObserver observer = txViewPager.getViewTreeObserver();
        observer.addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
            @Override
            public void onGlobalLayout() {
                observer.removeGlobalOnLayoutListener(this);
                BRAnimator.animateBackgroundDim(backgroundLayout, false);
                BRAnimator.animateSignalSlide(txViewPager, false, null);
            }
        });

    }

    @Override
    public void onStop() {
        super.onStop();
        final Activity app = getActivity();
        BRAnimator.animateBackgroundDim(backgroundLayout, true);
        BRAnimator.animateSignalSlide(txViewPager, true, new BRAnimator.OnSlideAnimationEnd() {
            @Override
            public void onAnimationEnd() {
                if (app != null)
                    app.getFragmentManager().popBackStack();
                else
                    Log.e(TAG, "onAnimationEnd: app is null");
            }
        });
    }

    @Override
    public void onResume() {
        super.onResume();
    }

    @Override
    public void onPause() {
        super.onPause();
    }

    public void setItems(List<TxItem> items) {
        this.items = items;
    }
}