package com.example.mygooglemaps

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.google.android.gms.maps.model.Marker

class SharedViewModel : ViewModel() {

    private val markers = MutableLiveData<MutableList<Marker>>()

    fun addMarkerToList(marker: Marker) {
        if (markers.value.isNullOrEmpty()) {
            markers.value = mutableListOf(marker)
        } else {
            markers.value!!.add(marker)
        }
    }

    fun editMarker(marker: Marker) {
        markers.value = markers.value?.map { if (it.id == marker.id) {
            marker
        } else {
            it
        }
        }?.toMutableList()
    }

    fun getAllMarkers(): MutableList<Marker> = markers.value ?: mutableListOf()
}