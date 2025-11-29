package com.example.levelup_gamerpractica.data.remote

import com.google.gson.annotations.SerializedName

data class ProductNetworkDto(
    val id: Long,
    val name: String,
    val price: Double,
    val category: String,

    @SerializedName("imageUrl")
    val imageUrl: String,

    val description: String,
    val manufacturer: String,
    val distributor: String
)