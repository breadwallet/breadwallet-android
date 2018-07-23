package com.breadwallet.protocols.messageexchange;

import android.support.test.runner.AndroidJUnit4;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * BreadWallet
 * <p/>
 * Created by Shivangi Gandhi on <shivangi@brd.com> 07/22/18.
 * Copyright (c) 2018 breadwallet LLC
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
 *
 * Tests related to the message exchange.
 */
@RunWith(AndroidJUnit4.class)
public class MessageExchangeTest {
    /**
     * Instructions:
     *  1. Recover wallet using the following test paper key:
     *     inherit, seed, spray, artefact, coast, antique, spirit, turkey, seven, shrimp, menu, canal
     *  2. Run tests.
     */

    String[] ACCOUNT_RESPONSE_TEST_CURRENCY_CODES = {"BTC", "BCH", "ETH", "BRD", "EOS", "ROGUE"};

    String[] ACCOUNT_RESPONSE_TEST_EXPECTED_ADDRESS = {
            "1FKqWmicLxDz6zbmAA2SanH17PsoXnMYF5", // BTC - subject to change if a transaction is made in this wallet.
            "1FKqWmicLxDz6zbmAA2SanH17PsoXnMYF5", // BCH - subject to change if a transaction is made in this wallet.
            "0x43dd8d6C36f2F91f21b7605A2FcD5d63FFA9f2FA", // ETH
            "0x43dd8d6C36f2F91f21b7605A2FcD5d63FFA9f2FA",  // BRD - ERC20 token that has been added to the wallet.
            "0x43dd8d6C36f2F91f21b7605A2FcD5d63FFA9f2FA", // EOS - ERC20 token that has not been added to the wallet.
            "" // ROGUE - non-existent currency
    };

    Protos.Status[] ACCOUNT_RESPONSE_TEST_EXPECTED_EXPECTED_STATUS = {
            Protos.Status.ACCEPTED,
            Protos.Status.ACCEPTED,
            Protos.Status.ACCEPTED,
            Protos.Status.ACCEPTED,
            Protos.Status.ACCEPTED,
            Protos.Status.REJECTED
    };

    /**
     * Test that AccountResponse is generated correctly for the specified currency code.
     */
    @Test
    public void testAccountResponse() {
        try {
            for (int i = ACCOUNT_RESPONSE_TEST_CURRENCY_CODES.length-1; i >= 0; i--) {
                // Generate a test request for the specified currency code.
                byte[] requestBytes = Protos.AccountRequest.newBuilder()
                        .setScope(ACCOUNT_RESPONSE_TEST_CURRENCY_CODES[i]).build().toByteArray();

                // Generate the response.
                byte[] responseBytes = PwbMaster.generateAccountResponse(requestBytes).toByteArray();
                Protos.AccountResponse response = Protos.AccountResponse.parseFrom(responseBytes);

                // Verify that the response is as expected.
                Assert.assertEquals(ACCOUNT_RESPONSE_TEST_CURRENCY_CODES[i], response.getScope());
                Assert.assertEquals(ACCOUNT_RESPONSE_TEST_EXPECTED_ADDRESS[i], response.getAddress());
                Assert.assertEquals(ACCOUNT_RESPONSE_TEST_EXPECTED_EXPECTED_STATUS[i], response.getStatus());
            }
        } catch (Exception e) {
            Assert.fail(e.getMessage());
        }
    }
}
