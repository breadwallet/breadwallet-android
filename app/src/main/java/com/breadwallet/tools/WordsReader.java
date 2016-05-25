package com.breadwallet.tools;

import android.content.Context;
import android.content.res.AssetManager;
import android.util.Log;

import com.breadwallet.R;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

/**
 * BreadWallet
 * <p/>
 * Created by Mihail Gutan on 9/28/15.
 * Copyright (c) 2016 breadwallet llc <mihail@breadwallet.com>
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

    public static List<String> getWordList(Context context) throws IOException {

        String languageCode = context.getString(R.string.lang);
//        Log.e(TAG, "The language is: " + languageCode);
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

        } finally {
            assert reader != null;
            reader.close();
        }
        if (wordList.size() != WORD_LIST_SIZE)
            throw new IllegalArgumentException("The list should have " + WORD_LIST_SIZE + " items");
        return wordList;
    }

}
