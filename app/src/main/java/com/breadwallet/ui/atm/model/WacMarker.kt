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

class WacMarker {
    companion object {
        private var bitmapRedemption:BitmapDescriptor? = null
        private var bitmapBuyOnly:BitmapDescriptor? = null

        fun getMarker(context:Context, atm : AtmMachine): MarkerOptions {
            val marker = LatLng(atm.latitude.toDouble(), atm.longitude.toDouble())

            val title = if (atm.redemption ==1) {
                "ATM"
            } else {
                "ATM - Buy Only"
            }

            return MarkerOptions()
                .position(marker)
                .title(title)
                .snippet(
                    getDetails(
                        atm
                    )
                )
                .icon(getDescription(context, atm.redemption == 1))
        }

        private fun getDescription(context: Context, isRedemptionEnabled: Boolean) : BitmapDescriptor {
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

        private fun getDetails(atm : AtmMachine) : String {
            return if (atm.addressDesc.contains(atm.city)) {
                atm.addressDesc
            } else {
                atm.addressDesc + " " + atm.city
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
}