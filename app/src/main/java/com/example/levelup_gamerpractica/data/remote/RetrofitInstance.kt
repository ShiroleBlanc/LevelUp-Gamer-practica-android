package com.example.levelup_gamerpractica.data.remote

import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

object RetrofitInstance {

    // AL USAR CABLE USB Y 'adb reverse':
    // Usamos 'localhost' porque el comando redirige el puerto 8081 del celular al PC.
    private const val BASE_URL = "http://localhost:8081/"

    // 1. Configurar el cliente OkHttp con el Interceptor
    // Esto inyecta el token automáticamente si existe en TokenManager
    private val client = OkHttpClient.Builder()
        .addInterceptor(AuthInterceptor())
        .build()

    val api: ApiService by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(client) // <-- 2. Asignar el cliente aquí
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(ApiService::class.java)
    }
}