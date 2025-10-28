package com.example.levelup_gamerpractica.data.local.dao

import androidx.room.*
import com.example.levelup_gamerpractica.data.local.entities.CartItem
import kotlinx.coroutines.flow.Flow // Importa Flow para datos reactivos

@Dao
interface CartDao {
    // Observa todos los items del carrito (Flow se actualiza automáticamente)
    @Query("SELECT ci.productId, ci.quantity, p.name, p.price, p.image FROM cart_items ci JOIN products p ON ci.productId = p.id")
    fun getCartItemsWithDetails(): Flow<List<CartItemWithDetails>>

    // Inserta o actualiza un item (si ya existe, reemplaza)
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertCartItem(item: CartItem)

    // Actualiza la cantidad (más eficiente que upsert si solo cambia cantidad)
    @Query("UPDATE cart_items SET quantity = :quantity WHERE productId = :productId")
    suspend fun updateQuantity(productId: Int, quantity: Int)

    // Elimina un item
    @Query("DELETE FROM cart_items WHERE productId = :productId")
    suspend fun deleteCartItem(productId: Int)

    // Vacía el carrito
    @Query("DELETE FROM cart_items")
    suspend fun clearCart()

    // Query para obtener un item específico (útil para saber si existe)
    @Query("SELECT * FROM cart_items WHERE productId = :productId LIMIT 1")
    suspend fun getCartItem(productId: Int): CartItem?
}

// Clase de datos para combinar CartItem y ProductDetails
data class CartItemWithDetails(
    val productId: Int,
    val quantity: Int,
    val name: String,
    val price: String,
    val image: String
)