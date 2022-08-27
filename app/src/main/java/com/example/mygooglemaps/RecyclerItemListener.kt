package com.example.mygooglemaps

import com.google.android.gms.maps.model.Marker

interface RecyclerItemListener {
    fun onItemClick(marker: Marker)
    fun onItemLongClick(marker: Marker)
}