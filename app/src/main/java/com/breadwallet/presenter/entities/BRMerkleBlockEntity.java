package com.breadwallet.presenter.entities;

/**
 * BreadWallet
 * <p/>
 * Created by Mihail on 9/25/15.
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
public class BRMerkleBlockEntity {

    private long id;
    private byte[] buff;
//    private byte[] blockHash = new byte[32];
//    private byte[] flags = new byte[32];
//    private byte[] hashes = new byte[32];
//    private int height;
//    private byte[] merkleRoot = new byte[32];
//    private int nonce;
//    private byte[] prevBlock = new byte[32];
//    private int target;
//    private long timeStamp;
//    private int totalTransactions;
//    private int version;

    private BRMerkleBlockEntity(){

    }

    public BRMerkleBlockEntity(byte[] merkleBlockBuff){
        this.buff = merkleBlockBuff;
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public byte[] getBuff() {
        return buff;
    }

//    public byte[] getBlockHash() {
//        return blockHash;
//    }
//
//    public void setBlockHash(byte[] blockHash) {
//        this.blockHash = blockHash;
//    }
//
//    public byte[] getFlags() {
//        return flags;
//    }
//
//    public void setFlags(byte[] flags) {
//        this.flags = flags;
//    }
//
//    public byte[] getHashes() {
//        return hashes;
//    }
//
//    public void setHashes(byte[] hashes) {
//        this.hashes = hashes;
//    }
//
//    public int getHeight() {
//        return height;
//    }
//
//    public void setHeight(int height) {
//        this.height = height;
//    }
//
//    public byte[] getMerkleRoot() {
//        return merkleRoot;
//    }
//
//    public void setMerkleRoot(byte[] merkleRoot) {
//        this.merkleRoot = merkleRoot;
//    }
//
//    public int getNonce() {
//        return nonce;
//    }
//
//    public void setNonce(int nonce) {
//        this.nonce = nonce;
//    }
//
//    public byte[] getPrevBlock() {
//        return prevBlock;
//    }
//
//    public void setPrevBlock(byte[] prevBlock) {
//        this.prevBlock = prevBlock;
//    }
//
//    public int getTarget() {
//        return target;
//    }
//
//    public void setTarget(int target) {
//        this.target = target;
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
//    public int getTotalTransactions() {
//        return totalTransactions;
//    }
//
//    public void setTotalTransactions(int totalTransactions) {
//        this.totalTransactions = totalTransactions;
//    }
//
//    public int getVersion() {
//        return version;
//    }
//
//    public void setVersion(int version) {
//        this.version = version;
//    }
}
