package com.breadwallet.presenter.customviews;

import android.content.Context;
import android.os.Handler;
import android.support.annotation.Nullable;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.AttributeSet;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;

import com.breadwallet.R;
import com.breadwallet.presenter.activities.BreadActivity;
import com.breadwallet.tools.animation.SpringAnimator;
import com.breadwallet.tools.manager.TxManager;

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
    private BRButton sentFilter;
    private BRButton receivedFilter;
    private BRButton pendingFilter;
    private BRButton completedFilter;
    private BRButton cancelButton;
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
        sentFilter = (BRButton) findViewById(R.id.sent_filter);
        receivedFilter = (BRButton) findViewById(R.id.received_filter);
        pendingFilter = (BRButton) findViewById(R.id.pending_filter);
        completedFilter = (BRButton) findViewById(R.id.complete_filter);
        cancelButton = (BRButton) findViewById(R.id.cancel_button);

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
                TxManager.getInstance().updateTxList(breadActivity);
            }
        }).start();

    }

    private void updateFilterButtonsUI(boolean[] switches) {
        sentFilter.setType(switches[0] ? 3 : 2);
        receivedFilter.setType(switches[1] ? 3 : 2);
        pendingFilter.setType(switches[2] ? 3 : 2);
        completedFilter.setType(switches[3] ? 3 : 2);
        if (TxManager.getInstance().adapter != null)
            TxManager.getInstance().adapter.filterBy(searchEdit.getText().toString(), filterSwitches);
    }

    private void setListeners() {
        searchEdit.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View v, boolean hasFocus) {
                if (!hasFocus) {
                    if (breadActivity.barFlipper != null) {
                        breadActivity.barFlipper.setDisplayedChild(0);
                        clearSwitches();
                    }
                }
            }
        });

        cancelButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                searchEdit.setText("");
                breadActivity.barFlipper.setDisplayedChild(0);
                clearSwitches();
                onShow(false);
            }
        });

        searchEdit.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (TxManager.getInstance().adapter != null)
                    TxManager.getInstance().adapter.filterBy(s.toString(), filterSwitches);
            }

            @Override
            public void afterTextChanged(Editable s) {

            }
        });

        sentFilter.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                filterSwitches[0] = !filterSwitches[0];
                filterSwitches[1] = false;
                updateFilterButtonsUI(filterSwitches);

            }
        });

        receivedFilter.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                filterSwitches[1] = !filterSwitches[1];
                filterSwitches[0] = false;
                updateFilterButtonsUI(filterSwitches);
            }
        });

        pendingFilter.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                filterSwitches[2] = !filterSwitches[2];
                filterSwitches[3] = false;
                updateFilterButtonsUI(filterSwitches);
            }
        });

        completedFilter.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                filterSwitches[3] = !filterSwitches[3];
                filterSwitches[2] = false;
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

    public void onShow(boolean b) {
        final InputMethodManager keyboard = (InputMethodManager)
                getContext().getSystemService(Context.INPUT_METHOD_SERVICE);
        if (b) {
            clearSwitches();
            updateFilterButtonsUI(filterSwitches);
            new Handler().postDelayed(new Runnable() {
                @Override
                public void run() {
                    searchEdit.requestFocus();
                    keyboard.showSoftInput(searchEdit, 0);
                }
            }, 400);
        } else {
            keyboard.hideSoftInputFromWindow(searchEdit.getWindowToken(), 0);
        }
    }

}