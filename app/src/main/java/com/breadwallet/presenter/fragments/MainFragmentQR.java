
package com.breadwallet.presenter.fragments;

import android.app.Activity;
import android.app.Fragment;
import android.app.FragmentManager;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Point;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.Display;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.breadwallet.R;
import com.breadwallet.BreadWalletApp;
import com.breadwallet.presenter.activities.MainActivity;
import com.breadwallet.presenter.customviews.BubbleTextView;
import com.breadwallet.tools.manager.SharedPreferencesManager;
import com.breadwallet.tools.adapter.MiddleViewAdapter;
import com.breadwallet.tools.animation.BRAnimator;
import com.breadwallet.tools.animation.SpringAnimator;
import com.breadwallet.tools.qrcode.QRCodeEncoder;
import com.breadwallet.wallet.BRWalletManager;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.WriterException;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

/**
 * BreadWallet
 * <p/>
 * Created by Mihail Gutan on 6/23/15.
 * Copyright (c) 2016 breadwallet llc <mihail@breadwallet.com>
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

public class MainFragmentQR extends Fragment {
    private static final String TAG = MainFragmentQR.class.getName();
    private ImageView qrcode;
    private TextView mainAddressText;
    private Bitmap bitmap;
    private FragmentSharing sharingFragment;
    private FragmentManager fm;
    public static File qrCodeImageFile;
    private String receiveAddress;
    private int bubbleState = 0;

    @Override
    public View onCreateView(LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        // The last two arguments ensure LayoutParams are inflated
        // properly.
        View rootView = inflater.inflate(R.layout.fragment_qr_main, container, false);

        BRWalletManager.refreshAddress();
        receiveAddress = SharedPreferencesManager.getReceiveAddress(getActivity());

        qrcode = (ImageView) rootView.findViewById(R.id.main_image_qr_code);
        sharingFragment = new FragmentSharing();
        final RelativeLayout main_fragment_qr = (RelativeLayout) rootView.findViewById(R.id.main_fragment_qr);
        mainAddressText = (TextView) rootView.findViewById(R.id.main_address_text);
        RelativeLayout addressLayout = (RelativeLayout) rootView.findViewById(R.id.theAddressLayout);
        generateQR();
        fm = getActivity().getFragmentManager();
        main_fragment_qr.setPadding(0, MainActivity.screenParametersPoint.y / 5, 0, 0);
        final BreadWalletApp breadWalletApp = (BreadWalletApp) MainActivity.app.getApplication();
        addressLayout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                breadWalletApp.cancelToast();
                if (BRAnimator.checkTheMultipressingAvailability()) {
                    MainActivity app = MainActivity.app;
                    if (app != null) app.hideAllBubbles();
                    Log.e(TAG, "finalReceiveAddress: " + receiveAddress);
                    sharingFragment.setTheAddress(receiveAddress);
                    saveBitmapToFile();
                    sharingFragment.show(fm, FragmentSharing.class.getName());
                }
            }
        });
        qrcode.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
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

        return rootView;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

    }

    private void generateQR() {
        Log.e(TAG, "generateQR: " + receiveAddress);
        Activity activity = getActivity();
        if (activity == null || qrcode == null) return;

        WindowManager manager = (WindowManager) activity.getSystemService(Context.WINDOW_SERVICE);
        Display display = manager.getDefaultDisplay();
        Point point = new Point();
        display.getSize(point);
        int width = point.x;
        int height = point.y;
        int smallerDimension = width < height ? width : height;
        smallerDimension = smallerDimension * 3 / 4;

        if (receiveAddress == null || receiveAddress.length() < 5)
            return;
        mainAddressText.setText(receiveAddress);
        QRCodeEncoder qrCodeEncoder = new QRCodeEncoder("bitcoin:" + receiveAddress,
                BarcodeFormat.QR_CODE.toString(),
                smallerDimension);
        try {
            bitmap = qrCodeEncoder.encodeAsBitmap();
        } catch (WriterException e) {
            e.printStackTrace();
        }
        qrcode.setPadding(1, 1, 1, 1);
        qrcode.setBackgroundResource(R.color.gray);
        qrcode.setImageBitmap(bitmap);

    }

    private void saveBitmapToFile() {
        FileOutputStream out = null;
        String path = Environment.getExternalStorageDirectory().toString();
        qrCodeImageFile = new File(path, "qrImage" + ".jpeg");
        if (qrCodeImageFile.exists()) {
            Log.d(TAG, "File exists! deleting");
            qrCodeImageFile.delete();
        } else {
            Log.d(TAG, "File did not exist, creating a new one");
        }
        try {
            out = new FileOutputStream(qrCodeImageFile);
            bitmap.compress(Bitmap.CompressFormat.JPEG, 100, out); // bmp is your Bitmap instance
            // PNG is a lossless format, the compression factor (100) is ignored
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            try {
                if (out != null) {
                    out.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
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
        generateQR();
    }

}
