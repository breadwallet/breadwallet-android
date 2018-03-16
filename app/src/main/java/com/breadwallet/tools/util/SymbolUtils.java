package com.breadwallet.tools.util;

import android.graphics.Paint;

/**
 * Created by byfieldj on 3/16/18.
 * <p>
 * This class checks if the device currently supports a given unicode symbol
 */

public class SymbolUtils {





    public SymbolUtils() {

    }


    public boolean doesDeviceSupportSymbol(String unicodeString) {


        Paint paint = new Paint();

        if (paint.hasGlyph(unicodeString)) {

            return true;
        }


        return false;
    }
}
