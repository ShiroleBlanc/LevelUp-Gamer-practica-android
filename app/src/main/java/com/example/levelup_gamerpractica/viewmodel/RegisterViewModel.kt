package com.example.levelup_gamerpractica.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.levelup_gamerpractica.data.local.AppRepository
import com.example.levelup_gamerpractica.data.local.entities.User
import com.example.levelup_gamerpractica.utils.PasswordHasher
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.Period
import java.time.format.DateTimeParseException

// Estados posibles de la UI de Registro
sealed class RegisterUiState {
    object Idle : RegisterUiState()
    object Loading : RegisterUiState()
    object Success : RegisterUiState()
    data class Error(val message: String) : RegisterUiState()
}

class RegisterViewModel(private val repository: AppRepository) : ViewModel() {

    // --- StateFlows para los campos del formulario ---
    private val _username = MutableStateFlow("")
    val username = _username.asStateFlow()

    private val _email = MutableStateFlow("")
    val email = _email.asStateFlow()

    private val _password = MutableStateFlow("")
    val password = _password.asStateFlow()

    private val _confirmPassword = MutableStateFlow("")
    val confirmPassword = _confirmPassword.asStateFlow()

    private val _birthDate = MutableStateFlow<LocalDate?>(null) // Guardar como LocalDate
    val birthDate = _birthDate.asStateFlow()

    // --- StateFlow para el estado general de la UI ---
    private val _uiState = MutableStateFlow<RegisterUiState>(RegisterUiState.Idle)
    val uiState = _uiState.asStateFlow()

    // --- StateFlows para errores de validación por campo ---
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


    // --- Funciones para actualizar los campos ---
    fun onUsernameChange(value: String) { _username.value = value; _usernameError.value = null }
    fun onEmailChange(value: String) { _email.value = value; _emailError.value = null }
    fun onPasswordChange(value: String) { _password.value = value; _passwordError.value = null }
    fun onConfirmPasswordChange(value: String) { _confirmPassword.value = value; _confirmPasswordError.value = null }
    fun onBirthDateChange(date: LocalDate?) { _birthDate.value = date; _birthDateError.value = null }


    // --- Función de Registro ---
    fun register() {
        if (!validateInputs()) {
            return // Detiene si hay errores de validación
        }

        viewModelScope.launch {
            _uiState.value = RegisterUiState.Loading
            // Hashea la contraseña antes de guardarla
            val passwordHash = PasswordHasher.hash(password.value)

            val newUser = User(
                username = username.value.trim(),
                email = email.value.trim().lowercase(),
                passwordHash = passwordHash
            )

            val result = repository.registerUser(newUser)
            _uiState.value = result.fold(
                onSuccess = { RegisterUiState.Success },
                onFailure = { RegisterUiState.Error(it.message ?: "Error al registrar") }
            )
        }
    }

    // --- Validación de Entradas ---
    private fun validateInputs(): Boolean {
        var isValid = true
        // Limpia errores previos
        _usernameError.value = null
        _emailError.value = null
        _passwordError.value = null
        _confirmPasswordError.value = null
        _birthDateError.value = null

        // Username
        if (username.value.trim().length < 4) {
            _usernameError.value = "Mínimo 4 caracteres."
            isValid = false
        } else if (username.value.trim().length > 12) {
            _usernameError.value = "Máximo 12 caracteres."
            isValid = false
        }

        // Email
        if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email.value.trim()).matches()) {
            _emailError.value = "Correo inválido."
            isValid = false
        }

        // Password
        if (password.value.length < 6) {
            _passwordError.value = "Mínimo 6 caracteres."
            isValid = false
        } else if (password.value.length > 12) {
            _passwordError.value = "Máximo 12 caracteres."
            isValid = false
        }

        // Confirm Password
        if (password.value != confirmPassword.value) {
            _confirmPasswordError.value = "Las contraseñas no coinciden."
            isValid = false
        }

        // Birth Date (Mayor de 18)
        val dob = birthDate.value
        if (dob == null) {
            _birthDateError.value = "Selecciona tu fecha de nacimiento."
            isValid = false
        } else {
            val age = Period.between(dob, LocalDate.now()).years
            if (age < 18) {
                _birthDateError.value = "Debes ser mayor de 18 años."
                isValid = false
            }
        }

        return isValid
    }

    // Llama a esto para resetear el estado de error/éxito después de mostrar un mensaje
    fun consumeUiState() {
        _uiState.value = RegisterUiState.Idle
    }
}

// Factory para el ViewModel
class RegisterViewModelFactory(private val repository: AppRepository) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(RegisterViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return RegisterViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}