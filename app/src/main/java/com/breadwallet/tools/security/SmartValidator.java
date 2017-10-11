package com.breadwallet.tools.security;

import android.app.Activity;
import android.content.Context;
import android.util.Log;

import com.breadwallet.R;
import com.breadwallet.presenter.activities.util.ActivityUTILS;
import com.breadwallet.tools.manager.BRSharedPrefs;
import com.breadwallet.tools.util.Bip39Reader;
import com.breadwallet.tools.util.TypesConverter;
import com.breadwallet.wallet.BRWalletManager;
import com.google.firebase.crash.FirebaseCrash;

import java.io.IOException;
import java.text.Normalizer;
import java.util.Arrays;
import java.util.List;

/**
 * BreadWallet
 * <p/>
 * Created by Mihail Gutan on <mihail@breadwallet.com> 10/11/17.
 * Copyright (c) 2017 breadwallet LLC
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
public class SmartValidator {

    private static final String TAG = SmartValidator.class.getName();

    public static boolean isPaperKeyValid(Context ctx, String phrase) {
        String[] words = new String[0];
        List<String> list;

        String[] cleanWordList = null;
        try {
            boolean isLocal = true;
            String languageCode = ctx.getString(R.string.lang);
            list = Bip39Reader.getWordList(ctx, languageCode);

            String[] phraseWords = phrase.split(" ");
            if (!list.contains(phraseWords[0])) {
                isLocal = false;
            }
            if (!isLocal) {
                String lang = Bip39Reader.getLang(ctx, phraseWords[0]);
                if (lang != null) {
                    list = Bip39Reader.getWordList(ctx, lang);
                }

            }
            words = list.toArray(new String[list.size()]);
            cleanWordList = Bip39Reader.cleanWordList(words);
            if (cleanWordList == null) return false;
        } catch (IOException e) {
            e.printStackTrace();
        }
        if (words.length != 2048) {
            RuntimeException ex = new IllegalArgumentException("words.length is not 2048");
            FirebaseCrash.report(ex);
            throw ex;
        }
        return BRWalletManager.getInstance().validateRecoveryPhrase(cleanWordList, phrase);


    }

    public static boolean isPaperKeyCorrect(String insertedPhrase, Context activity) {
        String normalizedPhrase = Normalizer.normalize(insertedPhrase.trim(), Normalizer.Form.NFKD);
        if (!SmartValidator.isPaperKeyValid(activity, normalizedPhrase))
            return false;
        BRWalletManager m = BRWalletManager.getInstance();
        byte[] rawPhrase = normalizedPhrase.getBytes();
        byte[] bytePhrase = TypesConverter.getNullTerminatedPhrase(rawPhrase);
        byte[] pubKey = m.getMasterPubKey(bytePhrase);
        byte[] pubKeyFromKeyStore = new byte[0];
        try {
            pubKeyFromKeyStore = BRKeyStore.getMasterPublicKey(activity);
        } catch (Exception e) {
            e.printStackTrace();
            BRErrorPipe.parseKeyStoreError(activity, e, "", true);
        }
        Arrays.fill(bytePhrase, (byte) 0);
        return Arrays.equals(pubKey, pubKeyFromKeyStore);
    }

    public static boolean checkFirstAddress(Activity app,  byte[] mpk) {
        String addressFromPrefs = BRSharedPrefs.getFirstAddress(app);
        String generatedAddress = BRWalletManager.getFirstAddress(mpk);
        if (!addressFromPrefs.equalsIgnoreCase(generatedAddress) && addressFromPrefs.length() != 0 && generatedAddress.length() != 0) {
            Log.e(TAG, "checkFirstAddress: WARNING, addresses don't match: Prefs:" + addressFromPrefs + ", gen:" + generatedAddress);
        }
        return addressFromPrefs.equals(generatedAddress);
    }
}
