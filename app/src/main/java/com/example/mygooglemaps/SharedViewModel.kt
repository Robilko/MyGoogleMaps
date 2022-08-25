package com.example.mygooglemaps

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.google.android.gms.maps.model.Marker

class SharedViewModel : ViewModel() {

    val markers = MutableLiveData<MutableList<Marker>>()

    fun addMarkerToList(marker: Marker) {
        if (markers.value.isNullOrEmpty()) {
            markers.value = mutableListOf(marker)
        } else {
            markers.value!!.add(marker)
        }
    }

    fun getAllMarkers(): MutableList<Marker> = markers.value ?: mutableListOf()
}