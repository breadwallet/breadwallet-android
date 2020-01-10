package com.breadwallet.legacy.presenter.customviews;

import android.annotation.SuppressLint;
import android.content.Context;
import androidx.annotation.Nullable;
import android.util.AttributeSet;
import android.widget.EditText;

import java.util.ArrayList;
import java.util.List;

/**
 * BreadWallet
 * <p/>
 * Created by Mihail Gutan on <mihail@breadwallet.com> 5/3/17.
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
@SuppressLint("AppCompatCustomView") // we don't need to support older versions
public class BREdit extends EditText {
    private static final String TAG = BREdit.class.getName();
    private List<EditTextEventListener> mEditTextEventListeners = new ArrayList<>();

    public enum EditTextEvent {
        CUT,
        PASTE,
        COPY
    }

    public BREdit(Context context) {
        super(context);
    }

    public BREdit(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
    }

    public BREdit(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    public BREdit(Context context, @Nullable AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
    }

    /**
     * Here you can catch paste, copy and cut events
     */
    @Override
    public boolean onTextContextMenuItem(int id) {
        boolean consumed = super.onTextContextMenuItem(id);
        switch (id) {
            case android.R.id.cut:
                fireEditTextEventListeners(EditTextEvent.CUT);
                break;
            case android.R.id.paste:
                fireEditTextEventListeners(EditTextEvent.PASTE);
                break;
            case android.R.id.copy:
                fireEditTextEventListeners(EditTextEvent.COPY);
                break;
        }
        return consumed;
    }

    public void fireEditTextEventListeners(EditTextEvent editTextEvent) {
        for (EditTextEventListener editTextEventListener : mEditTextEventListeners) {
            editTextEventListener.onEvent(editTextEvent);
        }
    }

    public interface EditTextEventListener {
        void onEvent(EditTextEvent editTextEvent);
    }

    public void addEditTextEventListener(EditTextEventListener editTextEventListener) {
        if (!mEditTextEventListeners.contains(editTextEventListener)) {
            mEditTextEventListeners.add(editTextEventListener);
        }
    }

    public void removeEditTextEventListener(EditTextEventListener editTextEventListener) {
        mEditTextEventListeners.remove(editTextEventListener);
    }

}
