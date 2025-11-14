package com.example.levelup_gamerpractica.data.local

import android.content.Context 
import android.net.Uri
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
import java.io.File 
import java.io.FileOutputStream 
import java.util.UUID
import com.example.levelup_gamerpractica.data.remote.RetrofitInstance
import com.example.levelup_gamerpractica.data.remote.ProductNetworkDto
import java.text.NumberFormat // Importar para formatear el precio
import java.util.Locale // Importar para el formato de CLP

class AppRepository(
    private val database: AppDatabase,
    private val context: Context 
) {

    private val apiService = RetrofitInstance.api
    private val productDao = database.productDao()

    private val _currentUser = MutableStateFlow<User?>(null)
    val currentUser: Flow<User?> = _currentUser.asStateFlow()

    val currentUserNameFlow: Flow<String?> = currentUser.map { user ->
        user?.username
    }

    // --- Operaciones de usuario ---
    suspend fun registerUser(user: User): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            if (database.userDao().getUserByEmail(user.email) != null) {
                return@withContext Result.failure(Exception("El correo ya está registrado."))
            }
            if (database.userDao().getUserByUsername(user.username) != null) {
                return@withContext Result.failure(Exception("El nombre de usuario ya está en uso."))
            }

            database.userDao().insertUser(user)
            Result.success(Unit)

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

    private fun copyImageToInternalStorage(contentUri: Uri): String {
        val inputStream = context.contentResolver.openInputStream(contentUri)
        val fileName = "profile_${UUID.randomUUID()}.jpg"
        val file = File(context.filesDir, fileName)
        val outputStream = FileOutputStream(file)

        inputStream.use { input ->
            outputStream.use { output ->
                input?.copyTo(output)
            }
        }
        return file.absolutePath
    }


    suspend fun updateProfilePicture(contentUriString: String?): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val user = _currentUser.value ?: throw Exception("No hay usuario logueado")

            if (contentUriString == null) {
                database.userDao().updateProfilePicture(user.id, null)
                _currentUser.value = user.copy(profilePictureUri = null)
                return@withContext Result.success(Unit)
            }

            val permanentFilePath = copyImageToInternalStorage(Uri.parse(contentUriString))

            database.userDao().updateProfilePicture(user.id, permanentFilePath)

            _currentUser.value = user.copy(profilePictureUri = permanentFilePath)

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun updateUserDetails(newUsername: String?, newEmail: String?): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val user = _currentUser.value ?: throw Exception("No hay usuario logueado")

            // Usar los valores nuevos solo si no son nulos o vacíos, sino mantener los antiguos
            val finalUsername = newUsername?.takeIf { it.isNotBlank() } ?: user.username
            val finalEmail = newEmail?.takeIf { it.isNotBlank() } ?: user.email

            if (finalEmail != user.email) {
                val emailCheck = database.userDao().getUserByEmail(finalEmail)
                if (emailCheck != null && emailCheck.id != user.id) {
                    return@withContext Result.failure(Exception("El nuevo correo ya está en uso."))
                }
            }
            if (finalUsername != user.username) {
                val usernameCheck = database.userDao().getUserByUsername(finalUsername)
                if (usernameCheck != null && usernameCheck.id != user.id) {
                    return@withContext Result.failure(Exception("El nuevo nombre de usuario ya está en uso."))
                }
            }

            val updatedUser = user.copy(username = finalUsername, email = finalEmail)
            database.userDao().updateUsername(user.id, finalUsername)
            database.userDao().updateUserEmail(user.id, finalEmail)

            _currentUser.value = updatedUser
            Result.success(Unit)

        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun updateUserPassword(oldPasswordHash: String, newPasswordHash: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val user = _currentUser.value ?: throw Exception("No hay usuario logueado")

            if (user.passwordHash != oldPasswordHash) {
                return@withContext Result.failure(Exception("La contraseña actual es incorrecta."))
            }

            database.userDao().updatePassword(user.id, newPasswordHash)

            _currentUser.value = user.copy(passwordHash = newPasswordHash)
            Result.success(Unit)

        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun mapDtoToEntity(dto: ProductNetworkDto): Product {
        val formatClp = NumberFormat.getCurrencyInstance(Locale("es", "CL")).apply {
            maximumFractionDigits = 0
        }
        val formattedPrice = formatClp.format(dto.price) // Convierte 29990.0 a "$29.990 CLP"

        return Product(
            id = dto.id.toInt(),
            name = dto.name,
            price = formattedPrice,
            category = dto.category,
            image = dto.image, // Asume que 'imageUrl' del backend es solo el nombre (ej: "catan")
            description = dto.description,
            manufacturer = dto.manufacturer,
            distributor = dto.distributor
        )
    }

    suspend fun refreshProducts() {
        withContext(Dispatchers.IO) {
            try {
                val productDtoList = apiService.getAllProducts()

                val productEntityList = productDtoList.map { mapDtoToEntity(it) }

                productDao.insertAll(productEntityList)

                println("AppRepository: Productos actualizados desde la API.")

            } catch (e: Exception) {
                println("AppRepository: Error al refrescar productos: ${e.message}")
            }
        }
    }


    val allProducts: Flow<List<Product>> = productDao.getAllProducts()
    val allCategories: Flow<List<String>> = productDao.getAllCategories()
    fun getProductsByCategory(category: String): Flow<List<Product>> = productDao.getProductsByCategory(category)



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
                database.cartDao().deleteCartItem(productId)
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
