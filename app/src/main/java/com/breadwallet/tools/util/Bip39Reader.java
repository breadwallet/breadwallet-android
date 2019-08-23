package com.breadwallet.tools.util;

import android.content.Context;
import android.content.res.AssetManager;
import android.util.Log;

import com.breadwallet.tools.manager.BRReportsManager;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.List;

/**
 * BreadWallet
 * <p/>
 * Created by Mihail Gutan <mihail@breadwallet.com> on 9/28/15.
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

public class Bip39Reader {
    private static final String TAG = Bip39Reader.class.getSimpleName();
    public static final int WORD_LIST_SIZE = 2048;

    private static final String FILE_PREFIX = "words/";
    private static final String FILE_SUFFIX = "-BIP39Words.txt";
    // UTF-8 BOM is a sequence of bytes (EF BB BF) that allows the reader to identify a file as being encoded in UTF-8.
    // Use FEFF because this is the Unicode char represented by EF BB BF.
    private static final String UTF8_BOM = "\uFEFF";


    public enum SupportedLanguage {
        EN("en"),
        ES("es"),
        FR("fr"),
        IT("it"),
        JA("ja"),
        KO("ko"),
        ZH_HANS("zh-Hans"),
        ZH_HANT("zh-Hant");

        private final String mLanguage;

        SupportedLanguage(String language) {
            mLanguage = language;
        }

        public String toString() {
            return mLanguage;
        }

        public static SupportedLanguage getSupportedLanguage(String targetLanguage) {
            for (SupportedLanguage supportedLanguage : SupportedLanguage.values()) {
                if (supportedLanguage.toString().equals(targetLanguage)) {
                    return supportedLanguage;
                }
            }

            // If the target language is not supported, use the default.
            return SupportedLanguage.EN;
        }
    }

    private Bip39Reader() {
    }

    public static List<String> getBip39Words(Context context, String targetLanguage) {
        if (targetLanguage == null) {
            // Return all words for all languages.
            return getAllBip39Words(context);
        }

        return getBip39WordsByLanguage(context, SupportedLanguage.getSupportedLanguage(targetLanguage));
    }

    static List<String> getAllBip39Words(Context context) {
        List<String> words = new ArrayList<>();
        for (SupportedLanguage supportedLanguage : SupportedLanguage.values()) {
            words.addAll(getBip39WordsByLanguage(context, supportedLanguage));
        }
        return words;
    }

    private static List<String> getBip39WordsByLanguage(Context context, SupportedLanguage language) {
        String fileName = FILE_PREFIX + language.toString() + FILE_SUFFIX;
        List<String> wordList = new ArrayList<>();
        BufferedReader reader = null;
        try {
            AssetManager assetManager = context.getResources().getAssets();
            InputStream inputStream = assetManager.open(fileName);
            reader = new BufferedReader(new InputStreamReader(inputStream));
            String line;

            while ((line = reader.readLine()) != null) {
                wordList.add(cleanWord(line));
            }
        } catch (Exception ex) {
            Log.e(TAG, "getList: ", ex);
        } finally {
            try {
                if (reader != null) {
                    reader.close();
                }
            } catch (IOException e) {
                Log.e(TAG, "getList: ", e);
            }
        }
        if (wordList.size() % WORD_LIST_SIZE != 0) {
            BRReportsManager.reportBug(new IllegalArgumentException("The list size should divide by " + WORD_LIST_SIZE), true);
        }
        return new ArrayList<>(wordList);
    }

    public static List<String> detectWords(Context context, String phrase) {
        if (Utils.isNullOrEmpty(phrase)) {
            return null;
        }
        String cleanPhrase = cleanPhrase(phrase);
        String firstWord = cleanPhrase.split(" ")[0];

        for (SupportedLanguage supportedLanguage : SupportedLanguage.values()) {
            List<String> words = getBip39WordsByLanguage(context, supportedLanguage);
            if (words.contains(firstWord)) {
                return words;
            }
        }
        return null;
    }

    public static Boolean isWordValid(Context context, String word) {
        return Bip39Reader.getBip39Words(context, null).contains(Bip39Reader.cleanWord(word));
    }

    public static String cleanWord(String word) {
        return Normalizer.normalize(word.trim().replace(UTF8_BOM, "").replace("　", "")
                .replace(" ", ""), Normalizer.Form.NFKD);
    }

    private static String cleanPhrase(String phraseToCheck) {
        return Normalizer.normalize(phraseToCheck.replace("　", " ")
                .replace("\n", " ").trim().replaceAll(" +", " "), Normalizer.Form.NFKD);
    }
}
