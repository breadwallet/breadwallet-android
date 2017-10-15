package com.breadwallet.presenter.fragments;

import android.app.Activity;
import android.app.Fragment;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.Nullable;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.breadwallet.R;
import com.breadwallet.presenter.customviews.BRButton;
import com.breadwallet.presenter.customviews.BRKeyboard;
import com.breadwallet.presenter.customviews.BRLinearLayoutWithCaret;
import com.breadwallet.tools.adapter.CurAdapter;
import com.breadwallet.tools.animation.BRAnimator;
import com.breadwallet.tools.animation.SlideDetector;
import com.breadwallet.tools.listeners.RecyclerItemClickListener;
import com.breadwallet.tools.manager.BRClipboardManager;
import com.breadwallet.tools.manager.BRSharedPrefs;
import com.breadwallet.tools.qrcode.QRUtils;
import com.breadwallet.tools.sqlite.CurrencyDataSource;
import com.breadwallet.tools.util.BRExchange;
import com.breadwallet.tools.util.BRConstants;
import com.breadwallet.tools.util.BRCurrency;
import com.breadwallet.tools.util.Utils;
import com.breadwallet.wallet.BRWalletManager;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;


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

public class FragmentRequestAmount extends Fragment {
    private static final String TAG = FragmentRequestAmount.class.getName();
    private BRKeyboard keyboard;
    private StringBuilder amountBuilder;
    private TextView isoText;
    private EditText amountEdit;
    public TextView mTitle;
    public TextView mAddress;
    public ImageView mQrImage;
    public LinearLayout backgroundLayout;
    public LinearLayout signalLayout;
    private String receiveAddress;
    private BRButton shareButton;
    private Button shareEmail;
    private Button shareTextMessage;
    private boolean shareButtonsShown = true;
    private String selectedIso;
    private Button isoButton;
    private Handler copyCloseHandler = new Handler();
    private LinearLayout keyboardLayout;
    private RelativeLayout amountLayout;
    private Button request;
    private BRLinearLayoutWithCaret shareButtonsLayout;
    private BRLinearLayoutWithCaret copiedLayout;
    private int keyboardIndex;
//    private int currListIndex;
    private ImageButton close;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        View rootView = inflater.inflate(R.layout.fragment_receive, container, false);
        backgroundLayout = (LinearLayout) rootView.findViewById(R.id.background_layout);
        signalLayout = (LinearLayout) rootView.findViewById(R.id.signal_layout);
        shareButtonsLayout = (BRLinearLayoutWithCaret) rootView.findViewById(R.id.share_buttons_layout);
        copiedLayout = (BRLinearLayoutWithCaret) rootView.findViewById(R.id.copied_layout);
//        currencyListLayout = (LinearLayout) rootView.findViewById(R.id.cur_spinner_layout);
//        currencyListLayout.setVisibility(View.VISIBLE);
        request = (Button) rootView.findViewById(R.id.request_button);
        keyboardLayout = (LinearLayout) rootView.findViewById(R.id.keyboard_layout);
        keyboardLayout.setVisibility(View.VISIBLE);
        amountLayout = (RelativeLayout) rootView.findViewById(R.id.amount_layout);
        amountLayout.setVisibility(View.VISIBLE);
        keyboard = (BRKeyboard) rootView.findViewById(R.id.keyboard);
        keyboard.setBRButtonBackgroundResId(R.drawable.keyboard_white_button);
        keyboard.setBRKeyboardColor(R.color.white);
        isoText = (TextView) rootView.findViewById(R.id.iso_text);
        amountEdit = (EditText) rootView.findViewById(R.id.amount_edit);
        amountBuilder = new StringBuilder(0);
        isoButton = (Button) rootView.findViewById(R.id.iso_button);
        mTitle = (TextView) rootView.findViewById(R.id.title);
        mAddress = (TextView) rootView.findViewById(R.id.address_text);
        mQrImage = (ImageView) rootView.findViewById(R.id.qr_image);
        shareButton = (BRButton) rootView.findViewById(R.id.share_button);
        shareEmail = (Button) rootView.findViewById(R.id.share_email);
        shareTextMessage = (Button) rootView.findViewById(R.id.share_text);
        shareButtonsLayout = (BRLinearLayoutWithCaret) rootView.findViewById(R.id.share_buttons_layout);
        close = (ImageButton) rootView.findViewById(R.id.close_button);
        keyboardIndex = signalLayout.indexOfChild(keyboardLayout);

        mTitle.setText(getString(R.string.Receive_request));
        setListeners();

        signalLayout.removeView(shareButtonsLayout);
        signalLayout.removeView(copiedLayout);
        signalLayout.removeView(request);

        showCurrencyList(false);
        selectedIso = BRSharedPrefs.getPreferredBTC(getContext()) ? "LTC" : BRSharedPrefs.getIso(getContext());

        signalLayout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
//                removeCurrencySelector();
            }
        });
        updateText();

        signalLayout.setLayoutTransition(BRAnimator.getDefaultTransition());

        signalLayout.setOnTouchListener(new SlideDetector(getContext(), signalLayout));

        return rootView;
    }

    private void setListeners() {

        amountEdit.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                removeCurrencySelector();
                showKeyboard(true);
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

        mQrImage.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                removeCurrencySelector();
                showKeyboard(false);
            }
        });

        keyboard.addOnInsertListener(new BRKeyboard.OnInsertListener() {
            @Override
            public void onClick(String key) {
                removeCurrencySelector();
                handleClick(key);
            }
        });


        shareEmail.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                removeCurrencySelector();
                if (!BRAnimator.isClickAllowed()) return;
                showKeyboard(false);
                String iso = selectedIso;
                String strAmount = amountEdit.getText().toString();
                BigDecimal bigAmount = new BigDecimal((Utils.isNullOrEmpty(strAmount) || strAmount.equalsIgnoreCase(".")) ? "0" : strAmount);
                long amount = BRExchange.getSatoshisFromAmount(getActivity(), iso, bigAmount).longValue();
                String bitcoinUri = Utils.createBitcoinUrl(receiveAddress, amount, null, null, null);
                QRUtils.share("mailto:", getActivity(), bitcoinUri);

            }
        });
        shareTextMessage.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                removeCurrencySelector();
                if (!BRAnimator.isClickAllowed()) return;
                showKeyboard(false);
                String iso = selectedIso;
                String strAmount = amountEdit.getText().toString();
                BigDecimal bigAmount = new BigDecimal((Utils.isNullOrEmpty(strAmount) || strAmount.equalsIgnoreCase(".")) ? "0" : strAmount);
                long amount = BRExchange.getSatoshisFromAmount(getActivity(), iso, bigAmount).longValue();
                String bitcoinUri = Utils.createBitcoinUrl(receiveAddress, amount, null, null, null);
                QRUtils.share("sms:", getActivity(), bitcoinUri);
            }
        });
        shareButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!BRAnimator.isClickAllowed()) return;
                shareButtonsShown = !shareButtonsShown;
                showShareButtons(shareButtonsShown);
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

        backgroundLayout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                removeCurrencySelector();
                if (!BRAnimator.isClickAllowed()) return;
                getActivity().onBackPressed();
            }
        });

        isoButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (selectedIso.equalsIgnoreCase(BRSharedPrefs.getIso(getContext()))) {
                    selectedIso = "LTC";
                } else {
                    selectedIso = BRSharedPrefs.getIso(getContext());
                }
                boolean generated = generateQrImage(receiveAddress, amountEdit.getText().toString(), selectedIso);
                if (!generated) throw new RuntimeException("failed to generate qr image for address");
                updateText();
            }
        });

    }

    private void copyText() {
        BRClipboardManager.putClipboard(getContext(), mAddress.getText().toString());
        showCopiedLayout(true);
    }

    private void toggleShareButtonsVisibility() {

        if (shareButtonsShown) {
            signalLayout.removeView(shareButtonsLayout);
            shareButtonsShown = false;
        } else {
            signalLayout.addView(shareButtonsLayout, signalLayout.getChildCount());
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
                BRAnimator.animateBackgroundDim(backgroundLayout, false);
                BRAnimator.animateSignalSlide(signalLayout, false, null);
                toggleShareButtonsVisibility();
            }
        });

        new Thread(new Runnable() {
            @Override
            public void run() {
                boolean success = BRWalletManager.refreshAddress(getActivity());
                if (!success) throw new RuntimeException("failed to retrieve address");

                receiveAddress = BRSharedPrefs.getReceiveAddress(getActivity());

                getActivity().runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        mAddress.setText(receiveAddress);
                        boolean generated = generateQrImage(receiveAddress, "0", "LTC");
                        if (!generated)
                            throw new RuntimeException("failed to generate qr image for address");
                    }
                });
            }
        }).start();

    }

    @Override
    public void onStop() {
        super.onStop();
        BRAnimator.animateBackgroundDim(backgroundLayout, true);
        BRAnimator.animateSignalSlide(signalLayout, true, new BRAnimator.OnSlideAnimationEnd() {
            @Override
            public void onAnimationEnd() {
                if (getActivity() != null) {
                    try {
                        getActivity().getFragmentManager().popBackStack();
                    } catch (Exception ignored) {

                    }
                }
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

        boolean generated = generateQrImage(receiveAddress, amountEdit.getText().toString(), selectedIso);
        if (!generated) throw new RuntimeException("failed to generate qr image for address");
    }

    private void handleDigitClick(Integer dig) {
        String currAmount = amountBuilder.toString();
        String iso = selectedIso;
        if (new BigDecimal(currAmount.concat(String.valueOf(dig))).doubleValue()
                <= BRExchange.getMaxAmount(getActivity(), iso).doubleValue()) {
            //do not insert 0 if the balance is 0 now
            if (currAmount.equalsIgnoreCase("0")) amountBuilder = new StringBuilder("");
            if ((currAmount.contains(".") && (currAmount.length() - currAmount.indexOf(".") > BRCurrency.getMaxDecimalPlaces(iso))))
                return;
            amountBuilder.append(dig);
            updateText();
        }
    }

    private void handleSeparatorClick() {
        String currAmount = amountBuilder.toString();
        if (currAmount.contains(".") || BRCurrency.getMaxDecimalPlaces(selectedIso) == 0)
            return;
        amountBuilder.append(".");
        updateText();
    }

    private void handleDeleteClick() {
        String currAmount = amountBuilder.toString();
        if (currAmount.length() > 0) {
            amountBuilder.deleteCharAt(currAmount.length() - 1);
            updateText();
        }

    }

    private void updateText() {
        if (getActivity() == null) return;
        String tmpAmount = amountBuilder.toString();
        amountEdit.setText(tmpAmount);
        isoText.setText(BRCurrency.getSymbolByIso(getActivity(), selectedIso));
        isoButton.setText(String.format("%s(%s)", BRCurrency.getCurrencyName(getActivity(),selectedIso), BRCurrency.getSymbolByIso(getActivity(), selectedIso)));

    }

    private void showKeyboard(boolean b) {
        int curIndex = keyboardIndex;

        if (!b) {
            signalLayout.removeView(keyboardLayout);
        } else {
            if (signalLayout.indexOfChild(keyboardLayout) == -1)
                signalLayout.addView(keyboardLayout, curIndex);
            else
                signalLayout.removeView(keyboardLayout);

        }
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                signalLayout.scrollTo(5, 10);
            }
        }, 2000);

    }

    private boolean generateQrImage(String address, String strAmount, String iso) {
        String amountArg = "";
        if (strAmount != null && !strAmount.isEmpty()) {
            BigDecimal bigAmount = new BigDecimal((Utils.isNullOrEmpty(strAmount) || strAmount.equalsIgnoreCase(".")) ? "0" : strAmount);
            long amount = BRExchange.getSatoshisFromAmount(getActivity(), iso, bigAmount).longValue();
            String am = new BigDecimal(amount).divide(new BigDecimal(100000000), 8, BRConstants.ROUNDING_MODE).toPlainString();
            amountArg = "?amount=" + am;
        }
        return BRWalletManager.getInstance().generateQR(getActivity(), "litecoin:" + address + amountArg, mQrImage);
    }


    private void removeCurrencySelector() {
//        showCurrencyList(false);
    }

    private void showShareButtons(boolean b) {
        if (!b) {
            signalLayout.removeView(shareButtonsLayout);
            shareButton.setType(2);
        } else {
            signalLayout.addView(shareButtonsLayout, signalLayout.getChildCount() - 1);
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

    private void showCurrencyList(boolean b) {
    }


}