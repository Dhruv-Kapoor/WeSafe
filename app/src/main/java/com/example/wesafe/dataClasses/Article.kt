package com.example.wesafe.dataClasses

import android.os.Parcelable
import androidx.annotation.DrawableRes
import kotlinx.android.parcel.Parcelize

@Parcelize
data class Article (
    val title: String,
    @DrawableRes
    val image: Int,
    val link: String
): Parcelable