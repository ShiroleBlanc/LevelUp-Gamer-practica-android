package com.example.levelup_gamerpractica.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.levelup_gamerpractica.data.local.AppRepository
import com.example.levelup_gamerpractica.data.model.RegisterRequest
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.Period // <--- IMPORTANTE: Necesario para calcular la edad
import java.time.format.DateTimeFormatter

sealed class RegisterUiState {
    object Idle : RegisterUiState()
    object Loading : RegisterUiState()
    object Success : RegisterUiState()
    data class Error(val message: String) : RegisterUiState()
}

class RegisterViewModel(private val repository: AppRepository) : ViewModel() {

    // --- ESTADOS DE ENTRADA ---
    private val _username = MutableStateFlow("")
    val username = _username.asStateFlow()

    private val _email = MutableStateFlow("")
    val email = _email.asStateFlow()

    private val _password = MutableStateFlow("")
    val password = _password.asStateFlow()

    private val _confirmPassword = MutableStateFlow("")
    val confirmPassword = _confirmPassword.asStateFlow()

    private val _birthDate = MutableStateFlow<LocalDate?>(null)
    val birthDate = _birthDate.asStateFlow()

    // --- ESTADOS DE UI ---
    private val _uiState = MutableStateFlow<RegisterUiState>(RegisterUiState.Idle)
    val uiState = _uiState.asStateFlow()

    // --- ESTADOS DE ERROR ---
    private val _usernameError = MutableStateFlow<String?>(null)
    val usernameError = _usernameError.asStateFlow()
    private val _emailError = MutableStateFlow<String?>(null)
    val emailError = _emailError.asStateFlow()
    private val _passwordError = MutableStateFlow<String?>(null)
    val passwordError = _passwordError.asStateFlow()
    private val _confirmPasswordError = MutableStateFlow<String?>(null)
    val confirmPasswordError = _confirmPasswordError.asStateFlow()
    private val _birthDateError = MutableStateFlow<String?>(null)
    val birthDateError = _birthDateError.asStateFlow()

    // --- SETTERS ---
    fun onUsernameChange(value: String) { _username.value = value; _usernameError.value = null }
    fun onEmailChange(value: String) { _email.value = value; _emailError.value = null }
    fun onPasswordChange(value: String) { _password.value = value; _passwordError.value = null }
    fun onConfirmPasswordChange(value: String) { _confirmPassword.value = value; _confirmPasswordError.value = null }
    fun onBirthDateChange(date: LocalDate?) { _birthDate.value = date; _birthDateError.value = null }

    // --- LÓGICA DE REGISTRO ---
    fun register() {
        if (!validateInputs()) {
            return
        }

        viewModelScope.launch {
            _uiState.value = RegisterUiState.Loading

            try {
                // 1. Formatear la fecha a String "YYYY-MM-DD"
                // Usamos !! porque validateInputs ya aseguró que birthDate no es null
                val dateString = birthDate.value!!.format(DateTimeFormatter.ISO_LOCAL_DATE)

                // 2. Crear el objeto DTO
                val request = RegisterRequest(
                    username = username.value.trim(),
                    email = email.value.trim(),
                    password = password.value,
                    dateOfBirth = dateString
                )

                // 3. Llamar al repositorio
                val result = repository.registerUserApi(request)

                // 4. Manejar la respuesta
                _uiState.value = result.fold(
                    onSuccess = { RegisterUiState.Success },
                    onFailure = { error ->
                        RegisterUiState.Error(error.message ?: "Error desconocido al registrarse")
                    }
                )
            } catch (e: Exception) {
                _uiState.value = RegisterUiState.Error(e.message ?: "Error inesperado")
            }
        }
    }

    private fun validateInputs(): Boolean {
        var isValid = true

        // Reiniciar errores visuales antes de validar
        _usernameError.value = null
        _emailError.value = null
        _passwordError.value = null
        _confirmPasswordError.value = null
        _birthDateError.value = null

        // 1. Validar Username
        if (username.value.isBlank() || username.value.length < 4) {
            _usernameError.value = "Mínimo 4 caracteres."
            isValid = false
        }

        // 2. Validar Email
        val emailRegex = "^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+$".toRegex()
        if (!email.value.matches(emailRegex)) {
            _emailError.value = "Correo inválido."
            isValid = false
        }

        // 3. Validar Password
        if (password.value.length < 6) {
            _passwordError.value = "Mínimo 6 caracteres."
            isValid = false
        }

        // 4. Validar Confirm Password
        if (password.value != confirmPassword.value) {
            _confirmPasswordError.value = "Las contraseñas no coinciden."
            isValid = false
        }

        // 5. Validar Fecha y Edad (AQUÍ ESTÁ EL CAMBIO PRINCIPAL)
        if (birthDate.value == null) {
            _birthDateError.value = "Selecciona tu fecha de nacimiento."
            isValid = false
        } else {
            // Calculamos la edad usando Period
            val today = LocalDate.now()
            val age = Period.between(birthDate.value, today).years

            if (age < 18) {
                _birthDateError.value = "Debes ser mayor de 18 años."
                isValid = false
            }
        }

        return isValid
    }

    fun consumeUiState() {
        _uiState.value = RegisterUiState.Idle
    }
}

class RegisterViewModelFactory(private val repository: AppRepository) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(RegisterViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return RegisterViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}