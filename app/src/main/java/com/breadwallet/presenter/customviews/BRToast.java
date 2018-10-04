package com.breadwallet.presenter.customviews;

import android.app.Activity;
import android.content.Context;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import com.breadwallet.BreadApp;
import com.breadwallet.R;
import com.breadwallet.tools.manager.BRSharedPrefs;

/**
 * BreadWallet
 * <p/>
 * Created by Mihail Gutan on <mihail@breadwallet.com> 3/20/17.
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
public class BRToast {
    private static boolean mCustomToastAvailable = true;
    private static String mOldMessage;
    private static Toast mToast;

    /**
     * Shows a custom mToast using the given string as a paramater,
     *
     * @param message the message to be shown in the custom mToast
     */

    public static void showCustomToast(Context context, String message, int yOffSet, int duration, int layoutDrawable) {
        if (!(context instanceof Activity)) {
            context = BreadApp.getBreadContext();
        }
        if (context == null || ((Activity) context).isDestroyed()) {
            return;
        }
        if (mToast == null) {
            mToast = new Toast(context);
        }
        if (mCustomToastAvailable || !mOldMessage.equals(message)) {
            mOldMessage = message;
            mCustomToastAvailable = false;
            LayoutInflater inflater = ((Activity) context).getLayoutInflater();
            View layout = inflater.inflate(R.layout.toast, (ViewGroup) ((Activity) context).findViewById(R.id.toast_layout_root));
            if (layoutDrawable != 0) {
                layout.setBackgroundResource(layoutDrawable);
            }
            TextView text = layout.findViewById(R.id.toast_text);
            text.setText(message);
            if (yOffSet == -1 || yOffSet == 0) {
                yOffSet = BRSharedPrefs.getScreenHeight(context) / 2;
            }
            mToast.setGravity(Gravity.TOP, 0, yOffSet);
            mToast.setDuration(duration);
            mToast.setView(layout);
            mToast.show();
        }
    }

    public static void cancelToast() {
        if (mToast != null) {
            mToast.cancel();
        }
    }

    public static boolean isToastShown() {
        return mToast != null && mToast.getView() != null && mToast.getView().isShown();
    }
}
