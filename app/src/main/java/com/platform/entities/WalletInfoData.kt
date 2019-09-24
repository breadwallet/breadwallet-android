package com.platform.entities

import org.json.JSONObject


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
data class WalletInfoData(
    var classVersion: Int = 0,
    var creationDate: Int = 0,
    var name: String? = null
) {
    companion object {
        private const val NAME = "name"
        private const val CLASS_VERSION = "classVersion"
        private const val CREATION_DATE = "creationDate"
    }

    /**
     * WalletInfoData:
     *
     *
     * Key: “wallet-info”
     *
     *
     * {
     * “classVersion”: 2, //used for versioning the schema
     * “creationDate”: 123475859, //Unix timestamp
     * “name”: “My Bread”,
     * “currentCurrency”: “USD”
     * }
     */

    constructor(json: JSONObject): this() {
        if (json.has(CLASS_VERSION)) {
            classVersion = json.getInt(CLASS_VERSION)
        }
        if (json.has(CREATION_DATE)) {
            creationDate = json.getInt(CREATION_DATE)
        }
        if (json.has(NAME)) {
            name = json.getString(NAME)
        }
    }

    fun toJSON(): JSONObject =
        JSONObject(mapOf(CLASS_VERSION to classVersion, CREATION_DATE to creationDate, NAME to name))
}
