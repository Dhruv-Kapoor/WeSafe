package com.example.wesafe

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.google.android.gms.maps.model.LatLng

class TravelActivityViewModel: ViewModel() {
    private val _pathCoordinates = ArrayList<LatLng>()
    private val pathCoordinates = MutableLiveData<ArrayList<LatLng>>()

    fun getPathCoordinated() = pathCoordinates as LiveData<ArrayList<LatLng>>

    fun addPathCoordinate(latLng: LatLng){
        _pathCoordinates.add(latLng)
        pathCoordinates.postValue(_pathCoordinates)
    }
}
