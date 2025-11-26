package com.example.levelup_gamerpractica.data.remote

import com.example.levelup_gamerpractica.utils.TokenManager
import okhttp3.Interceptor
import okhttp3.Response

class AuthInterceptor : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val originalRequest = chain.request()
        val token = TokenManager.getToken()
        val url = originalRequest.url.toString()

        // --- CORRECCIÓN CRÍTICA ---
        // Si la URL es de login o registro, NO adjuntamos el token.
        // Esto evita que un token viejo o roto bloquee el acceso a estas rutas públicas.
        if (url.contains("/auth/login") || url.contains("/auth/register")) {
            return chain.proceed(originalRequest)
        }

        // Si no hay token guardado, seguimos normal
        if (token == null) {
            return chain.proceed(originalRequest)
        }

        // Solo si es una ruta protegida y tenemos token, lo añadimos
        val newRequest = originalRequest.newBuilder()
            .addHeader("Authorization", "Bearer $token")
            .build()

        return chain.proceed(newRequest)
    }
}