package com.breadwallet.presenter.fragments;

import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RelativeLayout;

import com.breadwallet.R;
import com.breadwallet.presenter.activities.MainActivity;
import com.breadwallet.tools.animations.SpringAnimator;

/**
 * Created by Mihail on 6/29/15.
 */
public class MainFragmentSettings extends Fragment {
    public static final String TAG = "MainFragmentSettings";

    private RelativeLayout settings;

    @Override
    public View onCreateView(LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        // The last two arguments ensure LayoutParams are inflated
        // properly.
        final View rootView = inflater.inflate(
                R.layout.fragment_settings, container, false);

        settings = (RelativeLayout) rootView.findViewById(R.id.settings);
        settings.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ((MainActivity) getActivity()).animateSettingsPressed();
                new Handler().postDelayed(new Runnable() {

                    @Override
                    public void run() {
                        Log.d(TAG, "Starting:   showBouncySlide()");
                        SpringAnimator.showBouncySlide(MainActivity.getApp().getMainFragmentSettingsPressed().getView());
                    }
                }, 200);

            }
        });
        return rootView;
    }

}
