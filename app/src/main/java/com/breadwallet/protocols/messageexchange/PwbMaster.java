package com.breadwallet.protocols.messageexchange;

import android.content.Context;
import android.util.Base64;
import android.util.Log;

import com.breadwallet.core.BRCoreKey;
import com.breadwallet.protocols.messageexchange.entities.EncryptedObject;
import com.breadwallet.protocols.messageexchange.entities.InboxEntry;
import com.breadwallet.protocols.messageexchange.entities.PairingObject;
import com.breadwallet.tools.crypto.Base58;
import com.breadwallet.tools.crypto.CryptoHelper;
import com.breadwallet.tools.crypto.HmacDrbg;
import com.breadwallet.tools.manager.BRSharedPrefs;
import com.breadwallet.tools.security.BRKeyStore;
import com.breadwallet.tools.threads.executor.BRExecutor;
import com.breadwallet.tools.util.Utils;
import com.google.protobuf.ByteString;
import com.google.protobuf.InvalidProtocolBufferException;

import java.io.UnsupportedEncodingException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * BreadWallet
 * <p/>
 * Created by Mihail Gutan on <mihail@breadwallet.com> 7/12/18.
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
 */

public final class PwbMaster {
    private static final String TAG = PwbMaster.class.getSimpleName();
    private static ByteString mSenderId = null;
    private static ByteString mPairingPublicKey = null;

    public static final int ENVELOPE_VERSION = 1;
    public static final int HMAC_SIZE_BYTES = 32;
    public static final String SERVICE_PWB = "PWB";

    public enum MessageType {
        LINK, PING, PONG,
        ACCOUNT_REQUEST, ACCOUNT_RESPONSE,
        PAYMENT_REQUEST, PAYMENT_RESPONSE,
        CALL_REQUEST, CALL_RESPONSE;
    }

    private PwbMaster() {
    }

    public static Protos.Envelope createEnvelope(ByteString encryptedMessage, MessageType messageType,
                                                 ByteString senderPublicKey, ByteString receiverPublicKey,
                                                 String uniqueId, ByteString nonce) {
        Protos.Envelope envelope = com.breadwallet.protocols.messageexchange.Protos.Envelope.newBuilder()
                .setVersion(ENVELOPE_VERSION)
                .setMessageType(messageType.name())
                .setService(SERVICE_PWB)
                .setEncryptedMessage(encryptedMessage)
                .setSenderPublicKey(senderPublicKey)
                .setReceiverPublicKey(receiverPublicKey)
                .setIdentifier(uniqueId)
                .setSignature(ByteString.EMPTY)
                .setNonce(nonce)
                .build();
        return envelope;

    }

    public static BRCoreKey getPairingKey(Context context, byte[] senderPublicKey, byte[] id) {
        if (Utils.isNullOrEmpty(senderPublicKey) || Utils.isNullOrEmpty(id)) {
            Log.e(TAG, "pairWallet: invalid query parameters");
            return null;
        }
        byte[] idSha256 = CryptoHelper.sha256(id);
        byte[] authKey = BRKeyStore.getAuthKey(context);
        HmacDrbg hmacDrbg = new HmacDrbg(authKey, idSha256);
        byte[] hmacDrgbResult = new byte[HMAC_SIZE_BYTES];
        hmacDrbg.nextBytes(hmacDrgbResult, 0, HMAC_SIZE_BYTES);
        return new BRCoreKey(hmacDrgbResult, false);
    }

    public static EncryptedObject encryptMessage(Context context, byte[] senderPublicKey, byte[] data) {
        byte[] nonce = CryptoHelper.generateRandomNonce();
        BRCoreKey authKey = new BRCoreKey(BRKeyStore.getAuthKey(context));
        byte[] encryptedData = authKey.encryptUsingSharedSecret(senderPublicKey, data, nonce);
        return new EncryptedObject(encryptedData, nonce);
    }

    public static void processInboxMessages(Context context, List<InboxEntry> inboxEntries) {
        Log.e(TAG, "processInboxMessages: " + inboxEntries.size());
        List<String> cursors = new ArrayList<>();
        for (InboxEntry inboxEntry : inboxEntries) {
            Protos.Envelope requestEnvelope = getEnvelopeFromInbox(inboxEntry);
            boolean isEnvelopValid = verifyEnvelope(requestEnvelope);
            if (!isEnvelopValid) {
                Log.e(TAG, "createResponseEnvelope: verifyEnvelope failed!!!");
                continue;
            }

            Protos.Envelope responseEnvelope = createResponseEnvelope(context, requestEnvelope);

            cursors.add(inboxEntry.getCursor());
            MessageApi.sendEnvelope(context, responseEnvelope.toByteArray());
        }
        MessageApi.sendAck(context, cursors);
    }

    public static void checkInboxAndRespond(Context context) {
        List<InboxEntry> inboxEntries = MessageApi.fetchInbox(context);
        processInboxMessages(context, inboxEntries);
    }

    /**
     * @param envelope the envelope to verify
     * @return true if valid
     */
    public static boolean verifyEnvelope(Protos.Envelope envelope) {
        byte[] signature = envelope.getSignature().toByteArray();
        byte[] senderPublicKey = envelope.getSenderPublicKey().toByteArray();
        envelope = envelope.toBuilder().setSignature(ByteString.EMPTY).build();
        byte[] envelopeDoubleSha256 = CryptoHelper.doubleSha256(envelope.toByteArray());
        BRCoreKey key = BRCoreKey.compactSignRecoverKey(envelopeDoubleSha256, signature);
        byte[] recoveredPubKey = key.getPubKey();
        return Arrays.equals(senderPublicKey, recoveredPubKey);
    }

    public static Protos.Envelope createResponseEnvelope(Context context, Protos.Envelope requestEnvelope) {
        int version = requestEnvelope.getVersion();
        String service = requestEnvelope.getService();
        String expiration = requestEnvelope.getExpiration();
        String messageType = requestEnvelope.getMessageType();
        ByteString encryptedMessage = requestEnvelope.getEncryptedMessage();
        ByteString senderPublicKey = requestEnvelope.getSenderPublicKey();
        ByteString receiverPublicKey = requestEnvelope.getReceiverPublicKey();
        String identifier = requestEnvelope.getIdentifier();
        ByteString nonce = requestEnvelope.getNonce();
        ByteString signature = requestEnvelope.getSignature();

        try {
            requestEnvelope = requestEnvelope.toBuilder().setSignature(ByteString.EMPTY).build();
            BRCoreKey authKey = new BRCoreKey(BRKeyStore.getAuthKey(context));
            byte[] pubKey = authKey.getPubKey();
            byte[] decryptedMessageBytes = authKey.decryptUsingSharedSecret(senderPublicKey.toByteArray(), encryptedMessage.toByteArray(), nonce.toByteArray());
            MessageType requestMessageType = MessageType.valueOf(messageType);
            ByteString messageResponse = generateResponseMessage(context, decryptedMessageBytes, requestMessageType);
            EncryptedObject responseEncryptedObject = encryptMessage(context, senderPublicKey.toByteArray(), messageResponse.toByteArray());
            MessageType responseMessageType = getResponseMessageType(requestMessageType);
            BRCoreKey pairingKey = getPairingKey(context, senderPublicKey.toByteArray(), identifier.getBytes());
            byte[] responseSignature = pairingKey.compactSign(CryptoHelper.doubleSha256(requestEnvelope.toByteArray()));
            Protos.Envelope responseEnvelope = createEnvelope(ByteString.copyFrom(responseEncryptedObject.getEncryptedData()),
                    responseMessageType, ByteString.copyFrom(pubKey), senderPublicKey, identifier,
                    ByteString.copyFrom(responseEncryptedObject.getNonce()));
            responseEnvelope = responseEnvelope.toBuilder().setSignature(ByteString.copyFrom(responseSignature)).build();
            return responseEnvelope;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    private static Protos.Envelope getEnvelopeFromInbox(InboxEntry entry) {
        Protos.Envelope requestEnvelope = null;
        try {
            byte[] envelopeData = Base64.decode(entry.getMessage(), Base64.NO_WRAP);
            requestEnvelope = Protos.Envelope.parseFrom(envelopeData);
        } catch (InvalidProtocolBufferException e) {
            e.printStackTrace();
            return null;
        }
        return requestEnvelope;
    }

    private static MessageType getResponseMessageType(MessageType requestMessageType) {
        switch (requestMessageType) {
            case LINK:
                return MessageType.LINK;
            case PING:
                return MessageType.PONG;
            case ACCOUNT_REQUEST:
                return MessageType.ACCOUNT_RESPONSE;
            case PAYMENT_REQUEST:
                return MessageType.PAYMENT_RESPONSE;
            case CALL_REQUEST:
                return MessageType.CALL_RESPONSE;
        }
        return null;
    }

    private static ByteString generateResponseMessage(Context context, byte[] requestDecryptedMessage, MessageType requestMessageType) {
        ByteString resultMessage = null;
        switch (requestMessageType) {
            case LINK:
                byte[] pubKey = new BRCoreKey(BRKeyStore.getAuthKey(context)).getPubKey();
                Protos.Link linkMessage = null;
                try {
                    linkMessage = Protos.Link.parseFrom(requestDecryptedMessage);
                } catch (InvalidProtocolBufferException e) {
                    e.printStackTrace();
                }
                mSenderId = linkMessage.getId();
                mPairingPublicKey = linkMessage.getPublicKey();
                //don't respond, at this point we received what we need for our paring, wait for other messages.
//                resultMessage = createLinkMessage(pubKey, Protos.Status.ACCEPTED, mSenderId);
                break;
            case PING:
                resultMessage = Protos.Pong.newBuilder().setPong("Hi Dan, you're so awesome.").getPongBytes();
                break;
            case ACCOUNT_REQUEST:
                break;
            case PAYMENT_REQUEST:
                break;
            case CALL_REQUEST:
                break;
        }
        return resultMessage;
    }

    private static ByteString createLinkMessage(byte[] pubKey, Protos.Status status, ByteString senderId) {
        return Protos.Link.newBuilder().setPublicKey(ByteString.copyFrom(pubKey))
                .setStatus(status).setId(senderId).build().toByteString();
    }

    public static void startPairing(final Context context, PairingObject pairingObject) {
        byte[] pubKey = new BRCoreKey(BRKeyStore.getAuthKey(context)).getPubKey();
        try {
            ByteString message = createLinkMessage(pubKey, Protos.Status.ACCEPTED, ByteString.copyFrom(pairingObject.getId(), StandardCharsets.UTF_8.name()));
            byte[] senderPublickey = BRCoreKey.decodeHex(pairingObject.getPublicKeyHex());
            EncryptedObject encryptedObject = encryptMessage(context, senderPublickey, message.toByteArray());
            ByteString encryptedMessage = ByteString.copyFrom(encryptedObject.getEncryptedData());
            Protos.Envelope envelope = createEnvelope(encryptedMessage, MessageType.LINK, ByteString.copyFrom(pubKey)
                    , ByteString.copyFrom(senderPublickey), BRSharedPrefs.getDeviceId(context), ByteString.copyFrom(encryptedObject.getNonce()));
            BRCoreKey pairingKey = getPairingKey(context, senderPublickey, pairingObject.getId().getBytes());
            byte[] signature = pairingKey.compactSign(envelope.toByteArray());
            envelope = envelope.toBuilder().setSignature(ByteString.copyFrom(signature)).build();
            Log.e(TAG, "startPairing: " + envelope.toString());
            final byte[] envelopeData = envelope.toByteArray();
            BRExecutor.getInstance().forLightWeightBackgroundTasks().execute(new Runnable() {
                @Override
                public void run() {
                    MessageApi.sendEnvelope(context, envelopeData);
                }
            });
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }

    }

    public static void sendTestPing(Context context) {
        byte[] pubKey = Base58.decode("26qwjQdU35N8dDP2dAarAiwB2bRTktLDkcbTBnR4iHHwj");
        String identifier = "Mihail";
        ByteString message = Protos.Pong.newBuilder().setPong("Hi Dan, you're so awesome.").getPongBytes();
        BRCoreKey authKey = new BRCoreKey(BRKeyStore.getAuthKey(context));
        byte[] myPubKey = authKey.getPubKey();

        MessageType requestMessageType = MessageType.PING;
        EncryptedObject responseEncryptedObject = encryptMessage(context, pubKey, message.toByteArray());
        MessageType responseMessageType = getResponseMessageType(requestMessageType);
        BRCoreKey pairingKey = getPairingKey(context, pubKey, identifier.getBytes());
        Protos.Envelope envelope = createEnvelope(ByteString.copyFrom(responseEncryptedObject.getEncryptedData()),
                responseMessageType, ByteString.copyFrom(pubKey), ByteString.copyFrom(myPubKey),
                identifier, ByteString.copyFrom(responseEncryptedObject.getNonce()));
        byte[] responseSignature = pairingKey.compactSign(envelope.toByteArray());
        envelope = envelope.toBuilder().setSignature(ByteString.copyFrom(responseSignature)).build();
        MessageApi.sendEnvelope(context, envelope.toByteArray());
    }

}
