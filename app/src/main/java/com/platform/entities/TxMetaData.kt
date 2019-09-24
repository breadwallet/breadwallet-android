/**
 * BreadWallet
 *
 *
 * Created by Mihail Gutan on <mihail></mihail>@breadwallet.com> 6/22/17.
 * Copyright (c) 2017 breadwallet LLC
 *
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package com.platform.entities

import org.json.JSONObject

data class TxMetaData(
        var deviceId: String? = null,
        var comment: String? = null,
        var exchangeCurrency: String? = null,
        var classVersion: Int = 0,
        var blockHeight: Int = 0,
        var exchangeRate: Double = 0.toDouble(),
        var fee: String? = null,
        var txSize: Int = 0,
        var creationTime: Int = 0
) {
    companion object {
        private const val CLASS_VERSION = "classVersion"
        private const val BLOCK_HEIGHT = "bh"
        private const val EXCHANGE_RATE = "er"
        private const val EXCHANGE_CURRENCY = "erc"
        private const val COMMENT = "comment"
        private const val FEE_RATE = "fr"
        private const val TX_SIZE = "s"
        private const val CREATION_TIME = "c"
        private const val DEVICE_ID = "dId"
    }

    /**
     * Key: “txn-<txHash>”
     *
     *
     * {
     * “classVersion”: 5, //used for versioning the schema
     * “bh”: 47583, //blockheight
     * “er”: 2800.1, //exchange rate
     * “erc”: “USD”, //exchange currency
     * “fr”: 300, //fee rate
     * “s”: fd, //size
     * “c”: 123475859 //created
     * “dId”: ”<UUID>” //DeviceId - This is a UUID that gets generated and then persisted, sent with every tx
     * “comment”: “Vodka for Mihail”
     * }
    </UUID></txHash> */

    constructor(txJSON: JSONObject): this() {
        if (txJSON.has(CLASS_VERSION)) {
            classVersion = txJSON.getInt(CLASS_VERSION)
        }
        if (txJSON.has(BLOCK_HEIGHT)) {
            blockHeight = txJSON.getInt(BLOCK_HEIGHT)
        }
        if (txJSON.has(EXCHANGE_RATE)) {
            exchangeRate = txJSON.getDouble(EXCHANGE_RATE)
        }
        if (txJSON.has(EXCHANGE_CURRENCY)) {
            exchangeCurrency = txJSON.getString(EXCHANGE_CURRENCY)
        }
        if (txJSON.has(COMMENT)) {
            comment = txJSON.getString(COMMENT)
        }
        if (txJSON.has(FEE_RATE)) {
            fee = txJSON.getString(FEE_RATE)
        }
        if (txJSON.has(TX_SIZE)) {
            txSize = txJSON.getInt(TX_SIZE)
        }
        if (txJSON.has(CREATION_TIME)) {
            creationTime = txJSON.getInt(CREATION_TIME)
        }
        if (txJSON.has(DEVICE_ID)) {
            deviceId = txJSON.getString(DEVICE_ID)
        }
    }

    fun toJSON(): JSONObject = JSONObject(mapOf(
        CLASS_VERSION to classVersion,
        BLOCK_HEIGHT to blockHeight,
        EXCHANGE_RATE to exchangeRate,
        EXCHANGE_CURRENCY to (exchangeCurrency ?: ""),
        FEE_RATE to fee,
        TX_SIZE to txSize,
        CREATION_TIME to creationTime,
        DEVICE_ID to (deviceId ?: ""),
        COMMENT to (comment ?: "")
    ))
}
