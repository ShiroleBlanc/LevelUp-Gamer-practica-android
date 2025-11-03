package com.example.levelup_gamerpractica.data.local.dao

import androidx.room.*
import com.example.levelup_gamerpractica.data.local.entities.User

@Dao
interface UserDao {
    @Insert(onConflict = OnConflictStrategy.ABORT) // No permite emails duplicados
    suspend fun insertUser(user: User)

    @Query("SELECT * FROM users WHERE email = :email LIMIT 1")
    suspend fun getUserByEmail(email: String): User?
}