package com.breadwallet.tools.util;

public class StringUtil {

    public static boolean isNullOrEmpty(String value){
        if(value==null || value.isEmpty()) return true;
        return false;
    }
}
