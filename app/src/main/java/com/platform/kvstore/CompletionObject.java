package com.platform.kvstore;

import com.platform.sqlite.KVItem;

import java.util.List;

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
public class CompletionObject {
    public static final String TAG = CompletionObject.class.getName();

    public enum RemoteKVStoreError {
        notFound,
        conflict,
        tombstone,
        unknown
    }

    public KVItem kv;
    public long version;
    public long time;
    public RemoteKVStoreError err;
    public byte[] value;
    public List<KVItem> kvs;
    public String key;

    public CompletionObject(long version, long time, RemoteKVStoreError err) {
        this.version = version;
        this.time = time;
        this.err = err;
    }

    public CompletionObject(String key, long version, long time, RemoteKVStoreError err) {
        this.key = key;
        this.version = version;
        this.time = time;
        this.err = err;
    }

    public CompletionObject(long version, long time, byte[] value, RemoteKVStoreError err) {
        this.value = value;
        this.version = version;
        this.time = time;
        this.err = err;
    }

    public CompletionObject(List<KVItem> keys, RemoteKVStoreError err) {
        this.kvs = keys;
        this.err = err;
    }
    public CompletionObject(List<KVItem> keys) {
        this.kvs = keys;
    }

    public CompletionObject(KVItem key, RemoteKVStoreError err) {
        this.kv = key;
        this.err = err;
    }

    public CompletionObject(RemoteKVStoreError err) {
        this.err = err;
    }

}
