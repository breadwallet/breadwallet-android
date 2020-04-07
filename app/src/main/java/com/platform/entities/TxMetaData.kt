/**
 * BreadWallet
 *
 * Created by Mihail Gutan <mihail@breadwallet.com> on 6/22/17.
 * Copyright (c) 2017 breadwallet LLC
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
package com.platform.entities

import com.platform.util.getDoubleOrDefault
import com.platform.util.getDoubleOrDefaultSafe
import com.platform.util.getIntOrDefault
import com.platform.util.getStringOrNull
import org.json.JSONObject

sealed class TxMetaData

@Suppress("ComplexMethod")
data class TxMetaDataValue(
    val deviceId: String? = null,
    val comment: String? = null,
    val exchangeCurrency: String? = null,
    val exchangeRate: Double = 0.toDouble(),
    val blockHeight: Int = 0,
    val fee: Double = 0.toDouble(),
    val txSize: Int = 0,
    val creationTime: Int = 0,
    val classVersion: Int = 3
) : TxMetaData() {
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

        fun fromJsonObject(json: JSONObject): TxMetaData = json.run {
            TxMetaDataValue(
                deviceId = getStringOrNull(DEVICE_ID),
                comment = getStringOrNull(COMMENT),
                exchangeCurrency = getStringOrNull(EXCHANGE_CURRENCY),
                classVersion = getIntOrDefault(CLASS_VERSION),
                blockHeight = getIntOrDefault(BLOCK_HEIGHT),
                exchangeRate = getDoubleOrDefault(EXCHANGE_RATE),
                fee = getDoubleOrDefaultSafe(FEE_RATE),
                txSize = getIntOrDefault(TX_SIZE),
                creationTime = getIntOrDefault(CREATION_TIME)
            )
        }
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

    fun toJSON(): JSONObject = JSONObject(
        mapOf(
            CLASS_VERSION to classVersion,
            BLOCK_HEIGHT to blockHeight,
            EXCHANGE_RATE to exchangeRate,
            EXCHANGE_CURRENCY to (exchangeCurrency ?: ""),
            FEE_RATE to fee,
            TX_SIZE to txSize,
            CREATION_TIME to creationTime,
            DEVICE_ID to (deviceId ?: ""),
            COMMENT to (comment ?: "")
        )
    )
}

object TxMetaDataEmpty : TxMetaData()

