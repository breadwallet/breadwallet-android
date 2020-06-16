package com.breadwallet.ui.wallet;

import android.content.Context;
import android.os.Handler;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.AttributeSet;
import android.view.KeyEvent;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import androidx.annotation.Nullable;
import com.breadwallet.R;
import com.breadwallet.legacy.presenter.customviews.BRButton;
import kotlinx.coroutines.channels.SendChannel;

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
public class BRSearchBar extends androidx.appcompat.widget.Toolbar {
    private static final String TAG = BRSearchBar.class.getName();

    private static final int SHOW_KEYBOARD_DELAY = 300;

    private EditText searchEdit;
    private BRButton sentFilter;
    private BRButton receivedFilter;
    private BRButton pendingFilter;
    private BRButton completedFilter;
    private BRButton cancelButton;
    private SendChannel<WalletScreen.E> output = null;

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

        searchEdit = findViewById(R.id.search_edit);
        sentFilter = findViewById(R.id.sent_filter);
        receivedFilter = findViewById(R.id.received_filter);
        pendingFilter = findViewById(R.id.pending_filter);
        completedFilter = findViewById(R.id.complete_filter);
        cancelButton = findViewById(R.id.cancel_button);

        setListeners();

        searchEdit.postDelayed(() -> {
            InputMethodManager keyboard = (InputMethodManager)
                    getContext().getSystemService(Context.INPUT_METHOD_SERVICE);
            keyboard.showSoftInput(searchEdit, 0);
        }, SHOW_KEYBOARD_DELAY); // delay to make it run when coming back from lock screen
    }


    private void setListeners() {
        searchEdit.setOnFocusChangeListener((view, hasFocus) -> {
            //TODO: What is this button?
            /*if (!hasFocus) {
                if (breadActivity.tool_bar_flipper != null) {
                    breadActivity.tool_bar_flipper.setDisplayedChild(0);
                    clearSwitches();
                }
            }*/
        });

        cancelButton.setOnClickListener(view -> {
            if (output != null) {
                output.offer(WalletScreen.E.OnSearchDismissClicked.INSTANCE);
            }
        });

        searchEdit.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence sequence, int start, int before, int count) {
                if (output != null) {
                    output.offer(new WalletScreen.E.OnQueryChanged(sequence.toString()));
                }
            }

            @Override
            public void afterTextChanged(Editable s) {

            }
        });
        searchEdit.setOnKeyListener((view, keyCode, event) -> {
            if ((event.getAction() == KeyEvent.ACTION_DOWN) && (keyCode == KeyEvent.KEYCODE_ENTER)) {
                if (output != null) {
                    output.offer(WalletScreen.E.OnSearchDismissClicked.INSTANCE);
                }
                return true;
            }
            return false;
        });

        sentFilter.setOnClickListener(view -> output.offer(WalletScreen.E.OnFilterSentClicked.INSTANCE));
        receivedFilter.setOnClickListener(view -> output.offer(WalletScreen.E.OnFilterReceivedClicked.INSTANCE));
        pendingFilter.setOnClickListener(view -> output.offer(WalletScreen.E.OnFilterPendingClicked.INSTANCE));
        completedFilter.setOnClickListener(view -> output.offer(WalletScreen.E.OnFilterCompleteClicked.INSTANCE));
    }

    public void onShow(boolean showKeyboard) {
        final InputMethodManager keyboard = (InputMethodManager)
                getContext().getSystemService(Context.INPUT_METHOD_SERVICE);
        if (showKeyboard) {
            new Handler().postDelayed(() -> {
                searchEdit.requestFocus();
                keyboard.showSoftInput(searchEdit, 0);
            }, SHOW_KEYBOARD_DELAY);
        } else {
            keyboard.hideSoftInputFromWindow(searchEdit.getWindowToken(), 0);
        }
    }

    public void setEventOutput(final SendChannel<WalletScreen.E> output) {
        this.output = output;
    }

    private static final int BUTTON_ACTIVE_TYPE = 3;
    private static final int BUTTON_INACTIVE_TYPE = 2;

    public void render(final WalletScreen.M model) {
        final int sentType = sentFilter.getType();
        final int receivedType = receivedFilter.getType();
        final int pendingType = pendingFilter.getType();
        final int completedType = completedFilter.getType();

        if (model.getFilterSent() && sentType != BUTTON_ACTIVE_TYPE) {
            sentFilter.setType(BUTTON_ACTIVE_TYPE);
        } else if (!model.getFilterSent() && sentType != BUTTON_INACTIVE_TYPE) {
            sentFilter.setType(BUTTON_INACTIVE_TYPE);
        }

        if (model.getFilterReceived() && receivedType != BUTTON_ACTIVE_TYPE) {
            receivedFilter.setType(BUTTON_ACTIVE_TYPE);
        } else if (!model.getFilterReceived() && receivedType != BUTTON_INACTIVE_TYPE) {
            receivedFilter.setType(BUTTON_INACTIVE_TYPE);
        }

        if (model.getFilterPending() && pendingType != BUTTON_ACTIVE_TYPE) {
            pendingFilter.setType(BUTTON_ACTIVE_TYPE);
        } else if (!model.getFilterPending() && pendingType != BUTTON_INACTIVE_TYPE) {
            pendingFilter.setType(BUTTON_INACTIVE_TYPE);
        }

        if (model.getFilterComplete() && completedType != BUTTON_ACTIVE_TYPE) {
            completedFilter.setType(BUTTON_ACTIVE_TYPE);
        } else if (!model.getFilterComplete() && completedType != BUTTON_INACTIVE_TYPE) {
            completedFilter.setType(BUTTON_INACTIVE_TYPE);
        }
    }
}
