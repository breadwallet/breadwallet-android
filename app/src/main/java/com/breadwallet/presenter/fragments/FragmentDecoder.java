package com.breadwallet.presenter.fragments;

import android.app.Fragment;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RelativeLayout;

import com.breadwallet.presenter.activities.MainActivity;
import com.breadwallet.tools.animation.BRAnimator;
import com.breadwallet.tools.qrcode.QRScannerView;
import com.breadwallet.tools.security.RequestHandler;
import com.google.zxing.Result;


public class FragmentDecoder extends Fragment implements QRScannerView.ResultHandler {
    private QRScannerView mScannerView;
    public static final String TAG = FragmentDecoder.class.getName();
//    private ImageView breadLogo;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        mScannerView = new QRScannerView(getActivity());
        final RelativeLayout blackPane = new RelativeLayout(getContext());
        blackPane.setLayoutParams(new RelativeLayout.LayoutParams(MainActivity.screenParametersPoint.x, MainActivity.screenParametersPoint.y));
        blackPane.setBackgroundColor(Color.BLACK);
//        breadLogo = new ImageView(getContext());
//        breadLogo.setImageDrawable(getActivity().getDrawable(R.drawable.breadwallet_big));
//        RelativeLayout.LayoutParams params = new RelativeLayout.LayoutParams(MainActivity.screenParametersPoint.x / 2, MainActivity.screenParametersPoint.x / 2);
//        params.addRule(RelativeLayout.CENTER_HORIZONTAL);
//        params.addRule(RelativeLayout.CENTER_VERTICAL);
//        blackPane.addView(breadLogo, params);

        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                mScannerView.removeView(blackPane);
            }
        }, 1000);
        mScannerView.addView(blackPane);
        return mScannerView;
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
//        new Handler().postDelayed(new Runnable() {
//            @Override
//            public void run() {
//                SpringAnimator.showCameraLogoAnimation(breadLogo);
//            }
//        },100);
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                if (mScannerView != null)
                    mScannerView.initViews();
            }
        }, 1000);

    }

    @Override
    public void onResume() {
        super.onResume();
        final FragmentDecoder instance = this;
        mScannerView.setResultHandler(instance);
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                mScannerView.startCamera();
            }
        }, 400);

    }

    @Override
    public void handleResult(Result rawResult) {
        final String text = rawResult.getText();
        final MainActivity app = MainActivity.app;
        if (app == null) return;
        app.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (!text.isEmpty()) {
                    BRAnimator.hideDecoderFragment();
                    Log.e(TAG, "BEFORE processRequest");
                    RequestHandler.processRequest(app, text);

                }
            }
        });

    }

    @Override
    public void onPause() {
        super.onPause();
        mScannerView.stopCamera();
    }
}
