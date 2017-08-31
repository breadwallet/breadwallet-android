package com.breadwallet.tools.util;

import com.google.firebase.crash.FirebaseCrash;

import org.apache.commons.compress.compressors.bzip2.BZip2CompressorInputStream;
import org.apache.commons.compress.compressors.bzip2.BZip2CompressorOutputStream;
import org.apache.commons.compress.utils.IOUtils;
import org.apache.commons.io.output.ByteArrayOutputStream;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

    /**
 * BreadWallet
 * <p/>
 * Created by Mihail Gutan on <mihail@breadwallet.com> 12/11/16.
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
public class BRCompressor {
    public static final String TAG = BRCompressor.class.getName();

    public static byte[] gZipExtract(byte[] compressed) {
        if (compressed == null || compressed.length == 0) return null;
        try {
            InputStream isr = new GZIPInputStream(new ByteArrayInputStream(compressed));
            return IOUtils.toByteArray(isr);
        } catch (IOException e) {
            FirebaseCrash.report(e);
            e.printStackTrace();
        }
        return null;
    }

    public static byte[] gZipCompress(byte[] data) {
        if (data == null) return null;
        byte[] compressedData = null;
        try {
            ByteArrayOutputStream byteStream =
                    new ByteArrayOutputStream(data.length);
            try {
                GZIPOutputStream zipStream =
                        new GZIPOutputStream(byteStream);
                try {
                    zipStream.write(data);
                } finally {
                    try {
                        zipStream.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            } finally {
                try {
                    byteStream.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            compressedData = byteStream.toByteArray();
        } catch (Exception e) {
            FirebaseCrash.report(e);
            e.printStackTrace();
        }
        return compressedData;
    }

    public static byte[] bz2Extract(byte[] compressed) {
        if (compressed == null || compressed.length == 0) return null;

        ByteArrayInputStream is = new ByteArrayInputStream(compressed);
        InputStream bin = null;
        try {
            bin = new BZip2CompressorInputStream(is);
            return IOUtils.toByteArray(bin);

        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                if (bin != null) {
                    bin.close();
                }
                is.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return null;
    }

    public static byte[] bz2Compress(byte[] data) {
        if (data == null) return null;
        byte[] compressedData = null;
        try {
            ByteArrayOutputStream byteStream =
                    new ByteArrayOutputStream(data.length);
            try {
                BZip2CompressorOutputStream bout =
                        new BZip2CompressorOutputStream(byteStream);
                try {
                    bout.write(data);
                } finally {
                    try {
                        bout.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            } finally {
                try {
                    byteStream.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            compressedData = byteStream.toByteArray();
        } catch (Exception e) {
            e.printStackTrace();
            FirebaseCrash.report(e);
        }
        return compressedData;

    }

    public static boolean isGZIPStream(byte[] bytes) {
        return bytes[0] == (byte) GZIPInputStream.GZIP_MAGIC
                && bytes[1] == (byte) (GZIPInputStream.GZIP_MAGIC >>> 8);
    }
}
