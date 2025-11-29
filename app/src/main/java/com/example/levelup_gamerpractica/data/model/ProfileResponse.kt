package com.example.levelup_gamerpractica.data.model

data class ProfileResponse(
    val id: Long,
    val username: String,
    val email: String,
    val userRole: String,
    val pointsBalance: Int,
    val userLevel: Int,
    val totalPointsEarned: Int,
    val profilePictureUrl: String?
)