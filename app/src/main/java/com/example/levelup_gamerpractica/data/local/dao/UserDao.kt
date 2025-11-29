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

    @Query("UPDATE users SET profilePictureUrl = :imagePath WHERE id = :userId")
    suspend fun updateProfilePicture(userId: Long, imagePath: String?)


    @Query("UPDATE users SET username = :newUsername WHERE id = :userId")
    suspend fun updateUsername(userId: Long, newUsername: String)

    @Query("UPDATE users SET email = :newEmail WHERE id = :userId")
    suspend fun updateUserEmail(userId: Long, newEmail: String)

    @Query("DELETE FROM users")
    suspend fun deleteAllUsers()
}