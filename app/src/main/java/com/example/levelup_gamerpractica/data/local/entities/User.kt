package com.example.levelup_gamerpractica.data.local.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "users")
data class User(
    @PrimaryKey(autoGenerate = false)
    val id: Long = 0,
    val username: String,
    val email: String,
    val profilePictureUrl: String? = null,
    val userRole: String = "ROLE_USER",
    val pointsBalance: Int = 0,
    val userLevel: Int = 1
)