package com.example.wesafe.dataClasses

data class DangerZone (
    val id: String,
    var safeCount: Int,
    var unsafeCount: Int,
    val latitude: Double,
    val longitude: Double,
    val radius: Int
){
    constructor(): this("",0,0,0.0,0.0,0)
}