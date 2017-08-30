package com.breadwallet.security;

import android.security.keystore.UserNotAuthenticatedException;
import android.support.test.rule.ActivityTestRule;
import android.support.test.runner.AndroidJUnit4;

import com.breadwallet.exceptions.BRKeystoreErrorException;
import com.breadwallet.presenter.activities.BreadActivity;
import com.breadwallet.tools.security.BRKeyStore;

import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.File;

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
    public ActivityTestRule<BreadActivity> mActivityRule = new ActivityTestRule<>(BreadActivity.class);

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
    public void setGetMasterPubKey() {
        byte[] pubKey = "26wZYDdvpmCrYZeUcxgqd1KquN4o6wXwLomBW5SjnwUqG".getBytes();
        BRKeyStore.putMasterPublicKey(pubKey, mActivityRule.getActivity());
        assertFilesExist(BRKeyStore.PUB_KEY_ALIAS);
        byte[] freshGet;
        freshGet = BRKeyStore.getMasterPublicKey(mActivityRule.getActivity());
        Assert.assertArrayEquals(freshGet, freshGet);
    }


    @Test
    public void setGetAuthKey() {
        byte[] authKey = "26wZYDdvpmCrYZeUcxgqd1KquN4o6wXwLomBW5SjnwUqG".getBytes();
        BRKeyStore.putAuthKey(authKey, mActivityRule.getActivity());
        assertFilesExist(BRKeyStore.AUTH_KEY_ALIAS);
        byte[] freshGet;
        freshGet = BRKeyStore.getAuthKey(mActivityRule.getActivity());
        Assert.assertArrayEquals(freshGet, freshGet);
    }

    @Test
    public void setGetToken() {
        byte[] token = "26wZYDdvpmCrYZeUcxgqd1KquN4o6wXwLomBW5SjnwUqG".getBytes();
        BRKeyStore.putToken(token, mActivityRule.getActivity());
        assertFilesExist(BRKeyStore.TOKEN_ALIAS);
        byte[] freshGet;
        freshGet = BRKeyStore.getToken(mActivityRule.getActivity());
        Assert.assertArrayEquals(freshGet, freshGet);
    }

    @Test
    public void setGetWalletCreationTime() {
        int time = 1479686841;
        BRKeyStore.putWalletCreationTime(time, mActivityRule.getActivity());
        assertFilesExist(BRKeyStore.WALLET_CREATION_TIME_ALIAS);
        int freshGet;
        freshGet = BRKeyStore.getWalletCreationTime(mActivityRule.getActivity());
        Assert.assertEquals(freshGet, freshGet);
    }

    @Test
    public void setGetPassCode() {
        String passCode = "0124";
        BRKeyStore.putPinCode(passCode, mActivityRule.getActivity());
        assertFilesExist(BRKeyStore.PASS_CODE_ALIAS);
        String freshGet;
        freshGet = BRKeyStore.getPinCode(mActivityRule.getActivity());
        Assert.assertEquals(freshGet, freshGet);

        passCode = "0000";
        BRKeyStore.putPinCode(passCode, mActivityRule.getActivity());
        assertFilesExist(BRKeyStore.PASS_CODE_ALIAS);
        freshGet = BRKeyStore.getPinCode(mActivityRule.getActivity());
        Assert.assertEquals(freshGet, freshGet);

        passCode = "9999";
        BRKeyStore.putPinCode(passCode, mActivityRule.getActivity());
        assertFilesExist(BRKeyStore.PASS_CODE_ALIAS);
        freshGet = BRKeyStore.getPinCode(mActivityRule.getActivity());
        Assert.assertEquals(freshGet, freshGet);

        passCode = "9876";
        BRKeyStore.putPinCode(passCode, mActivityRule.getActivity());
        assertFilesExist(BRKeyStore.PASS_CODE_ALIAS);
        freshGet = BRKeyStore.getPinCode(mActivityRule.getActivity());
        Assert.assertEquals(freshGet, freshGet);
    }

    @Test
    public void setGetFailCount() {
        int failCount = 2;
        BRKeyStore.putFailCount(failCount, mActivityRule.getActivity());
        assertFilesExist(BRKeyStore.FAIL_COUNT_ALIAS);
        int freshGet;
        freshGet = BRKeyStore.getFailCount(mActivityRule.getActivity());
        Assert.assertEquals(freshGet, freshGet);
    }

    @Test
    public void setGetSpendLimit() {
        long spendLimit = 100000;
        BRKeyStore.putSpendLimit(spendLimit, mActivityRule.getActivity());
        assertFilesExist(BRKeyStore.SPEND_LIMIT_ALIAS);
        long freshGet;
        freshGet = BRKeyStore.getSpendLimit(mActivityRule.getActivity());
        Assert.assertEquals(freshGet, freshGet);
    }

    @Test
    public void setGetSFailTimeStamp() {
        long failTime = 1479686841;
        BRKeyStore.putFailTimeStamp(failTime, mActivityRule.getActivity());
        assertFilesExist(BRKeyStore.FAIL_TIMESTAMP_ALIAS);
        long freshGet;
        freshGet = BRKeyStore.getFailTimeStamp(mActivityRule.getActivity());
        Assert.assertEquals(freshGet, freshGet);
    }

    @Test
    public void setGetLastPasscodeUsedTime() {
        long time = 1479686841;
        BRKeyStore.putLastPinUsedTime(time, mActivityRule.getActivity());
        assertFilesExist(BRKeyStore.PASS_TIME_ALIAS);
        long freshGet;
        freshGet = BRKeyStore.getLastPinUsedTime(mActivityRule.getActivity());
        Assert.assertEquals(freshGet, freshGet);
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
        BRKeyStore.putSpendLimit(10000000, mActivityRule.getActivity());
        BRKeyStore.putLastPinUsedTime(1479686841, mActivityRule.getActivity());

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

        Assert.assertArrayEquals(phrase, new byte[0]);
        Assert.assertEquals(canary, "");
        Assert.assertArrayEquals(BRKeyStore.getMasterPublicKey(mActivityRule.getActivity()), new byte[0]);
        Assert.assertArrayEquals(BRKeyStore.getAuthKey(mActivityRule.getActivity()), new byte[0]);
        Assert.assertArrayEquals(BRKeyStore.getToken(mActivityRule.getActivity()), new byte[0]);
        Assert.assertEquals(BRKeyStore.getWalletCreationTime(mActivityRule.getActivity()), 0);
        Assert.assertEquals(BRKeyStore.getPinCode(mActivityRule.getActivity()), "");
        Assert.assertEquals(BRKeyStore.getFailCount(mActivityRule.getActivity()), 0);
        Assert.assertEquals(BRKeyStore.getFailTimeStamp(mActivityRule.getActivity()), 0);
        Assert.assertEquals(BRKeyStore.getSpendLimit(mActivityRule.getActivity()), 0);
        Assert.assertEquals(BRKeyStore.getLastPinUsedTime(mActivityRule.getActivity()), 0);

    }

    @Test
    public void testKeyStoreAuthTime() {
        Assert.assertEquals(BRKeyStore.AUTH_DURATION_SEC, 300);
    }

    @Test
    public void testKeyStoreAliasMap() {
        Assert.assertNotNull(aliasObjectMap);
        Assert.assertEquals(aliasObjectMap.size(), 11);
    }

    public void assertFilesExist(String alias) {
        Assert.assertTrue(new File(BRKeyStore.getEncryptedDataFilePath(aliasObjectMap.get(alias).datafileName, mActivityRule.getActivity())).exists());
        Assert.assertTrue(new File(BRKeyStore.getEncryptedDataFilePath(aliasObjectMap.get(alias).ivFileName, mActivityRule.getActivity())).exists());
    }

    public void assertFilesDontExist(String alias) {
        Assert.assertFalse(new File(BRKeyStore.getEncryptedDataFilePath(aliasObjectMap.get(alias).datafileName, mActivityRule.getActivity())).exists());
        Assert.assertFalse(new File(BRKeyStore.getEncryptedDataFilePath(aliasObjectMap.get(alias).ivFileName, mActivityRule.getActivity())).exists());
    }

}
