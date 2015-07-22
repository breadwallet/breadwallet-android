package com.breadwallet.tools.animation;

import android.support.v4.app.FragmentTransaction;
import android.util.Log;

import com.breadwallet.R;
import com.breadwallet.presenter.activities.MainActivity;

/**
 * Created by Mihail on 7/13/15.
 */
public class FragmentAnimator {
    public static final String TAG = "FragmentAnimator";
    private static final MainActivity APP = MainActivity.getApp();

    public static void animateDecoderFragment() {
        APP.setDecoderFragmentOn(true);
        //Disabled inspection: <Expected resource type anim>
        FragmentTransaction fragmentTransaction = APP.getSupportFragmentManager().beginTransaction();
        fragmentTransaction.setCustomAnimations(R.animator.from_bottom, R.animator.to_top);
        fragmentTransaction.replace(R.id.mainlayout, APP.getMainFragmentDecoder());
        int temp = fragmentTransaction.commit();
        Log.e(TAG, String.valueOf(temp));
    }
}
