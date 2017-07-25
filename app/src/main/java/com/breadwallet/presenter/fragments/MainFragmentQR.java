
package com.breadwallet.presenter.fragments;

import android.app.Activity;
import android.app.Fragment;
import android.app.FragmentManager;
import android.content.Context;
import android.content.ContextWrapper;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.breadwallet.R;
import com.breadwallet.BreadWalletApp;
import com.breadwallet.presenter.activities.MainActivity;
import com.breadwallet.presenter.customviews.BubbleTextView;
import com.breadwallet.tools.manager.BRTipsManager;
import com.breadwallet.tools.manager.SharedPreferencesManager;
import com.breadwallet.tools.adapter.MiddleViewAdapter;
import com.breadwallet.tools.animation.BRAnimator;
import com.breadwallet.tools.animation.SpringAnimator;
import com.breadwallet.wallet.BRWalletManager;

import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;

import static android.R.attr.path;

/**
 * BreadWallet
 * <p>
 * Created by Mihail Gutan <mihail@breadwallet.com> on 6/23/15.
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

public class MainFragmentQR extends Fragment {
    private static final String TAG = MainFragmentQR.class.getName();
    private ImageView qrcode;
    private TextView mainAddressText;
    private LinearLayout qrImageLayout;
    private Bitmap bitmap;
    private FragmentSharing sharingFragment;
    private FragmentManager fm;
    public static File qrCodeImageFile;
    private String receiveAddress;
    private int bubbleState = 0;
    private RelativeLayout addressLayout;

    @Override
    public View onCreateView(LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        // The last two arguments ensure LayoutParams are inflated
        // properly.
        final View rootView = inflater.inflate(R.layout.fragment_qr_main, container, false);

        BRWalletManager.refreshAddress();
        receiveAddress = SharedPreferencesManager.getReceiveAddress(getActivity());
        qrImageLayout = (LinearLayout) rootView.findViewById(R.id.qr_image_address_layout);
        qrcode = (ImageView) rootView.findViewById(R.id.main_image_qr_code);
        sharingFragment = new FragmentSharing();
        final RelativeLayout mainFragmentQr = (RelativeLayout) rootView.findViewById(R.id.main_fragment_qr);
        mainAddressText = (TextView) rootView.findViewById(R.id.main_address_text);
        addressLayout = (RelativeLayout) rootView.findViewById(R.id.theAddressLayout);
        String bitcoinUrl = "bitcoin:" + receiveAddress;
        mainAddressText.setText(receiveAddress);
        BRWalletManager.getInstance(getActivity()).generateQR(bitcoinUrl, qrcode);
        fm = getActivity().getFragmentManager();
        mainFragmentQr.setPadding(0, MainActivity.screenParametersPoint.y / 5, 0, 0);
        final BreadWalletApp breadWalletApp = (BreadWalletApp) getActivity().getApplication();
        addressLayout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                breadWalletApp.cancelToast();
                if (BRAnimator.checkTheMultipressingAvailability()) {
                    MainActivity app = MainActivity.app;
                    if (app != null) app.hideAllBubbles();
//                    Log.e(TAG, "finalReceiveAddress: " + receiveAddress);
                    sharingFragment.setTheAddress(receiveAddress);
                    saveBitmapToFile();
                    sharingFragment.show(fm, FragmentSharing.class.getName());
                }
            }
        });
        qrcode.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showTips();
            }
        });
        qrImageLayout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                showTips();
            }
        });

        final MainActivity app = MainActivity.app;
        if (app != null) {
            app.qrBubble1 = (BubbleTextView) rootView.findViewById(R.id.qr_bubble1);
            app.qrBubble2 = (BubbleTextView) rootView.findViewById(R.id.qr_bubble2);
        }
        rootView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (app != null) {
                    app.hideAllBubbles();
                }
            }
        });

        setTipsPositions(rootView);

        return rootView;
    }

    private void setTipsPositions(final View rootView) {
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                View temp = rootView.findViewById(R.id.qr_image_address_layout);
                int qrBubble1Position = temp == null ? MainActivity.screenParametersPoint.y / 2 : temp.getHeight() / 3 + temp.getHeight() / 10;
                int qrBubble2Position = temp == null ? MainActivity.screenParametersPoint.y / 2 : temp.getHeight() - temp.getHeight() / 7;
                BRTipsManager.setQrBubblesPosition(qrBubble1Position, qrBubble2Position);
            }
        }, 200);
    }

    private void showTips() {
        if (BRAnimator.checkTheMultipressingAvailability()) {
            MainActivity app = MainActivity.app;
            if (app != null) {
                app.hideAllBubbles();
                if (bubbleState == 0) {
                    ((MainActivity) getActivity()).hideAllBubbles();
                    app.qrBubble1.setVisibility(View.VISIBLE);
                    app.qrBubble2.setVisibility(View.GONE);
                    SpringAnimator.showBubbleAnimation(app.qrBubble1);
                    bubbleState++;
                } else if (bubbleState == 1) {
                    app.qrBubble1.setVisibility(View.GONE);
                    app.qrBubble2.setVisibility(View.VISIBLE);
                    SpringAnimator.showBubbleAnimation(app.qrBubble2);
                    bubbleState++;
                } else {
                    app.hideAllBubbles();
                    bubbleState = 0;
                }
            }

        }
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

    }

    private void saveBitmapToFile() {


        ContextWrapper cw = new ContextWrapper(getActivity());
        // path to /data/data/yourapp/app_data/imageDir
        File directory = cw.getDir("qrcodes", Context.MODE_PRIVATE);
        // Create imageDir
        File mypath = new File(directory, "qrImage.jpeg");

        FileOutputStream fos = null;
        try {
            fos = new FileOutputStream(mypath);
            // Use the compress method on the BitMap object to write image to the OutputStream
            if (bitmap != null)
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, fos);
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            try {
                fos.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

    }

    public static File getQRImageFile(Activity app) {
        ContextWrapper cw = new ContextWrapper(app);
        // path to /data/data/yourapp/app_data/imageDir
        File directory = cw.getDir("qrcodes", Context.MODE_PRIVATE);
        return new File(directory, "qrImage.jpeg");

    }

    @Override
    public void onResume() {
        super.onResume();
        MiddleViewAdapter.resetMiddleView(getActivity(), null);
        refreshAddress(null);
    }

    public void refreshAddress(String str) {
        if (str != null) {
            receiveAddress = str;
        } else {
            receiveAddress = SharedPreferencesManager.getReceiveAddress(getActivity());
        }
        String bitcoinUrl = "bitcoin:" + receiveAddress;
        if (mainAddressText == null) return;
        mainAddressText.setText(receiveAddress);
        BRWalletManager.getInstance(getActivity()).generateQR(bitcoinUrl, qrcode);
    }

}
