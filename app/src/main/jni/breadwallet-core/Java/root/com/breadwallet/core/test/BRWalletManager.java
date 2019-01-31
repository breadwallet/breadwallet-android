/*
 * BreadWallet
 *
 * Created by Ed Gamble <ed@breadwallet.com> on 1/22/18.
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
package com.breadwallet.core.test;

import com.breadwallet.core.BRCoreAddress;
import com.breadwallet.core.BRCoreChainParams;
import com.breadwallet.core.BRCoreKey;
import com.breadwallet.core.BRCoreMasterPubKey;
import com.breadwallet.core.BRCoreMerkleBlock;
import com.breadwallet.core.BRCorePaymentProtocolACK;
import com.breadwallet.core.BRCorePaymentProtocolInvoiceRequest;
import com.breadwallet.core.BRCorePaymentProtocolMessage;
import com.breadwallet.core.BRCorePaymentProtocolRequest;
import com.breadwallet.core.BRCorePeer;
import com.breadwallet.core.BRCorePeerManager;
import com.breadwallet.core.BRCoreTransaction;
import com.breadwallet.core.BRCoreTransactionInput;
import com.breadwallet.core.BRCoreTransactionOutput;
import com.breadwallet.core.BRCoreWallet;
import com.breadwallet.core.BRCoreWalletManager;

import java.security.SecureRandom;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

/**
 *
 */
public class BRWalletManager extends BRCoreWalletManager {
    static {
        if (System.getProperties().containsKey("wallet.test"))
            System.loadLibrary("Core");
    }

    Executor listenerExecutor;
    public BRWalletManager(BRCoreMasterPubKey masterPubKey,
                           BRCoreChainParams chainParams,
                           double earliestPeerTime) {
        super(masterPubKey, chainParams, earliestPeerTime);
        listenerExecutor = Executors.newSingleThreadExecutor();
    }

    @Override
    protected BRCoreWallet.Listener createWalletListener() {
        return new BRCoreWalletManager.WrappedExecutorWalletListener(
                super.createWalletListener(),
                listenerExecutor);
    }

    @Override
    protected BRCorePeerManager.Listener createPeerManagerListener() {
        return new BRCoreWalletManager.WrappedExecutorPeerManagerListener(
                super.createPeerManagerListener(),
                listenerExecutor);
    }

    @Override
    public void syncStarted() {
        System.err.println("syncStarted (BRWalletManager)");
    }

    private static final double BIP39_CREATION_TIME = 1388534400.0;
    private static final String RANDOM_TEST_PAPER_KEY =
            "axis husband project any sea patch drip tip spirit tide bring belt";

    private static final String USABLE_PAPER_KEY =
            "ginger settle marine tissue robot crane night number ramp coast roast critic";

    private static final String THE_PAPER_KEY = USABLE_PAPER_KEY;

    private static final double THE_PAPER_KEY_TIME =
        (System.currentTimeMillis() / 1000) - 3 * 7 * 24 * 60 * 60;// Three weeks ago.

    private static final int SLEEP_TIME_IN_SECONDS = 3 * 60; // 5 minutes

    public static void main(String[] args) {

        Configuration configuration = parseArguments(
                null == args ? new String[] {""} : args);

        // Allow debugger to connect
        if (configuration.delay) {
            try {
                Thread.sleep(10 * 1000);
            } catch (Exception ex) {
            }
        }

        runTests();

        final BRCoreMasterPubKey masterPubKey =
                new BRCoreMasterPubKey(THE_PAPER_KEY.getBytes(), true);

        final List<BRWalletManager> walletManagers = new LinkedList<>();

        for (BRCoreChainParams chainParams : configuration.listOfParams) {
            walletManagers.add(
                    new BRWalletManager(masterPubKey,
                            chainParams,
                            THE_PAPER_KEY_TIME));
        }

        if (walletManagers.isEmpty()) return;

        describeWalletManager(walletManagers.get(0));

        for (BRWalletManager walletManager : walletManagers)
            walletManager.getPeerManager().connect();

        try {
            Thread.sleep (15 * 1000);
            for (BRWalletManager walletManager : walletManagers) {
                System.err.println ("Retry");
                walletManager.getPeerManager().disconnect();
                walletManager.getPeerManager().connect();
                walletManager.getPeerManager().rescan();
            }

            Thread.sleep(SLEEP_TIME_IN_SECONDS * 1000);
            System.err.println("Times Up - Done");

            Thread.sleep(2 * 1000);
        } catch (InterruptedException ex) {
            System.err.println("Interrupted - Done");
        }

        for (BRWalletManager walletManager : walletManagers)
            walletManager.getPeerManager().disconnect();

        walletManagers.clear();
        forceGC();

        System.exit(0);
    }

    private static void forceGC () {
        System.gc();
        System.err.println ("Spinning");
        List<Integer> ignore = new LinkedList<>();
        for (int i = 0; i < 1000; i++)
            for (int j = 0; j < 10000; j++)
                ignore.add (j);
        System.err.println ("Spinning Done");
    }


    private static void describeWalletManager(BRWalletManager manager) {
        System.err.println("MasterPubKey: " + manager.masterPubKey.toString());
        System.err.println("\nChainParams: " + manager.chainParams.toString());
        System.err.println("\n" + manager.toString());

        // Wallet and PeerManager
        System.err.println("\n" + manager.getWallet());
        System.err.println("\n" + manager.getPeerManager());
    }

    private static Configuration parseArguments(String[] args) {
        List<BRCoreChainParams> listOfParams = new LinkedList<>();
        boolean delay = false;

        for (int i = 0; i < args.length; i++) {
            switch (args[i]) {
                case "-main":
                    listOfParams.add(BRCoreChainParams.mainnetChainParams);
                    break;
                case "-test":
                    listOfParams.add(BRCoreChainParams.testnetChainParams);
                    break;
                case "-maincash":
                    listOfParams.add(BRCoreChainParams.mainnetBcashChainParams);
                    break;
                case "-testcash":
                    listOfParams.add(BRCoreChainParams.testnetBcashChainParams);
                    break;
                case "-delay":
                    delay = true;
                    break;
                case "":
                    break;
                default:
                    System.err.println("Unexpected argument (" + args[i] + ") - ignoring");
                    break;
            }
        }
        return new Configuration(delay, listOfParams);
    }

    private static class Configuration {
        boolean delay;
        List<BRCoreChainParams> listOfParams;

        public Configuration(boolean delay, List<BRCoreChainParams> listOfParams) {
            this.delay = delay;
            this.listOfParams = listOfParams;
        }
    }

    private static void runTests() {
        System.out.println ("\nStarting Tests:");

        runKeyTests();
        runTransactionTests();
        runWalletTests();
        runWalletManagerTests();
        runPeerManagerTests();
        runPaymentProtocolTests();
        // TODO: Fix
        runGCTests();
        System.out.println ("Completed Tests\n");
    }

    private static void runGCTests () {
        System.out.println ("    GC:");

        final BRCoreMasterPubKey masterPubKey =
                new BRCoreMasterPubKey(THE_PAPER_KEY.getBytes(), true);

        final BRCoreChainParams chainParams =
                BRCoreChainParams.testnetChainParams;

        BRWalletManager walletManager =
                new BRWalletManager(masterPubKey, chainParams, BIP39_CREATION_TIME);

        walletManager.getWallet();
        walletManager.getPeerManager();

        // Do not connect.

        walletManager = null;

        forceGC();
        forceGC();
    }

    private static void runKeyTests() {
        System.out.println("    Key:");

        BRCoreKey key = null;
        BRCoreAddress addr1;
        BRCoreAddress addr2;

        //
        //
        //
        System.out.println("        ScriptPubKey:");

        byte[] secret = { // 32
                0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
                0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 1
        };

        key = new BRCoreKey(secret, true);
        System.out.println("            " + key.address());

        addr1 = new BRCoreAddress(key.address());
        asserting (addr1.isValid());

        byte[] script = addr1.getPubKeyScript();

        addr2 = BRCoreAddress.fromScriptPubKey(script);
        asserting (addr1.stringify().equals(addr2.stringify()));

        System.out.println("        BitCash:");
        String bitcoinAddr1 = key.address();
        System.out.println("          Coin: " + bitcoinAddr1);
        String bitcashAddr  = BRCoreAddress.bcashEncodeBitcoin(bitcoinAddr1);
        System.out.println("          Cash: " + bitcashAddr);
        String bitcoinAddr2 = BRCoreAddress.bcashDecodeBitcoin(bitcashAddr);
        asserting (bitcoinAddr1.equals(bitcoinAddr2));
        String bitcoinAddr3 = BRCoreAddress.bcashDecodeBitcoin("bitcoincash:qzfhn2f7dwsfqdf6nlu07rw6c3ssqe9rm5a5y8tgm9");
        asserting (null != bitcoinAddr3 && !bitcoinAddr3.isEmpty());

        System.out.println (" Mihail's BCH: " + "bitcoincash:qzc93708k7x0w3gr32thxc5fla38xf8x8vq8h33fva");
        System.out.println (" Mihail's BTC: " + BRCoreAddress.bcashDecodeBitcoin("bitcoincash:qzc93708k7x0w3gr32thxc5fla38xf8x8vq8h33fva"));

        BRCoreAddress addrX1 = new BRCoreAddress(
                "bitcoincash:qzc93708k7x0w3gr32thxc5fla38xf8x8vq8h33fva");
        asserting (!addrX1.isValid());
        BRCoreAddress addrX2 = new BRCoreAddress(
                BRCoreAddress.bcashDecodeBitcoin("bitcoincash:qzc93708k7x0w3gr32thxc5fla38xf8x8vq8h33fva"));
        asserting (addrX2.isValid());


        String bitcoinAddr8 = "n2gzmWpFmcyC1WamqXvZs4kFf16sJD5MNN";
        String bitcashAddr8 = "qr5yphdxvy8f5gycmpe32kmmxna3hl4g5uvh59hkcn";

        System.out.println ("      Coin/Cash Loop");
        for (int i = 0; i < 100; i++) {
            assert (bitcoinAddr8.equals(BRCoreAddress.bcashDecodeBitcoin(bitcashAddr8)));
            assert (bitcashAddr8.equals(BRCoreAddress.bcashEncodeBitcoin(bitcoinAddr8)));
        }
        //
        //
        //
        System.out.println("        BIP32Sequence:");

        byte[] seed = { 0x00, 0x01, 0x02, 0x03, 0x04, 0x05, 0x06, 0x07, 0x08, 0x09, 0x0A, 0x0B, 0x0C, 0x0D, 0x0E, 0x0F };

        key = new BRCoreKey(seed, 1, 2 | 0x80000000L);
        System.out.println("            000102030405060708090a0b0c0d0e0f/0H/1/2H prv = " + BRCoreKey.encodeHex(key.getSecret()));
        asserting (BRCoreKey.encodeHex(key.getSecret()).equals("cbce0d719ecf7431d88e6a89fa1483e02e35092af60c042b1df2ff59fa424dca"));

        key = new BRCoreKey (seed, 0, 97);
        System.out.println("            000102030405060708090a0b0c0d0e0f/0H/0/97 prv = " + BRCoreKey.encodeHex(key.getSecret()));
        asserting (BRCoreKey.encodeHex(key.getSecret()).equals("00136c1ad038f9a00871895322a487ed14f1cdc4d22ad351cfa1a0d235975dd7"));

        //
        //
        //
        System.out.println("        MasterPubKey:");

        BRCoreMasterPubKey keyFromPaperKey = new BRCoreMasterPubKey(THE_PAPER_KEY.getBytes(), true);
        byte[] keySerialized = keyFromPaperKey.serialize();
        BRCoreMasterPubKey keyFromBytes = new BRCoreMasterPubKey(keySerialized, false);
        asserting (Arrays.equals(keySerialized, keyFromBytes.serialize()));

        System.out.println("            encodeSeed:");

        final byte[] randomSeed = new SecureRandom().generateSeed(16);
        byte[] paperKeyBytes = BRCoreMasterPubKey.generatePaperKey(randomSeed, words);
        System.out.println("               PaperKey: " + Arrays.toString(paperKeyBytes));
        System.out.println("               PaperKey: " + new String(paperKeyBytes));
        System.out.println("               PaperKey: " + Arrays.toString(new String(paperKeyBytes).getBytes()));
        asserting (Arrays.equals(paperKeyBytes, new String (paperKeyBytes).getBytes()));
        asserting (0 != paperKeyBytes[paperKeyBytes.length - 1]);
        //
        //
        //
        System.out.println("        Key/Addr:");

        BRCoreAddress addr = null;
        String addrString = null;

        asserting (!BRCoreKey.isValidBitcoinPrivateKey("S6c56bnXQiBjk9mqSYE7ykVQ7NzrRz"));

        asserting (BRCoreKey.isValidBitcoinPrivateKey("S6c56bnXQiBjk9mqSYE7ykVQ7NzrRy"));
        key = new BRCoreKey("S6c56bnXQiBjk9mqSYE7ykVQ7NzrRy");
        addrString = key.address();
        System.out.println("            privKey:S6c56bnXQiBjk9mqSYE7ykVQ7NzrRy = " + addrString);
        asserting (addrString.equals("1CciesT23BNionJeXrbxmjc7ywfiyM4oLW"));

        asserting (BRCoreKey.isValidBitcoinPrivateKey("SzavMBLoXU6kDrqtUVmffv"));
        key = new BRCoreKey("SzavMBLoXU6kDrqtUVmffv");
        addrString = key.address();
        System.out.println("            privKey:SzavMBLoXU6kDrqtUVmffv = " + addrString);
        asserting (addrString.equals("1CC3X2gu58d6wXUWMffpuzN9JAfTUWu4Kj"));

        asserting ("S6c56bnXQiBjk9mqSYE7ykVQ7NzrRy".equals(
                BRCoreKey.encodeASCII(BRCoreKey.decodeASCII("S6c56bnXQiBjk9mqSYE7ykVQ7NzrRy"))));

        // uncompressed private key
        asserting (BRCoreKey.isValidBitcoinPrivateKey("5Kb8kLf9zgWQnogidDA76MzPL6TsZZY36hWXMssSzNydYXYB9KF"));
        key = new BRCoreKey("5Kb8kLf9zgWQnogidDA76MzPL6TsZZY36hWXMssSzNydYXYB9KF");
        addrString = key.address();
        System.out.println("            privKey:5Kb8kLf9zgWQnogidDA76MzPL6TsZZY36hWXMssSzNydYXYB9KF = " + addrString);
        asserting (addrString.equals("1CC3X2gu58d6wXUWMffpuzN9JAfTUWu4Kj"));

        // uncompressed private key export
        String privKeyString = key.getPrivKey();
        System.out.println("            privKey:" + privKeyString);
        asserting (privKeyString.equals("5Kb8kLf9zgWQnogidDA76MzPL6TsZZY36hWXMssSzNydYXYB9KF"));

        // compressed private key
        asserting (BRCoreKey.isValidBitcoinPrivateKey("KyvGbxRUoofdw3TNydWn2Z78dBHSy2odn1d3wXWN2o3SAtccFNJL"));
        key = new BRCoreKey("KyvGbxRUoofdw3TNydWn2Z78dBHSy2odn1d3wXWN2o3SAtccFNJL");
        addrString = key.address();
        System.out.println("            privKey:KyvGbxRUoofdw3TNydWn2Z78dBHSy2odn1d3wXWN2o3SAtccFNJL = " + addrString);
        asserting (addrString.equals("1JMsC6fCtYWkTjPPdDrYX3we2aBrewuEM3"));

        privKeyString = key.getPrivKey();
        System.out.println("            privKey:" + privKeyString);
        asserting (privKeyString.equals("KyvGbxRUoofdw3TNydWn2Z78dBHSy2odn1d3wXWN2o3SAtccFNJL"));

        // signing
        System.out.println("        Key/Addr Sign:");

        key = new BRCoreKey(secret, true);

        byte[] messageDigest = BRCoreKey.encodeSHA256("Everything should be made as simple as possible, but not simpler.");
        System.out.println ("            messageDigest: " + Arrays.toString((messageDigest)));

        byte[] signature = key.sign(messageDigest);
        System.out.println ("            signature    : " + Arrays.toString((signature)));

        asserting (key.verify(messageDigest, signature));
        //

        /*
            BRKeySetSecret(&key, &uint256("0000000000000000000000000000000000000000000000000000000000000001"), 1);
    msg = ;
    BRSHA256(&md, msg, strlen(msg));
    sigLen = BRKeySign(&key, sig, sizeof(sig), md);

    char sig1[] = "\x30\x44\x02\x20\x33\xa6\x9c\xd2\x06\x54\x32\xa3\x0f\x3d\x1c\xe4\xeb\x0d\x59\xb8\xab\x58\xc7\x4f\x27"
    "\xc4\x1a\x7f\xdb\x56\x96\xad\x4e\x61\x08\xc9\x02\x20\x6f\x80\x79\x82\x86\x6f\x78\x5d\x3f\x64\x18\xd2\x41\x63\xdd"
    "\xae\x11\x7b\x7d\xb4\xd5\xfd\xf0\x07\x1d\xe0\x69\xfa\x54\x34\x22\x62";

    if (sigLen != sizeof(sig1) - 1 || memcmp(sig, sig1, sigLen) != 0)
        r = 0, fprintf(stderr, "***FAILED*** %s: BRKeySign() test 1\n", __func__);

    if (! BRKeyVerify(&key, md, sig, sigLen))
        r = 0, fprintf(stderr, "***FAILED*** %s: BRKeyVerify() test 1\n", __func__);


         */
    }


    private static void runTransactionTests () {
        System.out.println("    Transaction:");
        byte[] secret = { // 32
                0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
                0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 1
        };

        byte[] inHash = { // 32
                0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
                0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 1
        };

        BRCoreKey k[] = new BRCoreKey[]{
                new BRCoreKey(),
                new BRCoreKey(secret, true)
        };

        System.out.println("        One Input / Two Outputs:");

        BRCoreAddress address = new BRCoreAddress(k[1].address());
        System.out.println("            Address: " + address.stringify());

        byte[] script = address.getPubKeyScript();
        System.out.println("            Script : " + Arrays.toString(script));

        //Address: 1BgGZ9tcN4rm9KBzDn7KprQz87SZ26SAMH
        //Script : [118, -87, 20, 117, 30, 118, -24, 25, -111, -106, -44, 84, -108, 28, 69, -47, -77, -93, 35, -15, 67, 59, -42, -120, -84]

        BRCoreTransaction transaction = new BRCoreTransaction();
        transaction.addInput(
                new BRCoreTransactionInput(inHash, 0, 1, script, new byte[]{}, 4294967295L));
        transaction.addOutput(
                new BRCoreTransactionOutput(100000000L, script));
        transaction.addOutput(
                new BRCoreTransactionOutput(4900000000L, script));

        byte[] transactionSerialized = transaction.serialize();
        System.out.println("            Transaction: " + Arrays.toString(transactionSerialized));
        // Transaction : [1, 0, 0, 0, 1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 1, 0, 0, 0, 0, 25, 118, 169, 20, 117, 30, 118, 232, 25, 145, 150, 212, 84, 148, 28, 69, 209, 179, 163, 35, 241, 67, 59, 214, 136, 172, 1, 0, 0, 0, 0, 0, 0, 0, 255, 255, 255, 255, 2, 0, 225, 245, 5, 0, 0, 0, 0, 25, 118, 169, 20, 117, 30, 118, 232, 25, 145, 150, 212, 84, 148, 28, 69, 209, 179, 163, 35, 241, 67, 59, 214, 136, 172, 0, 17, 16, 36, 1, 0, 0, 0, 25, 118, 169, 20, 117, 30, 118, 232, 25, 145, 150, 212, 84, 148, 28, 69, 209, 179, 163, 35, 241, 67, 59, 214, 136, 172, 0, 0, 0, 0, ]

        asserting (transactionSerialized.length != 0);

        BRCoreTransaction transactionFromSerialized = new BRCoreTransaction(transactionSerialized);

        asserting (transactionFromSerialized.getInputs().length == 1
                && transactionFromSerialized.getOutputs().length == 2);

        asserting (transaction.getTimestamp() == transactionFromSerialized.getTimestamp()
                && transaction.getBlockHeight() == transactionFromSerialized.getBlockHeight());

        System.out.println("            Signed");
        transaction.sign(k, 0x00);
        asserting (transaction.isSigned());

        BRCoreAddress sigAddress;
        // TODO: Fix
//        sigAddress = BRCoreAddress.fromScriptSignature(script);
//        asserting (address.stringify().equals(sigAddress.stringify()));

        transactionSerialized = transaction.serialize();
        transactionFromSerialized = new BRCoreTransaction (transactionSerialized);
        asserting (transactionFromSerialized.isSigned());

        asserting (Arrays.equals(transactionSerialized, transactionFromSerialized.serialize()));

        System.out.println("        Five Inputs / Four Outputs:");

        transaction = new BRCoreTransaction();
        transaction.addInput(
                new BRCoreTransactionInput(inHash, 0, 1, script, new byte[]{}, 4294967295L));
        transaction.addInput(
                new BRCoreTransactionInput(inHash, 0, 1, script, new byte[]{}, 4294967295L));
        transaction.addInput(
                new BRCoreTransactionInput(inHash, 0, 1, script, new byte[]{}, 4294967295L));
        transaction.addInput(
                new BRCoreTransactionInput(inHash, 0, 1, script, new byte[]{}, 4294967295L));
        transaction.addInput(
                new BRCoreTransactionInput(inHash, 0, 1, script, new byte[]{}, 4294967295L));

        transaction.addOutput(
                new BRCoreTransactionOutput(4900000000L, script));
        transaction.addOutput(
                new BRCoreTransactionOutput(4900000000L, script));
        transaction.addOutput(
                new BRCoreTransactionOutput(4900000000L, script));
        transaction.addOutput(
                new BRCoreTransactionOutput(4900000000L, script));

        transaction.sign(k, 0x00);
        asserting (transaction.isSigned());
        BRCoreTransactionInput inputs[] = transaction.getInputs();
        sigAddress = BRCoreAddress.fromScriptSignature(
                inputs[inputs.length - 1].getScript());
        // TODO: Fix
        // asserting (address.stringify().equals(sigAddress.stringify()));

        System.out.println("        Set Output Amount:");

        BRCoreTransactionOutput out1 = new BRCoreTransactionOutput(4900000000L, script);
        asserting (4900000000L == out1.getAmount());
        out1.setAmount(100);
        asserting (100 == out1.getAmount());

    }

    private static void runPaymentProtocolTests () {
        System.out.println ("    PaymentProtocol:");
        runPaymentProtocolRequestTest();
        runPaymentProtocolACKTest();
        runPaymentProtocolMessageTest();
        runPaymentProtocolInvoiceRequestTest();
    }

    private static void runPaymentProtocolInvoiceRequestTest() {
        System.out.println("        InvoiceRequest");

        byte[] secret = { // 32
                0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
                0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 2
        };
        BRCoreKey sendKey = new BRCoreKey(secret, true);
        BRCoreKey recvKey = new BRCoreKey(secret, true);

        BRCorePaymentProtocolInvoiceRequest invoiceRequest =
                new BRCorePaymentProtocolInvoiceRequest(sendKey, 0, null, null, null, null, null);


        byte[] serialized = invoiceRequest.serialize();

        BRCorePaymentProtocolInvoiceRequest invoiceRequestFromSerialized =
                new BRCorePaymentProtocolInvoiceRequest(serialized);

        asserting (Arrays.equals(sendKey.getPubKey(), invoiceRequestFromSerialized.getSenderPublicKey().getPubKey()));
    }

    private static void runPaymentProtocolMessageTest () {
        System.out.println ("        Message");

        //
        //
        int intId[] = { 0xff, 0x00, 0xff, 0x00, 0xff, 0x00, 0xff, 0x00, 0xff, 0x00, 0xff, 0x00, 0xff, 0x00, 0xff, 0x00,
                0xff, 0x00, 0xff, 0x00, 0xff, 0x00, 0xff, 0x00, 0xff, 0x00, 0xff, 0x00, 0xff, 0x00, 0xff, 0x00 };
        byte id[] = asBytes(intId);

        byte msg[] = getPaymentProtocolMessageBytes();

//        BRPaymentProtocolMessage *msg1 = BRPaymentProtocolMessageNew(BRPaymentProtocolMessageTypeACK, (uint8_t *)buf,
//                sizeof(buf) - 1, 1, NULL, id, sizeof(id));

        BRCorePaymentProtocolMessage message =
                new BRCorePaymentProtocolMessage(BRCorePaymentProtocolMessage.MessageType.ACK, msg,
                        1, "", id);

        byte[] serialized = message.serialize();

        BRCorePaymentProtocolMessage messageSerialized =
                new BRCorePaymentProtocolMessage(serialized);

        asserting (Arrays.equals(message.getMessage(), messageSerialized.getMessage()));

    }

    private static void runPaymentProtocolRequestTest() {
        System.out.println ("        Request");

        // Request
        byte requestData[] = getPaymentProtocolRequestBytes();
        BRCorePaymentProtocolRequest request = new BRCorePaymentProtocolRequest(requestData);
        byte serializedRequestData[] = request.serialize();
        asserting (Arrays.equals(serializedRequestData, requestData));

        // BRPaymentProtocolRequestCert
        byte[][] certs = request.getCerts();
        asserting (3 == certs.length);
    }

    private static void runPaymentProtocolACKTest() {
        System.out.println ("        ACK");

        byte data[] = getPaymentProtocolAckBytes();
        BRCorePaymentProtocolACK ack = new BRCorePaymentProtocolACK(data);
        byte serialized[] = ack.serialize();
        asserting (Arrays.equals(data, serialized));

        asserting (!ack.getCustomerMemo().isEmpty());
        System.out.println ("            Customer Memo: " + ack.getCustomerMemo());
    }

    private static void runWalletTests() {
        System.out.println("    Wallet:");

        final long SATOSHIS = 100000000L;
        final long MAX_MONEY = 2100000L * SATOSHIS;

        final int SEQUENCE_GAP_LIMIT_EXTERNAL = 10;
        final int SEQUENCE_GAP_LIMIT_INTERNAL = 5;

        byte[] secret = { // 32
                0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
                0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 1
        };

        byte[] inHash = { // 32
                0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
                0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 1
        };


        final byte[] randomSeed = new SecureRandom().generateSeed(16);
        byte[] phrase = BRCoreMasterPubKey.generatePaperKey(randomSeed, words);
        //byte[] phrase = "a random seed".getBytes();

        BRCoreMasterPubKey mpk = new BRCoreMasterPubKey(phrase, true);

        BRCoreWallet.Listener walletListener = getWalletListener();

        BRCoreWallet w = createWallet(new BRCoreTransaction[]{}, mpk, walletListener);
        BRCoreAddress recvAddr = w.getReceiveAddress();

        // A random addr
        BRCoreKey k = new BRCoreKey(secret, true);
        BRCoreAddress addr = new BRCoreAddress(k.address());

        BRCoreTransaction tx = w.createTransaction(1, addr);
        asserting(null == tx); // no money

        tx = w.createTransaction(SATOSHIS, addr);
        asserting(null == tx); // no money
        asserting(0 == w.getTransactions().length);

        byte[] inScript = addr.getPubKeyScript();      // from rando
        byte[] outScript = recvAddr.getPubKeyScript();  // to me

        System.out.println("        One SATOSHI");

        tx = new BRCoreTransaction();
        tx.addInput(
                new BRCoreTransactionInput(inHash, 0, 1, inScript, new byte[]{}, 4294967295L));
        tx.addOutput(
                new BRCoreTransactionOutput(SATOSHIS, outScript));
        w.signTransaction(tx, 0x00, phrase);
        asserting (tx.isSigned());
        System.out.println("            Signed");
        w.registerTransaction(tx);

        asserting(SATOSHIS == w.getBalance());
        asserting(1 == w.getTransactions().length);

        // Register twice - no extra money
        w.registerTransaction(tx);
        asserting(SATOSHIS == w.getBalance());

        System.out.println("        LockTime");
        tx = new BRCoreTransaction();
        tx.addInput(
                new BRCoreTransactionInput(inHash, 1, 1, inScript, new byte[]{}, 4294967295L - 1));
        tx.addOutput(
                new BRCoreTransactionOutput(SATOSHIS, outScript));
        tx.setLockTime(1000);
        tx.sign(k, 0x00);
        asserting(tx.isSigned());
        asserting(w.transactionIsPending(tx));

        // Locktime prevents - no extra money
        w.registerTransaction(tx);
        asserting(SATOSHIS == w.getBalance());

        byte[][] hashes = new byte[][]{tx.getHash()};

        // pass locktime - money added
        w.updateTransactions(hashes, 1000, 1);
        asserting(2 * SATOSHIS == w.getBalance());

        //
        //
        //
        System.out.println("        Timestamp");

        tx = new BRCoreTransaction();
        tx.addInput (
                new BRCoreTransactionInput(inHash, 0, 1, inScript, new byte[] {}, 4294967295L));
        tx.addOutput(
                new BRCoreTransactionOutput(SATOSHIS, outScript));
        tx.sign(k, 0x00);
        tx.setTimestamp (1);
        asserting (tx.isSigned());

        System.out.println("            Init w/ One SATOSHI");

        w = createWallet(new BRCoreTransaction[] { tx }, mpk, walletListener);
        asserting (SATOSHIS == w.getBalance());
        asserting (w.getAllAddresses().length == 1 + SEQUENCE_GAP_LIMIT_EXTERNAL + SEQUENCE_GAP_LIMIT_INTERNAL);

        byte[] txHash = tx.getHash();

        System.out.println("            Can't send Two SATOSHI");
        // unsigned
        tx = w.createTransaction(2*SATOSHIS, addr);
        asserting (null == tx);

        //
        System.out.println("            Can send half SATOSHI");
        asserting (w.getFeeForTransactionAmount(SATOSHIS/2) >= 1000);
        tx = w.createTransaction(SATOSHIS/2, addr);
        asserting (null != tx);
        asserting (! tx.isSigned());

	    w.signTransaction(tx, 0, phrase);
        asserting (tx.isSigned());

        tx.setTimestamp(1);
        w.registerTransaction(tx);
        asserting (w.getBalance() + w.getTransactionFee(tx) == SATOSHIS/2);
        asserting(w.transactionIsVerified(tx));
        asserting(w.transactionIsValid(tx));
        asserting(!w.transactionIsPending(tx));

        asserting(2 == w.getTransactions().length);
        BRCoreTransaction foundTX = w.transactionForHash(txHash);

        // TODO: removeTransaction leads to a memory error
        // The transaction for txHash is freed but other dependent transactions are also freed
        // presumably they are all already registered?  Seems not.
//        w.removeTransaction(txHash);
//        asserting(0 == w.getTransactions().length);

        //
        // MPK from pubKey
        System.out.println("        MasterPubKey from PubKey");

        byte[] mpkSerialized = mpk.serialize();
        mpk = new BRCoreMasterPubKey(mpkSerialized, false);
        w = createWallet(new BRCoreTransaction[]{}, mpk, walletListener);

        tx = new BRCoreTransaction();
        tx.addInput(
                new BRCoreTransactionInput(inHash, 0, 1, inScript, new byte[]{}, 4294967295L));
        tx.addOutput(
                new BRCoreTransactionOutput(SATOSHIS, outScript));
        w.signTransaction(tx, 0x00, phrase);
        asserting (tx.isSigned());
        System.out.println("            Signed");
        w.registerTransaction(tx);
        asserting(SATOSHIS == w.getBalance());

        tx = w.createTransaction(SATOSHIS/2, addr);
        asserting (null != tx);
        asserting (! tx.isSigned());

        w.signTransaction(tx, 0, phrase);
        asserting (tx.isSigned());
        System.out.println("            Can send half SATOSHI");


        //
        System.out.println("        fromOutputs");
        BRCoreTransactionOutput[] outputs = {
                new BRCoreTransactionOutput(SATOSHIS/10, outScript),
                new BRCoreTransactionOutput(SATOSHIS/10, outScript),
                new BRCoreTransactionOutput(SATOSHIS/10, outScript)
        };

        BRCoreTransaction transaction = w.createTransactionForOutputs(outputs);
        asserting (null != transaction);
    }

    private static void runWalletManagerTests() {
        System.out.println("    Wallet Manager:");

        final long SATOSHIS = 100000000L;

        byte[] secret = { // 32
                0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
                0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 1
        };

        byte[] inHash = { // 32
                0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
                0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 1
        };


        final byte[] randomSeed = new SecureRandom().generateSeed(16);
        byte[] phrase = BRCoreMasterPubKey.generatePaperKey(randomSeed, words);

        BRCoreMasterPubKey mpk = new BRCoreMasterPubKey(phrase, true);

        BRCoreWalletManager wm = new BRCoreWalletManager(mpk, BRCoreChainParams.testnetChainParams, -1);

        BRCoreWallet w = wm.getWallet();
        asserting(null != w);
        BRCoreAddress recvAddr = w.getReceiveAddress();

        // A random addr
        BRCoreKey k = new BRCoreKey(secret, true);
        BRCoreAddress addr = new BRCoreAddress(k.address());

        byte[] inScript = addr.getPubKeyScript();      // from rando
        byte[] outScript = recvAddr.getPubKeyScript();  // to me

        System.out.println("        One SATOSHI");

        BRCoreTransaction tx = new BRCoreTransaction();
        tx.addInput(
                new BRCoreTransactionInput(inHash, 0, 1, inScript, new byte[]{}, 4294967295L));
        tx.addOutput(
                new BRCoreTransactionOutput(SATOSHIS, outScript));

//        wm.signAndPublishTransaction(tx, phrase);
        wm.getWallet().signTransaction(tx, 0x00, phrase);

        asserting (tx.isSigned());
        System.out.println("            Signed");
    }

    //
    //
    //
    private static void runPeerManagerTests() {
        System.out.println("    Peer Manager:");

        final byte[] randomSeed = new SecureRandom().generateSeed(16);
        byte[] phrase = BRCoreMasterPubKey.generatePaperKey(randomSeed, words);

        BRCoreMasterPubKey mpk = new BRCoreMasterPubKey(phrase, true);

        BRCoreWallet w = createWallet(new BRCoreTransaction[]{}, mpk, getWalletListener());

        System.out.println("            Peers");

        BRCorePeer peer = new BRCorePeer(1);
        BRCorePeer[] peers = new BRCorePeer[1024];
        for (int i = 0; i < 1024; i++)
            peers[i] = peer;

        System.out.println("            Blocks");

        BRCoreMerkleBlock block = new BRCoreMerkleBlock(getMerkleBlockBytes(), 100001);
        asserting (null != block);
        asserting (100001 == block.getHeight());

        byte[] hash = block.getBlockHash();
        asserting (!block.containsTransactionHash(hash));

        BRCoreMerkleBlock[] blocks = new BRCoreMerkleBlock[1024];
        for (int i = 0; i < 1024; i++)
            blocks[i] = block;

        System.out.println("            Manager");

        BRCorePeerManager pm = new BRCorePeerManager(
                BRCoreChainParams.testnetChainParams,
                w,
                0,
                blocks,
                peers,
                getPeerManagerListener());
        asserting (null != pm);

//        pm.testSaveBlocksCallback(false, blocks);
//        pm.testSavePeersCallback(false, peers);
    }

    private static BRCoreWallet.Listener getWalletListener () {
        return  new BRCoreWallet.Listener() {
            @Override
            public void balanceChanged(long balance) {
                System.out.println(String.format("            balance   : %d", balance));
            }

            @Override
            public void onTxAdded(BRCoreTransaction transaction) {
                System.out.println(String.format("            tx added  : %s",
                        BRCoreKey.encodeHex(transaction.getHash())));

            }

            @Override
            public void onTxUpdated(String hash, int blockHeight, int timeStamp) {
                System.out.println(String.format("            tx updated: %s", hash));
            }

            @Override
            public void onTxDeleted(String hash, int notifyUser, int recommendRescan) {
                System.out.println(String.format("            tx deleted: %s", hash));

            }
        };
    }

    private static BRCorePeerManager.Listener getPeerManagerListener () {
        return new BRCorePeerManager.Listener() {
            @Override
            public void syncStarted() {
                System.out.println(String.format("            syncStarted"));

            }

            @Override
            public void syncStopped(String error) {
                System.out.println(String.format("            syncStopped: %s", error));

            }

            @Override
            public void txStatusUpdate() {
                System.out.println(String.format("            txStatusUpdate"));
            }

            @Override
            public void saveBlocks(boolean replace, BRCoreMerkleBlock[] blocks) {
                System.out.println(String.format("            saveBlocks: %d", blocks.length));
            }

            @Override
            public void savePeers(boolean replace, BRCorePeer[] peers) {
                System.out.println(String.format("            savePeers: %d", peers.length));
            }

            @Override
            public boolean networkIsReachable() {
                System.out.println(String.format("            networkIsReachable"));
                return false;
            }

            @Override
            public void txPublished(String error) {
                System.out.println(String.format("            txPublished: %s", error));

            }
        };
    }

    private static BRCoreWallet createWallet (BRCoreTransaction[] transactions,
                                              BRCoreMasterPubKey masterPubKey,
                                              BRCoreWallet.Listener listener) {
        try {
            return new BRCoreWallet(transactions, masterPubKey, listener);
        }
        catch (BRCoreWallet.WalletExecption ex) {
            asserting (false);
            return null;
        }
    }

    private static byte[] getMerkleBlockBytes () {
        int intBuffer[] =
                {0x01, 0x00, 0x00, 0x00, 0x06, 0xe5, 0x33, 0xfd, 0x1a, 0xda, 0x86, 0x39, 0x1f, 0x3f, 0x6c, 0x34, 0x32, 0x04, 0xb0, 0xd2, 0x78, 0xd4, 0xaa, 0xec, 0x1c
                        , 0x0b, 0x20, 0xaa, 0x27, 0xba, 0x03, 0x00, 0x00, 0x00, 0x00, 0x00, 0x6a, 0xbb, 0xb3, 0xeb, 0x3d, 0x73, 0x3a, 0x9f, 0xe1, 0x89, 0x67, 0xfd, 0x7d, 0x4c, 0x11, 0x7e, 0x4c
                        , 0xcb, 0xba, 0xc5, 0xbe, 0xc4, 0xd9, 0x10, 0xd9, 0x00, 0xb3, 0xae, 0x07, 0x93, 0xe7, 0x7f, 0x54, 0x24, 0x1b, 0x4d, 0x4c, 0x86, 0x04, 0x1b, 0x40, 0x89, 0xcc, 0x9b, 0x0c
                        , 0x00, 0x00, 0x00, 0x08, 0x4c, 0x30, 0xb6, 0x3c, 0xfc, 0xdc, 0x2d, 0x35, 0xe3, 0x32, 0x94, 0x21, 0xb9, 0x80, 0x5e, 0xf0, 0xc6, 0x56, 0x5d, 0x35, 0x38, 0x1c, 0xa8, 0x57
                        , 0x76, 0x2e, 0xa0, 0xb3, 0xa5, 0xa1, 0x28, 0xbb, 0xca, 0x50, 0x65, 0xff, 0x96, 0x17, 0xcb, 0xcb, 0xa4, 0x5e, 0xb2, 0x37, 0x26, 0xdf, 0x64, 0x98, 0xa9, 0xb9, 0xca, 0xfe
                        , 0xd4, 0xf5, 0x4c, 0xba, 0xb9, 0xd2, 0x27, 0xb0, 0x03, 0x5d, 0xde, 0xfb, 0xbb, 0x15, 0xac, 0x1d, 0x57, 0xd0, 0x18, 0x2a, 0xae, 0xe6, 0x1c, 0x74, 0x74, 0x3a, 0x9c, 0x4f
                        , 0x78, 0x58, 0x95, 0xe5, 0x63, 0x90, 0x9b, 0xaf, 0xec, 0x45, 0xc9, 0xa2, 0xb0, 0xff, 0x31, 0x81, 0xd7, 0x77, 0x06, 0xbe, 0x8b, 0x1d, 0xcc, 0x91, 0x11, 0x2e, 0xad, 0xa8
                        , 0x6d, 0x42, 0x4e, 0x2d, 0x0a, 0x89, 0x07, 0xc3, 0x48, 0x8b, 0x6e, 0x44, 0xfd, 0xa5, 0xa7, 0x4a, 0x25, 0xcb, 0xc7, 0xd6, 0xbb, 0x4f, 0xa0, 0x42, 0x45, 0xf4, 0xac, 0x8a
                        , 0x1a, 0x57, 0x1d, 0x55, 0x37, 0xea, 0xc2, 0x4a, 0xdc, 0xa1, 0x45, 0x4d, 0x65, 0xed, 0xa4, 0x46, 0x05, 0x54, 0x79, 0xaf, 0x6c, 0x6d, 0x4d, 0xd3, 0xc9, 0xab, 0x65, 0x84
                        , 0x48, 0xc1, 0x0b, 0x69, 0x21, 0xb7, 0xa4, 0xce, 0x30, 0x21, 0xeb, 0x22, 0xed, 0x6b, 0xb6, 0xa7, 0xfd, 0xe1, 0xe5, 0xbc, 0xc4, 0xb1, 0xdb, 0x66, 0x15, 0xc6, 0xab, 0xc5
                        , 0xca, 0x04, 0x21, 0x27, 0xbf, 0xaf, 0x9f, 0x44, 0xeb, 0xce, 0x29, 0xcb, 0x29, 0xc6, 0xdf, 0x9d, 0x05, 0xb4, 0x7f, 0x35, 0xb2, 0xed, 0xff, 0x4f, 0x00, 0x64, 0xb5, 0x78
                        , 0xab, 0x74, 0x1f, 0xa7, 0x82, 0x76, 0x22, 0x26, 0x51, 0x20, 0x9f, 0xe1, 0xa2, 0xc4, 0xc0, 0xfa, 0x1c, 0x58, 0x51, 0x0a, 0xec, 0x8b, 0x09, 0x0d, 0xd1, 0xeb, 0x1f, 0x82
                        , 0xf9, 0xd2, 0x61, 0xb8, 0x27, 0x3b, 0x52, 0x5b, 0x02, 0xff, 0x1a
                };
        return asBytes(intBuffer);
    }

    private static byte[] getPaymentProtocolAckBytes() {
        int intBuffer[] =
                {0x0a, 0x00, 0x12, 0x5f, 0x54, 0x72, 0x61, 0x6e, 0x73, 0x61, 0x63, 0x74, 0x69, 0x6f, 0x6e, 0x20, 0x72, 0x65, 0x63, 0x65, 0x69, 0x76, 0x65
                        , 0x64, 0x20, 0x62, 0x79, 0x20, 0x42, 0x69, 0x74, 0x50, 0x61, 0x79, 0x2e, 0x20, 0x49, 0x6e, 0x76, 0x6f, 0x69, 0x63, 0x65, 0x20, 0x77, 0x69, 0x6c, 0x6c, 0x20, 0x62, 0x65
                        , 0x20, 0x6d, 0x61, 0x72, 0x6b, 0x65, 0x64, 0x20, 0x61, 0x73, 0x20, 0x70, 0x61, 0x69, 0x64, 0x20, 0x69, 0x66, 0x20, 0x74, 0x68, 0x65, 0x20, 0x74, 0x72, 0x61, 0x6e, 0x73
                        , 0x61, 0x63, 0x74, 0x69, 0x6f, 0x6e, 0x20, 0x69, 0x73, 0x20, 0x63, 0x6f, 0x6e, 0x66, 0x69, 0x72, 0x6d, 0x65, 0x64, 0x2e
                };
        return asBytes(intBuffer);
    }

    private static byte[] getPaymentProtocolMessageBytes () {
        int intBuffer[] =
                { 0x0a, 0x00, 0x12, 0x5f, 0x54, 0x72, 0x61, 0x6e, 0x73, 0x61, 0x63, 0x74, 0x69, 0x6f, 0x6e, 0x20, 0x72, 0x65, 0x63, 0x65, 0x69, 0x76, 0x65
                , 0x64, 0x20, 0x62, 0x79, 0x20, 0x42, 0x69, 0x74, 0x50, 0x61, 0x79, 0x2e, 0x20, 0x49, 0x6e, 0x76, 0x6f, 0x69, 0x63, 0x65, 0x20, 0x77, 0x69, 0x6c, 0x6c, 0x20, 0x62, 0x65
                , 0x20, 0x6d, 0x61, 0x72, 0x6b, 0x65, 0x64, 0x20, 0x61, 0x73, 0x20, 0x70, 0x61, 0x69, 0x64, 0x20, 0x69, 0x66, 0x20, 0x74, 0x68, 0x65, 0x20, 0x74, 0x72, 0x61, 0x6e, 0x73
                , 0x61, 0x63, 0x74, 0x69, 0x6f, 0x6e, 0x20, 0x69, 0x73, 0x20, 0x63, 0x6f, 0x6e, 0x66, 0x69, 0x72, 0x6d, 0x65, 0x64, 0x2e
        };
        return asBytes (intBuffer);
    }
    private static byte[] getPaymentProtocolRequestBytes() {
        int intBuffer[] =
                {0x08, 0x01, 0x12, 0x0b, 0x78, 0x35, 0x30, 0x39, 0x2b, 0x73, 0x68, 0x61, 0x32, 0x35, 0x36, 0x1a, 0xb8, 0x1d, 0x0a, 0xc9, 0x0b, 0x30, 0x82
                        , 0x05, 0xc5, 0x30, 0x82, 0x04, 0xad, 0xa0, 0x03, 0x02, 0x01, 0x02, 0x02, 0x07, 0x2b, 0x85, 0x8c, 0x53, 0xee, 0xed, 0x2f, 0x30, 0x0d, 0x06, 0x09, 0x2a, 0x86, 0x48, 0x86
                        , 0xf7, 0x0d, 0x01, 0x01, 0x05, 0x05, 0x00, 0x30, 0x81, 0xca, 0x31, 0x0b, 0x30, 0x09, 0x06, 0x03, 0x55, 0x04, 0x06, 0x13, 0x02, 0x55, 0x53, 0x31, 0x10, 0x30, 0x0e, 0x06
                        , 0x03, 0x55, 0x04, 0x08, 0x13, 0x07, 0x41, 0x72, 0x69, 0x7a, 0x6f, 0x6e, 0x61, 0x31, 0x13, 0x30, 0x11, 0x06, 0x03, 0x55, 0x04, 0x07, 0x13, 0x0a, 0x53, 0x63, 0x6f, 0x74
                        , 0x74, 0x73, 0x64, 0x61, 0x6c, 0x65, 0x31, 0x1a, 0x30, 0x18, 0x06, 0x03, 0x55, 0x04, 0x0a, 0x13, 0x11, 0x47, 0x6f, 0x44, 0x61, 0x64, 0x64, 0x79, 0x2e, 0x63, 0x6f, 0x6d
                        , 0x2c, 0x20, 0x49, 0x6e, 0x63, 0x2e, 0x31, 0x33, 0x30, 0x31, 0x06, 0x03, 0x55, 0x04, 0x0b, 0x13, 0x2a, 0x68, 0x74, 0x74, 0x70, 0x3a, 0x2f, 0x2f, 0x63, 0x65, 0x72, 0x74
                        , 0x69, 0x66, 0x69, 0x63, 0x61, 0x74, 0x65, 0x73, 0x2e, 0x67, 0x6f, 0x64, 0x61, 0x64, 0x64, 0x79, 0x2e, 0x63, 0x6f, 0x6d, 0x2f, 0x72, 0x65, 0x70, 0x6f, 0x73, 0x69, 0x74
                        , 0x6f, 0x72, 0x79, 0x31, 0x30, 0x30, 0x2e, 0x06, 0x03, 0x55, 0x04, 0x03, 0x13, 0x27, 0x47, 0x6f, 0x20, 0x44, 0x61, 0x64, 0x64, 0x79, 0x20, 0x53, 0x65, 0x63, 0x75, 0x72
                        , 0x65, 0x20, 0x43, 0x65, 0x72, 0x74, 0x69, 0x66, 0x69, 0x63, 0x61, 0x74, 0x69, 0x6f, 0x6e, 0x20, 0x41, 0x75, 0x74, 0x68, 0x6f, 0x72, 0x69, 0x74, 0x79, 0x31, 0x11, 0x30
                        , 0x0f, 0x06, 0x03, 0x55, 0x04, 0x05, 0x13, 0x08, 0x30, 0x37, 0x39, 0x36, 0x39, 0x32, 0x38, 0x37, 0x30, 0x1e, 0x17, 0x0d, 0x31, 0x33, 0x30, 0x34, 0x32, 0x35, 0x31, 0x39
                        , 0x31, 0x31, 0x30, 0x30, 0x5a, 0x17, 0x0d, 0x31, 0x35, 0x30, 0x34, 0x32, 0x35, 0x31, 0x39, 0x31, 0x31, 0x30, 0x30, 0x5a, 0x30, 0x81, 0xbe, 0x31, 0x13, 0x30, 0x11, 0x06
                        , 0x0b, 0x2b, 0x06, 0x01, 0x04, 0x01, 0x82, 0x37, 0x3c, 0x02, 0x01, 0x03, 0x13, 0x02, 0x55, 0x53, 0x31, 0x19, 0x30, 0x17, 0x06, 0x0b, 0x2b, 0x06, 0x01, 0x04, 0x01, 0x82
                        , 0x37, 0x3c, 0x02, 0x01, 0x02, 0x13, 0x08, 0x44, 0x65, 0x6c, 0x61, 0x77, 0x61, 0x72, 0x65, 0x31, 0x1d, 0x30, 0x1b, 0x06, 0x03, 0x55, 0x04, 0x0f, 0x13, 0x14, 0x50, 0x72
                        , 0x69, 0x76, 0x61, 0x74, 0x65, 0x20, 0x4f, 0x72, 0x67, 0x61, 0x6e, 0x69, 0x7a, 0x61, 0x74, 0x69, 0x6f, 0x6e, 0x31, 0x10, 0x30, 0x0e, 0x06, 0x03, 0x55, 0x04, 0x05, 0x13
                        , 0x07, 0x35, 0x31, 0x36, 0x33, 0x39, 0x36, 0x36, 0x31, 0x0b, 0x30, 0x09, 0x06, 0x03, 0x55, 0x04, 0x06, 0x13, 0x02, 0x55, 0x53, 0x31, 0x10, 0x30, 0x0e, 0x06, 0x03, 0x55
                        , 0x04, 0x08, 0x13, 0x07, 0x47, 0x65, 0x6f, 0x72, 0x67, 0x69, 0x61, 0x31, 0x10, 0x30, 0x0e, 0x06, 0x03, 0x55, 0x04, 0x07, 0x13, 0x07, 0x41, 0x74, 0x6c, 0x61, 0x6e, 0x74
                        , 0x61, 0x31, 0x15, 0x30, 0x13, 0x06, 0x03, 0x55, 0x04, 0x0a, 0x13, 0x0c, 0x42, 0x69, 0x74, 0x50, 0x61, 0x79, 0x2c, 0x20, 0x49, 0x6e, 0x63, 0x2e, 0x31, 0x13, 0x30, 0x11
                        , 0x06, 0x03, 0x55, 0x04, 0x03, 0x13, 0x0a, 0x62, 0x69, 0x74, 0x70, 0x61, 0x79, 0x2e, 0x63, 0x6f, 0x6d, 0x30, 0x82, 0x01, 0x22, 0x30, 0x0d, 0x06, 0x09, 0x2a, 0x86, 0x48
                        , 0x86, 0xf7, 0x0d, 0x01, 0x01, 0x01, 0x05, 0x00, 0x03, 0x82, 0x01, 0x0f, 0x00, 0x30, 0x82, 0x01, 0x0a, 0x02, 0x82, 0x01, 0x01, 0x00, 0xc4, 0x6e, 0xef, 0xc2, 0x8b, 0x15
                        , 0x7d, 0x03, 0x71, 0x7f, 0x0c, 0x00, 0xa1, 0xd6, 0x7b, 0xa7, 0x61, 0x2c, 0x1f, 0x2b, 0x56, 0x21, 0x82, 0xce, 0x99, 0x60, 0x2c, 0x47, 0x68, 0xff, 0x8f, 0xbd, 0x10, 0x66
                        , 0x85, 0xd9, 0x39, 0x26, 0x32, 0x66, 0xbb, 0x9e, 0x10, 0x7d, 0x05, 0x7d, 0xb8, 0x44, 0x50, 0x2d, 0x8e, 0xc6, 0x1e, 0x88, 0x7e, 0xa5, 0x5b, 0x55, 0xc2, 0xc1, 0x71, 0x21
                        , 0x89, 0x64, 0x54, 0xa3, 0x19, 0xf6, 0x5b, 0x3d, 0xb3, 0x4c, 0x86, 0x29, 0xa7, 0x5b, 0x3e, 0x12, 0x3f, 0xe2, 0x07, 0x6d, 0x85, 0xcf, 0x4f, 0x64, 0x4a, 0xe3, 0xf6, 0xfb
                        , 0x84, 0x29, 0xc5, 0xa7, 0x83, 0x0d, 0xf4, 0x65, 0x85, 0x9c, 0x4d, 0x6c, 0x0b, 0xcd, 0xbc, 0x12, 0x86, 0x5f, 0xab, 0x22, 0x18, 0xbd, 0x65, 0xf2, 0xb2, 0x53, 0x00, 0x12
                        , 0xce, 0x49, 0x96, 0x98, 0xcc, 0xae, 0x02, 0x59, 0xac, 0x0b, 0x34, 0x70, 0xa8, 0x56, 0x6b, 0x70, 0x5e, 0x1a, 0x66, 0x1a, 0xd8, 0x28, 0x64, 0x29, 0xac, 0xf0, 0xb3, 0x13
                        , 0x6e, 0x4c, 0xdf, 0x4d, 0x91, 0x19, 0x08, 0x4a, 0x5b, 0x6e, 0xcf, 0x19, 0x76, 0x94, 0xc2, 0xb5, 0x57, 0x82, 0x70, 0x12, 0x11, 0xca, 0x28, 0xda, 0xfa, 0x6d, 0x96, 0xac
                        , 0xec, 0xc2, 0x23, 0x2a, 0xc5, 0xe9, 0xa8, 0x61, 0x81, 0xd4, 0xf7, 0x41, 0x7f, 0xd8, 0xd9, 0x38, 0x50, 0x7f, 0x6d, 0x0c, 0x62, 0x52, 0x94, 0x02, 0x16, 0x30, 0x09, 0x46
                        , 0xf7, 0x62, 0x70, 0x13, 0xd7, 0x49, 0x98, 0xe0, 0x92, 0x2d, 0x4b, 0x9c, 0x97, 0xa7, 0x77, 0x9b, 0x1d, 0x56, 0xf3, 0x0c, 0x07, 0xd0, 0x26, 0x9b, 0x15, 0x89, 0xbd, 0x60
                        , 0x4d, 0x38, 0x4a, 0x52, 0x37, 0x21, 0x3c, 0x75, 0xd0, 0xc6, 0xbf, 0x81, 0x1b, 0xce, 0x8c, 0xdb, 0xbb, 0x06, 0xc1, 0xa2, 0xc6, 0xe4, 0x79, 0xd2, 0x71, 0xfd, 0x02, 0x03
                        , 0x01, 0x00, 0x01, 0xa3, 0x82, 0x01, 0xb8, 0x30, 0x82, 0x01, 0xb4, 0x30, 0x0f, 0x06, 0x03, 0x55, 0x1d, 0x13, 0x01, 0x01, 0xff, 0x04, 0x05, 0x30, 0x03, 0x01, 0x01, 0x00
                        , 0x30, 0x1d, 0x06, 0x03, 0x55, 0x1d, 0x25, 0x04, 0x16, 0x30, 0x14, 0x06, 0x08, 0x2b, 0x06, 0x01, 0x05, 0x05, 0x07, 0x03, 0x01, 0x06, 0x08, 0x2b, 0x06, 0x01, 0x05, 0x05
                        , 0x07, 0x03, 0x02, 0x30, 0x0e, 0x06, 0x03, 0x55, 0x1d, 0x0f, 0x01, 0x01, 0xff, 0x04, 0x04, 0x03, 0x02, 0x05, 0xa0, 0x30, 0x33, 0x06, 0x03, 0x55, 0x1d, 0x1f, 0x04, 0x2c
                        , 0x30, 0x2a, 0x30, 0x28, 0xa0, 0x26, 0xa0, 0x24, 0x86, 0x22, 0x68, 0x74, 0x74, 0x70, 0x3a, 0x2f, 0x2f, 0x63, 0x72, 0x6c, 0x2e, 0x67, 0x6f, 0x64, 0x61, 0x64, 0x64, 0x79
                        , 0x2e, 0x63, 0x6f, 0x6d, 0x2f, 0x67, 0x64, 0x73, 0x33, 0x2d, 0x37, 0x32, 0x2e, 0x63, 0x72, 0x6c, 0x30, 0x53, 0x06, 0x03, 0x55, 0x1d, 0x20, 0x04, 0x4c, 0x30, 0x4a, 0x30
                        , 0x48, 0x06, 0x0b, 0x60, 0x86, 0x48, 0x01, 0x86, 0xfd, 0x6d, 0x01, 0x07, 0x17, 0x03, 0x30, 0x39, 0x30, 0x37, 0x06, 0x08, 0x2b, 0x06, 0x01, 0x05, 0x05, 0x07, 0x02, 0x01
                        , 0x16, 0x2b, 0x68, 0x74, 0x74, 0x70, 0x3a, 0x2f, 0x2f, 0x63, 0x65, 0x72, 0x74, 0x69, 0x66, 0x69, 0x63, 0x61, 0x74, 0x65, 0x73, 0x2e, 0x67, 0x6f, 0x64, 0x61, 0x64, 0x64
                        , 0x79, 0x2e, 0x63, 0x6f, 0x6d, 0x2f, 0x72, 0x65, 0x70, 0x6f, 0x73, 0x69, 0x74, 0x6f, 0x72, 0x79, 0x2f, 0x30, 0x81, 0x80, 0x06, 0x08, 0x2b, 0x06, 0x01, 0x05, 0x05, 0x07
                        , 0x01, 0x01, 0x04, 0x74, 0x30, 0x72, 0x30, 0x24, 0x06, 0x08, 0x2b, 0x06, 0x01, 0x05, 0x05, 0x07, 0x30, 0x01, 0x86, 0x18, 0x68, 0x74, 0x74, 0x70, 0x3a, 0x2f, 0x2f, 0x6f
                        , 0x63, 0x73, 0x70, 0x2e, 0x67, 0x6f, 0x64, 0x61, 0x64, 0x64, 0x79, 0x2e, 0x63, 0x6f, 0x6d, 0x2f, 0x30, 0x4a, 0x06, 0x08, 0x2b, 0x06, 0x01, 0x05, 0x05, 0x07, 0x30, 0x02
                        , 0x86, 0x3e, 0x68, 0x74, 0x74, 0x70, 0x3a, 0x2f, 0x2f, 0x63, 0x65, 0x72, 0x74, 0x69, 0x66, 0x69, 0x63, 0x61, 0x74, 0x65, 0x73, 0x2e, 0x67, 0x6f, 0x64, 0x61, 0x64, 0x64
                        , 0x79, 0x2e, 0x63, 0x6f, 0x6d, 0x2f, 0x72, 0x65, 0x70, 0x6f, 0x73, 0x69, 0x74, 0x6f, 0x72, 0x79, 0x2f, 0x67, 0x64, 0x5f, 0x69, 0x6e, 0x74, 0x65, 0x72, 0x6d, 0x65, 0x64
                        , 0x69, 0x61, 0x74, 0x65, 0x2e, 0x63, 0x72, 0x74, 0x30, 0x1f, 0x06, 0x03, 0x55, 0x1d, 0x23, 0x04, 0x18, 0x30, 0x16, 0x80, 0x14, 0xfd, 0xac, 0x61, 0x32, 0x93, 0x6c, 0x45
                        , 0xd6, 0xe2, 0xee, 0x85, 0x5f, 0x9a, 0xba, 0xe7, 0x76, 0x99, 0x68, 0xcc, 0xe7, 0x30, 0x25, 0x06, 0x03, 0x55, 0x1d, 0x11, 0x04, 0x1e, 0x30, 0x1c, 0x82, 0x0a, 0x62, 0x69
                        , 0x74, 0x70, 0x61, 0x79, 0x2e, 0x63, 0x6f, 0x6d, 0x82, 0x0e, 0x77, 0x77, 0x77, 0x2e, 0x62, 0x69, 0x74, 0x70, 0x61, 0x79, 0x2e, 0x63, 0x6f, 0x6d, 0x30, 0x1d, 0x06, 0x03
                        , 0x55, 0x1d, 0x0e, 0x04, 0x16, 0x04, 0x14, 0xb9, 0x41, 0x17, 0x56, 0x7a, 0xe7, 0xc3, 0xef, 0x50, 0x72, 0x82, 0xac, 0xc4, 0xd5, 0x51, 0xc6, 0xbf, 0x7f, 0xa4, 0x4a, 0x30
                        , 0x0d, 0x06, 0x09, 0x2a, 0x86, 0x48, 0x86, 0xf7, 0x0d, 0x01, 0x01, 0x05, 0x05, 0x00, 0x03, 0x82, 0x01, 0x01, 0x00, 0xb8, 0xd5, 0xac, 0xa9, 0x63, 0xa6, 0xf9, 0xa0, 0xb5
                        , 0xc5, 0xaf, 0x03, 0x4a, 0xcc, 0x83, 0x2a, 0x13, 0xf1, 0xbb, 0xeb, 0x93, 0x2d, 0x39, 0x7a, 0x7d, 0x4b, 0xd3, 0xa4, 0x5e, 0x6a, 0x3d, 0x6d, 0xb3, 0x10, 0x9a, 0x23, 0x54
                        , 0xa8, 0x08, 0x14, 0xee, 0x3e, 0x6c, 0x7c, 0xef, 0xf5, 0xd7, 0xf4, 0xa9, 0x83, 0xdb, 0xde, 0x55, 0xf0, 0x96, 0xba, 0x99, 0x2d, 0x0f, 0xff, 0x4f, 0xe1, 0xa9, 0x2e, 0xaa
                        , 0xb7, 0x9b, 0xd1, 0x47, 0xb3, 0x52, 0x1e, 0xe3, 0x61, 0x2c, 0xee, 0x2c, 0xf7, 0x59, 0x5b, 0xc6, 0x35, 0xa1, 0xfe, 0xef, 0xc6, 0xdb, 0x5c, 0x58, 0x3a, 0x59, 0x23, 0xc7
                        , 0x1c, 0x86, 0x4d, 0xda, 0xcb, 0xcf, 0xf4, 0x63, 0xe9, 0x96, 0x7f, 0x4c, 0x02, 0xbd, 0xd7, 0x72, 0x71, 0x63, 0x55, 0x75, 0x96, 0x7e, 0xc2, 0x3e, 0x8b, 0x6c, 0xdb, 0xda
                        , 0xb6, 0x32, 0xce, 0x79, 0x07, 0x2f, 0x47, 0x70, 0x4a, 0x6e, 0xf1, 0xf1, 0x60, 0x31, 0x08, 0x37, 0xde, 0x45, 0x6e, 0x4a, 0x01, 0xa2, 0x2b, 0xbf, 0x89, 0xd8, 0xe0, 0xf5
                        , 0x26, 0x7d, 0xfb, 0x71, 0x99, 0x8a, 0xde, 0x3e, 0xa2, 0x60, 0xdc, 0x9b, 0xc6, 0xcf, 0xf3, 0x89, 0x9a, 0x88, 0xca, 0xf6, 0xa5, 0xe0, 0xea, 0x74, 0x97, 0xff, 0xbc, 0x42
                        , 0xed, 0x4f, 0xa6, 0x95, 0x51, 0xe5, 0xe0, 0xb2, 0x15, 0x6e, 0x9e, 0x2d, 0x22, 0x5b, 0xa7, 0xa5, 0xe5, 0x6d, 0xe5, 0xff, 0x13, 0x0a, 0x4c, 0x6e, 0x5f, 0x1a, 0x99, 0x68
                        , 0x68, 0x7b, 0x82, 0x62, 0x0f, 0x86, 0x17, 0x02, 0xd5, 0x6c, 0x44, 0x29, 0x79, 0x9f, 0xff, 0x9d, 0xb2, 0x56, 0x2b, 0xc2, 0xdc, 0xe9, 0x7f, 0xe7, 0xe3, 0x4a, 0x1f, 0xab
                        , 0xb0, 0x39, 0xe5, 0xe7, 0x8b, 0xd4, 0xda, 0xe6, 0x0f, 0x58, 0x68, 0xa5, 0xe8, 0xa3, 0xf8, 0xc3, 0x30, 0xe3, 0x7f, 0x38, 0xfb, 0xfe, 0x1f, 0x0a, 0xe2, 0x09, 0x30, 0x82
                        , 0x04, 0xde, 0x30, 0x82, 0x03, 0xc6, 0xa0, 0x03, 0x02, 0x01, 0x02, 0x02, 0x02, 0x03, 0x01, 0x30, 0x0d, 0x06, 0x09, 0x2a, 0x86, 0x48, 0x86, 0xf7, 0x0d, 0x01, 0x01, 0x05
                        , 0x05, 0x00, 0x30, 0x63, 0x31, 0x0b, 0x30, 0x09, 0x06, 0x03, 0x55, 0x04, 0x06, 0x13, 0x02, 0x55, 0x53, 0x31, 0x21, 0x30, 0x1f, 0x06, 0x03, 0x55, 0x04, 0x0a, 0x13, 0x18
                        , 0x54, 0x68, 0x65, 0x20, 0x47, 0x6f, 0x20, 0x44, 0x61, 0x64, 0x64, 0x79, 0x20, 0x47, 0x72, 0x6f, 0x75, 0x70, 0x2c, 0x20, 0x49, 0x6e, 0x63, 0x2e, 0x31, 0x31, 0x30, 0x2f
                        , 0x06, 0x03, 0x55, 0x04, 0x0b, 0x13, 0x28, 0x47, 0x6f, 0x20, 0x44, 0x61, 0x64, 0x64, 0x79, 0x20, 0x43, 0x6c, 0x61, 0x73, 0x73, 0x20, 0x32, 0x20, 0x43, 0x65, 0x72, 0x74
                        , 0x69, 0x66, 0x69, 0x63, 0x61, 0x74, 0x69, 0x6f, 0x6e, 0x20, 0x41, 0x75, 0x74, 0x68, 0x6f, 0x72, 0x69, 0x74, 0x79, 0x30, 0x1e, 0x17, 0x0d, 0x30, 0x36, 0x31, 0x31, 0x31
                        , 0x36, 0x30, 0x31, 0x35, 0x34, 0x33, 0x37, 0x5a, 0x17, 0x0d, 0x32, 0x36, 0x31, 0x31, 0x31, 0x36, 0x30, 0x31, 0x35, 0x34, 0x33, 0x37, 0x5a, 0x30, 0x81, 0xca, 0x31, 0x0b
                        , 0x30, 0x09, 0x06, 0x03, 0x55, 0x04, 0x06, 0x13, 0x02, 0x55, 0x53, 0x31, 0x10, 0x30, 0x0e, 0x06, 0x03, 0x55, 0x04, 0x08, 0x13, 0x07, 0x41, 0x72, 0x69, 0x7a, 0x6f, 0x6e
                        , 0x61, 0x31, 0x13, 0x30, 0x11, 0x06, 0x03, 0x55, 0x04, 0x07, 0x13, 0x0a, 0x53, 0x63, 0x6f, 0x74, 0x74, 0x73, 0x64, 0x61, 0x6c, 0x65, 0x31, 0x1a, 0x30, 0x18, 0x06, 0x03
                        , 0x55, 0x04, 0x0a, 0x13, 0x11, 0x47, 0x6f, 0x44, 0x61, 0x64, 0x64, 0x79, 0x2e, 0x63, 0x6f, 0x6d, 0x2c, 0x20, 0x49, 0x6e, 0x63, 0x2e, 0x31, 0x33, 0x30, 0x31, 0x06, 0x03
                        , 0x55, 0x04, 0x0b, 0x13, 0x2a, 0x68, 0x74, 0x74, 0x70, 0x3a, 0x2f, 0x2f, 0x63, 0x65, 0x72, 0x74, 0x69, 0x66, 0x69, 0x63, 0x61, 0x74, 0x65, 0x73, 0x2e, 0x67, 0x6f, 0x64
                        , 0x61, 0x64, 0x64, 0x79, 0x2e, 0x63, 0x6f, 0x6d, 0x2f, 0x72, 0x65, 0x70, 0x6f, 0x73, 0x69, 0x74, 0x6f, 0x72, 0x79, 0x31, 0x30, 0x30, 0x2e, 0x06, 0x03, 0x55, 0x04, 0x03
                        , 0x13, 0x27, 0x47, 0x6f, 0x20, 0x44, 0x61, 0x64, 0x64, 0x79, 0x20, 0x53, 0x65, 0x63, 0x75, 0x72, 0x65, 0x20, 0x43, 0x65, 0x72, 0x74, 0x69, 0x66, 0x69, 0x63, 0x61, 0x74
                        , 0x69, 0x6f, 0x6e, 0x20, 0x41, 0x75, 0x74, 0x68, 0x6f, 0x72, 0x69, 0x74, 0x79, 0x31, 0x11, 0x30, 0x0f, 0x06, 0x03, 0x55, 0x04, 0x05, 0x13, 0x08, 0x30, 0x37, 0x39, 0x36
                        , 0x39, 0x32, 0x38, 0x37, 0x30, 0x82, 0x01, 0x22, 0x30, 0x0d, 0x06, 0x09, 0x2a, 0x86, 0x48, 0x86, 0xf7, 0x0d, 0x01, 0x01, 0x01, 0x05, 0x00, 0x03, 0x82, 0x01, 0x0f, 0x00
                        , 0x30, 0x82, 0x01, 0x0a, 0x02, 0x82, 0x01, 0x01, 0x00, 0xc4, 0x2d, 0xd5, 0x15, 0x8c, 0x9c, 0x26, 0x4c, 0xec, 0x32, 0x35, 0xeb, 0x5f, 0xb8, 0x59, 0x01, 0x5a, 0xa6, 0x61
                        , 0x81, 0x59, 0x3b, 0x70, 0x63, 0xab, 0xe3, 0xdc, 0x3d, 0xc7, 0x2a, 0xb8, 0xc9, 0x33, 0xd3, 0x79, 0xe4, 0x3a, 0xed, 0x3c, 0x30, 0x23, 0x84, 0x8e, 0xb3, 0x30, 0x14, 0xb6
                        , 0xb2, 0x87, 0xc3, 0x3d, 0x95, 0x54, 0x04, 0x9e, 0xdf, 0x99, 0xdd, 0x0b, 0x25, 0x1e, 0x21, 0xde, 0x65, 0x29, 0x7e, 0x35, 0xa8, 0xa9, 0x54, 0xeb, 0xf6, 0xf7, 0x32, 0x39
                        , 0xd4, 0x26, 0x55, 0x95, 0xad, 0xef, 0xfb, 0xfe, 0x58, 0x86, 0xd7, 0x9e, 0xf4, 0x00, 0x8d, 0x8c, 0x2a, 0x0c, 0xbd, 0x42, 0x04, 0xce, 0xa7, 0x3f, 0x04, 0xf6, 0xee, 0x80
                        , 0xf2, 0xaa, 0xef, 0x52, 0xa1, 0x69, 0x66, 0xda, 0xbe, 0x1a, 0xad, 0x5d, 0xda, 0x2c, 0x66, 0xea, 0x1a, 0x6b, 0xbb, 0xe5, 0x1a, 0x51, 0x4a, 0x00, 0x2f, 0x48, 0xc7, 0x98
                        , 0x75, 0xd8, 0xb9, 0x29, 0xc8, 0xee, 0xf8, 0x66, 0x6d, 0x0a, 0x9c, 0xb3, 0xf3, 0xfc, 0x78, 0x7c, 0xa2, 0xf8, 0xa3, 0xf2, 0xb5, 0xc3, 0xf3, 0xb9, 0x7a, 0x91, 0xc1, 0xa7
                        , 0xe6, 0x25, 0x2e, 0x9c, 0xa8, 0xed, 0x12, 0x65, 0x6e, 0x6a, 0xf6, 0x12, 0x44, 0x53, 0x70, 0x30, 0x95, 0xc3, 0x9c, 0x2b, 0x58, 0x2b, 0x3d, 0x08, 0x74, 0x4a, 0xf2, 0xbe
                        , 0x51, 0xb0, 0xbf, 0x87, 0xd0, 0x4c, 0x27, 0x58, 0x6b, 0xb5, 0x35, 0xc5, 0x9d, 0xaf, 0x17, 0x31, 0xf8, 0x0b, 0x8f, 0xee, 0xad, 0x81, 0x36, 0x05, 0x89, 0x08, 0x98, 0xcf
                        , 0x3a, 0xaf, 0x25, 0x87, 0xc0, 0x49, 0xea, 0xa7, 0xfd, 0x67, 0xf7, 0x45, 0x8e, 0x97, 0xcc, 0x14, 0x39, 0xe2, 0x36, 0x85, 0xb5, 0x7e, 0x1a, 0x37, 0xfd, 0x16, 0xf6, 0x71
                        , 0x11, 0x9a, 0x74, 0x30, 0x16, 0xfe, 0x13, 0x94, 0xa3, 0x3f, 0x84, 0x0d, 0x4f, 0x02, 0x03, 0x01, 0x00, 0x01, 0xa3, 0x82, 0x01, 0x32, 0x30, 0x82, 0x01, 0x2e, 0x30, 0x1d
                        , 0x06, 0x03, 0x55, 0x1d, 0x0e, 0x04, 0x16, 0x04, 0x14, 0xfd, 0xac, 0x61, 0x32, 0x93, 0x6c, 0x45, 0xd6, 0xe2, 0xee, 0x85, 0x5f, 0x9a, 0xba, 0xe7, 0x76, 0x99, 0x68, 0xcc
                        , 0xe7, 0x30, 0x1f, 0x06, 0x03, 0x55, 0x1d, 0x23, 0x04, 0x18, 0x30, 0x16, 0x80, 0x14, 0xd2, 0xc4, 0xb0, 0xd2, 0x91, 0xd4, 0x4c, 0x11, 0x71, 0xb3, 0x61, 0xcb, 0x3d, 0xa1
                        , 0xfe, 0xdd, 0xa8, 0x6a, 0xd4, 0xe3, 0x30, 0x12, 0x06, 0x03, 0x55, 0x1d, 0x13, 0x01, 0x01, 0xff, 0x04, 0x08, 0x30, 0x06, 0x01, 0x01, 0xff, 0x02, 0x01, 0x00, 0x30, 0x33
                        , 0x06, 0x08, 0x2b, 0x06, 0x01, 0x05, 0x05, 0x07, 0x01, 0x01, 0x04, 0x27, 0x30, 0x25, 0x30, 0x23, 0x06, 0x08, 0x2b, 0x06, 0x01, 0x05, 0x05, 0x07, 0x30, 0x01, 0x86, 0x17
                        , 0x68, 0x74, 0x74, 0x70, 0x3a, 0x2f, 0x2f, 0x6f, 0x63, 0x73, 0x70, 0x2e, 0x67, 0x6f, 0x64, 0x61, 0x64, 0x64, 0x79, 0x2e, 0x63, 0x6f, 0x6d, 0x30, 0x46, 0x06, 0x03, 0x55
                        , 0x1d, 0x1f, 0x04, 0x3f, 0x30, 0x3d, 0x30, 0x3b, 0xa0, 0x39, 0xa0, 0x37, 0x86, 0x35, 0x68, 0x74, 0x74, 0x70, 0x3a, 0x2f, 0x2f, 0x63, 0x65, 0x72, 0x74, 0x69, 0x66, 0x69
                        , 0x63, 0x61, 0x74, 0x65, 0x73, 0x2e, 0x67, 0x6f, 0x64, 0x61, 0x64, 0x64, 0x79, 0x2e, 0x63, 0x6f, 0x6d, 0x2f, 0x72, 0x65, 0x70, 0x6f, 0x73, 0x69, 0x74, 0x6f, 0x72, 0x79
                        , 0x2f, 0x67, 0x64, 0x72, 0x6f, 0x6f, 0x74, 0x2e, 0x63, 0x72, 0x6c, 0x30, 0x4b, 0x06, 0x03, 0x55, 0x1d, 0x20, 0x04, 0x44, 0x30, 0x42, 0x30, 0x40, 0x06, 0x04, 0x55, 0x1d
                        , 0x20, 0x00, 0x30, 0x38, 0x30, 0x36, 0x06, 0x08, 0x2b, 0x06, 0x01, 0x05, 0x05, 0x07, 0x02, 0x01, 0x16, 0x2a, 0x68, 0x74, 0x74, 0x70, 0x3a, 0x2f, 0x2f, 0x63, 0x65, 0x72
                        , 0x74, 0x69, 0x66, 0x69, 0x63, 0x61, 0x74, 0x65, 0x73, 0x2e, 0x67, 0x6f, 0x64, 0x61, 0x64, 0x64, 0x79, 0x2e, 0x63, 0x6f, 0x6d, 0x2f, 0x72, 0x65, 0x70, 0x6f, 0x73, 0x69
                        , 0x74, 0x6f, 0x72, 0x79, 0x30, 0x0e, 0x06, 0x03, 0x55, 0x1d, 0x0f, 0x01, 0x01, 0xff, 0x04, 0x04, 0x03, 0x02, 0x01, 0x06, 0x30, 0x0d, 0x06, 0x09, 0x2a, 0x86, 0x48, 0x86
                        , 0xf7, 0x0d, 0x01, 0x01, 0x05, 0x05, 0x00, 0x03, 0x82, 0x01, 0x01, 0x00, 0xd2, 0x86, 0xc0, 0xec, 0xbd, 0xf9, 0xa1, 0xb6, 0x67, 0xee, 0x66, 0x0b, 0xa2, 0x06, 0x3a, 0x04
                        , 0x50, 0x8e, 0x15, 0x72, 0xac, 0x4a, 0x74, 0x95, 0x53, 0xcb, 0x37, 0xcb, 0x44, 0x49, 0xef, 0x07, 0x90, 0x6b, 0x33, 0xd9, 0x96, 0xf0, 0x94, 0x56, 0xa5, 0x13, 0x30, 0x05
                        , 0x3c, 0x85, 0x32, 0x21, 0x7b, 0xc9, 0xc7, 0x0a, 0xa8, 0x24, 0xa4, 0x90, 0xde, 0x46, 0xd3, 0x25, 0x23, 0x14, 0x03, 0x67, 0xc2, 0x10, 0xd6, 0x6f, 0x0f, 0x5d, 0x7b, 0x7a
                        , 0xcc, 0x9f, 0xc5, 0x58, 0x2a, 0xc1, 0xc4, 0x9e, 0x21, 0xa8, 0x5a, 0xf3, 0xac, 0xa4, 0x46, 0xf3, 0x9e, 0xe4, 0x63, 0xcb, 0x2f, 0x90, 0xa4, 0x29, 0x29, 0x01, 0xd9, 0x72
                        , 0x2c, 0x29, 0xdf, 0x37, 0x01, 0x27, 0xbc, 0x4f, 0xee, 0x68, 0xd3, 0x21, 0x8f, 0xc0, 0xb3, 0xe4, 0xf5, 0x09, 0xed, 0xd2, 0x10, 0xaa, 0x53, 0xb4, 0xbe, 0xf0, 0xcc, 0x59
                        , 0x0b, 0xd6, 0x3b, 0x96, 0x1c, 0x95, 0x24, 0x49, 0xdf, 0xce, 0xec, 0xfd, 0xa7, 0x48, 0x91, 0x14, 0x45, 0x0e, 0x3a, 0x36, 0x6f, 0xda, 0x45, 0xb3, 0x45, 0xa2, 0x41, 0xc9
                        , 0xd4, 0xd7, 0x44, 0x4e, 0x3e, 0xb9, 0x74, 0x76, 0xd5, 0xa2, 0x13, 0x55, 0x2c, 0xc6, 0x87, 0xa3, 0xb5, 0x99, 0xac, 0x06, 0x84, 0x87, 0x7f, 0x75, 0x06, 0xfc, 0xbf, 0x14
                        , 0x4c, 0x0e, 0xcc, 0x6e, 0xc4, 0xdf, 0x3d, 0xb7, 0x12, 0x71, 0xf4, 0xe8, 0xf1, 0x51, 0x40, 0x22, 0x28, 0x49, 0xe0, 0x1d, 0x4b, 0x87, 0xa8, 0x34, 0xcc, 0x06, 0xa2, 0xdd
                        , 0x12, 0x5a, 0xd1, 0x86, 0x36, 0x64, 0x03, 0x35, 0x6f, 0x6f, 0x77, 0x6e, 0xeb, 0xf2, 0x85, 0x50, 0x98, 0x5e, 0xab, 0x03, 0x53, 0xad, 0x91, 0x23, 0x63, 0x1f, 0x16, 0x9c
                        , 0xcd, 0xb9, 0xb2, 0x05, 0x63, 0x3a, 0xe1, 0xf4, 0x68, 0x1b, 0x17, 0x05, 0x35, 0x95, 0x53, 0xee, 0x0a, 0x84, 0x08, 0x30, 0x82, 0x04, 0x00, 0x30, 0x82, 0x02, 0xe8, 0xa0
                        , 0x03, 0x02, 0x01, 0x02, 0x02, 0x01, 0x00, 0x30, 0x0d, 0x06, 0x09, 0x2a, 0x86, 0x48, 0x86, 0xf7, 0x0d, 0x01, 0x01, 0x05, 0x05, 0x00, 0x30, 0x63, 0x31, 0x0b, 0x30, 0x09
                        , 0x06, 0x03, 0x55, 0x04, 0x06, 0x13, 0x02, 0x55, 0x53, 0x31, 0x21, 0x30, 0x1f, 0x06, 0x03, 0x55, 0x04, 0x0a, 0x13, 0x18, 0x54, 0x68, 0x65, 0x20, 0x47, 0x6f, 0x20, 0x44
                        , 0x61, 0x64, 0x64, 0x79, 0x20, 0x47, 0x72, 0x6f, 0x75, 0x70, 0x2c, 0x20, 0x49, 0x6e, 0x63, 0x2e, 0x31, 0x31, 0x30, 0x2f, 0x06, 0x03, 0x55, 0x04, 0x0b, 0x13, 0x28, 0x47
                        , 0x6f, 0x20, 0x44, 0x61, 0x64, 0x64, 0x79, 0x20, 0x43, 0x6c, 0x61, 0x73, 0x73, 0x20, 0x32, 0x20, 0x43, 0x65, 0x72, 0x74, 0x69, 0x66, 0x69, 0x63, 0x61, 0x74, 0x69, 0x6f
                        , 0x6e, 0x20, 0x41, 0x75, 0x74, 0x68, 0x6f, 0x72, 0x69, 0x74, 0x79, 0x30, 0x1e, 0x17, 0x0d, 0x30, 0x34, 0x30, 0x36, 0x32, 0x39, 0x31, 0x37, 0x30, 0x36, 0x32, 0x30, 0x5a
                        , 0x17, 0x0d, 0x33, 0x34, 0x30, 0x36, 0x32, 0x39, 0x31, 0x37, 0x30, 0x36, 0x32, 0x30, 0x5a, 0x30, 0x63, 0x31, 0x0b, 0x30, 0x09, 0x06, 0x03, 0x55, 0x04, 0x06, 0x13, 0x02
                        , 0x55, 0x53, 0x31, 0x21, 0x30, 0x1f, 0x06, 0x03, 0x55, 0x04, 0x0a, 0x13, 0x18, 0x54, 0x68, 0x65, 0x20, 0x47, 0x6f, 0x20, 0x44, 0x61, 0x64, 0x64, 0x79, 0x20, 0x47, 0x72
                        , 0x6f, 0x75, 0x70, 0x2c, 0x20, 0x49, 0x6e, 0x63, 0x2e, 0x31, 0x31, 0x30, 0x2f, 0x06, 0x03, 0x55, 0x04, 0x0b, 0x13, 0x28, 0x47, 0x6f, 0x20, 0x44, 0x61, 0x64, 0x64, 0x79
                        , 0x20, 0x43, 0x6c, 0x61, 0x73, 0x73, 0x20, 0x32, 0x20, 0x43, 0x65, 0x72, 0x74, 0x69, 0x66, 0x69, 0x63, 0x61, 0x74, 0x69, 0x6f, 0x6e, 0x20, 0x41, 0x75, 0x74, 0x68, 0x6f
                        , 0x72, 0x69, 0x74, 0x79, 0x30, 0x82, 0x01, 0x20, 0x30, 0x0d, 0x06, 0x09, 0x2a, 0x86, 0x48, 0x86, 0xf7, 0x0d, 0x01, 0x01, 0x01, 0x05, 0x00, 0x03, 0x82, 0x01, 0x0d, 0x00
                        , 0x30, 0x82, 0x01, 0x08, 0x02, 0x82, 0x01, 0x01, 0x00, 0xde, 0x9d, 0xd7, 0xea, 0x57, 0x18, 0x49, 0xa1, 0x5b, 0xeb, 0xd7, 0x5f, 0x48, 0x86, 0xea, 0xbe, 0xdd, 0xff, 0xe4
                        , 0xef, 0x67, 0x1c, 0xf4, 0x65, 0x68, 0xb3, 0x57, 0x71, 0xa0, 0x5e, 0x77, 0xbb, 0xed, 0x9b, 0x49, 0xe9, 0x70, 0x80, 0x3d, 0x56, 0x18, 0x63, 0x08, 0x6f, 0xda, 0xf2, 0xcc
                        , 0xd0, 0x3f, 0x7f, 0x02, 0x54, 0x22, 0x54, 0x10, 0xd8, 0xb2, 0x81, 0xd4, 0xc0, 0x75, 0x3d, 0x4b, 0x7f, 0xc7, 0x77, 0xc3, 0x3e, 0x78, 0xab, 0x1a, 0x03, 0xb5, 0x20, 0x6b
                        , 0x2f, 0x6a, 0x2b, 0xb1, 0xc5, 0x88, 0x7e, 0xc4, 0xbb, 0x1e, 0xb0, 0xc1, 0xd8, 0x45, 0x27, 0x6f, 0xaa, 0x37, 0x58, 0xf7, 0x87, 0x26, 0xd7, 0xd8, 0x2d, 0xf6, 0xa9, 0x17
                        , 0xb7, 0x1f, 0x72, 0x36, 0x4e, 0xa6, 0x17, 0x3f, 0x65, 0x98, 0x92, 0xdb, 0x2a, 0x6e, 0x5d, 0xa2, 0xfe, 0x88, 0xe0, 0x0b, 0xde, 0x7f, 0xe5, 0x8d, 0x15, 0xe1, 0xeb, 0xcb
                        , 0x3a, 0xd5, 0xe2, 0x12, 0xa2, 0x13, 0x2d, 0xd8, 0x8e, 0xaf, 0x5f, 0x12, 0x3d, 0xa0, 0x08, 0x05, 0x08, 0xb6, 0x5c, 0xa5, 0x65, 0x38, 0x04, 0x45, 0x99, 0x1e, 0xa3, 0x60
                        , 0x60, 0x74, 0xc5, 0x41, 0xa5, 0x72, 0x62, 0x1b, 0x62, 0xc5, 0x1f, 0x6f, 0x5f, 0x1a, 0x42, 0xbe, 0x02, 0x51, 0x65, 0xa8, 0xae, 0x23, 0x18, 0x6a, 0xfc, 0x78, 0x03, 0xa9
                        , 0x4d, 0x7f, 0x80, 0xc3, 0xfa, 0xab, 0x5a, 0xfc, 0xa1, 0x40, 0xa4, 0xca, 0x19, 0x16, 0xfe, 0xb2, 0xc8, 0xef, 0x5e, 0x73, 0x0d, 0xee, 0x77, 0xbd, 0x9a, 0xf6, 0x79, 0x98
                        , 0xbc, 0xb1, 0x07, 0x67, 0xa2, 0x15, 0x0d, 0xdd, 0xa0, 0x58, 0xc6, 0x44, 0x7b, 0x0a, 0x3e, 0x62, 0x28, 0x5f, 0xba, 0x41, 0x07, 0x53, 0x58, 0xcf, 0x11, 0x7e, 0x38, 0x74
                        , 0xc5, 0xf8, 0xff, 0xb5, 0x69, 0x90, 0x8f, 0x84, 0x74, 0xea, 0x97, 0x1b, 0xaf, 0x02, 0x01, 0x03, 0xa3, 0x81, 0xc0, 0x30, 0x81, 0xbd, 0x30, 0x1d, 0x06, 0x03, 0x55, 0x1d
                        , 0x0e, 0x04, 0x16, 0x04, 0x14, 0xd2, 0xc4, 0xb0, 0xd2, 0x91, 0xd4, 0x4c, 0x11, 0x71, 0xb3, 0x61, 0xcb, 0x3d, 0xa1, 0xfe, 0xdd, 0xa8, 0x6a, 0xd4, 0xe3, 0x30, 0x81, 0x8d
                        , 0x06, 0x03, 0x55, 0x1d, 0x23, 0x04, 0x81, 0x85, 0x30, 0x81, 0x82, 0x80, 0x14, 0xd2, 0xc4, 0xb0, 0xd2, 0x91, 0xd4, 0x4c, 0x11, 0x71, 0xb3, 0x61, 0xcb, 0x3d, 0xa1, 0xfe
                        , 0xdd, 0xa8, 0x6a, 0xd4, 0xe3, 0xa1, 0x67, 0xa4, 0x65, 0x30, 0x63, 0x31, 0x0b, 0x30, 0x09, 0x06, 0x03, 0x55, 0x04, 0x06, 0x13, 0x02, 0x55, 0x53, 0x31, 0x21, 0x30, 0x1f
                        , 0x06, 0x03, 0x55, 0x04, 0x0a, 0x13, 0x18, 0x54, 0x68, 0x65, 0x20, 0x47, 0x6f, 0x20, 0x44, 0x61, 0x64, 0x64, 0x79, 0x20, 0x47, 0x72, 0x6f, 0x75, 0x70, 0x2c, 0x20, 0x49
                        , 0x6e, 0x63, 0x2e, 0x31, 0x31, 0x30, 0x2f, 0x06, 0x03, 0x55, 0x04, 0x0b, 0x13, 0x28, 0x47, 0x6f, 0x20, 0x44, 0x61, 0x64, 0x64, 0x79, 0x20, 0x43, 0x6c, 0x61, 0x73, 0x73
                        , 0x20, 0x32, 0x20, 0x43, 0x65, 0x72, 0x74, 0x69, 0x66, 0x69, 0x63, 0x61, 0x74, 0x69, 0x6f, 0x6e, 0x20, 0x41, 0x75, 0x74, 0x68, 0x6f, 0x72, 0x69, 0x74, 0x79, 0x82, 0x01
                        , 0x00, 0x30, 0x0c, 0x06, 0x03, 0x55, 0x1d, 0x13, 0x04, 0x05, 0x30, 0x03, 0x01, 0x01, 0xff, 0x30, 0x0d, 0x06, 0x09, 0x2a, 0x86, 0x48, 0x86, 0xf7, 0x0d, 0x01, 0x01, 0x05
                        , 0x05, 0x00, 0x03, 0x82, 0x01, 0x01, 0x00, 0x32, 0x4b, 0xf3, 0xb2, 0xca, 0x3e, 0x91, 0xfc, 0x12, 0xc6, 0xa1, 0x07, 0x8c, 0x8e, 0x77, 0xa0, 0x33, 0x06, 0x14, 0x5c, 0x90
                        , 0x1e, 0x18, 0xf7, 0x08, 0xa6, 0x3d, 0x0a, 0x19, 0xf9, 0x87, 0x80, 0x11, 0x6e, 0x69, 0xe4, 0x96, 0x17, 0x30, 0xff, 0x34, 0x91, 0x63, 0x72, 0x38, 0xee, 0xcc, 0x1c, 0x01
                        , 0xa3, 0x1d, 0x94, 0x28, 0xa4, 0x31, 0xf6, 0x7a, 0xc4, 0x54, 0xd7, 0xf6, 0xe5, 0x31, 0x58, 0x03, 0xa2, 0xcc, 0xce, 0x62, 0xdb, 0x94, 0x45, 0x73, 0xb5, 0xbf, 0x45, 0xc9
                        , 0x24, 0xb5, 0xd5, 0x82, 0x02, 0xad, 0x23, 0x79, 0x69, 0x8d, 0xb8, 0xb6, 0x4d, 0xce, 0xcf, 0x4c, 0xca, 0x33, 0x23, 0xe8, 0x1c, 0x88, 0xaa, 0x9d, 0x8b, 0x41, 0x6e, 0x16
                        , 0xc9, 0x20, 0xe5, 0x89, 0x9e, 0xcd, 0x3b, 0xda, 0x70, 0xf7, 0x7e, 0x99, 0x26, 0x20, 0x14, 0x54, 0x25, 0xab, 0x6e, 0x73, 0x85, 0xe6, 0x9b, 0x21, 0x9d, 0x0a, 0x6c, 0x82
                        , 0x0e, 0xa8, 0xf8, 0xc2, 0x0c, 0xfa, 0x10, 0x1e, 0x6c, 0x96, 0xef, 0x87, 0x0d, 0xc4, 0x0f, 0x61, 0x8b, 0xad, 0xee, 0x83, 0x2b, 0x95, 0xf8, 0x8e, 0x92, 0x84, 0x72, 0x39
                        , 0xeb, 0x20, 0xea, 0x83, 0xed, 0x83, 0xcd, 0x97, 0x6e, 0x08, 0xbc, 0xeb, 0x4e, 0x26, 0xb6, 0x73, 0x2b, 0xe4, 0xd3, 0xf6, 0x4c, 0xfe, 0x26, 0x71, 0xe2, 0x61, 0x11, 0x74
                        , 0x4a, 0xff, 0x57, 0x1a, 0x87, 0x0f, 0x75, 0x48, 0x2e, 0xcf, 0x51, 0x69, 0x17, 0xa0, 0x02, 0x12, 0x61, 0x95, 0xd5, 0xd1, 0x40, 0xb2, 0x10, 0x4c, 0xee, 0xc4, 0xac, 0x10
                        , 0x43, 0xa6, 0xa5, 0x9e, 0x0a, 0xd5, 0x95, 0x62, 0x9a, 0x0d, 0xcf, 0x88, 0x82, 0xc5, 0x32, 0x0c, 0xe4, 0x2b, 0x9f, 0x45, 0xe6, 0x0d, 0x9f, 0x28, 0x9c, 0xb1, 0xb9, 0x2a
                        , 0x5a, 0x57, 0xad, 0x37, 0x0f, 0xaf, 0x1d, 0x7f, 0xdb, 0xbd, 0x9f, 0x22, 0x9b, 0x01, 0x0a, 0x04, 0x6d, 0x61, 0x69, 0x6e, 0x12, 0x1f, 0x08, 0xe0, 0xb6, 0x0d, 0x12, 0x19
                        , 0x76, 0xa9, 0x14, 0xa5, 0x33, 0xd4, 0xfa, 0x07, 0x66, 0x34, 0xaf, 0xef, 0x47, 0x45, 0x1f, 0x6a, 0xec, 0x8c, 0xdc, 0x1e, 0x49, 0xda, 0xf0, 0x88, 0xac, 0x18, 0xee, 0xe1
                        , 0x80, 0x9b, 0x05, 0x20, 0xf2, 0xe8, 0x80, 0x9b, 0x05, 0x2a, 0x39, 0x50, 0x61, 0x79, 0x6d, 0x65, 0x6e, 0x74, 0x20, 0x72, 0x65, 0x71, 0x75, 0x65, 0x73, 0x74, 0x20, 0x66
                        , 0x6f, 0x72, 0x20, 0x42, 0x69, 0x74, 0x50, 0x61, 0x79, 0x20, 0x69, 0x6e, 0x76, 0x6f, 0x69, 0x63, 0x65, 0x20, 0x38, 0x63, 0x58, 0x35, 0x52, 0x62, 0x4e, 0x38, 0x61, 0x6f
                        , 0x66, 0x63, 0x35, 0x33, 0x61, 0x57, 0x41, 0x6d, 0x35, 0x58, 0x46, 0x44, 0x32, 0x2b, 0x68, 0x74, 0x74, 0x70, 0x73, 0x3a, 0x2f, 0x2f, 0x62, 0x69, 0x74, 0x70, 0x61, 0x79
                        , 0x2e, 0x63, 0x6f, 0x6d, 0x2f, 0x69, 0x2f, 0x38, 0x63, 0x58, 0x35, 0x52, 0x62, 0x4e, 0x38, 0x61, 0x6f, 0x66, 0x63, 0x35, 0x33, 0x61, 0x57, 0x41, 0x6d, 0x35, 0x58, 0x46
                        , 0x44, 0x2a, 0x80, 0x02, 0x5e, 0xf8, 0x8b, 0xec, 0x4e, 0x09, 0xbe, 0x97, 0x9b, 0x07, 0x06, 0x64, 0x76, 0x4a, 0xfa, 0xe4, 0xfa, 0x3b, 0x1e, 0xca, 0x95, 0x47, 0x44, 0xa7
                        , 0x66, 0x99, 0xb1, 0x85, 0x30, 0x18, 0x3e, 0x6f, 0x46, 0x7e, 0xc5, 0x92, 0x39, 0x13, 0x66, 0x8c, 0x5a, 0xbe, 0x38, 0x2c, 0xb7, 0xef, 0x6a, 0x88, 0x58, 0xfa, 0xe6, 0x18
                        , 0x0c, 0x47, 0x8e, 0x81, 0x17, 0x9d, 0x39, 0x35, 0xcd, 0x53, 0x23, 0xf0, 0xc5, 0xcc, 0x2e, 0xea, 0x0f, 0x1e, 0x29, 0xb5, 0xa6, 0xb2, 0x65, 0x4b, 0x4c, 0xbd, 0xa3, 0x89
                        , 0xea, 0xee, 0x32, 0x21, 0x5c, 0x87, 0x77, 0xaf, 0xbb, 0xe0, 0x7d, 0x60, 0xa4, 0xf9, 0xfa, 0x07, 0xab, 0x6e, 0x9a, 0x6d, 0x3a, 0xd2, 0xa9, 0xef, 0xb5, 0x25, 0x22, 0x16
                        , 0x31, 0xc8, 0x04, 0x4e, 0xc7, 0x59, 0xd9, 0xc1, 0xfc, 0xcc, 0x39, 0xbb, 0x3e, 0xe4, 0xf4, 0x4e, 0xbc, 0x7c, 0x1c, 0xc8, 0x24, 0x83, 0x41, 0x44, 0x27, 0x22, 0xac, 0x88
                        , 0x0d, 0xa0, 0xc7, 0xd5, 0x9d, 0x69, 0x67, 0x06, 0xc7, 0xbc, 0xf0, 0x91, 0x01, 0xb4, 0x92, 0x5a, 0x07, 0x84, 0x22, 0x0a, 0x93, 0xc5, 0xb3, 0x09, 0xda, 0xd8, 0xe3, 0x26
                        , 0x61, 0xf2, 0xcc, 0xab, 0x4e, 0xc8, 0x68, 0xb2, 0xde, 0x00, 0x0f, 0x24, 0x2d, 0xb7, 0x3f, 0xff, 0xb2, 0x69, 0x37, 0xcf, 0x83, 0xed, 0x6d, 0x2e, 0xfa, 0xa7, 0x71, 0xd2
                        , 0xd2, 0xc6, 0x97, 0x84, 0x4b, 0x83, 0x94, 0x8c, 0x98, 0x25, 0x2b, 0x5f, 0x35, 0x2e, 0xdd, 0x4f, 0xe9, 0x6b, 0x29, 0xcb, 0xe0, 0xc9, 0xca, 0x3d, 0x10, 0x7a, 0x3e, 0xb7
                        , 0x90, 0xda, 0xb5, 0xdd, 0xd7, 0x3d, 0xe6, 0xc7, 0x48, 0xf2, 0x04, 0x7d, 0xb4, 0x25, 0xc8, 0x0c, 0x39, 0x13, 0x54, 0x73, 0xca, 0xca, 0xd3, 0x61, 0x9b, 0xaa, 0xf2, 0x8e
                        , 0x39, 0x1d, 0xa4, 0xa6, 0xc7, 0xb8, 0x2b, 0x74
                };
        return asBytes(intBuffer);
    }

    private static byte[] asBytes (int ints[]) {
        byte bytes[] = new byte[ints.length];

        for (int i = 0; i < ints.length; i++)
            bytes[i] = (byte) ints[i];

        return bytes;

    }
    
    private static void asserting (boolean assertion) {
        if (!assertion) {
            throw new AssertionError();
        }
    }

    private static String[] words = {
                "abandon",
                "ability",
                "able",
                "about",
                "above",
                "absent",
                "absorb",
                "abstract",
                "absurd",
                "abuse",
                "access",
                "accident",
                "account",
                "accuse",
                "achieve",
                "acid",
                "acoustic",
                "acquire",
                "across",
                "act",
                "action",
                "actor",
                "actress",
                "actual",
                "adapt",
                "add",
                "addict",
                "address",
                "adjust",
                "admit",
                "adult",
                "advance",
                "advice",
                "aerobic",
                "affair",
                "afford",
                "afraid",
                "again",
                "age",
                "agent",
                "agree",
                "ahead",
                "aim",
                "air",
                "airport",
                "aisle",
                "alarm",
                "album",
                "alcohol",
                "alert",
                "alien",
                "all",
                "alley",
                "allow",
                "almost",
                "alone",
                "alpha",
                "already",
                "also",
                "alter",
                "always",
                "amateur",
                "amazing",
                "among",
                "amount",
                "amused",
                "analyst",
                "anchor",
                "ancient",
                "anger",
                "angle",
                "angry",
                "animal",
                "ankle",
                "announce",
                "annual",
                "another",
                "answer",
                "antenna",
                "antique",
                "anxiety",
                "any",
                "apart",
                "apology",
                "appear",
                "apple",
                "approve",
                "april",
                "arch",
                "arctic",
                "area",
                "arena",
                "argue",
                "arm",
                "armed",
                "armor",
                "army",
                "around",
                "arrange",
                "arrest",
                "arrive",
                "arrow",
                "art",
                "artefact",
                "artist",
                "artwork",
                "ask",
                "aspect",
                "assault",
                "asset",
                "assist",
                "assume",
                "asthma",
                "athlete",
                "atom",
                "attack",
                "attend",
                "attitude",
                "attract",
                "auction",
                "audit",
                "august",
                "aunt",
                "author",
                "auto",
                "autumn",
                "average",
                "avocado",
                "avoid",
                "awake",
                "aware",
                "away",
                "awesome",
                "awful",
                "awkward",
                "axis",
                "baby",
                "bachelor",
                "bacon",
                "badge",
                "bag",
                "balance",
                "balcony",
                "ball",
                "bamboo",
                "banana",
                "banner",
                "bar",
                "barely",
                "bargain",
                "barrel",
                "base",
                "basic",
                "basket",
                "battle",
                "beach",
                "bean",
                "beauty",
                "because",
                "become",
                "beef",
                "before",
                "begin",
                "behave",
                "behind",
                "believe",
                "below",
                "belt",
                "bench",
                "benefit",
                "best",
                "betray",
                "better",
                "between",
                "beyond",
                "bicycle",
                "bid",
                "bike",
                "bind",
                "biology",
                "bird",
                "birth",
                "bitter",
                "black",
                "blade",
                "blame",
                "blanket",
                "blast",
                "bleak",
                "bless",
                "blind",
                "blood",
                "blossom",
                "blouse",
                "blue",
                "blur",
                "blush",
                "board",
                "boat",
                "body",
                "boil",
                "bomb",
                "bone",
                "bonus",
                "book",
                "boost",
                "border",
                "boring",
                "borrow",
                "boss",
                "bottom",
                "bounce",
                "box",
                "boy",
                "bracket",
                "brain",
                "brand",
                "brass",
                "brave",
                "bread",
                "breeze",
                "brick",
                "bridge",
                "brief",
                "bright",
                "bring",
                "brisk",
                "broccoli",
                "broken",
                "bronze",
                "broom",
                "brother",
                "brown",
                "brush",
                "bubble",
                "buddy",
                "budget",
                "buffalo",
                "build",
                "bulb",
                "bulk",
                "bullet",
                "bundle",
                "bunker",
                "burden",
                "burger",
                "burst",
                "bus",
                "business",
                "busy",
                "butter",
                "buyer",
                "buzz",
                "cabbage",
                "cabin",
                "cable",
                "cactus",
                "cage",
                "cake",
                "call",
                "calm",
                "camera",
                "camp",
                "can",
                "canal",
                "cancel",
                "candy",
                "cannon",
                "canoe",
                "canvas",
                "canyon",
                "capable",
                "capital",
                "captain",
                "car",
                "carbon",
                "card",
                "cargo",
                "carpet",
                "carry",
                "cart",
                "case",
                "cash",
                "casino",
                "castle",
                "casual",
                "cat",
                "catalog",
                "catch",
                "category",
                "cattle",
                "caught",
                "cause",
                "caution",
                "cave",
                "ceiling",
                "celery",
                "cement",
                "census",
                "century",
                "cereal",
                "certain",
                "chair",
                "chalk",
                "champion",
                "change",
                "chaos",
                "chapter",
                "charge",
                "chase",
                "chat",
                "cheap",
                "check",
                "cheese",
                "chef",
                "cherry",
                "chest",
                "chicken",
                "chief",
                "child",
                "chimney",
                "choice",
                "choose",
                "chronic",
                "chuckle",
                "chunk",
                "churn",
                "cigar",
                "cinnamon",
                "circle",
                "citizen",
                "city",
                "civil",
                "claim",
                "clap",
                "clarify",
                "claw",
                "clay",
                "clean",
                "clerk",
                "clever",
                "click",
                "client",
                "cliff",
                "climb",
                "clinic",
                "clip",
                "clock",
                "clog",
                "close",
                "cloth",
                "cloud",
                "clown",
                "club",
                "clump",
                "cluster",
                "clutch",
                "coach",
                "coast",
                "coconut",
                "code",
                "coffee",
                "coil",
                "coin",
                "collect",
                "color",
                "column",
                "combine",
                "come",
                "comfort",
                "comic",
                "common",
                "company",
                "concert",
                "conduct",
                "confirm",
                "congress",
                "connect",
                "consider",
                "control",
                "convince",
                "cook",
                "cool",
                "copper",
                "copy",
                "coral",
                "core",
                "corn",
                "correct",
                "cost",
                "cotton",
                "couch",
                "country",
                "couple",
                "course",
                "cousin",
                "cover",
                "coyote",
                "crack",
                "cradle",
                "craft",
                "cram",
                "crane",
                "crash",
                "crater",
                "crawl",
                "crazy",
                "cream",
                "credit",
                "creek",
                "crew",
                "cricket",
                "crime",
                "crisp",
                "critic",
                "crop",
                "cross",
                "crouch",
                "crowd",
                "crucial",
                "cruel",
                "cruise",
                "crumble",
                "crunch",
                "crush",
                "cry",
                "crystal",
                "cube",
                "culture",
                "cup",
                "cupboard",
                "curious",
                "current",
                "curtain",
                "curve",
                "cushion",
                "custom",
                "cute",
                "cycle",
                "dad",
                "damage",
                "damp",
                "dance",
                "danger",
                "daring",
                "dash",
                "daughter",
                "dawn",
                "day",
                "deal",
                "debate",
                "debris",
                "decade",
                "december",
                "decide",
                "decline",
                "decorate",
                "decrease",
                "deer",
                "defense",
                "define",
                "defy",
                "degree",
                "delay",
                "deliver",
                "demand",
                "demise",
                "denial",
                "dentist",
                "deny",
                "depart",
                "depend",
                "deposit",
                "depth",
                "deputy",
                "derive",
                "describe",
                "desert",
                "design",
                "desk",
                "despair",
                "destroy",
                "detail",
                "detect",
                "develop",
                "device",
                "devote",
                "diagram",
                "dial",
                "diamond",
                "diary",
                "dice",
                "diesel",
                "diet",
                "differ",
                "digital",
                "dignity",
                "dilemma",
                "dinner",
                "dinosaur",
                "direct",
                "dirt",
                "disagree",
                "discover",
                "disease",
                "dish",
                "dismiss",
                "disorder",
                "display",
                "distance",
                "divert",
                "divide",
                "divorce",
                "dizzy",
                "doctor",
                "document",
                "dog",
                "doll",
                "dolphin",
                "domain",
                "donate",
                "donkey",
                "donor",
                "door",
                "dose",
                "double",
                "dove",
                "draft",
                "dragon",
                "drama",
                "drastic",
                "draw",
                "dream",
                "dress",
                "drift",
                "drill",
                "drink",
                "drip",
                "drive",
                "drop",
                "drum",
                "dry",
                "duck",
                "dumb",
                "dune",
                "during",
                "dust",
                "dutch",
                "duty",
                "dwarf",
                "dynamic",
                "eager",
                "eagle",
                "early",
                "earn",
                "earth",
                "easily",
                "east",
                "easy",
                "echo",
                "ecology",
                "economy",
                "edge",
                "edit",
                "educate",
                "effort",
                "egg",
                "eight",
                "either",
                "elbow",
                "elder",
                "electric",
                "elegant",
                "element",
                "elephant",
                "elevator",
                "elite",
                "else",
                "embark",
                "embody",
                "embrace",
                "emerge",
                "emotion",
                "employ",
                "empower",
                "empty",
                "enable",
                "enact",
                "end",
                "endless",
                "endorse",
                "enemy",
                "energy",
                "enforce",
                "engage",
                "engine",
                "enhance",
                "enjoy",
                "enlist",
                "enough",
                "enrich",
                "enroll",
                "ensure",
                "enter",
                "entire",
                "entry",
                "envelope",
                "episode",
                "equal",
                "equip",
                "era",
                "erase",
                "erode",
                "erosion",
                "error",
                "erupt",
                "escape",
                "essay",
                "essence",
                "estate",
                "eternal",
                "ethics",
                "evidence",
                "evil",
                "evoke",
                "evolve",
                "exact",
                "example",
                "excess",
                "exchange",
                "excite",
                "exclude",
                "excuse",
                "execute",
                "exercise",
                "exhaust",
                "exhibit",
                "exile",
                "exist",
                "exit",
                "exotic",
                "expand",
                "expect",
                "expire",
                "explain",
                "expose",
                "express",
                "extend",
                "extra",
                "eye",
                "eyebrow",
                "fabric",
                "face",
                "faculty",
                "fade",
                "faint",
                "faith",
                "fall",
                "false",
                "fame",
                "family",
                "famous",
                "fan",
                "fancy",
                "fantasy",
                "farm",
                "fashion",
                "fat",
                "fatal",
                "father",
                "fatigue",
                "fault",
                "favorite",
                "feature",
                "february",
                "federal",
                "fee",
                "feed",
                "feel",
                "female",
                "fence",
                "festival",
                "fetch",
                "fever",
                "few",
                "fiber",
                "fiction",
                "field",
                "figure",
                "file",
                "film",
                "filter",
                "final",
                "find",
                "fine",
                "finger",
                "finish",
                "fire",
                "firm",
                "first",
                "fiscal",
                "fish",
                "fit",
                "fitness",
                "fix",
                "flag",
                "flame",
                "flash",
                "flat",
                "flavor",
                "flee",
                "flight",
                "flip",
                "float",
                "flock",
                "floor",
                "flower",
                "fluid",
                "flush",
                "fly",
                "foam",
                "focus",
                "fog",
                "foil",
                "fold",
                "follow",
                "food",
                "foot",
                "force",
                "forest",
                "forget",
                "fork",
                "fortune",
                "forum",
                "forward",
                "fossil",
                "foster",
                "found",
                "fox",
                "fragile",
                "frame",
                "frequent",
                "fresh",
                "friend",
                "fringe",
                "frog",
                "front",
                "frost",
                "frown",
                "frozen",
                "fruit",
                "fuel",
                "fun",
                "funny",
                "furnace",
                "fury",
                "future",
                "gadget",
                "gain",
                "galaxy",
                "gallery",
                "game",
                "gap",
                "garage",
                "garbage",
                "garden",
                "garlic",
                "garment",
                "gas",
                "gasp",
                "gate",
                "gather",
                "gauge",
                "gaze",
                "general",
                "genius",
                "genre",
                "gentle",
                "genuine",
                "gesture",
                "ghost",
                "giant",
                "gift",
                "giggle",
                "ginger",
                "giraffe",
                "girl",
                "give",
                "glad",
                "glance",
                "glare",
                "glass",
                "glide",
                "glimpse",
                "globe",
                "gloom",
                "glory",
                "glove",
                "glow",
                "glue",
                "goat",
                "goddess",
                "gold",
                "good",
                "goose",
                "gorilla",
                "gospel",
                "gossip",
                "govern",
                "gown",
                "grab",
                "grace",
                "grain",
                "grant",
                "grape",
                "grass",
                "gravity",
                "great",
                "green",
                "grid",
                "grief",
                "grit",
                "grocery",
                "group",
                "grow",
                "grunt",
                "guard",
                "guess",
                "guide",
                "guilt",
                "guitar",
                "gun",
                "gym",
                "habit",
                "hair",
                "half",
                "hammer",
                "hamster",
                "hand",
                "happy",
                "harbor",
                "hard",
                "harsh",
                "harvest",
                "hat",
                "have",
                "hawk",
                "hazard",
                "head",
                "health",
                "heart",
                "heavy",
                "hedgehog",
                "height",
                "hello",
                "helmet",
                "help",
                "hen",
                "hero",
                "hidden",
                "high",
                "hill",
                "hint",
                "hip",
                "hire",
                "history",
                "hobby",
                "hockey",
                "hold",
                "hole",
                "holiday",
                "hollow",
                "home",
                "honey",
                "hood",
                "hope",
                "horn",
                "horror",
                "horse",
                "hospital",
                "host",
                "hotel",
                "hour",
                "hover",
                "hub",
                "huge",
                "human",
                "humble",
                "humor",
                "hundred",
                "hungry",
                "hunt",
                "hurdle",
                "hurry",
                "hurt",
                "husband",
                "hybrid",
                "ice",
                "icon",
                "idea",
                "identify",
                "idle",
                "ignore",
                "ill",
                "illegal",
                "illness",
                "image",
                "imitate",
                "immense",
                "immune",
                "impact",
                "impose",
                "improve",
                "impulse",
                "inch",
                "include",
                "income",
                "increase",
                "index",
                "indicate",
                "indoor",
                "industry",
                "infant",
                "inflict",
                "inform",
                "inhale",
                "inherit",
                "initial",
                "inject",
                "injury",
                "inmate",
                "inner",
                "innocent",
                "input",
                "inquiry",
                "insane",
                "insect",
                "inside",
                "inspire",
                "install",
                "intact",
                "interest",
                "into",
                "invest",
                "invite",
                "involve",
                "iron",
                "island",
                "isolate",
                "issue",
                "item",
                "ivory",
                "jacket",
                "jaguar",
                "jar",
                "jazz",
                "jealous",
                "jeans",
                "jelly",
                "jewel",
                "job",
                "join",
                "joke",
                "journey",
                "joy",
                "judge",
                "juice",
                "jump",
                "jungle",
                "junior",
                "junk",
                "just",
                "kangaroo",
                "keen",
                "keep",
                "ketchup",
                "key",
                "kick",
                "kid",
                "kidney",
                "kind",
                "kingdom",
                "kiss",
                "kit",
                "kitchen",
                "kite",
                "kitten",
                "kiwi",
                "knee",
                "knife",
                "knock",
                "know",
                "lab",
                "label",
                "labor",
                "ladder",
                "lady",
                "lake",
                "lamp",
                "language",
                "laptop",
                "large",
                "later",
                "latin",
                "laugh",
                "laundry",
                "lava",
                "law",
                "lawn",
                "lawsuit",
                "layer",
                "lazy",
                "leader",
                "leaf",
                "learn",
                "leave",
                "lecture",
                "left",
                "leg",
                "legal",
                "legend",
                "leisure",
                "lemon",
                "lend",
                "length",
                "lens",
                "leopard",
                "lesson",
                "letter",
                "level",
                "liar",
                "liberty",
                "library",
                "license",
                "life",
                "lift",
                "light",
                "like",
                "limb",
                "limit",
                "link",
                "lion",
                "liquid",
                "list",
                "little",
                "live",
                "lizard",
                "load",
                "loan",
                "lobster",
                "local",
                "lock",
                "logic",
                "lonely",
                "long",
                "loop",
                "lottery",
                "loud",
                "lounge",
                "love",
                "loyal",
                "lucky",
                "luggage",
                "lumber",
                "lunar",
                "lunch",
                "luxury",
                "lyrics",
                "machine",
                "mad",
                "magic",
                "magnet",
                "maid",
                "mail",
                "main",
                "major",
                "make",
                "mammal",
                "man",
                "manage",
                "mandate",
                "mango",
                "mansion",
                "manual",
                "maple",
                "marble",
                "march",
                "margin",
                "marine",
                "market",
                "marriage",
                "mask",
                "mass",
                "master",
                "match",
                "material",
                "math",
                "matrix",
                "matter",
                "maximum",
                "maze",
                "meadow",
                "mean",
                "measure",
                "meat",
                "mechanic",
                "medal",
                "media",
                "melody",
                "melt",
                "member",
                "memory",
                "mention",
                "menu",
                "mercy",
                "merge",
                "merit",
                "merry",
                "mesh",
                "message",
                "metal",
                "method",
                "middle",
                "midnight",
                "milk",
                "million",
                "mimic",
                "mind",
                "minimum",
                "minor",
                "minute",
                "miracle",
                "mirror",
                "misery",
                "miss",
                "mistake",
                "mix",
                "mixed",
                "mixture",
                "mobile",
                "model",
                "modify",
                "mom",
                "moment",
                "monitor",
                "monkey",
                "monster",
                "month",
                "moon",
                "moral",
                "more",
                "morning",
                "mosquito",
                "mother",
                "motion",
                "motor",
                "mountain",
                "mouse",
                "move",
                "movie",
                "much",
                "muffin",
                "mule",
                "multiply",
                "muscle",
                "museum",
                "mushroom",
                "music",
                "must",
                "mutual",
                "myself",
                "mystery",
                "myth",
                "naive",
                "name",
                "napkin",
                "narrow",
                "nasty",
                "nation",
                "nature",
                "near",
                "neck",
                "need",
                "negative",
                "neglect",
                "neither",
                "nephew",
                "nerve",
                "nest",
                "net",
                "network",
                "neutral",
                "never",
                "news",
                "next",
                "nice",
                "night",
                "noble",
                "noise",
                "nominee",
                "noodle",
                "normal",
                "north",
                "nose",
                "notable",
                "note",
                "nothing",
                "notice",
                "novel",
                "now",
                "nuclear",
                "number",
                "nurse",
                "nut",
                "oak",
                "obey",
                "object",
                "oblige",
                "obscure",
                "observe",
                "obtain",
                "obvious",
                "occur",
                "ocean",
                "october",
                "odor",
                "off",
                "offer",
                "office",
                "often",
                "oil",
                "okay",
                "old",
                "olive",
                "olympic",
                "omit",
                "once",
                "one",
                "onion",
                "online",
                "only",
                "open",
                "opera",
                "opinion",
                "oppose",
                "option",
                "orange",
                "orbit",
                "orchard",
                "order",
                "ordinary",
                "organ",
                "orient",
                "original",
                "orphan",
                "ostrich",
                "other",
                "outdoor",
                "outer",
                "output",
                "outside",
                "oval",
                "oven",
                "over",
                "own",
                "owner",
                "oxygen",
                "oyster",
                "ozone",
                "pact",
                "paddle",
                "page",
                "pair",
                "palace",
                "palm",
                "panda",
                "panel",
                "panic",
                "panther",
                "paper",
                "parade",
                "parent",
                "park",
                "parrot",
                "party",
                "pass",
                "patch",
                "path",
                "patient",
                "patrol",
                "pattern",
                "pause",
                "pave",
                "payment",
                "peace",
                "peanut",
                "pear",
                "peasant",
                "pelican",
                "pen",
                "penalty",
                "pencil",
                "people",
                "pepper",
                "perfect",
                "permit",
                "person",
                "pet",
                "phone",
                "photo",
                "phrase",
                "physical",
                "piano",
                "picnic",
                "picture",
                "piece",
                "pig",
                "pigeon",
                "pill",
                "pilot",
                "pink",
                "pioneer",
                "pipe",
                "pistol",
                "pitch",
                "pizza",
                "place",
                "planet",
                "plastic",
                "plate",
                "play",
                "please",
                "pledge",
                "pluck",
                "plug",
                "plunge",
                "poem",
                "poet",
                "point",
                "polar",
                "pole",
                "police",
                "pond",
                "pony",
                "pool",
                "popular",
                "portion",
                "position",
                "possible",
                "post",
                "potato",
                "pottery",
                "poverty",
                "powder",
                "power",
                "practice",
                "praise",
                "predict",
                "prefer",
                "prepare",
                "present",
                "pretty",
                "prevent",
                "price",
                "pride",
                "primary",
                "print",
                "priority",
                "prison",
                "private",
                "prize",
                "problem",
                "process",
                "produce",
                "profit",
                "program",
                "project",
                "promote",
                "proof",
                "property",
                "prosper",
                "protect",
                "proud",
                "provide",
                "public",
                "pudding",
                "pull",
                "pulp",
                "pulse",
                "pumpkin",
                "punch",
                "pupil",
                "puppy",
                "purchase",
                "purity",
                "purpose",
                "purse",
                "push",
                "put",
                "puzzle",
                "pyramid",
                "quality",
                "quantum",
                "quarter",
                "question",
                "quick",
                "quit",
                "quiz",
                "quote",
                "rabbit",
                "raccoon",
                "race",
                "rack",
                "radar",
                "radio",
                "rail",
                "rain",
                "raise",
                "rally",
                "ramp",
                "ranch",
                "random",
                "range",
                "rapid",
                "rare",
                "rate",
                "rather",
                "raven",
                "raw",
                "razor",
                "ready",
                "real",
                "reason",
                "rebel",
                "rebuild",
                "recall",
                "receive",
                "recipe",
                "record",
                "recycle",
                "reduce",
                "reflect",
                "reform",
                "refuse",
                "region",
                "regret",
                "regular",
                "reject",
                "relax",
                "release",
                "relief",
                "rely",
                "remain",
                "remember",
                "remind",
                "remove",
                "render",
                "renew",
                "rent",
                "reopen",
                "repair",
                "repeat",
                "replace",
                "report",
                "require",
                "rescue",
                "resemble",
                "resist",
                "resource",
                "response",
                "result",
                "retire",
                "retreat",
                "return",
                "reunion",
                "reveal",
                "review",
                "reward",
                "rhythm",
                "rib",
                "ribbon",
                "rice",
                "rich",
                "ride",
                "ridge",
                "rifle",
                "right",
                "rigid",
                "ring",
                "riot",
                "ripple",
                "risk",
                "ritual",
                "rival",
                "river",
                "road",
                "roast",
                "robot",
                "robust",
                "rocket",
                "romance",
                "roof",
                "rookie",
                "room",
                "rose",
                "rotate",
                "rough",
                "round",
                "route",
                "royal",
                "rubber",
                "rude",
                "rug",
                "rule",
                "run",
                "runway",
                "rural",
                "sad",
                "saddle",
                "sadness",
                "safe",
                "sail",
                "salad",
                "salmon",
                "salon",
                "salt",
                "salute",
                "same",
                "sample",
                "sand",
                "satisfy",
                "satoshi",
                "sauce",
                "sausage",
                "save",
                "say",
                "scale",
                "scan",
                "scare",
                "scatter",
                "scene",
                "scheme",
                "school",
                "science",
                "scissors",
                "scorpion",
                "scout",
                "scrap",
                "screen",
                "script",
                "scrub",
                "sea",
                "search",
                "season",
                "seat",
                "second",
                "secret",
                "section",
                "security",
                "seed",
                "seek",
                "segment",
                "select",
                "sell",
                "seminar",
                "senior",
                "sense",
                "sentence",
                "series",
                "service",
                "session",
                "settle",
                "setup",
                "seven",
                "shadow",
                "shaft",
                "shallow",
                "share",
                "shed",
                "shell",
                "sheriff",
                "shield",
                "shift",
                "shine",
                "ship",
                "shiver",
                "shock",
                "shoe",
                "shoot",
                "shop",
                "short",
                "shoulder",
                "shove",
                "shrimp",
                "shrug",
                "shuffle",
                "shy",
                "sibling",
                "sick",
                "side",
                "siege",
                "sight",
                "sign",
                "silent",
                "silk",
                "silly",
                "silver",
                "similar",
                "simple",
                "since",
                "sing",
                "siren",
                "sister",
                "situate",
                "six",
                "size",
                "skate",
                "sketch",
                "ski",
                "skill",
                "skin",
                "skirt",
                "skull",
                "slab",
                "slam",
                "sleep",
                "slender",
                "slice",
                "slide",
                "slight",
                "slim",
                "slogan",
                "slot",
                "slow",
                "slush",
                "small",
                "smart",
                "smile",
                "smoke",
                "smooth",
                "snack",
                "snake",
                "snap",
                "sniff",
                "snow",
                "soap",
                "soccer",
                "social",
                "sock",
                "soda",
                "soft",
                "solar",
                "soldier",
                "solid",
                "solution",
                "solve",
                "someone",
                "song",
                "soon",
                "sorry",
                "sort",
                "soul",
                "sound",
                "soup",
                "source",
                "south",
                "space",
                "spare",
                "spatial",
                "spawn",
                "speak",
                "special",
                "speed",
                "spell",
                "spend",
                "sphere",
                "spice",
                "spider",
                "spike",
                "spin",
                "spirit",
                "split",
                "spoil",
                "sponsor",
                "spoon",
                "sport",
                "spot",
                "spray",
                "spread",
                "spring",
                "spy",
                "square",
                "squeeze",
                "squirrel",
                "stable",
                "stadium",
                "staff",
                "stage",
                "stairs",
                "stamp",
                "stand",
                "start",
                "state",
                "stay",
                "steak",
                "steel",
                "stem",
                "step",
                "stereo",
                "stick",
                "still",
                "sting",
                "stock",
                "stomach",
                "stone",
                "stool",
                "story",
                "stove",
                "strategy",
                "street",
                "strike",
                "strong",
                "struggle",
                "student",
                "stuff",
                "stumble",
                "style",
                "subject",
                "submit",
                "subway",
                "success",
                "such",
                "sudden",
                "suffer",
                "sugar",
                "suggest",
                "suit",
                "summer",
                "sun",
                "sunny",
                "sunset",
                "super",
                "supply",
                "supreme",
                "sure",
                "surface",
                "surge",
                "surprise",
                "surround",
                "survey",
                "suspect",
                "sustain",
                "swallow",
                "swamp",
                "swap",
                "swarm",
                "swear",
                "sweet",
                "swift",
                "swim",
                "swing",
                "switch",
                "sword",
                "symbol",
                "symptom",
                "syrup",
                "system",
                "table",
                "tackle",
                "tag",
                "tail",
                "talent",
                "talk",
                "tank",
                "tape",
                "target",
                "task",
                "taste",
                "tattoo",
                "taxi",
                "teach",
                "team",
                "tell",
                "ten",
                "tenant",
                "tennis",
                "tent",
                "term",
                "test",
                "text",
                "thank",
                "that",
                "theme",
                "then",
                "theory",
                "there",
                "they",
                "thing",
                "this",
                "thought",
                "three",
                "thrive",
                "throw",
                "thumb",
                "thunder",
                "ticket",
                "tide",
                "tiger",
                "tilt",
                "timber",
                "time",
                "tiny",
                "tip",
                "tired",
                "tissue",
                "title",
                "toast",
                "tobacco",
                "today",
                "toddler",
                "toe",
                "together",
                "toilet",
                "token",
                "tomato",
                "tomorrow",
                "tone",
                "tongue",
                "tonight",
                "tool",
                "tooth",
                "top",
                "topic",
                "topple",
                "torch",
                "tornado",
                "tortoise",
                "toss",
                "total",
                "tourist",
                "toward",
                "tower",
                "town",
                "toy",
                "track",
                "trade",
                "traffic",
                "tragic",
                "train",
                "transfer",
                "trap",
                "trash",
                "travel",
                "tray",
                "treat",
                "tree",
                "trend",
                "trial",
                "tribe",
                "trick",
                "trigger",
                "trim",
                "trip",
                "trophy",
                "trouble",
                "truck",
                "true",
                "truly",
                "trumpet",
                "trust",
                "truth",
                "try",
                "tube",
                "tuition",
                "tumble",
                "tuna",
                "tunnel",
                "turkey",
                "turn",
                "turtle",
                "twelve",
                "twenty",
                "twice",
                "twin",
                "twist",
                "two",
                "type",
                "typical",
                "ugly",
                "umbrella",
                "unable",
                "unaware",
                "uncle",
                "uncover",
                "under",
                "undo",
                "unfair",
                "unfold",
                "unhappy",
                "uniform",
                "unique",
                "unit",
                "universe",
                "unknown",
                "unlock",
                "until",
                "unusual",
                "unveil",
                "update",
                "upgrade",
                "uphold",
                "upon",
                "upper",
                "upset",
                "urban",
                "urge",
                "usage",
                "use",
                "used",
                "useful",
                "useless",
                "usual",
                "utility",
                "vacant",
                "vacuum",
                "vague",
                "valid",
                "valley",
                "valve",
                "van",
                "vanish",
                "vapor",
                "various",
                "vast",
                "vault",
                "vehicle",
                "velvet",
                "vendor",
                "venture",
                "venue",
                "verb",
                "verify",
                "version",
                "very",
                "vessel",
                "veteran",
                "viable",
                "vibrant",
                "vicious",
                "victory",
                "video",
                "view",
                "village",
                "vintage",
                "violin",
                "virtual",
                "virus",
                "visa",
                "visit",
                "visual",
                "vital",
                "vivid",
                "vocal",
                "voice",
                "void",
                "volcano",
                "volume",
                "vote",
                "voyage",
                "wage",
                "wagon",
                "wait",
                "walk",
                "wall",
                "walnut",
                "want",
                "warfare",
                "warm",
                "warrior",
                "wash",
                "wasp",
                "waste",
                "water",
                "wave",
                "way",
                "wealth",
                "weapon",
                "wear",
                "weasel",
                "weather",
                "web",
                "wedding",
                "weekend",
                "weird",
                "welcome",
                "west",
                "wet",
                "whale",
                "what",
                "wheat",
                "wheel",
                "when",
                "where",
                "whip",
                "whisper",
                "wide",
                "width",
                "wife",
                "wild",
                "will",
                "win",
                "window",
                "wine",
                "wing",
                "wink",
                "winner",
                "winter",
                "wire",
                "wisdom",
                "wise",
                "wish",
                "witness",
                "wolf",
                "woman",
                "wonder",
                "wood",
                "wool",
                "word",
                "work",
                "world",
                "worry",
                "worth",
                "wrap",
                "wreck",
                "wrestle",
                "wrist",
                "write",
                "wrong",
                "yard",
                "year",
                "yellow",
                "you",
                "young",
                "youth",
                "zebra",
                "zero",
                "zone",
                "zoo"
        };

}
