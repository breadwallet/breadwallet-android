package com.platform.tools;

import android.content.Context;
import android.net.Uri;
import android.security.keystore.UserNotAuthenticatedException;
import android.text.format.DateUtils;
import android.util.Base64;
import android.util.Log;

import com.breadwallet.core.BRCoreKey;
import com.breadwallet.core.BRCoreMasterPubKey;
import com.breadwallet.presenter.interfaces.BRAuthCompletion;
import com.breadwallet.tools.manager.BRSharedPrefs;
import com.breadwallet.tools.security.AuthManager;
import com.breadwallet.tools.security.BRKeyStore;
import com.breadwallet.tools.security.PostAuth;
import com.breadwallet.tools.threads.executor.BRExecutor;
import com.breadwallet.tools.util.BRConstants;
import com.breadwallet.tools.util.Utils;
import com.platform.APIClient;
import com.platform.middlewares.plugins.WalletPlugin;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import okhttp3.Request;
import okhttp3.RequestBody;


/**
 * BreadWallet
 * <p/>
 * Created by Mihail Gutan on <mihail@breadwallet.com> 1/25/17.
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
public class BRBitId {
    public static final String TAG = BRBitId.class.getName();
    private static final String BITCOIN_SIGNED_MESSAGE_HEADER = "Bitcoin Signed Message:\n";
    private static final String BIT_ID = "bitid";
    private static final String BIT_ID_URL = "bitid_url";
    private static final String BIT_ID_INDEX = "bitid_index";
    private static final String PROMPT_STRING = "prompt_string";
    private static final String STRING_TO_SIGN = "string_to_sign";
    private static final String ADDRESS = "address";
    private static final String SIGNATURE = "signature";
    private static final String HTTPS = "https";
    private static final String HTTP = "http";
    private static final String URI = "uri";
    private static final String SHA_256 = "SHA-256";
    private static final String X_QUERY_PARAM_STRING = "?x=";
    private static final String BIT_ID_SCHEME = "bitid://";
    private static final String URI_DELIMITER = "://";
    private static final String QUERY_PARAM_U = "u";
    private static final String QUERY_PARAM_X = "x";
    private static final String QUERY_PARAM_U_VALUE = "1";

    private static String mBitUri;
    private static String mStringToSign;
    private static String mPromptString;
    private static int mIndex;
    private static volatile Map<String, Boolean> mBitIdKeys = new HashMap<>();

    public static boolean isBitId(String uri) {
        try {
            URI bitIdUri = new URI(uri);
            if (BIT_ID.equals(bitIdUri.getScheme())) {
                return true;
            }
        } catch (URISyntaxException e) {
            e.printStackTrace();
        }
        return false;
    }

    public static void signBitID(final Context context, String uri, JSONObject jsonBody) {

        if (uri == null && jsonBody != null) {
            try {
                uri = jsonBody.getString(BIT_ID_URL);
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }

        if (uri == null) {
            Log.e(TAG, "signBitID: uri is null");
            return;
        }
        mBitUri = uri;

        final URI bitIdUri;
        try {
            bitIdUri = new URI(mBitUri);
        } catch (URISyntaxException e) {
            e.printStackTrace();
            Log.e(TAG, "signBitID: returning false: ", e);
            return;
        }

        final String biUri = bitIdUri.getHost() == null ? bitIdUri.toString() : bitIdUri.getHost();
        final boolean authNeeded = !mBitIdKeys.containsKey(biUri);

        if (jsonBody != null) {
            try {
                mPromptString = jsonBody.getString(PROMPT_STRING);
                mIndex = jsonBody.getInt(BIT_ID_INDEX);
                mStringToSign = jsonBody.getString(STRING_TO_SIGN);
            } catch (JSONException e) {
                e.printStackTrace();
                return;
            }
        } else if (BIT_ID.equals(bitIdUri.getScheme())) {
            if (context == null) {
                Log.e(TAG, "signBitID: app is null, returning true still");
                return;
            }
            mPromptString = "BitID Authentication Request";
        }

        BRExecutor.getInstance().forBackgroundTasks().execute(new Runnable() {
            @Override
            public void run() {
                try {
                    Thread.sleep(DateUtils.SECOND_IN_MILLIS / 2);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }

                if (authNeeded) {
                    BRExecutor.getInstance().forMainThreadTasks().execute(new Runnable() {
                        @Override
                        public void run() {
                            AuthManager.getInstance().authPrompt(context, mPromptString, bitIdUri.getHost(),
                                    true, false, new BRAuthCompletion() {
                                        @Override
                                        public void onComplete() {
                                            PostAuth.getInstance().onBitIDAuth(context, true);
                                        }

                                        @Override
                                        public void onCancel() {
                                            PostAuth.getInstance().onBitIDAuth(context, false);
                                        }
                                    });
                        }
                    });

                } else {
                    PostAuth.getInstance().onBitIDAuth(context, true);
                }

            }
        });
    }

    public static void completeBitID(final Context context, boolean authenticated) {
        final byte[] phrase;
        final byte[] seed;
        if (!authenticated) {
            WalletPlugin.sendBitIdResponse(null, false);
            return;
        }
        if (context == null) {
            Log.e(TAG, "completeBitID: app is null");
            return;
        }
        if (mBitUri == null) {
            Log.e(TAG, "completeBitID: mBitUri is null");
            return;
        }

        final Uri uri = Uri.parse(mBitUri);

        try {
            phrase = BRKeyStore.getPhrase(context, BRConstants.REQUEST_PHRASE_BITID);
        } catch (UserNotAuthenticatedException e) {
            return;
        }
        if (Utils.isNullOrEmpty(phrase)) throw new NullPointerException("cant happen");
        seed = BRCoreKey.getSeedFromPhrase(phrase);
        if (Utils.isNullOrEmpty(seed)) {
            Log.e(TAG, "completeBitID: seed is null!");
            return;
        }

        //run the callback
        BRExecutor.getInstance().forBackgroundTasks().execute(new Runnable() {
            @Override
            public void run() {
                try {
                    if (mStringToSign == null) {
                        //meaning it's a link handling
                        bitIdLink(context, uri, seed);
                    } else {
                        //meaning its the wallet plugin, glidera auth (platform)
                        bitIdPlatform(context, uri, seed);
                    }
                } finally {
                    //release everything
                    mBitUri = null;
                    mStringToSign = null;
                    mPromptString = null;
                    mIndex = 0;
                    Arrays.fill(phrase, (byte) 0);
                    Arrays.fill(seed, (byte) 0);
                }
            }
        });

    }

    private static void bitIdPlatform(Context context, Uri uri, byte[] seed) {

        final String biUri = uri.getHost() == null ? uri.toString() : uri.getHost();
        final byte[] key = BRCoreMasterPubKey.bip32BitIDKey(seed, mIndex, biUri);
        if (key == null) {
            Log.d(TAG, "bitIdPlatform: key is null!");
            return;
        }
        if (mStringToSign == null) {
            Log.d(TAG, "bitIdPlatform: mStringToSign is null!");
            return;
        }
        final String sig = BRBitId.signMessage(mStringToSign, new BRCoreKey(key));
        final String address = new BRCoreKey(key).addressLegacy();

        JSONObject postJson = new JSONObject();
        Log.d(TAG, "GLIDERA: address:" + address);
        try {
            postJson.put(ADDRESS, address);
            postJson.put(SIGNATURE, sig);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        //keep auth off for this host for 60 seconds.
        if (!mBitIdKeys.containsKey(biUri)) {
            mBitIdKeys.put(biUri, true);
            Log.d(TAG, "run: saved temporary sig for key: " + biUri);
            BRExecutor.getInstance().forBackgroundTasks().execute(new Runnable() {
                @Override
                public void run() {
                    try {
                        Thread.sleep(DateUtils.MINUTE_IN_MILLIS);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    Log.d(TAG, "run: removed temporary sig for key: " + biUri);
                    if (mBitIdKeys != null) {
                        mBitIdKeys.remove(biUri);
                    }
                }
            });
        }
        WalletPlugin.sendBitIdResponse(postJson, true);
    }

    private static void bitIdLink(Context context, Uri uri, byte[] seed) {
        String nonce;
        String scheme = HTTPS;

        String u = uri.getQueryParameter(QUERY_PARAM_U);
        if (u != null && u.equalsIgnoreCase(QUERY_PARAM_U_VALUE)) {
            scheme = HTTP;
        }
        String x = uri.getQueryParameter(QUERY_PARAM_X);
        if (Utils.isNullOrEmpty(x)) {
            // we are generating our own nonce
            nonce = newNonce(context, uri.getHost() + uri.getPath());
        } else {
            // service is providing a nonce
            nonce = x;
        }

        String callbackUrl = scheme + URI_DELIMITER + uri.getHost() + uri.getPath();

        // build a payload consisting of the signature, address and signed uri
        String uriWithNonce = BIT_ID_SCHEME + uri.getHost() + uri.getPath() + X_QUERY_PARAM_STRING + nonce;

        Log.e(TAG, "LINK: callbackUrl:" + callbackUrl);
        final byte[] key = BRCoreMasterPubKey.bip32BitIDKey(seed, mIndex, mBitUri);

        if (key == null) {
            Log.e(TAG, "completeBitID: key is null!");
            return;
        }

        final String sig = BRBitId.signMessage(uriWithNonce, new BRCoreKey(key));
        final String address = new BRCoreKey(key).addressLegacy();
        Log.d(TAG, "LINK: address: " + address);
        JSONObject postJson = new JSONObject();
        try {
            postJson.put(ADDRESS, address);
            postJson.put(SIGNATURE, sig);
            postJson.put(URI, uriWithNonce);
        } catch (JSONException e) {
            e.printStackTrace();
        }

        RequestBody requestBody = RequestBody.create(null, postJson.toString());
        Request request = new Request.Builder()
                .url(callbackUrl + X_QUERY_PARAM_STRING + nonce)
                .post(requestBody)
                .header(BRConstants.HEADER_CONTENT_TYPE, BRConstants.CONTENT_TYPE_JSON)
                .build();
        APIClient.BRResponse res = APIClient.getInstance(context).sendRequest(request, true);
        if (!res.isSuccessful()) {
            Log.e(TAG, "bitIdLink: Request was unsuccesful: " + res.getCode());
        }
    }

    private static String newNonce(Context context, String nonceKey) {
        // load previous nonces. we save all nonces generated for each service
        // so they are not used twice from the same device
        List<Integer> existingNonces = BRSharedPrefs.getBitIdNonces(context, nonceKey);

        String nonce = "";
        while (existingNonces.contains(Integer.valueOf(nonce))) {
            nonce = String.valueOf(System.currentTimeMillis() / DateUtils.SECOND_IN_MILLIS);
        }
        existingNonces.add(Integer.valueOf(nonce));
        BRSharedPrefs.putBitIdNonces(context, existingNonces, nonceKey);

        return nonce;
    }

    private static String signMessage(String message, BRCoreKey key) {
        byte[] signingData = formatMessageForBitcoinSigning(message);

        MessageDigest digest;
        try {
            digest = MessageDigest.getInstance(SHA_256);
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
            return null;
        }
        byte[] sha256First = digest.digest(signingData);
        byte[] sha256Second = digest.digest(sha256First);
        byte[] signature = key.compactSign(sha256Second);

        return Base64.encodeToString(signature, Base64.NO_WRAP);
    }

    private static byte[] formatMessageForBitcoinSigning(String message) {
        byte[] headerBytes = null;
        byte[] messageBytes = null;

        try {
            headerBytes = BITCOIN_SIGNED_MESSAGE_HEADER.getBytes(BRConstants.UTF_8);
            messageBytes = message.getBytes(BRConstants.UTF_8);
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
        if (headerBytes == null || messageBytes == null) {
            return new byte[0];
        }

        int cap = 1 + headerBytes.length + varIntSize(messageBytes.length) + messageBytes.length;

        ByteBuffer dataBuffer = ByteBuffer.allocate(cap).order(ByteOrder.LITTLE_ENDIAN);
        dataBuffer.put((byte) headerBytes.length);          //put header count
        dataBuffer.put(headerBytes);                        //put the header
        putVarInt(message.length(), dataBuffer);            //put message count
        dataBuffer.put(messageBytes);                       //put the message

        return dataBuffer.array();
    }

    /**
     * Returns the encoding size in bytes of its input value.
     *
     * @param i the integer to be measured
     * @return the encoding size in bytes of its input value
     */
    private static int varIntSize(int i) {
        int result = 0;
        do {
            result++;
            i >>>= 7;
        } while (i != 0);
        return result;
    }

    /**
     * Encodes an integer in a variable-length encoding, 7 bits per byte, to a
     * ByteBuffer sink.
     *
     * @param v    the value to encode
     * @param sink the ByteBuffer to add the encoded value
     */
    private static void putVarInt(int v, ByteBuffer sink) {
        while (true) {
            int bits = v & 0x7f;
            v >>>= 7;
            if (v == 0) {
                sink.put((byte) bits);
                return;
            }
            sink.put((byte) (bits | 0x80));
        }
    }
}
