/*
 * EthereumTransaction
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

import java.math.BigDecimal;

/**
 *
 */
public class BREthereumTransaction extends BREthereumLightNode.ReferenceWithDefaultUnit {

    /**
     *
     * @param node
     * @param identifier
     * @param unit  The transaction's unit; should be identical with that unit used to create
     *              the transaction identifier.
     */
    protected BREthereumTransaction (BREthereumLightNode node, long identifier, BREthereumAmount.Unit unit) {
        super(node, identifier, unit);
    }

    public boolean isConfirmed () {
        return node.get().jniTransactionIsConfirmed(identifier);
    }

    public boolean isSubmitted () {
        return node.get().jniTransactionIsSubmitted(identifier);
    }

    public String getSourceAddress () {
        return node.get().jniTransactionSourceAddress(identifier);
    }

    public String getTargetAddress () {
        return node.get().jniTransactionTargetAddress(identifier);
    }

    public String getHash () {
        return node.get().jniTransactionGetHash (identifier);
    }

    //
    // Amount
    //
    public String getAmount () {
        return getAmount(defaultUnit);
    }

    public String getAmount(BREthereumAmount.Unit unit) {
        validUnitOrException(unit);
        return node.get().jniTransactionGetAmount(identifier, unit.jniValue);
    }

    /**
     * Convert the amount to fiat.
     *
     * As an (typical) example - assume the conversion is 600 $/ETH. `fiatPerCryto` would be
     * 600.0 and `unitForCrypto` would be Unit.ETHER_ETHER. Assume the amount was 1.2 ETH.  The
     * return will be $720.0
     *
     * @param fiatPerCrypto
     * @param unitForFiatPerCrypto
     * @return
     */
    public double getAmountInFiat (double fiatPerCrypto,
                                   BREthereumAmount.Unit unitForFiatPerCrypto) {
        return fiatPerCrypto * Double.parseDouble(getAmount(unitForFiatPerCrypto));
    }

    /**
     * See `double getAmountInFiat (double, Unit)`
     *
     */
    public BigDecimal getAmountInFiat (BigDecimal fiatPerCrypto,
                                       BREthereumAmount.Unit unitForFiatPerCrypto) {
        return fiatPerCrypto.multiply(new BigDecimal(getAmount(unitForFiatPerCrypto)));
    }

    //
    // Fee
    //

    /**
     * The fee in GWEI
     *
     * @return in GWEI
     */
    public String getFee () {
        return getFee(BREthereumAmount.Unit.ETHER_GWEI);
    }

    /**
     * The fee in `unit`
     *
     * @param unit must be an ether unit, otherwise fatal()
     * @return in `unit`
     */
    public String getFee (BREthereumAmount.Unit unit) {
        assert (!unit.isTokenUnit());
        return node.get().jniTransactionGetFee(identifier, unit.jniValue);
    }

    /**
     * Convert the fee to fiat.
     *
     * As an (typical) example - assume the conversion is 600 $/ETH. `fiatPerCryto` would be
     * 600.0 and `unitForCrypto` would be Unit.ETHER_ETHER. Assume the fee was 42000 GWEI (as
     * 21000 gas * 2 GWEI/gas).
     *
     * @param fiatPerCrypto
     * @param unitForFiatPerCrypto
     * @return
     */
    public double getFeeInFiat (double fiatPerCrypto,
                                BREthereumAmount.Unit unitForFiatPerCrypto) {
        return fiatPerCrypto * Double.parseDouble(getFee(unitForFiatPerCrypto));
    }

    /**
     * See `double getFeeInFiat (double, Unit)`
     *
     */
    public BigDecimal getFeeInFiat (BigDecimal fiatPerCrypto,
                                    BREthereumAmount.Unit unitForFiatPerCrypto) {
        return fiatPerCrypto.multiply(new BigDecimal(getFee(unitForFiatPerCrypto)));
    }

    //
    // Gas Price, Limit, Used
    //

    /**
     * The gasPrise in GWEI
     *
     * @return in GWEI
     */
    public String getGasPrice () {
        return getGasPrice(BREthereumAmount.Unit.ETHER_GWEI);
    }

    /**
     * The gasPrice in `unit`
     *
     * @param unit unit must be an ether unit, otherwise fatal()
     * @return in `unit`
     */
    public String getGasPrice (BREthereumAmount.Unit unit) {
        assert (!unit.isTokenUnit());
        return node.get().jniTransactionGetGasPrice(identifier, unit.jniValue);
    }

    /**
     * The gasLimit in `gas`
     *
     * @return in `gas`
     */
    public long getGasLimit () {
        return node.get().jniTransactionGetGasLimit(identifier);
    }

    /**
     * The gasUsed in `gas`
     *
     * @return in `gas`
     */
    public long getGasUsed () {
        return node.get().jniTransactionGetGasUsed(identifier);
    }

    //
    // Nonce
    //
    public long getNonce () {
        return node.get().jniTransactionGetNonce(identifier);
    }

    //
    // Block Number, Timestamp
    //
    public long getBlockNumber () {
        return node.get().jniTransactionGetBlockNumber(identifier);
    }

    public long getBlockTimestamp () {
        return node.get().jniTransactionGetBlockTimestamp(identifier);
    }

    public long getBlockConfirmations () {
        return node.get().jniTransactionGetBlockConfirmations(identifier);
    }
}
