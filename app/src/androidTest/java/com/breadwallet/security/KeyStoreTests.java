package com.breadwallet.security;

import android.security.keystore.UserNotAuthenticatedException;
import android.support.test.rule.ActivityTestRule;
import android.support.test.runner.AndroidJUnit4;

import com.breadwallet.exceptions.BRKeystoreErrorException;
import com.breadwallet.presenter.activities.MainActivity;
import com.breadwallet.tools.security.KeyStoreManager;

import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.File;

import static com.breadwallet.tools.security.KeyStoreManager.aliasObjectMap;


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
    public ActivityTestRule<MainActivity> mActivityRule = new ActivityTestRule<>(MainActivity.class);

    @Test
    public void setGetPhrase() {
        //set get phrase
        byte[] phrase = "axis husband project any sea patch drip tip spirit tide bring belt".getBytes();
        try {
            KeyStoreManager.putPhrase(phrase, mActivityRule.getActivity(), 0);
        } catch (UserNotAuthenticatedException e) {
            e.printStackTrace();
        }
        assertFilesExist(KeyStoreManager.PHRASE_ALIAS);

        byte[] freshGet = new byte[0];
        try {
            freshGet = KeyStoreManager.getPhrase(mActivityRule.getActivity(), 0);
        } catch (UserNotAuthenticatedException e) {
            e.printStackTrace();
        }
        Assert.assertArrayEquals(freshGet, phrase);

        //set get Japaneese phrase
        byte[] japPhrase = "こせき　ぎじにってい　けっこん　せつぞく　うんどう　ふこう　にっすう　こせい　きさま　なまみ　たきび　はかい".getBytes();
        try {
            KeyStoreManager.putPhrase(japPhrase, mActivityRule.getActivity(), 0);
        } catch (UserNotAuthenticatedException e) {
            e.printStackTrace();
            Assert.fail();
        }
        assertFilesExist(KeyStoreManager.PHRASE_ALIAS);
        byte[] freshJapGet = new byte[0];
        try {
            freshJapGet = KeyStoreManager.getPhrase(mActivityRule.getActivity(), 0);
        } catch (UserNotAuthenticatedException e) {
            e.printStackTrace();
        }
        Assert.assertArrayEquals(freshJapGet, japPhrase);

    }

    @Test
    public void setGetCanary() {
        String canary = "canary";
        try {
            KeyStoreManager.putCanary(canary, mActivityRule.getActivity(), 0);
        } catch (UserNotAuthenticatedException e) {
            e.printStackTrace();
            Assert.fail();
        }
        assertFilesExist(KeyStoreManager.CANARY_ALIAS);
        String freshGet = "";
        try {
            freshGet = KeyStoreManager.getCanary(mActivityRule.getActivity(), 0);
        } catch (UserNotAuthenticatedException e) {
            e.printStackTrace();
        }
        Assert.assertEquals(freshGet, canary);
    }

    @Test
    public void setGetMasterPubKey() {
        byte[] pubKey = "26wZYDdvpmCrYZeUcxgqd1KquN4o6wXwLomBW5SjnwUqG".getBytes();
        KeyStoreManager.putMasterPublicKey(pubKey, mActivityRule.getActivity());
        assertFilesExist(KeyStoreManager.PUB_KEY_ALIAS);
        byte[] freshGet;
        freshGet = KeyStoreManager.getMasterPublicKey(mActivityRule.getActivity());
        Assert.assertArrayEquals(freshGet, freshGet);
    }


    @Test
    public void setGetAuthKey() {
        byte[] authKey = "26wZYDdvpmCrYZeUcxgqd1KquN4o6wXwLomBW5SjnwUqG".getBytes();
        KeyStoreManager.putAuthKey(authKey, mActivityRule.getActivity());
        assertFilesExist(KeyStoreManager.AUTH_KEY_ALIAS);
        byte[] freshGet;
        freshGet = KeyStoreManager.getAuthKey(mActivityRule.getActivity());
        Assert.assertArrayEquals(freshGet, freshGet);
    }

    @Test
    public void setGetToken() {
        byte[] token = "26wZYDdvpmCrYZeUcxgqd1KquN4o6wXwLomBW5SjnwUqG".getBytes();
        KeyStoreManager.putToken(token, mActivityRule.getActivity());
        assertFilesExist(KeyStoreManager.TOKEN_ALIAS);
        byte[] freshGet;
        freshGet = KeyStoreManager.getToken(mActivityRule.getActivity());
        Assert.assertArrayEquals(freshGet, freshGet);
    }

    @Test
    public void setGetWalletCreationTime() {
        int time = 1479686841;
        KeyStoreManager.putWalletCreationTime(time, mActivityRule.getActivity());
        assertFilesExist(KeyStoreManager.WALLET_CREATION_TIME_ALIAS);
        int freshGet;
        freshGet = KeyStoreManager.getWalletCreationTime(mActivityRule.getActivity());
        Assert.assertEquals(freshGet, freshGet);
    }

    @Test
    public void setGetPassCode() {
        String passCode = "0124";
        KeyStoreManager.putPassCode(passCode, mActivityRule.getActivity());
        assertFilesExist(KeyStoreManager.PASS_CODE_ALIAS);
        String freshGet;
        freshGet = KeyStoreManager.getPassCode(mActivityRule.getActivity());
        Assert.assertEquals(freshGet, freshGet);

        passCode = "0000";
        KeyStoreManager.putPassCode(passCode, mActivityRule.getActivity());
        assertFilesExist(KeyStoreManager.PASS_CODE_ALIAS);
        freshGet = KeyStoreManager.getPassCode(mActivityRule.getActivity());
        Assert.assertEquals(freshGet, freshGet);

        passCode = "9999";
        KeyStoreManager.putPassCode(passCode, mActivityRule.getActivity());
        assertFilesExist(KeyStoreManager.PASS_CODE_ALIAS);
        freshGet = KeyStoreManager.getPassCode(mActivityRule.getActivity());
        Assert.assertEquals(freshGet, freshGet);

        passCode = "9876";
        KeyStoreManager.putPassCode(passCode, mActivityRule.getActivity());
        assertFilesExist(KeyStoreManager.PASS_CODE_ALIAS);
        freshGet = KeyStoreManager.getPassCode(mActivityRule.getActivity());
        Assert.assertEquals(freshGet, freshGet);
    }

    @Test
    public void setGetFailCount() {
        int failCount = 2;
        KeyStoreManager.putFailCount(failCount, mActivityRule.getActivity());
        assertFilesExist(KeyStoreManager.FAIL_COUNT_ALIAS);
        int freshGet;
        freshGet = KeyStoreManager.getFailCount(mActivityRule.getActivity());
        Assert.assertEquals(freshGet, freshGet);
    }

    @Test
    public void setGetSpendLimit() {
        long spendLimit = 100000;
        KeyStoreManager.putSpendLimit(spendLimit, mActivityRule.getActivity());
        assertFilesExist(KeyStoreManager.SPEND_LIMIT_ALIAS);
        long freshGet;
        freshGet = KeyStoreManager.getSpendLimit(mActivityRule.getActivity());
        Assert.assertEquals(freshGet, freshGet);
    }

    @Test
    public void setGetSFailTimeStamp() {
        long failTime = 1479686841;
        KeyStoreManager.putFailTimeStamp(failTime, mActivityRule.getActivity());
        assertFilesExist(KeyStoreManager.FAIL_TIMESTAMP_ALIAS);
        long freshGet;
        freshGet = KeyStoreManager.getFailTimeStamp(mActivityRule.getActivity());
        Assert.assertEquals(freshGet, freshGet);
    }

    @Test
    public void setGetLastPasscodeUsedTime() {
        long time = 1479686841;
        KeyStoreManager.putLastPasscodeUsedTime(time, mActivityRule.getActivity());
        assertFilesExist(KeyStoreManager.PASS_TIME_ALIAS);
        long freshGet;
        freshGet = KeyStoreManager.getLastPasscodeUsedTime(mActivityRule.getActivity());
        Assert.assertEquals(freshGet, freshGet);
    }

    @Test
    public void testClearKeyStore() {
        try {
            KeyStoreManager.putPhrase("axis husband project any sea patch drip tip spirit tide bring belt".getBytes(), mActivityRule.getActivity(), 0);
        } catch (UserNotAuthenticatedException e) {
            e.printStackTrace();
            Assert.fail();
        }
        try {
            KeyStoreManager.putCanary("canary", mActivityRule.getActivity(), 0);
        } catch (UserNotAuthenticatedException e) {
            e.printStackTrace();
            Assert.fail();
        }
        KeyStoreManager.putMasterPublicKey("26wZYDdvpmCrYZeUcxgqd1KquN4o6wXwLomBW5SjnwUqG".getBytes(), mActivityRule.getActivity());
        KeyStoreManager.putAuthKey("26wZYDdvpmCrYZeUcxgqd1KquN4o6wXwLomBW5SjnwUqG".getBytes(), mActivityRule.getActivity());
        KeyStoreManager.putToken("26wZYDdvpmCrYZeUcxgqd1KquN4o6wXwLomBW5SjnwUqG".getBytes(), mActivityRule.getActivity());
        KeyStoreManager.putWalletCreationTime(1479686841, mActivityRule.getActivity());
        KeyStoreManager.putPassCode("0123", mActivityRule.getActivity());
        KeyStoreManager.putFailCount(3, mActivityRule.getActivity());
        KeyStoreManager.putFailTimeStamp(1479686841, mActivityRule.getActivity());
        KeyStoreManager.putSpendLimit(10000000, mActivityRule.getActivity());
        KeyStoreManager.putLastPasscodeUsedTime(1479686841, mActivityRule.getActivity());

        for (String a : aliasObjectMap.keySet()) {
            assertFilesExist(a);
        }

        KeyStoreManager.resetWalletKeyStore(mActivityRule.getActivity());

        for (String a : aliasObjectMap.keySet()) {
            assertFilesDontExist(a);
        }


        byte[] phrase = "some".getBytes();
        try {
            phrase = KeyStoreManager.getPhrase(mActivityRule.getActivity(), 0);
        } catch (UserNotAuthenticatedException e) {
            e.printStackTrace();
        }

        String canary = "some";

        try {
            canary = KeyStoreManager.getCanary(mActivityRule.getActivity(), 0);
        } catch (UserNotAuthenticatedException e) {
            e.printStackTrace();
        }

        Assert.assertArrayEquals(phrase, new byte[0]);
        Assert.assertEquals(canary, "");
        Assert.assertArrayEquals(KeyStoreManager.getMasterPublicKey(mActivityRule.getActivity()), new byte[0]);
        Assert.assertArrayEquals(KeyStoreManager.getAuthKey(mActivityRule.getActivity()), new byte[0]);
        Assert.assertArrayEquals(KeyStoreManager.getToken(mActivityRule.getActivity()), new byte[0]);
        Assert.assertEquals(KeyStoreManager.getWalletCreationTime(mActivityRule.getActivity()), 0);
        Assert.assertEquals(KeyStoreManager.getPassCode(mActivityRule.getActivity()), "");
        Assert.assertEquals(KeyStoreManager.getFailCount(mActivityRule.getActivity()), 0);
        Assert.assertEquals(KeyStoreManager.getFailTimeStamp(mActivityRule.getActivity()), 0);
        Assert.assertEquals(KeyStoreManager.getSpendLimit(mActivityRule.getActivity()), 0);
        Assert.assertEquals(KeyStoreManager.getLastPasscodeUsedTime(mActivityRule.getActivity()), 0);

    }

    @Test
    public void testKeyStoreAuthTime() {
        Assert.assertEquals(KeyStoreManager.AUTH_DURATION_SEC, 300);
    }

    @Test
    public void testKeyStoreAliasMap() {
        Assert.assertNotNull(aliasObjectMap);
        Assert.assertEquals(aliasObjectMap.size(), 11);
    }

    public void assertFilesExist(String alias) {
        Assert.assertTrue(new File(KeyStoreManager.getEncryptedDataFilePath(aliasObjectMap.get(alias).datafileName, mActivityRule.getActivity())).exists());
        Assert.assertTrue(new File(KeyStoreManager.getEncryptedDataFilePath(aliasObjectMap.get(alias).ivFileName, mActivityRule.getActivity())).exists());
    }

    public void assertFilesDontExist(String alias) {
        Assert.assertFalse(new File(KeyStoreManager.getEncryptedDataFilePath(aliasObjectMap.get(alias).datafileName, mActivityRule.getActivity())).exists());
        Assert.assertFalse(new File(KeyStoreManager.getEncryptedDataFilePath(aliasObjectMap.get(alias).ivFileName, mActivityRule.getActivity())).exists());
    }

}
