package com.breadwallet.presenter.fragments;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RelativeLayout;

import com.breadwallet.R;
import com.breadwallet.presenter.activities.MainActivity;
import com.breadwallet.tools.animation.FragmentAnimator;

/**
 * Created by Mihail on 6/29/15.
 */
public class MainFragmentSettingsAll extends Fragment {
    public static final String TAG = "MainFragmentSettings";

    private RelativeLayout settings;
    private MainActivity app;
    private MainFragmentSettingsAll mainMainFragmentSettingsAll;

    @Override
    public View onCreateView(LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        // The last two arguments ensure LayoutParams are inflated
        // properly.
        final View rootView = inflater.inflate(
                R.layout.fragment_settings_all, container, false);

        return rootView;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        app = MainActivity.getApp();
        mainMainFragmentSettingsAll = this;
        settings = (RelativeLayout) getView().findViewById(R.id.settings);
        settings.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                FragmentAnimator.animateSlideToLeft(app, app.getFragmentSettings(), mainMainFragmentSettingsAll);
                Log.d(TAG, "Starting:   showBouncySlide()");
            }
        });
    }
}
