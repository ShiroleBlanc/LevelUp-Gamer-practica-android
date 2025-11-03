package com.example.levelup_gamerpractica.data.local.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "products")
data class Product(
    @PrimaryKey val id: Int,
    val name: String,
    val price: String,
    val category: String,
    val image: String,
    val description: String,
    val manufacturer: String,
    val distributor: String
)