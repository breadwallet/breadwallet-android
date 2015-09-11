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
import com.breadwallet.tools.adapter.MiddleViewAdapter;
import com.breadwallet.tools.animation.FragmentAnimator;

/**
 * BreadWallet
 * <p/>
 * Created by Mihail on 8/4/15.
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

public class MainFragmentSettingsAll extends Fragment {
    public static final String TAG = "MainFragmentSettings";

    private RelativeLayout settings;
    private RelativeLayout importPrivateKeys;
    private MainActivity app;
    private MainFragmentSettingsAll mainFragmentSettingsAll;

    @Override
    public View onCreateView(LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        // The last two arguments ensure LayoutParams are inflated
        // properly.
        final View rootView = inflater.inflate(
                R.layout.fragment_settings_all, container, false);
        app = MainActivity.app;
        importPrivateKeys = (RelativeLayout) rootView.findViewById(R.id.import_private_key);
        importPrivateKeys.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (FragmentAnimator.checkTheMultipressingAvailability(300)) {
                    FragmentAnimator.animateDecoderFragment();
                }
            }
        });
        mainFragmentSettingsAll = this;
        settings = (RelativeLayout) rootView.findViewById(R.id.settings);
        settings.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (FragmentAnimator.checkTheMultipressingAvailability(300)) {
                    FragmentAnimator.animateSlideToLeft(app, app.fragmentSettings, mainFragmentSettingsAll);
                    Log.d(TAG, "Starting:   showBouncySlide()");
                }
            }
        });
        return rootView;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

    }

    @Override
    public void onResume() {
        super.onResume();
        MiddleViewAdapter.resetMiddleView(null);
    }

}
