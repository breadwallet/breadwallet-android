package com.breadwallet.tools.security;

import android.app.Activity;
import android.content.Context;
import android.content.DialogInterface;
import android.security.keystore.KeyPermanentlyInvalidatedException;

import com.breadwallet.R;
import com.breadwallet.presenter.activities.IntroActivity;
import com.breadwallet.presenter.activities.MainActivity;
import com.breadwallet.tools.animation.BRAnimator;
import com.google.firebase.crash.FirebaseCrash;

import java.security.InvalidKeyException;

/**
 * BreadWallet
 * <p/>
 * Created by Mihail Gutan on <mihail@breadwallet.com> 7/31/17.
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
public class BRErrorPipe {

    private static android.app.AlertDialog dialog;

    public static void parseKeyStoreError(final Context context, Exception e, String alias, boolean report) {
        if (report) FirebaseCrash.report(e);

        if (e instanceof KeyPermanentlyInvalidatedException) {
            BRErrorPipe.showKeyStoreDialog(context, "KeyStore Error: " + alias, "Your Breadwallet encrypted data was recently invalidated because your Android lock screen was disabled.", context.getString(R.string.ok), null,
                    new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int which) {
                            dialog.cancel();
                        }
                    }, null, new DialogInterface.OnDismissListener() {
                        @Override
                        public void onDismiss(DialogInterface dialogInterface) {
                            if (context instanceof IntroActivity) {
                                if (BRAnimator.checkTheMultipressingAvailability()) {
                                    ((IntroActivity) context).showRecoverWalletFragment();
                                }
                            }
                        }
                    });
        } else if (e instanceof InvalidKeyException) {
            showKeyStoreDialog(context, "KeyStore Error", "Failed to load KeyStore(" + alias + "). Please try again later or enter your phrase to recover your Breadwallet now.", "recover now", "try later",
                    context instanceof IntroActivity ?
                            new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog, int which) {
                                    if (BRAnimator.checkTheMultipressingAvailability()) {
                                        ((IntroActivity) context).showRecoverWalletFragment();
                                    }
                                }
                            } : null, new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int which) {
                            ((Activity) context).finish();
                        }
                    },
                    null);
        } else {
            showKeyStoreDialog(context,
                    "KeyStore Error",
                    "Failed to load KeyStore:\n" + e.getClass().getSimpleName() + "\n" + e.getMessage(),
                    "Close", null,
                    new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            dialog.dismiss();
                            ((Activity) context).finish();
                        }
                    },
                    null,
                    new DialogInterface.OnDismissListener() {
                        @Override
                        public void onDismiss(DialogInterface dialog) {
                            ((Activity) context).finish();
                        }
                    });
        }
    }

    public static void parseError(final Context context, String message, Exception ex, final boolean critical) {
        FirebaseCrash.report(ex);
        showKeyStoreDialog(context,
                "Internal error:",
                message,
                "Close", null,
                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                        if (critical)
                            ((Activity) context).finish();
                    }
                },
                null,
                new DialogInterface.OnDismissListener() {
                    @Override
                    public void onDismiss(DialogInterface dialog) {
                        if (critical)
                            ((Activity) context).finish();
                    }
                });
    }

    public static void showKeyStoreDialog(Context context, final String title, final String message, final String posButton, final String negButton,
                                          final DialogInterface.OnClickListener posButtonListener,
                                          final DialogInterface.OnClickListener negButtonListener,
                                          final DialogInterface.OnDismissListener dismissListener) {
        Activity app = (Activity) context;
        if (app == null) app = MainActivity.app;
        if (app == null) app = IntroActivity.app;
        if (app == null) {
            return;
        }
        final Activity finalApp = app;
        if (finalApp != null)
            finalApp.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    if (finalApp != null) {
                        if (dialog != null && dialog.isShowing()) {
                            if (dialog.getOwnerActivity() != null && !dialog.getOwnerActivity().isDestroyed())
                                dialog.dismiss();
                            else
                                return;
                        }
                        dialog = new android.app.AlertDialog.Builder(finalApp).
                                setTitle(title)
                                .setMessage(message)
                                .setPositiveButton(posButton, posButtonListener)
                                .setNegativeButton(negButton, negButtonListener)
                                .setOnDismissListener(dismissListener)
                                .setIcon(android.R.drawable.ic_dialog_alert)
                                .show();
                    }
                }
            });
    }
}
