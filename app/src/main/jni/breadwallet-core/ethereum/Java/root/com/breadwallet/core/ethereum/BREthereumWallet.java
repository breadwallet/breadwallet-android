/*
 * EthereumWallet
 *
 * Created by Ed Gamble <ed@breadwallet.com> on 3/20/18.
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

import com.breadwallet.core.ethereum.BREthereumAmount.Unit;

import java.math.BigDecimal;
import java.util.LinkedList;
import java.util.List;

/**
 * An EthereumWallet holds either ETHER or TOKEN values.
 */
public class BREthereumWallet extends BREthereumLightNode.ReferenceWithDefaultUnit {

    //
    // Account
    //
    private BREthereumAccount account;

    public BREthereumAccount getAccount () {
        return account;
    }

    //
    // Network
    //
    private BREthereumNetwork network;

    public BREthereumNetwork getNetwork () {
        return network;
    }

    //
    // Token
    //
    private BREthereumToken token = null;

    public BREthereumToken getToken () {
        return token;
    }

    public boolean walletHoldsEther () {
        return null == token;
    }

    public String getSymbol () {
        return null == token ? "ETH" : token.getSymbol();
    }

    //
    // Constructors
    //

    protected BREthereumWallet (BREthereumLightNode node, long identifier,
                                BREthereumAccount account,
                                BREthereumNetwork network) {
        super (node, identifier, Unit.ETHER_ETHER);
        this.account = account;
        this.network = network;
    }

    protected BREthereumWallet (BREthereumLightNode node, long identifier,
                                BREthereumAccount account,
                                BREthereumNetwork network,
                                BREthereumToken token) {
        this (node, identifier, account, network);
        this.token = token;
        this.defaultUnit = Unit.TOKEN_DECIMAL;
        this.defaultUnitUsesToken = true;
    }

    //
    // Default Gas Price (ETH in WEI)
    //
    public static final long MAXIMUM_DEFAULT_GAS_PRICE = 100000000000000L; // 100 GWEI

    public static final long GAS_PRICE_1_GWEI  =  1000000000000L;
    public static final long GAS_PRICE_2_GWEI  =  2000000000000L;
    public static final long GAS_PRICE_4_GWEI  =  4000000000000L;
    public static final long GAS_PRICE_10_GWEI = 10000000000000L;
    public static final long GAS_PRICE_20_GWEI = 20000000000000L;

    public long getDefaultGasPrice () {
        return node.get().jniWalletGetDefaultGasPrice(identifier);
    }

    public void setDefaultGasPrice (long gasPrice) {
        assert (gasPrice <= MAXIMUM_DEFAULT_GAS_PRICE);
        node.get().jniWalletSetDefaultGasPrice(identifier, gasPrice);
    }

    //
    // Default Gas Limit (in 'gas')
    //
    public long getDefaultGasLimit () {
        return node.get().jniWalletGetDefaultGasLimit(identifier);
    }
    public void setDefaultGasLimit (long gasLimit) {
        node.get().jniWalletSetDefaultGasLimit (identifier, gasLimit);
    }

    //
    // Balance
    //

    /**
     * Get the current balance - as of the last call to updateBalance().  The `result` and the
     * wallet's `defaultUnit` specify the current balance.
     * @return
     */
    public String getBalance () {
        return getBalance(defaultUnit);
    }

    /**
     * Get the current balance (see above); `result` and `unit` specify the current balance.
     * @param unit
     * @return
     */
    public String getBalance(Unit unit) {
        validUnitOrException(unit);
        return node.get().jniGetWalletBalance(identifier, unit.jniValue);
    }

    /**
     * Convert the balance to fiat.
     *
     * As an (typical) example - assume the conversion is 600 $/ETH. `fiatPerCryto` would be
     * 600.0 and `unitForCrypto` would be Unit.ETHER_ETHER. Assume the amount was 1.2 ETH.  The
     * return will be $720.0
     *
     * @param fiatPerCrypto
     * @param unitForFiatPerCrypto
     * @return
     */
    public double getBalanceInFiat (double fiatPerCrypto,
                                   BREthereumAmount.Unit unitForFiatPerCrypto) {
        return fiatPerCrypto * Double.parseDouble(getBalance(unitForFiatPerCrypto));
    }

    /**
     * See `double getBalanceInFiat (double, Unit)`
     *
     */
    public BigDecimal getBalanceInFiat (BigDecimal fiatPerCrypto,
                                       BREthereumAmount.Unit unitForFiatPerCrypto) {
        return fiatPerCrypto.multiply(new BigDecimal(getBalance(unitForFiatPerCrypto)));
    }


    /**
     * Force a balance update (by querying the Ethereum Blockchain) and then assign the
     * wallet's balance.  Access with getBalance().
     */
    public void updateBalance () {
        node.get().jniForceWalletBalanceUpdate(identifier);
    }

    //
    // Estimate GasPrice and Gas
    //

    /**
     * Estimate the gasPrice needed for timely processing of transactions into the blockchain.
     * This method changes the wallet's defaultGasPrice which is used then by createTransaction().
     *
     * Estimate provided after callback BREthereumLightNode.estimateGasPrice
     */
    public void estimateGasPrice () {
        node.get().jniEstimateWalletGasPrice(identifier);
    }

    /**
     * Updates the gasEstimate for `transaction`.  To access the estimate use:
     *   transaction.getGasEstimate().
     *
     * Estimate provided after callback BREthereumLightNode.estimateGas
     *
     * @param transaction
     */
    public void estimateGas (BREthereumTransaction transaction) {
        node.get().jniTransactionEstimateGas(identifier, transaction.identifier);
    }

    //
    // Transactions
    //

    /**
     * Estimate the transaction fee for this wallet's typical transaction (e.g. transfer ETHER;
     * transfer TOKEN).  `amount` and `amountUnit` specify the quantity transferred; `result` and
     * `resultUnit` specify the fee.
     *
     * @param amount
     * @param amountUnit
     * @param resultUnit
     * @return
     */
    public String transactionEstimatedFee (String amount,
                                           Unit amountUnit,
                                           Unit resultUnit) {
        return node.get().jniTransactionEstimateFee(identifier, amount,
                amountUnit.jniValue,
                resultUnit.jniValue);
    }

    /**
     * Estimate the transaction fee using the default amount and result units.  See above.
     *
     * @param amount
     * @return
     */
    public String transactionEstimatedFee (String amount) {
        return transactionEstimatedFee(amount, defaultUnit, defaultUnit);
    }

    /**
     * Create a new transaction.
     *
     * The created transaction will use the wallet's defaultGasPrice and defaultGasLimit.
     *
     * Note: the wallet's defaultGasPrice can be changed prior to calling this method with either
     * setDefaultGasPrice() or estimateGasPrice().  In practice the defaultGasPrice only needs to
     * be updated occasionally per Ethereum blockchain 'congestion'.
     *
     * Note: the wallet's defaultGasLimit needs to be large enough to support the transaction.  For
     * transfers of ETHER, the gasLimit default is 21000 (which is standardized).  For transfers of
     * TOKENs, the gasLimit is set to 92000 - which is not guaranteed to be large enough but has
     * been set high enough to handle most all TOKENs.
     *
     * @param targetAddress
     * @param amount
     * @param amountUnit
     * @return
     */
    public BREthereumTransaction createTransaction(String targetAddress,
                                                   String amount,
                                                   Unit amountUnit) {
        BREthereumLightNode lightNode = node.get();

        // Note: The created transaction's unit will be `amountUnit`.  This unit may differ
        // from the wallet's defaultUnit - which should not be a problem.
        return new BREthereumTransaction(lightNode,
                lightNode.jniCreateTransaction(identifier,
                        targetAddress,
                        amount,
                        amountUnit.jniValue),
                amountUnit);
    }

     /**
     * Sign a transaction with `paperKey`
     *
     * @param transaction
     * @param paperKey
     */
    public void sign (BREthereumTransaction transaction,
                      String paperKey) {
        node.get().jniSignTransaction(identifier, transaction.identifier, paperKey);
    }

    /**
     * sign a transaction with `privateKey`
     *
     * @param transaction
     * @param privateKey
     */
    public void signWithPrivateKey (BREthereumTransaction transaction,
                                    byte[] privateKey) {
        node.get().jniSignTransactionWithPrivateKey(identifier, transaction.identifier, privateKey);
    }

    /**
     * Submit a transaction to the Ethereum Blockchain.
     *
     * @param transaction
     */
    public void submit (BREthereumTransaction transaction) {
        node.get().jniSubmitTransaction(identifier, transaction.identifier);
    }

    /**
     * Get an array of transactions held in wallet.
     *
     * @return
     */
    public BREthereumTransaction[] getTransactions () {
        long[] transactionIds = node.get().jniGetTransactions(identifier);

        // We don't know the length just yet; otherwise ...
        // BREthereumTransaction[] transactions = new BREthereumTransaction[transactionIds.length];

        List<BREthereumTransaction> transactions = new LinkedList<>();

        for (int i = 0; i < transactionIds.length; i++)
            if (node.get().jniTransactionIsSubmitted(transactionIds[i]))
                // transaction = node.get().transactionLookupOrCreate (tid)
                // transaction.setDefaultUnit (defaultUnit).
                transactions.add(new BREthereumTransaction(node.get(), transactionIds[i], defaultUnit));

        return transactions.toArray(new BREthereumTransaction[transactions.size()]);
    }
  }
