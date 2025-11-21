package com.example.levelup_gamerpractica.data.local.dao

import androidx.room.*
import com.example.levelup_gamerpractica.data.local.entities.User

@Dao
interface UserDao {

    // Insertar o actualizar usuario (Login/Registro)
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUser(user: User)

    // Buscar por email
    @Query("SELECT * FROM users WHERE email = :email LIMIT 1")
    suspend fun getUserByEmail(email: String): User?

    // Buscar por username
    @Query("SELECT * FROM users WHERE username = :username LIMIT 1")
    suspend fun getUserByUsername(username: String): User?

    // --- AQUÍ ESTABA EL ERROR ---
    // Antes decía: "UPDATE users SET profilePictureUri = :imagePath..."
    // Ahora debe decir: "profilePictureUrl"
    @Query("UPDATE users SET profilePictureUrl = :imagePath WHERE id = :userId")
    suspend fun updateProfilePicture(userId: Long, imagePath: String?)

    // Actualizar contraseña (opcional, ya que el backend manda)
    // Asegúrate de que la columna en SQL coincida con la entidad (passwordHash ya no existe, recuerda)
    // Si eliminaste passwordHash de la entidad, borra o comenta este método.
    // @Query("UPDATE users SET passwordHash = :newPassword WHERE id = :userId")
    // suspend fun updatePassword(userId: Long, newPassword: String)

    @Query("UPDATE users SET username = :newUsername WHERE id = :userId")
    suspend fun updateUsername(userId: Long, newUsername: String)

    @Query("UPDATE users SET email = :newEmail WHERE id = :userId")
    suspend fun updateUserEmail(userId: Long, newEmail: String)

    @Query("DELETE FROM users")
    suspend fun deleteAllUsers()
}