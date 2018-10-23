package com.elastos.jni;

import android.content.Context;

import com.breadwallet.BreadApp;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Locale;

/**
 * @Author xidaokun
 * @Date 18-9-26
 * @Exp 说明
 */
public class Utility {

    public static String[] LANGS = {"en", "es", "fr", "ja", "zh"};

    static {
        System.loadLibrary("utility");
    }

    public static void initLanguage(Context context){
        if(BreadApp.mElaLanguage==null || BreadApp.mElaWords==null){
            String languageCode = Locale.getDefault().getLanguage();
            boolean exists = false;
            for (String s : LANGS) if (s.equalsIgnoreCase(languageCode)) exists = true;
            if (!exists) {
                languageCode = "en"; //use en if not a supported lang
            }
            BreadApp.mElaLanguage = getLanguage(languageCode);
            BreadApp.mElaWords = getWords(context, languageCode +"-BIP39Words.txt");
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


    public static String getSinglePrivateKey(String mnemonic){
        return getSinglePrivateKey(BreadApp.mElaLanguage, mnemonic, BreadApp.mElaWords, "");
    }

    public static String getSinglePublicKey(String mnemonic){
        return getSinglePublicKey(BreadApp.mElaLanguage, mnemonic, BreadApp.mElaWords, "");
    }

    public static String generateMnemonic(String language, String words){
        return nativeGenerateMnemonic(language, words);
    }

    private static String getSinglePrivateKey(String jlanguage, String jmnemonic, String jwords, String jpassword){
        return nativeGetSinglePrivateKey(jlanguage, jmnemonic, jwords, jpassword);
    }

    private static String getSinglePublicKey(String jlanguage, String jmnemonic, String jwords, String jpassword){
        return nativeGetSinglePublicKey(jlanguage, jmnemonic, jwords, jpassword);
    }

    public static String getAddress(String jpublickey){
        return nativeGetAddress(jpublickey);
    }

    public static String generateRawTransaction(String transaction){
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
