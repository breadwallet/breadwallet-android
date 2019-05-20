/**
 * BreadWallet
 * <p>
 * Created by Mihail Gutan <mihail@breadwallet.com> on 10/09/2018.
 * Copyright (c) 2018 breadwallet LLC
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

package com.breadwallet.presenter.fragments;

import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.text.format.DateUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import com.breadwallet.BuildConfig;
import com.breadwallet.R;
import com.breadwallet.presenter.customviews.BRButton;
import com.breadwallet.presenter.customviews.BRLinearLayoutWithCaret;
import com.breadwallet.presenter.entities.CryptoRequest;
import com.breadwallet.presenter.fragments.utils.ModalDialogFragment;
import com.breadwallet.tools.animation.SlideDetector;
import com.breadwallet.tools.animation.UiUtils;
import com.breadwallet.tools.manager.BRClipboardManager;
import com.breadwallet.tools.qrcode.QRUtils;
import com.breadwallet.tools.threads.executor.BRExecutor;
import com.breadwallet.tools.util.Utils;
import com.breadwallet.wallet.WalletsMaster;
import com.breadwallet.wallet.abstracts.BalanceUpdateListener;
import com.breadwallet.wallet.abstracts.BaseWalletManager;
import com.breadwallet.wallet.util.CryptoUriParser;
import com.breadwallet.wallet.wallets.bitcoin.WalletBitcoinManager;

import java.math.BigDecimal;
import java.util.Map;

public class FragmentShowLegacyAddress extends ModalDialogFragment implements BalanceUpdateListener {
    private static final String TAG = FragmentShowLegacyAddress.class.getName();
    public TextView mTitle;
    public TextView mAddress;
    public ImageView mQrImage;
    private String mReceiveAddress;
    private BRButton mShareButton;
    private BRLinearLayoutWithCaret mCopiedLayout;
    private ImageButton mCloseButton;
    private Handler mCopyHandler = new Handler();
    private ViewGroup mBackgroundLayout;
    private ViewGroup mSignalLayout;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        // The last two arguments ensure LayoutParams are inflated properly.

        ViewGroup rootView = assignRootView((ViewGroup) inflater.inflate(R.layout.fragment_show_legacy_address, container, false));
        mBackgroundLayout = assignBackgroundLayout((ViewGroup) rootView.findViewById(R.id.background_layout));
        mSignalLayout = assignSignalLayout((ViewGroup) rootView.findViewById(R.id.signal_layout));
        mTitle = rootView.findViewById(R.id.title);
        mAddress = rootView.findViewById(R.id.address_text);
        mQrImage = rootView.findViewById(R.id.qr_image);
        mShareButton = rootView.findViewById(R.id.share_button);
        mCopiedLayout = rootView.findViewById(R.id.copied_layout);
        mCloseButton = rootView.findViewById(R.id.close_button);
        setListeners();
        WalletsMaster.getInstance().getCurrentWallet(getActivity()).addBalanceChangedListener(this);
        mSignalLayout.removeView(mCopiedLayout);
        mSignalLayout.setLayoutTransition(UiUtils.getDefaultTransition());
        mSignalLayout.setOnTouchListener(new SlideDetector(getContext(), mSignalLayout));
        return rootView;
    }

    private void setListeners() {
        mShareButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                BaseWalletManager walletManager = WalletsMaster.getInstance().getCurrentWallet(getActivity());
                CryptoRequest cryptoRequest = new CryptoRequest.Builder().setAddress(walletManager.decorateAddress(mReceiveAddress)).setAmount(BigDecimal.ZERO).build();
                Uri cryptoUri = CryptoUriParser.createCryptoUrl(getActivity(), walletManager, cryptoRequest);
                QRUtils.sendShareIntent(getActivity(), cryptoUri.toString(), cryptoRequest.getAddress(false));
            }
        });
        mAddress.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                copyText();
            }
        });
        mBackgroundLayout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (!UiUtils.isClickAllowed()) {
                    return;
                }
                getActivity().onBackPressed();
            }
        });
        mQrImage.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                copyText();
            }
        });
        mCloseButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                closeWithAnimation();
            }
        });
    }

    private void showCopiedLayout(boolean show) {
        if (show) {
            if (mSignalLayout.indexOfChild(mCopiedLayout) == -1) {
                mSignalLayout.addView(mCopiedLayout, mSignalLayout.indexOfChild(mShareButton));
                mCopyHandler.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        mSignalLayout.removeView(mCopiedLayout);
                    }
                }, DateUtils.SECOND_IN_MILLIS * 2);
            } else {
                mCopyHandler.removeCallbacksAndMessages(null);
                mSignalLayout.removeView(mCopiedLayout);
            }
        } else {
            mSignalLayout.removeView(mCopiedLayout);
            mCopyHandler.removeCallbacksAndMessages(null);
        }
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        updateQr();
    }

    private void updateQr() {
        BRExecutor.getInstance().forLightWeightBackgroundTasks().execute(new Runnable() {
            @Override
            public void run() {
                final WalletBitcoinManager walletBitcoinManager = WalletBitcoinManager.getInstance(getActivity());
                walletBitcoinManager.refreshAddress(getActivity());
                BRExecutor.getInstance().forMainThreadTasks().execute(new Runnable() {
                    @Override
                    public void run() {
                        mReceiveAddress = walletBitcoinManager.getWallet().getLegacyAddress().stringify();
                        String decorated = walletBitcoinManager.decorateAddress(mReceiveAddress);
                        mAddress.setText(decorated);
                        Utils.correctTextSizeIfNeeded(mAddress);
                        CryptoRequest request = new CryptoRequest.Builder().setAddress(decorated).setAmount(BigDecimal.ZERO).build();
                        Uri uri = CryptoUriParser.createCryptoUrl(getActivity(), walletBitcoinManager, request);
                        if (!QRUtils.generateQR(getActivity(), uri.toString(), mQrImage)) {
                            throw new IllegalStateException("failed to generate qr image for address");
                        }
                    }
                });
            }
        });
    }

    private void copyText() {
        BRClipboardManager.putClipboard(getActivity(), mAddress.getText().toString());
        // The testnet does not work with the BCH address format so copy the legacy address for testing purposes.
        if (BuildConfig.BITCOIN_TESTNET) {
            BRClipboardManager.putClipboard(getActivity(), WalletsMaster.getInstance()
                    .getCurrentWallet(getActivity()).undecorateAddress(mAddress.getText().toString()));
        }

        showCopiedLayout(true);
    }

    @Override
    public void onResume() {
        super.onResume();
        WalletsMaster.getInstance().getCurrentWallet(getActivity()).addBalanceChangedListener(this);
    }

    @Override
    public void onPause() {
        super.onPause();
        WalletsMaster.getInstance().getCurrentWallet(getActivity()).removeBalanceChangedListener(this);
    }

    @Override
    public void onBalanceChanged(String currencyCode, BigDecimal newBalance) {
        updateQr();
    }

    @Override
    public void onBalancesChanged(Map<String, BigDecimal> balanceMap) {
        updateQr();
    }
}