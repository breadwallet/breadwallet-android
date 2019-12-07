package com.platform.entities

import com.breadwallet.crypto.WalletManagerMode
import com.platform.util.getIntOrDefault
import com.platform.util.getStringOrNull
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
    val classVersion: Int = DEFAULT_CLASS_VERSION,
    val creationDate: Int = DEFAULT_CREATION_DATE,
    val name: String? = null,
    val connectionModes: Map<String, WalletManagerMode> = emptyMap()
) {
    companion object {
        private const val NAME = "name"
        private const val CLASS_VERSION = "classVersion"
        private const val CREATION_DATE = "creationDate"
        private const val CONNECTION_MODES = "connectionModes"

        private const val DEFAULT_CLASS_VERSION = 3
        private const val DEFAULT_CREATION_DATE = 0

        fun fromJsonObject(json: JSONObject): WalletInfoData = json.run {
            val mutableModes = mutableMapOf<String, WalletManagerMode>()
            if (has(CONNECTION_MODES)) {
                val modesObj = getJSONObject(CONNECTION_MODES)
                modesObj.keys().forEach {
                    mutableModes[it] = WalletManagerMode.fromSerialization(modesObj.getInt(it))
                }
            }
            WalletInfoData(
                classVersion = getIntOrDefault(CLASS_VERSION, DEFAULT_CLASS_VERSION),
                creationDate = getIntOrDefault(CREATION_DATE, DEFAULT_CREATION_DATE),
                name = getStringOrNull(NAME),
                connectionModes = mutableModes.toMap()
            )
        }
    }

    fun toJSON(): JSONObject =
        JSONObject(
            mapOf(
                CLASS_VERSION to classVersion,
                CREATION_DATE to creationDate,
                NAME to name,
                CONNECTION_MODES to connectionModes.mapValues { it.value.toSerialization() }
            )
        )
}