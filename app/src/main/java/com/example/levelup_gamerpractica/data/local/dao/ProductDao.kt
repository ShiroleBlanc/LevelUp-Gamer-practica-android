package com.example.levelup_gamerpractica.data.local.dao

import androidx.room.*
import com.example.levelup_gamerpractica.data.local.entities.Product
import kotlinx.coroutines.flow.Flow

@Dao
interface ProductDao {

    // --- MÉTODOS BASE (Privados o auxiliares) ---

    // 1. Intenta insertar. Si existe, no hace nada (retorna -1)
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertIgnore(products: List<Product>): List<Long>

    // 2. Actualiza solo si existe
    @Update
    suspend fun update(products: List<Product>)

    // --- MÉTODOS PÚBLICOS (Upsert Seguro) ---

    /**
     * Esta es la solución MÁGICA.
     * Intenta insertar los productos. Los que ya existen (conflicto), los actualiza.
     * AL NO USAR 'REPLACE', NO SE BORRA LA FILA, Y EL CARRITO SOBREVIVE.
     */
    @Transaction
    suspend fun safeUpsertAll(products: List<Product>) {
        val insertResults = insertIgnore(products)
        val productsToUpdate = mutableListOf<Product>()

        for (i in insertResults.indices) {
            if (insertResults[i] == -1L) {
                // El producto ya existía, lo agregamos a la lista para actualizar
                productsToUpdate.add(products[i])
            }
        }

        if (productsToUpdate.isNotEmpty()) {
            update(productsToUpdate)
        }
    }

    // Sobrecarga para un solo producto (usado en sincronización)
    @Transaction
    suspend fun safeUpsertProduct(product: Product) {
        safeUpsertAll(listOf(product))
    }

    // --- MÉTODOS DE LECTURA ---

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

    // Mantenemos el antiguo por compatibilidad, pero recomendamos no usarlo si hay carrito
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(products: List<Product>)

    // Método auxiliar para insertar un producto individual con REPLACE (si se necesitara explícitamente)
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProduct(product: Product)
}