package com.example.levelup_gamerpractica.data.remote

import com.example.levelup_gamerpractica.utils.TokenManager
import okhttp3.Interceptor
import okhttp3.Response

class AuthInterceptor : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val originalRequest = chain.request()
        val token = TokenManager.getToken()

        // Si no hay token, mandamos la petición tal cual (ej: Login/Registro)
        if (token == null) {
            return chain.proceed(originalRequest)
        }

        // Si hay token, lo añadimos al Header
        val newRequest = originalRequest.newBuilder()
            .addHeader("Authorization", "Bearer $token")
            .build()

        return chain.proceed(newRequest)
    }
}