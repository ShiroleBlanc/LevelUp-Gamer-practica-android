package com.example.levelup_gamerpractica.utils

/**
 * Singleton para compartir el token entre la UI y Retrofit
 */
object TokenManager {
    private var token: String? = null

    fun setToken(newToken: String?) {
        token = newToken
    }

    fun getToken(): String? {
        return token
    }
}