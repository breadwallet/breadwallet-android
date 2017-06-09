package com.breadwallet.presenter.fragments;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ArgbEvaluator;
import android.animation.LayoutTransition;
import android.animation.ValueAnimator;
import android.app.Activity;
import android.app.Fragment;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.view.animation.OvershootInterpolator;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.breadwallet.R;
import com.breadwallet.presenter.customviews.BRToast;
import com.breadwallet.tools.animation.BRAnimator;
import com.breadwallet.tools.animation.SlideDetector;
import com.breadwallet.tools.animation.SpringAnimator;
import com.breadwallet.tools.manager.BRClipboardManager;
import com.breadwallet.tools.manager.SharedPreferencesManager;
import com.breadwallet.tools.qrcode.QRUtils;
import com.breadwallet.tools.util.Utils;
import com.breadwallet.wallet.BRWalletManager;

import static com.breadwallet.tools.animation.BRAnimator.animateBackgroundDim;
import static com.breadwallet.tools.animation.BRAnimator.animateSignalSlide;


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

public class FragmentReceive extends Fragment {
    private static final String TAG = FragmentReceive.class.getName();

    public TextView mTitle;
    public TextView mAddress;
    public ImageView mQrImage;
    public LinearLayout backgroundLayout;
    public LinearLayout signalLayout;

    private String receiveAddress;
    private View shareSeparator;
    private View separator;
    private Button shareButton;
    private Button shareEmail;
    private Button shareTextMessage;
    private Button requestButton;
    private LinearLayout shareButtonsLayout;
    private boolean shareButtonsShown = true;
    private boolean isReceive;
    private ImageButton close;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        // The last two arguments ensure LayoutParams are inflated
        // properly.

        View rootView = inflater.inflate(R.layout.fragment_receive, container, false);
        mTitle = (TextView) rootView.findViewById(R.id.title);
        mAddress = (TextView) rootView.findViewById(R.id.address_text);
        mQrImage = (ImageView) rootView.findViewById(R.id.qr_image);
        backgroundLayout = (LinearLayout) rootView.findViewById(R.id.background_layout);
        signalLayout = (LinearLayout) rootView.findViewById(R.id.signal_layout);
        shareButton = (Button) rootView.findViewById(R.id.share_button);
        shareEmail = (Button) rootView.findViewById(R.id.share_email);
        shareTextMessage = (Button) rootView.findViewById(R.id.share_text);
        shareButtonsLayout = (LinearLayout) rootView.findViewById(R.id.share_buttons_layout);
        requestButton = (Button) rootView.findViewById(R.id.request_button);
        shareSeparator = rootView.findViewById(R.id.share_separator);
        separator = rootView.findViewById(R.id.separator);
        close = (ImageButton) rootView.findViewById(R.id.close_button);
        LayoutTransition layoutTransition = signalLayout.getLayoutTransition();
        layoutTransition.enableTransitionType(LayoutTransition.CHANGING);
        setListeners();
        signalLayout.setOnTouchListener(new SlideDetector(getContext(), signalLayout));
        BRWalletManager.getInstance().addBalanceChangedListener(new BRWalletManager.OnBalanceChanged() {
            @Override
            public void onBalanceChanged(long balance) {
                updateQr();
            }
        });

        return rootView;
    }


    private void setListeners() {
        shareEmail.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!BRAnimator.isClickAllowed()) return;
                SpringAnimator.springView(v);
                String bitcoinUri = Utils.createBitcoinUrl(receiveAddress, 0, null, null, null);
                QRUtils.share("mailto:", getActivity(), bitcoinUri);

            }
        });
        shareTextMessage.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!BRAnimator.isClickAllowed()) return;
                SpringAnimator.springView(v);
                String bitcoinUri = Utils.createBitcoinUrl(receiveAddress, 0, null, null, null);
                QRUtils.share("sms:", getActivity(), bitcoinUri);
            }
        });
        shareButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!BRAnimator.isClickAllowed()) return;
                SpringAnimator.springView(v);
                toggleShareButtonsVisibility();
            }
        });
        mAddress.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!BRAnimator.isClickAllowed()) return;
                BRClipboardManager.putClipboard(getContext(), mAddress.getText().toString());
                BRToast.showCustomToast(getActivity(), "Copied to Clipboard.", (int) mAddress.getY(), Toast.LENGTH_SHORT, R.drawable.toast_layout_blue);
            }
        });
        requestButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!BRAnimator.isClickAllowed()) return;
                SpringAnimator.springView(v);
                Activity app = getActivity();
                app.onBackPressed();
                BRAnimator.showRequestFragment(app, receiveAddress);

            }
        });

        backgroundLayout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!BRAnimator.isClickAllowed()) return;
                getActivity().onBackPressed();
            }
        });

        close.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Activity app = getActivity();
                if (app != null)
                    app.getFragmentManager().popBackStack();
            }
        });
    }

    private void toggleShareButtonsVisibility() {

        if (shareButtonsShown) {
//            shareButton.setBackground(getResources().getDrawable(R.drawable.button_secondary_gray_stroke));
//            shareButton.setCompoundDrawables(getResources().getDrawable(R.drawable.ic_share_vertical_gray), null, null, null);
            signalLayout.removeView(shareButtonsLayout);
            signalLayout.removeView(shareSeparator);
            shareButtonsShown = false;
        } else {
//            shareButton.setBackground(getResources().getDrawable(R.drawable.button_secondary_blue_stroke));
//            shareButton.setCompoundDrawables(getResources().getDrawable(R.drawable.ic_share_vertical_blue), null, null, null);
            signalLayout.addView(shareSeparator, isReceive ? signalLayout.getChildCount() - 2 : signalLayout.getChildCount());
            signalLayout.addView(shareButtonsLayout, isReceive ? signalLayout.getChildCount() - 2 : signalLayout.getChildCount());

            shareButtonsShown = true;
        }

    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        final ViewTreeObserver observer = signalLayout.getViewTreeObserver();
        observer.addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
            @Override
            public void onGlobalLayout() {
                observer.removeGlobalOnLayoutListener(this);
                animateBackgroundDim(backgroundLayout, false);
                animateSignalSlide(signalLayout, false, null);
                toggleShareButtonsVisibility();
            }
        });

        Bundle extras = getArguments();
        isReceive = extras.getBoolean("receive");
        if (!isReceive) {
            signalLayout.removeView(separator);
            signalLayout.removeView(requestButton);
            mTitle.setText("My Address");
        }

        new Thread(new Runnable() {
            @Override
            public void run() {
                updateQr();
            }
        }).start();

    }

    private void updateQr(){
        boolean success = BRWalletManager.refreshAddress(getActivity());
        if (!success) throw new RuntimeException("failed to retrieve address");
        getActivity().runOnUiThread(new Runnable() {
            @Override
            public void run() {
                receiveAddress = SharedPreferencesManager.getReceiveAddress(getActivity());
                mAddress.setText(receiveAddress);
                boolean generated = BRWalletManager.getInstance().generateQR(getActivity(), "bitcoin:" + receiveAddress, mQrImage);
                if (!generated)
                    throw new RuntimeException("failed to generate qr image for address");
            }
        });
    }


    @Override
    public void onStop() {
        super.onStop();
        animateBackgroundDim(backgroundLayout, true);
        animateSignalSlide( signalLayout, true, new BRAnimator.OnSlideAnimationEnd() {
            @Override
            public void onAnimationEnd() {
                if (getActivity() != null)
                    getActivity().getFragmentManager().popBackStack();
            }
        });
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