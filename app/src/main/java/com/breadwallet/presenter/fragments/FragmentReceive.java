package com.breadwallet.presenter.fragments;

import android.app.Activity;
import android.app.Fragment;
import android.content.Context;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.Nullable;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.breadwallet.BreadApp;
import com.breadwallet.BuildConfig;
import com.breadwallet.R;
import com.breadwallet.presenter.customviews.BRButton;
import com.breadwallet.presenter.customviews.BRKeyboard;
import com.breadwallet.presenter.customviews.BRLinearLayoutWithCaret;
import com.breadwallet.tools.animation.BRAnimator;
import com.breadwallet.tools.animation.SlideDetector;
import com.breadwallet.tools.manager.BRClipboardManager;
import com.breadwallet.tools.manager.BRSharedPrefs;
import com.breadwallet.tools.qrcode.QRUtils;
import com.breadwallet.tools.threads.executor.BRExecutor;
import com.breadwallet.tools.util.BRConstants;
import com.breadwallet.tools.util.Utils;
import com.breadwallet.wallet.WalletsMaster;
import com.breadwallet.wallet.abstracts.BaseWalletManager;
import com.breadwallet.wallet.abstracts.OnBalanceChangedListener;
import com.breadwallet.wallet.util.CryptoUriParser;

import java.math.BigDecimal;

import static com.breadwallet.tools.animation.BRAnimator.animateBackgroundDim;
import static com.breadwallet.tools.animation.BRAnimator.animateSignalSlide;
import static com.platform.HTTPServer.URL_SUPPORT;


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
    private String mReceiveAddress;
    private View separator;
    private BRButton shareButton;
    private Button shareEmail;
    private Button shareTextMessage;
    private Button requestButton;
    private BRLinearLayoutWithCaret shareButtonsLayout;
    private BRLinearLayoutWithCaret copiedLayout;
    private boolean shareButtonsShown = false;
    private boolean showRequestAnAmount;
    private ImageButton close;
    private Handler copyCloseHandler = new Handler();
    private BRKeyboard keyboard;
    private View separator2;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        // The last two arguments ensure LayoutParams are inflated
        // properly.

        View rootView = inflater.inflate(R.layout.fragment_receive, container, false);
        mTitle = rootView.findViewById(R.id.title);
        mAddress = rootView.findViewById(R.id.address_text);
        mQrImage = rootView.findViewById(R.id.qr_image);
        backgroundLayout = rootView.findViewById(R.id.background_layout);
        signalLayout = rootView.findViewById(R.id.signal_layout);
        shareButton = rootView.findViewById(R.id.share_button);
        shareEmail = rootView.findViewById(R.id.share_email);
        shareTextMessage = rootView.findViewById(R.id.share_text);
        shareButtonsLayout = rootView.findViewById(R.id.share_buttons_layout);
        copiedLayout = rootView.findViewById(R.id.copied_layout);
        requestButton = rootView.findViewById(R.id.request_button);
        keyboard = rootView.findViewById(R.id.keyboard);
        keyboard.setBRButtonBackgroundResId(R.drawable.keyboard_white_button);
        keyboard.setBRKeyboardColor(R.color.white);
        separator = rootView.findViewById(R.id.separator);
        close = rootView.findViewById(R.id.close_button);
        separator2 = rootView.findViewById(R.id.separator2);
        separator2.setVisibility(View.GONE);
        setListeners();

        WalletsMaster.getInstance(getActivity()).getCurrentWallet(getActivity()).addBalanceChangedListener(new OnBalanceChangedListener() {
            @Override
            public void onBalanceChanged(String iso, BigDecimal newBalance) {
                updateQr();
            }
        });

        ImageButton faq = rootView.findViewById(R.id.faq_button);

        faq.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!BRAnimator.isClickAllowed()) return;
                Activity app = getActivity();
                if (app == null) {
                    Log.e(TAG, "onClick: app is null, can't start the webview with url: " + URL_SUPPORT);
                    return;
                }

                BaseWalletManager wm = WalletsMaster.getInstance(app).getCurrentWallet(app);
                BRAnimator.showSupportFragment(app, BRConstants.receive, wm);
            }
        });

        signalLayout.removeView(shareButtonsLayout);
        signalLayout.removeView(copiedLayout);

        signalLayout.setLayoutTransition(BRAnimator.getDefaultTransition());

        signalLayout.setOnTouchListener(new SlideDetector(getContext(), signalLayout));

        return rootView;
    }


    private void setListeners() {
        shareEmail.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!BRAnimator.isClickAllowed()) return;
                BaseWalletManager walletManager = WalletsMaster.getInstance(getActivity()).getCurrentWallet(getActivity());
                Uri cryptoUri = CryptoUriParser.createCryptoUrl(getActivity(), walletManager,
                        walletManager.decorateAddress(getActivity(), mReceiveAddress),
                        new BigDecimal(0), null, null, null);
                QRUtils.share("mailto:", getActivity(), cryptoUri.toString());


            }
        });
        shareTextMessage.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!BRAnimator.isClickAllowed()) return;
                BaseWalletManager walletManager = WalletsMaster.getInstance(getActivity()).getCurrentWallet(getActivity());
                Uri cryptoUri = CryptoUriParser.createCryptoUrl(getActivity(), walletManager,
                        walletManager.decorateAddress(getActivity(), mReceiveAddress),
                        new BigDecimal(0), null, null, null);
                QRUtils.share("sms:", getActivity(), cryptoUri.toString());
            }
        });
        shareButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!BRAnimator.isClickAllowed()) return;
                shareButtonsShown = !shareButtonsShown;
                showShareButtons(shareButtonsShown);
            }
        });
        mAddress.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!BRAnimator.isClickAllowed()) return;
                copyText();
            }
        });
        requestButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!BRAnimator.isClickAllowed()) return;
                Activity app = getActivity();
                app.getFragmentManager().popBackStack();
                BRAnimator.showRequestFragment(app);

            }
        });

        backgroundLayout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!BRAnimator.isClickAllowed()) return;
                getActivity().onBackPressed();
            }
        });
        mQrImage.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!BRAnimator.isClickAllowed()) return;
                copyText();
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

    private void showShareButtons(boolean b) {
        if (!b) {
            signalLayout.removeView(shareButtonsLayout);
            shareButton.setType(2);
        } else {
            signalLayout.addView(shareButtonsLayout, showRequestAnAmount ? signalLayout.getChildCount() - 2 : signalLayout.getChildCount());
            shareButton.setType(3);
            showCopiedLayout(false);
        }
    }

    private void showCopiedLayout(boolean b) {
        if (!b) {
            signalLayout.removeView(copiedLayout);
            copyCloseHandler.removeCallbacksAndMessages(null);
        } else {
            if (signalLayout.indexOfChild(copiedLayout) == -1) {
                signalLayout.addView(copiedLayout, signalLayout.indexOfChild(shareButton));
                showShareButtons(false);
                shareButtonsShown = false;
                copyCloseHandler.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        signalLayout.removeView(copiedLayout);
                    }
                }, 2000);
            } else {
                copyCloseHandler.removeCallbacksAndMessages(null);
                signalLayout.removeView(copiedLayout);
            }
        }
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        final ViewTreeObserver observer = signalLayout.getViewTreeObserver();
        observer.addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
            @Override
            public void onGlobalLayout() {
                observer.removeOnGlobalLayoutListener(this);
                animateBackgroundDim(backgroundLayout, false);
                animateSignalSlide(signalLayout, false, null);
            }
        });
        BaseWalletManager wm = WalletsMaster.getInstance(getActivity()).getCurrentWallet(getActivity());

        Bundle extras = getArguments();
        boolean isReceive = extras.getBoolean("receive");
        showRequestAnAmount = isReceive && wm.getUiConfiguration().showRequestAnAmount;
        if (!showRequestAnAmount) {
            signalLayout.removeView(separator);
            signalLayout.removeView(requestButton);
            mTitle.setText(getString(R.string.UnlockScreen_myAddress));
        }

        BRExecutor.getInstance().forLightWeightBackgroundTasks().execute(new Runnable() {
            @Override
            public void run() {
                updateQr();
            }
        });

    }

    private void updateQr() {
        final Context ctx = getContext() == null ? BreadApp.getBreadContext() : (Activity) getContext();
        BRExecutor.getInstance().forLightWeightBackgroundTasks().execute(new Runnable() {
            @Override
            public void run() {
                final BaseWalletManager wm = WalletsMaster.getInstance(ctx).getCurrentWallet(ctx);
                wm.refreshAddress(ctx);
                BRExecutor.getInstance().forMainThreadTasks().execute(new Runnable() {
                    @Override
                    public void run() {
                        mReceiveAddress = BRSharedPrefs.getReceiveAddress(ctx, wm.getIso(ctx));
                        String decorated = wm.decorateAddress(ctx, mReceiveAddress);
                        mAddress.setText(decorated);
                        Utils.correctTextSizeIfNeeded(mAddress);
                        Uri uri = CryptoUriParser.createCryptoUrl(ctx, wm, decorated, new BigDecimal(0), null, null, null);
                        boolean generated = QRUtils.generateQR(ctx, uri.toString(), mQrImage);
                        if (!generated)
                            throw new RuntimeException("failed to generate qr image for address");
                    }
                });
            }
        });

    }

    private void copyText() {
        Activity app = getActivity();
        BRClipboardManager.putClipboard(app, mAddress.getText().toString());
        //copy the legacy for testing purposes (testnet faucet money receiving)
        if (Utils.isEmulatorOrDebug(app) && BuildConfig.BITCOIN_TESTNET)
            BRClipboardManager.putClipboard(app, WalletsMaster.getInstance(app).getCurrentWallet(app).undecorateAddress(app, mAddress.getText().toString()));

        showCopiedLayout(true);
    }

    @Override
    public void onStop() {
        super.onStop();
        animateBackgroundDim(backgroundLayout, true);
        animateSignalSlide(signalLayout, true, new BRAnimator.OnSlideAnimationEnd() {
            @Override
            public void onAnimationEnd() {
                Activity app = getActivity();
                if (app != null && !app.isDestroyed())
                    app.getFragmentManager().popBackStack();
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