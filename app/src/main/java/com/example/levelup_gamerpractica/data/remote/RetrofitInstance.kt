package com.example.levelup_gamerpractica.data.remote

import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

object RetrofitInstance {

    // CAMBIA ESTO por la IP de tu PC donde corre el backend.
    // (No uses 'localhost' o '127.0.0.1', el emulador no lo entenderá)
    // Si usas el emulador de Android Studio, la IP suele ser 10.0.2.2
    private const val BASE_URL = "http://localhost:8081/" // ¡Asegúrate que el puerto sea 8081!

    val api: ApiService by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(ApiService::class.java)
    }
}