package com.example.levelup_gamerpractica.data.local.dao

import androidx.room.*
import com.example.levelup_gamerpractica.data.local.entities.CartItem
import com.example.levelup_gamerpractica.data.local.entities.Product // Asegúrate de importar Product
import kotlinx.coroutines.flow.Flow

@Dao
interface CartDao {

    // --- CAMBIO PRINCIPAL ---
    // 1. Usamos @Transaction porque Room hará dos consultas internas (una para el carrito, otra para buscar los productos)
    // 2. Seleccionamos SOLO la tabla del carrito. Room llenará los datos del producto automáticamente gracias a @Relation
    @Transaction
    @Query("SELECT * FROM cart_items")
    fun getCartItemsWithDetails(): Flow<List<CartItemWithDetails>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertCartItem(item: CartItem)

    @Query("UPDATE cart_items SET quantity = :quantity WHERE productId = :productId")
    suspend fun updateQuantity(productId: Long, quantity: Int)

    @Query("DELETE FROM cart_items WHERE productId = :productId")
    suspend fun deleteCartItem(productId: Long)

    @Query("DELETE FROM cart_items")
    suspend fun clearCart()

    @Query("SELECT * FROM cart_items WHERE productId = :productId LIMIT 1")
    suspend fun getCartItem(productId: Long): CartItem?
}

// --- CLASE DE RELACIÓN CORREGIDA ---
// Esta clase le dice a Room: "Toma el CartItem, mira su ID, y búscame el Product completo que coincida"
data class CartItemWithDetails(
    @Embedded
    val cartItem: CartItem,

    @Relation(
        parentColumn = "productId", // El ID en la tabla CartItem
        entityColumn = "id"         // El ID en la tabla Product
    )
    val product: Product // <--- ¡AQUÍ ESTÁ LA MAGIA! Esto soluciona el error 'unresolved reference product'
)