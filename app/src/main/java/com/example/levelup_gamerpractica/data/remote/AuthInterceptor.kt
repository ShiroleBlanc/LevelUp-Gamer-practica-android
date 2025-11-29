package com.example.levelup_gamerpractica.data.remote

import com.example.levelup_gamerpractica.utils.TokenManager
import okhttp3.Interceptor
import okhttp3.Response

class AuthInterceptor : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val originalRequest = chain.request()
        val token = TokenManager.getToken()
        val url = originalRequest.url.toString()

        if (url.contains("/auth/login") || url.contains("/auth/register")) {
            return chain.proceed(originalRequest)
        }

        if (token == null) {
            return chain.proceed(originalRequest)
        }

        val newRequest = originalRequest.newBuilder()
            .addHeader("Authorization", "Bearer $token")
            .build()

        return chain.proceed(newRequest)
    }
}