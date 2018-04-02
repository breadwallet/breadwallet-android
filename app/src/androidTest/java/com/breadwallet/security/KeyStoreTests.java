package com.breadwallet.security;

import android.app.Activity;
import android.security.keystore.UserNotAuthenticatedException;
import android.support.test.rule.ActivityTestRule;
import android.support.test.runner.AndroidJUnit4;

import com.breadwallet.presenter.activities.settings.TestActivity;
import com.breadwallet.tools.security.BRKeyStore;
import com.breadwallet.tools.threads.executor.BRExecutor;

import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.math.BigDecimal;

import static com.breadwallet.tools.security.BRKeyStore.SPEND_LIMIT_ALIAS;
import static com.breadwallet.tools.security.BRKeyStore.TOTAL_LIMIT_ALIAS;
import static com.breadwallet.tools.security.BRKeyStore.aliasObjectMap;


/**
 * BreadWallet
 * <p/>
 * Created by Mihail Gutan on <mihail@breadwallet.com> 11/20/16.
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
@RunWith(AndroidJUnit4.class)
public class KeyStoreTests {
    public static final String TAG = KeyStoreTests.class.getName();

    @Rule
    public ActivityTestRule<TestActivity> mActivityRule = new ActivityTestRule<>(TestActivity.class);

    @Test
    public void setGetPhrase() {
        //set get phrase
        byte[] phrase = "axis husband project any sea patch drip tip spirit tide bring belt".getBytes();
        try {
            BRKeyStore.putPhrase(phrase, mActivityRule.getActivity(), 0);
        } catch (UserNotAuthenticatedException e) {
            e.printStackTrace();
            Assert.fail();
        }
        assertFilesExist(BRKeyStore.PHRASE_ALIAS);

        byte[] freshGet = new byte[0];
        try {
            freshGet = BRKeyStore.getPhrase(mActivityRule.getActivity(), 0);
        } catch (UserNotAuthenticatedException e) {
            e.printStackTrace();
        }
        Assert.assertArrayEquals(freshGet, phrase);

        //set get Japaneese phrase
        byte[] japPhrase = "こせき　ぎじにってい　けっこん　せつぞく　うんどう　ふこう　にっすう　こせい　きさま　なまみ　たきび　はかい".getBytes();
        try {
            BRKeyStore.putPhrase(japPhrase, mActivityRule.getActivity(), 0);
        } catch (UserNotAuthenticatedException e) {
            e.printStackTrace();
            Assert.fail();
        }
        assertFilesExist(BRKeyStore.PHRASE_ALIAS);
        byte[] freshJapGet = new byte[0];
        try {
            freshJapGet = BRKeyStore.getPhrase(mActivityRule.getActivity(), 0);
        } catch (UserNotAuthenticatedException e) {
            e.printStackTrace();
        }
        Assert.assertArrayEquals(freshJapGet, japPhrase);

    }

    @Test
    public void setGetCanary() {
        String canary = "canary";
        try {
            BRKeyStore.putCanary(canary, mActivityRule.getActivity(), 0);
        } catch (UserNotAuthenticatedException e) {
            e.printStackTrace();
            Assert.fail();
        }
        assertFilesExist(BRKeyStore.CANARY_ALIAS);
        String freshGet = "";
        try {
            freshGet = BRKeyStore.getCanary(mActivityRule.getActivity(), 0);
        } catch (UserNotAuthenticatedException e) {
            e.printStackTrace();
        }
        Assert.assertEquals(freshGet, canary);
    }

    @Test
    public void setGetMultiple() {
        final String canary = "canary";
        for (int i = 0; i < 100; i++) {
            BRExecutor.getInstance().forLightWeightBackgroundTasks().execute(new Runnable() {
                @Override
                public void run() {
                    try {
                        boolean b = BRKeyStore.putCanary(canary, mActivityRule.getActivity(), 0);
                        Assert.assertTrue(b);
                    } catch (UserNotAuthenticatedException e) {
                        e.printStackTrace();
                        Assert.fail();
                    }
                    try {
                        String b = BRKeyStore.getCanary(mActivityRule.getActivity(), 0);
                        Assert.assertEquals(b, canary);
                    } catch (UserNotAuthenticatedException e) {
                        e.printStackTrace();
                        Assert.fail();
                    }
                }
            });

        }

        for (int i = 0; i < 100; i++) {
            BRExecutor.getInstance().forLightWeightBackgroundTasks().execute(new Runnable() {
                @Override
                public void run() {
                    String freshGet = "";
                    try {
                        freshGet = BRKeyStore.getCanary(mActivityRule.getActivity(), 0);
                    } catch (UserNotAuthenticatedException e) {
                        e.printStackTrace();
                    }
                    Assert.assertEquals(freshGet, canary);
                }
            });

        }
        try {
            Thread.sleep(10000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        assertFilesExist(BRKeyStore.CANARY_ALIAS);

    }

    @Test
    public void setGetAll() {
        byte[] pubKey = "26wZYDdvpmCrYZeUcxgqd1KquN4o6wXwLomBW5SjnwUqG".getBytes();
        BRKeyStore.putMasterPublicKey(pubKey, mActivityRule.getActivity());
        assertFilesExist(BRKeyStore.PUB_KEY_ALIAS);
        byte[] freshGet = BRKeyStore.getMasterPublicKey(mActivityRule.getActivity());
        Assert.assertArrayEquals(pubKey, freshGet);

        pubKey = "26wZYDdvpmCrYZeUcxgqd1KquN4o6wXwLomBW5SjnwUqG".getBytes();
        BRKeyStore.putEthPublicKey(pubKey, mActivityRule.getActivity());
        assertFilesExist(BRKeyStore.ETH_PUBKEY_ALIAS);
        freshGet = BRKeyStore.getEthPublicKey(mActivityRule.getActivity());
        Assert.assertArrayEquals(freshGet, pubKey);

        byte[] authKey = "26wZYDdvpmCrYZeUcxgqd1KquN4o6wXwLomBW5SjnwUqG".getBytes();
        BRKeyStore.putAuthKey(authKey, mActivityRule.getActivity());
        assertFilesExist(BRKeyStore.AUTH_KEY_ALIAS);
        freshGet = BRKeyStore.getAuthKey(mActivityRule.getActivity());
        Assert.assertArrayEquals(freshGet, authKey);

        byte[] token = "26wZYDdvpmCrYZeUcxgqd1KquN4o6wXwLomBW5SjnwUqG".getBytes();
        BRKeyStore.putToken(token, mActivityRule.getActivity());
        assertFilesExist(BRKeyStore.TOKEN_ALIAS);
        freshGet = BRKeyStore.getToken(mActivityRule.getActivity());
        Assert.assertArrayEquals(freshGet, token);

        int time = 1479686841;
        BRKeyStore.putWalletCreationTime(time, mActivityRule.getActivity());
        assertFilesExist(BRKeyStore.WALLET_CREATION_TIME_ALIAS);
        int freshTime = BRKeyStore.getWalletCreationTime(mActivityRule.getActivity());
        Assert.assertEquals(time, freshTime);

        String passCode = "0124";
        BRKeyStore.putPinCode(passCode, mActivityRule.getActivity());
        assertFilesExist(BRKeyStore.PASS_CODE_ALIAS);
        String freshPass = BRKeyStore.getPinCode(mActivityRule.getActivity());
        Assert.assertEquals(passCode, freshPass);
        passCode = "0000";
        BRKeyStore.putPinCode(passCode, mActivityRule.getActivity());
        assertFilesExist(BRKeyStore.PASS_CODE_ALIAS);
        freshPass = BRKeyStore.getPinCode(mActivityRule.getActivity());
        Assert.assertEquals(passCode, freshPass);
        passCode = "9999";
        BRKeyStore.putPinCode(passCode, mActivityRule.getActivity());
        assertFilesExist(BRKeyStore.PASS_CODE_ALIAS);
        freshPass = BRKeyStore.getPinCode(mActivityRule.getActivity());
        Assert.assertEquals(passCode, freshPass);
        passCode = "9876";
        BRKeyStore.putPinCode(passCode, mActivityRule.getActivity());
        assertFilesExist(BRKeyStore.PASS_CODE_ALIAS);
        freshPass = BRKeyStore.getPinCode(mActivityRule.getActivity());
        Assert.assertEquals(passCode, freshPass);

        int failCount = 2;
        BRKeyStore.putFailCount(failCount, mActivityRule.getActivity());
        assertFilesExist(BRKeyStore.FAIL_COUNT_ALIAS);
        int freshFailCount = BRKeyStore.getFailCount(mActivityRule.getActivity());
        Assert.assertEquals(failCount, freshFailCount);

        long failTime = 1479686841;
        BRKeyStore.putFailTimeStamp(failTime, mActivityRule.getActivity());
        assertFilesExist(BRKeyStore.FAIL_TIMESTAMP_ALIAS);
        long freshFailTime = BRKeyStore.getFailTimeStamp(mActivityRule.getActivity());
        Assert.assertEquals(failTime, freshFailTime);

        long passTime = 1479686841;
        BRKeyStore.putLastPinUsedTime(time, mActivityRule.getActivity());
        assertFilesExist(BRKeyStore.PASS_TIME_ALIAS);
        long freshPassTime = BRKeyStore.getLastPinUsedTime(mActivityRule.getActivity());
        Assert.assertEquals(passTime, freshPassTime);


        BigDecimal spendLimitBtc = new BigDecimal(1000);
        BigDecimal spendLimitEth = new BigDecimal(800);
        BigDecimal spendLimitBch = new BigDecimal(600);
        BRKeyStore.putSpendLimit(mActivityRule.getActivity(), spendLimitBtc, "BTC");
        BRKeyStore.putSpendLimit(mActivityRule.getActivity(), spendLimitEth, "ETH");
        BRKeyStore.putSpendLimit(mActivityRule.getActivity(), spendLimitBch, "BCH");
        BigDecimal freshLimitBtc = BRKeyStore.getSpendLimit(mActivityRule.getActivity(), "BTC");
        BigDecimal freshLimitEth = BRKeyStore.getSpendLimit(mActivityRule.getActivity(), "ETH");
        BigDecimal freshLimitBch = BRKeyStore.getSpendLimit(mActivityRule.getActivity(), "BCH");
        Assert.assertNotNull(freshLimitBtc);
        Assert.assertNotNull(freshLimitEth);
        Assert.assertNotNull(freshLimitBch);
        Assert.assertTrue(freshLimitBtc.compareTo(spendLimitBtc) == 0);
        Assert.assertTrue(freshLimitEth.compareTo(spendLimitEth) == 0);
        Assert.assertTrue(freshLimitBch.compareTo(spendLimitBch) == 0);

    }

    @Test
    public void testClearKeyStore() {
        try {
            BRKeyStore.putPhrase("axis husband project any sea patch drip tip spirit tide bring belt".getBytes(), mActivityRule.getActivity(), 0);
        } catch (UserNotAuthenticatedException e) {
            e.printStackTrace();
            Assert.fail();
        }
        try {
            BRKeyStore.putCanary("canary", mActivityRule.getActivity(), 0);
        } catch (UserNotAuthenticatedException e) {
            e.printStackTrace();
            Assert.fail();
        }

        BRKeyStore.putMasterPublicKey("26wZYDdvpmCrYZeUcxgqd1KquN4o6wXwLomBW5SjnwUqG".getBytes(), mActivityRule.getActivity());
        BRKeyStore.putAuthKey("26wZYDdvpmCrYZeUcxgqd1KquN4o6wXwLomBW5SjnwUqG".getBytes(), mActivityRule.getActivity());
        BRKeyStore.putToken("26wZYDdvpmCrYZeUcxgqd1KquN4o6wXwLomBW5SjnwUqG".getBytes(), mActivityRule.getActivity());
        BRKeyStore.putWalletCreationTime(1479686841, mActivityRule.getActivity());
        BRKeyStore.putPinCode("0123", mActivityRule.getActivity());
        BRKeyStore.putFailCount(3, mActivityRule.getActivity());
        BRKeyStore.putFailTimeStamp(1479686841, mActivityRule.getActivity());
        BRKeyStore.putSpendLimit(mActivityRule.getActivity(), new BigDecimal(10000000), "BTC");
        BRKeyStore.putLastPinUsedTime(1479686841, mActivityRule.getActivity());
        BRKeyStore.putTotalLimit(mActivityRule.getActivity(), new BigDecimal(1479686841), "BTC");
        BRKeyStore.putEthPublicKey("26wZYDdvpmCrYZeUcxgqd1KquN4o6wXwLomBW5SjnwUqG".getBytes(), mActivityRule.getActivity());

        for (String a : aliasObjectMap.keySet()) {
            assertFilesExist(a);
        }

        BRKeyStore.resetWalletKeyStore(mActivityRule.getActivity());

        for (String a : aliasObjectMap.keySet()) {
            assertFilesDontExist(a);
        }


        byte[] phrase = "some".getBytes();
        try {
            phrase = BRKeyStore.getPhrase(mActivityRule.getActivity(), 0);
        } catch (UserNotAuthenticatedException e) {
            e.printStackTrace();
        }

        String canary = "some";

        try {
            canary = BRKeyStore.getCanary(mActivityRule.getActivity(), 0);
        } catch (UserNotAuthenticatedException e) {
            e.printStackTrace();
        }

        Assert.assertNull(phrase);
        Assert.assertEquals(null, canary);
        Assert.assertEquals(null, BRKeyStore.getMasterPublicKey(mActivityRule.getActivity()));
        Assert.assertEquals(null, BRKeyStore.getEthPublicKey(mActivityRule.getActivity()));
        Assert.assertEquals(null, BRKeyStore.getAuthKey(mActivityRule.getActivity()));
        Assert.assertEquals(null, BRKeyStore.getToken(mActivityRule.getActivity()));
        Assert.assertEquals(0, BRKeyStore.getWalletCreationTime(mActivityRule.getActivity()));
        Assert.assertEquals("", BRKeyStore.getPinCode(mActivityRule.getActivity()));
        Assert.assertEquals(0, BRKeyStore.getFailCount(mActivityRule.getActivity()));
        Assert.assertEquals(0, BRKeyStore.getFailTimeStamp(mActivityRule.getActivity()));
        Assert.assertEquals(0, BRKeyStore.getLastPinUsedTime(mActivityRule.getActivity()));

    }

    @Test
    public void testKeyStoreAuthTime() {
        Assert.assertEquals(BRKeyStore.AUTH_DURATION_SEC, 300);
    }

    @Test
    public void testKeyStoreAliasMap() {
        Assert.assertNotNull(aliasObjectMap);
        Assert.assertEquals(aliasObjectMap.size(), 13);
    }

    public void assertFilesExist(String alias) {
        Activity app = mActivityRule.getActivity();
        if (alias.equalsIgnoreCase(SPEND_LIMIT_ALIAS)) return;
        if (alias.equalsIgnoreCase(TOTAL_LIMIT_ALIAS)) return;
        byte[] data = BRKeyStore.retrieveEncryptedData(app, alias);
        Assert.assertNotNull(data);
        Assert.assertNotEquals(data.length, 0);

        byte[] iv = BRKeyStore.retrieveEncryptedData(app, aliasObjectMap.get(alias).ivFileName);
        Assert.assertNotNull(iv);
        Assert.assertNotEquals(iv.length, 0);

    }

    public void assertFilesDontExist(String alias) {
        Activity app = mActivityRule.getActivity();
        byte[] data = BRKeyStore.retrieveEncryptedData(app, alias);
        Assert.assertNull(data);

        byte[] iv = BRKeyStore.retrieveEncryptedData(app, aliasObjectMap.get(alias).ivFileName);
        Assert.assertNull(iv);
    }

}
