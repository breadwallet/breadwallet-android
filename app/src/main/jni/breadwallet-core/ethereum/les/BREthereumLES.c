//
//  BREthereumLES.h
//  breadwallet-core Ethereum
//
//  Created by Lamont Samuels on 5/01/18.
//  Copyright (c) 2018 breadwallet LLC
//
//  Permission is hereby granted, free of charge, to any person obtaining a copy
//  of this software and associated documentation files (the "Software"), to deal
//  in the Software without restriction, including without limitation the rights
//  to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
//  copies of the Software, and to permit persons to whom the Software is
//  furnished to do so, subject to the following conditions:
//
//  The above copyright notice and this permission notice shall be included in
//  all copies or substantial portions of the Software.
//
//  THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
//  IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
//  FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
//  AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
//  LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
//  OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
//  THE SOFTWARE.


static void ethereumLESCreateHeader(void) {
/*
    BRRlpCoder coder = rlpCoderCreate();

    BRRlpItem headerItems[10];
    size_t itemsCount = 0;

 
        [+0x00, [key_0, value_0], [key_1, value_1], …]
        “protocolVersion” P: is 1 for the LPV1 protocol version.
        “networkId” P: should be 0 for testnet, 1 for mainnet.
        “headTd” P: Total Difficulty of the best chain. Integer, as found in block header.
        “headHash” B_32: the hash of the best (i.e. highest TD) known block.
        “headNum” P: the number of the best (i.e. highest TD) known block.
        “genesisHash” B_32: the hash of the Genesis block.
        “serveHeaders” (no value): present if the peer can serve header chain downloads.
        “serveChainSince” P: present if the peer can serve Body/Receipts ODR requests starting from the given block number.
        “serveStateSince” P: present if the peer can serve Proof/Code ODR requests starting from the given block number.
        “txRelay” (no value): present if the peer can relay transactions to the ETH network.
        “flowControl/BL”, “flowControl/MRC”, “flowControl/MRR”: see Client Side Flow Control
 
    headerItems[0] = rlpEncodeItemUInt64(coder, 0x00);

    //protocolVersion
    BRRlpItem keyPair [2];
    keyPair[0] = rlpEncodeItemString(coder, "protocolVersion");
    keyPair[1] = rlpEncodeItemUInt64(coder, 1);
    headerItems[1] = rlpEncodeListItems(coder, keyPair, 2);
 
    //networkId
    keyPair[0] = rlpEncodeItemString(coder, "networkId");
    keyPair[1] = rlpEncodeItemUInt64(coder, ctx->header.chainId);
    headerItems[2] = rlpEncodeListItems(coder, keyPair, 2);

    //headTd
    keyPair[0] = rlpEncodeItemString(coder, "headTd");
    keyPair[1] = rlpEncodeItemUInt64(coder, ctx->header.headerTd);
    headerItems[3] = rlpEncodeListItems(coder, keyPair, 2);
 
    //headHash
    keyPair[0] = rlpEncodeItemString(coder, "headHash");
    keyPair[1] = rlpEncodeItemBytes(coder, ctx->header.headHash, sizeof(ctx->header.headHash));
    headerItems[4] = rlpEncodeListItems(coder, keyPair, 2);
 
    //headNum
    keyPair[0] = rlpEncodeItemString(coder, "headNum");
    keyPair[1] = rlpEncodeItemUInt64(coder, ctx->header.headerTd);
    headerItems[5] = rlpEncodeListItems(coder, keyPair, 2);
 
    //genesisHash
    keyPair[0] = rlpEncodeItemString(coder, "genesisHash");
    keyPair[1] = rlpEncodeItemBytes(coder, ctx->header.genesisHash, sizeof(ctx->header.genesisHash));
    headerItems[6] = rlpEncodeListItems(coder, keyPair, 2);
 
    BRRlpItem encoding = rlpEncodeListItems(coder, headerItems, itemsCount);
    BRRlpData result;

    rlpDataExtract(coder, encoding, &result.bytes, &result.bytesCount);
 
    rlpCoderRelease(coder);
    */
}
