package com.breadwallet.ui.wallet;

import android.content.Context;
import android.os.Handler;
import android.support.annotation.Nullable;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.AttributeSet;
import android.view.KeyEvent;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;

import com.breadwallet.R;
import com.breadwallet.presenter.customviews.BRButton;
import com.breadwallet.ui.wallet.model.TxFilter;

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

    private static final int SHOW_KEYBOARD_DELAY = 300;

    private enum Filter {
        SENT,
        RECEIVED,
        PENDING,
        COMPLETED
    }

    private EditText searchEdit;
    private BRButton sentFilter;
    private BRButton receivedFilter;
    private BRButton pendingFilter;
    private BRButton completedFilter;
    private BRButton cancelButton;
    private WalletActivity breadActivity;
    private FilterListener mFilterListener;
    private boolean[] mFilterSwitches = new boolean[Filter.values().length];

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
        if (getContext() instanceof FilterListener) {
            mFilterListener = (FilterListener) getContext();
        } else {
            throw new IllegalStateException("BRSearchBar's host must implement " + FilterListener.class.getName());
        }

        breadActivity = (WalletActivity) getContext();
        searchEdit = findViewById(R.id.search_edit);
        sentFilter = findViewById(R.id.sent_filter);
        receivedFilter = findViewById(R.id.received_filter);
        pendingFilter = findViewById(R.id.pending_filter);
        completedFilter = findViewById(R.id.complete_filter);
        cancelButton = findViewById(R.id.cancel_button);

        clearSwitches();
        setListeners();


        searchEdit.requestFocus();
        searchEdit.postDelayed(() -> {
            InputMethodManager keyboard = (InputMethodManager)
                    getContext().getSystemService(Context.INPUT_METHOD_SERVICE);
            keyboard.showSoftInput(searchEdit, 0);
        }, SHOW_KEYBOARD_DELAY); // delay to make it run when coming back from lock screen
    }

    private void updateFilterButtonsUI(boolean[] switches) {
        sentFilter.setType(switches[Filter.SENT.ordinal()] ? 3 : 2);
        receivedFilter.setType(switches[Filter.RECEIVED.ordinal()] ? 3 : 2);
        pendingFilter.setType(switches[Filter.PENDING.ordinal()] ? 3 : 2);
        completedFilter.setType(switches[Filter.COMPLETED.ordinal()] ? 3 : 2);
        mFilterListener.onFilterChanged(getFilter(searchEdit.getText().toString()));
    }

    private void setListeners() {
        searchEdit.setOnFocusChangeListener((view, hasFocus) -> {
            if (!hasFocus) {
                if (breadActivity.mBarFlipper != null) {
                    breadActivity.mBarFlipper.setDisplayedChild(0);
                    clearSwitches();
                }
            }
        });

        cancelButton.setOnClickListener(view -> {
            searchEdit.setText("");
            breadActivity.resetFlipper();
            clearSwitches();
            onShow(false);
        });

        searchEdit.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence sequence, int start, int before, int count) {
                mFilterListener.onFilterChanged(getFilter(sequence.toString()));
            }

            @Override
            public void afterTextChanged(Editable s) {

            }
        });
        searchEdit.setOnKeyListener((view, keyCode, event) -> {
            if ((event.getAction() == KeyEvent.ACTION_DOWN) && (keyCode == KeyEvent.KEYCODE_ENTER)) {
                onShow(false);
                return true;
            }
            return false;
        });

        sentFilter.setOnClickListener(view -> {
            mFilterSwitches[Filter.SENT.ordinal()] = !mFilterSwitches[Filter.SENT.ordinal()];
            mFilterSwitches[Filter.RECEIVED.ordinal()] = false;
            updateFilterButtonsUI(mFilterSwitches);

        });

        receivedFilter.setOnClickListener(view -> {
            mFilterSwitches[Filter.RECEIVED.ordinal()] = !mFilterSwitches[Filter.RECEIVED.ordinal()];
            mFilterSwitches[Filter.SENT.ordinal()] = false;
            updateFilterButtonsUI(mFilterSwitches);
        });

        pendingFilter.setOnClickListener(view -> {
            mFilterSwitches[Filter.PENDING.ordinal()] = !mFilterSwitches[Filter.PENDING.ordinal()];
            mFilterSwitches[Filter.COMPLETED.ordinal()] = false;
            updateFilterButtonsUI(mFilterSwitches);
        });

        completedFilter.setOnClickListener(view -> {
            mFilterSwitches[Filter.COMPLETED.ordinal()] = !mFilterSwitches[Filter.COMPLETED.ordinal()];
            mFilterSwitches[Filter.PENDING.ordinal()] = false;
            updateFilterButtonsUI(mFilterSwitches);
        });
    }

    public void clearSwitches() {
        for (int i = 0; i < Filter.values().length; i++) {
            mFilterSwitches[i] = false;
        }
    }

    public void onShow(boolean showKeyboard) {

        final InputMethodManager keyboard = (InputMethodManager)
                getContext().getSystemService(Context.INPUT_METHOD_SERVICE);
        clearSwitches();
        updateFilterButtonsUI(mFilterSwitches);
        if (showKeyboard) {
            new Handler().postDelayed(() -> {
                searchEdit.requestFocus();
                keyboard.showSoftInput(searchEdit, 0);
            }, SHOW_KEYBOARD_DELAY);

        } else {
            keyboard.hideSoftInputFromWindow(searchEdit.getWindowToken(), 0);
            mFilterListener.onFilterChanged(new TxFilter());
        }
    }

    private TxFilter getFilter(String query) {
        return new TxFilter(query.toLowerCase().trim(), mFilterSwitches[Filter.SENT.ordinal()],
                mFilterSwitches[Filter.RECEIVED.ordinal()], mFilterSwitches[Filter.PENDING.ordinal()],
                mFilterSwitches[Filter.COMPLETED.ordinal()]);
    }

    interface FilterListener {

        /**
         * Filter list of transactions by the given Filter.
         *
         * @param filter The filter to be applied.
         */
        void onFilterChanged(TxFilter filter);

    }
}
