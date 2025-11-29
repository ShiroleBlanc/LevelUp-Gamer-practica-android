package com.example.levelup_gamerpractica.data.local.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "products")
data class Product(
    @PrimaryKey
    val id: Long,
    val name: String,
    val price: Double,
    val category: String,
    val imageUrl: String,
    val description: String,
    val manufacturer: String,
    val distributor: String
)