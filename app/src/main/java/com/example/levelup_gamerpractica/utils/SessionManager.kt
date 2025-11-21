package com.example.levelup_gamerpractica.utils

import android.content.Context
import android.content.SharedPreferences

/**
 * Clase para manejar el guardado del Token JWT en el celular.
 * Funciona similar al localStorage de la web.
 */
class SessionManager(context: Context) {
    private var prefs: SharedPreferences = context.getSharedPreferences("LevelUpPrefs", Context.MODE_PRIVATE)

    companion object {
        const val USER_TOKEN = "user_token"
    }

    /**
     * Guarda el token que recibimos del login.
     */
    fun saveAuthToken(token: String) {
        val editor = prefs.edit()
        editor.putString(USER_TOKEN, token)
        editor.apply()
    }

    /**
     * Recupera el token guardado.
     * Retorna null si no hay ninguno.
     */
    fun fetchAuthToken(): String? {
        return prefs.getString(USER_TOKEN, null)
    }

    /**
     * Cierra sesión borrando el token.
     */
    fun logout() {
        val editor = prefs.edit()
        editor.remove(USER_TOKEN)
        editor.apply()
    }

    /**
     * Verifica rápidamente si el usuario está logueado.
     */
    fun isLoggedIn(): Boolean {
        return fetchAuthToken() != null
    }
}