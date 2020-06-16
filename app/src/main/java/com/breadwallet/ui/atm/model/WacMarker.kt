package com.breadwallet.ui.atm.model

import android.content.Context
import cash.just.sdk.model.AtmMachine
import com.google.android.gms.maps.model.MarkerOptions

class WacMarker {
    companion object {
        fun getMarker(context:Context, atm : AtmMachine): MarkerOptions {
            return AtmMarkerInfo(atm).toMarkerOptions(context)
        }
    }
}