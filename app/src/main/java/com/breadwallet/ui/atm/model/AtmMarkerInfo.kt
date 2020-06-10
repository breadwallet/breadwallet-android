package com.breadwallet.ui.atm.model

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.Drawable
import androidx.annotation.ColorInt
import androidx.core.content.ContextCompat
import cash.just.sdk.model.AtmMachine
import com.breadwallet.R
import com.google.android.gms.maps.model.BitmapDescriptor
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions

class AtmMarkerInfo(val atm: AtmMachine) {

    companion object {
        private var bitmapRedemption:BitmapDescriptor? = null
        private var bitmapBuyOnly:BitmapDescriptor? = null

        private fun getBitmapDescription(context: Context, isRedemptionEnabled: Boolean) : BitmapDescriptor {
            return if (isRedemptionEnabled) {
                if (bitmapRedemption == null) {
                    bitmapRedemption = bitmapDescriptorFromVector(
                        context,
                        context.getColor(R.color.white),
                        R.drawable.ic_icon_cs_square_black_padding
                    )
                }
                bitmapRedemption!!
            } else {
                if (bitmapBuyOnly == null) {
                    bitmapBuyOnly = bitmapDescriptorFromVector(
                        context,
                        context.getColor(R.color.gray),
                        R.drawable.ic_icon_cs_square_black_padding
                    )
                }
                bitmapBuyOnly!!
            }
        }

        private fun bitmapDescriptorFromVector(
            context: Context, @ColorInt color:Int,
            vectorResId: Int
        ): BitmapDescriptor? {
            val vectorDrawable: Drawable = ContextCompat.getDrawable(context, vectorResId)!!
            vectorDrawable.setBounds(
                0,
                0,
                vectorDrawable.intrinsicWidth,
                vectorDrawable.intrinsicHeight
            )
            val bitmap: Bitmap = Bitmap.createBitmap(
                vectorDrawable.intrinsicWidth,
                vectorDrawable.intrinsicHeight,
                Bitmap.Config.ARGB_8888
            )

            val canvas = Canvas(bitmap)
            canvas.drawColor(color)
            vectorDrawable.draw(canvas)

            return BitmapDescriptorFactory.fromBitmap(bitmap)
        }
    }

    fun getTitle(): String {
        return if (atm.redemption ==1) {
            "ATM"
        } else {
            "ATM - Buy Only"
        }
    }

    fun getPosition() : LatLng {
        return LatLng(atm.latitude.toDouble(), atm.longitude.toDouble())
    }

    fun getSnippet(): String {
        return if (atm.addressDesc.contains(atm.city)) {
            atm.addressDesc
        } else {
            atm.addressDesc + " " + atm.city
        }
    }

    fun toMarkerOptions(context:Context): MarkerOptions {
        return MarkerOptions()
            .position(getPosition())
            .title(getTitle())
            .snippet(getSnippet())
            .icon(getBitmapDescription(context, atm.redemption ==1))
    }
}