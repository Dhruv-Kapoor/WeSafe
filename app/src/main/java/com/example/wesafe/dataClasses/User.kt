package com.example.wesafe.dataClasses

class User (
    val id: String,
    val name: String,
    val picUrl: String?,
    val email: String,
    var friends: ArrayList<String>?,
    val phone: String
){
    constructor(): this("","",null,"", null, "")
}