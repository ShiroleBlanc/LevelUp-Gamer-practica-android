package com.example.levelup_gamerpractica.data.local.dao

import androidx.room.*
import com.example.levelup_gamerpractica.data.local.entities.Product
import kotlinx.coroutines.flow.Flow

@Dao
interface ProductDao {
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertIgnore(products: List<Product>): List<Long>
    @Update
    suspend fun update(products: List<Product>)
    @Transaction
    suspend fun safeUpsertAll(products: List<Product>) {
        val insertResults = insertIgnore(products)
        val productsToUpdate = mutableListOf<Product>()
        for (i in insertResults.indices) {
            if (insertResults[i] == -1L) {
                productsToUpdate.add(products[i])
            }
        }

        if (productsToUpdate.isNotEmpty()) {
            update(productsToUpdate)
        }
    }
    @Transaction
    suspend fun safeUpsertProduct(product: Product) {
        safeUpsertAll(listOf(product))
    }
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
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(products: List<Product>)
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProduct(product: Product)
}