package com.breadwallet.presenter.fragments;

import android.app.Activity;
import android.app.Fragment;
import android.content.res.Configuration;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.constraint.ConstraintLayout;
import android.support.constraint.ConstraintSet;
import android.support.transition.AutoTransition;
import android.support.transition.TransitionManager;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.view.animation.OvershootInterpolator;
import android.view.inputmethod.EditorInfo;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import com.breadwallet.R;
import com.breadwallet.core.BRCoreAddress;
import com.breadwallet.core.BRCoreTransaction;
import com.breadwallet.presenter.customviews.BRButton;
import com.breadwallet.presenter.customviews.BRDialogView;
import com.breadwallet.presenter.customviews.BRKeyboard;
import com.breadwallet.presenter.customviews.BRLinearLayoutWithCaret;
import com.breadwallet.presenter.customviews.BRText;
import com.breadwallet.presenter.entities.PaymentItem;
import com.breadwallet.presenter.entities.RequestObject;
import com.breadwallet.tools.animation.BRAnimator;
import com.breadwallet.tools.animation.BRDialog;
import com.breadwallet.tools.animation.SlideDetector;
import com.breadwallet.tools.animation.SpringAnimator;
import com.breadwallet.tools.manager.BRClipboardManager;
import com.breadwallet.tools.manager.BRReportsManager;
import com.breadwallet.tools.manager.BRSharedPrefs;
import com.breadwallet.tools.manager.SendManager;
import com.breadwallet.wallet.abstracts.BaseWalletManager;
import com.breadwallet.wallet.wallets.util.CryptoUriParser;
import com.breadwallet.tools.threads.BRExecutor;
import com.breadwallet.tools.util.BRConstants;
import com.breadwallet.tools.util.CurrencyUtils;
import com.breadwallet.tools.util.Utils;
import com.breadwallet.wallet.WalletsMaster;

import java.math.BigDecimal;

import static com.breadwallet.wallet.wallets.util.CryptoUriParser.parseRequest;
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

public class FragmentSend extends Fragment {
    private static final String TAG = FragmentSend.class.getName();
    public ScrollView backgroundLayout;
    public LinearLayout signalLayout;
    private BRKeyboard keyboard;
    private EditText addressEdit;
    private Button scan;
    private Button paste;
    private Button send;
    private EditText commentEdit;
    private StringBuilder amountBuilder;
    private TextView isoText;
    private EditText amountEdit;
    private TextView balanceText;
    private TextView feeText;
    private ImageView edit;
    private long curBalance;
    private String selectedIso;
    private Button isoButton;
    private int keyboardIndex;
    private LinearLayout keyboardLayout;
    private ImageButton close;
    private ConstraintLayout amountLayout;
    private BRButton regular;
    private BRButton economy;
    private BRLinearLayoutWithCaret feeLayout;
    private boolean feeButtonsShown = false;
    private BRText feeDescription;
    private BRText warningText;
    private boolean amountLabelOn = true;

    private static String savedMemo;
    private static String savedIso;
    private static String savedAmount;

    private boolean ignoreCleanup;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        View rootView = inflater.inflate(R.layout.fragment_send, container, false);
        backgroundLayout = (ScrollView) rootView.findViewById(R.id.background_layout);
        signalLayout = (LinearLayout) rootView.findViewById(R.id.signal_layout);
        keyboard = (BRKeyboard) rootView.findViewById(R.id.keyboard);
        keyboard.setBRButtonBackgroundResId(R.drawable.keyboard_white_button);
        keyboard.setBRKeyboardColor(R.color.white);
        isoText = (TextView) rootView.findViewById(R.id.iso_text);
        addressEdit = (EditText) rootView.findViewById(R.id.address_edit);
        scan = (Button) rootView.findViewById(R.id.scan);
        paste = (Button) rootView.findViewById(R.id.paste_button);
        send = (Button) rootView.findViewById(R.id.send_button);
        commentEdit = (EditText) rootView.findViewById(R.id.comment_edit);
        amountEdit = (EditText) rootView.findViewById(R.id.amount_edit);
        balanceText = (TextView) rootView.findViewById(R.id.balance_text);
        feeText = (TextView) rootView.findViewById(R.id.fee_text);
        edit = (ImageView) rootView.findViewById(R.id.edit);
        isoButton = (Button) rootView.findViewById(R.id.iso_button);
        keyboardLayout = (LinearLayout) rootView.findViewById(R.id.keyboard_layout);
        amountLayout = (ConstraintLayout) rootView.findViewById(R.id.amount_layout);
        feeLayout = (BRLinearLayoutWithCaret) rootView.findViewById(R.id.fee_buttons_layout);
        feeDescription = (BRText) rootView.findViewById(R.id.fee_description);
        warningText = (BRText) rootView.findViewById(R.id.warning_text);

        regular = (BRButton) rootView.findViewById(R.id.left_button);
        economy = (BRButton) rootView.findViewById(R.id.right_button);
        close = (ImageButton) rootView.findViewById(R.id.close_button);
        selectedIso = BRSharedPrefs.isCryptoPreferred(getActivity()) ? WalletsMaster.getInstance(getActivity()).getCurrentWallet(getActivity()).getIso(getActivity()) : BRSharedPrefs.getPreferredFiatIso(getContext());

        amountBuilder = new StringBuilder(0);
        setListeners();
        isoText.setText(getString(R.string.Send_amountLabel));
        isoText.setTextSize(18);
        isoText.setTextColor(getContext().getColor(R.color.light_gray));
        isoText.requestLayout();
        signalLayout.setOnTouchListener(new SlideDetector(getContext(), signalLayout));

        signalLayout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
            }
        });


        showFeeSelectionButtons(feeButtonsShown);

        edit.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                feeButtonsShown = !feeButtonsShown;
                showFeeSelectionButtons(feeButtonsShown);
            }
        });
        keyboardIndex = signalLayout.indexOfChild(keyboardLayout);

        ImageButton faq = (ImageButton) rootView.findViewById(R.id.faq_button);

        faq.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!BRAnimator.isClickAllowed()) return;
                Activity app = getActivity();
                if (app == null) {
                    Log.e(TAG, "onClick: app is null, can't start the webview with url: " + URL_SUPPORT);
                    return;
                }
                BRAnimator.showSupportFragment(app, BRConstants.send);
            }
        });

        showKeyboard(false);
        setButton(true);

        signalLayout.setLayoutTransition(BRAnimator.getDefaultTransition());

        return rootView;
    }

    private void setListeners() {
        amountEdit.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showKeyboard(true);
                if (amountLabelOn) { //only first time
                    amountLabelOn = false;
                    amountEdit.setHint("0");
                    amountEdit.setTextSize(24);
                    balanceText.setVisibility(View.VISIBLE);
                    feeText.setVisibility(View.VISIBLE);
                    edit.setVisibility(View.VISIBLE);
                    isoText.setTextColor(getContext().getColor(R.color.almost_black));
                    isoText.setText(CurrencyUtils.getSymbolByIso(getActivity(), selectedIso));
                    isoText.setTextSize(28);
                    final float scaleX = amountEdit.getScaleX();
                    amountEdit.setScaleX(0);

                    AutoTransition tr = new AutoTransition();
                    tr.setInterpolator(new OvershootInterpolator());
                    tr.addListener(new android.support.transition.Transition.TransitionListener() {
                        @Override
                        public void onTransitionStart(@NonNull android.support.transition.Transition transition) {

                        }

                        @Override
                        public void onTransitionEnd(@NonNull android.support.transition.Transition transition) {
                            amountEdit.requestLayout();
                            amountEdit.animate().setDuration(100).scaleX(scaleX);
                        }

                        @Override
                        public void onTransitionCancel(@NonNull android.support.transition.Transition transition) {

                        }

                        @Override
                        public void onTransitionPause(@NonNull android.support.transition.Transition transition) {

                        }

                        @Override
                        public void onTransitionResume(@NonNull android.support.transition.Transition transition) {

                        }
                    });

                    ConstraintSet set = new ConstraintSet();
                    set.clone(amountLayout);
                    TransitionManager.beginDelayedTransition(amountLayout, tr);

                    int px4 = Utils.getPixelsFromDps(getContext(), 4);
//                    int px8 = Utils.getPixelsFromDps(getContext(), 8);
                    set.connect(balanceText.getId(), ConstraintSet.TOP, isoText.getId(), ConstraintSet.BOTTOM, px4);
                    set.connect(feeText.getId(), ConstraintSet.TOP, balanceText.getId(), ConstraintSet.BOTTOM, px4);
                    set.connect(feeText.getId(), ConstraintSet.BOTTOM, ConstraintSet.PARENT_ID, ConstraintSet.BOTTOM, px4);
                    set.connect(isoText.getId(), ConstraintSet.TOP, ConstraintSet.PARENT_ID, ConstraintSet.TOP, px4);
                    set.connect(isoText.getId(), ConstraintSet.BOTTOM, -1, ConstraintSet.TOP, -1);
                    set.applyTo(amountLayout);

                }

            }
        });

        //needed to fix the overlap bug
        commentEdit.setOnKeyListener(new View.OnKeyListener() {
            public boolean onKey(View v, int keyCode, KeyEvent event) {
                if ((event.getAction() == KeyEvent.ACTION_DOWN) && (keyCode == KeyEvent.KEYCODE_ENTER)) {
                    amountLayout.requestLayout();
                    return true;
                }
                return false;
            }
        });

//        commentEdit.addTextChangedListener(new BRTextWatcher());
//        addressEdit.addTextChangedListener(new BRTextWatcher());

        paste.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!BRAnimator.isClickAllowed()) return;
                String bitcoinUrl = BRClipboardManager.getClipboard(getActivity());
                if (Utils.isNullOrEmpty(bitcoinUrl) || !isInputValid(bitcoinUrl)) {
                    showClipboardError();
                    return;
                }

                RequestObject obj = parseRequest(bitcoinUrl);

                if (obj == null || obj.address == null) {
                    showClipboardError();
                    return;
                }
                final BRCoreAddress address = new BRCoreAddress(obj.address);
                final WalletsMaster wm = WalletsMaster.getInstance(getActivity());

                if (address.isValid()) {
                    final Activity app = getActivity();
                    if (app == null) {
                        Log.e(TAG, "paste onClick: app is null");
                        return;
                    }
                    BRExecutor.getInstance().forLightWeightBackgroundTasks().execute(new Runnable() {
                        @Override
                        public void run() {
                            BaseWalletManager wallet = wm.getCurrentWallet(app);
                            if (wallet.getWallet().containsAddress(address)) {
                                app.runOnUiThread(new Runnable() {
                                    @Override
                                    public void run() {
                                        BRDialog.showCustomDialog(getActivity(), "", getResources().getString(R.string.Send_containsAddress), getResources().getString(R.string.AccessibilityLabels_close), null, new BRDialogView.BROnClickListener() {
                                            @Override
                                            public void onClick(BRDialogView brDialogView) {
                                                brDialogView.dismiss();
                                            }
                                        }, null, null, 0);
                                        BRClipboardManager.putClipboard(getActivity(), "");
                                    }
                                });

                            } else if (wallet.getWallet().addressIsUsed(address)) {
                                app.runOnUiThread(new Runnable() {
                                    @Override
                                    public void run() {
                                        BRDialog.showCustomDialog(getActivity(), getString(R.string.Send_UsedAddress_firstLine), getString(R.string.Send_UsedAddress_secondLIne), "Ignore", "Cancel", new BRDialogView.BROnClickListener() {
                                            @Override
                                            public void onClick(BRDialogView brDialogView) {
                                                brDialogView.dismiss();
                                                addressEdit.setText(address.stringify());
                                            }
                                        }, new BRDialogView.BROnClickListener() {
                                            @Override
                                            public void onClick(BRDialogView brDialogView) {
                                                brDialogView.dismiss();
                                            }
                                        }, null, 0);
                                    }
                                });

                            } else {
                                app.runOnUiThread(new Runnable() {
                                    @Override
                                    public void run() {
                                        addressEdit.setText(address.stringify());

                                    }
                                });
                            }
                        }
                    });

                } else {
                    showClipboardError();
                }

            }
        });

        isoButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (selectedIso.equalsIgnoreCase(BRSharedPrefs.getPreferredFiatIso(getContext()))) {
                    Activity app = getActivity();
                    selectedIso = WalletsMaster.getInstance(app).getCurrentWallet(app).getIso(app);
                } else {
                    selectedIso = BRSharedPrefs.getPreferredFiatIso(getContext());
                }
                updateText();

            }
        });

        scan.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!BRAnimator.isClickAllowed()) return;
                saveMetaData();
                BRAnimator.openScanner(getActivity(), BRConstants.SCANNER_REQUEST);

            }
        });
        send.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //not allowed now
                if (!BRAnimator.isClickAllowed()) return;
                WalletsMaster master = WalletsMaster.getInstance(getActivity());
                BaseWalletManager wallet = master.getCurrentWallet(getActivity());
                //get the current wallet used
                if (wallet == null) {
                    Log.e(TAG, "onClick: Wallet is null and it can't happen.");
                    BRReportsManager.reportBug(new NullPointerException("Wallet is null and it can't happen."), true);
                    return;
                }
                boolean allFilled = true;
                String addressString = addressEdit.getText().toString();
                String amountStr = amountBuilder.toString();
                String comment = commentEdit.getText().toString();

                //inserted amount
                BigDecimal rawAmount = new BigDecimal(Utils.isNullOrEmpty(amountStr) ? "0" : amountStr);
                //is the chosen ISO a crypto (could be a fiat currency)
                boolean isIsoCrypto = master.isIsoCrypto(getActivity(), selectedIso);

                BigDecimal cryptoAmount = isIsoCrypto ? wallet.getSmallestCryptoForCrypto(getActivity(), rawAmount) : wallet.getSmallestCryptoForFiat(getActivity(), rawAmount);
                BRCoreAddress address = new BRCoreAddress(addressString);
                Activity app = getActivity();
                if (address.stringify().isEmpty() || !address.isValid()) {
                    allFilled = false;

                    BRDialog.showCustomDialog(app, app.getString(R.string.Alert_error), app.getString(R.string.Send_noAddress), app.getString(R.string.AccessibilityLabels_close), null, new BRDialogView.BROnClickListener() {
                        @Override
                        public void onClick(BRDialogView brDialogView) {
                            brDialogView.dismissWithAnimation();
                        }
                    }, null, null, 0);
                    return;
                }
                if (cryptoAmount.doubleValue() <= 0) {
                    allFilled = false;
                    SpringAnimator.failShakeAnimation(getActivity(), amountEdit);
                }
                if (cryptoAmount.longValue() > wallet.getCachedBalance(getActivity())) {
                    allFilled = false;
                    SpringAnimator.failShakeAnimation(getActivity(), balanceText);
                    SpringAnimator.failShakeAnimation(getActivity(), feeText);
                }

//                Log.e(TAG, "before createTransaction: smallestCryptoAmount.longValue: " + cryptoAmount.longValue() + ", addrs: " + address.stringify());
                BRCoreTransaction tx = wallet.getWallet().createTransaction(cryptoAmount.longValue(), address);
                if (tx == null) {
                    BRDialog.showCustomDialog(app, app.getString(R.string.Alert_error), app.getString(R.string.Send_creatTransactionError), app.getString(R.string.AccessibilityLabels_close), null, new BRDialogView.BROnClickListener() {
                        @Override
                        public void onClick(BRDialogView brDialogView) {
                            brDialogView.dismissWithAnimation();
                        }
                    }, null, null, 0);
                    return;
                }

                if (allFilled) {
                    PaymentItem item = new PaymentItem(tx, null, false, comment);
                    SendManager.sendTransaction(getActivity(), item, wallet);
                }
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


        addressEdit.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                if ((event != null && (event.getKeyCode() == KeyEvent.KEYCODE_ENTER)) || (actionId == EditorInfo.IME_ACTION_DONE) || (actionId == EditorInfo.IME_ACTION_NEXT)) {
                    Utils.hideKeyboard(getActivity());
                    new Handler().postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            showKeyboard(true);
                        }
                    }, 500);

                }
                return false;
            }
        });

        keyboard.addOnInsertListener(new BRKeyboard.OnInsertListener() {
            @Override
            public void onClick(String key) {
                handleClick(key);
            }
        });

        regular.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                setButton(true);
            }
        });
        economy.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                setButton(false);
            }
        });
//        updateText();

    }

    private void showKeyboard(boolean b) {
        int curIndex = keyboardIndex;

        if (!b) {
            signalLayout.removeView(keyboardLayout);

        } else {
            Utils.hideKeyboard(getActivity());
            if (signalLayout.indexOfChild(keyboardLayout) == -1)
                signalLayout.addView(keyboardLayout, curIndex);
            else
                signalLayout.removeView(keyboardLayout);

        }
    }

    private void showClipboardError() {
        BRDialog.showCustomDialog(getActivity(), getString(R.string.Send_emptyPasteboard), getResources().getString(R.string.Send_invalidAddressTitle), getString(R.string.AccessibilityLabels_close), null, new BRDialogView.BROnClickListener() {
            @Override
            public void onClick(BRDialogView brDialogView) {
                brDialogView.dismiss();
            }
        }, null, null, 0);
        BRClipboardManager.putClipboard(getActivity(), "");
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
                BRAnimator.animateSignalSlide(signalLayout, false, new BRAnimator.OnSlideAnimationEnd() {
                    @Override
                    public void onAnimationEnd() {
                        Bundle bundle = getArguments();
                        if (bundle != null && bundle.getString("url") != null)
                            setUrl(bundle.getString("url"));
                    }
                });
            }
        });

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
        loadMetaData();

    }

    @Override
    public void onPause() {
        super.onPause();
        Utils.hideKeyboard(getActivity());
        if (!ignoreCleanup) {
            savedIso = null;
            savedAmount = null;
            savedMemo = null;
        }
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
    }

    private void handleDigitClick(Integer dig) {
        String currAmount = amountBuilder.toString();
        String iso = selectedIso;
        WalletsMaster master = WalletsMaster.getInstance(getActivity());
        if (new BigDecimal(currAmount.concat(String.valueOf(dig))).doubleValue()
                <= master.getCurrentWallet(getActivity()).getMaxAmount(getActivity()).doubleValue()) {
            //do not insert 0 if the balance is 0 now
            if (currAmount.equalsIgnoreCase("0")) amountBuilder = new StringBuilder("");
            if ((currAmount.contains(".") && (currAmount.length() - currAmount.indexOf(".") > CurrencyUtils.getMaxDecimalPlaces(getActivity(), iso))))
                return;
            amountBuilder.append(dig);
            updateText();
        }
    }

    private void handleSeparatorClick() {
        String currAmount = amountBuilder.toString();
        if (currAmount.contains(".") || CurrencyUtils.getMaxDecimalPlaces(getActivity(), selectedIso) == 0)
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
        Activity app = getActivity();
        if (app == null) return;
        String tmpAmount = amountBuilder.toString();
        setAmount();
        WalletsMaster master = WalletsMaster.getInstance(app);
        BaseWalletManager wallet = master.getCurrentWallet(app);
        String balanceString;
        if (selectedIso == null)
            selectedIso = wallet.getIso(app);
        //String iso = selectedIso;
        curBalance = wallet.getCachedBalance(app);
        if (!amountLabelOn)
            isoText.setText(CurrencyUtils.getSymbolByIso(app, selectedIso));
        isoButton.setText(String.format("%s(%s)", selectedIso, CurrencyUtils.getSymbolByIso(app, selectedIso)));

        //is the chosen ISO a crypto (could be also a fiat currency)
        boolean isIsoCrypto = WalletsMaster.getInstance(getActivity()).isIsoCrypto(getActivity(), selectedIso);

        //Amount depending on ISO
        BigDecimal smallestAmount;

        if (Utils.isNullOrEmpty(tmpAmount) || tmpAmount.equalsIgnoreCase(".")) {
            smallestAmount = new BigDecimal(0);
        } else {
            if (isIsoCrypto) {
                smallestAmount = wallet.getSmallestCryptoForCrypto(app, new BigDecimal(tmpAmount));
            } else {
                smallestAmount = wallet.getSmallestCryptoForFiat(app, new BigDecimal(tmpAmount));
            }
        }

        //if it's null then make it 0
        if (smallestAmount == null) smallestAmount = new BigDecimal(0);

        //wallet's balance for the selected ISO
        BigDecimal balanceForISO = isIsoCrypto ? new BigDecimal(curBalance) : wallet.getFiatForSmallestCrypto(app, new BigDecimal(curBalance));
//        Log.e(TAG, "updateText: balanceForISO:" + balanceForISO);
        //formattedBalance
        String formattedBalance = CurrencyUtils.getFormattedAmount(app, selectedIso, balanceForISO);
//        Log.e(TAG, "updateText: formattedBalance:" + formattedBalance);

        //Balance depending on the selected ISO
        long fee;
        if (smallestAmount.longValue() == 0) {
            fee = 0;
        } else {
            BRCoreAddress coreAddress = new BRCoreAddress(addressEdit.getText().toString());
            if (Utils.isNullOrEmpty(coreAddress.stringify()) || !coreAddress.isValid() || wallet.getWallet().createTransaction(smallestAmount.longValue(), coreAddress) == null) {
                fee = wallet.getWallet().getFeeForTransactionAmount(smallestAmount.longValue());
            } else {
                BRCoreTransaction tx = wallet.getWallet().createTransaction(smallestAmount.longValue(), coreAddress);
                if (tx == null)
                    fee = wallet.getWallet().getFeeForTransactionAmount(smallestAmount.longValue());
                else {
                    fee = wallet.getWallet().getTransactionFee(tx);
                    if (fee == 0)
                        fee = wallet.getWallet().getFeeForTransactionAmount(smallestAmount.longValue());
                }
            }
        }

//        Log.e(TAG, "updateText: fee:" + fee);
        //get the fee for iso (in cents, satoshis..)
        BigDecimal feeForISO = isIsoCrypto ? wallet.getSmallestCryptoForCrypto(app, new BigDecimal(fee)) : wallet.getFiatForSmallestCrypto(app, new BigDecimal(fee));
//        Log.e(TAG, "updateText: feeForISO:" + feeForISO);

        if (balanceForISO == null) balanceForISO = new BigDecimal(0);

        //format the fee to the selected ISO
        String aproxFee = CurrencyUtils.getFormattedAmount(app, selectedIso, feeForISO);
//        Log.e(TAG, "updateText: aproxFee:" + aproxFee);

        boolean isOverTheBalance = smallestAmount.doubleValue() > balanceForISO.doubleValue();
        if (isOverTheBalance) {
            balanceText.setTextColor(getContext().getColor(R.color.warning_color));
            feeText.setTextColor(getContext().getColor(R.color.warning_color));
            amountEdit.setTextColor(getContext().getColor(R.color.warning_color));
            if (!amountLabelOn)
                isoText.setTextColor(getContext().getColor(R.color.warning_color));
        } else {
            balanceText.setTextColor(getContext().getColor(R.color.light_gray));
            feeText.setTextColor(getContext().getColor(R.color.light_gray));
            amountEdit.setTextColor(getContext().getColor(R.color.almost_black));
            if (!amountLabelOn)
                isoText.setTextColor(getContext().getColor(R.color.almost_black));
        }
        balanceString = String.format(getString(R.string.Send_balance), formattedBalance);
        balanceText.setText(String.format("%s", balanceString));
        feeText.setText(String.format(getString(R.string.Send_fee), aproxFee));
        amountLayout.requestLayout();
    }

    public void setUrl(String url) {
        RequestObject obj = CryptoUriParser.parseRequest(url);
        WalletsMaster master = WalletsMaster.getInstance(getActivity());
        if (obj == null) return;
        if (obj.address != null && addressEdit != null) {
            addressEdit.setText(obj.address.trim());
        }
        if (obj.message != null && commentEdit != null) {
            commentEdit.setText(obj.message);
        }
        if (obj.amount != null) {
            BigDecimal satoshiAmount = new BigDecimal(obj.amount).multiply(new BigDecimal(100000000));
            amountBuilder = new StringBuilder(master.getCurrentWallet(getActivity()).getFiatForSmallestCrypto(getActivity(), satoshiAmount).toPlainString());
            updateText();

        }
    }

    private void showFeeSelectionButtons(boolean b) {
        if (!b) {
            signalLayout.removeView(feeLayout);
        } else {
            signalLayout.addView(feeLayout, signalLayout.indexOfChild(amountLayout) + 1);

        }
    }

    private void setAmount() {
        String tmpAmount = amountBuilder.toString();
        int divider = tmpAmount.length();
        if (tmpAmount.contains(".")) {
            divider = tmpAmount.indexOf(".");
        }
        StringBuilder newAmount = new StringBuilder();
        for (int i = 0; i < tmpAmount.length(); i++) {
            newAmount.append(tmpAmount.charAt(i));
            if (divider > 3 && divider - 1 != i && divider > i && ((divider - i - 1) % 3 == 0)) {
                newAmount.append(",");
            }
        }
        amountEdit.setText(newAmount.toString());
    }

    private void setButton(boolean isRegular) {
        BaseWalletManager wallet = WalletsMaster.getInstance(getActivity()).getCurrentWallet(getActivity());
        String iso = wallet.getIso(getActivity());
        if (isRegular) {
            BRSharedPrefs.putFavorStandardFee(getActivity(), iso, true);
            regular.setTextColor(getContext().getColor(R.color.white));
            regular.setBackground(getContext().getDrawable(R.drawable.b_half_left_blue));
            economy.setTextColor(getContext().getColor(R.color.dark_blue));
            economy.setBackground(getContext().getDrawable(R.drawable.b_half_right_blue_stroke));
            feeDescription.setText(String.format(getString(R.string.FeeSelector_estimatedDeliver), getString(R.string.FeeSelector_regularTime)));
            warningText.getLayoutParams().height = 0;
        } else {
            BRSharedPrefs.putFavorStandardFee(getActivity(), iso, false);
            regular.setTextColor(getContext().getColor(R.color.dark_blue));
            regular.setBackground(getContext().getDrawable(R.drawable.b_half_left_blue_stroke));
            economy.setTextColor(getContext().getColor(R.color.white));
            economy.setBackground(getContext().getDrawable(R.drawable.b_half_right_blue));
            feeDescription.setText(String.format(getString(R.string.FeeSelector_estimatedDeliver), getString(R.string.FeeSelector_economyTime)));
            warningText.getLayoutParams().height = LinearLayout.LayoutParams.WRAP_CONTENT;
        }
        warningText.requestLayout();
        updateText();
    }

    private boolean isInputValid(String input) {
        return input.matches("[a-zA-Z0-9]*");
    }

    // from the link above
    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);

        // Checks whether a hardware keyboard is available
        if (newConfig.hardKeyboardHidden == Configuration.HARDKEYBOARDHIDDEN_NO) {
            Log.e(TAG, "onConfigurationChanged: hidden");
            showKeyboard(true);
        } else if (newConfig.hardKeyboardHidden == Configuration.HARDKEYBOARDHIDDEN_YES) {
            Log.e(TAG, "onConfigurationChanged: shown");
            showKeyboard(false);
        }
    }

    private void saveMetaData() {
        if (!commentEdit.getText().toString().isEmpty())
            savedMemo = commentEdit.getText().toString();
        if (!amountBuilder.toString().isEmpty())
            savedAmount = amountBuilder.toString();
        savedIso = selectedIso;
        ignoreCleanup = true;
    }

    private void loadMetaData() {
        ignoreCleanup = false;
        if (!Utils.isNullOrEmpty(savedMemo))
            commentEdit.setText(savedMemo);
        if (!Utils.isNullOrEmpty(savedIso))
            selectedIso = savedIso;
        if (!Utils.isNullOrEmpty(savedAmount)) {
            amountBuilder = new StringBuilder(savedAmount);
            new Handler().postDelayed(new Runnable() {
                @Override
                public void run() {
                    amountEdit.performClick();
                    updateText();
                }
            }, 500);

        }
    }

}