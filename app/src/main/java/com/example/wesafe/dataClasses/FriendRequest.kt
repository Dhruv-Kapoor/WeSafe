package com.example.wesafe.dataClasses

data class FriendRequest(
    val id: String,
    val name: String
) {
    constructor(): this("","")
}