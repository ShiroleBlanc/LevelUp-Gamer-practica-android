package com.example.levelup_gamerpractica.data.remote

import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

object RetrofitInstance {
    //ir al cmd(windows + r) y escribir "ipconfig")
    //buscar la ipv4 de la conexion de red. ej. Direccion IPv4: 192.168.1.87
    //pegar en BASE_URL el valor de la ipv4 "http://TU_DIRECCION_IPV4:8081/"
    const val BASE_URL = "http://192.168.1.87:8081/"

    private val client = OkHttpClient.Builder()
        .addInterceptor(AuthInterceptor())
        .build()

    val api: ApiService by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(ApiService::class.java)
    }
}