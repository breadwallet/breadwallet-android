package com.breadwallet.wallet;

import android.content.Context;
import android.os.Handler;
import android.util.Log;

import com.breadwallet.tools.CurrencyManager;
import com.breadwallet.tools.CustomLogger;
import com.breadwallet.tools.WordsReader;
import com.breadwallet.tools.security.KeyStoreManager;

import java.io.IOException;
import java.security.SecureRandom;
import java.util.List;

/**
 * BreadWallet
 * <p/>
 * Created by Mihail on 12/4/15.
 * Copyright (c) 2015 Mihail Gutan <mihail@breadwallet.com>
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

public class BRTestWallet {
    public static final String TAG = BRTestWallet.class.getName();
    private static BRTestWallet instance;
    private byte[] walletBuff;
    private static Context ctx;

    private BRTestWallet() {
        /**
         * initialize the class
         */
    }

    public static synchronized BRTestWallet getInstance(Context context) {
        ctx = context;
        if (instance == null) {
            instance = new BRTestWallet();
        }
        return instance;
    }


    public String generateRandomSeed() {

        final SecureRandom sr = new SecureRandom();
        final byte[] keyBytes = new byte[16];
        sr.nextBytes(keyBytes);
        String[] words = new String[0];
        List<String> list;
        try {
            list = WordsReader.getWordList(ctx);
            words = list.toArray(new String[list.size()]);
//            CustomLogger.LogThis(words);
        } catch (IOException e) {
            e.printStackTrace();
        }
        if (words.length < 2000)
            throw new IllegalArgumentException("the list is wrong, size: " + words.length);
        String phrase = new String(encodeSeed(keyBytes, words));
        Log.e(TAG, "THE COOL RESULT: " + phrase);
//        String phrase = "short apple trunk riot coyote innocent zebra venture ill lava shop test";
        boolean success = KeyStoreManager.setKeyStoreString(phrase, ctx);
        Log.e(TAG, "setKeyStoreString was successful: " + success);
        return success ? phrase : null;
    }

    /**
     * Wallet callbacks
     */

    public void onBalanceChanged(final long balance) {
        Log.e(TAG, "THIS IS THE BALANCE FROM C: " + balance);
        CurrencyManager.getInstance(ctx).setBalance(balance);
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                long newBalance = 1435992491;
                Log.e(TAG, "Changing balance to: " + newBalance);
                CurrencyManager.getInstance(ctx).setBalance(newBalance);
            }
        }, 10000);
    }

    public void onTxAdded(byte[] tx) {

    }

    public void onTxUpdated(byte[] tx) {

    }

    public void onTxDeleted(byte[] tx) {

    }

    private native byte[] encodeSeed(byte[] seed, String[] wordList);

    public native void initWallet();

    public native byte[] getMasterPubKey(String normalizedString);
}
