package com.breadwallet.presenter.fragments;

import android.app.Fragment;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.breadwallet.R;

import junit.framework.Assert;


/**
 * BreadWallet
 * <p>
 * Created by Mihail Gutan <mihail@breadwallet.com> on 6/29/15.
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

public class FragmentBreadSignal extends Fragment {
    private static final String TAG = FragmentBreadSignal.class.getName();

    public static final String TITLE = "title";
    public static final String ICON_DESCRIPTION = "iconDescription";
    public static final String RES_ID = "resId";
    public TextView mTitle;
    public TextView mDescription;
    public ImageView mIcon;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        // The last two arguments ensure LayoutParams are inflated
        // properly.

        View rootView = inflater.inflate(R.layout.fragment_signal, container, false);
        mTitle = (TextView) rootView.findViewById(R.id.title);
        mDescription = (TextView) rootView.findViewById(R.id.description);
        mIcon = (ImageView) rootView.findViewById(R.id.qr_image);

        Bundle bundle = this.getArguments();

        if (bundle != null) {
            String title = bundle.getString(TITLE, "");
            String description = bundle.getString(ICON_DESCRIPTION, "");
            int resId = bundle.getInt(RES_ID, 0);
            Assert.assertNotSame(title, "");
            Assert.assertNotSame(description, "");
            Assert.assertNotSame(resId, 0);

            mTitle.setText(title);
            mDescription.setText(description);
            mIcon.setImageResource(resId);
        } else {
            Log.e(TAG, "onCreateView: bundle is null!");
        }
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                if (getActivity() != null)
                    getActivity().getFragmentManager().popBackStack();
            }
        }, 1500);

        return rootView;
    }


    @Override
    public void onResume() {
        super.onResume();
    }

    @Override
    public void onPause() {
        super.onPause();
    }

}