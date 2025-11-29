package com.example.levelup_gamerpractica.data.local

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
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
import java.io.InputStream
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

    val currentUserNameFlow: Flow<String?> = currentUser.map { user ->
        user?.username
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

    suspend fun registerUserApi(request: RegisterRequest): Result<String> = withContext(Dispatchers.IO) {
        try {
            val response = apiService.register(request)
            if (response.isSuccessful) {
                Result.success("Registro exitoso.")
            } else {
                val errorMsg = response.errorBody()?.string() ?: "Error registro"
                Result.failure(Exception(errorMsg))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun loadUserProfile(): Boolean = withContext(Dispatchers.IO) {
        try {
            if (!sessionManager.isLoggedIn()) return@withContext false
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
                val list = apiService.getAllProducts()
                val entities = list.map { mapDtoToEntity(it) }

                productDao.safeUpsertAll(entities)

                println("AppRepository: Productos actualizados (Safe Upsert).")
            } catch (e: Exception) {
                println("AppRepository: Error al refrescar productos: ${e.message}")
            }
        }
    }

    val allProducts = productDao.getAllProducts()
    val allCategories = productDao.getAllCategories()
    fun getProductsByCategory(cat: String) = productDao.getProductsByCategory(cat)

    val cartItems: Flow<List<CartItemWithDetails>> = cartDao.getCartItemsWithDetails()

    private suspend fun syncCartFromBackend() {
        try {
            val response = apiService.getMyCart()

            if (response.isSuccessful && response.body() != null) {
                val backendCart = response.body()!!

                cartDao.clearCart()

                backendCart.items.forEach { itemDto ->
                    val productEntity = mapDtoToEntity(itemDto.product)
                    productDao.safeUpsertProduct(productEntity)

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
            Log.e("AppRepository", "Excepción sincronizando carrito: ${e.message}")
            e.printStackTrace()
        }
    }

    suspend fun addToCart(productId: Long) = withContext(Dispatchers.IO) {
        if (sessionManager.isLoggedIn()) {
            try {
                val request = CartItemRequest(productId = productId.toInt(), quantity = 1)
                val response = apiService.addItemToCart(request)
                if (response.isSuccessful) {
                    val existing = cartDao.getCartItem(productId)
                    val newQty = (existing?.quantity ?: 0) + 1
                    cartDao.upsertCartItem(CartItem(productId, newQty))
                }
            } catch (e: Exception) {
                Log.e("Cart", "Error al añadir al backend: ${e.message}")
            }
        } else {
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
            } catch (e: Exception) { e.printStackTrace() }
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

    suspend fun checkout(): Result<String> = withContext(Dispatchers.IO) {
        if (!sessionManager.isLoggedIn()) {
            return@withContext Result.failure(Exception("Debes iniciar sesión para comprar."))
        }

        try {
            val response = apiService.checkout()
            if (response.isSuccessful && response.body() != null) {
                cartDao.clearCart()
                loadUserProfile()
                Result.success("Compra realizada con éxito.")
            } else {
                val error = response.errorBody()?.string() ?: "Error en el pago"
                Result.failure(Exception(error))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    private fun compressAndCopyImage(contentUri: Uri): String {
        val contentResolver = context.contentResolver

        val options = BitmapFactory.Options().apply {
            inJustDecodeBounds = true
        }
        contentResolver.openInputStream(contentUri)?.use { stream ->
            BitmapFactory.decodeStream(stream, null, options)
        }

        options.inSampleSize = calculateInSampleSize(options, 800, 800)
        options.inJustDecodeBounds = false

        val bitmap = contentResolver.openInputStream(contentUri)?.use { stream ->
            BitmapFactory.decodeStream(stream, null, options)
        } ?: throw Exception("No se pudo procesar la imagen")

        val file = File(context.cacheDir, "temp_profile_upload.jpg")
        FileOutputStream(file).use { out ->
            bitmap.compress(Bitmap.CompressFormat.JPEG, 60, out)
        }

        bitmap.recycle()

        return file.absolutePath
    }

    private fun calculateInSampleSize(options: BitmapFactory.Options, reqWidth: Int, reqHeight: Int): Int {
        val (height: Int, width: Int) = options.run { outHeight to outWidth }
        var inSampleSize = 1

        if (height > reqHeight || width > reqWidth) {
            val halfHeight: Int = height / 2
            val halfWidth: Int = width / 2

            while ((halfHeight / inSampleSize) >= reqHeight && (halfWidth / inSampleSize) >= reqWidth) {
                inSampleSize *= 2
            }
        }
        return inSampleSize
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

            val filePath = compressAndCopyImage(uri)
            val file = File(filePath)

            val requestFile = file.asRequestBody("image/jpeg".toMediaTypeOrNull())
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
                val errorBodyStr = response.errorBody()?.string()
                val errorMsg = if (!errorBodyStr.isNullOrBlank()) {
                    errorBodyStr
                } else {
                    "Error del servidor: ${response.code()} ${response.message()}"
                }
                Result.failure(Exception(errorMsg))
            }

        } catch (e: Exception) {
            e.printStackTrace()
            Result.failure(Exception(e.message ?: "Error desconocido en repositorio"))
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
    suspend fun updateUserPassword(o: String, n: String): Result<Unit> = Result.success(Unit)
}