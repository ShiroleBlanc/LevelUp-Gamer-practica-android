package com.example.levelup_gamerpractica.data.model

data class ProfileResponse(
    val id: Long,
    val username: String,
    val email: String,
    val userRole: String,        // ROLE_USER o ROLE_DUOC
    val pointsBalance: Int,      // Puntos para gastar
    val userLevel: Int,          // Nivel actual
    val totalPointsEarned: Int,  // Puntos totales
    val profilePictureUrl: String?
)