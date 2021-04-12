package com.example.wesafe.dataClasses

data class LocationUpdate(
    val latitude: Double,
    val longitude: Double,
    val time: Long
){
    constructor(): this(0.0,0.0,0)
}