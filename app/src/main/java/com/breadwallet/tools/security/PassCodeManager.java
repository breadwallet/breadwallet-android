package com.breadwallet.tools.security;

/**
 * BreadWallet
 * <p/>
 * Created by Mihail on 8/20/15.
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
public class PassCodeManager {
    public static final String TAG = "PassCodeManager";
    private static char[] PASS_CODE = {'1', '2', '3', '4'};

    public static boolean checkAuth(String pass) {
        if (pass.length() == PASS_CODE.length) {
            int pos = 0;
            for (char c : PASS_CODE) {
                if (c != pass.charAt(pos++)) return false;
            }
        } else {
            return false;
        }
        return true;
    }

    public static void setPassCode(String pass) {
        PASS_CODE = new char[pass.length()];
        for (int i = 0; i < pass.length(); i++) {
            PASS_CODE[i] = pass.charAt(i);
        }
    }
}
