package com.breadwallet.wallet;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.KeyguardManager;
import android.content.Context;
import android.content.DialogInterface;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Handler;
import android.os.SystemClock;
import android.security.keystore.UserNotAuthenticatedException;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;

import com.breadwallet.R;
import com.breadwallet.presenter.customviews.BRDialogView;
import com.breadwallet.presenter.customviews.BRToast;
import com.breadwallet.presenter.entities.ImportPrivKeyEntity;
import com.breadwallet.presenter.entities.TxItem;
import com.breadwallet.tools.animation.BRAnimator;
import com.breadwallet.tools.animation.BRDialog;
import com.breadwallet.tools.animation.SpringAnimator;
import com.breadwallet.tools.manager.BRReportsManager;
import com.breadwallet.tools.manager.BRSharedPrefs;
import com.breadwallet.tools.security.BRKeyStore;
import com.breadwallet.tools.sqlite.MerkleBlockDataSource;
import com.breadwallet.tools.sqlite.PeerDataSource;
import com.breadwallet.tools.sqlite.TransactionDataSource;
import com.breadwallet.tools.threads.BRExecutor;
import com.breadwallet.tools.threads.ImportPrivKeyTask;
import com.breadwallet.tools.util.BRConstants;
import com.breadwallet.tools.util.TypesConverter;
import com.breadwallet.tools.util.Utils;
import com.breadwallet.tools.util.Bip39Reader;
import com.breadwallet.wallet.interfaces.BaseWallet;
import com.breadwallet.wallet.interfaces.OnBalanceChanged;
import com.breadwallet.wallet.wallets.WalletBitcoin;
import com.breadwallet.wallet.wallets.WalletBitcoinCash;
import com.platform.entities.WalletInfo;
import com.platform.tools.KVStoreManager;

import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * BreadWallet
 * <p/>
 * Created by Mihail Gutan <mihail@breadwallet.com> on 12/10/15.
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

public class WalletsMaster {
    private static final String TAG = WalletsMaster.class.getName();

    private static WalletsMaster instance;

    public long getBalance(Context context) {
        return BRSharedPrefs.getCatchedBalance(context);
    }

    private List<BaseWallet> mWallets = new ArrayList<>();

    private WalletsMaster() {
        mWallets.add(WalletBitcoin.getInstance());
        mWallets.add(WalletBitcoinCash.getInstance());
    }

    public static WalletsMaster getInstance() {
        if (instance == null) {
            instance = new WalletsMaster();
        }
        return instance;
    }

    //return the needed wallet for the iso
    public BaseWallet getWalletByIso(String iso) {
        if (Utils.isNullOrEmpty(iso)) return null;
        if (iso.equalsIgnoreCase("BTC")) return WalletBitcoin.getInstance();
        if (iso.equalsIgnoreCase("BCH")) return WalletBitcoinCash.getInstance();
        return null;
    }

    public synchronized boolean generateRandomSeed(final Context ctx) {
        SecureRandom sr = new SecureRandom();
        final String[] words;
        List<String> list;
        String languageCode = Locale.getDefault().getLanguage();
        if (languageCode == null) languageCode = "en";
        list = Bip39Reader.bip39List(ctx, languageCode);
        words = list.toArray(new String[list.size()]);
        final byte[] randomSeed = sr.generateSeed(16);
        if (words.length != 2048) {
            BRReportsManager.reportBug(new IllegalArgumentException("the list is wrong, size: " + words.length), true);
            return false;
        }
        if (randomSeed.length != 16)
            throw new NullPointerException("failed to create the seed, seed length is not 128: " + randomSeed.length);
        byte[] strPhrase = encodeSeed(randomSeed, words);
        if (strPhrase == null || strPhrase.length == 0) {
            BRReportsManager.reportBug(new NullPointerException("failed to encodeSeed"), true);
            return false;
        }
        String[] splitPhrase = new String(strPhrase).split(" ");
        if (splitPhrase.length != 12) {
            BRReportsManager.reportBug(new NullPointerException("phrase does not have 12 words:" + splitPhrase.length + ", lang: " + languageCode), true);
            return false;
        }
        boolean success = false;
        try {
            success = BRKeyStore.putPhrase(strPhrase, ctx, BRConstants.PUT_PHRASE_NEW_WALLET_REQUEST_CODE);
        } catch (UserNotAuthenticatedException e) {
            return false;
        }
        if (!success) return false;
        byte[] phrase;
        try {
            phrase = BRKeyStore.getPhrase(ctx, 0);
        } catch (UserNotAuthenticatedException e) {
            throw new RuntimeException("Failed to retrieve the phrase even though at this point the system auth was asked for sure.");
        }
        if (Utils.isNullOrEmpty(phrase)) throw new NullPointerException("phrase is null!!");
        byte[] nulTermPhrase = TypesConverter.getNullTerminatedPhrase(phrase);
        if (nulTermPhrase == null || nulTermPhrase.length == 0)
            throw new RuntimeException("nulTermPhrase is null");
        byte[] seed = getSeedFromPhrase(nulTermPhrase);
        if (seed == null || seed.length == 0) throw new RuntimeException("seed is null");
        byte[] authKey = getAuthPrivKeyForAPI(seed);
        if (authKey == null || authKey.length == 0) {
            BRReportsManager.reportBug(new IllegalArgumentException("authKey is invalid"), true);
        }
        BRKeyStore.putAuthKey(authKey, ctx);
        int walletCreationTime = (int) (System.currentTimeMillis() / 1000);
        BRKeyStore.putWalletCreationTime(walletCreationTime, ctx);
        final WalletInfo info = new WalletInfo();
        info.creationDate = walletCreationTime;
        BRExecutor.getInstance().forLightWeightBackgroundTasks().execute(new Runnable() {
            @Override
            public void run() {
                KVStoreManager.getInstance().putWalletInfo(ctx, info); //push the creation time to the kv store
            }
        });

        byte[] strBytes = TypesConverter.getNullTerminatedPhrase(strPhrase);
        byte[] pubKey = WalletsMaster.getInstance().getMasterPubKey(strBytes);
        BRKeyStore.putMasterPublicKey(pubKey, ctx);

        return true;

    }

    public boolean wipeKeyStore(Context context) {
        Log.d(TAG, "wipeKeyStore");
        return BRKeyStore.resetWalletKeyStore(context);
    }

    /**
     * true if keystore is available and we know that no wallet exists on it
     */
    public boolean noWallet(Context ctx) {
        byte[] pubkey = BRKeyStore.getMasterPublicKey(ctx);

        if (pubkey == null || pubkey.length == 0) {
            byte[] phrase;
            try {
                phrase = BRKeyStore.getPhrase(ctx, 0);
                //if not authenticated, an error will be thrown and returned false, so no worry about mistakenly removing the wallet
                if (phrase == null || phrase.length == 0) {
                    return true;
                }
            } catch (UserNotAuthenticatedException e) {
                return false;
            }

        }
        return false;
    }

    public boolean noWalletForPlatform(Context ctx) {
        byte[] pubkey = BRKeyStore.getMasterPublicKey(ctx);
        return pubkey == null || pubkey.length == 0;
    }

    /**
     * true if device passcode is enabled
     */
    public boolean isPasscodeEnabled(Context ctx) {
        KeyguardManager keyguardManager = (KeyguardManager) ctx.getSystemService(Activity.KEYGUARD_SERVICE);
        return keyguardManager.isKeyguardSecure();
    }

    public boolean isNetworkAvailable(Context ctx) {
        if (ctx == null) return false;
        ConnectivityManager cm = (ConnectivityManager) ctx.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo netInfo = cm.getActiveNetworkInfo();
        return netInfo != null && netInfo.isConnectedOrConnecting();

    }

    public static boolean refreshAddress(Context ctx) {
        String address = getReceiveAddress();
        if (Utils.isNullOrEmpty(address)) {
            Log.e(TAG, "refreshAddress: WARNING, retrieved address:" + address);
            return false;
        }
        BRSharedPrefs.putReceiveAddress(ctx, address);
        return true;

    }

    public void wipeWalletButKeystore(final Context ctx) {
        Log.d(TAG, "wipeWalletButKeystore");
        BRExecutor.getInstance().forLightWeightBackgroundTasks().execute(new Runnable() {
            @Override
            public void run() {
                BRPeerManager.getInstance().peerManagerFreeEverything();
                walletFreeEverything();
                TransactionDataSource.getInstance(ctx).deleteAllTransactions();
                MerkleBlockDataSource.getInstance(ctx).deleteAllBlocks();
                PeerDataSource.getInstance(ctx).deleteAllPeers();
                BRSharedPrefs.clearAllPrefs(ctx);
            }
        });

    }

    public void wipeAll(Context app) {
        wipeKeyStore(app);
        wipeWalletButKeystore(app);
    }

    public boolean trySweepWallet(final Context ctx, final String privKey) {
        if (ctx == null) return false;
        if (isValidBitcoinBIP38Key(privKey)) {
            Log.d(TAG, "isValidBitcoinBIP38Key true");
            ((Activity) ctx).runOnUiThread(new Runnable() {
                @Override
                public void run() {

                    final AlertDialog.Builder builder = new AlertDialog.Builder(ctx);
//                    builder.setTitle("password protected key");

                    final View input = ((Activity) ctx).getLayoutInflater().inflate(R.layout.view_bip38password_dialog, null);
                    // Specify the type of input expected; this, for example, sets the input as a password, and will mask the text
                    builder.setView(input);

                    final EditText editText = (EditText) input.findViewById(R.id.bip38password_edittext);

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
                            Log.e(TAG, "onClick: before");
                            BRExecutor.getInstance().forLightWeightBackgroundTasks().execute(new Runnable() {
                                @Override
                                public void run() {
                                    String decryptedKey = decryptBip38Key(privKey, pass);
                                    Log.e(TAG, "onClick: after");

                                    if (decryptedKey.equals("")) {
                                        SpringAnimator.springView(input);
                                        trySweepWallet(ctx, privKey);
                                    } else {
                                        trySweepWallet(ctx, decryptedKey);
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
        } else if (isValidBitcoinPrivateKey(privKey)) {
            Log.d(TAG, "isValidBitcoinPrivateKey true");
            new ImportPrivKeyTask(((Activity) ctx)).execute(privKey);
            return true;
        } else {
            Log.e(TAG, "trySweepWallet: !isValidBitcoinPrivateKey && !isValidBitcoinBIP38Key");
            return false;
        }
    }

    public void setBalanceChangedListener(OnBalanceChanged listener) {
        for (BaseWallet wallet : mWallets) {
            //todo add listeners to all wallets
        }
    }

    public void refreshBalances(Context app) {
        for (BaseWallet wallet : mWallets) {
            //todo add listeners to all wallets
        }
    }

    public void initWallets(Context app) {
        for (BaseWallet wallet : mWallets) {
            //todo init all wallets
        }
    }


    public void startTheWalletIfExists(final Activity app) {
        final WalletsMaster m = WalletsMaster.getInstance();
        if (!m.isPasscodeEnabled(app)) {
            //Device passcode/password should be enabled for the app to work
            BRDialog.showCustomDialog(app, app.getString(R.string.JailbreakWarnings_title), app.getString(R.string.Prompts_NoScreenLock_body_android),
                    app.getString(R.string.AccessibilityLabels_close), null, new BRDialogView.BROnClickListener() {
                        @Override
                        public void onClick(BRDialogView brDialogView) {
                            app.finish();
                        }
                    }, null, new DialogInterface.OnDismissListener() {
                        @Override
                        public void onDismiss(DialogInterface dialog) {
                            app.finish();
                        }
                    }, 0);
        } else {
            if (!m.noWallet(app)) {
                BRAnimator.startBreadActivity(app, true);
            }
            //else just sit in the intro screen

        }
    }

    @WorkerThread
    public void initWallet(final Context ctx) {
        if (ActivityUTILS.isMainThread()) throw new NetworkOnMainThreadException();
        if (itInitiatingWallet) return;
        itInitiatingWallet = true;
        try {
            Log.d(TAG, "initWallet:" + Thread.currentThread().getName());
            if (ctx == null) {
                Log.e(TAG, "initWallet: ctx is null");
                return;
            }
            BRWalletManager m = BRWalletManager.getInstance();
            final BRPeerManager pm = BRPeerManager.getInstance();

            if (!m.isCreated()) {
                List<BRTransactionEntity> transactions = TransactionDataSource.getInstance(ctx).getAllTransactions();
                int transactionsCount = transactions.size();
                if (transactionsCount > 0) {
                    m.createTxArrayWithCount(transactionsCount);
                    for (BRTransactionEntity entity : transactions) {
                        m.putTransaction(entity.getBuff(), entity.getBlockheight(), entity.getTimestamp());
                    }
                }

                byte[] pubkeyEncoded = BRKeyStore.getMasterPublicKey(ctx);
                if (Utils.isNullOrEmpty(pubkeyEncoded)) {
                    Log.e(TAG, "initWallet: pubkey is missing");
                    return;
                }
                //Save the first address for future check
                m.createWallet(transactionsCount, pubkeyEncoded);
                String firstAddress = BRWalletManager.getFirstAddress(pubkeyEncoded);
                BRSharedPrefs.putFirstAddress(ctx, firstAddress);
                long fee = BRSharedPrefs.getFeePerKb(ctx);
                if (fee == 0) {
                    fee = defaultFee();
                    BREventManager.getInstance().pushEvent("wallet.didUseDefaultFeePerKB");
                }
                BRWalletManager.getInstance().setFeePerKb(fee, isEconomyFee);
            }

            if (!pm.isCreated()) {
                List<BRMerkleBlockEntity> blocks = MerkleBlockDataSource.getInstance(ctx).getAllMerkleBlocks();
                List<BRPeerEntity> peers = PeerDataSource.getInstance(ctx).getAllPeers();
                final int blocksCount = blocks.size();
                final int peersCount = peers.size();
                if (blocksCount > 0) {
                    pm.createBlockArrayWithCount(blocksCount);
                    for (BRMerkleBlockEntity entity : blocks) {
                        pm.putBlock(entity.getBuff(), entity.getBlockHeight());
                    }
                }
                if (peersCount > 0) {
                    pm.createPeerArrayWithCount(peersCount);
                    for (BRPeerEntity entity : peers) {
                        pm.putPeer(entity.getAddress(), entity.getPort(), entity.getTimeStamp());
                    }
                }
                Log.d(TAG, "blocksCount before connecting: " + blocksCount);
                Log.d(TAG, "peersCount before connecting: " + peersCount);

                int walletTime = BRKeyStore.getWalletCreationTime(ctx);

                Log.e(TAG, "initWallet: walletTime: " + walletTime);
                pm.create(walletTime, blocksCount, peersCount);
                BRPeerManager.getInstance().updateFixedPeer(ctx);
            }

            pm.connect();
            if (BRSharedPrefs.getStartHeight(ctx) == 0)
                BRSharedPrefs.putStartHeight(ctx, BRPeerManager.getCurrentBlockHeight());
        } finally {
            itInitiatingWallet = false;
        }
    }

    public void addBalanceChangedListener(OnBalanceChanged listener) {
        if (balanceListeners == null) {
            Log.e(TAG, "addBalanceChangedListener: statusUpdateListeners is null");
            return;
        }
        if (!balanceListeners.contains(listener))
            balanceListeners.add(listener);
    }

    public void removeListener(OnBalanceChanged listener) {
        if (balanceListeners == null) {
            Log.e(TAG, "addBalanceChangedListener: statusUpdateListeners is null");
            return;
        }
        balanceListeners.remove(listener);

    }

    public interface OnBalanceChanged {
        void onBalanceChanged(long balance);

    }

    private native byte[] encodeSeed(byte[] seed, String[] wordList);

    public native void createWallet(int transactionCount, byte[] pubkey);

    public native void putTransaction(byte[] transaction, long blockHeight, long timeStamp);

    public native void createTxArrayWithCount(int count);

    public native byte[] getMasterPubKey(byte[] normalizedString);

    public static native String getReceiveAddress();

    public native TxItem[] getTransactions();

    public static native boolean validateAddress(String address);

    public native boolean addressContainedInWallet(String address);

    public native boolean addressIsUsed(String address);

    public native int feeForTransaction(String addressHolder, long amountHolder);

    public native int feeForTransactionAmount(long amountHolder);

    public native long getMinOutputAmount();

    public native long getMaxOutputAmount();

    public native boolean isCreated();

    public native byte[] tryTransaction(String addressHolder, long amountHolder);

    // returns the given amount (amount is in satoshis) in local currency units (i.e. pennies, pence)
    // price is local currency units per bitcoin
    public native long localAmount(long amount, double price);

    // returns the given local currency amount in satoshis
    // price is local currency units (i.e. pennies, pence) per bitcoin
    public native long bitcoinAmount(long localAmount, double price);

    public native void walletFreeEverything();

    public native boolean validateRecoveryPhrase(String[] words, String phrase);

    public native static String getFirstAddress(byte[] mpk);

    public native byte[] publishSerializedTransaction(byte[] serializedTransaction, byte[] phrase);

    public native long getTotalSent();

    public native long setFeePerKb(long fee, boolean ignore);

    public native boolean isValidBitcoinPrivateKey(String key);

    public native boolean isValidBitcoinBIP38Key(String key);

    public native String getAddressFromPrivKey(String key);

    public native void createInputArray();

    public native void addInputToPrivKeyTx(byte[] hash, int vout, byte[] script, long amount);

    public native boolean confirmKeySweep(byte[] tx, String key);

    public native ImportPrivKeyEntity getPrivKeyObject();

    public native String decryptBip38Key(String privKey, String pass);

    public native String reverseTxHash(String txHash);

    public native String txHashToHex(byte[] txHash);

//    public native String txHashSha256Hex(String txHash);

    public native long nativeBalance();

    public native long defaultFee();

    public native long maxFee();

//    public native int getTxCount();

    public native long getMinOutputAmountRequested();

    public static native byte[] getAuthPrivKeyForAPI(byte[] seed);

    public static native String getAuthPublicKeyForAPI(byte[] privKey);

    public static native byte[] getSeedFromPhrase(byte[] phrase);

    public static native boolean isTestNet();

    public static native byte[] sweepBCash(byte[] pubKey, String address, byte[] phrase);

    public static native long getBCashBalance(byte[] pubKey);

    public static native int getTxSize(byte[] serializedTx);


}