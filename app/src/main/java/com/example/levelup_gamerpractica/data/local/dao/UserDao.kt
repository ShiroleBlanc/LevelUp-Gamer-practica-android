package com.example.levelup_gamerpractica.data.local.dao

import androidx.room.*
import com.example.levelup_gamerpractica.data.local.entities.User
import kotlinx.coroutines.flow.Flow

@Dao
interface UserDao {
    @Insert(onConflict = OnConflictStrategy.ABORT) // No permite emails duplicados
    suspend fun insertUser(user: User)

    @Query("SELECT * FROM users WHERE email = :email LIMIT 1")
    suspend fun getUserByEmail(email: String): User?

    // --- FUNCIÓN AÑADIDA ---
    // Obtiene un usuario por su ID (asumiendo que User tiene @PrimaryKey val id: Int)
    @Query("SELECT * FROM users WHERE id = :id LIMIT 1")
    suspend fun getUserById(id: Int): User?

    // --- FUNCIÓN AÑADIDA ---
    // Actualiza la URI de la foto de perfil para un usuario específico por su ID
    @Query("UPDATE users SET profilePictureUri = :uri WHERE id = :userId")
    suspend fun updateProfilePicture(userId: Int, uri: String?)
}