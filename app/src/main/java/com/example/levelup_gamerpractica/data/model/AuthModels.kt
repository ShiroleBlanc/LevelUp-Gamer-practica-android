package com.example.levelup_gamerpractica.data.model

import com.google.gson.annotations.SerializedName

// Modelo para enviar datos al hacer LOGIN
data class LoginRequest(
    val username: String,
    val password: String
)

// Modelo para recibir la respuesta del LOGIN (el Token)
data class LoginResponse(
    val token: String
)

// Modelo para enviar datos al hacer REGISTRO
data class RegisterRequest(
    val username: String,
    val email: String,
    val password: String,
    @SerializedName("dateOfBirth") val dateOfBirth: String
)