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
import android.os.Bundle
import android.view.View
import android.widget.Toast
import cash.just.wac.Wac
import cash.just.wac.WacSDK
import cash.just.wac.model.AtmListResponse
import cash.just.wac.model.AtmMachine
import com.bluelinelabs.conductor.RouterTransaction
import com.bluelinelabs.conductor.changehandler.FadeChangeHandler
import com.breadwallet.R
import com.breadwallet.legacy.presenter.entities.CryptoRequest
import com.breadwallet.legacy.wallet.wallets.bitcoin.WalletBitcoinManager
import com.breadwallet.tools.animation.BRDialog
import com.breadwallet.ui.BaseController
import com.breadwallet.ui.platform.PlatformConfirmTransactionController
import com.breadwallet.ui.send.SendSheetController
import com.google.android.gms.maps.CameraUpdate
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.model.LatLngBounds
import com.google.android.gms.maps.model.LatLngBounds.Builder
import com.platform.PlatformTransactionBus
import kotlinx.android.synthetic.main.fragment_map.*
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

    private var map:GoogleMap? = null
    private var atmList: List<AtmMachine> = ArrayList()

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreateView(view: View) {
        super.onCreateView(view)

        prepareMap(view.context)

        if (!WacSDK.isSessionCreated()) {

            WacSDK.createSession(BitcoinServer.getServer(), object: Wac.SessionCallback {
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

        // searchView.setOnQueryTextFocusChangeListener { _, hasFocus ->
        //     if (hasFocus) {
        //        showDialog()
        //     }
        // }
    }

    // private fun showDialog() {
    //
    //     BRDialog.showCustomDialog(
    //         applicationContext!!, "Withdrawal requested",
    //         "Please send the amount of 0.069 BTC to the ATM",
    //         "Send", "Details", { dialog ->
    //             val builder = CryptoRequest.Builder()
    //             builder.address = "n4VQ5YdHf7hLQ2gWQYYrcxoE5B7nWuDFNF "
    //             builder.amount = 0.234f.toBigDecimal()
    //             builder.currencyCode = WalletBitcoinManager.BITCOIN_CURRENCY_CODE
    //             val request = builder.build()
    //             router.pushController(RouterTransaction.with(
    //                 SendSheetController(
    //                     request //make it default
    //                 )
    //             ))
    //             dialog.dismissWithAnimation()
    //         },
    //         { dialog ->
    //             // router.pushController(RouterTransaction.with(
    //             //     CashOutStatusController(
    //             //          //make it default
    //             //          getAwaitingState()
    //             //     )
    //             // ))
    //             dialog.dismissWithAnimation()
    //             dialog.dismissWithAnimation()
    //         }, null)
    // }

    // private fun getAwaitingState() : CashStatus {
    //     return CashStatus("BTC","A","n4VQ5YdHf7hLQ2gWQYYrcxoE5B7nWuDFNF","20.0","0.2345","",
    //         "","","Mataró, Cami de la Geganta","","")
    // }
    //
    // private fun getFundedState() : CashStatus {
    //     return CashStatus("1234-1234","V","n4VQ5YdHf7hLQ2gWQYYrcxoE5B7nWuDFNF","10","0.2345","",
    //         "","","Mataró, Cami de la Geganta","","")
    // }

    private fun fetchAtms(){
        WacSDK.getAtmList().enqueue(object: retrofit2.Callback<AtmListResponse> {
            override fun onResponse(call: Call<AtmListResponse>, response: Response<AtmListResponse>) {
                map?.let {
                    response.body()?.let { response ->
                        atmList = response.data.items
                        addAtmMarkers(it, atmList)
                    }
                }
            }

            override fun onFailure(call: Call<AtmListResponse>, t: Throwable) {
                Toast.makeText(applicationContext, t.message, Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun addAtmMarkers(map:GoogleMap, list:List<AtmMachine>) {
        val builder = Builder()
        list.forEach { atm ->
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

    private fun prepareMap(context : Context) {
        AtmMapHelper.addMapFragment(context, R.id.mapFragment, "ATMS_MAP")
            .getMapAsync { googleMap ->
                googleMap?.let {
                    map = it

                    it.uiSettings.isMapToolbarEnabled = false
                    it.uiSettings.isMyLocationButtonEnabled = true
                    it.uiSettings.isZoomControlsEnabled = true
                    it.uiSettings.isZoomGesturesEnabled = true

                    it.setOnMarkerClickListener {
                        false
                    }

                    it.setOnInfoWindowClickListener { atm ->
                        moveToVerification(atm.tag as AtmMachine)
                    }

                    if (atmList.isNotEmpty()) {
                       addAtmMarkers(it, atmList)
                    }
                }
            }
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
