package com.elastos.jni;

import android.content.Context;

import com.breadwallet.BreadApp;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Locale;

/**
 * BreadWallet
 * <p/>
 * Created by Mihail Gutan <mihail@breadwallet.com> on 8/4/15.
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
public class Utility {

    public static String[] LANGS = {"en", "es", "fr", "ja", "zh"};

    private static Utility mInstance;

    private static String mLanguage = null;

    private static String mWords = null;

    static {
        System.loadLibrary("utility");
    }

    private Utility(Context context){
        initLanguage(context);
    }

    public static Utility getInstance(Context context){
        if(mInstance == null){
            mInstance = new Utility(context);
        }
        initLanguage(context);
        return mInstance;
    }

    public static void initLanguage(Context context){
        if(mLanguage==null || mWords==null){
            String languageCode = Locale.getDefault().getLanguage();
            boolean exists = false;
            for (String s : LANGS) if (s.equalsIgnoreCase(languageCode)) exists = true;
            if (!exists) {
                languageCode = "en"; //use en if not a supported lang
            }
            mLanguage = getLanguage(languageCode);
            mWords = getWords(context, languageCode +"-BIP39Words.txt");
        }
    }

    private static String getLanguage(String languageCode){
        if(languageCode.equalsIgnoreCase("en")) return "english";
        if(languageCode.equalsIgnoreCase("es")) return "spanish";
        if(languageCode.equalsIgnoreCase("fr")) return "french";
        if(languageCode.equalsIgnoreCase("ja")) return "japanese";
        if(languageCode.equalsIgnoreCase("zh")) return "chinese";
        return "english";
    }


    public String getSinglePrivateKey(String mnemonic){
        return getSinglePrivateKey(mLanguage, mnemonic, mWords, "");
    }

    public String getSinglePublicKey(String mnemonic){
        return getSinglePublicKey(mLanguage, mnemonic, mWords, "");
    }

    public String generateMnemonic(String language, String words){
        return nativeGenerateMnemonic(language, words);
    }

    private String getSinglePrivateKey(String jlanguage, String jmnemonic, String jwords, String jpassword){
        return nativeGetSinglePrivateKey(jlanguage, jmnemonic, jwords, jpassword);
    }

    private String getSinglePublicKey(String jlanguage, String jmnemonic, String jwords, String jpassword){
        return nativeGetSinglePublicKey(jlanguage, jmnemonic, jwords, jpassword);
    }

    public  String getAddress(String jpublickey){
        return nativeGetAddress(jpublickey);
    }

    public  String generateRawTransaction(String transaction){
        return nativeGenerateRawTransaction(transaction);
    }

    private static native String nativeGenerateMnemonic(String language, String words);

    private static native String nativeGetSinglePrivateKey(String jlanguage, String jmnemonic, String jwords, String jpassword);

    private static native String nativeGetSinglePublicKey(String jlanguage, String jmnemonic, String jwords, String jpassword);

    private static native String nativeGetAddress(String jpublickey);

    private static native String nativeGenerateRawTransaction(String jtransaction);

    public static String getWords(Context context, String name) {
        if (name == null) return null;
        StringBuffer content = new StringBuffer();
        InputStream inputStream = null;
        try {
            inputStream = context.getResources().getAssets().open("words/"+name);
        } catch (IOException e) {
            e.printStackTrace();
        }

        try {
            if (inputStream != null) {
                InputStreamReader inputReader = new InputStreamReader(inputStream);
                BufferedReader buffreader = new BufferedReader(inputReader);
                String line;
                while ((line = buffreader.readLine()) != null)
                    content.append(line).append("\n");
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            try {
                if (inputStream != null) inputStream.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        return content.toString();
    }
}
