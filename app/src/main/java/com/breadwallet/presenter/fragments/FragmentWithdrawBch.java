package com.breadwallet.presenter.fragments;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Fragment;
import android.content.Context;
import android.content.DialogInterface;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.breadwallet.BreadWalletApp;
import com.breadwallet.R;
import com.breadwallet.presenter.activities.MainActivity;
import com.breadwallet.presenter.customviews.BubbleTextView;
import com.breadwallet.presenter.entities.RequestObject;
import com.breadwallet.tools.adapter.CustomPagerAdapter;
import com.breadwallet.tools.adapter.MiddleViewAdapter;
import com.breadwallet.tools.animation.BRAnimator;
import com.breadwallet.tools.animation.SpringAnimator;
import com.breadwallet.tools.manager.BRClipboardManager;
import com.breadwallet.tools.manager.BRTipsManager;
import com.breadwallet.tools.manager.SharedPreferencesManager;
import com.breadwallet.tools.security.BRErrorPipe;
import com.breadwallet.tools.security.KeyStoreManager;
import com.breadwallet.tools.security.PassCodeManager;
import com.breadwallet.tools.security.PostAuthenticationProcessor;
import com.breadwallet.tools.security.RequestHandler;
import com.breadwallet.tools.util.Utils;
import com.breadwallet.wallet.BRPeerManager;
import com.breadwallet.wallet.BRWalletManager;

import java.net.URI;

import static com.breadwallet.tools.util.BRConstants.AUTH_FOR_BCH;

/**
 * BreadWallet
 * <p>
 * Created by Mihail Gutan <mihail@breadwallet.com> on 7/14/15.
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

public class FragmentWithdrawBch extends Fragment {
    private static final String TAG = FragmentWithdrawBch.class.getName();
    public RelativeLayout mainFragmentLayout;
    public static String address;
    public static TextView txIdLabel;
    public static TextView txId;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        // Inflate the layout for this fragment
        final View rootView = inflater.inflate(R.layout.fragment_withdraw_bch, container, false);

        final Button scanQRButton = (Button) rootView.findViewById(R.id.main_button_scan_qr_code);
        mainFragmentLayout = (RelativeLayout) rootView.findViewById(R.id.main_fragment);
        final Button payAddressFromClipboardButton = (Button)
                rootView.findViewById(R.id.main_button_pay_address_from_clipboard);

        txId = (TextView) rootView.findViewById(R.id.tx_id);
        txIdLabel = (TextView) rootView.findViewById(R.id.tx_id_label);
        txId.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                BRClipboardManager.copyToClipboard(getActivity(), txId.getText().toString());
                ((BreadWalletApp) getActivity().getApplication()).showCustomToast(getActivity(), "Transaction Id copied", MainActivity.screenParametersPoint.y / 2, Toast.LENGTH_LONG, 0);
            }
        });

        mainFragmentLayout.setPadding(0, MainActivity.screenParametersPoint.y / 5, 0, 0);
        scanQRButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                FragmentDecoder.withdrawingBCH = true;
                if (BRAnimator.checkTheMultipressingAvailability()) {
                    BRAnimator.animateDecoderFragment();
                }
            }
        });

        payAddressFromClipboardButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                final AlertDialog[] alert = {null};
                final AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
                ((MainActivity) getActivity()).hideAllBubbles();
                if (BRAnimator.checkTheMultipressingAvailability()) {

                    final String bitcoinUrl = BRClipboardManager.readFromClipboard(getActivity());
                    String ifAddress = null;
                    RequestObject obj = RequestHandler.getRequestFromString(bitcoinUrl);
                    if (obj == null) {
                        //builder.setTitle(getResources().getString(R.string.alert));
                        builder.setMessage(getResources().getString(R.string.mainfragment_clipboard_invalid_data));
                        builder.setNeutralButton(getResources().getString(R.string.ok),
                                new DialogInterface.OnClickListener() {
                                    public void onClick(DialogInterface dialog, int which) {
                                        dialog.dismiss();
                                    }
                                });
                        alert[0] = builder.create();
                        alert[0].show();
                        BRClipboardManager.copyToClipboard(getActivity(), "");
                        return;
                    }
                    ifAddress = obj.address;
                    if (ifAddress == null) {
                        //builder.setTitle(getResources().getString(R.string.alert));
                        builder.setMessage(getResources().getString(R.string.mainfragment_clipboard_invalid_data));
                        builder.setNeutralButton(getResources().getString(R.string.ok),
                                new DialogInterface.OnClickListener() {
                                    public void onClick(DialogInterface dialog, int which) {
                                        dialog.dismiss();
                                    }
                                });
                        alert[0] = builder.create();
                        alert[0].show();
                        BRClipboardManager.copyToClipboard(getActivity(), "");
                        return;
                    }
//                    final String finalAddress = tempAddress;
                    BRWalletManager wm = BRWalletManager.getInstance(getActivity());

                    if (wm.isValidBitcoinPrivateKey(ifAddress) || wm.isValidBitcoinBIP38Key(ifAddress)) {
                        BRWalletManager.getInstance(getActivity()).confirmSweep(getActivity(), ifAddress);
                        return;
                    }

                    if (checkIfAddressIsValid(ifAddress)) {
                        final BRWalletManager m = BRWalletManager.getInstance(getActivity());
                        final String finalIfAddress = ifAddress;
                        new Thread(new Runnable() {
                            @Override
                            public void run() {
                                final boolean contained = m.addressContainedInWallet(finalIfAddress);
                                final boolean used = m.addressIsUsed(finalIfAddress);
                                getActivity().runOnUiThread(new Runnable() {
                                    @Override
                                    public void run() {
                                        if (contained) {

                                            //builder.setTitle(getResources().getString(R.string.alert));
                                            builder.setMessage(getResources().getString(R.string.address_already_in_your_wallet));
                                            builder.setNeutralButton(getResources().getString(R.string.ok),
                                                    new DialogInterface.OnClickListener() {
                                                        public void onClick(DialogInterface dialog, int which) {
                                                            dialog.dismiss();
                                                        }
                                                    });
                                            alert[0] = builder.create();
                                            alert[0].show();
                                            BRClipboardManager.copyToClipboard(getActivity(), "");


                                        } else if (used) {
                                            builder.setTitle(getResources().getString(R.string.warning));

                                            builder.setMessage(getResources().getString(R.string.address_already_used));
                                            builder.setPositiveButton(getResources().getString(R.string.ignore),
                                                    new DialogInterface.OnClickListener() {
                                                        public void onClick(DialogInterface dialog, int which) {
                                                            dialog.dismiss();
                                                            FragmentScanResult.address = finalIfAddress;
                                                            BRAnimator.animateScanResultFragment();
                                                        }
                                                    });
                                            builder.setNegativeButton(getResources().getString(R.string.cancel),
                                                    new DialogInterface.OnClickListener() {
                                                        public void onClick(DialogInterface dialog, int which) {
                                                            dialog.dismiss();
                                                        }
                                                    });
                                            alert[0] = builder.create();
                                            alert[0].show();
                                        } else {
                                            confirmSendingBCH(getActivity(), bitcoinUrl);
                                        }
                                    }
                                });
                            }
                        }).start();

                    } else {
                        //builder.setTitle(getResources().getString(R.string.alert));
                        builder.setMessage(getResources().getString(R.string.mainfragment_clipboard_invalid_data));
                        builder.setNeutralButton(getResources().getString(R.string.ok),
                                new DialogInterface.OnClickListener() {
                                    public void onClick(DialogInterface dialog, int which) {
                                        dialog.dismiss();
                                    }
                                });
                        alert[0] = builder.create();
                        alert[0].show();
                        BRClipboardManager.copyToClipboard(getActivity(), "");
                    }
                }
            }

        });

        return rootView;
    }


    @Override
    public void onResume() {
        super.onResume();
        updateUi();
    }

    public static void confirmSendingBCH(final Activity app, final String theAddress) {
        if (BRPeerManager.getCurrentBlockHeight() < 478559) {
            BRErrorPipe.showKeyStoreDialog(app, "Not synced", "Please wait for syncing to complete before using this feature.", "close", null,
                    new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int which) {
                            dialog.cancel();
                        }
                    }, null, null);
        } else {
            address = theAddress;
            if (BRWalletManager.getBCashBalance(KeyStoreManager.getMasterPublicKey(app)) == 0) {
                BRErrorPipe.showKeyStoreDialog(app, "No balance", "You have 0 BCH", "close", null,
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int which) {
                                dialog.cancel();
                            }
                        }, null, null);
            } else {
                ((BreadWalletApp) app.getApplication()).promptForAuthentication(app, AUTH_FOR_BCH, null, theAddress, "Sending out BCH", null, true);
            }
        }

    }

    public static void updateUi() {
        if (MainActivity.app != null) {
            MainActivity.app.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    String txIdString = SharedPreferencesManager.getBCHTxId(MainActivity.app);
                    if (Utils.isNullOrEmpty(txIdString)) {
                        txIdLabel.setVisibility(View.GONE);
                        txId.setVisibility(View.GONE);
                    } else {
                        txIdLabel.setVisibility(View.VISIBLE);
                        txId.setVisibility(View.VISIBLE);
                        txId.setText(txIdString);
                    }
                }
            });
        } else {
            Log.e(TAG, "updateUi: app is null");
        }
    }

    private boolean checkIfAddressIsValid(String str) {
        return BRWalletManager.validateAddress(str.trim());
    }

    //called from native
    public static void setBCHTxId(String txId) {
        if (MainActivity.app != null) {
            SharedPreferencesManager.putBCHTxId(MainActivity.app, txId);
            updateUi();
        } else {
            Log.e(TAG, "updateUi: app is null");
        }
    }

}