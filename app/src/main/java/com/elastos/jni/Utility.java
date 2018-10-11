package com.elastos.jni;

/**
 * @Author xidaokun
 * @Date 18-9-26
 * @Exp 说明
 */
public class Utility {

    static {
        System.loadLibrary("utility");
    }

    public static String generateMnemonic(String language, String path){
        return nativeGenerateMnemonic(language, path);
    }

    public static String generatePrivateKey(){
        return nativeGeneratePrivateKey();
    }

    public static String getPublicKey(String privateKey){
        return nativeGetPublicKey(privateKey);
    }

    public static String getAddress(String publicKey){
        return nativeGetAddress(publicKey);
    }

    public static String getPrivateKey(String mmemonic, String language, String path){
        return nativeGetPrivateKey(mmemonic, language, path);
    }

    public static String generateRawTransaction(String transaction){
        return nativeGenerateRawTransaction(transaction);
    }


    private static native String nativeGenerateMnemonic(String language, String path);

    private static native String nativeGeneratePrivateKey();

    private static native String nativeGetPublicKey(String privateKey);

    private static native String nativeGetAddress(String publicKey);

    private static native String nativeGetPrivateKey(String mmemonic, String language, String path);

    private static native String nativeGenerateRawTransaction(String transaction);
}
