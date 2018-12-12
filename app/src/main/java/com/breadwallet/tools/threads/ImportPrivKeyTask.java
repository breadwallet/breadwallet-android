package com.breadwallet.tools.threads;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.AsyncTask;
import android.os.Handler;
import android.os.SystemClock;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;

import com.breadwallet.BreadApp;
import com.breadwallet.R;
import com.breadwallet.core.BRCoreAddress;
import com.breadwallet.core.BRCoreKey;
import com.breadwallet.core.BRCorePeerManager;
import com.breadwallet.core.BRCoreTransaction;
import com.breadwallet.core.BRCoreTransactionInput;
import com.breadwallet.core.BRCoreTransactionOutput;
import com.breadwallet.presenter.customviews.BRDialogView;
import com.breadwallet.presenter.customviews.BRToast;
import com.breadwallet.tools.animation.BRDialog;
import com.breadwallet.tools.animation.SpringAnimator;
import com.breadwallet.tools.manager.BRApiManager;
import com.breadwallet.tools.manager.BRReportsManager;
import com.breadwallet.tools.manager.BRSharedPrefs;
import com.breadwallet.tools.threads.executor.BRExecutor;
import com.breadwallet.tools.util.CurrencyUtils;
import com.breadwallet.tools.util.TypesConverter;
import com.breadwallet.tools.util.Utils;
import com.breadwallet.wallet.WalletsMaster;
import com.breadwallet.wallet.abstracts.BaseWalletManager;
import com.breadwallet.wallet.wallets.CryptoAddress;
import com.breadwallet.wallet.wallets.CryptoTransaction;
import com.breadwallet.wallet.wallets.bitcoin.BaseBitcoinWalletManager;
import com.breadwallet.wallet.wallets.bitcoin.WalletBitcoinManager;
import com.breadwallet.wallet.wallets.bitcoin.WalletBchManager;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.math.BigDecimal;

/**
 * BreadWallet
 * <p/>
 * Created by Mihail Gutan <mihail@breadwallet.com> on 6/2/16.
 * Copyright (c) 2016 breadwallet LLC
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

public class ImportPrivKeyTask extends AsyncTask<String, String, String> {
    public static final String TAG = ImportPrivKeyTask.class.getName();

    private static final String UTXO_URL_FORMAT = "https://%s/q/addr/%s/utxo?currency=%s";
    private static final int TOAST_DELAY = 340;
    private Context mContext;
    private BRCoreKey mKey;
    private CryptoTransaction mTransaction;
    private String mCurrencyCode;

    public ImportPrivKeyTask(Activity activity) {
        mContext = activity;
    }

    //params[0] = private key, params[1] = wallet's currency code
    @Override
    protected String doInBackground(String... params) {
        if (params.length != 2) {
            throw new RuntimeException("Must have 2 params");
        }

        String stringKey = params[0];
        if (Utils.isNullOrEmpty(stringKey) || mContext == null) {
            Log.e(TAG, "ImportPrivKeyTask:doInBackground: failed: " + mContext);
            return null;
        }
        mKey = new BRCoreKey();
        mKey.setPrivKey(stringKey);
        BRExecutor.getInstance().forMainThreadTasks().execute(new Runnable() {
            @Override
            public void run() {
                new Handler().postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        BRToast.showCustomToast(mContext, mContext.getString(R.string.Import_checking),
                                BRSharedPrefs.getScreenHeight(mContext) / 4, Toast.LENGTH_LONG, 0);
                    }
                }, TOAST_DELAY);

            }
        });
        mCurrencyCode = BaseBitcoinWalletManager.BITCOIN_CURRENCY_CODE;
        String errorMessage = trySweep(mCurrencyCode, mKey);

        //If not success then we don't have BTC in this private key, check BCH
        if (errorMessage != null) {
            mCurrencyCode = BaseBitcoinWalletManager.BITCASH_CURRENCY_CODE;
            errorMessage = trySweep(mCurrencyCode, mKey);
        }

        final String finalErrorMessage = errorMessage;
        if (!Utils.isNullOrEmpty(errorMessage)) {
            BRExecutor.getInstance().forMainThreadTasks().execute(new Runnable() {
                @Override
                public void run() {
                    Context context = BreadApp.getBreadContext();
                    BRDialog.showCustomDialog(context, context.getString(R.string.Alert_error),
                            finalErrorMessage, context.getString(R.string.Button_ok), null, new BRDialogView.BROnClickListener() {
                                @Override
                                public void onClick(BRDialogView brDialogView) {
                                    brDialogView.dismissWithAnimation();
                                }
                            }, null, null, 0);
                }
            });
        }

        return null;
    }

    /**
     * Checks with the server to see if this private key contains assets of type currencyCode
     *
     * @param currencyCode - the currency code for the wallet we want to check
     * @param coreKey      - the private key we want to check
     * @return returns error String or null if succeeded
     */
    private String trySweep(String currencyCode, BRCoreKey coreKey) {

        String theAddress = coreKey.addressLegacy();

        BaseWalletManager walletManager = WalletsMaster.getInstance(mContext).getWalletByIso(mContext, currencyCode);

        String decoratedAddress = walletManager.decorateAddress(theAddress);

        //automatically uses testnet if x-testnet is true
        String utxoUrl = String.format(UTXO_URL_FORMAT, BreadApp.HOST, decoratedAddress, currencyCode);

        String responseBody = BRApiManager.urlGET(mContext, utxoUrl);
        String errorMessage = null;
        if (isJsonArray(responseBody)) {
            mTransaction = createSweepingTx(mContext, responseBody);
            if (mTransaction == null) {
                errorMessage = mContext.getString(R.string.Import_Error_empty);
                return errorMessage;
            }
        } else {
            errorMessage = responseBody;
        }
        return errorMessage;
    }

    private boolean isJsonArray(String jsonString) {
        if (Utils.isNullOrEmpty(jsonString)) {
            return false;
        }
        try {
            new JSONArray(jsonString);
            return true;
        } catch (JSONException e) {
            e.printStackTrace();
            return false;
        }
    }

    @Override
    protected void onPostExecute(String s) {
        super.onPostExecute(s);
        if (mTransaction == null) return;

        final BaseWalletManager walletManager = WalletsMaster.getInstance(mContext).getWalletByIso(mContext, mCurrencyCode);

        BigDecimal bigAmount = walletManager.getTransactionAmount(mTransaction);
        BigDecimal bigFee = BigDecimal.ZERO;

        for (BRCoreTransactionInput in : mTransaction.getCoreTx().getInputs())
            bigFee = bigFee.add(new BigDecimal(in.getAmount()));
        for (BRCoreTransactionOutput out : mTransaction.getCoreTx().getOutputs())
            bigFee = bigFee.subtract(new BigDecimal(out.getAmount()));

        String formattedFiatAmount = CurrencyUtils.getFormattedAmount(mContext, BRSharedPrefs.getPreferredFiatIso(mContext), walletManager.getFiatForSmallestCrypto(mContext, bigAmount, null));

        //bits, BTCs..
        String amount = CurrencyUtils.getFormattedAmount(mContext, walletManager.getCurrencyCode(), bigAmount);
        String fee = CurrencyUtils.getFormattedAmount(mContext, walletManager.getCurrencyCode(), bigFee.abs());
        String message = String.format(mContext.getString(R.string.Import_confirm), amount, fee);
        String posButton = String.format("%s (%s)", amount, formattedFiatAmount);
        BRDialog.showCustomDialog(mContext, "", message, posButton, mContext.getString(R.string.Button_cancel), new BRDialogView.BROnClickListener() {
            @Override
            public void onClick(BRDialogView brDialogView) {
                BRExecutor.getInstance().forLightWeightBackgroundTasks().execute(new Runnable() {
                    @Override
                    public void run() {

                        if (mTransaction == null) {
                            BRExecutor.getInstance().forMainThreadTasks().execute(new Runnable() {
                                @Override
                                public void run() {
                                    BRDialog.showCustomDialog(mContext, mContext.getString(R.string.JailbreakWarnings_title),
                                            mContext.getString(R.string.Import_Error_notValid), mContext.getString(R.string.Button_ok), null, new BRDialogView.BROnClickListener() {
                                                @Override
                                                public void onClick(BRDialogView brDialogView) {
                                                    brDialogView.dismissWithAnimation();
                                                }
                                            }, null, null, 0);
                                }
                            });
                            return;
                        }

                        mTransaction.getCoreTx().sign(mKey, walletManager.getForkId());
                        BRCorePeerManager peerManager = mCurrencyCode.equalsIgnoreCase("BTC") ? ((WalletBitcoinManager) walletManager).getPeerManager() : ((WalletBchManager) walletManager).getPeerManager();

                        if (!mTransaction.getCoreTx().isSigned()) {
                            String err = "transaction is not signed";
                            Log.e(TAG, "run: " + err);
                            BRReportsManager.reportBug(new IllegalArgumentException(err));
                            return;
                        }

                        peerManager.publishTransaction(mTransaction.getCoreTx());
                    }
                });

                brDialogView.dismissWithAnimation();

            }
        }, new BRDialogView.BROnClickListener() {
            @Override
            public void onClick(BRDialogView brDialogView) {
                brDialogView.dismissWithAnimation();
            }
        }, null, 0);

    }

    private CryptoTransaction createSweepingTx(final Context app, String jsonString) {

        if (jsonString == null || jsonString.isEmpty()) {
            return null;
        }
        BaseWalletManager walletManager = WalletsMaster.getInstance(app).getWalletByIso(app, mCurrencyCode);
        if (walletManager == null) {
            String err = "createSweepingTx: wallet is null for: " + mCurrencyCode;
            BRReportsManager.reportBug(new NullPointerException(err));
            Log.e(TAG, err);
            return null;
        }

        BRCoreTransaction transaction = new BRCoreTransaction();
        long totalAmount = 0;

        try {
            JSONArray jsonArray = new JSONArray(jsonString);
            int length = jsonArray.length();

            for (int i = 0; i < length; i++) {
                JSONObject obj = jsonArray.getJSONObject(i);
                byte[] txid = TypesConverter.hexToBytesReverse(obj.getString("txid"));
                int vout = obj.getInt("vout");
                byte[] scriptPubKey = TypesConverter.hexToBytes(obj.getString("scriptPubKey"));
                long amount = obj.getLong("satoshis");
                totalAmount += amount;
                BRCoreTransactionInput in = new BRCoreTransactionInput(txid, vout, amount, scriptPubKey, new byte[]{}, new byte[]{}, -1);
                transaction.addInput(in);
            }

            if (totalAmount <= 0) {
                return null;
            }

            CryptoAddress address = walletManager.getReceiveAddress(app); //cast, assuming it's BTC or BCH for now
            BRCoreAddress coreAddr = (BRCoreAddress) address.getCoreObject(); //assume BTC and BCH for now

            BigDecimal fee = walletManager.getFeeForTransactionSize(new BigDecimal(transaction.getSize() + 34 + (mKey.getPubKey().length - 33) * transaction.getInputs().length));
            transaction.addOutput(new BRCoreTransactionOutput(new BigDecimal(totalAmount).subtract(fee).longValue(), coreAddr.getPubKeyScript()));
            return new CryptoTransaction(transaction);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return null;
    }


    public static boolean trySweepWallet(final Context ctx, final String privKey, final BaseWalletManager walletManager) {
        if (ctx == null) {
            Log.e(TAG, "trySweepWallet: ctx is null");
            return false;
        }
        if (BRCoreKey.isValidBitcoinBIP38Key(privKey)) {
            Log.d(TAG, "isValidBitcoinBIP38Key true");
            ((Activity) ctx).runOnUiThread(new Runnable() {
                @Override
                public void run() {

                    final AlertDialog.Builder builder = new AlertDialog.Builder(ctx);
//                    builder.setTitle("password protected key");

                    final View input = ((Activity) ctx).getLayoutInflater().inflate(R.layout.view_bip38password_dialog, null);
                    // Specify the type of input expected; this, for example, sets the input as a password, and will mask the text
                    builder.setView(input);

                    final EditText editText = input.findViewById(R.id.bip38password_edittext);

                    new Handler().postDelayed(new Runnable() {
                        public void run() {
                            editText.dispatchTouchEvent(MotionEvent.obtain(SystemClock.uptimeMillis(), SystemClock.uptimeMillis(), MotionEvent.ACTION_DOWN, 0, 0, 0));
                            editText.dispatchTouchEvent(MotionEvent.obtain(SystemClock.uptimeMillis(), SystemClock.uptimeMillis(), MotionEvent.ACTION_UP, 0, 0, 0));

                        }
                    }, 100);

                    // Set up the buttons
                    builder.setPositiveButton(ctx.getString(R.string.Button_ok), new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            if (ctx != null)
                                ((Activity) ctx).runOnUiThread(new Runnable() {
                                    @Override
                                    public void run() {
                                        BRToast.showCustomToast(ctx, ctx.getString(R.string.Import_checking), 500, Toast.LENGTH_LONG, R.drawable.toast_layout_blue);
                                    }
                                });
                            if (editText == null) {
                                Log.e(TAG, "onClick: edit text is null!");
                                return;
                            }

                            final String pass = editText.getText().toString();
                            BRExecutor.getInstance().forLightWeightBackgroundTasks().execute(new Runnable() {
                                @Override
                                public void run() {
                                    String decryptedKey = BRCoreKey.decryptBip38Key(privKey, pass);
                                    //if the decryptedKey is not empty then we have a regular private key and isValidBitcoinBIP38Key will be false
                                    if (decryptedKey.equals("")) {
                                        SpringAnimator.springView(input);
                                        trySweepWallet(ctx, privKey, walletManager);
                                    } else {
                                        trySweepWallet(ctx, decryptedKey, walletManager);
                                    }
                                }
                            });

                        }
                    });
                    builder.setNegativeButton(ctx.getString(R.string.Button_cancel), new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            dialog.cancel();
                        }
                    });

                    builder.show();
                }
            });
            return true;
        } else if (BRCoreKey.isValidBitcoinPrivateKey(privKey)) {
            Log.d(TAG, "isValidBitcoinPrivateKey true");
            new ImportPrivKeyTask(((Activity) ctx)).execute(privKey, walletManager.getCurrencyCode());
            return true;
        } else {
            Log.e(TAG, "trySweepWallet: !isValidBitcoinPrivateKey && !isValidBitcoinBIP38Key");
            return false;
        }
    }

}
