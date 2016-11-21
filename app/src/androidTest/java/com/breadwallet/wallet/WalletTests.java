package com.breadwallet.wallet;

import com.breadwallet.presenter.entities.RequestObject;
import com.breadwallet.tools.security.RequestHandler;


import org.junit.Test;

import java.math.BigDecimal;
import java.security.InvalidAlgorithmParameterException;

import static org.junit.Assert.assertEquals;


/**
 * BreadWallet
 * <p/>
 * Created by Mihail Gutan on 4/29/16.
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
public class WalletTests {
    public static final String TAG = WalletTests.class.getName();

    static {
        System.loadLibrary("core");
    }

    @Test
    public void paymentRequestTest() throws InvalidAlgorithmParameterException {

        RequestObject obj = RequestHandler.getRequestFromString("n2eMqTT929pb1RDNuqEnxdaLau1rxy3efi");
        assertEquals("n2eMqTT929pb1RDNuqEnxdaLau1rxy3efi", obj.address);

//        r = [BRPaymentRequest requestWithString:@"1BTCorgHwCg6u2YSAWKgS17qUad6kHmtQ"];
//        XCTAssertFalse(r.isValid);
//        XCTAssertEqualObjects(@"bitcoin:1BTCorgHwCg6u2YSAWKgS17qUad6kHmtQ", r.string,
//        @"[BRPaymentRequest requestWithString:]");

        obj = RequestHandler.getRequestFromString("bitcoin:n2eMqTT929pb1RDNuqEnxdaLau1rxy3efi");
        assertEquals(obj.address, "n2eMqTT929pb1RDNuqEnxdaLau1rxy3efi");

        obj = RequestHandler.getRequestFromString("bitcoin:n2eMqTT929pb1RDNuqEnxdaLau1rxy3efi?amount=1");
        assertEquals(obj.address, "n2eMqTT929pb1RDNuqEnxdaLau1rxy3efi");
        BigDecimal bigDecimal = new BigDecimal(obj.amount);
        long amountAsLong = bigDecimal.longValue();
        assertEquals(String.valueOf(amountAsLong), "100000000");

        obj = RequestHandler.getRequestFromString("bitcoin:n2eMqTT929pb1RDNuqEnxdaLau1rxy3efi?amount=0.00000001");
        assertEquals(obj.address, "n2eMqTT929pb1RDNuqEnxdaLau1rxy3efi");
        bigDecimal = new BigDecimal(obj.amount);
        amountAsLong = bigDecimal.longValue();
        assertEquals(String.valueOf(amountAsLong), "1");

        obj = RequestHandler.getRequestFromString("bitcoin:n2eMqTT929pb1RDNuqEnxdaLau1rxy3efi?amount=21000000");
        assertEquals(obj.address, "n2eMqTT929pb1RDNuqEnxdaLau1rxy3efi");
        bigDecimal = new BigDecimal(obj.amount);
        amountAsLong = bigDecimal.longValue();
        assertEquals(String.valueOf(amountAsLong), "2100000000000000");

        // test for floating point rounding issues, these values cannot be exactly represented with an IEEE 754 double
        obj = RequestHandler.getRequestFromString("bitcoin:n2eMqTT929pb1RDNuqEnxdaLau1rxy3efi?amount=20999999.99999999");
        assertEquals(obj.address, "n2eMqTT929pb1RDNuqEnxdaLau1rxy3efi");
        bigDecimal = new BigDecimal(obj.amount);
        amountAsLong = bigDecimal.longValue();
        assertEquals(String.valueOf(amountAsLong), "2099999999999999");

        obj = RequestHandler.getRequestFromString("bitcoin:n2eMqTT929pb1RDNuqEnxdaLau1rxy3efi?amount=20999999.99999995");
        assertEquals(obj.address, "n2eMqTT929pb1RDNuqEnxdaLau1rxy3efi");
        bigDecimal = new BigDecimal(obj.amount);
        amountAsLong = bigDecimal.longValue();
        assertEquals(String.valueOf(amountAsLong), "2099999999999995");

        obj = RequestHandler.getRequestFromString("bitcoin:n2eMqTT929pb1RDNuqEnxdaLau1rxy3efi?amount=0.07433");
        assertEquals(obj.address, "n2eMqTT929pb1RDNuqEnxdaLau1rxy3efi");
        bigDecimal = new BigDecimal(obj.amount);
        amountAsLong = bigDecimal.longValue();
        assertEquals(String.valueOf(amountAsLong), "7433000");

        // invalid amount string
        obj = RequestHandler.getRequestFromString("bitcoin:n2eMqTT929pb1RDNuqEnxdaLau1rxy3efi?amount=foobar");
        assertEquals(obj.address, "n2eMqTT929pb1RDNuqEnxdaLau1rxy3efi");
        assertEquals(obj.amount, null);

        // test correct encoding of '&' in argument value
        obj = RequestHandler.getRequestFromString("bitcoin:n2eMqTT929pb1RDNuqEnxdaLau1rxy3efi?label=foo%26bar");
        assertEquals(obj.address, "n2eMqTT929pb1RDNuqEnxdaLau1rxy3efi");
        assertEquals(obj.label, "foo");

        // test handling of ' ' in label or message
        obj = RequestHandler.getRequestFromString("bitcoin:n2eMqTT929pb1RDNuqEnxdaLau1rxy3efi?label=foo bar&message=bar foo");
        assertEquals(obj.address, "n2eMqTT929pb1RDNuqEnxdaLau1rxy3efi");
        assertEquals(obj.label, "foo bar");
        assertEquals(obj.message, "bar foo");

        // test bip73
        obj = RequestHandler.getRequestFromString("bitcoin:n2eMqTT929pb1RDNuqEnxdaLau1rxy3efi?r=https://foobar.com");
        assertEquals(obj.address, "n2eMqTT929pb1RDNuqEnxdaLau1rxy3efi");
        assertEquals(obj.r, "https://foobar.com");

        obj = RequestHandler.getRequestFromString("bitcoin:?r=https://foobar.com");
        assertEquals(obj.address, null);
        assertEquals(obj.r, "https://foobar.com");
    }

    @Test
    public void currencyManagerTests() {

//        String amount;
//        String result;

//        MainActivity app = MainActivity.app;
//        if (app != null) {
//
//            amount = "232432.42234";
//            result = CurrencyManager.getInstance(app).getFormattedCurrencyString("USD", amount);
//            assertEquals("$232432.42234", result);
//        }
    }
}
