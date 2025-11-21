package com.example.levelup_gamerpractica.data.local.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "users")
data class User(
    // Usamos el ID del backend (Long), no autogenerado por Room
    @PrimaryKey(autoGenerate = false)
    val id: Long = 0,

    val username: String,
    val email: String,

    // --- CAMPO ELIMINADO: passwordHash ---
    // (La seguridad la maneja el Token JWT, no guardamos claves aquí)

    // URL de la foto que viene del servidor (http://10.0.2.2:8081/uploads/...)
    val profilePictureUrl: String? = null,

    // --- NUEVOS CAMPOS DE GAMIFICACIÓN ---
    val userRole: String = "ROLE_USER",
    val pointsBalance: Int = 0,
    val userLevel: Int = 1
)