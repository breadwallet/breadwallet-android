/**
 * BreadWallet
 * <p/>
 * Created by Mihail Gutan on <mihail@breadwallet.com> 11/13/16.
 * Copyright (c) 2016 breadwallet LLC
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
package com.platform.interfaces;

import com.platform.kvstore.CompletionObject;

/**
 * Interface for the available operations with server's KV store.
 */
public interface KVStoreAdaptor {

    /**
     * Return the most recent version of the key in the server.
     *
     * @param key
     * @return
     */
    CompletionObject ver(String key);

    /**
     * Save a value under a key. If it's a new key, then pass 0 as the If-None-Match header, otherwise you must
     * pass the current version in the database (which may be retrieved with KVStoreAdaptor.get).
     *
     * @param key     Existing key or 0 if it's a new key.
     * @param value   Value to be stored.
     * @param version Current version in the database.
     * @return
     */
    CompletionObject put(String key, byte[] value, long version);

    /**
     * Mark a key as deleted in the KV store.
     *
     * @param key     The key to save the data under
     * @param version Version of the key.
     * @return
     */
    CompletionObject del(String key, long version);

    /**
     * Retrieve a value from a key. If the given version is older than the server's version conflict will be returned.
     *
     * @param key     The key to retrieve from the KV store
     * @param version Current version in the database.
     * @return
     */
    CompletionObject get(String key, long version);

    /**
     * Retrieve all the keys from the server belonging to the current user.
     *
     * @return
     */
    CompletionObject keys();

}
