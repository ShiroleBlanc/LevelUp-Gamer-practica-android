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
            // Usa las funciones del UserDao (el archivo en tu Canvas)
            if (database.userDao().getUserByEmail(user.email) != null) {
                Result.failure(Exception("El correo ya está registrado."))
            } else if (database.userDao().getUserByUsername(user.username) != null) {
                Result.failure(Exception("El nombre de usuario ya está en uso."))
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

    suspend fun logoutUser() = withContext(Dispatchers.IO) {
        _currentUser.value = null
        clearCart()
    }

    suspend fun updateProfilePicture(uri: String?): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val user = _currentUser.value
            if (user != null) {
                database.userDao().updateProfilePicture(user.id, uri)
                // Actualizar el usuario en memoria (StateFlow)
                _currentUser.value = user.copy(profilePictureUri = uri)
                Result.success(Unit)
            } else {
                Result.failure(Exception("No hay ningún usuario logueado."))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // --- FUNCIÓN ACTUALIZADA ---
    /**
     * Actualiza el nombre de usuario y/o email del usuario actual.
     * Acepta valores nulos si el usuario no quiere cambiar uno de los campos.
     */
    suspend fun updateUserDetails(newUsername: String?, newEmail: String?): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val user = _currentUser.value ?: throw Exception("No hay usuario logueado")
            var updatedUser = user

            // Actualizar nombre de usuario si se proporcionó y es diferente
            if (newUsername != null && newUsername != user.username) {
                if (database.userDao().getUserByUsername(newUsername) != null) {
                    throw Exception("El nombre de usuario ya está en uso.")
                }
                database.userDao().updateUsername(user.id, newUsername)
                updatedUser = updatedUser.copy(username = newUsername)
            }

            // Actualizar email si se proporcionó y es diferente
            if (newEmail != null && newEmail != user.email) {
                if (database.userDao().getUserByEmail(newEmail) != null) {
                    throw Exception("El correo electrónico ya está en uso.")
                }
                database.userDao().updateUserEmail(user.id, newEmail)
                updatedUser = updatedUser.copy(email = newEmail)
            }

            _currentUser.value = updatedUser // Actualizar el StateFlow
            Result.success(Unit)

        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // --- FUNCIÓN ACTUALIZADA ---
    /**
     * Actualiza la contraseña del usuario actual tras verificar la antigua.
     * Usa los nombres de parámetro correctos que espera el ViewModel.
     */
    suspend fun updateUserPassword(oldPassword: String, newPassword: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val user = _currentUser.value ?: throw Exception("No hay usuario logueado")

            // 1. Verificar la contraseña antigua
            if (user.passwordHash != oldPassword) {
                throw Exception("La contraseña actual es incorrecta.")
            }

            // 2. Actualizar a la nueva contraseña
            database.userDao().updatePassword(user.id, newPassword)

            // 3. Actualizar el StateFlow (con el hash nuevo)
            _currentUser.value = user.copy(passwordHash = newPassword)
            Result.success(Unit)

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
