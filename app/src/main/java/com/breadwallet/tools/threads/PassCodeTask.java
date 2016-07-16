package com.breadwallet.tools.threads;

import android.app.Activity;
import android.app.FragmentManager;
import android.os.Handler;
import android.os.SystemClock;
import android.util.Log;
import android.view.MotionEvent;

import com.breadwallet.presenter.activities.MainActivity;
import com.breadwallet.presenter.fragments.PasswordDialogFragment;
import com.breadwallet.tools.adapter.CustomPagerAdapter;
import com.breadwallet.tools.security.KeyStoreManager;

/**
 * BreadWallet
 * <p/>
 * Created by Mihail Gutan on 3/24/16.
 * Copyright (c) 2016 breadwallet llc <mihail@breadwallet.com>
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
public class PassCodeTask extends Thread {
    public static final String TAG = PassCodeTask.class.getName();
    static PasswordDialogFragment passwordDialogFragment;
    int pass = 0;
    private Activity activity;

    public PassCodeTask(Activity activity) {
        this.activity = activity;
    }

    @Override
    public void run() {
        super.run();

        if (activity == null) return;
        final FragmentManager fm = activity.getFragmentManager();

        while (pass == 0 && activity != null) {
            activity.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    Log.e(TAG, "PassCodeTask, while, run...");
                    if (passwordDialogFragment == null) {
                        passwordDialogFragment = new PasswordDialogFragment();
                    }
                    if (!passwordDialogFragment.isVisible()) {
                        try {
                            fm.beginTransaction().remove(passwordDialogFragment).commitAllowingStateLoss();
                            passwordDialogFragment = new PasswordDialogFragment();
                            passwordDialogFragment.setFirstTimeTrue();
                            passwordDialogFragment.show(fm, PasswordDialogFragment.class.getName());
                        } catch (Exception e) {
                            e.printStackTrace();
                        }

                    }

                }
            });
            try {
                Thread.sleep(500);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            pass = KeyStoreManager.getPassCode(activity);

        }
        if (passwordDialogFragment != null) {
            try {
                passwordDialogFragment.dismiss();
                ensureHideKeyboard();

            } catch (IllegalStateException ex) {
                ex.printStackTrace();
            }
        }

    }

    private void ensureHideKeyboard() {
        final CustomPagerAdapter adapter = CustomPagerAdapter.adapter;
        if (adapter != null)
            if (adapter.mainFragment.mainFragmentLayout != null) {
                activity.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        (new Handler()).postDelayed(new Runnable() {

                            public void run() {
                                adapter.mainFragment.mainFragmentLayout.dispatchTouchEvent(MotionEvent.obtain(SystemClock.uptimeMillis(), SystemClock.uptimeMillis(), MotionEvent.ACTION_DOWN, 0, 0, 0));
                                adapter.mainFragment.mainFragmentLayout.dispatchTouchEvent(MotionEvent.obtain(SystemClock.uptimeMillis(), SystemClock.uptimeMillis(), MotionEvent.ACTION_UP, 0, 0, 0));

                            }
                        }, 100);
                    }
                });

            }
    }
}

