package com.breadwallet.presenter.fragments;

import android.app.Activity;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.Nullable;
import android.support.v4.app.FragmentActivity;
import android.text.format.DateUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.breadwallet.presenter.customviews.BRButton;
import com.breadwallet.presenter.customviews.BRKeyboard;
import com.breadwallet.presenter.customviews.BRLinearLayoutWithCaret;
import com.breadwallet.presenter.entities.CryptoRequest;
import com.breadwallet.presenter.fragments.utils.ModalDialogFragment;
import com.breadwallet.tools.animation.UiUtils;
import com.breadwallet.tools.animation.SlideDetector;
import com.breadwallet.tools.manager.BRClipboardManager;
import com.breadwallet.tools.manager.BRSharedPrefs;
import com.breadwallet.tools.qrcode.QRUtils;
import com.breadwallet.tools.threads.executor.BRExecutor;
import com.breadwallet.tools.util.BRConstants;
import com.breadwallet.tools.util.CurrencyUtils;
import com.breadwallet.tools.util.Utils;
import com.breadwallet.wallet.WalletsMaster;
import com.breadwallet.wallet.abstracts.BaseWalletManager;
import com.breadwallet.wallet.util.CryptoUriParser;
import com.breadwallet.R;
import com.platform.HTTPServer;

import java.math.BigDecimal;


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

public class FragmentRequestAmount extends ModalDialogFragment implements BRKeyboard.OnInsertListener {
    private static final String TAG = FragmentRequestAmount.class.getName();
    private BRKeyboard mKeyboard;
    private StringBuilder mAmountBuilder;
    private TextView mCurrencyCodeText;
    private EditText mAmountEdit;
    public TextView mTitle;
    public TextView mAddress;
    public ImageView mQrImage;
    private String mReceiveAddress;
    private BRButton mShareButton;
    private boolean mShareButtonsShown = true;
    private String mSelectedCurrencyCode;
    private Button mCurrencyCodeButton;
    private Handler mCopyHandler = new Handler();
    private LinearLayout mKeyboardLayout;
    private RelativeLayout mAmountLayout;
    private Button mRequestButton;
    private BRLinearLayoutWithCaret mCopiedLayout;
    private int mKeyboardIndex;
    private ImageButton mCloseButton;
    private BaseWalletManager mWallet;
    private ViewGroup mBackgroundLayout;
    private ViewGroup mSignalLayout;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        ViewGroup rootView = assignRootView((ViewGroup) inflater.inflate(R.layout.fragment_receive, container, false));
        mBackgroundLayout = assignBackgroundLayout((ViewGroup) rootView.findViewById(R.id.background_layout));
        mSignalLayout = assignSignalLayout((ViewGroup) rootView.findViewById(R.id.signal_layout));
        mCopiedLayout = rootView.findViewById(R.id.copied_layout);
        mRequestButton = rootView.findViewById(R.id.request_button);
        mKeyboardLayout = rootView.findViewById(R.id.keyboard_layout);
        mKeyboardLayout.setVisibility(View.VISIBLE);
        mAmountLayout = rootView.findViewById(R.id.amount_layout);
        mAmountLayout.setVisibility(View.VISIBLE);
        mKeyboard = rootView.findViewById(R.id.keyboard);
        mKeyboard.setDeleteImage(R.drawable.ic_delete_gray);
        mKeyboard.setBRKeyboardColor(R.color.white);
        mCurrencyCodeText = rootView.findViewById(R.id.iso_text);
        mAmountEdit = rootView.findViewById(R.id.amount_edit);
        mAmountBuilder = new StringBuilder(0);
        mCurrencyCodeButton = rootView.findViewById(R.id.iso_button);
        mTitle = rootView.findViewById(R.id.title);
        mAddress = rootView.findViewById(R.id.address_text);
        mQrImage = rootView.findViewById(R.id.qr_image);
        mShareButton = rootView.findViewById(R.id.share_button);
        mCloseButton = rootView.findViewById(R.id.close_button);
        mKeyboardIndex = mSignalLayout.indexOfChild(mKeyboardLayout);

        ImageButton faq = rootView.findViewById(R.id.faq_button);

        faq.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!UiUtils.isClickAllowed()) return;
                Activity app = getActivity();
                if (app == null) {
                    Log.e(TAG, "onClick: app is null, can't start the webview with url: "
                            + (HTTPServer.getPlatformUrl(HTTPServer.URL_SUPPORT)));
                    return;
                }
                BaseWalletManager wm = WalletsMaster.getInstance().getCurrentWallet(app);

                UiUtils.showSupportFragment((FragmentActivity) app, BRConstants.FAQ_REQUEST_AMOUNT, wm);
            }
        });

        mTitle.setText(getString(R.string.Receive_request));
        setListeners();

        mSignalLayout.removeView(mCopiedLayout);
        mSignalLayout.removeView(mRequestButton);

        showCurrencyList(false);

        String currentIso = BRSharedPrefs.getCurrentWalletCurrencyCode(getActivity());
        mSelectedCurrencyCode = BRSharedPrefs.isCryptoPreferred(getActivity()) ? currentIso : BRSharedPrefs.getPreferredFiatIso(getContext());

        mSignalLayout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
//                removeCurrencySelector();
            }
        });
        updateText();

        mSignalLayout.setLayoutTransition(UiUtils.getDefaultTransition());

        mSignalLayout.setOnTouchListener(new SlideDetector(getContext(), mSignalLayout));

        return rootView;
    }

    private void setListeners() {
        mAmountEdit.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                removeCurrencySelector();
                showKeyboard(true);
            }
        });

        mCloseButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                closeWithAnimation();
            }
        });

        mQrImage.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                removeCurrencySelector();
                showKeyboard(false);
            }
        });

        mKeyboard.setOnInsertListener(this);

        mShareButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!UiUtils.isClickAllowed()) {
                    return;
                }
                BaseWalletManager walletManager = WalletsMaster.getInstance().getCurrentWallet(getActivity());
                CryptoRequest cryptoRequest = new CryptoRequest.Builder().setAddress(walletManager.decorateAddress(mReceiveAddress)).setAmount(getAmount()).build();
                Uri cryptoUri = CryptoUriParser.createCryptoUrl(getActivity(), walletManager, cryptoRequest);
                QRUtils.sendShareIntent(getActivity(), cryptoUri.toString(), cryptoUri.toString());
                showKeyboard(false);
            }
        });
        mAddress.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                removeCurrencySelector();
                copyText();
                showKeyboard(false);
            }
        });

        mBackgroundLayout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                removeCurrencySelector();
                if (!UiUtils.isClickAllowed()) return;
                getActivity().onBackPressed();
            }
        });

        mCurrencyCodeButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mSelectedCurrencyCode.equalsIgnoreCase(BRSharedPrefs.getPreferredFiatIso(getContext()))) {
                    mSelectedCurrencyCode = mWallet.getCurrencyCode();
                } else {
                    mSelectedCurrencyCode = BRSharedPrefs.getPreferredFiatIso(getContext());
                }
                BaseWalletManager wm = WalletsMaster.getInstance().getCurrentWallet(getActivity());

                boolean generated = generateQrImage(wm.decorateAddress(mReceiveAddress), mAmountEdit.getText().toString(), mSelectedCurrencyCode);
                if (!generated)
                    throw new RuntimeException("failed to generate qr image for address");
                updateText();
            }
        });

    }

    private void copyText() {
        BRClipboardManager.putClipboard(getContext(), mAddress.getText().toString());
        showCopiedLayout(true);
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        BRExecutor.getInstance().forLightWeightBackgroundTasks().execute(new Runnable() {
            @Override
            public void run() {
                final BaseWalletManager wallet = WalletsMaster.getInstance().getCurrentWallet(getActivity());

                wallet.refreshAddress(getActivity());
                mReceiveAddress = wallet.getAddress(getContext());

                BRExecutor.getInstance().forMainThreadTasks().execute(new Runnable() {
                    @Override
                    public void run() {
                        mAddress.setText(mWallet.decorateAddress(mReceiveAddress));
                        boolean generated = generateQrImage(mWallet.decorateAddress(mReceiveAddress), null, wallet.getCurrencyCode());
                        if (!generated)
                            throw new RuntimeException("failed to generate qr image for address");
                    }
                });
            }
        });

    }

    @Override
    public void onResume() {
        super.onResume();
        mWallet = WalletsMaster.getInstance().getCurrentWallet(getActivity());
    }

    @Override
    public void onPause() {
        super.onPause();
    }


    private void handleClick(String key) {
        if (key == null) {
            Log.e(TAG, "handleClick: key is null! ");
            return;
        }

        if (key.isEmpty()) {
            handleDeleteClick();
        } else if (Character.isDigit(key.charAt(0))) {
            handleDigitClick(Integer.parseInt(key.substring(0, 1)));
        } else if (key.charAt(0) == '.') {
            handleSeparatorClick();
        }
        BaseWalletManager wm = WalletsMaster.getInstance().getCurrentWallet(getActivity());

        boolean generated = generateQrImage(wm.decorateAddress(mReceiveAddress), mAmountEdit.getText().toString(), mSelectedCurrencyCode);
        if (!generated) throw new RuntimeException("failed to generate qr image for address");
    }

    private void handleDigitClick(Integer digit) {
        String currAmount = mAmountBuilder.toString();
        if (new BigDecimal(currAmount.concat(String.valueOf(digit))).doubleValue()
                <= mWallet.getMaxAmount(getActivity()).doubleValue()) {
            //do not insert 0 if the balance is 0 now
            if (currAmount.equalsIgnoreCase("0")) mAmountBuilder = new StringBuilder("");
            if ((currAmount.contains(".") && (currAmount.length() - currAmount.indexOf(".") > CurrencyUtils.getMaxDecimalPlaces(getActivity(), mSelectedCurrencyCode))))
                return;
            mAmountBuilder.append(digit);
            updateText();
        }
    }

    private void handleSeparatorClick() {
        String currAmount = mAmountBuilder.toString();
        if (currAmount.contains(".") || CurrencyUtils.getMaxDecimalPlaces(getActivity(), mSelectedCurrencyCode) == 0)
            return;
        mAmountBuilder.append(".");
        updateText();
    }

    private void handleDeleteClick() {
        String currAmount = mAmountBuilder.toString();
        if (currAmount.length() > 0) {
            mAmountBuilder.deleteCharAt(currAmount.length() - 1);
            updateText();
        }

    }

    private void updateText() {
        if (getActivity() == null) return;
        String tmpAmount = mAmountBuilder.toString();
        mAmountEdit.setText(tmpAmount);
        mCurrencyCodeText.setText(CurrencyUtils.getSymbolByIso(getActivity(), mSelectedCurrencyCode));
        mCurrencyCodeButton.setText(String.format("%s(%s)", mSelectedCurrencyCode, CurrencyUtils.getSymbolByIso(getActivity(), mSelectedCurrencyCode)));

    }

    private void showKeyboard(boolean b) {
        if (!b) {
            mSignalLayout.removeView(mKeyboardLayout);
        } else {
            if (mSignalLayout.indexOfChild(mKeyboardLayout) == -1)
                mSignalLayout.addView(mKeyboardLayout, mKeyboardIndex);
            else
                mSignalLayout.removeView(mKeyboardLayout);

        }
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                mSignalLayout.scrollTo(5, 10);
            }
        }, DateUtils.SECOND_IN_MILLIS * 2);

    }

    private boolean generateQrImage(String address, String strAmount, String iso) {
        BaseWalletManager walletManager = WalletsMaster.getInstance().getCurrentWallet(getActivity());

        boolean isCrypto = WalletsMaster.getInstance().isIsoCrypto(getActivity(), iso);

        BigDecimal bigAmount = new BigDecimal((Utils.isNullOrEmpty(strAmount) || strAmount.equalsIgnoreCase(".")) ? "0" : strAmount);

        BigDecimal amount = isCrypto ? walletManager.getSmallestCryptoForCrypto(getActivity(), bigAmount) : walletManager.getSmallestCryptoForFiat(getActivity(), bigAmount);

        Uri uri = CryptoUriParser.createCryptoUrl(getActivity(), walletManager,
                new CryptoRequest.Builder()
                        .setAddress(address)
                        .setAmount(amount)
                        .build());

        return QRUtils.generateQR(getActivity(), uri.toString(), mQrImage);
    }

    private void removeCurrencySelector() {
//        showCurrencyList(false);
    }

    private BigDecimal getAmount() {
        BaseWalletManager wm = WalletsMaster.getInstance().getCurrentWallet(getActivity());
        String strAmount = mAmountEdit.getText().toString();
        boolean isIsoCrypto = WalletsMaster.getInstance().isIsoCrypto(getActivity(), mSelectedCurrencyCode);
        BigDecimal bigAmount = new BigDecimal((Utils.isNullOrEmpty(strAmount) || strAmount.equalsIgnoreCase(".")) ? "0" : strAmount);
        return isIsoCrypto ? wm.getSmallestCryptoForCrypto(getActivity(), bigAmount) : wm.getSmallestCryptoForFiat(getActivity(), bigAmount);
    }


    private void showCopiedLayout(boolean b) {
        if (!b) {
            mSignalLayout.removeView(mCopiedLayout);
            mCopyHandler.removeCallbacksAndMessages(null);
        } else {
            if (mSignalLayout.indexOfChild(mCopiedLayout) == -1) {
                mSignalLayout.addView(mCopiedLayout, mSignalLayout.indexOfChild(mShareButton));
                mShareButtonsShown = false;
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
        }
    }

    private void showCurrencyList(boolean b) {
    }


    @Override
    public void onKeyInsert(String key) {
        removeCurrencySelector();
        handleClick(key);
    }
}