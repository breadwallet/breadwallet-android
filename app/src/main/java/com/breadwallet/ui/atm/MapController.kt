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
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.recyclerview.widget.LinearLayoutManager
import cash.just.sdk.Cash
import cash.just.sdk.CashSDK
import cash.just.sdk.model.AtmListResponse
import cash.just.sdk.model.AtmMachine
import com.bluelinelabs.conductor.RouterTransaction
import com.bluelinelabs.conductor.changehandler.FadeChangeHandler
import com.breadwallet.R
import com.breadwallet.ui.BaseController
import com.breadwallet.ui.atm.model.AtmClusterRenderer
import com.breadwallet.ui.atm.model.AtmMarkerInfo
import com.breadwallet.ui.atm.model.ClusteredAtm
import com.breadwallet.ui.atm.model.WacMarker
import com.breadwallet.ui.platform.PlatformConfirmTransactionController
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.Marker
import com.google.maps.android.clustering.ClusterManager
import com.mikepenz.fastadapter.adapters.FastItemAdapter
import com.platform.PlatformTransactionBus
import kotlinx.android.synthetic.main.fragment_map.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.withContext
import okhttp3.internal.filterList
import retrofit2.Call
import retrofit2.Response

enum class ATMViewMode {
    MAP,
    LIST
}

enum class ATMMode {
    ALL,
    REDEMPTION
}

@Suppress("TooManyFunctions")
class MapController(
    args: Bundle
) : BaseController(args) {

    override val layoutId = R.layout.fragment_map
    private var viewMode:ATMViewMode = ATMViewMode.MAP
    private var atmMode:ATMMode = ATMMode.ALL

    private var map:GoogleMap? = null
    private var atmList: List<AtmMachine> = ArrayList()

    @Suppress("MagicNumber")
    private var texas = LatLng(31.000000, -100.000000)
    @Suppress("MagicNumber")
    private var initialZoom = 5f
    private lateinit var appContext:Context
    private val enableMapClusters = false
    @Suppress("MagicNumber")
    private val clustersThreshold = 100
    private var isAllAtms = true
    private val fastAdapter = FastItemAdapter<AtmItem>()
    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreateView(view: View) {
        super.onCreateView(view)
        appContext = view.context.applicationContext
        prepareMap(view.context)
        buttonRedeemOnly.isEnabled = false
        loadingView.visibility = View.VISIBLE
        atmRecyclerList.visibility = View.GONE

        if (!CashSDK.isSessionCreated()) {
            Log.d("MapController", "Creating session")
            CashSDK.createSession(BitcoinServer.getServer(), object: Cash.SessionCallback {
                override fun onSessionCreated(sessionKey: String) {
                    fetchAtms()
                }

                override fun onError(errorMessage: String?) {
                    Toast.makeText(applicationContext, errorMessage, Toast.LENGTH_SHORT).show()
                }
            })
        } else {
            Log.d("MapController", "Session already created.")
            fetchAtms()
        }

        handlePlatformMessages().launchIn(viewCreatedScope)

        searchView.setOnQueryTextFocusChangeListener { _, hasFocus ->
            // if (hasFocus) {
            //     moveToVerification(getAtmMachineMock())
            // }
        }

        atmRecyclerList.layoutManager = LinearLayoutManager(
            router.activity,
            LinearLayoutManager.VERTICAL,
            false
        )

        atmRecyclerList.adapter = fastAdapter

        fastAdapter.onClickListener = { _, _, item, _ ->
            // Handle click here
            moveToVerification(item.atmMachine)
            false
        }

        listMapButton.setOnClickListener {
            viewMode = when(viewMode) {
                ATMViewMode.MAP -> {
                    listMapButton.setImageResource(R.drawable.ic_view_list)
                    atmRecyclerList.visibility = View.GONE
                    mapFragment.visibility = View.VISIBLE
                    ATMViewMode.LIST
                }
                ATMViewMode.LIST -> {
                    listMapButton.setImageResource(R.drawable.ic_map_marker)
                    atmRecyclerList.visibility = View.VISIBLE
                    mapFragment.visibility = View.GONE
                    ATMViewMode.MAP
                }
            }
        }

        buttonRedeemOnly.setOnClickListener {
            if (atmList.isNotEmpty() && map != null) {
                loadingView.visibility = View.VISIBLE

                buttonRedeemOnly.isEnabled = false
                if (isAllAtms) {
                    buttonRedeemOnly.text = "Show All"
                    isAllAtms = false
                    atmMode = ATMMode.REDEMPTION
                } else {
                    buttonRedeemOnly.text = "Show Redeem Only"
                    isAllAtms = true
                    atmMode = ATMMode.ALL
                }

                proceedToAddMarkers(it.context, map!!, atmList, atmMode)
                populateList(atmList, atmMode)
            }
        }
    }

    private fun populateList(list:List<AtmMachine>, mode:ATMMode){
        fastAdapter.clear()
        list.forEach {
            if (mode == ATMMode.REDEMPTION) {
                if (it.redemption == 1) {
                    fastAdapter.add(AtmItem(it))
                }
            } else {
                fastAdapter.add(AtmItem(it))
            }
        }
    }
    private fun fetchAtms() {
        CashSDK.getAtmList().enqueue(object: retrofit2.Callback<AtmListResponse> {
            override fun onResponse(call: Call<AtmListResponse>, response: Response<AtmListResponse>) {
                response.body()?.let { safeResponse ->
                    atmList = safeResponse.data.items
                    map?.let { mapObject ->
                        proceedToAddMarkers(appContext, mapObject, atmList, atmMode)
                    } ?: run {
                        Log.e("MapController", "Map not ready to load markers")
                    }

                    populateList(atmList, atmMode)
                }
            }

            override fun onFailure(call: Call<AtmListResponse>, t: Throwable) {
                Toast.makeText(applicationContext, t.message, Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun addAtmMarkers(map:GoogleMap, list:List<AtmMachine>) {
        map.clear()
        list.forEach { atm ->
            val markerOpt = WacMarker.getMarker(applicationContext!!, atm)
            val marker = map.addMarker(markerOpt)
            marker.tag = atm
        }

        map.moveCamera(CameraUpdateFactory.newLatLng(texas))
        map.animateCamera(CameraUpdateFactory.zoomTo(initialZoom))
    }

    private fun prepareMap(context : Context) {
        AtmMapHelper.addMapFragment(context, R.id.mapFragment, "ATMS_MAP")
            .getMapAsync { googleMap ->
                googleMap?.let {
                    Log.d("MapController", "Map prepared")
                    map = it

                    it.uiSettings.isMapToolbarEnabled = false
                    it.uiSettings.isMyLocationButtonEnabled = true
                    it.uiSettings.isZoomControlsEnabled = true
                    it.uiSettings.isZoomGesturesEnabled = true

                    it.setOnMarkerClickListener {
                        false
                    }

                    it.setOnInfoWindowClickListener { info ->
                       processInfoWindowClicked(context, info)
                    }
                    Log.d("MapController", "atm list size {$atmList.size}")
                    proceedToAddMarkers(context, it, atmList, atmMode)
                }
            }
    }

    private fun processInfoWindowClicked(context:Context, marker: Marker) {
        val atm = marker.tag as AtmMachine
        if (atm.redemption == 1) {
            moveToVerification(atm)
        } else {
            Toast.makeText(context, "This ATM does support only to buy," +
                " redemption is still not supported", Toast.LENGTH_SHORT).show()
        }
    }

    private fun proceedToAddMarkers(context:Context, map:GoogleMap, atmList:List<AtmMachine>, atmMode:ATMMode) {
        var filteredList = atmList
        if (atmMode == ATMMode.REDEMPTION) {
            filteredList = atmList.filterList {
                this.redemption == 1
            }
        }

        if (enableMapClusters && filteredList.size > clustersThreshold) {
            addAtmMarkersWithCluster(context, map, filteredList)
        } else if (filteredList.isNotEmpty()) {
            addAtmMarkers(map, filteredList)
        }

        buttonRedeemOnly.isEnabled = true
        loadingView.visibility = View.GONE
    }

    private fun addAtmMarkersWithCluster(context: Context, map:GoogleMap, list:List<AtmMachine>){
        val clusterManager = ClusterManager<ClusteredAtm>(context, map)
        map.setOnCameraIdleListener(clusterManager)
        map.setOnMarkerClickListener(clusterManager)
        clusterManager.setOnClusterItemInfoWindowClickListener {
            //TODO the click event is not implemented
        }

        clusterManager.renderer = AtmClusterRenderer(context, map, clusterManager)
        list.forEach {
            val atm = ClusteredAtm(AtmMarkerInfo(it))
            clusterManager.addItem(atm)
        }
    }

    private fun moveToVerification(atm:AtmMachine) {
        val controller = RequestCashCodeController(atm)
        val transaction = RouterTransaction.with(controller)
            .popChangeHandler(FadeChangeHandler())
            .pushChangeHandler(FadeChangeHandler())

        router.replaceTopController(transaction)
    }

    private fun handlePlatformMessages() = PlatformTransactionBus.requests().onEach {
        withContext(Dispatchers.Main) {
            val transaction = RouterTransaction.with(PlatformConfirmTransactionController(it))
            router.pushController(transaction)
        }
    }
}
