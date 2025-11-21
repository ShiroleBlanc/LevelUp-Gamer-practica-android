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
     * Registro: Envía los datos al backend (Spring Boot).
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
     * Login: Envía credenciales, recibe Token y lo guarda.
     */
    suspend fun loginUserApi(username: String, password: String): Result<User> = withContext(Dispatchers.IO) {
        try {
            val request = LoginRequest(username, password)
            val response = apiService.login(request)

            if (response.isSuccessful && response.body() != null) {
                val token = response.body()!!.token

                // 1. Guardar token en Disco (para cuando cierres la app)
                sessionManager.saveAuthToken(token)

                // 2. Guardar token en Memoria (para usarlo YA en las siguientes peticiones)
                TokenManager.setToken(token)

                // 3. Crear usuario local temporal
                // (En una app completa, aquí llamaríamos a 'apiService.getMyProfile()' para llenar los datos reales)
                val user = User(
                    id = 0, // ID temporal hasta que carguemos el perfil real
                    username = username,
                    email = "",
                    userRole = "ROLE_USER",
                    pointsBalance = 0,
                    userLevel = 1
                )

                // Actualizar el estado de la UI
                _currentUser.value = user

                // Guardar usuario básico en Room (Caché)
                userDao.insertUser(user)

                Result.success(user)
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
     * Logout: Limpia tokens y datos locales.
     */
    suspend fun logoutUser() = withContext(Dispatchers.IO) {
        sessionManager.logout()        // Borrar de SharedPreferences
        TokenManager.setToken(null)    // Borrar de Memoria
        _currentUser.value = null      // Limpiar estado UI
        clearCart()                    // Vaciar carrito
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
                // Eliminar foto: Usamos el nombre correcto 'profilePictureUrl' en el DAO
                userDao.updateProfilePicture(user.id, null)
                _currentUser.value = user.copy(profilePictureUrl = null)
                return@withContext Result.success(Unit)
            }

            // Guardar nueva foto en almacenamiento interno del celular
            val permanentFilePath = copyImageToInternalStorage(Uri.parse(contentUriString))

            // Actualizar en Room (asegúrate que tu UserDao use profilePictureUrl)
            userDao.updateProfilePicture(user.id, permanentFilePath)

            // Actualizar estado en UI
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

            // Actualizar en Room
            userDao.updateUsername(user.id, finalUsername)
            userDao.updateUserEmail(user.id, finalEmail)

            // Actualizar en UI
            _currentUser.value = user.copy(username = finalUsername, email = finalEmail)

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // Esta función es solo local, la contraseña real se cambia en el backend
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
                // 1. Obtener de la API
                val productDtoList = apiService.getAllProducts()

                // 2. Convertir a Entidades Locales
                val productEntityList = productDtoList.map { mapDtoToEntity(it) }

                // 3. Actualizar Caché (Room)
                productDao.deleteAll() // Asegúrate de tener este método en tu ProductDao
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