package com.example.levelup_gamerpractica.data.local

import com.example.levelup_gamerpractica.data.local.AppDatabase
import com.example.levelup_gamerpractica.data.local.dao.CartItemWithDetails
import com.example.levelup_gamerpractica.data.local.entities.CartItem
import com.example.levelup_gamerpractica.data.local.entities.Product
import com.example.levelup_gamerpractica.data.local.entities.User
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow     // <-- IMPORTAR
import kotlinx.coroutines.flow.asStateFlow       // <-- IMPORTAR
import kotlinx.coroutines.flow.map                 // <-- IMPORTAR
import kotlinx.coroutines.withContext

// Repositorio: Único punto de acceso a los datos (abstrae si vienen de BD local, red, etc.)
class AppRepository(private val database: AppDatabase) {

    // --- User State Flow ---
    // Mantiene al usuario actual en memoria
    private val _currentUser = MutableStateFlow<User?>(null)
    val currentUser: Flow<User?> = _currentUser.asStateFlow()

    // Un Flow derivado que solo expone el nombre del usuario (o null)
    // ¡Esto es lo que usará tu CatalogViewModel!
    val currentUserNameFlow: Flow<String?> = currentUser.map { user ->
        user?.username // <-- ¡CORREGIDO! Coincide con tu entidad User.kt
    }

    // --- User Operations ---
    suspend fun registerUser(user: User): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            // Simple validación (debería ser más robusta)
            if (database.userDao().getUserByEmail(user.email) != null) {
                Result.failure(Exception("El correo ya está registrado."))
            } else {
                database.userDao().insertUser(user)
                Result.success(Unit)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun loginUser(email: String, passwordHash: String): Result<User> = withContext(Dispatchers.IO) {
        try {
            val user = database.userDao().getUserByEmail(email)
            if (user != null && user.passwordHash == passwordHash) { // Comparar hashes
                _currentUser.value = user // <-- ¡CAMBIO IMPORTANTE! Guarda el usuario
                Result.success(user)
            } else {
                _currentUser.value = null // <-- Asegura que esté nulo si falla
                Result.failure(Exception("Correo o contraseña incorrectos."))
            }
        } catch (e: Exception) {
            _currentUser.value = null // <-- Asegura que esté nulo si hay error
            Result.failure(e)
        }
    }

    // (Opcional pero recomendado) Añade una función de logout
    suspend fun logoutUser() = withContext(Dispatchers.IO) {
        _currentUser.value = null
        // Opcionalmente, puedes limpiar el carrito al hacer logout
        // clearCart()
    }


    // --- Product Operations ---
    val allProducts: Flow<List<Product>> = database.productDao().getAllProducts()
    val allCategories: Flow<List<String>> = database.productDao().getAllCategories()
    fun getProductsByCategory(category: String): Flow<List<Product>> = database.productDao().getProductsByCategory(category)

    // --- Cart Operations ---
    val cartItems: Flow<List<CartItemWithDetails>> = database.cartDao().getCartItemsWithDetails()

    suspend fun addToCart(productId: Int) = withContext(Dispatchers.IO) {
        val existingItem = database.cartDao().getCartItem(productId)
        if (existingItem != null) {
            database.cartDao().updateQuantity(productId, existingItem.quantity + 1)
        } else {
            database.cartDao().upsertCartItem(CartItem(productId = productId, quantity = 1))
        }
    }

    suspend fun increaseCartItemQuantity(productId: Int) = withContext(Dispatchers.IO) {
        val item = database.cartDao().getCartItem(productId)
        if (item != null) {
            database.cartDao().updateQuantity(productId, item.quantity + 1)
        }
    }

    suspend fun decreaseCartItemQuantity(productId: Int) = withContext(Dispatchers.IO) {
        val item = database.cartDao().getCartItem(productId)
        if (item != null) {
            val newQuantity = item.quantity - 1
            if (newQuantity > 0) {
                database.cartDao().updateQuantity(productId, newQuantity)
            } else {
                database.cartDao().deleteCartItem(productId) // Elimina si la cantidad es 0
            }
        }
    }

    suspend fun removeFromCart(productId: Int) = withContext(Dispatchers.IO) {
        database.cartDao().deleteCartItem(productId)
    }

    suspend fun clearCart() = withContext(Dispatchers.IO) {
        database.cartDao().clearCart()
    }
}


