package com.example.levelup_gamerpractica.data.local

import android.content.Context
import android.net.Uri
import com.example.levelup_gamerpractica.data.local.dao.CartItemWithDetails
import com.example.levelup_gamerpractica.data.local.entities.CartItem
import com.example.levelup_gamerpractica.data.local.entities.Product
import com.example.levelup_gamerpractica.data.local.entities.User
import com.example.levelup_gamerpractica.data.model.LoginRequest
import com.example.levelup_gamerpractica.data.model.RegisterRequest
import com.example.levelup_gamerpractica.data.remote.ProductNetworkDto
import com.example.levelup_gamerpractica.data.remote.RetrofitInstance
import com.example.levelup_gamerpractica.utils.SessionManager
import com.example.levelup_gamerpractica.utils.TokenManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
// Eliminamos NumberFormat, ya no lo necesitamos aquí
import java.util.UUID
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody

class AppRepository(
    private val database: AppDatabase,
    private val context: Context
) {

    // --- INSTANCIAS ---
    private val apiService = RetrofitInstance.api
    private val productDao = database.productDao()
    private val userDao = database.userDao()
    private val sessionManager = SessionManager(context)

    // --- ESTADO DE USUARIO ---
    private val _currentUser = MutableStateFlow<User?>(null)
    val currentUser: Flow<User?> = _currentUser.asStateFlow()

    val currentUserNameFlow: Flow<String?> = currentUser.map { user ->
        user?.username
    }

    // ========================================================================
    // AUTENTICACIÓN CON BACKEND (LOGIN Y REGISTRO)
    // ========================================================================

    suspend fun registerUserApi(request: RegisterRequest): Result<String> = withContext(Dispatchers.IO) {
        try {
            val response = apiService.register(request)
            if (response.isSuccessful) {
                Result.success("Registro exitoso. Por favor inicia sesión.")
            } else {
                val errorMsg = response.errorBody()?.string() ?: "Error en el registro"
                Result.failure(Exception(errorMsg))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun loginUserApi(username: String, password: String): Result<User> = withContext(Dispatchers.IO) {
        try {
            val request = LoginRequest(username, password)
            val loginResponse = apiService.login(request)

            if (loginResponse.isSuccessful && loginResponse.body() != null) {
                val token = loginResponse.body()!!.token

                sessionManager.saveAuthToken(token)
                TokenManager.setToken(token)

                val profileResponse = apiService.getUserProfile()

                if (profileResponse.isSuccessful && profileResponse.body() != null) {
                    val profile = profileResponse.body()!!

                    val user = User(
                        id = profile.id,
                        username = profile.username,
                        email = profile.email,
                        userRole = profile.userRole,
                        pointsBalance = profile.pointsBalance,
                        userLevel = profile.userLevel,
                        profilePictureUrl = profile.profilePictureUrl
                    )

                    _currentUser.value = user
                    userDao.insertUser(user)

                    Result.success(user)
                } else {
                    val basicUser = User(id = 0, username = username, email = "")
                    _currentUser.value = basicUser
                    Result.success(basicUser)
                }
            } else {
                _currentUser.value = null
                Result.failure(Exception("Credenciales incorrectas"))
            }
        } catch (e: Exception) {
            _currentUser.value = null
            Result.failure(e)
        }
    }

    suspend fun loadUserProfile(): Boolean = withContext(Dispatchers.IO) {
        try {
            val response = apiService.getUserProfile()
            if (response.isSuccessful && response.body() != null) {
                val profile = response.body()!!

                val user = User(
                    id = profile.id,
                    username = profile.username,
                    email = profile.email,
                    userRole = profile.userRole,
                    pointsBalance = profile.pointsBalance,
                    userLevel = profile.userLevel,
                    profilePictureUrl = profile.profilePictureUrl
                )

                _currentUser.value = user
                userDao.insertUser(user)
                true
            } else {
                false
            }
        } catch (e: Exception) {
            false
        }
    }

    suspend fun logoutUser() = withContext(Dispatchers.IO) {
        sessionManager.logout()
        TokenManager.setToken(null)
        _currentUser.value = null
        clearCart()
        userDao.deleteAllUsers()
    }

    // ========================================================================
    // GESTIÓN DE PERFIL
    // ========================================================================

    private fun copyImageToInternalStorage(contentUri: Uri): String {
        val inputStream = context.contentResolver.openInputStream(contentUri)
        val fileName = "temp_profile_upload.jpg"
        val file = File(context.cacheDir, fileName)
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
                userDao.updateProfilePicture(user.id, null)
                _currentUser.value = user.copy(profilePictureUrl = null)
                return@withContext Result.success(Unit)
            }

            val uri = Uri.parse(contentUriString)
            val filePath = copyImageToInternalStorage(uri)
            val file = File(filePath)

            val requestFile = file.asRequestBody("image/*".toMediaTypeOrNull())
            val body = MultipartBody.Part.createFormData("file", file.name, requestFile)

            val response = apiService.uploadProfilePicture(body)

            if (response.isSuccessful && response.body() != null) {
                val newUrl = response.body()!!["profilePictureUrl"]

                if (newUrl != null) {
                    userDao.updateProfilePicture(user.id, newUrl)
                    _currentUser.value = user.copy(profilePictureUrl = newUrl)
                }
                if(file.exists()) file.delete()
                Result.success(Unit)
            } else {
                val errorMsg = response.errorBody()?.string() ?: "Error al subir imagen"
                Result.failure(Exception(errorMsg))
            }

        } catch (e: Exception) {
            e.printStackTrace()
            Result.failure(e)
        }
    }

    suspend fun updateUserDetails(newUsername: String?, newEmail: String?): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val user = _currentUser.value ?: throw Exception("No hay usuario logueado")
            val updates = mutableMapOf<String, String>()

            if (!newUsername.isNullOrBlank() && newUsername != user.username) {
                updates["username"] = newUsername
            }
            if (!newEmail.isNullOrBlank() && newEmail != user.email) {
                updates["email"] = newEmail
            }

            if (updates.isEmpty()) {
                return@withContext Result.success(Unit)
            }

            val response = apiService.updateProfile(updates)

            if (response.isSuccessful && response.body() != null) {
                val updatedProfile = response.body()!!
                val updatedUser = user.copy(
                    username = updatedProfile.username,
                    email = updatedProfile.email
                )

                userDao.updateUsername(user.id, updatedProfile.username)
                userDao.updateUserEmail(user.id, updatedProfile.email)

                _currentUser.value = updatedUser
                Result.success(Unit)
            } else {
                val errorMsg = response.errorBody()?.string() ?: "Error al actualizar perfil"
                Result.failure(Exception(errorMsg))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun updateUserPassword(oldPasswordHash: String, newPasswordHash: String): Result<Unit> = withContext(Dispatchers.IO) {
        Result.success(Unit)
    }

    // ========================================================================
    // PRODUCTOS (CORREGIDO PARA LONG Y DOUBLE)
    // ========================================================================

    private fun mapDtoToEntity(dto: ProductNetworkDto): Product {
        // CAMBIO IMPORTANTE: Ya no formateamos el precio a String.
        // Lo pasamos directo como Double.

        return Product(
            id = dto.id.toLong(), // Aseguramos que sea Long
            name = dto.name,
            price = dto.price,    // Double puro
            category = dto.category,
            imageUrl = dto.imageUrl, // Cambiado de 'image' a 'imageUrl'
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

                productDao.deleteAll()
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


    // ========================================================================
    // CARRITO (ACTUALIZADO PARA USAR ID LONG)
    // ========================================================================

    val cartItems: Flow<List<CartItemWithDetails>> = database.cartDao().getCartItemsWithDetails()

    // Todos los parámetros productId ahora son Long
    suspend fun addToCart(productId: Long) = withContext(Dispatchers.IO) {
        val existingItem = database.cartDao().getCartItem(productId)
        if (existingItem != null) {
            database.cartDao().updateQuantity(productId, existingItem.quantity + 1)
        } else {
            // Nota: Asegúrate de que tu entidad CartItem también tenga productId como Long
            database.cartDao().upsertCartItem(CartItem(productId = productId, quantity = 1))
        }
    }

    suspend fun increaseCartItemQuantity(productId: Long) = withContext(Dispatchers.IO) {
        val item = database.cartDao().getCartItem(productId)
        if (item != null) {
            database.cartDao().updateQuantity(productId, item.quantity + 1)
        }
    }

    suspend fun decreaseCartItemQuantity(productId: Long) = withContext(Dispatchers.IO) {
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

    suspend fun removeFromCart(productId: Long) = withContext(Dispatchers.IO) {
        database.cartDao().deleteCartItem(productId)
    }

    suspend fun clearCart() = withContext(Dispatchers.IO) {
        database.cartDao().clearCart()
    }
}