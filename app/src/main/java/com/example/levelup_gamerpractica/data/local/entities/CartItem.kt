package com.example.levelup_gamerpractica.data.local.entities

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey
import com.example.levelup_gamerpractica.data.local.entities.Product


@Entity(
    tableName = "cart_items",
    foreignKeys = [ForeignKey(
        entity = Product::class,
        parentColumns = ["id"],
        childColumns = ["productId"],
        onDelete = ForeignKey.CASCADE // Si se borra un producto, se borra del carrito
    )]
)
data class CartItem(
    @PrimaryKey val productId: Int,
    val quantity: Int
)