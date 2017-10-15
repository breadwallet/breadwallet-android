package com.breadwallet.tools.security;

import android.app.Activity;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.security.keystore.KeyPermanentlyInvalidatedException;
import android.util.Log;

import com.breadwallet.BreadApp;
import com.breadwallet.R;
import com.breadwallet.presenter.activities.camera.ScanQRActivity;
import com.breadwallet.presenter.activities.intro.IntroActivity;
import com.breadwallet.presenter.activities.intro.RecoverActivity;
import com.breadwallet.tools.animation.BRAnimator;
import com.breadwallet.tools.manager.BRReportsManager;
import com.breadwallet.tools.manager.BRSharedPrefs;
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

    private static final String TAG = BRErrorPipe.class.getName();

    private static android.app.AlertDialog dialog;

//    public static void parseKeyStoreError(final Context context, Exception e, String alias, boolean report) {
//        if (e instanceof KeyPermanentlyInvalidatedException) {
//            BRErrorPipe.showKeyStoreDialog(context, context.getString(R.string.Alert_keystore_title_android) + ": " + alias, context.getString(R.string.Alert_keystore_invalidated_android), context.getString(R.string.Button_ok), null,
//                    new DialogInterface.OnClickListener() {
//                        public void onClick(DialogInterface dialog, int which) {
//                            dialog.cancel();
//                        }
//                    }, null, new DialogInterface.OnDismissListener() {
//                        @Override
//                        public void onDismiss(DialogInterface dialogInterface) {
//                            if (context instanceof Activity) {
//                                if (!BRAnimator.isClickAllowed()) return;
//                                Intent intent = new Intent(context, RecoverActivity.class);
//                                context.startActivity(intent);
//                                ((Activity) context).overridePendingTransition(R.anim.enter_from_right, R.anim.exit_to_left);
//                                BRSharedPrefs.putGreetingsShown(context, true);
//
//                            }
//                        }
//                    });
//            report = false;
//        } else if (e instanceof InvalidKeyException) {
//            showKeyStoreDialog(context, context.getString(R.string.Alert_keystore_title_android), "Failed to load KeyStore(" + alias + "). Please try again later or enter your phrase to recover your Bread now.", context.getString(R.string.AccessibilityLabels_close), null,
//                    null, new DialogInterface.OnClickListener() {
//                        public void onClick(DialogInterface dialog, int which) {
//                            ((Activity) context).finish();
//                        }
//                    },
//                    null);
//        } else {
//            showKeyStoreDialog(context,
//                    context.getString(R.string.Alert_keystore_title_android),
//                    "Failed to load KeyStore:\n" + e.getClass().getSimpleName() + "\n" + e.getMessage(),
//                    context.getString(R.string.AccessibilityLabels_close), null,
//                    new DialogInterface.OnClickListener() {
//                        @Override
//                        public void onClick(DialogInterface dialog, int which) {
//                            dialog.dismiss();
//                            ((Activity) context).finish();
//                        }
//                    },
//                    null,
//                    new DialogInterface.OnDismissListener() {
//                        @Override
//                        public void onDismiss(DialogInterface dialog) {
//                            ((Activity) context).finish();
//                        }
//                    });
//        }
//        if (report) {
//            BRReportsManager.reportBug(e);
//        }
//    }
//
//    public static void parseError(final Context context, String message, Exception ex, final boolean critical) {
//        BRReportsManager.reportBug(ex);
//        showKeyStoreDialog(context,
//                "Internal error:",
//                message,
//                context.getString(R.string.AccessibilityLabels_close), null,
//                new DialogInterface.OnClickListener() {
//                    @Override
//                    public void onClick(DialogInterface dialog, int which) {
//                        dialog.dismiss();
//                        if (critical)
//                            ((Activity) context).finish();
//                    }
//                },
//                null,
//                new DialogInterface.OnDismissListener() {
//                    @Override
//                    public void onDismiss(DialogInterface dialog) {
//                        if (critical)
//                            ((Activity) context).finish();
//                    }
//                });
//    }
//
//    public static void showKeyStoreDialog(Context context, final String title, final String message, final String posButton, final String negButton,
//                                          final DialogInterface.OnClickListener posButtonListener,
//                                          final DialogInterface.OnClickListener negButtonListener,
//                                          final DialogInterface.OnDismissListener dismissListener) {
//        Activity app = (Activity) context;
//        if (app == null) app = (Activity) BreadApp.getBreadContext();
//        if (app == null) {
//            Log.e(TAG, "showKeyStoreDialog: app is null");
//            return;
//        }
//        final Activity finalApp = app;
//        if (finalApp != null)
//            finalApp.runOnUiThread(new Runnable() {
//                @Override
//                public void run() {
//                    if (finalApp != null) {
//                        if (dialog != null && dialog.isShowing()) {
//                            if (dialog.getOwnerActivity() != null && !dialog.getOwnerActivity().isDestroyed())
//                                dialog.dismiss();
//                            else
//                                return;
//                        }
//                        if (!finalApp.isDestroyed())
//                            dialog = new android.app.AlertDialog.Builder(finalApp).
//                                    setTitle(title)
//                                    .setMessage(message)
//                                    .setPositiveButton(posButton, posButtonListener)
//                                    .setNegativeButton(negButton, negButtonListener)
//                                    .setOnDismissListener(dismissListener)
//                                    .setIcon(android.R.drawable.ic_dialog_alert)
//                                    .show();
//                    }
//                }
//            });
//    }
}
