package com.example.levelup_gamerpractica.data.local

import android.content.Context
import android.net.Uri
import com.example.levelup_gamerpractica.data.local.dao.CartItemWithDetails
import com.example.levelup_gamerpractica.data.local.entities.CartItem
import com.example.levelup_gamerpractica.data.local.entities.Product
import com.example.levelup_gamerpractica.data.local.entities.User
import com.example.levelup_gamerpractica.data.model.*
import com.example.levelup_gamerpractica.data.remote.ProductNetworkDto
import com.example.levelup_gamerpractica.data.remote.RetrofitInstance
import com.example.levelup_gamerpractica.utils.SessionManager
import com.example.levelup_gamerpractica.utils.TokenManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.text.NumberFormat
import java.util.Locale
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody

class AppRepository(
    private val database: AppDatabase,
    private val context: Context
) {

    private val apiService = RetrofitInstance.api
    private val productDao = database.productDao()
    private val userDao = database.userDao()
    private val cartDao = database.cartDao()
    private val sessionManager = SessionManager(context)

    private val _currentUser = MutableStateFlow<User?>(null)
    val currentUser: Flow<User?> = _currentUser.asStateFlow()

    // Derivamos un flujo que solo emite el nombre de usuario (o null) para la UI del catálogo
    val currentUserNameFlow: Flow<String?> = currentUser.map { user ->
        user?.username
    }

    // --- AUTH (Login con Sync) ---

    suspend fun registerUserApi(request: RegisterRequest): Result<String> = withContext(Dispatchers.IO) {
        try {
            val response = apiService.register(request)
            if (response.isSuccessful) Result.success("Registro exitoso")
            else Result.failure(Exception(response.errorBody()?.string() ?: "Error"))
        } catch (e: Exception) { Result.failure(e) }
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
                        id = profile.id, username = profile.username, email = profile.email,
                        userRole = profile.userRole, pointsBalance = profile.pointsBalance,
                        userLevel = profile.userLevel, profilePictureUrl = profile.profilePictureUrl
                    )
                    _currentUser.value = user
                    userDao.insertUser(user)

                    // SYNC: Descargar carrito del servidor
                    syncCartFromBackend()

                    Result.success(user)
                } else {
                    val basicUser = User(id = 0, username = username, email = "")
                    _currentUser.value = basicUser
                    Result.success(basicUser)
                }
            } else {
                Result.failure(Exception("Credenciales incorrectas"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun loadUserProfile(): Boolean = withContext(Dispatchers.IO) {
        try {
            val response = apiService.getUserProfile()
            if (response.isSuccessful && response.body() != null) {
                val profile = response.body()!!
                val user = User(
                    id = profile.id, username = profile.username, email = profile.email,
                    userRole = profile.userRole, pointsBalance = profile.pointsBalance,
                    userLevel = profile.userLevel, profilePictureUrl = profile.profilePictureUrl
                )
                _currentUser.value = user
                userDao.insertUser(user)

                // SYNC: Descargar carrito al inicio automático
                syncCartFromBackend()

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
        cartDao.clearCart()
        userDao.deleteAllUsers()
    }

    // --- PROFILE ---

    private fun copyImageToInternalStorage(contentUri: Uri): String {
        val inputStream = context.contentResolver.openInputStream(contentUri)
        val file = File(context.cacheDir, "temp_profile_upload.jpg")
        val outputStream = FileOutputStream(file)
        inputStream.use { input -> outputStream.use { output -> input?.copyTo(output) } }
        return file.absolutePath
    }

    suspend fun updateProfilePicture(contentUriString: String?): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val user = _currentUser.value ?: throw Exception("No user")
            if (contentUriString == null) {
                return@withContext Result.success(Unit)
            }
            val file = File(copyImageToInternalStorage(Uri.parse(contentUriString)))
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
                if(file.exists()) file.delete()
                Result.failure(Exception("Error subida"))
            }
        } catch (e: Exception) { Result.failure(e) }
    }

    suspend fun updateUserDetails(newUsername: String?, newEmail: String?): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val user = _currentUser.value ?: throw Exception("No user")
            val updates = mutableMapOf<String, String>()
            if (!newUsername.isNullOrBlank() && newUsername != user.username) updates["username"] = newUsername
            if (!newEmail.isNullOrBlank() && newEmail != user.email) updates["email"] = newEmail

            if (updates.isEmpty()) return@withContext Result.success(Unit)

            val response = apiService.updateProfile(updates)
            if (response.isSuccessful && response.body() != null) {
                val updatedProfile = response.body()!!
                userDao.updateUsername(user.id, updatedProfile.username)
                userDao.updateUserEmail(user.id, updatedProfile.email)
                _currentUser.value = user.copy(username = updatedProfile.username, email = updatedProfile.email)
                Result.success(Unit)
            } else {
                Result.failure(Exception("Error actualización"))
            }
        } catch (e: Exception) { Result.failure(e) }
    }

    // --- ¡AQUÍ ESTÁ LA FUNCIÓN FALTANTE! ---
    suspend fun updateUserPassword(oldPasswordHash: String, newPasswordHash: String): Result<Unit> = withContext(Dispatchers.IO) {
        // Por ahora retornamos éxito simulado ya que no hay endpoint en el backend aún para cambio de password
        Result.success(Unit)
    }

    // ========================================================================
    // CARRITO (Lógica Híbrida Room + API)
    // ========================================================================

    val cartItems: Flow<List<CartItemWithDetails>> = cartDao.getCartItemsWithDetails()

    private suspend fun syncCartFromBackend() {
        try {
            val response = apiService.getMyCart()
            if (response.isSuccessful && response.body() != null) {
                updateLocalFromResponse(response.body()!!)
            }
        } catch (e: Exception) { e.printStackTrace() }
    }

    suspend fun addToCart(productId: Int) = withContext(Dispatchers.IO) {
        val user = _currentUser.value
        if (user != null) {
            // ONLINE
            try {
                val request = CartItemRequest(productId, 1)
                val response = apiService.addItemToCart(request)
                if (response.isSuccessful && response.body() != null) {
                    updateLocalFromResponse(response.body()!!)
                }
            } catch (e: Exception) { e.printStackTrace() }
        } else {
            // OFFLINE
            val existingItem = cartDao.getCartItem(productId)
            if (existingItem != null) {
                cartDao.updateQuantity(productId, existingItem.quantity + 1)
            } else {
                cartDao.upsertCartItem(CartItem(productId = productId, quantity = 1))
            }
        }
    }

    suspend fun increaseCartItemQuantity(productId: Int) = withContext(Dispatchers.IO) {
        val currentItem = cartDao.getCartItem(productId) ?: return@withContext
        val newQuantity = currentItem.quantity + 1

        val user = _currentUser.value
        if (user != null) {
            try {
                val response = apiService.updateCartItem(productId, mapOf("quantity" to newQuantity))
                if (response.isSuccessful && response.body() != null) {
                    updateLocalFromResponse(response.body()!!)
                }
            } catch (e: Exception) { e.printStackTrace() }
        } else {
            cartDao.updateQuantity(productId, newQuantity)
        }
    }

    suspend fun decreaseCartItemQuantity(productId: Int) = withContext(Dispatchers.IO) {
        val currentItem = cartDao.getCartItem(productId) ?: return@withContext
        val newQuantity = currentItem.quantity - 1

        val user = _currentUser.value
        if (user != null) {
            try {
                val response = apiService.updateCartItem(productId, mapOf("quantity" to newQuantity))
                if (response.isSuccessful && response.body() != null) {
                    updateLocalFromResponse(response.body()!!)
                }
            } catch (e: Exception) { e.printStackTrace() }
        } else {
            if (newQuantity > 0) {
                cartDao.updateQuantity(productId, newQuantity)
            } else {
                cartDao.deleteCartItem(productId)
            }
        }
    }

    suspend fun removeFromCart(productId: Int) = withContext(Dispatchers.IO) {
        val user = _currentUser.value
        if (user != null) {
            try {
                val response = apiService.removeCartItem(productId)
                if (response.isSuccessful && response.body() != null) {
                    updateLocalFromResponse(response.body()!!)
                }
            } catch (e: Exception) { e.printStackTrace() }
        } else {
            cartDao.deleteCartItem(productId)
        }
    }

    suspend fun clearCart() = withContext(Dispatchers.IO) {
        val user = _currentUser.value
        if (user != null) {
            try {
                val response = apiService.clearCartApi()
                if (response.isSuccessful) {
                    cartDao.clearCart()
                }
            } catch (e: Exception) { e.printStackTrace() }
        } else {
            cartDao.clearCart()
        }
    }

    private suspend fun updateLocalFromResponse(serverCart: CartResponse) {
        cartDao.clearCart()
        val localItems = serverCart.items.map {
            CartItem(productId = it.product.id.toInt(), quantity = it.quantity)
        }
        localItems.forEach { cartDao.upsertCartItem(it) }
    }

    // ========================================================================
    // CHECKOUT
    // ========================================================================

    suspend fun checkout(): Result<OrderResponse> = withContext(Dispatchers.IO) {
        try {
            val response = apiService.checkout()
            if (response.isSuccessful && response.body() != null) {
                cartDao.clearCart()
                loadUserProfile()
                Result.success(response.body()!!)
            } else {
                Result.failure(Exception(response.errorBody()?.string() ?: "Error en checkout"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getMyOrders(): Result<List<OrderResponse>> = withContext(Dispatchers.IO) {
        try {
            val response = apiService.getMyOrders()
            if (response.isSuccessful && response.body() != null) {
                Result.success(response.body()!!)
            } else {
                Result.failure(Exception("Error al cargar órdenes"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // --- PRODUCTOS ---

    private fun mapDtoToEntity(dto: ProductNetworkDto): Product {
        val formatClp = NumberFormat.getCurrencyInstance(Locale("es", "CL")).apply { maximumFractionDigits = 0 }
        return Product(
            id = dto.id.toInt(), name = dto.name, price = formatClp.format(dto.price),
            category = dto.category, image = dto.image, description = dto.description,
            manufacturer = dto.manufacturer, distributor = dto.distributor
        )
    }

    suspend fun refreshProducts() {
        try {
            val productDtoList = apiService.getAllProducts()
            val productEntityList = productDtoList.map { mapDtoToEntity(it) }
            productDao.deleteAll()
            productDao.insertAll(productEntityList)
        } catch (e: Exception) { e.printStackTrace() }
    }

    val allProducts: Flow<List<Product>> = productDao.getAllProducts()
    val allCategories: Flow<List<String>> = productDao.getAllCategories()
    fun getProductsByCategory(category: String): Flow<List<Product>> = productDao.getProductsByCategory(category)
}