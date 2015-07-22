package com.breadwallet.presenter.fragments.allsettings;

import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RelativeLayout;

import com.breadwallet.R;
import com.breadwallet.presenter.activities.MainActivity;
import com.breadwallet.tools.animation.SpringAnimator;

/**
 * Created by Mihail on 6/29/15.
 */
public class FragmentSettings extends Fragment {
    private RelativeLayout about;
    private MainActivity app;

    @Override
    public View onCreateView(LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        // The last two arguments ensure LayoutParams are inflated
        // properly.
        View rootView = inflater.inflate(
                R.layout.fragment_settings_pressed, container, false);
        app = MainActivity.getApp();
        about = (RelativeLayout) rootView.findViewById(R.id.about);
        about.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                app.animateSideToLeft(app.getMainFragmentSettingsAll(), app.getFragmentAbout());
                new Handler().postDelayed(new Runnable() {

                    @Override
                    public void run() {
                        SpringAnimator.showBouncySlide(MainActivity.getApp().getFragmentAbout().getView());
                    }
                }, 200);

            }
        });
        return rootView;
    }
}
