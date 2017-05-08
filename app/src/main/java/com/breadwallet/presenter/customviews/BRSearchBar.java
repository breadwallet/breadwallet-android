package com.breadwallet.presenter.customviews;

import android.content.Context;
import android.support.annotation.Nullable;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;

import com.breadwallet.R;
import com.breadwallet.presenter.activities.BreadActivity;
import com.breadwallet.tools.animation.SpringAnimator;

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

        searchEdit.requestFocus();
        searchEdit.postDelayed(new Runnable() {

            @Override
            public void run() {
                InputMethodManager keyboard = (InputMethodManager)
                        getContext().getSystemService(Context.INPUT_METHOD_SERVICE);
                keyboard.showSoftInput(searchEdit, 0);
            }
        }, 200); //use 300 to make it run when coming back from lock screen

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


}