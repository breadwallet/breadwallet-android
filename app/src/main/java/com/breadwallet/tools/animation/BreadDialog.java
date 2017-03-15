package com.breadwallet.tools.animation;

import android.app.Activity;
import android.content.Context;
import android.content.DialogInterface;
import android.support.annotation.NonNull;
import android.util.Log;

/**
 * BreadWallet
 * <p/>
 * Created by Mihail Gutan on <mihail@breadwallet.com> 3/15/17.
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
public class BreadDialog {
    private static final String TAG = BreadDialog.class.getName();


    /**
     * Safe from any threads
     *
     * @param app needs to be activity
     */
    public static void showCustomDialog(@NonNull final Context app, @NonNull final String title, @NonNull final String message,
                                        @NonNull final String posButton, final String negButton, final DialogInterface.OnClickListener posListener,
                                        final DialogInterface.OnClickListener negListener, final DialogInterface.OnDismissListener dismissListener, final int iconRes) {
        if (app == null) {
            Log.e(TAG, "showCustomDialog: FAILED, context is null");
            return;
        }
        ((Activity) app).runOnUiThread(new Runnable() {
            @Override
            public void run() {
                android.app.AlertDialog.Builder builder = new android.app.AlertDialog.Builder(app)
                        .setTitle(title)
                        .setMessage(message)
                        .setPositiveButton(posButton, posListener);
                if (iconRes != 0)
                    builder.setIcon(iconRes);

                if (negListener != null && negButton != null)
                    builder.setNegativeButton(negButton, negListener);
                if (dismissListener != null)
                    builder.setOnDismissListener(dismissListener);

                builder.show();
            }
        });
    }
}
