package com.breadwallet.ui.atm.model

import android.content.Context
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.model.MarkerOptions
import com.google.maps.android.clustering.ClusterManager
import com.google.maps.android.clustering.view.DefaultClusterRenderer

class AtmClusterRenderer(val
    context: Context,
    map: GoogleMap,
    clusterManager: ClusterManager<ClusteredAtm>
) : DefaultClusterRenderer<ClusteredAtm>(context, map, clusterManager) {

    override fun onBeforeClusterItemRendered(item: ClusteredAtm, markerOptions: MarkerOptions) {
        super.onBeforeClusterItemRendered(item, markerOptions)
        val markerInfo = item.getMarkerInfo().toMarkerOptions(context)
        markerOptions.icon(markerInfo.icon)
        markerOptions.snippet(markerInfo.snippet)
        markerOptions.title(markerInfo.title)
    }
}