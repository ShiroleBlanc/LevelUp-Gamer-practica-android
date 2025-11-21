package com.example.levelup_gamerpractica

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.lifecycle.lifecycleScope // <-- IMPORTANTE
import com.example.levelup_gamerpractica.data.local.LevelUpGamerApplication // <-- IMPORTANTE
import com.example.levelup_gamerpractica.navigation.AppNavigation
import com.example.levelup_gamerpractica.navigation.Routes
import com.example.levelup_gamerpractica.ui.theme.LevelUpGamerPracticaTheme
import com.example.levelup_gamerpractica.utils.SessionManager
import com.example.levelup_gamerpractica.utils.TokenManager
import kotlinx.coroutines.launch // <-- IMPORTANTE

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val sessionManager = SessionManager(this)
        val savedToken = sessionManager.fetchAuthToken()

        // Obtenemos el repositorio desde la Application
        val repository = (application as LevelUpGamerApplication).repository

        var startDestination = Routes.LOGIN

        if (savedToken != null) {
            Log.d("DEBUG_SESION", "Token encontrado. Restaurando sesión...")
            TokenManager.setToken(savedToken)
            startDestination = Routes.CATALOG

            // --- ¡AQUÍ ESTÁ LA MAGIA! ---
            // Lanzamos una tarea en segundo plano para cargar el perfil
            lifecycleScope.launch {
                val success = repository.loadUserProfile()
                if (success) {
                    Log.d("DEBUG_SESION", "Perfil de usuario recargado correctamente")
                } else {
                    Log.d("DEBUG_SESION", "Error al recargar perfil (Token expirado?)")
                    // Opcional: Si falla (ej. token viejo), podrías forzar logout aquí
                }
            }
            // -----------------------------
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