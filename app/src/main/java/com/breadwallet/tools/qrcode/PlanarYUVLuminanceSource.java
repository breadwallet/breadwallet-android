package com.breadwallet.tools.qrcode;


import com.google.zxing.LuminanceSource;

/**
 * This object extends LuminanceSource around an array of YUV data returned from
 * the camera driver, with the option to crop to a rectangle within the full
 * data. This can be used to exclude superfluous pixels around the perimeter and
 * speed up decoding. It works for any pixel format where the Y channel is
 * planar and appears first, including YCbCr_420_SP and YCbCr_422_SP.
 */
final public class PlanarYUVLuminanceSource extends LuminanceSource {

    private final byte[] mYuvData;

    public PlanarYUVLuminanceSource(byte[] yuvData, int width, int height) {
        super(width, height);

        mYuvData = yuvData;
    }

    @Override
    public byte[] getRow(int y, byte[] row) {
        if (y < 0 || y >= getHeight()) {
            throw new IllegalArgumentException("Requested row is outside the image: " + y);
        }
        final int width = getWidth();
        if (row == null || row.length < width) {
            row = new byte[width];
        }
        final int offset = y * width;
        System.arraycopy(mYuvData, offset, row, 0, width);
        return row;
    }

    @Override
    public byte[] getMatrix() {
        return mYuvData;
    }

    @Override
    public boolean isCropSupported() {
        return true;
    }

}
