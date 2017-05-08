package com.breadwallet.presenter.customviews;

import android.content.Context;
import android.support.annotation.Nullable;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;
import android.widget.EditText;

import com.breadwallet.R;
import com.breadwallet.presenter.activities.BreadActivity;
import com.breadwallet.tools.animation.SpringAnimator;
import com.breadwallet.tools.util.Utils;

/**
 * BreadWallet
 * <p/>
 * Created by Mihail Gutan on <mihail@breadwallet.com> 5/8/17.
 * Copyright (c) 2017 breadwallet LLC
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
public class BRSearchBar extends android.support.v7.widget.Toolbar {

    private static final String TAG = BRSearchBar.class.getName();

    private EditText searchEdit;
    //    private LinearLayout filterButtonsLayout;
    private BreadButton sentFilter;
    private BreadButton receivedFilter;
    private BreadButton pendingFilter;
    private BreadButton completedFilter;
    private BreadButton cancelButton;
    private BreadActivity breadActivity;

    public boolean[] filterSwitches = new boolean[4];


    public BRSearchBar(Context context) {
        super(context);
        init();
    }

    public BRSearchBar(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public BRSearchBar(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private void init() {
        inflate(getContext(), R.layout.search_bar, this);
        breadActivity = (BreadActivity) getContext();
        searchEdit = (EditText) findViewById(R.id.search_edit);
//        filterButtonsLayout = (LinearLayout) findViewById(R.id.filter_buttons_layout);
        sentFilter = (BreadButton) findViewById(R.id.sent_filter);
        receivedFilter = (BreadButton) findViewById(R.id.received_filter);
        pendingFilter = (BreadButton) findViewById(R.id.pending_filter);
        completedFilter = (BreadButton) findViewById(R.id.complete_filter);
        cancelButton = (BreadButton) findViewById(R.id.cancel_button);

        clearSwitches();

        setListeners();

        Utils.hideKeyboard(getContext());
        new Thread(new Runnable() {
            @Override
            public void run() {
                breadActivity.updateTxList();
            }
        }).start();

    }

    private void updateFilterButtonsUI(boolean[] switches) {
        sentFilter.setBackgroundResource(switches[0] ? R.drawable.button_secondary_blue_stroke : R.drawable.button_secondary_gray_stroke);
        sentFilter.setTextColor(switches[0] ? getContext().getColor(R.color.dark_blue) : getContext().getColor(R.color.light_gray));
        receivedFilter.setBackgroundResource(switches[1] ? R.drawable.button_secondary_blue_stroke : R.drawable.button_secondary_gray_stroke);
        receivedFilter.setTextColor(switches[1] ? getContext().getColor(R.color.dark_blue) : getContext().getColor(R.color.light_gray));
        pendingFilter.setBackgroundResource(switches[2] ? R.drawable.button_secondary_blue_stroke : R.drawable.button_secondary_gray_stroke);
        pendingFilter.setTextColor(switches[2] ? getContext().getColor(R.color.dark_blue) : getContext().getColor(R.color.light_gray));
        completedFilter.setBackgroundResource(switches[3] ? R.drawable.button_secondary_blue_stroke : R.drawable.button_secondary_gray_stroke);
        completedFilter.setTextColor(switches[3] ? getContext().getColor(R.color.dark_blue) : getContext().getColor(R.color.light_gray));
        if (breadActivity.adapter != null)
            breadActivity.adapter.filterBy(searchEdit.getText().toString(), filterSwitches);
    }

    private void setListeners() {
//        searchEdit.setOnFocusChangeListener(new View.OnFocusChangeListener() {
//            @Override
//            public void onFocusChange(View v, boolean hasFocus) {
//                if (!hasFocus) {
//                    searchManager.animateSearchVisibility(false);
//                } else {
//                    InputMethodManager imm = (InputMethodManager) getContext().getSystemService(Context.INPUT_METHOD_SERVICE);
//                    imm.toggleSoftInput(InputMethodManager.SHOW_FORCED, 0);
//                }
//            }
//        });

        cancelButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                SpringAnimator.springView(v);
                breadActivity.barFlipper.setDisplayedChild(0);
                clearSwitches();

            }
        });

        searchEdit.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (breadActivity.adapter != null)
                    breadActivity.adapter.filterBy(s.toString(), filterSwitches);
                Log.e(TAG, "onTextChanged: " + s.toString());
            }

            @Override
            public void afterTextChanged(Editable s) {

            }
        });

        sentFilter.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                SpringAnimator.springView(v);
                filterSwitches[0] = !filterSwitches[0];
                updateFilterButtonsUI(filterSwitches);

            }
        });

        receivedFilter.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                SpringAnimator.springView(v);
                filterSwitches[1] = !filterSwitches[1];
                updateFilterButtonsUI(filterSwitches);
            }
        });

        pendingFilter.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                SpringAnimator.springView(v);
                filterSwitches[2] = !filterSwitches[2];
                updateFilterButtonsUI(filterSwitches);
            }
        });

        completedFilter.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                SpringAnimator.springView(v);
                filterSwitches[3] = !filterSwitches[3];
                updateFilterButtonsUI(filterSwitches);
            }
        });
    }

    public void clearSwitches() {
        filterSwitches[0] = false;
        filterSwitches[1] = false;
        filterSwitches[2] = false;
        filterSwitches[3] = false;
    }


//    private class BRSearchManager {
//
//        private float searchEditXScale;
//        private float primaryYScale;
//        private float secondaryYScale;
//        private float priceChangeYScale;
//        private float filterButtonsLayoutXScale;
//
//
//        public void init() {
//            searchEditXScale = searchEdit.getScaleX();
//            primaryYScale = primaryPrice.getScaleY();
//            secondaryYScale = secondaryPrice.getScaleY();
//            priceChangeYScale = priceChange.getScaleY();
//            filterButtonsLayoutXScale = filterButtonsLayout.getScaleX();
//            filterButtonsLayout.setScaleX(0);
//            filterButtonsLayout.setVisibility(View.GONE);
//
//        }
//
//
//        void animateSearchVisibility(boolean b) {
//            int duration = 300;
//            final int durationShort = 200;
//            if (b) {
//                searchIcon.setBackgroundResource(R.drawable.ic_close_black_24dp);
//                searchEdit.setVisibility(View.VISIBLE);
//                searchEdit.setText("");
//                searchEdit.setScaleX(0);
//                searchEdit.setPivotX(searchEdit.getX());
//                filterButtonsLayout.setVisibility(View.VISIBLE);
//                filterButtonsLayout.setPivotX(filterButtonsLayout.getX());
//
//                searchEdit.animate().scaleX(searchEditXScale).setDuration(duration).setInterpolator(new OvershootInterpolator(0.7f)).setListener(new AnimatorListenerAdapter() {
//                    @Override
//                    public void onAnimationEnd(Animator animation) {
//                        super.onAnimationEnd(animation);
//                        searchEdit.setScaleX(searchEditXScale);
//                        searchEdit.requestFocus();
//                        filterButtonsLayout.animate().scaleX(filterButtonsLayoutXScale).setDuration(durationShort * 2).setListener(new AnimatorListenerAdapter() {
//                            @Override
//                            public void onAnimationEnd(Animator animation) {
//                                super.onAnimationEnd(animation);
//                                filterButtonsLayout.setScaleX(filterButtonsLayoutXScale);
//                            }
//                        });
//                        searchManager.clearSwitches();
//                        updateFilterButtonsUI(searchManager.filterSwitches);
//                    }
//                });
//                primaryPrice.animate().scaleY(0).setDuration(durationShort).setListener(new AnimatorListenerAdapter() {
//                    @Override
//                    public void onAnimationEnd(Animator animation) {
//                        super.onAnimationEnd(animation);
//                        primaryPrice.setScaleY(0);
//                    }
//                });
//                secondaryPrice.animate().scaleY(0).setDuration(durationShort).setListener(new AnimatorListenerAdapter() {
//                    @Override
//                    public void onAnimationEnd(Animator animation) {
//                        super.onAnimationEnd(animation);
//                        secondaryPrice.setScaleY(0);
//                    }
//                });
//                priceChange.animate().scaleY(0).setDuration(durationShort).setListener(new AnimatorListenerAdapter() {
//                    @Override
//                    public void onAnimationEnd(Animator animation) {
//                        super.onAnimationEnd(animation);
//                        priceChange.setScaleY(0);
//                    }
//                });
//
//
//            } else {
//                searchIcon.setBackgroundResource(R.drawable.ic_search_black_24dp);
//                primaryPrice.setVisibility(View.VISIBLE);
//                secondaryPrice.setVisibility(View.VISIBLE);
//                priceChange.setVisibility(View.VISIBLE);
//                primaryPrice.animate().scaleY(primaryYScale).setDuration(duration).setListener(new AnimatorListenerAdapter() {
//                    @Override
//                    public void onAnimationEnd(Animator animation) {
//                        super.onAnimationEnd(animation);
//                        primaryPrice.setScaleY(primaryYScale);
//                    }
//                });
//                secondaryPrice.animate().scaleY(secondaryYScale).setDuration(duration).setListener(new AnimatorListenerAdapter() {
//                    @Override
//                    public void onAnimationEnd(Animator animation) {
//                        super.onAnimationEnd(animation);
//                        secondaryPrice.setScaleY(secondaryYScale);
//                    }
//                });
//                priceChange.animate().scaleY(priceChangeYScale).setDuration(duration).setListener(new AnimatorListenerAdapter() {
//                    @Override
//                    public void onAnimationEnd(Animator animation) {
//                        super.onAnimationEnd(animation);
//                        priceChange.setScaleY(priceChangeYScale);
//                    }
//                });
//
//                searchEdit.animate().scaleX(0).setDuration(durationShort).setInterpolator(null).setListener(new AnimatorListenerAdapter() {
//                    @Override
//                    public void onAnimationEnd(Animator animation) {
//                        super.onAnimationEnd(animation);
//                        searchEdit.setVisibility(View.GONE);
//                    }
//                });
//
//                filterButtonsLayout.animate().scaleX(0).setDuration(durationShort / 2).setListener(new AnimatorListenerAdapter() {
//                    @Override
//                    public void onAnimationEnd(Animator animation) {
//                        super.onAnimationEnd(animation);
//                        filterButtonsLayout.setScaleX(0);
//                        filterButtonsLayout.setVisibility(View.GONE);
//                    }
//                });
//
//            }
//        }
//    }
}
