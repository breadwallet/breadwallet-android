package com.breadwallet.presenter.fragments;

import android.app.Activity;
import android.arch.lifecycle.ViewModelProviders;
import android.content.res.Configuration;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.constraint.ConstraintLayout;
import android.support.constraint.ConstraintSet;
import android.support.transition.AutoTransition;
import android.support.transition.TransitionManager;
import android.support.v4.app.FragmentActivity;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.OvershootInterpolator;
import android.view.inputmethod.EditorInfo;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.breadwallet.BuildConfig;
import com.breadwallet.R;
import com.breadwallet.presenter.customviews.BRButton;
import com.breadwallet.presenter.customviews.BRDialogView;
import com.breadwallet.presenter.customviews.BRKeyboard;
import com.breadwallet.presenter.customviews.BRLinearLayoutWithCaret;
import com.breadwallet.presenter.customviews.BaseTextView;
import com.breadwallet.presenter.entities.CryptoRequest;
import com.breadwallet.presenter.fragments.utils.ModalDialogFragment;
import com.breadwallet.presenter.viewmodels.SendViewModel;
import com.breadwallet.tools.animation.UiUtils;
import com.breadwallet.tools.animation.BRDialog;
import com.breadwallet.tools.animation.SlideDetector;
import com.breadwallet.tools.animation.SpringAnimator;
import com.breadwallet.tools.manager.BRClipboardManager;
import com.breadwallet.tools.manager.BRReportsManager;
import com.breadwallet.tools.manager.BRSharedPrefs;
import com.breadwallet.tools.manager.SendManager;
import com.breadwallet.tools.threads.executor.BRExecutor;
import com.breadwallet.tools.util.BRConstants;
import com.breadwallet.tools.util.CurrencyUtils;
import com.breadwallet.tools.util.Utils;
import com.breadwallet.wallet.WalletsMaster;
import com.breadwallet.wallet.util.CryptoUriParser;
import com.breadwallet.wallet.abstracts.BaseWalletManager;
import com.breadwallet.wallet.wallets.bitcoin.BaseBitcoinWalletManager;
import com.breadwallet.wallet.wallets.ethereum.WalletEthManager;

import java.math.BigDecimal;
import java.math.RoundingMode;

import static com.breadwallet.wallet.util.CryptoUriParser.parseRequest;
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

public class FragmentSend extends ModalDialogFragment implements BRKeyboard.OnInsertListener {
    private static final String TAG = FragmentSend.class.getName();

    private BRKeyboard mKeyboard;
    private EditText mAddressEdit;
    private Button mScan;
    private Button mPaste;
    private Button mSend;
    private EditText mCommentEdit;
    private TextView mCurrencyCode;
    private EditText mAmountEdit;
    private TextView mBalanceText;
    private TextView mFeeText;
    private ImageView mEditFeeIcon;
    private String mSelectedCurrencyCode;
    private Button mCurrencyCodeButton;
    private int mKeyboardIndex;
    private LinearLayout mKeyboardLayout;
    private ImageButton mCloseButton;
    private ConstraintLayout mAmountLayout;
    private BRButton mRegularFeeButton;
    private BRButton mEconomyFeeButton;
    private BRLinearLayoutWithCaret mFeeLayout;
    private boolean mIsFeeButtonsShown = false;
    private BaseTextView mFeeDescription;
    private BaseTextView mEconomyFeeWarningText;
    private boolean mIsAmountLabelShown = true;
    private static final int CURRENCY_CODE_TEXT_SIZE = 18;
    private SendViewModel mViewModel;
    private ViewGroup mBackgroundLayout;
    private ViewGroup mSignalLayout;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        ViewGroup rootView = assignRootView((ViewGroup) inflater.inflate(R.layout.fragment_send, container, false));
        mBackgroundLayout = assignBackgroundLayout((ViewGroup) rootView.findViewById(R.id.background_layout));
        mSignalLayout = assignSignalLayout((ViewGroup) rootView.findViewById(R.id.signal_layout));
        mKeyboard = rootView.findViewById(R.id.keyboard);
        mKeyboard.setBRButtonBackgroundResId(R.drawable.keyboard_white_button);
        mKeyboard.setBRKeyboardColor(R.color.white);
        mKeyboard.setDeleteImage(R.drawable.ic_delete_gray);
        mCurrencyCode = rootView.findViewById(R.id.iso_text);
        mAddressEdit = rootView.findViewById(R.id.address_edit);
        mScan = rootView.findViewById(R.id.scan);
        mPaste = rootView.findViewById(R.id.paste_button);
        mSend = rootView.findViewById(R.id.send_button);
        mCommentEdit = rootView.findViewById(R.id.comment_edit);
        mAmountEdit = rootView.findViewById(R.id.amount_edit);
        mBalanceText = rootView.findViewById(R.id.balance_text);
        mFeeText = rootView.findViewById(R.id.fee_text);
        mEditFeeIcon = rootView.findViewById(R.id.edit);
        mCurrencyCodeButton = rootView.findViewById(R.id.iso_button);
        mKeyboardLayout = rootView.findViewById(R.id.keyboard_layout);
        mAmountLayout = rootView.findViewById(R.id.amount_layout);
        mFeeLayout = rootView.findViewById(R.id.fee_buttons_layout);
        mFeeDescription = rootView.findViewById(R.id.fee_description);
        mEconomyFeeWarningText = rootView.findViewById(R.id.warning_text);

        mRegularFeeButton = rootView.findViewById(R.id.left_button);
        mEconomyFeeButton = rootView.findViewById(R.id.right_button);
        mCloseButton = rootView.findViewById(R.id.close_button);
        BaseWalletManager wm = WalletsMaster.getInstance(getActivity()).getCurrentWallet(getActivity());
        mSelectedCurrencyCode = BRSharedPrefs.isCryptoPreferred(getActivity()) ? wm.getIso() : BRSharedPrefs.getPreferredFiatIso(getContext());

        mViewModel = ViewModelProviders.of(this).get(SendViewModel.class);

        setListeners();
        mCurrencyCode.setText(getString(R.string.Send_amountLabel));
        mCurrencyCode.setTextSize(CURRENCY_CODE_TEXT_SIZE);
        mCurrencyCode.setTextColor(getContext().getColor(R.color.light_gray));
        mCurrencyCode.requestLayout();
        mSignalLayout.setOnTouchListener(new SlideDetector(getContext(), mSignalLayout));

        mSignalLayout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
            }
        });

        showFeeSelectionButtons(mIsFeeButtonsShown);

        mEditFeeIcon.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mIsFeeButtonsShown = !mIsFeeButtonsShown;
                showFeeSelectionButtons(mIsFeeButtonsShown);
            }
        });
        mKeyboardIndex = mSignalLayout.indexOfChild(mKeyboardLayout);

        ImageButton faq = rootView.findViewById(R.id.faq_button);

        faq.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!UiUtils.isClickAllowed()) return;
                Activity app = getActivity();
                if (app == null) {
                    Log.e(TAG, "onClick: app is null, can't start the webview with url: " + URL_SUPPORT);
                    return;
                }
                BaseWalletManager wm = WalletsMaster.getInstance(app).getCurrentWallet(app);
                UiUtils.showSupportFragment((FragmentActivity) app, BRConstants.FAQ_SEND, wm);
            }
        });

        showKeyboard(false);
        setButton(true);

        mSignalLayout.setLayoutTransition(UiUtils.getDefaultTransition());

        return rootView;
    }

    private void setListeners() {
        mAmountEdit.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                BaseWalletManager wm = WalletsMaster.getInstance(getActivity()).getCurrentWallet(getActivity());
                showKeyboard(true);
                if (mIsAmountLabelShown) { //only first time
                    mIsAmountLabelShown = false;
                    mAmountEdit.setHint("0");
                    mAmountEdit.setTextSize(24);
                    mBalanceText.setVisibility(View.VISIBLE);
                    mEditFeeIcon.setVisibility(View.VISIBLE);
                    mFeeText.setVisibility(View.VISIBLE);
                    mCurrencyCode.setTextColor(getContext().getColor(R.color.almost_black));
                    mCurrencyCode.setText(CurrencyUtils.getSymbolByIso(getActivity(), mSelectedCurrencyCode));
                    mCurrencyCode.setTextSize(28);
                    final float scaleX = mAmountEdit.getScaleX();
                    mAmountEdit.setScaleX(0);

                    AutoTransition tr = new AutoTransition();
                    tr.setInterpolator(new OvershootInterpolator());
                    tr.addListener(new android.support.transition.Transition.TransitionListener() {
                        @Override
                        public void onTransitionStart(@NonNull android.support.transition.Transition transition) {

                        }

                        @Override
                        public void onTransitionEnd(@NonNull android.support.transition.Transition transition) {
                            mAmountEdit.requestLayout();
                            mAmountEdit.animate().setDuration(100).scaleX(scaleX);
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
                    set.clone(mAmountLayout);
                    TransitionManager.beginDelayedTransition(mAmountLayout, tr);

                    int px4 = Utils.getPixelsFromDps(getContext(), 4);
                    set.connect(mBalanceText.getId(), ConstraintSet.TOP, mCurrencyCode.getId(), ConstraintSet.BOTTOM, px4);
                    set.connect(mFeeText.getId(), ConstraintSet.TOP, mBalanceText.getId(), ConstraintSet.BOTTOM, px4);
                    set.connect(mFeeText.getId(), ConstraintSet.BOTTOM, ConstraintSet.PARENT_ID, ConstraintSet.BOTTOM, px4);
                    set.connect(mCurrencyCode.getId(), ConstraintSet.TOP, ConstraintSet.PARENT_ID, ConstraintSet.TOP, px4);
                    set.connect(mCurrencyCode.getId(), ConstraintSet.BOTTOM, -1, ConstraintSet.TOP, -1);
                    set.applyTo(mAmountLayout);

                }

            }
        });

        //needed to fix the overlap bug
        mCommentEdit.setOnKeyListener(new View.OnKeyListener() {
            public boolean onKey(View v, int keyCode, KeyEvent event) {
                if ((event.getAction() == KeyEvent.ACTION_DOWN) && (keyCode == KeyEvent.KEYCODE_ENTER)) {
                    mAmountLayout.requestLayout();
                    return true;
                }
                return false;
            }
        });

        mCommentEdit.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View v, boolean hasFocus) {
                showKeyboard(!hasFocus);
            }
        });

        mPaste.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!UiUtils.isClickAllowed()) return;
                String theUrl = BRClipboardManager.getClipboard(getActivity());
                if (Utils.isNullOrEmpty(theUrl)) {
                    sayClipboardEmpty();
                    return;
                }
                showKeyboard(false);

                final BaseWalletManager wm = WalletsMaster.getInstance(getActivity()).getCurrentWallet(getActivity());


                if (Utils.isEmulatorOrDebug(getActivity()) && BuildConfig.BITCOIN_TESTNET) {
                    theUrl = wm.decorateAddress(theUrl);
                }

                final CryptoRequest obj = parseRequest(getActivity(), theUrl);

                if (obj == null || Utils.isNullOrEmpty(obj.address)) {
                    sayInvalidClipboardData();
                    return;
                }
                if (Utils.isEmulatorOrDebug(getActivity())) {
                    Log.d(TAG, "Send Address -> " + obj.address);
                    Log.d(TAG, "Send Value -> " + obj.value);
                    Log.d(TAG, "Send Amount -> " + obj.amount);
                }

                if (obj.iso != null && !obj.iso.equalsIgnoreCase(wm.getIso())) {
                    sayInvalidAddress(); //invalid if the screen is Bitcoin and scanning BitcoinCash for instance
                    return;
                }


                if (wm.isAddressValid(obj.address)) {
                    final Activity app = getActivity();
                    if (app == null) {
                        Log.e(TAG, "mPaste onClick: app is null");
                        return;
                    }
                    BRExecutor.getInstance().forLightWeightBackgroundTasks().execute(new Runnable() {
                        @Override
                        public void run() {
                            if (wm.containsAddress(obj.address)) {
                                app.runOnUiThread(new Runnable() {
                                    @Override
                                    public void run() {
                                        BRDialog.showCustomDialog(getActivity(), "", getResources().getString(R.string.Send_containsAddress),
                                                getResources().getString(R.string.AccessibilityLabels_close), null, new BRDialogView.BROnClickListener() {
                                                    @Override
                                                    public void onClick(BRDialogView brDialogView) {
                                                        brDialogView.dismiss();
                                                    }
                                                }, null, null, 0);
                                        BRClipboardManager.putClipboard(getActivity(), "");
                                    }
                                });

                            } else if (wm.addressIsUsed(obj.address)) {
                                app.runOnUiThread(new Runnable() {
                                    @Override
                                    public void run() {
                                        String title = String.format("%1$s addresses are intended for single use only.", wm.getName());
                                        BRDialog.showCustomDialog(getActivity(), title, getString(R.string.Send_UsedAddress_secondLIne),
                                                "Ignore", "Cancel", new BRDialogView.BROnClickListener() {
                                                    @Override
                                                    public void onClick(BRDialogView brDialogView) {
                                                        brDialogView.dismiss();
                                                        mAddressEdit.setText(wm.decorateAddress(obj.address));
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
                                        Log.e(TAG, "run: " + wm.getIso());
                                        mAddressEdit.setText(wm.decorateAddress(obj.address));

                                    }
                                });
                            }
                        }
                    });

                } else {
                    sayInvalidClipboardData();
                }

            }
        });

        mCurrencyCodeButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mSelectedCurrencyCode.equalsIgnoreCase(BRSharedPrefs.getPreferredFiatIso(getContext()))) {
                    Activity app = getActivity();
                    mSelectedCurrencyCode = WalletsMaster.getInstance(app).getCurrentWallet(app).getIso();
                } else {
                    mSelectedCurrencyCode = BRSharedPrefs.getPreferredFiatIso(getContext());
                }
                updateText();

            }
        });

        mScan.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!UiUtils.isClickAllowed()) return;
                saveViewModelData();
                UiUtils.openScanner(getActivity(), BRConstants.SCANNER_REQUEST);

            }
        });
        mSend.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //not allowed now
                if (!UiUtils.isClickAllowed()) return;
                WalletsMaster master = WalletsMaster.getInstance(getActivity());
                final BaseWalletManager wm = master.getCurrentWallet(getActivity());
                //get the current wallet used
                if (wm == null) {
                    Log.e(TAG, "onClick: Wallet is null and it can't happen.");
                    BRReportsManager.reportBug(new NullPointerException("Wallet is null and it can't happen."), true);
                    return;
                }
                boolean allFilled = true;
                String rawAddress = mAddressEdit.getText().toString();
                String amountStr = mViewModel.getAmount();
                String comment = mCommentEdit.getText().toString();

                //inserted amount
                BigDecimal rawAmount = new BigDecimal(Utils.isNullOrEmpty(amountStr) || amountStr.equalsIgnoreCase(".") ? "0" : amountStr);
                //is the chosen ISO a crypto (could be a fiat currency)
                boolean isIsoCrypto = master.isIsoCrypto(getActivity(), mSelectedCurrencyCode);

                BigDecimal cryptoAmount = isIsoCrypto ? wm.getSmallestCryptoForCrypto(getActivity(), rawAmount) : wm.getSmallestCryptoForFiat(getActivity(), rawAmount);

                CryptoRequest req = CryptoUriParser.parseRequest(getActivity(), rawAddress);
                if (req == null || Utils.isNullOrEmpty(req.address)) {
                    sayInvalidClipboardData();
                    return;
                }
                final Activity app = getActivity();
                if (!wm.isAddressValid(req.address)) {

                    BRDialog.showCustomDialog(app, app.getString(R.string.Alert_error), app.getString(R.string.Send_noAddress),
                            app.getString(R.string.AccessibilityLabels_close), null, new BRDialogView.BROnClickListener() {
                                @Override
                                public void onClick(BRDialogView brDialogView) {
                                    brDialogView.dismissWithAnimation();
                                }
                            }, null, null, 0);
                    return;
                }
                if (cryptoAmount.compareTo(BigDecimal.ZERO) <= 0) {
                    allFilled = false;
                    SpringAnimator.failShakeAnimation(getActivity(), mAmountEdit);
                }

                if (cryptoAmount.compareTo(wm.getCachedBalance(getActivity())) > 0) {
                    allFilled = false;
                    SpringAnimator.failShakeAnimation(getActivity(), mBalanceText);
                    SpringAnimator.failShakeAnimation(getActivity(), mFeeText);
                }

                if (WalletsMaster.getInstance(getActivity()).isIsoErc20(getActivity(), wm.getIso())) {

                    BigDecimal rawFee = wm.getEstimatedFee(cryptoAmount, mAddressEdit.getText().toString());
                    BaseWalletManager ethWm = WalletEthManager.getInstance(app);
                    BigDecimal isoFee = isIsoCrypto ? rawFee : ethWm.getFiatForSmallestCrypto(app, rawFee, null);
                    BigDecimal b = ethWm.getCachedBalance(app);
                    if (isoFee.compareTo(b) > 0) {
                        if (allFilled) {
                            BigDecimal ethVal = ethWm.getCryptoForSmallestCrypto(app, isoFee);
                            sayInsufficientEthereumForFee(app, ethVal.setScale(ethWm.getMaxDecimalPlaces(app), BRConstants.ROUNDING_MODE).toPlainString());

                            allFilled = false;
                        }
                    }

                }

                if (allFilled) {
                    final CryptoRequest item = new CryptoRequest(null, false, comment, req.address, cryptoAmount);
                    BRExecutor.getInstance().forLightWeightBackgroundTasks().execute(new Runnable() {
                        @Override
                        public void run() {
                            SendManager.sendTransaction(getActivity(), item, wm, null);

                        }
                    });

                    closeWithAnimation();
                }
            }
        });

        mBackgroundLayout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!UiUtils.isClickAllowed()) return;
                getActivity().onBackPressed();
            }
        });

        mCloseButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                closeWithAnimation();
            }
        });

        mAddressEdit.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                if ((event != null && (event.getKeyCode() == KeyEvent.KEYCODE_ENTER))
                        || (actionId == EditorInfo.IME_ACTION_DONE) || (actionId == EditorInfo.IME_ACTION_NEXT)) {
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

        mAddressEdit.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View v, boolean hasFocus) {
                showKeyboard(!hasFocus);
            }
        });

        mKeyboard.setOnInsertListener(this);

        mRegularFeeButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                setButton(true);
            }
        });
        mEconomyFeeButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                setButton(false);
            }
        });

    }

    private void showKeyboard(boolean b) {
        if (!b) {
            mSignalLayout.removeView(mKeyboardLayout);

        } else {
            Utils.hideKeyboard(getActivity());
            if (mSignalLayout.indexOfChild(mKeyboardLayout) == -1)
                mSignalLayout.addView(mKeyboardLayout, mKeyboardIndex);
            else
                mSignalLayout.removeView(mKeyboardLayout);

        }
    }

    private void sayClipboardEmpty() {
        BRDialog.showCustomDialog(getActivity(), "", getResources().getString(R.string.Send_emptyPasteboard),
                getString(R.string.AccessibilityLabels_close), null, new BRDialogView.BROnClickListener() {
                    @Override
                    public void onClick(BRDialogView brDialogView) {
                        brDialogView.dismiss();
                    }
                }, null, null, 0);
    }

    private void sayInvalidClipboardData() {
        BRDialog.showCustomDialog(getActivity(), "", getResources().getString(R.string.Send_invalidAddressTitle),
                getString(R.string.AccessibilityLabels_close), null, new BRDialogView.BROnClickListener() {
                    @Override
                    public void onClick(BRDialogView brDialogView) {
                        brDialogView.dismiss();
                    }
                }, null, null, 0);
    }

    private void saySomethingWentWrong() {
        BRDialog.showCustomDialog(getActivity(), "", "Something went wrong.",
                getString(R.string.AccessibilityLabels_close), null, new BRDialogView.BROnClickListener() {

                    @Override
                    public void onClick(BRDialogView brDialogView) {
                        brDialogView.dismiss();
                    }
                }, null, null, 0);
    }

    private void sayInvalidAddress() {
        BRDialog.showCustomDialog(getActivity(), "", getResources().getString(R.string.Send_invalidAddressMessage),
                getString(R.string.AccessibilityLabels_close), null, new BRDialogView.BROnClickListener() {
                    @Override
                    public void onClick(BRDialogView brDialogView) {
                        brDialogView.dismiss();
                    }
                }, null, null, 0);
    }

    private void sayInsufficientEthereumForFee(final Activity app, String ethNeeded) {
        String message = String.format("You must have at least %s Ethereum in your wallet in order to transfer this type of token. " +
                "Would you like to go to your Ethereum wallet now?", ethNeeded);
        BRDialog.showCustomDialog(app, "Insufficient Ethereum Balance", message, app.getString(R.string.Button_continueAction),
                app.getString(R.string.Button_cancel), new BRDialogView.BROnClickListener() {
                    @Override
                    public void onClick(BRDialogView brDialogView) {
                        brDialogView.dismissWithAnimation();
                        app.getFragmentManager().popBackStack();
                        new Handler().postDelayed(new Runnable() {
                            @Override
                            public void run() {
                                if (!app.isDestroyed())
                                    app.onBackPressed();
                            }
                        }, 1000);
                    }
                }, new BRDialogView.BROnClickListener() {
                    @Override
                    public void onClick(BRDialogView brDialogView) {
                        brDialogView.dismissWithAnimation();
                    }
                }, null, 0);
    }


    @Override
    public void onResume() {
        super.onResume();
        loadViewModelData();
    }

    @Override
    public void onPause() {
        super.onPause();
        Utils.hideKeyboard(getActivity());
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

    private void handleDigitClick(Integer digit) {
        String currAmount = mViewModel.getAmount();
        BaseWalletManager wm = WalletsMaster.getInstance(getActivity()).getCurrentWallet(getActivity());
        if (new BigDecimal(currAmount.concat(String.valueOf(digit))).compareTo(wm.getMaxAmount(getActivity())) <= 0) {
            //do not insert 0 if the balance is 0 now
            if (currAmount.equalsIgnoreCase("0")) {
                mViewModel.setAmount("");
            }
            boolean isDigitLimitReached = (currAmount.length() - currAmount.indexOf(".") > CurrencyUtils.getMaxDecimalPlaces(getActivity(), mSelectedCurrencyCode));
            if ((currAmount.contains(".") && isDigitLimitReached)) {
                return;
            }
            mViewModel.setAmount(mViewModel.getAmount() + digit);
            updateText();
        }
    }

    private void handleSeparatorClick() {
        String currAmount = mViewModel.getAmount();
        if (currAmount.contains(".") || CurrencyUtils.getMaxDecimalPlaces(getActivity(), mSelectedCurrencyCode) == 0)
            return;
        mViewModel.setAmount(mViewModel.getAmount() + ".");
        updateText();
    }

    private void handleDeleteClick() {
        String currAmount = mViewModel.getAmount();
        if (currAmount.length() > 0) {
            currAmount = currAmount.substring(0, currAmount.length() - 1);
            mViewModel.setAmount(currAmount);
            updateText();
        }

    }

    private void updateText() {
        Activity app = getActivity();
        if (app == null) return;

        String stringAmount = mViewModel.getAmount();
        setAmount();
        BaseWalletManager wm = WalletsMaster.getInstance(app).getCurrentWallet(app);
        String balanceString;
        if (mSelectedCurrencyCode == null)
            mSelectedCurrencyCode = wm.getIso();
        BigDecimal mCurrentBalance = wm.getCachedBalance(app);
        if (!mIsAmountLabelShown)
            mCurrencyCode.setText(CurrencyUtils.getSymbolByIso(app, mSelectedCurrencyCode));
        mCurrencyCodeButton.setText(mSelectedCurrencyCode);

        //is the chosen ISO a crypto (could be also a fiat currency)
        boolean isIsoCrypto = WalletsMaster.getInstance(app).isIsoCrypto(app, mSelectedCurrencyCode);
        boolean isWalletErc20 = WalletsMaster.getInstance(app).isIsoErc20(app, wm.getIso());
        BigDecimal inputAmount = new BigDecimal(Utils.isNullOrEmpty(stringAmount) || stringAmount.equalsIgnoreCase(".") ? "0" : stringAmount);

        //smallest crypto e.g. satoshis
        BigDecimal cryptoAmount = isIsoCrypto ? wm.getSmallestCryptoForCrypto(app, inputAmount) : wm.getSmallestCryptoForFiat(app, inputAmount);

        //wallet's balance for the selected ISO
        BigDecimal isoBalance = isIsoCrypto ? wm.getCryptoForSmallestCrypto(app, mCurrentBalance) : wm.getFiatForSmallestCrypto(app, mCurrentBalance, null);
        if (isoBalance == null) isoBalance = BigDecimal.ZERO;

        BigDecimal rawFee = wm.getEstimatedFee(cryptoAmount, mAddressEdit.getText().toString());

        //get the fee for iso (dollars, bits, BTC..)
        BigDecimal isoFee = isIsoCrypto ? rawFee : wm.getFiatForSmallestCrypto(app, rawFee, null);

        //format the fee to the selected ISO
        String formattedFee = CurrencyUtils.getFormattedAmount(app, mSelectedCurrencyCode, isoFee);

        if (isWalletErc20) {
            BaseWalletManager ethWm = WalletEthManager.getInstance(app);
            isoFee = isIsoCrypto ? rawFee : ethWm.getFiatForSmallestCrypto(app, rawFee, null);
            formattedFee = CurrencyUtils.getFormattedAmount(app, isIsoCrypto ? ethWm.getIso() : mSelectedCurrencyCode, isoFee);
        }

        boolean isOverTheBalance = inputAmount.compareTo(isoBalance) > 0;

        if (isOverTheBalance) {
            mBalanceText.setTextColor(getContext().getColor(R.color.warning_color));
            mFeeText.setTextColor(getContext().getColor(R.color.warning_color));
            mAmountEdit.setTextColor(getContext().getColor(R.color.warning_color));
            if (!mIsAmountLabelShown)
                mCurrencyCode.setTextColor(getContext().getColor(R.color.warning_color));
        } else {
            mBalanceText.setTextColor(getContext().getColor(R.color.light_gray));
            mFeeText.setTextColor(getContext().getColor(R.color.light_gray));
            mAmountEdit.setTextColor(getContext().getColor(R.color.almost_black));
            if (!mIsAmountLabelShown)
                mCurrencyCode.setTextColor(getContext().getColor(R.color.almost_black));
        }
        //formattedBalance
        String formattedBalance = CurrencyUtils.getFormattedAmount(app, mSelectedCurrencyCode,
                isIsoCrypto ? wm.getSmallestCryptoForCrypto(app, isoBalance) : isoBalance);
        balanceString = String.format(getString(R.string.Send_balance), formattedBalance);
        mBalanceText.setText(balanceString);
        mFeeText.setText(String.format(getString(R.string.Send_fee), formattedFee));
        mAmountLayout.requestLayout();
    }


    private void showFeeSelectionButtons(boolean b) {
        if (!b) {
            mSignalLayout.removeView(mFeeLayout);
        } else {
            mSignalLayout.addView(mFeeLayout, mSignalLayout.indexOfChild(mAmountLayout) + 1);

        }
    }

    private void setAmount() {
        String tmpAmount = mViewModel.getAmount();
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
        mAmountEdit.setText(newAmount.toString());
    }

    private void setButton(boolean isRegular) {
        BaseWalletManager wallet = WalletsMaster.getInstance(getActivity()).getCurrentWallet(getActivity());
        String iso = wallet.getIso();
        if (isRegular) {
            BRSharedPrefs.putFavorStandardFee(getActivity(), iso, true);
            mRegularFeeButton.setTextColor(getContext().getColor(R.color.white));
            mRegularFeeButton.setBackground(getContext().getDrawable(R.drawable.b_half_left_blue));
            mEconomyFeeButton.setTextColor(getContext().getColor(R.color.dark_blue));
            mEconomyFeeButton.setBackground(getContext().getDrawable(R.drawable.b_half_right_blue_stroke));
            mFeeDescription.setText(String.format(getString(R.string.FeeSelector_estimatedDeliver), getString(R.string.FeeSelector_regularTime)));
            mEconomyFeeWarningText.getLayoutParams().height = 0;
        } else {
            BRSharedPrefs.putFavorStandardFee(getActivity(), iso, false);
            mRegularFeeButton.setTextColor(getContext().getColor(R.color.dark_blue));
            mRegularFeeButton.setBackground(getContext().getDrawable(R.drawable.b_half_left_blue_stroke));
            mEconomyFeeButton.setTextColor(getContext().getColor(R.color.white));
            mEconomyFeeButton.setBackground(getContext().getDrawable(R.drawable.b_half_right_blue));
            mFeeDescription.setText(String.format(getString(R.string.FeeSelector_estimatedDeliver), getString(R.string.FeeSelector_economyTime)));
            mEconomyFeeWarningText.getLayoutParams().height = LinearLayout.LayoutParams.WRAP_CONTENT;
        }
        mEconomyFeeWarningText.requestLayout();
        updateText();
    }

    // from the link above
    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);

        // Checks whether a hardware mKeyboard is available
        if (newConfig.hardKeyboardHidden == Configuration.HARDKEYBOARDHIDDEN_NO) {
            Log.e(TAG, "onConfigurationChanged: hidden");
            showKeyboard(true);
        } else if (newConfig.hardKeyboardHidden == Configuration.HARDKEYBOARDHIDDEN_YES) {
            Log.e(TAG, "onConfigurationChanged: shown");
            showKeyboard(false);
        }
    }

    public void loadViewModelData() {
        if (mAddressEdit != null) {
            if (!Utils.isNullOrEmpty(mViewModel.getAddress())) {
                BaseWalletManager walletManager = WalletsMaster.getInstance(getActivity()).getCurrentWallet(getActivity());
                mAddressEdit.setText(walletManager.decorateAddress(mViewModel.getAddress()));
            }
            if (!Utils.isNullOrEmpty(mViewModel.getMemo())) {
                mCommentEdit.setText(mViewModel.getMemo());
            }
            if (!Utils.isNullOrEmpty(mViewModel.getChosenCode())) {
                mSelectedCurrencyCode = mViewModel.getChosenCode().toUpperCase();
            }
        }
    }

    public void saveViewModelData(CryptoRequest request) {
        String address = null;
        String code = null;
        String amount = null;
        String memo = null;
        if (request == null) {
            if (mCommentEdit != null) {
                memo = mCommentEdit.getText().toString();
                address = mAddressEdit.getText().toString();
                code = mSelectedCurrencyCode;
            }
        } else {
            BaseWalletManager wm = WalletsMaster.getInstance(getActivity()).getCurrentWallet(getActivity());
            address = request.address;
            memo = request.message;
            code = request.iso;

            if (request.amount != null) {
                BigDecimal satoshiAmount = request.amount.multiply(new BigDecimal(BaseBitcoinWalletManager.ONE_BITCOIN_IN_SATOSHIS));
                amount = wm.getFiatForSmallestCrypto(getActivity(), satoshiAmount, null).toPlainString();
            } else if (request.value != null) {
                // ETH request amount param is named `value`
                BigDecimal fiatAmount = wm.getFiatForSmallestCrypto(getActivity(), request.value, null);
                fiatAmount = fiatAmount.setScale(2, RoundingMode.HALF_EVEN);
                amount = fiatAmount.toPlainString();
            }
        }
        if (!Utils.isNullOrEmpty(address)) {
            mViewModel.setAddress(address);
        }
        if (!Utils.isNullOrEmpty(memo)) {
            mViewModel.setMemo(memo);
        }
        if (!Utils.isNullOrEmpty(code)) {
            mViewModel.setChosenCode(code);
        }
        if (!Utils.isNullOrEmpty(amount)) {
            mViewModel.setAmount(amount);
        }
        loadViewModelData();
    }

    public void saveViewModelData() {
        saveViewModelData(null);
    }

    @Override
    public void onKeyInsert(String key) {
        handleClick(key);
    }
}
