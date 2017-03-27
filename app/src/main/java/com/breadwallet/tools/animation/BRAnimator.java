package com.breadwallet.tools.animation;

import android.app.Activity;
import android.app.Fragment;
import android.app.FragmentTransaction;
import android.os.Bundle;
import android.os.Handler;
import android.view.View;

import com.breadwallet.R;
import com.breadwallet.presenter.fragments.FragmentBreadMenu;
import com.breadwallet.presenter.fragments.FragmentBreadSignal;

import java.util.Stack;


/**
 * BreadWallet
 * <p>
 * Created by Mihail Gutan <mihail@breadwallet.com> on 7/13/15.
 * Copyright (c) 2016 breadwallet LLC
 * <p>
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 * <p>
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 * <p>
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */

public class BRAnimator {
    private static final String TAG = BRAnimator.class.getName();
    public static boolean decoderFragmentOn;
    public static boolean scanResultFragmentOn;
    public static int level = 0;
    public static boolean wipeWalletOpen = false;
    //    private static Stack<Fragment> previous = new Stack<>();
    private static boolean multiplePressingAvailable = true;
    public static int horizontalSlideDuration = 300;
    private static boolean horizontalSlideAvailable = true;
    private static View copy;

    private static FragmentBreadSignal fragmentSignal;
    private static FragmentBreadMenu fragmentMenu;
    private static boolean clickAllowed = true;


    public static void showBreadSignal(Activity activity, String title, String iconDescription, int drawableId) {
        fragmentSignal = new FragmentBreadSignal();
        Bundle bundle = new Bundle();
        bundle.putString(FragmentBreadSignal.TITLE, title);
        bundle.putString(FragmentBreadSignal.ICON_DESCRIPTION, iconDescription);
        bundle.putInt(FragmentBreadSignal.RES_ID, drawableId);
        fragmentSignal.setArguments(bundle);
        FragmentTransaction transaction = activity.getFragmentManager().beginTransaction();
        transaction.setCustomAnimations(R.animator.from_bottom, R.animator.to_bottom, R.animator.from_bottom, R.animator.to_bottom);
        transaction.add(android.R.id.content, fragmentSignal, fragmentSignal.getClass().getName());
        transaction.addToBackStack(null);
        transaction.commit();
    }

    public static void showBreadMenu(Activity activity) {
        fragmentMenu = new FragmentBreadMenu();
        FragmentTransaction transaction = activity.getFragmentManager().beginTransaction();
        transaction.setCustomAnimations(R.animator.from_bottom, R.animator.to_bottom, R.animator.from_bottom, R.animator.to_bottom);
        transaction.add(android.R.id.content, fragmentMenu, fragmentMenu.getClass().getName());
        transaction.addToBackStack(null);
        transaction.commit();

    }

    public static boolean isClickAllowed() {
        if (clickAllowed) {
            clickAllowed = false;
            new Handler().postDelayed(new Runnable() {
                @Override
                public void run() {
                    clickAllowed = true;
                }
            }, 300);
            return true;
        } else return false;
    }

}