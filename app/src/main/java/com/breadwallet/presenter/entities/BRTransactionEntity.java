package com.breadwallet.presenter.entities;

/**
 * BreadWallet
 * <p/>
 * Created by Mihail on 9/23/15.
 * Copyright (c) 2015 Mihail Gutan <mihail@breadwallet.com>
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
public class BRTransactionEntity {
    private byte[] buff;
    private int blockheight;
    private long timestamp;
    private byte[] txHash;

    private BRTransactionEntity(){

    }

    public long getBlockheight() {
        return blockheight;
    }

    public void setBlockheight(int blockheight) {
        this.blockheight = blockheight;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }

    public byte[] getTxHash() {
        return txHash;
    }

    public void setTxHash(byte[] txHash) {
        this.txHash = txHash;
    }

//    private int blockHeight;
//    private int lockTime;
//    private long timeStamp;
//    private byte[] txHash = new byte[32];
//    private HashSet<BRTxInputEntity> inputs;
//    private HashSet<BRTxOutputEntity> outputs;

    public BRTransactionEntity(byte[] txBuff,int blockheight, long timestamp, byte[] txHash){
        this.blockheight = blockheight;
        this.timestamp = timestamp;
        this.buff = txBuff;
        this.txHash = txHash;
    }

    public byte[] getBuff() {
        return buff;
    }



//    public int getBlockHeight() {
//        return blockHeight;
//    }
//
//    public void setBlockHeight(int blockHeight) {
//        this.blockHeight = blockHeight;
//    }
//
//
//
//    public HashSet<BRTxInputEntity> getInputs() {
//        return inputs;
//    }
//
//    public void setInputs(HashSet<BRTxInputEntity> inputs) {
//        this.inputs = inputs;
//    }
//
//    public int getLockTime() {
//        return lockTime;
//    }
//
//    public void setLockTime(int lockTime) {
//        this.lockTime = lockTime;
//    }
//
//    public HashSet<BRTxOutputEntity> getOutputs() {
//        return outputs;
//    }
//
//    public void setOutputs(HashSet<BRTxOutputEntity> outputs) {
//        this.outputs = outputs;
//    }
//
//    public long getTimeStamp() {
//        return timeStamp;
//    }
//
//    public void setTimeStamp(long timeStamp) {
//        this.timeStamp = timeStamp;
//    }
//
//    public byte[] getTxHash() {
//        return txHash;
//    }
//
//    public void setTxHash(byte[] txHash) {
//        this.txHash = txHash;
//    }
}
