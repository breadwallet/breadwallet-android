package com.breadwallet.tools.animation;

import android.Manifest;
import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ArgbEvaluator;
import android.animation.LayoutTransition;
import android.animation.ObjectAnimator;
import android.animation.PropertyValuesHolder;
import android.animation.ValueAnimator;
import android.app.Activity;
import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.ColorStateList;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.text.TextUtils;
import android.util.Log;
import android.util.TypedValue;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.DecelerateInterpolator;
import android.view.animation.OvershootInterpolator;
import android.widget.TextView;

import com.breadwallet.BreadApp;
import com.breadwallet.R;
import com.breadwallet.presenter.activities.BreadActivity;
import com.breadwallet.presenter.activities.PinActivity;
import com.breadwallet.presenter.activities.camera.ScanQRActivity;
import com.breadwallet.presenter.customviews.BRDialogView;
import com.breadwallet.presenter.entities.TxItem;
import com.breadwallet.presenter.fragments.FragmentMenu;
import com.breadwallet.presenter.fragments.FragmentSignal;
import com.breadwallet.presenter.fragments.FragmentReceive;
import com.breadwallet.presenter.fragments.FragmentRequestAmount;
import com.breadwallet.presenter.fragments.FragmentSend;
import com.breadwallet.presenter.fragments.FragmentSupport;
import com.breadwallet.presenter.fragments.FragmentTransactionDetails;
import com.breadwallet.presenter.interfaces.BROnSignalCompletion;
import com.breadwallet.tools.manager.BRSharedPrefs;
import com.breadwallet.tools.util.BRConstants;
import com.breadwallet.tools.util.Utils;
import com.google.firebase.crash.FirebaseCrash;

import java.util.List;


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
    private static FragmentSignal fragmentSignal;
    private static boolean clickAllowed = true;
    public static int SLIDE_ANIMATION_DURATION = 300;
    public static float t1Size;
    public static float t2Size;

    public static void showBreadSignal(Activity activity, String title, String iconDescription, int drawableId, BROnSignalCompletion completion) {
        fragmentSignal = new FragmentSignal();
        Bundle bundle = new Bundle();
        bundle.putString(FragmentSignal.TITLE, title);
        bundle.putString(FragmentSignal.ICON_DESCRIPTION, iconDescription);
        fragmentSignal.setCompletion(completion);
        bundle.putInt(FragmentSignal.RES_ID, drawableId);
        fragmentSignal.setArguments(bundle);
        FragmentTransaction transaction = activity.getFragmentManager().beginTransaction();
        transaction.setCustomAnimations(R.animator.from_bottom, R.animator.to_bottom, R.animator.from_bottom, R.animator.to_bottom);
        transaction.add(android.R.id.content, fragmentSignal, fragmentSignal.getClass().getName());
        transaction.addToBackStack(null);
        transaction.commit();
    }

    public static void init(Activity app) {
        if (app == null) return;
//        t1Size = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, 30, app.getResources().getDisplayMetrics());
//        t2Size = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, 16, app.getResources().getDisplayMetrics());
        t1Size = 30;
        t2Size = 16;
    }

    public static void showFragmentByTag(Activity app, String tag) {
        if (tag == null) return;
        //catch animation duration, make it 0 for no animation, then restore it.
        final int slideAnimation = SLIDE_ANIMATION_DURATION;
        try {
            SLIDE_ANIMATION_DURATION = 0;
            if (tag.equalsIgnoreCase(FragmentSend.class.getName())) {
                showSendFragment(app, null);
            } else if (tag.equalsIgnoreCase(FragmentReceive.class.getName())) {
                showReceiveFragment(app, true);
            } else if (tag.equalsIgnoreCase(FragmentRequestAmount.class.getName())) {
                showRequestFragment(app, BRSharedPrefs.getReceiveAddress(app));
            } else if (tag.equalsIgnoreCase(FragmentMenu.class.getName())) {
                showMenuFragment(app);
            } else {
                Log.e(TAG, "showFragmentByTag: error, no such tag: " + tag);
            }
        } finally {
            new Handler().postDelayed(new Runnable() {
                @Override
                public void run() {
                    SLIDE_ANIMATION_DURATION = slideAnimation;
                }
            }, 800);

        }
    }

    public static void showSendFragment(Activity app, final String bitcoinUrl) {
        if (app == null) {
            Log.e(TAG, "showSendFragment: app is null");
            return;
        }
        FragmentSend fragmentSend = (FragmentSend) app.getFragmentManager().findFragmentByTag(FragmentSend.class.getName());
        if (fragmentSend != null && fragmentSend.isAdded()) {
            fragmentSend.setUrl(bitcoinUrl);
            return;
        }
        try {
            fragmentSend = new FragmentSend();
            if (bitcoinUrl != null && !bitcoinUrl.isEmpty()) {
                Bundle bundle = new Bundle();
                bundle.putString("url", bitcoinUrl);
                fragmentSend.setArguments(bundle);
            }
            app.getFragmentManager().beginTransaction()
                    .setCustomAnimations(0, 0, 0, R.animator.plain_300)
                    .add(android.R.id.content, fragmentSend, FragmentSend.class.getName())
                    .addToBackStack(FragmentSend.class.getName()).commit();
        } finally {

        }

    }
    public static void showSupportFragment(Activity app, String articleId) {
        BRDialog.showCustomDialog(
                app,
                BreadApp.getInstance().getString(R.string.Support_Title),
                BreadApp.getInstance().getString(R.string.Support_Message),
                null,
                null,
                new BRDialogView.BROnClickListener() {
                    @Override
                    public void onClick(BRDialogView brDialogView) {
                        brDialogView.dismissWithAnimation();
                    }
                },
                null,
                null,
                0);
        FirebaseCrash.log("showSupportFragment");

        // Temporarily disable SupportFragment
//        if (app == null) {
//            Log.e(TAG, "showSupportFragment: app is null");
//            return;
//        }
//        FragmentSupport fragmentSupport = (FragmentSupport) app.getFragmentManager().findFragmentByTag(FragmentSupport.class.getName());
//        if (fragmentSupport != null && fragmentSupport.isAdded()) {
//            app.getFragmentManager().popBackStack();
//            return;
//        }
//        try {
//            fragmentSupport = new FragmentSupport();
//            if (articleId != null && !articleId.isEmpty()) {
//                Bundle bundle = new Bundle();
//                bundle.putString("articleId", articleId);
//                fragmentSupport.setArguments(bundle);
//            }
//            app.getFragmentManager().beginTransaction()
//                    .setCustomAnimations(0, 0, 0, R.animator.plain_300)
//                    .add(android.R.id.content, fragmentSupport, FragmentSend.class.getName())
//                    .addToBackStack(FragmentSend.class.getName()).commit();
//        } finally {
//
//        }

    }

    public static void showTransactionPager(Activity app, List<TxItem> items, int position) {
        if (app == null) {
            Log.e(TAG, "showSendFragment: app is null");
            return;
        }
        FragmentTransactionDetails fragmentTransactionDetails = (FragmentTransactionDetails) app.getFragmentManager().findFragmentByTag(FragmentTransactionDetails.class.getName());
        if (fragmentTransactionDetails != null && fragmentTransactionDetails.isAdded()) {
            Log.e(TAG, "showTransactionPager: Already showing");
            return;
        }
        fragmentTransactionDetails = new FragmentTransactionDetails();
        fragmentTransactionDetails.setItems(items);
        Bundle bundle = new Bundle();
        bundle.putInt("pos", position);
        fragmentTransactionDetails.setArguments(bundle);

        app.getFragmentManager().beginTransaction()
                .setCustomAnimations(0, 0, 0, R.animator.plain_300)
                .add(android.R.id.content, fragmentTransactionDetails, FragmentTransactionDetails.class.getName())
                .addToBackStack(FragmentTransactionDetails.class.getName()).commit();

    }

    public static void openScanner(Activity app, int requestID) {
        try {
            if (app == null) return;

            // Check if the camera permission is granted
            if (ContextCompat.checkSelfPermission(app,
                    Manifest.permission.CAMERA)
                    != PackageManager.PERMISSION_GRANTED) {
                // Should we show an explanation?
                if (ActivityCompat.shouldShowRequestPermissionRationale(app,
                        Manifest.permission.CAMERA)) {
                    BRDialog.showCustomDialog(app, "Permission Required.", "Allow camera access in \"Settings\" > \"Apps\" > \"loafwallet\" > \"Permissions\"", "close", null, new BRDialogView.BROnClickListener() {
                        @Override
                        public void onClick(BRDialogView brDialogView) {
                            brDialogView.dismiss();
                        }
                    }, null, null, 0);
                } else {
                    // No explanation needed, we can request the permission.
                    ActivityCompat.requestPermissions(app,
                            new String[]{Manifest.permission.CAMERA},
                            BRConstants.CAMERA_REQUEST_ID);
                }
            } else {
                // Permission is granted, open camera
                Intent intent = new Intent(app, ScanQRActivity.class);
                app.startActivityForResult(intent, requestID);
                app.overridePendingTransition(R.anim.fade_up, R.anim.fade_down);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    public static LayoutTransition getDefaultTransition() {
        LayoutTransition itemLayoutTransition = new LayoutTransition();
        itemLayoutTransition.setStartDelay(LayoutTransition.APPEARING, 0);
        itemLayoutTransition.setStartDelay(LayoutTransition.DISAPPEARING, 0);
        itemLayoutTransition.setStartDelay(LayoutTransition.CHANGE_APPEARING, 0);
        itemLayoutTransition.setStartDelay(LayoutTransition.CHANGE_DISAPPEARING, 0);
        itemLayoutTransition.setStartDelay(LayoutTransition.CHANGING, 0);
        itemLayoutTransition.setDuration(100);
        itemLayoutTransition.setInterpolator(LayoutTransition.CHANGING, new OvershootInterpolator(2f));
        Animator scaleUp = ObjectAnimator.ofPropertyValuesHolder((Object) null, PropertyValuesHolder.ofFloat(View.SCALE_X, 1, 1), PropertyValuesHolder.ofFloat(View.SCALE_Y, 0, 1));
        scaleUp.setDuration(50);
        scaleUp.setStartDelay(50);
        Animator scaleDown = ObjectAnimator.ofPropertyValuesHolder((Object) null, PropertyValuesHolder.ofFloat(View.SCALE_X, 1, 1), PropertyValuesHolder.ofFloat(View.SCALE_Y, 1, 0));
        scaleDown.setDuration(2);
        itemLayoutTransition.setAnimator(LayoutTransition.APPEARING, scaleUp);
        itemLayoutTransition.setAnimator(LayoutTransition.DISAPPEARING, null);
        itemLayoutTransition.enableTransitionType(LayoutTransition.CHANGING);
        return itemLayoutTransition;
    }

    public static void showRequestFragment(Activity app, String address) {
        if (app == null) {
            Log.e(TAG, "showRequestFragment: app is null");
            return;
        }
        if (Utils.isNullOrEmpty(address)) {
            Log.e(TAG, "showRequestFragment: address is empty: " + address);
            return;
        }

        FragmentRequestAmount fragmentRequestAmount = (FragmentRequestAmount) app.getFragmentManager().findFragmentByTag(FragmentRequestAmount.class.getName());
        if (fragmentRequestAmount != null && fragmentRequestAmount.isAdded())
            return;

        fragmentRequestAmount = new FragmentRequestAmount();
        Bundle bundle = new Bundle();
        bundle.putString("address", address);
        fragmentRequestAmount.setArguments(bundle);
        app.getFragmentManager().beginTransaction()
                .setCustomAnimations(0, 0, 0, R.animator.plain_300)
                .add(android.R.id.content, fragmentRequestAmount, FragmentRequestAmount.class.getName())
                .addToBackStack(FragmentRequestAmount.class.getName()).commit();

    }

    //isReceive tells the Animator that the Receive fragment is requested, not My Address
    public static void showReceiveFragment(Activity app, boolean isReceive) {
        if (app == null) {
            Log.e(TAG, "showReceiveFragment: app is null");
            return;
        }
        FragmentReceive fragmentReceive = (FragmentReceive) app.getFragmentManager().findFragmentByTag(FragmentReceive.class.getName());
        if (fragmentReceive != null && fragmentReceive.isAdded())
            return;
        fragmentReceive = new FragmentReceive();
        Bundle args = new Bundle();
        args.putBoolean("receive", isReceive);
        fragmentReceive.setArguments(args);

        app.getFragmentManager().beginTransaction()
                .setCustomAnimations(0, 0, 0, R.animator.plain_300)
                .add(android.R.id.content, fragmentReceive, FragmentReceive.class.getName())
                .addToBackStack(FragmentReceive.class.getName()).commit();

    }

    public static void showMenuFragment(Activity app) {
        if (app == null) {
            Log.e(TAG, "showReceiveFragment: app is null");
            return;
        }
        FragmentTransaction transaction = app.getFragmentManager().beginTransaction();
        transaction.setCustomAnimations(0, 0, 0, R.animator.plain_300);
        transaction.add(android.R.id.content, new FragmentMenu(), FragmentMenu.class.getName());
        transaction.addToBackStack(FragmentMenu.class.getName());
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

    public static void killAllFragments(Activity app) {
        if (app != null)
            app.getFragmentManager().popBackStack(null, FragmentManager.POP_BACK_STACK_INCLUSIVE);
    }

    public static void startBreadIfNotStarted(Activity app) {
        if (!(app instanceof BreadActivity))
            startBreadActivity(app, false);
    }

    public static void startBreadActivity(Activity from, boolean auth) {
        Class toStart = auth ? PinActivity.class : BreadActivity.class;
        Intent intent = new Intent(from, toStart);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        from.startActivity(intent);
        from.overridePendingTransition(R.anim.fade_up, R.anim.fade_down);
        if (!from.isDestroyed()) {
            from.finish();
        }
    }

    public static void swapPriceTexts(final Context ctx, final TextView t1, final TextView t2) {
        BRSharedPrefs.putPreferredBTC(ctx, !BRSharedPrefs.getPreferredBTC(ctx));

        final float t1Size = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, 30, ctx.getResources().getDisplayMetrics());
        final float t2Size = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, 14, ctx.getResources().getDisplayMetrics());

        final float t1W = t1.getWidth();
        final float t2W = t2.getWidth();
        final float t1X = t1.getX();
        final float t2X = t2.getX();

        String t1Text = t1.getText().toString();
        String t2Text = t2.getText().toString();
        String eq = " = ";
        if (!t2Text.substring(0, 3).equalsIgnoreCase(eq))
            throw new RuntimeException("secondaryPrice does not start with | = | ");
        t1.setText(t2Text.substring(3));
        t2.setText(TextUtils.concat(eq, t1Text));

        float marLeft = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 16, ctx.getResources().getDisplayMetrics());


        ColorStateList t1Colors = t1.getTextColors();
        ColorStateList t2Colors = t2.getTextColors();
        t1.setTextColor(t2Colors);
        t2.setTextColor(t1Colors);

        final int ANIMATION_DURATION = 400;

        //Animate the first text
        ValueAnimator an1 = ValueAnimator.ofFloat(t1Size, t2Size);
        an1.setDuration(ANIMATION_DURATION);
        an1.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator valueAnimator) {
                float animatedValue = (float) valueAnimator.getAnimatedValue();
                t1.setTextSize(TypedValue.COMPLEX_UNIT_PX, animatedValue);
            }
        });
        an1.start();

        t1.animate().x(t2W * 2).setInterpolator(new DecelerateOvershootInterpolator(0.5f, 0.5f)).setDuration(ANIMATION_DURATION);
        //Animate the second text

        ValueAnimator an2 = ValueAnimator.ofFloat(t2Size, t1Size);
        an2.setDuration(ANIMATION_DURATION);
        an2.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator valueAnimator) {
                float animatedValue = (float) valueAnimator.getAnimatedValue();
                t2.setTextSize(TypedValue.COMPLEX_UNIT_PX, animatedValue);
            }
        });
        an2.start();

        t2.animate().x(marLeft).setInterpolator(new DecelerateOvershootInterpolator(0.5f, 0.5f)).setDuration(ANIMATION_DURATION);

    }

    public static void animateSignalSlide(ViewGroup signalLayout, final boolean reverse, final OnSlideAnimationEnd listener) {
        float translationY = signalLayout.getTranslationY();
        float signalHeight = signalLayout.getHeight();
        signalLayout.setTranslationY(reverse ? translationY : translationY + signalHeight);
        signalLayout.animate().translationY(reverse ? 2600 : translationY).setDuration(SLIDE_ANIMATION_DURATION)
                .setInterpolator(reverse ? new DecelerateInterpolator() : new OvershootInterpolator(0.7f))
                .setListener(new AnimatorListenerAdapter() {
                    @Override
                    public void onAnimationEnd(Animator animation) {
                        super.onAnimationEnd(animation);
                        if (listener != null)
                            listener.onAnimationEnd();
                    }
                });

    }

    public static void animateBackgroundDim(final ViewGroup backgroundLayout, boolean reverse) {
        int transColor = reverse ? R.color.black_trans : android.R.color.transparent;
        int blackTransColor = reverse ? android.R.color.transparent : R.color.black_trans;

        ValueAnimator anim = new ValueAnimator();
        anim.setIntValues(transColor, blackTransColor);
        anim.setEvaluator(new ArgbEvaluator());
        anim.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator valueAnimator) {
                backgroundLayout.setBackgroundColor((Integer) valueAnimator.getAnimatedValue());
            }
        });

        anim.setDuration(SLIDE_ANIMATION_DURATION);
        anim.start();
    }


    public interface OnSlideAnimationEnd {
        void onAnimationEnd();
    }

}