package com.breadwallet.presenter.fragments;

import android.app.Activity;
import android.app.Fragment;
import android.content.Intent;
import android.content.res.Configuration;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.Nullable;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.view.inputmethod.EditorInfo;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import com.breadwallet.R;
import com.breadwallet.presenter.activities.settings.WebViewActivity;
import com.breadwallet.presenter.customviews.BRDialogView;
import com.breadwallet.presenter.customviews.BRKeyboard;
import com.breadwallet.presenter.entities.PaymentItem;
import com.breadwallet.presenter.entities.RequestObject;
import com.breadwallet.tools.adapter.CurAdapter;
import com.breadwallet.tools.animation.BRAnimator;
import com.breadwallet.tools.animation.BRDialog;
import com.breadwallet.tools.animation.SlideDetector;
import com.breadwallet.tools.animation.SpringAnimator;
import com.breadwallet.tools.listeners.RecyclerItemClickListener;
import com.breadwallet.tools.manager.BRClipboardManager;
import com.breadwallet.tools.manager.BRSharedPrefs;
import com.breadwallet.tools.security.BitcoinUrlHandler;
import com.breadwallet.tools.security.BRSender;
import com.breadwallet.tools.sqlite.CurrencyDataSource;
import com.breadwallet.tools.util.BRConstants;
import com.breadwallet.tools.util.BRExchange;
import com.breadwallet.tools.util.BRCurrency;
import com.breadwallet.tools.util.Utils;
import com.breadwallet.wallet.BRWalletManager;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import static com.breadwallet.tools.security.BitcoinUrlHandler.getRequestFromString;
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
    //    private RecyclerView currencyRecycler;
    private EditText commentEdit;
    private StringBuilder amountBuilder;
    private TextView isoText;
    private EditText amountEdit;
    private TextView balanceText;
    private long curBalance;
    private String selectedIso;
    //    private CurAdapter curAdapter;
    private Button isoButton;
    private int keyboardIndex;
    //    private int currListIndex;
    private LinearLayout keyboardLayout;
    //    private LinearLayout currencyListLayout;
    private ImageButton close;

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
//        currencyRecycler = (RecyclerView) rootView.findViewById(R.id.cur_spinner);
        amountEdit = (EditText) rootView.findViewById(R.id.amount_edit);
        balanceText = (TextView) rootView.findViewById(R.id.balance_text);
        isoButton = (Button) rootView.findViewById(R.id.iso_button);
        keyboardLayout = (LinearLayout) rootView.findViewById(R.id.keyboard_layout);
//        currencyListLayout = (LinearLayout) rootView.findViewById(R.id.cur_spinner_layout);
        close = (ImageButton) rootView.findViewById(R.id.close_button);
        selectedIso = BRSharedPrefs.getPreferredBTC(getContext()) ? "BTC" : BRSharedPrefs.getIso(getContext());

        amountBuilder = new StringBuilder(0);
        setListeners();
        isoText.setText("Amount");
        isoText.setTextSize(18);
        isoText.setTextColor(getContext().getColor(R.color.light_gray));
        signalLayout.setOnTouchListener(new SlideDetector(getContext(), signalLayout));

        signalLayout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
//                removeCurrencySelector();
            }
        });
//        currListIndex = signalLayout.indexOfChild(currencyListLayout);
        keyboardIndex = signalLayout.indexOfChild(keyboardLayout);

        ImageButton faq = (ImageButton) rootView.findViewById(R.id.faq_button);

        faq.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Activity app = getActivity();
                if (app == null) {
                    Log.e(TAG, "onClick: app is null, can't start the webview with url: " + URL_SUPPORT);
                    return;
                }
                Intent intent = new Intent(app, WebViewActivity.class);
                intent.putExtra("url", URL_SUPPORT);
                intent.putExtra("articleId", BRConstants.send);
                app.startActivity(intent);
                app.overridePendingTransition(R.anim.enter_from_bottom, R.anim.empty_300);
            }
        });

        showKeyboard(false);
//        showCurrencyList(false);

        signalLayout.setLayoutTransition(BRAnimator.getDefaultTransition());

        return rootView;
    }

    private void setListeners() {
        amountEdit.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showKeyboard(true);
                if (amountEdit.getHint() == null || !amountEdit.getHint().toString().equalsIgnoreCase("0")) { //only first time
                    amountEdit.setHint("0");
                    amountEdit.setTextSize(24);
                    balanceText.setVisibility(View.VISIBLE);
                    isoText.setTextColor(getContext().getColor(R.color.almost_black));
                    isoText.setText(BRCurrency.getSymbolByIso(getActivity(), selectedIso));
                    isoText.setTextSize(28);
                }

            }
        });

        paste.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
//                removeCurrencySelector();
                if (!BRAnimator.isClickAllowed()) return;
                String bitcoinUrl = BRClipboardManager.getClipboard(getActivity());
                if (Utils.isNullOrEmpty(bitcoinUrl) || !isInputValid(bitcoinUrl)) {
                    showClipboardError();
                    return;
                }
                String address = null;

                RequestObject obj = getRequestFromString(bitcoinUrl);

                if (obj == null || obj.address == null) {
                    showClipboardError();
                    return;
                }
                address = obj.address;
                final BRWalletManager wm = BRWalletManager.getInstance();

//                if (wm.isValidBitcoinPrivateKey(address) || wm.isValidBitcoinBIP38Key(address)) {
////                        wm.confirmSweep(getActivity(), address);
////                        addressEdit.setText("");
//                    return;
//                }

                if (BRWalletManager.validateAddress(address)) {
                    final String finalAddress = address;
                    final Activity app = getActivity();
                    if (app == null) {
                        Log.e(TAG, "paste onClick: app is null");
                        return;
                    }
                    new Thread(new Runnable() {
                        @Override
                        public void run() {
                            if (wm.addressContainedInWallet(finalAddress)) {
                                app.runOnUiThread(new Runnable() {
                                    @Override
                                    public void run() {
                                        BRDialog.showCustomDialog(getActivity(), "Address contained", getResources().getString(R.string.address_already_in_your_wallet), "close", null, new BRDialogView.BROnClickListener() {
                                            @Override
                                            public void onClick(BRDialogView brDialogView) {
                                                brDialogView.dismiss();
                                            }
                                        }, null, null, 0);
                                        BRClipboardManager.putClipboard(getActivity(), "");
                                    }
                                });

                            } else if (wm.addressIsUsed(finalAddress)) {
                                app.runOnUiThread(new Runnable() {
                                    @Override
                                    public void run() {
                                        BRDialog.showCustomDialog(getActivity(), "Address used", getResources().getString(R.string.address_already_used), "Ignore", "Cancel", new BRDialogView.BROnClickListener() {
                                            @Override
                                            public void onClick(BRDialogView brDialogView) {
                                                brDialogView.dismiss();
                                                addressEdit.setText(finalAddress);
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
                                        addressEdit.setText(finalAddress);

                                    }
                                });
                            }
                        }
                    }).start();

                } else {
                    showClipboardError();
                }

            }
        });

        isoButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
//                showCurrencyList(true);
                if (selectedIso.equalsIgnoreCase(BRSharedPrefs.getIso(getContext()))) {
                    selectedIso = "BTC";
                } else {
                    selectedIso = BRSharedPrefs.getIso(getContext());
                }
                updateText();

            }
        });

        scan.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
//                removeCurrencySelector();
                if (!BRAnimator.isClickAllowed()) return;
                BRAnimator.openScanner(getActivity(), BRConstants.SCANNER_REQUEST);

            }
        });
        send.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
//                removeCurrencySelector();
                //not allowed now
                if (!BRAnimator.isClickAllowed()) {
                    return;
                }

                boolean allFilled = true;
                String address = addressEdit.getText().toString();
                String amountStr = amountBuilder.toString();
                String iso = selectedIso;
                String comment = commentEdit.getText().toString();

                //get amount in satoshis from any isos
                BigDecimal bigAmount = new BigDecimal(Utils.isNullOrEmpty(amountStr) ? "0" : amountStr);
                BigDecimal satoshiAmount = BRExchange.getSatoshisFromAmount(getActivity(), iso, bigAmount);
//                long amount = BRExchange.getAmountFromSatoshis(getActivity(), iso, satoshiAmount).longValue();

                if (address.isEmpty() || !BRWalletManager.validateAddress(address)) {
                    allFilled = false;
                    SpringAnimator.failShakeAnimation(getActivity(), addressEdit);
                }
                if (amountStr.isEmpty()) {
                    allFilled = false;
                    SpringAnimator.failShakeAnimation(getActivity(), amountEdit);
                }
                if (satoshiAmount.longValue() > BRWalletManager.getInstance().getBalance(getActivity())) {
                    SpringAnimator.failShakeAnimation(getActivity(), balanceText);
                }

                if (allFilled)
                    BRSender.getInstance().sendTransaction(getContext(), new PaymentItem(new String[]{address}, null, satoshiAmount.longValue(), null, false, comment));
            }
        });

        backgroundLayout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
//                removeCurrencySelector();
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

//        currencyRecycler.addOnItemTouchListener(new RecyclerItemClickListener(getContext(),
//                currencyRecycler, new RecyclerItemClickListener.OnItemClickListener() {
//            @Override
//            public void onItemClick(View view, int position, float x, float y) {
//                Log.e(TAG, "onItemClick: " + position);
////                BRAnimator.showTransactionPager(BreadActivity.this, adapter.getItems(), position);
//                selectedIso = curAdapter.getItemAtPos(position);
//
//                Log.e(TAG, "onItemSelected: " + selectedIso);
////                isoText.setText(BRCurrency.getSymbolByIso(getActivity(), selectedIso));
//                updateText();
//                removeCurrencySelector();
//
//            }
//
//            @Override
//            public void onLongItemClick(View view, int position) {
//
//            }
//        }));

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
//                removeCurrencySelector();
                handleClick(key);
            }
        });

//        final List<String> curList = new ArrayList<>();
//        curList.add("BTC");
//        if (getActivity() == null) return;
//        curList.addAll(CurrencyDataSource.getInstance(getActivity()).getAllISOs());
//        curAdapter = new CurAdapter(getContext(), curList);
//        currencyRecycler.setLayoutManager(new LinearLayoutManager(getContext(), LinearLayoutManager.HORIZONTAL, false));
//        currencyRecycler.setAdapter(curAdapter);
//        selectedIso = curAdapter.getItemAtPos(0);
        updateText();

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

//    private void showCurrencyList(boolean b) {
//        Log.e(TAG, "showCurrencyList: " + currListIndex);
//
//        if (!b) {
//            signalLayout.removeView(currencyListLayout);
//        } else {
//            if (signalLayout.indexOfChild(currencyListLayout) == -1)
//                signalLayout.addView(currencyListLayout, currListIndex);
//            else
//                signalLayout.removeView(currencyListLayout);
//
//        }
//    }

    private void showClipboardError() {
        BRDialog.showCustomDialog(getActivity(), "Clipboard empty", getResources().getString(R.string.Send_invalidAddressTitle), "close", null, new BRDialogView.BROnClickListener() {
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
        setAmount();
        String balanceString;
        String iso = selectedIso;
        curBalance = BRWalletManager.getInstance().getBalance(getActivity());
        isoText.setText(BRCurrency.getSymbolByIso(getActivity(), selectedIso));
        isoButton.setText(String.format("%s(%s)", selectedIso, BRCurrency.getSymbolByIso(getActivity(), selectedIso)));
        //Balance depending on ISO
        BigDecimal balanceForISO = BRExchange.getAmountFromSatoshis(getActivity(), iso, new BigDecimal(curBalance));
        //formattedBalance
        String formattedBalance = BRCurrency.getFormattedCurrencyString(getActivity(), iso, balanceForISO);
        //Balance depending on ISO
        long fee = curBalance == 0 ? 0 : BRWalletManager.getInstance().feeForTransactionAmount(curBalance);
        BigDecimal feeForISO = BRExchange.getAmountFromSatoshis(getActivity(), iso, new BigDecimal(curBalance == 0 ? 0 : fee));
        //formattedBalance
        String aproxFee = BRCurrency.getFormattedCurrencyString(getActivity(), iso, feeForISO);
        if (new BigDecimal((tmpAmount.isEmpty() || tmpAmount.equalsIgnoreCase(".")) ? "0" : tmpAmount).doubleValue() > balanceForISO.doubleValue()) {
//            balanceString = String.format("Insufficient funds. Try an amount below your current balance: %s", formattedBalance);
            balanceText.setTextColor(getContext().getColor(R.color.warning_color));
            amountEdit.setTextColor(getContext().getColor(R.color.warning_color));
            isoText.setTextColor(getContext().getColor(R.color.warning_color));
        } else {
//            balanceString = String.format("Current Balance: %s", formattedBalance);
            balanceText.setTextColor(getContext().getColor(R.color.light_gray));
            amountEdit.setTextColor(getContext().getColor(R.color.almost_black));
            isoText.setTextColor(getContext().getColor(R.color.almost_black));
        }
        balanceString = String.format("Current Balance: %s", formattedBalance);
        balanceText.setText(String.format("%s, Fee: %s", balanceString, aproxFee));

    }

    public void setUrl(String url) {
        RequestObject obj = BitcoinUrlHandler.getRequestFromString(url);
        if (obj == null) return;
        if (obj.address != null && addressEdit != null) {
            addressEdit.setText(obj.address.trim());
        }
        if (obj.message != null && commentEdit != null) {
            commentEdit.setText(obj.message);
        }
        if (obj.amount != null) {
            String iso = selectedIso;
            BigDecimal satoshiAmount = new BigDecimal(obj.amount).multiply(new BigDecimal(100000000));
            amountBuilder = new StringBuilder(BRExchange.getAmountFromSatoshis(getActivity(), iso, satoshiAmount).toPlainString());

            updateText();

        }
    }

//    private void removeCurrencySelector() {
//        try {
//            signalLayout.removeView(currencyListLayout);
//        } catch (IllegalStateException ignored) {
//
//        }
//    }

    private void setAmount() {
        String tmpAmount = amountBuilder.toString();
        int divider = tmpAmount.length();
        if (tmpAmount.contains(".")) {
            divider = tmpAmount.indexOf(".");
        }
        Log.e(TAG, "setAmount: " + divider);
        StringBuilder newAmount = new StringBuilder();
        for (int i = 0; i < tmpAmount.length(); i++) {
            newAmount.append(tmpAmount.charAt(i));
            if (divider > 3 && divider - 1 != i && divider > i && ((divider - i - 1) % 3 == 0)) {
                newAmount.append(",");
            }
        }
        amountEdit.setText(newAmount.toString());
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
            showKeyboard(true);
        } else if (newConfig.hardKeyboardHidden == Configuration.HARDKEYBOARDHIDDEN_YES) {
            showKeyboard(false);
        }
    }

}