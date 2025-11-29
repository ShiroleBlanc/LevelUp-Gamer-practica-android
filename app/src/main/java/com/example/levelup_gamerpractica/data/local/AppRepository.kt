package com.example.levelup_gamerpractica.data.local

import android.content.Context
import android.net.Uri
import android.util.Log
import com.example.levelup_gamerpractica.data.local.dao.CartItemWithDetails
import com.example.levelup_gamerpractica.data.local.entities.CartItem
import com.example.levelup_gamerpractica.data.local.entities.Product
import com.example.levelup_gamerpractica.data.local.entities.User
import com.example.levelup_gamerpractica.data.model.CartItemRequest
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
    private val cartDao = database.cartDao()
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

                    // --- NUEVO: Sincronizar carrito al loguear ---
                    syncCartFromBackend()

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
            if (!sessionManager.isLoggedIn()) return@withContext false

            // Asegurar que el TokenManager tenga el token en memoria
            if (TokenManager.getToken() == null) {
                sessionManager.fetchAuthToken()?.let { TokenManager.setToken(it) }
            }

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

                // --- NUEVO: Si recargamos perfil, también aseguramos el carrito ---
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

        // --- IMPORTANTE: Limpiar base de datos local al salir ---
        cartDao.clearCart()
        userDao.deleteAllUsers()
    }

    // ========================================================================
    // GESTIÓN DE PERFIL (Mantenemos tu código original)
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
        // Implementación pendiente en backend según lo conversado, placeholder:
        Result.success(Unit)
    }

    // ========================================================================
    // PRODUCTOS
    // ========================================================================

    private fun mapDtoToEntity(dto: ProductNetworkDto): Product {
        return Product(
            id = dto.id,
            name = dto.name,
            price = dto.price,
            category = dto.category,
            imageUrl = dto.imageUrl,
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
    // CARRITO (LÓGICA HÍBRIDA SINCRONIZADA)
    // ========================================================================

    val cartItems: Flow<List<CartItemWithDetails>> = cartDao.getCartItemsWithDetails()

    /**
     * Descarga el carrito del servidor y reemplaza el local.
     */
    private suspend fun syncCartFromBackend() {
        try {
            val response = apiService.getMyCart()
            if (response.isSuccessful && response.body() != null) {
                val backendCart = response.body()!!

                // 1. Limpiamos carrito local para evitar duplicados viejos
                cartDao.clearCart()

                // 2. Insertamos los items del backend en Room
                backendCart.items.forEach { itemDto ->
                    cartDao.upsertCartItem(
                        CartItem(
                            productId = itemDto.product.id,
                            quantity = itemDto.quantity
                        )
                    )
                }
                Log.d("AppRepository", "Carrito sincronizado: ${backendCart.items.size} items")
            }
        } catch (e: Exception) {
            Log.e("AppRepository", "Error sincronizando carrito: ${e.message}")
        }
    }

    suspend fun addToCart(productId: Long) = withContext(Dispatchers.IO) {
        if (sessionManager.isLoggedIn()) {
            // MODO ONLINE: Primero API, luego Local
            try {
                // El backend espera int, pasamos productId.toInt()
                val request = CartItemRequest(productId = productId.toInt(), quantity = 1)
                val response = apiService.addItemToCart(request)

                if (response.isSuccessful) {
                    // Si el backend dice OK, actualizamos localmente
                    val existing = cartDao.getCartItem(productId)
                    val newQty = (existing?.quantity ?: 0) + 1
                    cartDao.upsertCartItem(CartItem(productId, newQty))
                }
            } catch (e: Exception) {
                Log.e("Cart", "Error al añadir al backend: ${e.message}")
            }
        } else {
            // MODO OFFLINE: Solo Local
            val existing = cartDao.getCartItem(productId)
            val newQty = (existing?.quantity ?: 0) + 1
            cartDao.upsertCartItem(CartItem(productId, newQty))
        }
    }

    suspend fun increaseCartItemQuantity(productId: Long) = withContext(Dispatchers.IO) {
        val currentItem = cartDao.getCartItem(productId) ?: return@withContext
        val newQty = currentItem.quantity + 1

        if (sessionManager.isLoggedIn()) {
            try {
                val body = mapOf("quantity" to newQty)
                val response = apiService.updateCartItem(productId.toInt(), body)
                if (response.isSuccessful) {
                    cartDao.updateQuantity(productId, newQty)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        } else {
            cartDao.updateQuantity(productId, newQty)
        }
    }

    suspend fun decreaseCartItemQuantity(productId: Long) = withContext(Dispatchers.IO) {
        val currentItem = cartDao.getCartItem(productId) ?: return@withContext
        val newQty = currentItem.quantity - 1

        if (newQty > 0) {
            if (sessionManager.isLoggedIn()) {
                try {
                    val body = mapOf("quantity" to newQty)
                    val response = apiService.updateCartItem(productId.toInt(), body)
                    if (response.isSuccessful) cartDao.updateQuantity(productId, newQty)
                } catch (e: Exception) { e.printStackTrace() }
            } else {
                cartDao.updateQuantity(productId, newQty)
            }
        } else {
            // Si baja a 0, eliminar
            removeFromCart(productId)
        }
    }

    suspend fun removeFromCart(productId: Long) = withContext(Dispatchers.IO) {
        if (sessionManager.isLoggedIn()) {
            try {
                val response = apiService.removeCartItem(productId.toInt())
                if (response.isSuccessful) {
                    cartDao.deleteCartItem(productId)
                }
            } catch (e: Exception) { e.printStackTrace() }
        } else {
            cartDao.deleteCartItem(productId)
        }
    }

    suspend fun clearCart() = withContext(Dispatchers.IO) {
        if (sessionManager.isLoggedIn()) {
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

    // ========================================================================
    // CHECKOUT / FINALIZAR COMPRA
    // ========================================================================

    suspend fun checkout(): Result<String> = withContext(Dispatchers.IO) {
        if (!sessionManager.isLoggedIn()) {
            return@withContext Result.failure(Exception("Debes iniciar sesión para comprar."))
        }

        try {
            val response = apiService.checkout()
            if (response.isSuccessful && response.body() != null) {
                // Compra exitosa en backend -> Limpiamos carrito local
                cartDao.clearCart()

                // Recargamos el perfil para que se actualicen los puntos y nivel
                loadUserProfile()

                Result.success("Compra realizada con éxito. Orden #${response.body()!!.id}")
            } else {
                val error = response.errorBody()?.string() ?: "Error en el pago"
                Result.failure(Exception(error))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}