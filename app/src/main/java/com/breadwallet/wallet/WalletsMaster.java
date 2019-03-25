package com.breadwallet.wallet;

import android.app.Activity;
import android.app.KeyguardManager;
import android.content.Context;
import android.content.DialogInterface;
import android.security.keystore.UserNotAuthenticatedException;
import android.support.annotation.WorkerThread;
import android.text.format.DateUtils;
import android.util.Log;

import com.breadwallet.BreadApp;
import com.breadwallet.R;
import com.breadwallet.core.BRCoreKey;
import com.breadwallet.core.BRCoreMasterPubKey;
import com.breadwallet.core.ethereum.BREthereumToken;
import com.breadwallet.presenter.customviews.BRDialogView;
import com.breadwallet.presenter.entities.TokenItem;
import com.breadwallet.tools.animation.UiUtils;
import com.breadwallet.tools.animation.BRDialog;
import com.breadwallet.tools.manager.BRReportsManager;
import com.breadwallet.tools.manager.BRSharedPrefs;
import com.breadwallet.tools.security.BRKeyStore;
import com.breadwallet.tools.threads.executor.BRExecutor;
import com.breadwallet.tools.util.BRConstants;
import com.breadwallet.tools.util.Bip39Reader;
import com.breadwallet.tools.util.TokenUtil;
import com.breadwallet.tools.util.TrustedNode;
import com.breadwallet.tools.util.Utils;
import com.breadwallet.wallet.abstracts.BaseWalletManager;
import com.breadwallet.wallet.abstracts.BalanceUpdateListener;
import com.breadwallet.wallet.wallets.bitcoin.BaseBitcoinWalletManager;
import com.breadwallet.wallet.wallets.bitcoin.WalletBchManager;
import com.breadwallet.wallet.wallets.bitcoin.WalletBitcoinManager;
import com.breadwallet.wallet.wallets.ethereum.WalletEthManager;
import com.breadwallet.wallet.wallets.ethereum.WalletTokenManager;
import com.platform.entities.TokenListMetaData;
import com.platform.entities.WalletInfoData;
import com.platform.tools.KVStoreManager;

import java.math.BigDecimal;
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

    private List<BaseWalletManager> mWallets = new ArrayList<>();
    private TokenListMetaData mTokenListMetaData;
    private List<BalanceUpdateListener> mBalancesUpdateListeners = new ArrayList<>();

    private WalletsMaster(Context app) {
    }

    public synchronized static WalletsMaster getInstance(Context app) {
        if (instance == null) {
            instance = new WalletsMaster(app);
        }
        return instance;
    }

    //expensive operation (uses the KVStore), only update when needed and not in a loop.
    public synchronized void updateWallets(Context app) {
        WalletEthManager ethWallet = WalletEthManager.getInstance(app);
        if (ethWallet == null) {
            return; //return empty wallet list if ETH is null (meaning no public key yet)
        }

        mWallets.clear();
        mTokenListMetaData = KVStoreManager.getTokenListMetaData(app);
        if (mTokenListMetaData == null) {
            List<TokenListMetaData.TokenInfo> enabled = new ArrayList<>();
            enabled.add(new TokenListMetaData.TokenInfo(WalletBitcoinManager.BITCOIN_CURRENCY_CODE, false, null));
            enabled.add(new TokenListMetaData.TokenInfo(WalletBchManager.BITCASH_CURRENCY_CODE, false, null));
            enabled.add(new TokenListMetaData.TokenInfo(WalletEthManager.ETH_CURRENCY_CODE, false, null));
            enabled.add(new TokenListMetaData.TokenInfo(WalletTokenManager.BRD_CURRENCY_CODE, true, WalletTokenManager.BRD_CONTRACT_ADDRESS));
            mTokenListMetaData = new TokenListMetaData(enabled, null);
            KVStoreManager.putTokenListMetaData(app, mTokenListMetaData); //put default currencies if null
        }

        for (TokenListMetaData.TokenInfo enabled : mTokenListMetaData.enabledCurrencies) {

            boolean isHidden = mTokenListMetaData.isCurrencyHidden(enabled.symbol);

            if (enabled.symbol.equalsIgnoreCase(BaseBitcoinWalletManager.BITCOIN_CURRENCY_CODE) && !isHidden) {
                //BTC wallet
                mWallets.add(WalletBitcoinManager.getInstance(app));
            } else if (enabled.symbol.equalsIgnoreCase(BaseBitcoinWalletManager.BITCASH_CURRENCY_CODE) && !isHidden) {
                //BCH wallet
                mWallets.add(WalletBchManager.getInstance(app));
            } else if (enabled.symbol.equalsIgnoreCase(WalletEthManager.ETH_CURRENCY_CODE) && !isHidden) {
                //ETH wallet
                mWallets.add(ethWallet);
            } else {
                //add ERC20 wallet
                WalletTokenManager tokenWallet = WalletTokenManager.getTokenWalletByIso(app, enabled.symbol);
                if (tokenWallet != null && !isHidden) {
                    mWallets.add(tokenWallet);
                }
            }

        }
    }

    public synchronized List<BaseWalletManager> getAllWallets(Context context) {
        if (mWallets == null || mWallets.size() == 0) {
            updateWallets(context);
        }
        return mWallets;

    }

    public synchronized List<String> getAllCurrencyCodesPossible(Context context) {
        List<String> currencyCodes = new ArrayList<>();
        currencyCodes.add(WalletBitcoinManager.BITCOIN_CURRENCY_CODE);
        currencyCodes.add(WalletBitcoinManager.BITCASH_CURRENCY_CODE);
        currencyCodes.add(WalletEthManager.ETH_CURRENCY_CODE);
        for (TokenItem tokenItem : TokenUtil.getTokenItems(context)) {
            currencyCodes.add(tokenItem.symbol.toUpperCase());
        }
        return currencyCodes;
    }

    //return the needed wallet for the iso
    public BaseWalletManager getWalletByIso(Context app, String currencyCode) {
        if (WalletBitcoinManager.BITCOIN_CURRENCY_CODE.equalsIgnoreCase(currencyCode)) {
            return WalletBitcoinManager.getInstance(app);
        }
        if (WalletBitcoinManager.BITCASH_CURRENCY_CODE.equalsIgnoreCase(currencyCode)) {
            return WalletBchManager.getInstance(app);
        }
        if (WalletEthManager.ETH_CURRENCY_CODE.equalsIgnoreCase(currencyCode)) {
            return WalletEthManager.getInstance(app);
        } else if (isCurrencyCodeErc20(app, currencyCode)) {
            return WalletTokenManager.getTokenWalletByIso(app, currencyCode);
        }
        return null;
    }

    public BaseWalletManager getCurrentWallet(Context app) {
        return getWalletByIso(app, BRSharedPrefs.getCurrentWalletCurrencyCode(app));
    }

    //get the total fiat balance held in all the wallets in the smallest unit (e.g. cents)
    public BigDecimal getAggregatedFiatBalance(Context app) {
        long start = System.currentTimeMillis();
        BigDecimal totalBalance = BigDecimal.ZERO;
        List<BaseWalletManager> list = new ArrayList<>(getAllWallets(app));
        for (BaseWalletManager wallet : list) {
            BigDecimal fiatBalance = wallet.getFiatBalance(app);
            if (fiatBalance != null) {
                totalBalance = totalBalance.add(fiatBalance);
            }
        }
        return totalBalance;
    }

    public synchronized boolean generateRandomSeed(final Context ctx) {
        if (Utils.isNullOrEmpty(BRKeyStore.getMasterPublicKey(ctx))) {
            SecureRandom sr = new SecureRandom();
            String languageCode = Locale.getDefault().getLanguage();
            List<String> wordList = Bip39Reader.getBip39Words(ctx, languageCode);
            final String[] words = wordList.toArray(new String[wordList.size()]);
            final byte[] randomSeed = sr.generateSeed(16);
            if (words.length != 2048) {
                BRReportsManager.reportBug(new IllegalArgumentException("the list is wrong, size: " + words.length), true);
                return false;
            }
            if (randomSeed.length != 16)
                throw new NullPointerException("failed to create the seed, seed length is not 128: " + randomSeed.length);
            byte[] paperKeyBytes = BRCoreMasterPubKey.generatePaperKey(randomSeed, words);
            if (paperKeyBytes == null || paperKeyBytes.length == 0) {
                BRReportsManager.reportBug(new NullPointerException("failed to encodeSeed"), true);
                return false;
            }
            String[] splitPhrase = new String(paperKeyBytes).split(" ");
            if (splitPhrase.length != 12) {
                BRReportsManager.reportBug(new NullPointerException("phrase does not have 12 words:" + splitPhrase.length + ", lang: " + languageCode), true);
                return false;
            }
            boolean success = false;
            try {
                success = BRKeyStore.putPhrase(paperKeyBytes, ctx, BRConstants.PUT_PHRASE_NEW_WALLET_REQUEST_CODE);
            } catch (UserNotAuthenticatedException e) {
                return false; // While this is wrong (never ignore a UNAE), it seems to not be causing issues at the moment.
            }
            if (!success) return false;
            byte[] phrase;
            try {
                phrase = BRKeyStore.getPhrase(ctx, 0);
            } catch (UserNotAuthenticatedException e) {
                throw new RuntimeException("Failed to retrieve the phrase even though at this point the system auth was asked for sure.");
            }
            if (Utils.isNullOrEmpty(phrase)) throw new NullPointerException("phrase is null!!");
            if (phrase.length == 0) throw new RuntimeException("phrase is empty");
            byte[] seed = BRCoreKey.getSeedFromPhrase(phrase);
            if (seed == null || seed.length == 0) throw new RuntimeException("seed is null");
            byte[] authKey = BRCoreKey.getAuthPrivKeyForAPI(seed);
            if (authKey == null || authKey.length == 0) {
                BRReportsManager.reportBug(new IllegalArgumentException("authKey is invalid"), true);
            }
            BRKeyStore.putAuthKey(authKey, ctx);
            int walletCreationTime = (int) (System.currentTimeMillis() / DateUtils.SECOND_IN_MILLIS);
            BRKeyStore.putWalletCreationTime(walletCreationTime, ctx);
            final WalletInfoData info = new WalletInfoData();
            info.creationDate = walletCreationTime;
            KVStoreManager.putWalletInfo(ctx, info); //push the creation time to the kv store

            //store the serialized in the KeyStore
            byte[] pubKey = new BRCoreMasterPubKey(paperKeyBytes, true).serialize();
            BRKeyStore.putMasterPublicKey(pubKey, ctx);
        }

        return true;
    }

    public boolean isIsoCrypto(Context app, String iso) {
        List<BaseWalletManager> list = new ArrayList<>(getAllWallets(app));
        for (BaseWalletManager w : list) {
            if (w.getCurrencyCode().equalsIgnoreCase(iso)) {
                return true;
            }
        }
        return false;
    }

    public boolean isCurrencyCodeErc20(Context app, String iso) {
        WalletEthManager walletEthManager = WalletEthManager.getInstance(app);
        if (Utils.isNullOrEmpty(iso) || walletEthManager == null) {
            return false;
        }
        BREthereumToken[] tokens = walletEthManager.node.getTokens();
        for (BREthereumToken token : tokens) {
            if (token.getSymbol().equalsIgnoreCase(iso)) {
                return true;
            }
        }
        return false;
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

    public void wipeWalletButKeystore(final Context ctx) {
        BRExecutor.getInstance().forLightWeightBackgroundTasks().execute(new Runnable() {
            @Override
            public void run() {
                List<BaseWalletManager> list = new ArrayList<>(getAllWallets(ctx));
                for (BaseWalletManager wallet : list) {
                    wallet.wipeData(ctx);
                }
            }
        });
    }

    public void wipeAll(Context app) {
        wipeKeyStore(app);
        wipeWalletButKeystore(app);
    }

    public void refreshBalances(Context app) {
        List<BaseWalletManager> list = new ArrayList<>(getAllWallets(app));
        for (BaseWalletManager wallet : list) {
            wallet.refreshCachedBalance(app);
        }
        for (BalanceUpdateListener balanceUpdateListener : mBalancesUpdateListeners) {
            balanceUpdateListener.onBalanceChanged(null);
        }
    }

    public void setSpendingLimitIfNotSet(final Context app, final BaseWalletManager wm) {
        if (app == null) return;
        BigDecimal limit = BRKeyStore.getTotalLimit(app, wm.getCurrencyCode());
        if (limit.compareTo(BigDecimal.ZERO) == 0) {
            BRExecutor.getInstance().forLightWeightBackgroundTasks().execute(new Runnable() {
                @Override
                public void run() {
                    long start = System.currentTimeMillis();
                    BaseWalletManager wallet = WalletsMaster.getInstance(app).getCurrentWallet(app);
                    BigDecimal totalSpent = wallet == null ? BigDecimal.ZERO : wallet.getTotalSent(app);
                    BigDecimal totalLimit = totalSpent.add(BRKeyStore.getSpendLimit(app, wm.getCurrencyCode()));
                    BRKeyStore.putTotalLimit(app, totalLimit, wm.getCurrencyCode());
                }
            });

        }
    }

    @WorkerThread
    public void initLastWallet(Context app) {
        if (app == null) {
            app = BreadApp.getBreadContext();
            if (app == null) {
                Log.e(TAG, "initLastWallet: FAILED, app is null");
                return;
            }
        }
        BaseWalletManager wallet = getWalletByIso(app, BRSharedPrefs.getCurrentWalletCurrencyCode(app));
        if (wallet == null) {
            wallet = getWalletByIso(app, BaseBitcoinWalletManager.BITCOIN_CURRENCY_CODE);
        }
        if (wallet != null) {
            wallet.connect(app);
        }
    }

    public static boolean isBrdWalletCreated(Context context) {
        return !Utils.isNullOrEmpty(BRKeyStore.getMasterPublicKey(context));
    }

    @WorkerThread
    public void updateFixedPeer(Context app, BaseWalletManager wm) {
        String node = BRSharedPrefs.getTrustNode(app, wm.getCurrencyCode());
        if (!Utils.isNullOrEmpty(node)) {
            String host = TrustedNode.getNodeHost(node);
            int port = TrustedNode.getNodePort(node);
            boolean success = wm.useFixedNode(host, port);
            if (!success) {
                Log.e(TAG, "updateFixedPeer: Failed to updateFixedPeer with input: " + node);
            } else {
                Log.d(TAG, "updateFixedPeer: succeeded");
            }
        }
        wm.connect(app);

    }

    public void startTheWalletIfExists(final Activity app) {
        final WalletsMaster m = WalletsMaster.getInstance(app);
        if (!m.isPasscodeEnabled(app)) {
            //Device passcode/password should be enabled for the app to work
            BRDialog.showCustomDialog(app, app.getString(R.string.JailbreakWarnings_title),
                    app.getString(R.string.Prompts_NoScreenLock_body_android),
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
                UiUtils.startBreadActivity(app, true);
            }
            //else just sit in the intro screen

        }
    }

    public boolean hasWallet(String currencyCode) {
        for (BaseWalletManager walletManager : mWallets) {
            if (walletManager.getCurrencyCode().equalsIgnoreCase(currencyCode)) {
                return true;
            }
        }
        return false;
    }

    public void addBalanceUpdateListener(BalanceUpdateListener balanceUpdateListener) {
        if (!mBalancesUpdateListeners.contains(balanceUpdateListener)) {
            mBalancesUpdateListeners.add(balanceUpdateListener);
        }
    }

    public void removeBalanceUpdateListener(BalanceUpdateListener onBalancesUpdated) {
        mBalancesUpdateListeners.remove(onBalancesUpdated);
    }

}
