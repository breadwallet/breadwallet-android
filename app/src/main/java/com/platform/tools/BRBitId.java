package com.platform.tools;

import android.app.Activity;
import android.net.Uri;
import android.security.keystore.UserNotAuthenticatedException;
import android.util.Base64;
import android.util.Log;

import io.digibyte.presenter.interfaces.BRAuthCompletion;
import io.digibyte.tools.manager.BRSharedPrefs;
import io.digibyte.tools.security.AuthManager;
import io.digibyte.tools.security.BRKeyStore;
import io.digibyte.tools.security.PostAuth;
import io.digibyte.tools.threads.BRExecutor;
import io.digibyte.tools.util.BRConstants;
import io.digibyte.tools.util.TypesConverter;
import io.digibyte.tools.util.Utils;
import io.digibyte.wallet.BRWalletManager;
import com.jniwrappers.BRBIP32Sequence;
import com.jniwrappers.BRKey;
import com.platform.APIClient;
import com.platform.middlewares.plugins.WalletPlugin;

import junit.framework.Assert;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
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
import okhttp3.Response;


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
    public static final String BITCOIN_SIGNED_MESSAGE_HEADER = "Digibyte Signed Message:\n";

    private static String _bitUri;
    private static String _bitIdUrl;
    private static String _strToSign = null;
    private static String _promptString = null;
    private static int _index = 0;
    private volatile static Map<String, Boolean> bitIdKeys = new HashMap<>();

    public static boolean isBitId(String uri) {
        try {
            URI bitIdUri = new URI(uri);
            if ("digiid".equals(bitIdUri.getScheme())) {
                return true;
            }
        } catch (URISyntaxException e) {
            e.printStackTrace();
        }
        return false;
    }

    public static void signBitID(final Activity app, String uri, JSONObject jsonBody) {

        if (uri == null && jsonBody != null) {
            try {
                uri = jsonBody.getString("digiid url");
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }

        if (uri == null) {
            Log.e(TAG, "signDigiID: uri is null");
            return;
        }
        _bitUri = uri;

        final URI bitIdUri;
        try {
            bitIdUri = new URI(_bitUri);
        } catch (URISyntaxException e) {
            e.printStackTrace();
            Log.e(TAG, "signDigiID: returning false: ", e);
            return;
        }

        final String biUri = bitIdUri.getHost() == null ? bitIdUri.toString() : bitIdUri.getHost();
        final boolean authNeeded = !bitIdKeys.containsKey(biUri);

        if (jsonBody != null) {
            try {
                _promptString = jsonBody.getString("prompt_string");
                _bitIdUrl = jsonBody.getString("digiid_url");
                _index = jsonBody.getInt("digiid_index");
                _strToSign = jsonBody.getString("string_to_sign");
            } catch (JSONException e) {
                e.printStackTrace();
                return;
            }
        } else if ("digiid".equals(bitIdUri.getScheme())) {
            if (app == null) {
                Log.e(TAG, "signDigiID: app is null, returning true still");
                return;
            }
            _promptString = "DigiID Authentication Request";
        }

//        Log.e(TAG, "signBitID: _bitUri: " + _bitUri);
//        Log.e(TAG, "signBitID: _strToSign: " + _strToSign);
//        Log.e(TAG, "signBitID: _index: " + _index);
        BRExecutor.getInstance().forBackgroundTasks().execute(new Runnable() {
            @Override
            public void run() {
                try {
                    Thread.sleep(500);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }

                if (authNeeded) {
                    app.runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            AuthManager.getInstance().authPrompt(app, _promptString, bitIdUri.getHost(), true,false, new BRAuthCompletion() {
                                @Override
                                public void onComplete() {
                                    PostAuth.getInstance().onBitIDAuth(app, true);
                                }

                                @Override
                                public void onCancel() {
                                    PostAuth.getInstance().onBitIDAuth(app, false);
                                }
                            });
                        }
                    });
                } else {
                    PostAuth.getInstance().onBitIDAuth(app, true);
                }

            }
        });
    }

    public static void completeBitID(final Activity app, boolean authenticated) {
        final byte[] phrase;
        final byte[] nulTermPhrase;
        final byte[] seed;
        if (!authenticated) {
            WalletPlugin.sendBitIdResponse(null, false);
            return;
        }
        if (app == null) {
            Log.e(TAG, "completeDigiID: app is null");
            return;
        }
        if (_bitUri == null) {
            Log.e(TAG, "completeDigiID _bitUri is null");
            return;
        }

        final Uri uri = Uri.parse(_bitUri);

        try {
            phrase = BRKeyStore.getPhrase(app, BRConstants.REQUEST_PHRASE_BITID);
        } catch (UserNotAuthenticatedException e) {
            return;
        }
        if (Utils.isNullOrEmpty(phrase)) throw new NullPointerException("cant happen");
        nulTermPhrase = TypesConverter.getNullTerminatedPhrase(phrase);
        seed = BRWalletManager.getSeedFromPhrase(nulTermPhrase);
        if (Utils.isNullOrEmpty(seed)) {
            Log.e(TAG, "completeDigiID: seed is null!");
            return;
        }

        //run the callback
        BRExecutor.getInstance().forBackgroundTasks().execute(new Runnable() {
            @Override
            public void run() {
                try {
                    if (_strToSign == null) {
                        //meaning it's a link handling
                        bitIdLink(app, uri, seed);
                    } else {
                        //meaning its the wallet plugin, glidera auth (platform)
                        bitIdPlatform(app, uri, seed);
                    }
                } finally {
                    //release everything
                    _bitUri = null;
                    _strToSign = null;
                    _bitIdUrl = null;
                    _promptString = null;
                    _index = 0;
                    Arrays.fill(phrase, (byte) 0);
                    if (nulTermPhrase != null) Arrays.fill(nulTermPhrase, (byte) 0);
                    Arrays.fill(seed, (byte) 0);
                }
            }
        });

    }

    private static void bitIdPlatform(Activity app, Uri uri, byte[] seed) {

        final String biUri = uri.getHost() == null ? uri.toString() : uri.getHost();
        final byte[] key = BRBIP32Sequence.getInstance().bip32BitIDKey(seed, _index, biUri);
        if (key == null) {
            Log.d(TAG, "DigiIDPlatform: key is null!");
            return;
        }
        if (_strToSign == null) {
            Log.d(TAG, "DigiIDPlatform: _strToSign is null!");
            return;
        }
        final String sig = BRBitId.signMessage(_strToSign, new BRKey(key));
        final String address = new BRKey(key).address();

        JSONObject postJson = new JSONObject();
        Log.e(TAG, "GLIDERA: address:" + address);
        try {
            postJson.put("address", address);
            postJson.put("signature", sig);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        //keep auth off for this host for 60 seconds.
        if (!bitIdKeys.containsKey(biUri)) {
            bitIdKeys.put(biUri, true);
            Log.d(TAG, "run: saved temporary sig for key: " + biUri);
            BRExecutor.getInstance().forBackgroundTasks().execute(new Runnable() {
                @Override
                public void run() {
                    try {
                        Thread.sleep(60000);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    Log.d(TAG, "run: removed temporary sig for key: " + biUri);
                    if (bitIdKeys != null)
                        bitIdKeys.remove(biUri);
                }
            });
        }
        WalletPlugin.sendBitIdResponse(postJson, true);
    }

    private static void bitIdLink(Activity app, Uri uri, byte[] seed) {
        String nonce = null;
        String scheme = "https";

        String u = uri.getQueryParameter("u");
        if (u != null && u.equalsIgnoreCase("1")) {
            scheme = "http";
        }
        String x = uri.getQueryParameter("x");
        if (Utils.isNullOrEmpty(x)) {
            nonce = newNonce(app, uri.getHost() + uri.getPath());     // we are generating our own nonce
        } else {
            nonce = x;              // service is providing a nonce
        }

        String callbackUrl = String.format("%s://%s%s", scheme, uri.getHost(), uri.getPath());

        // build a payload consisting of the signature, address and signed uri

        String uriWithNonce = String.format("digiid://%s%s?x=%s", uri.getHost(), uri.getPath(), nonce);

        Log.e(TAG, "LINK: callbackUrl:" + callbackUrl);
        final byte[] key = BRBIP32Sequence.getInstance().bip32BitIDKey(seed, _index, _bitUri);

        if (key == null) {
            Log.d(TAG, "completeDigiID: key is null!");
            return;
        }

        final String sig = BRBitId.signMessage(uriWithNonce, new BRKey(key));
        final String address = new BRKey(key).address();
        Log.e(TAG, "LINK: address: " + address);
        JSONObject postJson = new JSONObject();
        try {
            postJson.put("address", address);
            postJson.put("signature", sig);
            postJson.put("uri", uriWithNonce);
        } catch (JSONException e) {
            e.printStackTrace();
        }

        RequestBody requestBody = RequestBody.create(null, postJson.toString());
        Request request = new Request.Builder()
                .url(callbackUrl + "?x=" + nonce)
                .post(requestBody)
                .header("Content-Type", "application/json")
                .build();
        Response res = APIClient.getInstance(app).sendRequest(request, true, 0);
        Log.e(TAG, "completeDigiID: res.code: " + res.code());
        Log.e(TAG, "completeDigiID: res.code: " + res.message());
        try {
            Log.e(TAG, "completeDigiID: body: " + res.body().string());
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            res.close();
        }
    }

    public static String newNonce(Activity app, String nonceKey) {
        // load previous nonces. we save all nonces generated for each service
        // so they are not used twice from the same device
        List<Integer> existingNonces = BRSharedPrefs.getBitIdNonces(app, nonceKey);

        String nonce = "";
        while (existingNonces.contains(Integer.valueOf(nonce))) {
            nonce = String.valueOf(System.currentTimeMillis() / 1000);
        }
        existingNonces.add(Integer.valueOf(nonce));
        BRSharedPrefs.putBitIdNonces(app, existingNonces, nonceKey);

        return nonce;
    }

    public static String signMessage(String message, BRKey key) {
        byte[] signingData = formatMessageForBitcoinSigning(message);

        MessageDigest digest = null;
        try {
            digest = MessageDigest.getInstance("SHA-256");
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
            headerBytes = BITCOIN_SIGNED_MESSAGE_HEADER.getBytes("UTF-8");
            messageBytes = message.getBytes("UTF-8");
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
        assert (headerBytes != null);
        assert (messageBytes != null);
        if (headerBytes == null || messageBytes == null) return new byte[0];

        int cap = 1 + headerBytes.length + varIntSize(messageBytes.length) + messageBytes.length;

        ByteBuffer dataBuffer = ByteBuffer.allocate(cap).order(ByteOrder.LITTLE_ENDIAN);
        dataBuffer.put((byte) headerBytes.length);          //put header count
        dataBuffer.put(headerBytes);                        //put the header
        putVarInt(message.length(), dataBuffer);            //put message count
        dataBuffer.put(messageBytes);                       //put the message
        byte[] result = dataBuffer.array();

        Assert.assertEquals(cap, result.length);

        return result;
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
