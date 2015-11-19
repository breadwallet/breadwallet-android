package com.breadwallet.presenter;

import android.annotation.TargetApi;
import android.app.Activity;
import android.app.Application;
import android.app.KeyguardManager;
import android.content.Context;
import android.content.DialogInterface;
import android.graphics.Point;
import android.os.Build;
import android.os.Handler;
import android.support.v4.BuildConfig;
import android.support.v4.app.FragmentActivity;
import android.support.v7.app.AlertDialog;
import android.util.Log;
import android.view.Display;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.TextView;
import android.widget.Toast;

import com.breadwallet.R;
import com.breadwallet.presenter.activities.MainActivity;
import com.breadwallet.tools.TypefaceUtil;

import org.acra.ACRA;
import org.acra.ReportField;
import org.acra.ReportingInteractionMode;
import org.acra.annotation.ReportsCrashes;

/**
 * BreadWallet
 * <p/>
 * Created by Mihail on 7/22/15.
 * Copyright (c) 2015 Mihail Gutan <mihail@breadwallet.com>
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

@ReportsCrashes(
        mailTo = "mihail@breadwallet.com", // my email here
        customReportContent = {
                ReportField.APP_VERSION_CODE,
                ReportField.APP_VERSION_NAME,
                ReportField.ANDROID_VERSION,
                ReportField.PACKAGE_NAME,
                ReportField.REPORT_ID,
                ReportField.BUILD,
                ReportField.STACK_TRACE
        },
        mode = ReportingInteractionMode.TOAST,
        resToastText = R.string.crash_toast_text
)
public class BreadWalletApp extends Application {
    public static final int BREAD_WALLET_IMAGE = 0;
    public static final int BREAD_WALLET_TEXT = 1;
    public static final int LOCKER_BUTTON = 2;
    public static final int PAY_BUTTON = 3;
    public static final String TAG = "BreadWalletApp";
    private boolean customToastAvailable = true;
    private String oldMessage;
    private Toast toast;
    public static int DISPLAY_WIDTH_PX;
    public static int DISPLAY_HEIGHT_PX;
    public static final String CREDENTIAL_TITLE = "Insert password";
    public static final String CREDENTIAL_DESCRIPTION = "Insert your password to unlock the app.";


    @Override
    public void onCreate() {
        super.onCreate();
        WindowManager wm = (WindowManager) getSystemService(Context.WINDOW_SERVICE);
        Display display = wm.getDefaultDisplay();
        Point size = new Point();
        display.getSize(size);
        DISPLAY_WIDTH_PX = size.x;
        DISPLAY_HEIGHT_PX = size.y;
        ACRA.init(this);
        TypefaceUtil.overrideFont(getApplicationContext(), "DEFAULT", "fonts/UbuntuMono-R.ttf");

    }

    /**
     * Shows a custom toast using the given string as a paramater,
     *
     * @param message the message to be shown in the custom toast
     */

    public void showCustomToast(Activity app, String message, int yOffSet, int duration) {
        if (toast == null) toast = new Toast(getApplicationContext());
        if (customToastAvailable || !oldMessage.equals(message)) {
            oldMessage = message;
            customToastAvailable = false;
            new Handler().postDelayed(new Runnable() {
                @Override
                public void run() {
                    customToastAvailable = true;
                }
            }, 1000);
            LayoutInflater inflater = app.getLayoutInflater();
            View layout = inflater.inflate(R.layout.toast,
                    (ViewGroup) app.findViewById(R.id.toast_layout_root));
            TextView text = (TextView) layout.findViewById(R.id.toast_text);
            text.setText(message);
            toast.setGravity(Gravity.BOTTOM, 0, yOffSet);
            toast.setDuration(duration);
            toast.setView(layout);
            toast.show();
        }
    }

    public void cancelToast() {
        if (toast != null) {
            Log.e(TAG, "Toast canceled");
            toast.cancel();
        }
    }

    public int getRelativeLeft(View myView) {
        if (myView.getParent() == myView.getRootView())
            return myView.getLeft();
        else
            return myView.getLeft() + getRelativeLeft((View) myView.getParent());
    }

    public int getRelativeTop(View myView) {
        if (myView.getParent() == myView.getRootView())
            return myView.getTop();
        else
            return myView.getTop() + getRelativeTop((View) myView.getParent());
    }

    public void setTopMiddleView(int view, String text) {
        MainActivity app = MainActivity.app;
        switch (view) {
            case BREAD_WALLET_IMAGE:
                if (app.viewFlipper.getDisplayedChild() == 1) {
                    app.viewFlipper.showPrevious();
                }
                break;
            case BREAD_WALLET_TEXT:
                if (app.viewFlipper.getDisplayedChild() == 0) {
                    app.viewFlipper.showNext();
                }
                ((TextView) app.viewFlipper.getCurrentView()).setText(text);
                break;
        }
    }

    public void setLockerPayButton(int view) {
        MainActivity app = MainActivity.app;
        Log.e(TAG, "Flipper has # of child: " + app.lockerPayFlipper.getChildCount());
        switch (view) {
            case LOCKER_BUTTON:
                if (app.lockerPayFlipper.getDisplayedChild() == 1) {
                    app.lockerPayFlipper.showPrevious();
                    if (MainActivity.unlocked) {
                        app.lockerButton.setVisibility(View.INVISIBLE);
                    } else {
                        app.lockerButton.setVisibility(View.VISIBLE);
                    }
                }
                break;
            case PAY_BUTTON:
                if (app.lockerPayFlipper.getDisplayedChild() == 0) {
                    app.lockerPayFlipper.showNext();
                }
                break;
        }

    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP_MR1)
    public void checkAndPromptForAuthentication(FragmentActivity context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            KeyguardManager keyguardManager = (KeyguardManager) context.getSystemService(Activity.KEYGUARD_SERVICE);
            if (keyguardManager.isKeyguardSecure()) {
//                Intent intent = keyguardManager.createConfirmDeviceCredentialIntent(CREDENTIAL_TITLE, CREDENTIAL_DESCRIPTION);
//                context.startActivityForResult(intent, 1);
                ((MainActivity) context).showAuthDialog();
            } else {
                showDeviceNotSecuredWarning(context);
            }
        } else {
            showCustomToast(context, "Ups! api level lower then 21", 300, Toast.LENGTH_SHORT);
        }

    }

    public void showDeviceNotSecuredWarning(final Activity context) {
        Log.e(TAG, "WARNING device is not secured!");
        new AlertDialog.Builder(context)
                .setTitle("Warning!")
                .setMessage("A device passcode is needed to safeguard your wallet. " +
                        "Go to settings and turn passcode on to continue.")
                .setNegativeButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        context.finish();
                    }
                })
                .setIcon(android.R.drawable.ic_dialog_alert)
                .show();
    }

    public boolean isEmulatorOrDebug() {
        String fing = Build.FINGERPRINT;
        boolean isEmulator = false;
        if (fing != null) {
            isEmulator = fing.contains("vbox") || fing.contains("generic");
        }
        return isEmulator || BuildConfig.DEBUG;
    }

    public void showCustomDialog(final String title, final String message, final String buttonText) {
        Log.e(TAG, "Showing a dialog!");
        MainActivity.app.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                new android.app.AlertDialog.Builder(MainActivity.app)
                        .setTitle(title)
                        .setMessage(message)
                        .setPositiveButton(buttonText, new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int which) {
                                dialog.cancel();
                            }
                        })
                        .setIcon(android.R.drawable.ic_dialog_alert)
                        .show();
            }
        });
    }

}
