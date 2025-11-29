package com.example.levelup_gamerpractica.data.local.entities

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.google.gson.annotations.SerializedName // Opcional, pero recomendado si usas Gson

@Entity(tableName = "products")
data class Product(
    @PrimaryKey
    val id: Long,           // CAMBIO 1: Int -> Long (Backend usa Long)

    val name: String,

    val price: Double,      // CAMBIO 2: String -> Double (Backend envía un número)

    val category: String,



    val imageUrl: String,

    val description: String,

    val manufacturer: String,

    val distributor: String
)