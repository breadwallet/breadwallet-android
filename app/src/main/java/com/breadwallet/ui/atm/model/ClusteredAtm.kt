package com.breadwallet.ui.atm.model

import com.breadwallet.ui.atm.utils.getFullAddress
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.clustering.ClusterItem

data class ClusteredAtm(val atmMarkerInfo: AtmMarkerInfo) : ClusterItem {

    override fun getPosition(): LatLng {
        return atmMarkerInfo.getPosition()
    }

    override fun getTitle(): String? {
        return atmMarkerInfo.getTitle()
    }

    override fun getSnippet(): String? {
        return atmMarkerInfo.atm.getFullAddress()
    }

    fun getMarkerInfo(): AtmMarkerInfo {
        return atmMarkerInfo
    }
}

