/*
 * EthereumLightNode
 *
 * Created by Ed Gamble <ed@breadwallet.com> on 3/7/18.
 * Copyright (c) 2018 breadwallet LLC
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package com.breadwallet.core.ethereum;

import com.breadwallet.core.BRCoreJniReference;
import java.lang.ref.WeakReference;
import java.util.HashMap;
import java.util.Map;

import static com.breadwallet.core.ethereum.BREthereumToken.jniGetTokenBRD;
import static com.breadwallet.core.ethereum.BREthereumToken.jniTokenAll;

/**
 *
 */
public class BREthereumLightNode extends BRCoreJniReference {

    //
    // Client
    //
    public interface Client {
        //        typedef void (*BREthereumClientHandlerGetBalance) (BREthereumClientContext context,
        //                                   BREthereumLightNode node,
        //                                   BREthereumWalletId wid,
        //                                   const char *address,
        //                                   int rid);
        void getBalance(int wid, String address, int rid);

        //        typedef void (*BREthereumClientHandlerGetGasPrice) (BREthereumClientContext context,
        //                                    BREthereumLightNode node,
        //                                    BREthereumWalletId wid,
        //                                    int rid);
        void getGasPrice(int wid, int rid);

        //        typedef void (*BREthereumClientHandlerEstimateGas) (BREthereumClientContext context,
        //                                    BREthereumLightNode node,
        //                                    BREthereumWalletId wid,
        //                                    BREthereumTransactionId tid,
        //                                    const char *to,
        //                                    const char *amount,
        //                                    const char *data,
        //                                    int rid);

        void getGasEstimate(int wid, int tid, String to, String amount, String data, int rid);

        //        typedef void (*BREthereumClientHandlerSubmitTransaction) (BREthereumClientContext context,
        //                                          BREthereumLightNode node,
        //                                          BREthereumWalletId wid,
        //                                          BREthereumTransactionId tid,
        //                                          const char *transaction,
        //                                          int rid);
        void submitTransaction(int wid, int tid, String rawTransaction, int rid);

        //        typedef void (*BREthereumClientHandlerGetTransactions) (BREthereumClientContext context,
        //                                        BREthereumLightNode node,
        //                                        const char *address,
        //                                        int rid);
        void getTransactions(String address, int rid);

        //        typedef void (*BREthereumClientHandlerGetLogs) (BREthereumClientContext context,
        //                                BREthereumLightNode node,
        //                                const char *contract,
        //                                const char *address,
        //                                const char *event,
        //                                int rid);
        void getLogs (String contract, String address, String event, int rid);

        //        typedef void (*BREthereumClientHandlerGetBlockNumber) (BREthereumClientContext context,
        //                                                    BREthereumLightNode node,
        //                                                    int rid);
        void getBlockNumber (int rid);

        //        typedef void (*BREthereumClientHandlerGetNonce) (BREthereumClientContext context,
        //                                                        BREthereumLightNode node,
        //                                                        const char *address,
        //                                                        int rid);
        void getNonce (String address, int rid);

    }

    //
    // Client Announcers
    //

    public void announceBalance(int wid, String balance, int rid) {
        jniAnnounceBalance(wid, balance, rid);
    }

    public void announceGasPrice(int wid, String gasPrice, int rid) {
        jniAnnounceGasPrice(wid, gasPrice, rid);
    }

    public void announceGasEstimate(int wid, int tid, String gasEstimate, int rid) {
        jniAnnounceGasEstimate(wid, tid, gasEstimate, rid);
    }

    public void announceSubmitTransaction(int wid, int tid, String hash, int rid) {
        jniAnnounceSubmitTransaction(wid, tid, hash, rid);
    }

    public void announceTransaction(int id,
                                    String hash,
                                    String from,
                                    String to,
                                    String contract,
                                    String amount, // value
                                    String gasLimit,
                                    String gasPrice,
                                    String data,
                                    String nonce,
                                    String gasUsed,
                                    String blockNumber,
                                    String blockHash,
                                    String blockConfirmations,
                                    String blockTransactionIndex,
                                    String blockTimestamp,
                                    // cumulative gas used,
                                    // confirmations
                                    // txreceipt_status
                                    String isError) {
        jniAnnounceTransaction(id, hash, from, to, contract, amount, gasLimit, gasPrice, data, nonce, gasUsed,
                blockNumber, blockHash, blockConfirmations, blockTransactionIndex, blockTimestamp,
                isError);
    }

    public void announceLog(int id,
                            String hash,
                            String contract,
                            String[] topics,
                            String data,
                            String gasPrice,
                            String gasUsed,
                            String logIndex,
                            String blockNumber,
                            String blockTransactionIndex,
                            String blockTimestamp) {
        jniAnnounceLog(id, hash, contract, topics, data, gasPrice, gasUsed, logIndex,
                blockNumber, blockTransactionIndex, blockTimestamp);
    }

    public void announceBlockNumber (String blockNumber, int rid) {
        jniAnnounceBlockNumber (blockNumber, rid);
    }

    public void announceNonce (String address, String nonce, int rid) {
        jniAnnounceNonce(address, nonce, rid);
    }

    //
    // Listener
    //
    // In the following the Event enumerations *must* match the corresponding declarations in
    // BREthereumLightNode.h - the enumerations values/indices must be identical.
    //
    public interface Listener {
        enum Status {
            SUCCESS,

            // Reference access
            ERROR_UNKNOWN_NODE,
            ERROR_UNKNOWN_TRANSACTION,
            ERROR_UNKNOWN_ACCOUNT,
            ERROR_UNKNOWN_WALLET,
            ERROR_UNKNOWN_BLOCK,
            ERROR_UNKNOWN_LISTENER,

            // Node
            ERROR_NODE_NOT_CONNECTED,

            // Transaction
            ERROR_TRANSACTION_X,

            // Acount
            // Wallet
            // Block
            // Listener

            // Numeric
            ERROR_NUMERIC_PARSE,
        }
        int NUMBER_OF_STATUS_EVENTS = 10;

        //
        // Wallet
        //
        enum WalletEvent {
            CREATED,
            BALANCE_UPDATED,
            DEFAULT_GAS_LIMIT_UPDATED,
            DEFAULT_GAS_PRICE_UPDATED,
            DELETED
        }
        int NUMBER_OF_WALLET_EVENTS = 5;

        void handleWalletEvent(BREthereumWallet wallet, WalletEvent event,
                               Status status,
                               String errorDescription);

        //
        // Block
        //
        enum BlockEvent {
            CREATED,
            DELETED
        }
        int NUMBER_OF_BLOCK_EVENT = 2;

        void handleBlockEvent(BREthereumBlock block, BlockEvent event,
                              Status status,
                              String errorDescription);

        //
        // Transaction
        //
        enum TransactionEvent {
            ADDED,
            REMOVED,

            CREATED,
            SIGNED,
            SUBMITTED,
            BLOCKED,  // aka confirmed
            ERRORED,
            GAS_ESTIMATE_UPDATED,
            BLOCK_CONFIRMATIONS_UPDATED
        }
        int NUMBER_OF_TRANSACTION_EVENTS = 9;

        void handleTransactionEvent(BREthereumWallet wallet,
                                    BREthereumTransaction transaction,
                                    TransactionEvent event,
                                    Status status,
                                    String errorDescription);
    }

    //
    // Light Node
    //
    WeakReference<Client> client;
    WeakReference<Listener> listener;

    //
    // Network
    //
    BREthereumNetwork network;

    public BREthereumNetwork getNetwork () {
        return network;
    }
    //
    // Account
    //
    BREthereumAccount account;

    public BREthereumAccount getAccount() {
        return account;
    }

    public String getAddress () {
        return account.getPrimaryAddress();
    }

    public byte[] getAddressPublicKey () {
        return account.getPrimaryAddressPublicKey();
    }

    //
    // Wallet
    //
    // We hold a mapping, from identifier to wallet, for all wallets held/managed by this node.
    // The Core already holds wallets and thus we don't actually need to 'duplicate' that
    // functionality; however, to not hold wallets here would mean that every getWallet(), every
    // event handler would need to create another Wallet (feeling like a 'value type').  We don't
    // do that - but we could, and might some day.
    //
    // We could hold a WeakReference (and we probably should) - but, at least with the current
    // Android app, we witnessed wallets being reclaimed between each event update.  The consequence
    // was that we re-created the wallets each time; only to have them reclaimed.  Now, that is
    // actually not that big a deal and it should disappear completely when the Android app holds
    // on to wallets that have transactions.
    //
    // Of course, if the wallet shows up, then it is in Core Ethereum, and it shouldn't be
    // a WeakReference() - since it clearly exists in Core.  We'll leave this as a string
    // reference and explicitly delete wallets on a 'DELETE' event.
    //
    protected Map<Long, BREthereumWallet> wallets = new HashMap<>();

    protected synchronized BREthereumWallet walletLookupOrCreate(long wid, BREthereumToken token) {
        BREthereumWallet wallet = wallets.get(wid);

        // If we never had a wallet, then create one.
        if (null == wallet) {

            // If `token` is null, then lookup the token for wallet.
            if (null == token) {
                long tokenRef = jniLightNodeWalletGetToken(wid);
                if (0 != tokenRef)
                    token = lookupTokenByReference (tokenRef);
            }

            wallet = (null == token
                    ? new BREthereumWallet(this, wid, account, network)
                    : new BREthereumWallet(this, wid, account, network, token));

            wallets.put(wid, wallet);

        }

        return wallet;
    }

    public BREthereumWallet getWallet () {
        long wid = jniLightNodeGetWallet();
        return walletLookupOrCreate (wid, null);
    }

    public BREthereumWallet getWallet(BREthereumToken token) {
        long wid = jniLightNodeGetWalletToken(token.getIdentifier());
        return walletLookupOrCreate(wid, token);
    }

    // TODO: Remove once 'client callbacks' are LightNode trampolines
    public BREthereumWallet getWalletByIdentifier (long wid) {
        return walletLookupOrCreate(wid, null);
    }

    //
    // Transaction
    //
    // We'll hold a mapping, from identifier to transaction, for all transactions.
    //
    protected Map<Long, WeakReference<BREthereumTransaction>> transactions = new HashMap<>();

    protected synchronized BREthereumTransaction transactionLookupOrCreate(long tid) {
        WeakReference<BREthereumTransaction> transactionRef = transactions.get(tid);

        if (null == transactionRef || null == transactionRef.get()) {
            long tokenReference = jniTransactionGetToken(tid);

            transactionRef = new WeakReference<>(
                    new BREthereumTransaction(this, tid,
                            (0 == tokenReference
                                    ? BREthereumAmount.Unit.ETHER_ETHER
                                    : BREthereumAmount.Unit.TOKEN_DECIMAL)));
            transactions.put(tid, transactionRef);
        }

        return transactionRef.get();
    }

    //
    // Block
    //
    protected Map<Long, BREthereumBlock> blocks = new HashMap<>();

    protected synchronized BREthereumBlock blockLookupOrCreate (long bid) {
        BREthereumBlock block = blocks.get(bid);

        if (null == block) {
            block = new BREthereumBlock(this, bid);
            blocks.put (bid, block);
        }
        return block;
    }

    public long getBlockHeight () {
        return jniLightNodeGetBlockHeight();
    }

    //
    // Tokens
    //
    protected final HashMap<String, BREthereumToken> tokensByAddress = new HashMap<>();
    protected final HashMap<Long, BREthereumToken> tokensByReference = new HashMap<>();
    public BREthereumToken[] tokens = null;
    public BREthereumToken tokenBRD;

    protected void initializeTokens () {
        long[] references =  jniTokenAll ();
        tokens = new BREthereumToken[references.length];

        for (int i = 0; i < references.length; i++)
            tokens[i] = new BREthereumToken(references[i]);

        for (BREthereumToken token : tokens) {
            System.err.println ("Token: " + token.getSymbol());
            tokensByReference.put(token.getIdentifier(), token);
            tokensByAddress.put (token.getAddress().toLowerCase(), token);
        }

        tokenBRD = lookupTokenByReference(jniGetTokenBRD());
    }

     public BREthereumToken lookupToken (String address) {
        return tokensByAddress.get(address.toLowerCase());
    }

    protected BREthereumToken lookupTokenByReference (long reference) {
        return tokensByReference.get(reference);
    }

    //
    // Constructor
    //

    public BREthereumLightNode(Client client, BREthereumNetwork network, String paperKey, String[] wordList) {
        this(BREthereumLightNode.jniCreateLightNode(client, network.getIdentifier(), paperKey, wordList),
                client, network);
    }

    public BREthereumLightNode(Client client, BREthereumNetwork network, byte[] publicKey) {
        this(BREthereumLightNode.jniCreateLightNode_PublicKey(client, network.getIdentifier(), publicKey),
                client, network);
    }

    private BREthereumLightNode(long identifier, Client client, BREthereumNetwork network) {
        super(identifier);

        // `this` is the JNI listener, using the `trampoline` functions to invoke
        // the installed `Listener`.
        jniAddListener(null);

        this.client = new WeakReference<>(client);
        this.network = network;
        this.account = new BREthereumAccount(this, jniLightNodeGetAccount());
        initializeTokens ();
    }


    public void addListener (Listener listener) {
        this.listener = new WeakReference<>(listener);
    }

    protected Listener getListener () {
	return null == listener ? null : listener.get();
    }

    //
    // Connect // Disconnect
    //
    public boolean connect () {
        return jniLightNodeConnect ();
    }
    public boolean disconnect () {
        return jniLightNodeDisconnect ();
    }

    //
    // Callback Announcements
    //
    // In the JNI Code, we had a problem directly accessing the Listener methods for the provided
    // listener (see addListener()).  So instead we'll access these methods below and then 'bounce'
    // to method calls for the listener.
    //
    // These methods also give us a chance to convert the `event`, as a `long`, to the Event.
    //
    protected void trampolineWalletEvent (int wid, int event, int status, String errorDescription) {
        Listener l =  getListener();
        if (null == l) return;
        // TODO: Resolve Bug
        if (event < 0 || event >= Listener.NUMBER_OF_WALLET_EVENTS) return;
        if (status < 0 || status >= Listener.NUMBER_OF_STATUS_EVENTS) return;

        // Lookup the wallet - this will create the wallet if it doesn't exist.  Thus, if the
        // `event` is `create`, we get a wallet; and even, if the `event` is `delete`, we get a
        // wallet too.
        BREthereumWallet wallet = walletLookupOrCreate(wid, null);

        // Invoke handler
        l.handleWalletEvent(wallet,
                Listener.WalletEvent.values()[(int) event],
                Listener.Status.values()[(int) status],
                errorDescription);
    }

    protected void trampolineBlockEvent (int bid, int event, int status, String errorDescription) {
        Listener l = getListener();
        if (null == l) return;
        // TODO: Resolve Bug
        if (event < 0 || event >= Listener.NUMBER_OF_BLOCK_EVENT) return;
        if (status < 0 || status >= Listener.NUMBER_OF_STATUS_EVENTS) return;

        // Nothing, at this point
        BREthereumBlock block = blockLookupOrCreate(bid);

        l.handleBlockEvent (block,
                Listener.BlockEvent.values()[(int) event],
                Listener.Status.values()[(int) status],
                errorDescription);
    }

    protected void trampolineTransactionEvent (int wid, int tid, int event, int status, String errorDescription) {
        Listener l =  getListener();
        if (null == l) return;
        // TODO: Resolve Bug
        if (event < 0 || event >= Listener.NUMBER_OF_TRANSACTION_EVENTS) return;
        if (status < 0 || status >= Listener.NUMBER_OF_STATUS_EVENTS) return;

        BREthereumWallet wallet = walletLookupOrCreate(wid, null);
        BREthereumTransaction transaction = transactionLookupOrCreate (tid);

        l.handleTransactionEvent(wallet, transaction,
                Listener.TransactionEvent.values()[(int) event],
                Listener.Status.values()[(int) status],
                errorDescription);
    }

     protected void trampolineGetBalance(int wid, String address, int rid) {
        client.get().getBalance(wid, address, rid);
    }

    protected void trampolineGetGasPrice(int wid, int rid) {
        client.get().getGasPrice(wid, rid);
    }

    protected void trampolineGetGasEstimate(int wid, int tid, String to, String amount, String data, int rid) {
        client.get().getGasEstimate(wid, tid, to, amount, data, rid);
    }

    protected void trampolineSubmitTransaction(int wid, int tid, String rawTransaction, int rid) {
        client.get().submitTransaction(wid, tid, rawTransaction, rid);
    }

    protected void trampolineGetTransactions(String address, int rid) {
        client.get().getTransactions(address, rid);
    }

    protected void trampolineGetLogs (String contract, String address, String event, int rid) {
        client.get().getLogs(contract, address, event, rid);
    }

    protected void trampolineGetBlockNumber (int rid) {
        client.get().getBlockNumber(rid);
    }

    protected void trampolineGetNonce (String address, int rid) {
        client.get().getNonce(address, rid);
    }

    //
    // JNI: Constructors
    //
    protected static native long jniCreateLightNode(Client client, long network, String paperKey, String[] wordList);
    protected static native long jniCreateLightNode_PublicKey(Client client, long network, byte[] publicKey);

    protected native void jniAddListener (Listener listener);

    //
    // JNI: Announcements
    //
    protected native void jniAnnounceTransaction(int id,
                                                 String hash,
                                                 String from,
                                                 String to,
                                                 String contract,
                                                 String amount, // value
                                                 String gasLimit,
                                                 String gasPrice,
                                                 String data,
                                                 String nonce,
                                                 String gasUsed,
                                                 String blockNumber,
                                                 String blockHash,
                                                 String blockConfirmations,
                                                 String blockTransactionIndex,
                                                 String blockTimestamp,
                                                 // cumulative gas used,
                                                 // confirmations
                                                 // txreceipt_status
                                                 String isError);

    protected native void jniAnnounceLog(int id,
                                         String hash,
                                         String contract,
                                         String[] topics,
                                         String data,
                                         String gasPrice,
                                         String gasUsed,
                                         String logIndex,
                                         String blockNumber,
                                         String blockTransactionIndex,
                                         String blockTimestamp);

    protected native void jniAnnounceBalance (int wid, String balance, int rid);
    protected native void jniAnnounceGasPrice (int wid, String gasPrice, int rid);
    protected native void jniAnnounceGasEstimate (int wid, int tid, String gasEstimate, int rid);
    protected native void jniAnnounceSubmitTransaction (int wid, int tid, String hash, int rid);
    protected native void jniAnnounceBlockNumber (String blockNumber, int rid);
    protected native void jniAnnounceNonce (String address, String nonce, int rid);


    // JNI: Account & Address
    protected native long jniLightNodeGetAccount();
    protected native String jniGetAccountPrimaryAddress(long accountId);
    protected native byte[] jniGetAccountPrimaryAddressPublicKey(long accountId);
    protected native byte[] jniGetAccountPrimaryAddressPrivateKey(long accountId, String paperKey);

    // JNI: Wallet
    protected native long jniLightNodeGetWallet();
    protected native long jniLightNodeGetWalletToken (long tokenId);
    protected native long jniLightNodeCreateWalletToken(long tokenId);
    protected native long jniLightNodeWalletGetToken (long wid);

    protected native String jniGetWalletBalance (long walletId, long unit);
    protected native void jniEstimateWalletGasPrice (long walletId);

    protected native void jniForceWalletBalanceUpdate(long wallet);

    protected native long jniWalletGetDefaultGasPrice (long wallet);
    protected native void jniWalletSetDefaultGasPrice (long wallet, long value);

    protected native long jniWalletGetDefaultGasLimit (long wallet);
    protected native void jniWalletSetDefaultGasLimit (long wallet, long value);
    //
    // JNI: Wallet Transactions
    //
    protected native long jniCreateTransaction (long walletId,
                                                String to,
                                                String amount,
                                                long amountUnit);

    protected native void jniSignTransaction (long walletId,
                                              long transactionId,
                                              String paperKey);

    protected native void jniSignTransactionWithPrivateKey(long walletId,
                                                           long transactionId,
                                                           byte[] privateKey);

    protected native void jniSubmitTransaction (long walletId,
                                                long transactionId);

    protected native long[] jniGetTransactions (long walletId);

    protected native void jniTransactionEstimateGas(long walletId,
                                                    long transactionId);

    protected native String jniTransactionEstimateFee (long walletId,
                                                       String amount,
                                                       long amountUnit,
                                                       long resultUnit);

    //
    // JNI: Transactions
    //
    protected native boolean jniTransactionHasToken (long transactionId);

    protected native String jniTransactionGetAmount(long transactionId, long unit);
    protected native String jniTransactionGetFee (long transactionId, long unit);
    protected native String jniTransactionSourceAddress (long transactionId);
    protected native String jniTransactionTargetAddress (long transactionId);
    protected native String jniTransactionGetHash (long transactionId);
    protected native String jniTransactionGetGasPrice (long transactionId, long unit);
    protected native long jniTransactionGetGasLimit (long transactionId);
    protected native long jniTransactionGetGasUsed (long transactionId);
    protected native long jniTransactionGetNonce (long transactionId);
    protected native long jniTransactionGetBlockNumber (long transactionId);
    protected native long jniTransactionGetBlockTimestamp (long transactionId);
    protected native long jniTransactionGetBlockConfirmations (long transactionId);
    protected native long jniTransactionGetToken (long transactionId);
    protected native boolean jniTransactionIsConfirmed (long transactionId);
    protected native boolean jniTransactionIsSubmitted (long transactionId);

    //
    // JNI: Tokens
    //
    // protected native String jniTokenGetAddress (long tokenId);

    //
    // JNI: Block
    //
    protected native long jniLightNodeGetBlockHeight ();
    protected native long jniBlockGetNumber (long bid);
    protected native long jniBlockGetTimestamp (long bid);
    protected native String jniBlockGetHash (long bid);

    //
    // JNI: Connect & Disconnect
    //
    protected native boolean jniLightNodeConnect ();
    protected native boolean jniLightNodeDisconnect ();

    // JNI: Initialize
    protected static native void initializeNative();

    static {
        initializeNative();
    }

    //
    // Support
    //

    //
    // Reference
    //
    static class Reference {
        WeakReference<BREthereumLightNode> node;
        long identifier;

        Reference(BREthereumLightNode node, long identifier) {
            this.node = new WeakReference<>(node);
            this.identifier = identifier;
        }
    }

    //
    // Reference With Default Unit
    //
    static class ReferenceWithDefaultUnit extends Reference {
        protected BREthereumAmount.Unit defaultUnit;
        protected boolean defaultUnitUsesToken = false;

        public BREthereumAmount.Unit getDefaultUnit() {
            return defaultUnit;
        }

        public void setDefaultUnit(BREthereumAmount.Unit unit) {
            validUnitOrException(unit);
            this.defaultUnit = unit;
        }

        //
        // Constructor
        //
        protected ReferenceWithDefaultUnit (BREthereumLightNode node,
                                            long identifier,
                                            BREthereumAmount.Unit unit) {
            super(node, identifier);
            this.defaultUnit = unit;
            this.defaultUnitUsesToken = unit.isTokenUnit();
        }

        //
        // Support
        //
        protected boolean validUnit(BREthereumAmount.Unit unit) {
            return (!defaultUnitUsesToken
                    ? (unit == BREthereumAmount.Unit.ETHER_WEI || unit == BREthereumAmount.Unit.ETHER_GWEI || unit == BREthereumAmount.Unit.ETHER_ETHER)
                    : (unit == BREthereumAmount.Unit.TOKEN_DECIMAL || unit == BREthereumAmount.Unit.TOKEN_INTEGER));
        }

        protected void validUnitOrException (BREthereumAmount.Unit unit) {
            if (!validUnit(unit))
                throw new IllegalArgumentException("Invalid Unit for instance type: " + unit.toString());
        }
    }
}
