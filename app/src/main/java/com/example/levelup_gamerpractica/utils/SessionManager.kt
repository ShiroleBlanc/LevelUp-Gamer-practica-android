package com.example.levelup_gamerpractica.utils

import android.content.Context
import android.content.SharedPreferences

class SessionManager(context: Context) {
    // Asegúrate de que este nombre "LevelUpPrefs" sea el mismo siempre
    private var prefs: SharedPreferences = context.getSharedPreferences("LevelUpPrefs", Context.MODE_PRIVATE)

    companion object {
        const val USER_TOKEN = "user_token"
    }

    fun saveAuthToken(token: String) {
        val editor = prefs.edit()
        editor.putString(USER_TOKEN, token)
        editor.apply() // Guarda en disco
    }

    fun fetchAuthToken(): String? {
        return prefs.getString(USER_TOKEN, null)
    }

    fun logout() {
        val editor = prefs.edit()
        editor.remove(USER_TOKEN)
        editor.apply()
    }

    fun isLoggedIn(): Boolean {
        return fetchAuthToken() != null
    }
}