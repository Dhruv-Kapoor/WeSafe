package com.example.wesafe.dataClasses

data class Driver(
    val name: String,
    val phone: String,
    val picUrl: String?,
    val rating: Int,
    val ratingCount: Int,
    val vehicleModel: String,
    val vehicleNo: String
){
    constructor(): this("","",null,0,0,"","")
}