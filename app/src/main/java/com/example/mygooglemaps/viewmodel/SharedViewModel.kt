package com.example.mygooglemaps.viewmodel

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.google.android.gms.maps.model.Marker

class SharedViewModel : ViewModel() {

    private var _markersLiveData = MutableLiveData<List<Marker>>(listOf())
    val markersLiveData = _markersLiveData

    fun addMarkerToList(marker: Marker) {
        _markersLiveData.value?.toMutableList()?.let { newList ->
            newList.add(marker)
            _markersLiveData.value = newList
        }
    }

    fun editMarker(marker: Marker) {
        _markersLiveData.value?.map {
            if (it.id == marker.id) {
                marker
            } else {
                it
            }
        }?.toMutableList()?.let { newList ->
            _markersLiveData.value = newList
        }
    }

    fun deleteMarker(marker: Marker) {
        _markersLiveData.value?.toMutableList()?.let { newList ->
            newList.remove(marker)
            _markersLiveData.value = newList
        }
    }

    fun getAllMarkers(): List<Marker> = markersLiveData.value ?: listOf()

    fun getSize(): Int = markersLiveData.value?.size ?: 0
}