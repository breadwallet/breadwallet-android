package com.breadwallet.ui.atm.model

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.drawable.Drawable
import androidx.core.content.ContextCompat
import cash.just.wac.model.AtmMachine
import com.breadwallet.R
import com.google.android.gms.maps.model.BitmapDescriptor
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions

class WacMarker {
    companion object {
        fun getMarker(context:Context, atm : AtmMachine): MarkerOptions {
            val marker = LatLng(atm.latitude.toDouble(), atm.longitude.toDouble())
            val bitmapDesc =
                bitmapDescriptorFromVector(
                    context,
                    R.drawable.ic_icon_cs_square_padding
                )

            return MarkerOptions()
                .position(marker)
                .title("ATM")
                .snippet(
                    getDetails(
                        atm
                    )
                )
                .icon(bitmapDesc)
        }

        private fun getDetails(atm : AtmMachine) : String {
            return if (atm.addressDesc.contains(atm.city)) {
                atm.addressDesc
            } else {
                atm.addressDesc + " " + atm.city
            }
        }

        private fun bitmapDescriptorFromVector(
            context: Context,
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
            canvas.drawColor(Color.BLACK)
            vectorDrawable.draw(canvas)

            return BitmapDescriptorFactory.fromBitmap(bitmap)
        }
    }
}