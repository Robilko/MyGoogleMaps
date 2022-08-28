package com.example.mygooglemaps.view.markers

import com.google.android.gms.maps.model.Marker

interface RecyclerItemListener {
    fun onItemClick(marker: Marker)
    fun onItemLongClick(marker: Marker)
}