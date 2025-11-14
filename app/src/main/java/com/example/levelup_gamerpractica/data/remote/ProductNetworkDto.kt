package com.example.levelup_gamerpractica.data.remote

import com.google.gson.annotations.SerializedName

// Esta clase DEBE coincidir con el JSON de tu backend
data class ProductNetworkDto(
    val id: Long,
    val name: String,
    val price: Double, // El backend envía Double, no String
    val category: String,

    // SerializedName mapea "imageUrl" (del JSON) a "image" (en Kotlin)
    @SerializedName("imageUrl")
    val image: String,

    val description: String,
    val manufacturer: String,
    val distributor: String
    // Ignoramos 'reviews' por ahora, ya que el DTO no las necesita
)