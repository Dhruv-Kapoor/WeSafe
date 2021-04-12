package com.example.wesafe.dataClasses

import androidx.annotation.DrawableRes

data class TravelTip(
    val title: String,
    @DrawableRes
    val image: Int
)