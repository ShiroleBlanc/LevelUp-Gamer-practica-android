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
import java.text.NumberFormat
import java.util.Locale
import java.util.UUID

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

    /**
     * Registro: Envía los datos al backend.
     */
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

    /**
     * Login:
     * 1. Envía credenciales.
     * 2. Recibe y guarda Token.
     * 3. Pide el perfil completo del usuario (con puntos y rol).
     */
    suspend fun loginUserApi(username: String, password: String): Result<User> = withContext(Dispatchers.IO) {
        try {
            // 1. Login para obtener Token
            val request = LoginRequest(username, password)
            val loginResponse = apiService.login(request)

            if (loginResponse.isSuccessful && loginResponse.body() != null) {
                val token = loginResponse.body()!!.token

                // Guardar token
                sessionManager.saveAuthToken(token)
                TokenManager.setToken(token)

                // 2. Obtener perfil real del usuario
                val profileResponse = apiService.getUserProfile()

                if (profileResponse.isSuccessful && profileResponse.body() != null) {
                    val profile = profileResponse.body()!!

                    // Mapear respuesta del backend a entidad local User
                    val user = User(
                        id = profile.id,
                        username = profile.username,
                        email = profile.email,
                        userRole = profile.userRole,
                        pointsBalance = profile.pointsBalance,
                        userLevel = profile.userLevel,
                        profilePictureUrl = profile.profilePictureUrl
                    )

                    // Actualizar estado y caché local
                    _currentUser.value = user
                    userDao.insertUser(user)

                    Result.success(user)
                } else {
                    // Si falla el perfil pero el login fue bueno, creamos un usuario temporal
                    // para no bloquear el acceso, aunque no se verán los puntos.
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

    /**
     * Cargar Perfil (Auto-Login):
     * Se usa cuando abres la app y ya tienes un token guardado.
     */
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

    /**
     * Logout: Limpia tokens y datos locales.
     */
    suspend fun logoutUser() = withContext(Dispatchers.IO) {
        sessionManager.logout()        // Borrar de disco
        TokenManager.setToken(null)    // Borrar de memoria
        _currentUser.value = null      // Limpiar estado UI
        clearCart()                    // Vaciar carrito local
        userDao.deleteAllUsers()       // Limpiar caché de usuarios
    }

    // ========================================================================
    // GESTIÓN DE PERFIL (Lógica Local por ahora)
    // ========================================================================

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
                userDao.updateProfilePicture(user.id, null)
                _currentUser.value = user.copy(profilePictureUrl = null)
                return@withContext Result.success(Unit)
            }

            val permanentFilePath = copyImageToInternalStorage(Uri.parse(contentUriString))

            // Nota: Esto solo actualiza la foto LOCAL en el celular.
            // Para subirla al servidor, necesitarías implementar 'apiService.uploadPicture'
            userDao.updateProfilePicture(user.id, permanentFilePath)
            _currentUser.value = user.copy(profilePictureUrl = permanentFilePath)

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun updateUserDetails(newUsername: String?, newEmail: String?): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val user = _currentUser.value ?: throw Exception("No hay usuario logueado")
            val finalUsername = newUsername?.takeIf { it.isNotBlank() } ?: user.username
            val finalEmail = newEmail?.takeIf { it.isNotBlank() } ?: user.email

            userDao.updateUsername(user.id, finalUsername)
            userDao.updateUserEmail(user.id, finalEmail)

            _currentUser.value = user.copy(username = finalUsername, email = finalEmail)

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun updateUserPassword(oldPasswordHash: String, newPasswordHash: String): Result<Unit> = withContext(Dispatchers.IO) {
        Result.success(Unit)
    }


    // ========================================================================
    // PRODUCTOS (Desde el Backend)
    // ========================================================================

    private fun mapDtoToEntity(dto: ProductNetworkDto): Product {
        val formatClp = NumberFormat.getCurrencyInstance(Locale("es", "CL")).apply {
            maximumFractionDigits = 0
        }
        val formattedPrice = formatClp.format(dto.price)

        return Product(
            id = dto.id.toInt(),
            name = dto.name,
            price = formattedPrice,
            category = dto.category,
            image = dto.image,
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
    // CARRITO (Local)
    // ========================================================================

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