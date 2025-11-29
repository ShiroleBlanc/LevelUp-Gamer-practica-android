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

        // Obtenemos el repositorio desde la Application
        val repository = (application as LevelUpGamerApplication).repository

        // CAMBIO 1: Definimos el destino inicial SIEMPRE como el Catálogo.
        // Ya no importa si hay token o no para la navegación inicial.
        val startDestination = Routes.CATALOG

        // Lógica de restauración de sesión (en segundo plano)
        if (savedToken != null) {
            Log.d("DEBUG_SESION", "Token encontrado. Restaurando sesión...")

            // 1. Ponemos el token en memoria para que las llamadas API funcionen
            TokenManager.setToken(savedToken)

            // 2. Validamos el token intentando cargar el perfil
            lifecycleScope.launch {
                // Esto actualizará el 'currentUser' en el Repositorio,
                // lo que a su vez actualizará la UI (foto de perfil, menú) reactivamente.
                val success = repository.loadUserProfile()

                if (success) {
                    Log.d("DEBUG_SESION", "Perfil cargado. Usuario validado.")
                } else {
                    Log.e("DEBUG_SESION", "Token expirado o inválido. Limpiando sesión.")

                    // Si el token no sirve, simplemente limpiamos.
                    // Como ya estamos en el Catálogo, la UI se actualizará sola
                    // (se ocultará la foto de perfil y aparecerá "Iniciar Sesión" en el menú).
                    sessionManager.logout()
                    TokenManager.setToken(null)

                    // Nota: Ya no es necesario reiniciar la Activity (finish/startActivity)
                    // porque la UI es reactiva gracias a los Flows del ViewModel.
                }
            }
        }

        setContent {
            LevelUpGamerPracticaTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    // Siempre inicia en Catálogo
                    AppNavigation(startDestination = startDestination)
                }
            }
        }
    }
}