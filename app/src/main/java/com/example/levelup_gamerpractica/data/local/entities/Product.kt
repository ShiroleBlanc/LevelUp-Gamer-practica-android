package com.example.levelup_gamerpractica.data.local.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "products")
data class Product(
    @PrimaryKey val id: Int, // Usar el ID del producto web
    val name: String,
    val price: String, // Mantener como String por formato CLP
    val category: String,
    val image: String, // Guardar URL o identificador de recurso local
    val description: String,
    val manufacturer: String,
    val distributor: String
    // No guardamos las reviews aquí directamente para normalizar
)