package com.breadwallet.tools.util;

/**
 * BreadWallet
 * <p/>
 * Created by Mihail Gutan on <mihail@breadwallet.com> 7/17/17.
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
public class TrustedNode {

    public static  String getNodeHost(String input) {
        if (input.contains(":")) {
            return input.split(":")[0];
        }
        return input;
    }

    public static  int getNodePort(String input) {
        int port = 0;
        if (input.contains(":")) {
            try {
                port = Integer.parseInt(input.split(":")[1]);
            } catch (Exception e) {

            }
        }
        return port;
    }

    public static  boolean isValid(String input) {
        try {
            if (input == null || input.length() == 0) return false;
            for (int i = 0; i < input.length(); i++) {
                char c = input.charAt(i);
                if (!Character.isDigit(c) && c != '.' && c != ':') return false;
            }
            String host;
            if (input.contains(":")) {
                String[] pieces = input.split(":");
                if (pieces.length > 2) return false;
                host = pieces[0];
                int port = Integer.parseInt(pieces[1]); //just try to see if it's a number
            } else {
                host = input;
            }
            String[] nums = host.split("\\.");
            if (nums.length != 4) return false;
            for (int i = 0; i < nums.length; i++) {
                int slice = Integer.parseInt(nums[i]);
                if (slice < 0 || slice > 255) return false;
            }
        } catch (Exception e) {
            return false;
        }

        return true;
    }
}
