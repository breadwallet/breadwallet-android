/**
 * BreadWallet
 *
 * Created by Drew Carlson <drew.carlson@breadwallet.com> on 11/6/19.
 * Copyright (c) 2019 breadwallet LLC
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
package com.breadwallet.ui.atm

import android.annotation.SuppressLint
import android.content.Context
import android.content.ContextWrapper
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.annotation.NonNull
import androidx.appcompat.app.AppCompatActivity
import cash.just.wac.Wac
import cash.just.wac.WacSDK
import cash.just.wac.model.AtmListResponse
import cash.just.wac.model.AtmMachine
import com.bluelinelabs.conductor.RouterTransaction
import com.bluelinelabs.conductor.changehandler.FadeChangeHandler
import com.breadwallet.R
import com.breadwallet.ui.BaseController
import com.breadwallet.ui.platform.PlatformConfirmTransactionController
import com.google.android.gms.maps.CameraUpdate
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLngBounds
import com.google.android.gms.maps.model.LatLngBounds.Builder
import com.platform.PlatformTransactionBus
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.withContext
import retrofit2.Call
import retrofit2.Response

@Suppress("TooManyFunctions")
class MapController(
    args: Bundle
) : BaseController(args) {

    override val layoutId = R.layout.fragment_map

    private lateinit var map:GoogleMap

    @SuppressLint("ReturnCount")
    private fun getActivityFromContext(@NonNull context: Context): AppCompatActivity? {
        while (context is ContextWrapper) {
            if (context is AppCompatActivity) return context
            return context.baseContext as AppCompatActivity
        }
        return null //we failed miserably
    }

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreateView(view: View) {
        super.onCreateView(view)

        prepareMap(view.context)
        if (!WacSDK.isSessionCreated()) {
            WacSDK.createSession(object: Wac.SessionCallback {
                override fun onSessionCreated(sessionKey: String) {
                    fetchAtms()
                }

                override fun onError(errorMessage: String?) {
                    Toast.makeText(applicationContext, errorMessage, Toast.LENGTH_SHORT).show()
                }
            })
        } else {
            fetchAtms()
        }

        handlePlatformMessages().launchIn(viewCreatedScope)
    }

    private fun fetchAtms(){
        WacSDK.getAtmList().enqueue(object: retrofit2.Callback<AtmListResponse> {
            override fun onResponse(call: Call<AtmListResponse>, response: Response<AtmListResponse>) {
                val builder = Builder()
                response.body()!!.data.items.forEach { atm ->
                    val markerOpt = WacMarker.getMarker(applicationContext!!, atm)

                    val marker = map.addMarker(markerOpt)
                    marker.tag = atm
                    builder.include(markerOpt.position)
                    val bounds: LatLngBounds = builder.build()

                    val padding = 0 // offset from edges of the map in pixels
                    val cameraUpdate: CameraUpdate = CameraUpdateFactory.newLatLngBounds(bounds, padding)
                    map.animateCamera(cameraUpdate)
                }
            }

            override fun onFailure(call: Call<AtmListResponse>, t: Throwable) {
                Toast.makeText(applicationContext, t.message, Toast.LENGTH_SHORT).show()
            }
        })
    }
    private fun prepareMap(context : Context) {
        val fragmentManager = getActivityFromContext(context)!!.supportFragmentManager
        val fragment : SupportMapFragment = fragmentManager.findFragmentById(R.id.mapFragment) as SupportMapFragment
        fragment.getMapAsync(object: OnMapReadyCallback {

            /**
             * This is where we can add markers or lines, add listeners or move the camera. In this case,
             * we just move the camera to Sydney and add a marker in Sydney.
             */
            override fun onMapReady(googleMap: GoogleMap?) {
                googleMap ?: return
                map = googleMap

                map.uiSettings.isMapToolbarEnabled = false
                map.uiSettings.isMyLocationButtonEnabled = true
                map.uiSettings.isZoomControlsEnabled = true
                map.uiSettings.isZoomGesturesEnabled = true

                map.setOnMarkerClickListener {
                    false
                }

                map.setOnInfoWindowClickListener { atm ->
                    moveToVerification(atm.tag as AtmMachine)
                }
            }
        })
    }

    private fun moveToVerification(atm:AtmMachine) {
        val controller = RequestCashCodeController(atm)
        val transaction = RouterTransaction.with(controller)
            .popChangeHandler(FadeChangeHandler())
            .pushChangeHandler(FadeChangeHandler())

        router.pushController(transaction)
    }

    private fun handlePlatformMessages() = PlatformTransactionBus.requests().onEach {
        withContext(Dispatchers.Main) {
            val transaction = RouterTransaction.with(PlatformConfirmTransactionController(it))
            router.pushController(transaction)
        }
    }
}
