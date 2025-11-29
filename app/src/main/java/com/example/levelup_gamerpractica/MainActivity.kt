package com.example.levelup_gamerpractica

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.lifecycle.lifecycleScope
import com.example.levelup_gamerpractica.data.local.LevelUpGamerApplication
import com.example.levelup_gamerpractica.navigation.AppNavigation
import com.example.levelup_gamerpractica.navigation.Routes
import com.example.levelup_gamerpractica.ui.theme.LevelUpGamerPracticaTheme
import com.example.levelup_gamerpractica.utils.SessionManager
import com.example.levelup_gamerpractica.utils.TokenManager
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val sessionManager = SessionManager(this)
        val savedToken = sessionManager.fetchAuthToken()

        val repository = (application as LevelUpGamerApplication).repository

        val startDestination = Routes.CATALOG

        if (savedToken != null) {
            Log.d("DEBUG_SESION", "Token encontrado. Restaurando sesión...")

            TokenManager.setToken(savedToken)

            lifecycleScope.launch {
                val success = repository.loadUserProfile()

                if (success) {
                    Log.d("DEBUG_SESION", "Perfil cargado. Usuario validado.")
                } else {
                    Log.e("DEBUG_SESION", "Token expirado o inválido. Limpiando sesión.")
                    sessionManager.logout()
                    TokenManager.setToken(null)
                }
            }
        }

        setContent {
            LevelUpGamerPracticaTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    AppNavigation(startDestination = startDestination)
                }
            }
        }
    }
}