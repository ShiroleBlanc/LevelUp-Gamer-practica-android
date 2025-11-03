package com.example.levelup_gamerpractica.data.local

import android.content.Context // <-- IMPORTANTE
import android.net.Uri // <-- IMPORTANTE
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
import java.io.File // <-- IMPORTANTE
import java.io.FileOutputStream // <-- IMPORTANTE
import java.util.UUID // <-- IMPORTANTE

// Repositorio: Único punto de acceso a los datos
// --- 1. AÑADIR 'context' AL CONSTRUCTOR ---
class AppRepository(
    private val database: AppDatabase,
    private val context: Context // <-- AÑADIDO
) {

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
            // Validar si el email ya existe
            if (database.userDao().getUserByEmail(user.email) != null) {
                return@withContext Result.failure(Exception("El correo ya está registrado."))
            }
            // Validar si el nombre de usuario ya existe
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
        clearCart() // Opcional: decide si el carrito debe borrarse al salir
    }

    // --- 2. FUNCIÓN DE COPIAR IMAGEN (NUEVA) ---
    /**
     * Copia una imagen desde una URI de contenido a un archivo permanente
     * en el almacenamiento interno de la app.
     * Devuelve la ruta (path) del nuevo archivo.
     */
    private fun copyImageToInternalStorage(contentUri: Uri): String {
        val inputStream = context.contentResolver.openInputStream(contentUri)
        // Crear un nombre de archivo único
        val fileName = "profile_${UUID.randomUUID()}.jpg"
        // Crear el archivo en el directorio interno de la app
        val file = File(context.filesDir, fileName)
        val outputStream = FileOutputStream(file)

        inputStream.use { input ->
            outputStream.use { output ->
                input?.copyTo(output)
            }
        }
        // Devolvemos la ruta absoluta del archivo que acabamos de guardar
        return file.absolutePath
    }


    // --- 3. FUNCIÓN 'updateProfilePicture' MODIFICADA ---
    /**
     * Actualiza la foto de perfil del usuario logueado.
     * Copia la imagen al almacenamiento interno y guarda la RUTA del archivo.
     */
    suspend fun updateProfilePicture(contentUriString: String?): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val user = _currentUser.value ?: throw Exception("No hay usuario logueado")

            // Si la URI es nula (ej. "Quitar foto"), guarda null
            if (contentUriString == null) {
                database.userDao().updateProfilePicture(user.id, null)
                _currentUser.value = user.copy(profilePictureUri = null)
                return@withContext Result.success(Unit)
            }

            // 1. Copiar la imagen de la URI temporal a un archivo permanente
            val permanentFilePath = copyImageToInternalStorage(Uri.parse(contentUriString))

            // 2. Actualizar la base de datos con la RUTA del nuevo archivo
            database.userDao().updateProfilePicture(user.id, permanentFilePath)

            // 3. Actualizar el StateFlow en memoria
            _currentUser.value = user.copy(profilePictureUri = permanentFilePath)

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // --- 4. FUNCIONES DE ACTUALIZACIÓN DE PERFIL (MODIFICADAS) ---
    /**
     * Actualiza el nombre de usuario y/o email del usuario actual.
     */
    suspend fun updateUserDetails(newUsername: String?, newEmail: String?): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val user = _currentUser.value ?: throw Exception("No hay usuario logueado")

            // Usar los valores nuevos solo si no son nulos o vacíos, sino mantener los antiguos
            val finalUsername = newUsername?.takeIf { it.isNotBlank() } ?: user.username
            val finalEmail = newEmail?.takeIf { it.isNotBlank() } ?: user.email

            // Validar si el nuevo email ya existe (si cambió)
            if (finalEmail != user.email) {
                val emailCheck = database.userDao().getUserByEmail(finalEmail)
                if (emailCheck != null && emailCheck.id != user.id) {
                    return@withContext Result.failure(Exception("El nuevo correo ya está en uso."))
                }
            }
            // Validar si el nuevo username ya existe (si cambió)
            if (finalUsername != user.username) {
                val usernameCheck = database.userDao().getUserByUsername(finalUsername)
                if (usernameCheck != null && usernameCheck.id != user.id) {
                    return@withContext Result.failure(Exception("El nuevo nombre de usuario ya está en uso."))
                }
            }

            val updatedUser = user.copy(username = finalUsername, email = finalEmail)

            // --- INICIO DE LA CORRECCIÓN ---
            // 2. Actualizar la base de datos
            // database.userDao().updateUser(updatedUser) // <- Esta función no existe en tu DAO
            // Usamos las funciones que sí existen:
            database.userDao().updateUsername(user.id, finalUsername)
            database.userDao().updateUserEmail(user.id, finalEmail)
            // --- FIN DE LA CORRECCIÓN ---

            // 3. Actualizar el StateFlow
            _currentUser.value = updatedUser
            Result.success(Unit)

        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Actualiza la contraseña del usuario actual tras verificar la antigua.
     */
    suspend fun updateUserPassword(oldPasswordHash: String, newPasswordHash: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val user = _currentUser.value ?: throw Exception("No hay usuario logueado")

            // 1. Verificar la contraseña actual
            if (user.passwordHash != oldPasswordHash) {
                return@withContext Result.failure(Exception("La contraseña actual es incorrecta."))
            }

            // 2. Actualizar la base de datos
            database.userDao().updatePassword(user.id, newPasswordHash)

            // 3. Actualizar el StateFlow
            _currentUser.value = user.copy(passwordHash = newPasswordHash)
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

    // --- INICIO DE LA CORRECCIÓN ---
    // Esta función estaba rota por mi respuesta anterior
    suspend fun clearCart() = withContext(Dispatchers.IO) {
        database.cartDao().clearCart()
    }
    // --- FIN DE LA CORRECCIÓN ---
}