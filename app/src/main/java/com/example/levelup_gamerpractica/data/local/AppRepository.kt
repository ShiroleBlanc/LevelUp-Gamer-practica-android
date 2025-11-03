package com.example.levelup_gamerpractica.data.local

import com.example.levelup_gamerpractica.data.local.AppDatabase
import com.example.levelup_gamerpractica.data.local.dao.CartItemWithDetails
import com.example.levelup_gamerpractica.data.local.entities.CartItem
import com.example.levelup_gamerpractica.data.local.entities.Product
import com.example.levelup_gamerpractica.data.local.entities.User
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

// Repositorio: Único punto de acceso a los datos
class AppRepository(private val database: AppDatabase) {

    // Mantiene al usuario actual en memoria
    private val _currentUser = MutableStateFlow<User?>(null)
    val currentUser: Flow<User?> = _currentUser.asStateFlow()

    // Un Flow derivado que solo expone el nombre del usuario (o null)
    val currentUserNameFlow: Flow<String?> = currentUser.map { user ->
        user?.username
    }

    // --- Operaciones de usuario ---
    suspend fun registerUser(user: User): Result<Unit> = withContext(Dispatchers.IO) {
        try {
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
            // Asumiendo que tu entidad User tiene un campo 'passwordHash'
            if (user != null && user.passwordHash == passwordHash) {
                _currentUser.value = user
                Result.success(user)
            } else {
                _currentUser.value = null
                Result.failure(Exception("Correo o contraseña incorrectos."))
            }
        } catch (e: Exception) {
            _currentUser.value = null
            Result.failure(e)
        }
    }

    // Añade una función de logout
    suspend fun logoutUser() = withContext(Dispatchers.IO) {
        _currentUser.value = null
        clearCart()
    }

    // --- FUNCIÓN AÑADIDA ---
    /**
     * Actualiza la foto de perfil del usuario logueado.
     * Esto actualiza tanto la BD como el StateFlow en memoria.
     */
    suspend fun updateProfilePicture(uri: String?): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val user = _currentUser.value

            // Asumiendo que tu entidad User es un data class y tiene 'id'
            if (user != null) {
                // 1. Actualizar la base de datos
                database.userDao().updateProfilePicture(user.id, uri)

                // 2. Actualizar el usuario en memoria (StateFlow)
                _currentUser.value = user.copy(profilePictureUri = uri)

                Result.success(Unit)
            } else {
                Result.failure(Exception("No hay ningún usuario logueado."))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }


    // --- operaciones de producto ---
    val allProducts: Flow<List<Product>> = database.productDao().getAllProducts()
    val allCategories: Flow<List<String>> = database.productDao().getAllCategories()
    fun getProductsByCategory(category: String): Flow<List<Product>> = database.productDao().getProductsByCategory(category)

    // --- Operaciones de carrito ---
    val cartItems: Flow<List<CartItemWithDetails>> = database.cartDao().getCartItemsWithDetails()

    // --- LÍNEA CORREGIDA ---
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