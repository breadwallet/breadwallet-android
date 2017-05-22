package com.breadwallet.tools.util;

import android.app.Activity;
import android.content.Context;
import android.content.res.AssetManager;

import com.google.firebase.crash.FirebaseCrash;

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

public class WordsReader {

    private static final String TAG = WordsReader.class.getName();
    private static final int WORD_LIST_SIZE = 2048;

    private static final String BOM = "\uFEFF";

    public static List<String> getWordList(Context context, String languageCode) throws IOException {

        String fileName = "words/" + languageCode + "-BIP39Words.txt";
        List<String> wordList = new ArrayList<>();
        BufferedReader reader = null;
        try {
            AssetManager assetManager = context.getResources().getAssets();
            InputStream inputStream = assetManager.open(fileName);
            reader = new BufferedReader(new InputStreamReader(inputStream));
            String line;
            while ((line = reader.readLine()) != null) {
                wordList.add(line);
            }
        } catch (Exception ex) {
            ex.printStackTrace();
            FirebaseCrash.report(ex);
        } finally {
            assert reader != null;
            reader.close();
        }
        if (wordList.size() != WORD_LIST_SIZE) {
            RuntimeException ex = new IllegalArgumentException("The list should have " + WORD_LIST_SIZE + " items");
            FirebaseCrash.report(ex);
            throw ex;
        }
        return wordList;
    }

    public static String getLang(Context context, String incorrect) {

        String[] langs = {"en", "es", "fr", "ja", "zh"};
        String lang = null;

        for (String l : langs) {
            String fileName = "words/" + l + "-BIP39Words.txt";
            List<String> wordList = new ArrayList<>();
            BufferedReader reader = null;
            try {
                AssetManager assetManager = context.getResources().getAssets();
                InputStream inputStream = assetManager.open(fileName);
                reader = new BufferedReader(new InputStreamReader(inputStream));
                String line;

                while ((line = reader.readLine()) != null) {
                    String word = line.trim().toLowerCase().replace(BOM, "");
                    wordList.add(word);
                }
            } catch (Exception ex) {
                ex.printStackTrace();

            } finally {
                assert reader != null;
                try {
                    reader.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            if (wordList.size() != WORD_LIST_SIZE) {
                RuntimeException ex = new IllegalArgumentException("The list should have " + WORD_LIST_SIZE + " items");
                FirebaseCrash.report(ex);
                throw ex;
            }
            if (wordList.contains(incorrect)) {
                lang = l;
            }

        }
        return lang;
    }

    public static String[] cleanWordList(String[] words) {
        if (words == null || words.length != 2048) return null;
        int length = words.length;
        String[] result = new String[length];

        for (int i = 0; i < length; i++) {
            result[i] = Normalizer.normalize(words[i], Normalizer.Form.NFKD).replace("　", "").replace(" ", "").replace(BOM, "");
        }
        return result;
    }

    public static String cleanPhrase(Activity activity, String phraseToCheck) {
        String phrase = Normalizer.normalize(phraseToCheck, Normalizer.Form.NFKD).replace("　", " ").replace("\n", " ").trim().replaceAll(" +", " ");

        String[] phraseWords = phrase.split(" ");

        String firstWord = phraseWords[0];

        List<String> allWords = getAllWordLists(activity);

        String lang = getLang(activity, firstWord);
        if (lang == null) {
            for (String word : phraseWords) {
                if (word.length() < 1 || word.charAt(0) < 0x3000 || allWords.contains(word))
                    continue;
                int length = word.length();
                for (int i = 0; i < length; i++) {
                    for (int j = (length - i > 8) ? 8 : length - i; j > 0; j--) {
                        String tmp = word.substring(i, i + j);
                        if (!allWords.contains(tmp)) continue;
                        phrase = phrase.replace(tmp, " " + tmp + " ");
                        while (phrase.contains("  ")) {
                            phrase = phrase.replace("  ", " ");
                        }
                        while (phrase.startsWith(" ")) {
                            phrase = phrase.substring(1, phrase.length());
                        }
                        while (phrase.endsWith(" ")) {
                            phrase = phrase.substring(0, phrase.length() - 1);
                        }
                        i += j - 1;
                        break;
                    }
                }
            }
        }
        return phrase;
    }

    public static List<String> getAllWordLists(Activity context) {
        String[] langs = {"en", "es", "fr", "ja", "zh"};
        List<String> result = new ArrayList<>();

        for (String l : langs) {
            String fileName = "words/" + l + "-BIP39Words.txt";
            List<String> wordList = new ArrayList<>();
            BufferedReader reader = null;
            try {
                AssetManager assetManager = context.getResources().getAssets();
                InputStream inputStream = assetManager.open(fileName);
                reader = new BufferedReader(new InputStreamReader(inputStream));
                String line;

                while ((line = reader.readLine()) != null) {
                    String word = line.trim().toLowerCase().replace(BOM, "");
                    wordList.add(word);
                }
            } catch (Exception ex) {
                ex.printStackTrace();

            } finally {
                assert reader != null;
                try {
                    reader.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            if (wordList.size() != WORD_LIST_SIZE) {
                RuntimeException ex = new IllegalArgumentException("The list should have " + WORD_LIST_SIZE + " items");
                FirebaseCrash.report(ex);
                throw ex;
            }
            result.addAll(wordList);

        }
        return result;
    }
}
