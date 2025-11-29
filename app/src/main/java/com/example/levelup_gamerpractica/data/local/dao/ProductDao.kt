package com.example.levelup_gamerpractica.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.levelup_gamerpractica.data.local.entities.Product
import kotlinx.coroutines.flow.Flow

@Dao
interface ProductDao {

    // Inserta la lista (reemplazando si hay conflicto)
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(products: List<Product>)

    // --- ¡ESTE ES EL MÉTODO QUE TE FALTA! ---
    // Borra todos los productos de la tabla (para refrescar la caché)
    @Query("DELETE FROM products")
    suspend fun deleteAll()

    @Query("SELECT * FROM products")
    fun getAllProducts(): Flow<List<Product>>

    @Query("SELECT * FROM products WHERE category = :category")
    fun getProductsByCategory(category: String): Flow<List<Product>>

    @Query("SELECT * FROM products WHERE id = :productId LIMIT 1")
    suspend fun getProductById(productId: Long): Product?

    @Query("SELECT DISTINCT category FROM products ORDER BY category ASC")
    fun getAllCategories(): Flow<List<String>>
}