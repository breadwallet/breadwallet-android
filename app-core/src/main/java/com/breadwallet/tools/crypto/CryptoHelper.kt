/**
 * BreadWallet
 *
 * Created by Mihail Gutan <mihail@breadwallet.com> on 11/28/16.
 * Copyright (c) 2016 breadwallet LLC
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
package com.breadwallet.tools.crypto

import android.text.format.DateUtils
import com.breadwallet.crypto.Coder
import com.breadwallet.crypto.Hasher
import com.breadwallet.crypto.Key
import com.breadwallet.crypto.Signer

import java.nio.ByteBuffer
import java.nio.ByteOrder

@Suppress("TooManyFunctions")
object CryptoHelper {

    private const val NONCE_SIZE = 12

    private val base58: Coder by lazy {
        Coder.createForAlgorithm(Coder.Algorithm.BASE58)
    }

    private val sha256: Hasher by lazy {
        Hasher.createForAlgorithm(Hasher.Algorithm.SHA256)
    }

    private val sha256_2: Hasher by lazy {
        Hasher.createForAlgorithm(Hasher.Algorithm.SHA256_2)
    }

    private val md5: Hasher by lazy {
        Hasher.createForAlgorithm(Hasher.Algorithm.MD5)
    }

    private val keccak256: Hasher by lazy {
        Hasher.createForAlgorithm(Hasher.Algorithm.KECCAK256)
    }

    private val compact: Signer by lazy {
        Signer.createForAlgorithm(Signer.Algorithm.COMPACT)
    }

    private val jose: Signer by lazy {
        Signer.createForAlgorithm(Signer.Algorithm.BASIC_JOSE)
    }

    private val basicDer: Signer by lazy {
        Signer.createForAlgorithm(Signer.Algorithm.BASIC_DER)
    }

    private val hex: Coder by lazy {
        Coder.createForAlgorithm(Coder.Algorithm.HEX)
    }

    @JvmStatic
    fun hexEncode(data: ByteArray): String {
        return hex.encode(data).or("")
    }

    @JvmStatic
    fun hexDecode(data: String): ByteArray? {
        return hex.decode(data).orNull()
    }

    fun signCompact(data: ByteArray, key: Key): ByteArray {
        return compact.sign(data, key).or(byteArrayOf())
    }

    fun signJose(data: ByteArray, key: Key): ByteArray {
        return jose.sign(data, key).or(byteArrayOf())
    }

    fun signBasicDer(data: ByteArray, key: Key): ByteArray {
        return basicDer.sign(data, key).or(byteArrayOf())
    }

    fun base58Encode(data: ByteArray): String {
        return base58.encode(data).or("")
    }

    fun base58Decode(data: String): ByteArray {
        return base58.decode(data).or(byteArrayOf())
    }

    @JvmStatic
    fun base58ofSha256(toEncode: ByteArray): String {
        val sha256First = sha256(toEncode)
        return base58.encode(sha256First).or("")
    }

    @JvmStatic
    fun doubleSha256(data: ByteArray): ByteArray? {
        return sha256_2.hash(data).orNull()
    }

    @JvmStatic
    fun sha256(data: ByteArray?): ByteArray? {
        return sha256.hash(data).orNull()
    }

    @JvmStatic
    fun md5(data: ByteArray): ByteArray? {
        return md5.hash(data).orNull()
    }

    fun keccak256(data: ByteArray): ByteArray? {
        return keccak256.hash(data).orNull()
    }

    /**
     * generate a nonce using microseconds-since-epoch
     */
    @JvmStatic
    @Suppress("MagicNumber")
    fun generateRandomNonce(): ByteArray {
        val nonce = ByteArray(NONCE_SIZE)
        val buffer = ByteBuffer.allocate(8)
        val t = System.nanoTime() / DateUtils.SECOND_IN_MILLIS
        buffer.order(ByteOrder.LITTLE_ENDIAN)
        buffer.putLong(t)
        val byteTime = buffer.array()
        System.arraycopy(byteTime, 0, nonce, 4, byteTime.size)
        return nonce
    }
}
