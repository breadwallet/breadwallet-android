package com.breadwallet.protocols.messageexchange;

import android.app.IntentService;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Parcelable;
import android.support.annotation.VisibleForTesting;
import android.support.annotation.WorkerThread;
import android.util.Base64;
import android.util.Log;

import com.breadwallet.BreadApp;
import com.breadwallet.core.BRCoreKey;
import com.breadwallet.protocols.messageexchange.entities.EncryptedMessage;
import com.breadwallet.protocols.messageexchange.entities.InboxEntry;
import com.breadwallet.protocols.messageexchange.entities.PairingMetaData;
import com.breadwallet.protocols.messageexchange.entities.RequestMetaData;
import com.breadwallet.tools.crypto.Base58;
import com.breadwallet.tools.crypto.CryptoHelper;
import com.breadwallet.tools.manager.BRSharedPrefs;
import com.breadwallet.tools.security.BRKeyStore;
import com.breadwallet.tools.util.Utils;
import com.breadwallet.wallet.WalletsMaster;
import com.breadwallet.wallet.abstracts.BaseWalletManager;
import com.breadwallet.wallet.wallets.CryptoTransaction;
import com.google.protobuf.ByteString;
import com.google.protobuf.InvalidProtocolBufferException;

import java.io.UnsupportedEncodingException;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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

public final class MessageExchangeService extends IntentService {
    private static final String TAG = MessageExchangeService.class.getSimpleName();
    private static byte[] mSenderId = null;
    private static byte[] mPairingPublicKey = null;
    private static final int NONCE_SIZE = 12;

    public static final int ENVELOPE_VERSION = 1;
    public static final String SERVICE_PWB = "PWB";

    // TODO: REMOVE
    public static final boolean USE_NEW_CODE = false;

    private enum MessageType {
        LINK,
        PING,
        PONG,
        ACCOUNT_REQUEST,
        ACCOUNT_RESPONSE,
        PAYMENT_REQUEST,
        PAYMENT_RESPONSE,
        CALL_REQUEST,
        CALL_RESPONSE
    }

    /**
     * The {@link MessageExchangeService} is responsible for retrieving encrypted messages from the server which
     * are ultimately from another wallet.
     */
    public MessageExchangeService() {
        super(TAG);
    }

    private static Protos.Envelope createEnvelope(ByteString encryptedMessage, MessageType messageType,
                                                 ByteString senderPublicKey, ByteString receiverPublicKey,
                                                 String uniqueId, ByteString nonce) {
        Protos.Envelope envelope = Protos.Envelope.newBuilder()
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

    //Deprecated
    public static BRCoreKey getPairingKey(byte[] authKey, byte[] id) {
        if (Utils.isNullOrEmpty(authKey) || Utils.isNullOrEmpty(id)) {
            Log.e(TAG, "pairWallet: invalid query parameters");
            return null;
        }
        return new BRCoreKey(authKey).getPairingKey(id);
    }

    public static EncryptedMessage encrypt(BRCoreKey pairingKey, byte[] senderPublicKey, byte[] data) {
        byte[] nonce = generateRandomNonce();
        byte[] encryptedData = pairingKey.encryptUsingSharedSecret(senderPublicKey, data, nonce);
        return new EncryptedMessage(encryptedData, nonce);
    }

    // DEPRECATE
    public static byte[] decrypt(BRCoreKey pairingKey, byte[] senderPublicKey, byte[] encryptedMessage, byte[] nonce) {
        return pairingKey.decryptUsingSharedSecret(senderPublicKey, encryptedMessage, nonce);
    }

    // DEPRECATE
    public static void processInboxMessages(Context context, List<InboxEntry> inboxEntries) {
        Log.e(TAG, "processInboxMessages: " + inboxEntries.size());
        List<String> cursors = new ArrayList<>();
        for (InboxEntry inboxEntry : inboxEntries) {
            Protos.Envelope requestEnvelope = getEnvelopeFromInbox(inboxEntry);
            boolean isEnvelopValid = verifyEnvelopeSignature(requestEnvelope);
            if (!isEnvelopValid) {
                Log.e(TAG, "createResponseEnvelope: verifyEnvelope failed!!!");
                continue;
            }
            byte[] authKey = BRKeyStore.getAuthKey(context);
            BRCoreKey pairingKey = getPairingKey(authKey, mSenderId);

            Protos.Envelope responseEnvelope = createResponseEnvelope(context, pairingKey, requestEnvelope);
            if (responseEnvelope == null) {
                Log.e(TAG, "processInboxMessages: responseEnvelope is null!");
                continue;
            }
            cursors.add(inboxEntry.getCursor());
            MessageExchangeNetworkHelper.sendEnvelope(context, responseEnvelope.toByteArray());
        }
        MessageExchangeNetworkHelper.sendAck(context, cursors);
    }

    // DEPRECATE
    public static void checkInboxAndRespond(Context context) {
        List<InboxEntry> inboxEntries = MessageExchangeNetworkHelper.fetchInbox(context);
        processInboxMessages(context, inboxEntries);
    }

    /**
     * Verifies the specified envelope's signature.
     *
     * @param envelope The envelope whose signature will be verified.
     * @return True, if the signature is valid; false, otherwise.
     */
    public static boolean verifyEnvelopeSignature(Protos.Envelope envelope) {
        byte[] signature = envelope.getSignature().toByteArray();
        byte[] senderPublicKey = envelope.getSenderPublicKey().toByteArray();
        envelope = envelope.toBuilder().setSignature(ByteString.EMPTY).build();
        byte[] envelopeDoubleSha256 = CryptoHelper.doubleSha256(envelope.toByteArray());
        if (Utils.isNullOrEmpty(signature)) {
            Log.e(TAG, "verifyEnvelope: signature missing.");
            return false;
        }
        BRCoreKey key = BRCoreKey.compactSignRecoverKey(envelopeDoubleSha256, signature);
        byte[] recoveredPubKey = key.getPubKey();
        return Arrays.equals(senderPublicKey, recoveredPubKey);
    }

    // Deprecated
    public static Protos.Envelope createResponseEnvelope(Context context, BRCoreKey pairingKey, Protos.Envelope requestEnvelope) {
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
            byte[] decryptedMessageBytes = decrypt(pairingKey, senderPublicKey.toByteArray(), encryptedMessage.toByteArray(), nonce.toByteArray());
            MessageType requestMessageType = MessageType.valueOf(messageType);
            ByteString messageResponse = generateResponseMessage(decryptedMessageBytes, requestMessageType);
            EncryptedMessage responseEncryptedMessage = encrypt(pairingKey, senderPublicKey.toByteArray(), messageResponse.toByteArray());
            MessageType responseMessageType = getResponseMessageType(requestMessageType);
            Protos.Envelope responseEnvelope = createEnvelope(ByteString.copyFrom(responseEncryptedMessage.getEncryptedData()),
                    responseMessageType, ByteString.copyFrom(pairingKey.getPubKey()), senderPublicKey, identifier,
                    ByteString.copyFrom(responseEncryptedMessage.getNonce()));
            byte[] responseSignature = pairingKey.compactSign(CryptoHelper.doubleSha256(responseEnvelope.toByteArray()));
            responseEnvelope = responseEnvelope.toBuilder().setSignature(ByteString.copyFrom(responseSignature)).build();
            return responseEnvelope;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    // Deprecated
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

    // Deprecated
    private static ByteString generateResponseMessage(byte[] requestDecryptedMessage, MessageType requestMessageType) {
        try {
            switch (requestMessageType) {
                case LINK:
                    Protos.Link linkMessage = Protos.Link.parseFrom(requestDecryptedMessage);

                    //Save id and pubkey for pairing key generation.
                    mSenderId = linkMessage.getId().toByteArray();
                    mPairingPublicKey = linkMessage.getPublicKey().toByteArray();
                    return null;
                case PING:
                    return Protos.Pong.newBuilder().setPong("This is the message of the day.").build().getPongBytes();
                case ACCOUNT_REQUEST:
                    return generateAccountResponse(requestDecryptedMessage);
                case PAYMENT_REQUEST:
                    return generatePaymentResponse(requestDecryptedMessage);
                case CALL_REQUEST:
                    return generateCallResponse(requestDecryptedMessage);
                default:
                    Log.e(TAG, "Request type not recognized:" + requestMessageType.name());
            }
        } catch (InvalidProtocolBufferException e) {
            Log.e(TAG, "Error parsing protobuf for " + requestMessageType.name() + "message.", e);
            // TODO: SHIV re-throw?
        }
        return null;
    }

    // DEPRECATE
    private static ByteString createLinkMessage(byte[] pubKey, Protos.Status status, ByteString senderId) {
        return Protos.Link.newBuilder().setPublicKey(ByteString.copyFrom(pubKey))
                .setStatus(status).setId(senderId).build().toByteString();
    }

    // DEPRECATE
    @VisibleForTesting
    protected static ByteString generateAccountResponse(byte[] requestDecryptedMessage) throws InvalidProtocolBufferException {
        Protos.AccountRequest request = Protos.AccountRequest.parseFrom(requestDecryptedMessage);

        String currencyCode = request.getScope();
        Context context = BreadApp.getBreadContext(); // TODO: SHIV remove once we have a service.
        BaseWalletManager walletManager = WalletsMaster.getInstance(context).getWalletByIso(context, currencyCode);

        Protos.AccountResponse.Builder responseBuilder = Protos.AccountResponse.newBuilder().setScope(currencyCode);
        if (walletManager == null) {
            responseBuilder.setStatus(Protos.Status.REJECTED);
        } else {
            String address = walletManager.getAddress();

            if (address == null) {
                responseBuilder.setStatus(Protos.Status.REJECTED);
            } else {
                responseBuilder.setAddress(address)
                        .setStatus(Protos.Status.ACCEPTED);
            }
        }

        return responseBuilder.build().toByteString();
    }

    // DEPRECATED
    private static ByteString generatePaymentResponse(byte[] requestDecryptedMessage) throws InvalidProtocolBufferException {
        Protos.PaymentRequest request = Protos.PaymentRequest.parseFrom(requestDecryptedMessage);
        String currencyCode = request.getScope();
        Protos.PaymentResponse.Builder responseBuilder = Protos.PaymentResponse.newBuilder().setScope(currencyCode);
        return responseBuilder.build().toByteString();
    }

    // DEPRECATED
    private static ByteString generateCallResponse(byte[] requestDecryptedMessage) throws InvalidProtocolBufferException {
        Protos.CallRequest request = Protos.CallRequest.parseFrom(requestDecryptedMessage);
        String currencyCode = request.getScope();
        Protos.CallResponse.Builder responseBuilder = Protos.CallResponse.newBuilder().setScope(currencyCode);
        return responseBuilder.build().toByteString();
    }


    // DEPRECATED
    @WorkerThread
    public static void startPairing(final Context context, PairingMetaData pairingMetaData) {
        try {
            byte[] ephemeralKey = BRCoreKey.decodeHex(pairingMetaData.getPublicKeyHex());
            mSenderId = pairingMetaData.getId().getBytes();
            byte[] authKey = BRKeyStore.getAuthKey(context);
            BRCoreKey pairingKey = getPairingKey(authKey, mSenderId);
            ByteString message = createLinkMessage(pairingKey.getPubKey(), Protos.Status.ACCEPTED, ByteString.copyFrom(pairingMetaData.getId(), StandardCharsets.UTF_8.name()));
            MessageExchangeNetworkHelper.sendAssociatedKey(context, pairingKey.getPubKey());

            EncryptedMessage encryptedMessage = encrypt(pairingKey, ephemeralKey, message.toByteArray());
            ByteString encryptedMessageByteString = ByteString.copyFrom(encryptedMessage.getEncryptedData());
            Protos.Envelope envelope = createEnvelope(encryptedMessageByteString, MessageType.LINK, ByteString.copyFrom(pairingKey.getPubKey())
                    , ByteString.copyFrom(ephemeralKey), BRSharedPrefs.getDeviceId(context), ByteString.copyFrom(encryptedMessage.getNonce()));

            byte[] signature = pairingKey.compactSign(CryptoHelper.doubleSha256(envelope.toByteArray()));
            envelope = envelope.toBuilder().setSignature(ByteString.copyFrom(signature)).build();
            Log.e(TAG, "startPairing: " + envelope.toString());
            final byte[] envelopeData = envelope.toByteArray();
            //network call
            MessageExchangeNetworkHelper.sendEnvelope(context, envelopeData);
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }

    }

    public static byte[] generateRandomNonce() {
        byte[] nonce = new byte[NONCE_SIZE];
        new SecureRandom().nextBytes(nonce);
        return nonce;
    }

    // REMOVE
    public static void sendTestPing(Context context) {
        Log.e(TAG, "sendTestPing: sending");
        byte[] pubKey = Base58.decode("iRbNunsoS9P3VeGg4T2qMsXgxd3zaveva47v4vgKtQ2m");
        String identifier = "Mihail";
        ByteString message = Protos.Ping.newBuilder().setPing("Hi Dan, you're so awesome.").build().getPingBytes();
        BRCoreKey authKey = new BRCoreKey(BRKeyStore.getAuthKey(context));
        byte[] myPubKey = authKey.getPubKey();

        MessageType requestMessageType = MessageType.PING;
        EncryptedMessage responseEncryptedMessage = encrypt(authKey, pubKey, message.toByteArray());
        BRCoreKey privateKey = new BRCoreKey(BRKeyStore.getAuthKey(context));
        Protos.Envelope envelope = createEnvelope(ByteString.copyFrom(responseEncryptedMessage.getEncryptedData()),
                requestMessageType, ByteString.copyFrom(myPubKey), ByteString.copyFrom(pubKey),
                identifier, ByteString.copyFrom(responseEncryptedMessage.getNonce()));
        byte[] responseSignature = privateKey.compactSign(CryptoHelper.doubleSha256(envelope.toByteArray()));
        envelope = envelope.toBuilder().setSignature(ByteString.copyFrom(responseSignature)).build();
        MessageExchangeNetworkHelper.sendEnvelope(context, envelope.toByteArray());
        Log.e(TAG, "sendTestPing: DONE.");
    }

    /***********************************************************************************************
     *                       EVENTUALLY FINAL BUT CURRENTLY WIP BELOW THIS                         *
     ***********************************************************************************************
     */

    public static final String ACTION_REQUEST_TO_PAIR = "com.breadwallet.protocols.messageexchange.ACTION_REQUEST_TO_PAIR";
    public static final String ACTION_RETRIEVE_MESSAGES = "com.breadwallet.protocols.messageexchange.ACTION_RETRIEVE_MESSAGES";
    private static final String ACTION_PROCESS_PAIR_REQUEST = "com.breadwallet.protocols.messageexchange.ACTION_PROCESS_PAIR_REQUEST";
    private static final String ACTION_PROCESS_REQUEST = "com.breadwallet.protocols.messageexchange.ACTION_PROCESS_REQUEST";
    public static final String ACTION_GET_USER_CONFIRMATION = "com.breadwallet.protocols.messageexchange.ACTION_GET_USER_CONFIRMATION";
    //    public static final String EXTRA_REQUEST_ID = "com.breadwallet.protocols.messageexchange.EXTRA_REQUEST_ID";
    private static final String EXTRA_IS_USER_APPROVED = "com.breadwallet.protocols.messageexchange.EXTRA_IS_USER_APPROVED";
    private static final String EXTRA_METADATA = "com.breadwallet.protocols.messageexchange.EXTRA_METADATA";

    // TODO: these should be stored in the DB in case of app restart or crash.
    private Map<String, Protos.Envelope> mPendingRequests = new HashMap<>();
    private PairingMetaData mPairingMetaData;

//    public enum ServiceCapability implements Parcelable {
//        ACCOUNT,
//        PAYMENT,
//        CALL,
//    }

    /**
     * Handles intents passed to the {@link MessageExchangeService} by creating a new worker thread to complete the work required.
     *
     * @param intent The intent specifying the work that needs to be completed.
     */
    @Override
    protected void onHandleIntent(Intent intent) {
        if (intent != null) {
            switch (intent.getAction()) {
                case ACTION_REQUEST_TO_PAIR:
                    // User scanned QR, to initiate pairing with a remote wallet,
                    mPairingMetaData = intent.getParcelableExtra(EXTRA_METADATA);

                    // Show more details about the pairing and ask the user to confirm.
                    //confirmRequest(null); // TODO METADATA, include link type
                    pair(true); //TODO TESTING
                    break;
                case ACTION_PROCESS_PAIR_REQUEST:
                    // User has approved or denied the pairing request after reviewing the details.
                    pair(intent.getBooleanExtra(EXTRA_IS_USER_APPROVED, false));
                    break;
                case ACTION_RETRIEVE_MESSAGES:
                    retrieveInboxEntries(this);
                    break;
                case ACTION_PROCESS_REQUEST:
                    boolean isUserApproved = intent.getBooleanExtra(EXTRA_IS_USER_APPROVED, false);
                    RequestMetaData requestMetaData = intent.getParcelableExtra(EXTRA_METADATA);
                    if (requestMetaData != null) {
                        processAsyncRequest(requestMetaData, isUserApproved);
                    } else {
                        Log.e(TAG, "Missing request meta data.  Ignoring intent.");
                    }
                    break;
                default:
                    Log.d(TAG, "Intent not recognized.");
            }
        }
    }

    public static Intent createIntent(Context context, String action) {
        Intent intent = new Intent(context, MessageExchangeService.class);
        intent.setAction(action);
        return intent;
    }

    public static Intent createIntent(Context context, String action, Parcelable parcelable) {
        Intent intent = new Intent(context, MessageExchangeService.class);
        intent.setAction(action)
                .putExtra(EXTRA_METADATA, parcelable);
        return intent;
    }

    /**
     * Creates an intent with the specified parameters for the {@link ConfirmationActivity} to start this service.
     * Used for: Link.
     *
     * @param context        The context in which we are operating.
     * @param isUserApproved True, if the user approved the pending request; false, otherwise.
     * @return An intent with the specified parameters.
     */
    public static Intent createIntent(Context context, boolean isUserApproved) {
        Intent intent = new Intent(context, MessageExchangeService.class);
        intent.setAction(ACTION_PROCESS_PAIR_REQUEST)
                .putExtra(EXTRA_IS_USER_APPROVED, isUserApproved);
        return intent;
    }

    /**
     * Creates an intent with the specified parameters for the {@link ConfirmationActivity} to start this service.
     * Used for: Payment Request, Call Request.
     *
     * @param context        The context in which we are operating.
     * @param parcelable     The parcelable containing meta data needed when the service starts again.
     * @param isUserApproved True, if the user approved the pending request; false, otherwise.
     * @return An intent with the specified parameters.
     */
    public static Intent createIntent(Context context, Parcelable parcelable, boolean isUserApproved) {
        Intent intent = new Intent(context, MessageExchangeService.class);
        intent.setAction(ACTION_PROCESS_REQUEST)
                .putExtra(EXTRA_METADATA, parcelable)
                .putExtra(EXTRA_IS_USER_APPROVED, isUserApproved);
        return intent;
    }

    public void pair(boolean isUserApproved) {
        try {
            byte[] ephemeralKey = BRCoreKey.decodeHex(mPairingMetaData.getPublicKeyHex());
            mSenderId = mPairingMetaData.getId().getBytes(); // TODO fix
            BRCoreKey pairingKey = getPairingKey();

            ByteString message;
            if (isUserApproved) {
                // TODO: I think this should be this wallet's id, not the remote entity's id, no?
                message = createLink(ByteString.copyFrom(pairingKey.getPubKey()), ByteString.copyFrom(mPairingMetaData.getId(), StandardCharsets.UTF_8.name()));
                MessageExchangeNetworkHelper.sendAssociatedKey(this, pairingKey.getPubKey());
            } else {
                message = createLink(Protos.Error.USER_DENIED);
            }

            EncryptedMessage encryptedMessage = encrypt(pairingKey, ephemeralKey, message.toByteArray());
            ByteString encryptedMessageByteString = ByteString.copyFrom(encryptedMessage.getEncryptedData());
            Protos.Envelope envelope = createEnvelope(encryptedMessageByteString, MessageType.LINK, ByteString.copyFrom(pairingKey.getPubKey()),
                    ByteString.copyFrom(ephemeralKey), BRSharedPrefs.getDeviceId(this), ByteString.copyFrom(encryptedMessage.getNonce()));
            //TODO: Shouldn't the message identifier be a new uuid? For requests, it should be new according to the spec.

            byte[] signature = pairingKey.compactSign(CryptoHelper.doubleSha256(envelope.toByteArray()));
            envelope = envelope.toBuilder().setSignature(ByteString.copyFrom(signature)).build();
            Log.d(TAG, "pair: request envelope:" + envelope.toString());

            // Send link request to remote wallet.
            MessageExchangeNetworkHelper.sendEnvelope(this, envelope.toByteArray());
        } catch (UnsupportedEncodingException e) {
            Log.e(TAG, "Failed to pair", e);
        }

    }

    /**
     * Retrieves the latest batch of messages from the server.
     *
     * @param context The context in which we are operating.
     */
    private void retrieveInboxEntries(Context context) {
        List<InboxEntry> inboxEntries = MessageExchangeNetworkHelper.fetchInbox(context);
        int numOfInboxEntries = inboxEntries.size();
        Log.d(TAG, "retrieveInboxEntries: " + numOfInboxEntries);

// TODO: CHECK THESE
//        int version = requestEnvelope.getVersion();
//        String service = requestEnvelope.getService();

        if (numOfInboxEntries > 0) {
            List<String> cursors = new ArrayList<>();
            for (InboxEntry inboxEntry : inboxEntries) {
                Protos.Envelope envelope = getEnvelopeFromInboxEntry(inboxEntry);
                String cursor = inboxEntry.getCursor();
                if (verifyEnvelopeSignature(envelope)) {
                    processEnvelope(cursor, envelope);
                    cursors.add(cursor);
                } else {
                    Log.e(TAG, "retrieveInboxEntries: signature verification failed. id: " + cursor);
                }
            }

            MessageExchangeNetworkHelper.sendAck(context, cursors);
        }
    }

    private static Protos.Envelope getEnvelopeFromInboxEntry(InboxEntry entry) {
        try {
            byte[] envelopeData = Base64.decode(entry.getMessage(), Base64.NO_WRAP);
            return Protos.Envelope.parseFrom(envelopeData);
        } catch (InvalidProtocolBufferException e) {
            Log.e(TAG, "Error decoding envelope. InboxEntry cursor: " + entry.getCursor(), e);
            return null;
        }
    }

    private void processEnvelope(String cursor, Protos.Envelope envelope) {
        byte[] decryptedMessage = decrypt(envelope);
        MessageType messageType = MessageType.valueOf(envelope.getMessageType());
        RequestMetaData metaData;

        //TODO: SHIV Add go metaData: PAYMENT_METHOD_NAME - Ethereum, PAYMENT_METHOS_CURRENCY_CODE - ETH // use for displaying the price, fee, total
        //DELIVERY_TIME, PRICE, FEE, TOTAL
        try {
            switch (messageType) {
                case LINK:
                    processSyncRequest(messageType, envelope, decryptedMessage);
                    break;
                case PING:
                    processSyncRequest(messageType, envelope, decryptedMessage);
                    break;
                case ACCOUNT_REQUEST:
                    processSyncRequest(messageType, envelope, decryptedMessage);
                    break;
                case PAYMENT_REQUEST:
                    // These are asynchronous requests.  Save until some event happens.
                    // Use cursor # to identify this request.
                    mPendingRequests.put(cursor, envelope);
                    Protos.PaymentRequest paymentRequest = Protos.PaymentRequest.parseFrom(decryptedMessage);
                    metaData = new RequestMetaData(cursor, paymentRequest.getScope(), paymentRequest.getNetwork(),
                            paymentRequest.getAddress(), paymentRequest.getAmount(), paymentRequest.getMemo());
                    confirmRequest(metaData);
                    break;
                case CALL_REQUEST:
                    // These are asynchronous requests.  Save until some event happens.
                    // Use cursor # to identify this request.
                    mPendingRequests.put(cursor, envelope);
                    Protos.CallRequest callRequest = Protos.CallRequest.parseFrom(decryptedMessage);
                    metaData = new RequestMetaData(cursor, callRequest.getScope(), callRequest.getNetwork(),
                            callRequest.getAddress(), callRequest.getAmount(), callRequest.getMemo());
                    confirmRequest(metaData);
                    break;
                default:
                    Log.e(TAG, "RequestMetaData type not recognized:" + messageType.name());
            }
        } catch (InvalidProtocolBufferException e) {
            Log.e(TAG, "Error parsing protobuf for " + messageType.name() + "message.", e);
            // TODO: SHIV re-throw?
        }
    }

    private void processSyncRequest(MessageType messageType, Protos.Envelope incomingEnvelope, byte[] decryptedMessage)
            throws InvalidProtocolBufferException {
        ByteString messageResponse;
        switch (messageType) {
            case LINK:
                processLink(incomingEnvelope, decryptedMessage);
            case PING:
                Protos.Ping ping = Protos.Ping.parseFrom(decryptedMessage);
                messageResponse = Protos.Pong.newBuilder().setPong(ping.getPing()).build().toByteString();
                break;
            case ACCOUNT_REQUEST:
                Protos.AccountRequest request = Protos.AccountRequest.parseFrom(decryptedMessage);
                messageResponse = processAccountRequest(request);
                break;
            default:
                // Should never happen because unknown message type is handled by the caller (processRequest()).
                throw new IllegalArgumentException("Request type unknown: " + messageType.name());
        }

        Protos.Envelope outgoingEnvelope = createResponseEnvelope(incomingEnvelope, messageResponse);
        MessageExchangeNetworkHelper.sendEnvelope(this, outgoingEnvelope.toByteArray());
    }

    private void processAsyncRequest(RequestMetaData requestMetaData, boolean isUserApproved) {
        Protos.Envelope requestEnvelope = mPendingRequests.get(requestMetaData.getId());
        MessageType messageType = MessageType.valueOf(requestEnvelope.getMessageType());

        ByteString messageResponse;
        switch (messageType) {
            case PAYMENT_REQUEST:
                messageResponse = processPaymentRequest(requestMetaData, isUserApproved);
                break;
            case CALL_REQUEST:
                messageResponse = processCallRequest(requestMetaData, isUserApproved);
                break;
            default:
                // Should never happen because unknown message type is handled by the caller (processRequest()).
                throw new IllegalArgumentException("Request type unknown: " + messageType.name());
        }

        sendResponse(requestEnvelope, messageResponse);
    }

    private BRCoreKey getPairingKey() {
        byte[] authKey = BRKeyStore.getAuthKey(this);
        if (Utils.isNullOrEmpty(authKey) || Utils.isNullOrEmpty(mSenderId)) {
            Log.e(TAG, "getPairingKey: Auth key or sender id is null.");
            return null;
        }
        return new BRCoreKey(authKey).getPairingKey(mSenderId);
    }

    private byte[] decrypt(Protos.Envelope requestEnvelope) {
        BRCoreKey pairingKey = getPairingKey();
        return pairingKey.decryptUsingSharedSecret(requestEnvelope.getSenderPublicKey().toByteArray(),
                requestEnvelope.getEncryptedMessage().toByteArray(),
                requestEnvelope.getNonce().toByteArray());
    }

    private Protos.Envelope createResponseEnvelope(Protos.Envelope requestEnvelope, ByteString messageResponse) {
        // Encrypt the response message.
        BRCoreKey pairingKey = getPairingKey();
        ByteString senderPublicKey = requestEnvelope.getSenderPublicKey();
        EncryptedMessage responseEncryptedMessage = encrypt(pairingKey, senderPublicKey.toByteArray(), messageResponse.toByteArray());

        // Determine type for response.
        MessageType requestMessageType = MessageType.valueOf(requestEnvelope.getMessageType());
        MessageType responseMessageType = getResponseMessageType(requestMessageType);

        // Create message envelope sans signature.
        Protos.Envelope responseEnvelope = createEnvelope(ByteString.copyFrom(responseEncryptedMessage.getEncryptedData()),
                responseMessageType, ByteString.copyFrom(pairingKey.getPubKey()), senderPublicKey, requestEnvelope.getIdentifier(),
                ByteString.copyFrom(responseEncryptedMessage.getNonce()));

        // Sign message envelope.
        byte[] responseSignature = pairingKey.compactSign(CryptoHelper.doubleSha256(responseEnvelope.toByteArray()));

        // Add signature to message envelope.
        return responseEnvelope.toBuilder().setSignature(ByteString.copyFrom(responseSignature)).build();
    }

    private void sendResponse(Protos.Envelope requestEnvelope, ByteString messageResponse) {
        Protos.Envelope envelope = createResponseEnvelope(requestEnvelope, messageResponse);
        MessageExchangeNetworkHelper.sendEnvelope(this, envelope.toByteArray());
    }

    /**
     * Prompts the user to confirm the pending remote request.
     *
     * @param requestMetaData The meta data related to the request.
     */
    private void confirmRequest(RequestMetaData requestMetaData) {
        Intent intent = new Intent(this, MessageExchangeService.class); // TODO: SHIV Add name of Jade's activity
        intent.setAction(ACTION_GET_USER_CONFIRMATION);
        Bundle bundle = new Bundle();
        bundle.putParcelable(EXTRA_METADATA, requestMetaData);
        startActivity(intent, bundle);
    }

    private ByteString createLink(ByteString pubKey, ByteString id) {
        Protos.Link.Builder responseBuilder = Protos.Link.newBuilder();
        responseBuilder.setPublicKey(pubKey)
                .setId(id)
                .setStatus(Protos.Status.ACCEPTED);

        return responseBuilder.build().toByteString();
    }

    private ByteString createLink(Protos.Error error) {
        Protos.Link.Builder responseBuilder = Protos.Link.newBuilder();
        responseBuilder.setStatus(Protos.Status.REJECTED)
                .setError(error);

        return responseBuilder.build().toByteString();
    }

    private void processLink(Protos.Envelope envelope, byte[] decryptedMessage) throws InvalidProtocolBufferException {
        Protos.Link link = Protos.Link.parseFrom(decryptedMessage);
        String senderId = mPairingMetaData.getId();
        if (senderId == null || !senderId.equals(link.getId())) {
            // If the response for the remote entity doesn't have the same id as the initial pairing request, reject.
            // TODO send response encrypted with ephemral key because we rejected the pairing key.
            // This is wrong:
            sendResponse(envelope, createLink(Protos.Error.REMOTE_ID_MISMATCH));
        }
    }

    private ByteString processAccountRequest(Protos.AccountRequest request) {
        String currencyCode = request.getScope();
        BaseWalletManager walletManager = WalletsMaster.getInstance(this).getWalletByIso(this, currencyCode);

        Protos.AccountResponse.Builder responseBuilder = Protos.AccountResponse.newBuilder();
        if (walletManager == null) {
            responseBuilder.setStatus(Protos.Status.REJECTED)
                    .setError(Protos.Error.SCOPE_UNKNOWN);
        } else {
            String address = walletManager.getAddress();
            if (address == null) {
                responseBuilder.setStatus(Protos.Status.REJECTED)
                        .setError(Protos.Error.NO_ADDRESS_FOUND);
            } else {
                responseBuilder.setScope(currencyCode)
                        .setAddress(address)
                        .setStatus(Protos.Status.ACCEPTED);
            }
        }

        return responseBuilder.build().toByteString();
    }

    private ByteString processPaymentRequest(RequestMetaData requestMetaData, boolean isUserApproved) {
        Protos.PaymentResponse.Builder responseBuilder = Protos.PaymentResponse.newBuilder();
        if (isUserApproved) {
            String currencyCode = requestMetaData.getCurrencyCode();
            BaseWalletManager walletManager = WalletsMaster.getInstance(this).getWalletByIso(this, currencyCode);

            boolean transactionSuccessful = false;
            // TODO: SHIV transact using requestMetaData.
            CryptoTransaction transaction = walletManager.createTransaction(new BigDecimal(requestMetaData.getAmount()), requestMetaData.getAddress());
            // Investigate PostAuth.getInstance().onPublishTxAuth(); ???

            if (transactionSuccessful) {
                responseBuilder.setScope(currencyCode)
                        .setStatus(Protos.Status.ACCEPTED)
                        .setTransactionId(transaction.getHash());
            } else {
                responseBuilder.setStatus(Protos.Status.REJECTED)
                        .setError(Protos.Error.TRANSACTION_FAILED);
            }
        } else {
            responseBuilder.setStatus(Protos.Status.REJECTED)
                        .setError(Protos.Error.USER_DENIED);
        }

        return responseBuilder.build().toByteString();
    }

    private ByteString processCallRequest(RequestMetaData requestMetaData, boolean isUserApproved) {
        Protos.CallResponse.Builder responseBuilder = Protos.CallResponse.newBuilder();
        if (isUserApproved) {
            String currencyCode = requestMetaData.getCurrencyCode();
            BaseWalletManager walletManager = WalletsMaster.getInstance(this).getWalletByIso(this, currencyCode);

            boolean transactionSuccessful = false;
            // TODO: SHIV transact using requestMetaData.
            CryptoTransaction transaction = walletManager.createTransaction(new BigDecimal(requestMetaData.getAmount()), requestMetaData.getAddress());
            // Investigate PostAuth.getInstance().onPublishTxAuth(); ???

            if (transactionSuccessful) {
                responseBuilder.setScope(currencyCode)
                        .setStatus(Protos.Status.ACCEPTED)
                        .setTransactionId(transaction.getHash());
            } else {
                responseBuilder.setStatus(Protos.Status.REJECTED)
                        .setError(Protos.Error.TRANSACTION_FAILED);
            }
        } else {
            responseBuilder.setStatus(Protos.Status.REJECTED)
                    .setError(Protos.Error.USER_DENIED);
        }

        return responseBuilder.build().toByteString();
    }
}
