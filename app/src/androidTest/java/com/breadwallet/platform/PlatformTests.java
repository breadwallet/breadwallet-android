/**
 * BreadWallet
 * <p/>
 * Created by Mihail Gutan <mihail@breadwallet.com> on 9/30/16.
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
package com.breadwallet.platform;

import androidx.test.rule.ActivityTestRule;
import android.util.Log;

import androidx.test.runner.AndroidJUnit4;
import com.breadwallet.legacy.presenter.activities.settings.TestActivity;
import com.breadwallet.tools.util.BRCompressor;
import com.breadwallet.tools.util.Utils;
import com.platform.APIClient;

import org.apache.commons.io.IOUtils;
import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import static org.junit.Assert.assertNotEquals;

@RunWith(AndroidJUnit4.class)
public class PlatformTests {
    public static final String TAG = PlatformTests.class.getName();

    @Rule
    public ActivityTestRule<TestActivity> mActivityRule = new ActivityTestRule<>(
            TestActivity.class);

    @Test
    public void bundleExtractTest() {
//        APIClient apiClient = APIClient.getInstance(mActivityRule.getActivity());
//
//        Request request = new Request.Builder()
//                .url(String.format("%s/assets/bundles/%s/download", BASE_URL, BRConstants.))
//                .get().build();
//
//        APIClient.BRResponse response = apiClient.sendRequest(request, false, 0);
//        apiClient.writeBundleToFile(response.getBody());
//        String extractFolderName = apiClient.getExtractedPath(mActivityRule.getActivity(), null);
//        apiClient.tryExtractTar();
//        File temp = new File(extractFolderName);
//        int filesExtracted = temp.listFiles().length;
//        Log.e(TAG, "bundleExtractTest: filesExtracted: " + filesExtracted);
//        Assert.assertNotSame(filesExtracted, 0);
//        Log.e(TAG, "bundleExtractTest: ");
//        if (temp.isDirectory()) {
//            String[] children = temp.list();
//            for (int i = 0; i < children.length; i++) {
//                new File(temp, children[i]).delete();
//            }
//        }
    }

    @Test
    public void bundleDownloadTest() {
//        APIClient apiClient = APIClient.getInstance(mActivityRule.getActivity());
//        Request request = new Request.Builder()
//                .get()
//                .url("https://s3.amazonaws.com/breadwallet-assets/bread-buy/7f5bc5c6cc005df224a6ea4567e508491acaffdc2e4769e5262a52f5b785e261.tar").build();
//        APIClient.BRResponse response = apiClient.sendRequest(request, false, 0);
//        File bundleFile = new File(apiClient.getBundleResource(mActivityRule.getActivity(), BREAD_POINT + ".tar"));
//        apiClient.writeBundleToFile(response.getBody());
//        String latestVersion = apiClient.fetchBundleVersion();
//        Assert.assertNotNull(latestVersion);
//        String currentTarVersion = getCurrentVersion(bundleFile);
//        Log.e(TAG, "bundleUpdateTest: latestVersion: " + latestVersion + ", currentTarVersion: " + currentTarVersion);
//
//        Assert.assertNotNull(currentTarVersion);
//        Assert.assertNotEquals(latestVersion, currentTarVersion);
    }

    @Test
    public void bundleUpdateTest() {

//        APIClient apiClient = APIClient.getInstance(mActivityRule.getActivity());
//
//        Request request = new Request.Builder()
//                .get()
//                .url("https://s3.amazonaws.com/breadwallet-assets/bread-buy/bundle.tar").build();
//        APIClient.BRResponse response = apiClient.sendRequest(request, false, 0);
//        byte[] bundleFileOldBytes = apiClient.writeBundleToFile(response.getBody());
//
//        request = new Request.Builder()
//                .get()
//                .url("https://s3.amazonaws.com/breadwallet-assets/bread-buy/bundle2.tar").build();
//        response = apiClient.sendRequest(request, false, 0);
//        File bundleFileLatest = new File(mActivityRule.getActivity().getFilesDir().getAbsolutePath() + String.format("/%s/%s.tar", APIClient.BUNDLES, BREAD_POINT + "-test"));
//        apiClient.writeBundleToFile(response.getBody());
//
//        request = new Request.Builder()
//                .get()
//                .url("https://s3.amazonaws.com/breadwallet-assets/bread-buy/bundle_bundle2.bspatch").build();
//        response = apiClient.sendRequest(request, false, 0);
//        File patch = new File(mActivityRule.getActivity().getFilesDir().getAbsolutePath() + String.format("/%s/%s.bspatch", APIClient.BUNDLES, "patch"));
//        byte[] patchBytes = apiClient.writeBundleToFile(response.getBody());
//
//        Assert.assertTrue(bundleFileOldBytes != null && bundleFileOldBytes.length > 10);
//        Assert.assertTrue(bundleFileLatest.exists() && bundleFileLatest.length() > 10);
//        Assert.assertTrue(patch.exists() && patch.length() > 10);
//
////        Log.e(TAG, "bundleUpdateTest: bundleFileOld.length(): " + bundleFileOld.length());
//        Log.e(TAG, "bundleUpdateTest: bundleFileLatest.length(): " + bundleFileLatest.length());
//        Log.e(TAG, "bundleUpdateTest: patch.length(): " + patch.length());
//
//        byte[] oldFileBytes = new byte[0];
//        byte[] correctFileBytes = new byte[0];
//        try {
//
//            FileOutputStream outputStream = new FileOutputStream(mActivityRule.getActivity().getFilesDir().getAbsolutePath() + String.format("/%s/%s.tar", APIClient.BUNDLES, BREAD_POINT));
//            Log.e(TAG, "bundleUpdateTest: beforeDiff");
//
//            Patch.patch(bundleFileOldBytes, patchBytes, outputStream);
//
//            byte[] updatedBundleBytes = IOUtils.toByteArray(new FileInputStream(bundleFileOld));
//            Log.e(TAG, "bundleUpdateTest: updatedBundleBytes: " + updatedBundleBytes.length);
//
//            oldFileBytes = IOUtils.toByteArray(new FileInputStream(bundleFileOld));
//            correctFileBytes = IOUtils.toByteArray(new FileInputStream(bundleFileLatest));
//
//        } catch (IOException | InvalidHeaderException | CompressorException e) {
//            e.printStackTrace();
//        } finally {
//            boolean delete = patch.delete();
//            Log.e(TAG, "WARNING bundleUpdateTest: deleting patch, file deleted: " + delete);
//        }
//        Log.e(TAG, "bundleUpdateTest: oldFileBytes: " + oldFileBytes.length + ", correctFileBytes: " + correctFileBytes.length);
//        Assert.assertArrayEquals(oldFileBytes, correctFileBytes);
//        Assert.assertTrue(oldFileBytes.length != 0 && correctFileBytes.length != 0);

    }

    private String getCurrentVersion(File bundleFile) {
        byte[] bFile;
        String currentTarVersion = null;
        try {
            bFile = IOUtils.toByteArray(new FileInputStream(bundleFile));
            Log.e(TAG, "bundleUpdateTest: bFile.length: " + bFile.length);
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(bFile);
            currentTarVersion = Utils.bytesToHex(hash);
        } catch (IOException | NoSuchAlgorithmException e) {
            e.printStackTrace();
        }
        return currentTarVersion;
    }

    @Test
    @Ignore("Signing not implemented")
    public void testGetToken() {
        APIClient apiClient = APIClient.getInstance(mActivityRule.getActivity());
        String token = apiClient.getToken();
        Assert.assertNotNull(token);
        assertNotEquals(token.length(), 0);
    }

    @Test
    public void testMeRequest() {
        APIClient apiClient = APIClient.getInstance(mActivityRule.getActivity());
        String response = apiClient.buyBitcoinMe();
        Assert.assertNotNull(response);
    }

    @Test
    public void testGZIP() {
        String data = "Ladies and Gentlemen of the class of '99: If I could offer you only one tip 11111111for the future, " +
                "sunscreen would be it.";
        Assert.assertFalse(BRCompressor.isGZIPStream(data.getBytes()));
        byte[] compressedData = BRCompressor.gZipCompress(data.getBytes());
        Assert.assertTrue(BRCompressor.isGZIPStream(compressedData));
        Log.e(TAG, "testGZIP: " + new String(compressedData));
        Assert.assertNotNull(compressedData);
        Assert.assertTrue(compressedData.length > 0);
        byte[] decompressedData = BRCompressor.gZipExtract(compressedData);
        Assert.assertFalse(BRCompressor.isGZIPStream(decompressedData));
        Assert.assertNotNull(decompressedData);
        Assert.assertEquals(new String(decompressedData), data);
        assertNotEquals(compressedData.length, decompressedData.length);
    }

    @Test
    public void testBZip2() {
        String data = "Ladies and Gentlemen of the class of '99: If I could offer you only one tip 11111111for the future, " +
                "sunscreen would be it.";
        byte[] compressedData = new byte[0];
        try {
            compressedData = BRCompressor.bz2Compress(data.getBytes());
        } catch (IOException e) {
            e.printStackTrace();
            Assert.fail();
        }

        Assert.assertNotNull(compressedData);
        Assert.assertTrue(compressedData.length > 0);
        byte[] decompressedData = BRCompressor.bz2Extract(compressedData);
        Assert.assertNotNull(decompressedData);
        Assert.assertEquals(new String(decompressedData), data);
        assertNotEquals(compressedData.length, decompressedData.length);
    }

    @Test
    public void testBitIdSignature() {
//        BRKey key = new BRKey("c4c9b99b714074736b65d9faab39145949894233a09d8100b91104750a82d31f");
//        String message = "https://breadwallet.com/bitid?nonce=123456789";
//        String expectedSig = "ICWek6XEVxu/1/x+TtWk178t6uFcToH019RWNnS+JEeJOr2XGkZKQwsSqEvJ7l3sfhUoX1jm4uWP7nmlyG5Y10E=";
//        String sig = BRBitId.signMessage(message, key);
//        Log.e(TAG, "sig: " + sig);
//        String expectedAddress = "mjBrDFeeX9moESGiRZZGeYrsUSNuvgwDVV";
//        String address = key.address();
//        Log.e(TAG, "address: " + address);
//        Assert.assertEquals(expectedAddress, address);
//        Assert.assertNotNull(sig);
//        Assert.assertEquals(expectedSig.length(), sig.length());
//        Assert.assertEquals(expectedSig, sig);
    }
}
