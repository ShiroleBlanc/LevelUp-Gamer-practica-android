package com.example.levelup_gamerpractica.ui.screens

import android.app.DatePickerDialog
import android.widget.DatePicker
import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.levelup_gamerpractica.data.local.LevelUpGamerApplication
import com.example.levelup_gamerpractica.viewmodel.RegisterUiState
import com.example.levelup_gamerpractica.viewmodel.RegisterViewModel
import com.example.levelup_gamerpractica.viewmodel.RegisterViewModelFactory
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Calendar

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RegisterScreen(
    modifier: Modifier = Modifier,
    onRegisterSuccess: () -> Unit,
    onNavigateBack: () -> Unit,
    registerViewModel: RegisterViewModel = viewModel(
        factory = RegisterViewModelFactory((LocalContext.current.applicationContext as LevelUpGamerApplication).repository)
    )
) {
    val username by registerViewModel.username.collectAsState()
    val email by registerViewModel.email.collectAsState()
    val password by registerViewModel.password.collectAsState()
    val confirmPassword by registerViewModel.confirmPassword.collectAsState()
    val birthDate by registerViewModel.birthDate.collectAsState()
    val uiState by registerViewModel.uiState.collectAsState()

    val usernameError by registerViewModel.usernameError.collectAsState()
    val emailError by registerViewModel.emailError.collectAsState()
    val passwordError by registerViewModel.passwordError.collectAsState()
    val confirmPasswordError by registerViewModel.confirmPasswordError.collectAsState()
    val birthDateError by registerViewModel.birthDateError.collectAsState()

    val context = LocalContext.current
    var passwordVisible by remember { mutableStateOf(false) }
    var confirmPasswordVisible by remember { mutableStateOf(false) }

    // Manejo de estado de UI (error/éxito)
    LaunchedEffect(uiState) {
        when (val state = uiState) {
            is RegisterUiState.Success -> {
                Toast.makeText(context, "Registro exitoso. Ahora inicia sesión.", Toast.LENGTH_LONG).show()
                onRegisterSuccess()
                registerViewModel.consumeUiState()
            }
            is RegisterUiState.Error -> {
                Toast.makeText(context, state.message, Toast.LENGTH_LONG).show()
                registerViewModel.consumeUiState()
            }
            else -> {}
        }
    }

    // --- Date Picker Dialog ---
    val calendar = Calendar.getInstance()
    val year = calendar.get(Calendar.YEAR)
    val month = calendar.get(Calendar.MONTH)
    val day = calendar.get(Calendar.DAY_OF_MONTH)

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
            .verticalScroll(rememberScrollState()), // Permite scroll si el teclado tapa
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {

        OutlinedTextField(
            value = username,
            onValueChange = registerViewModel::onUsernameChange,
            label = { Text("Nombre de Usuario") },
            leadingIcon = { Icon(Icons.Filled.Person, contentDescription = null) },
            isError = usernameError != null,
            supportingText = { if (usernameError != null) Text(usernameError!!) },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(8.dp))

        OutlinedTextField(
            value = email,
            onValueChange = registerViewModel::onEmailChange,
            label = { Text("Correo Electrónico") },
            leadingIcon = { Icon(Icons.Filled.Email, contentDescription = null) },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
            isError = emailError != null,
            supportingText = { if (emailError != null) Text(emailError!!) },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(8.dp))

        OutlinedTextField(
            value = password,
            onValueChange = registerViewModel::onPasswordChange,
            label = { Text("Contraseña") },
            leadingIcon = { Icon(Icons.Filled.Lock, contentDescription = null) },
            visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
            isError = passwordError != null,
            supportingText = { if (passwordError != null) Text(passwordError!!) },
            trailingIcon = {
                val image = if (passwordVisible) Icons.Filled.VisibilityOff else Icons.Filled.Visibility
                IconButton(onClick = {passwordVisible = !passwordVisible}){ Icon(image, null) }
            },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(8.dp))

        OutlinedTextField(
            value = confirmPassword,
            onValueChange = registerViewModel::onConfirmPasswordChange,
            label = { Text("Confirmar Contraseña") },
            leadingIcon = { Icon(Icons.Filled.Lock, contentDescription = null) },
            visualTransformation = if (confirmPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
            isError = confirmPasswordError != null,
            supportingText = { if (confirmPasswordError != null) Text(confirmPasswordError!!) },
            trailingIcon = {
                val image = if (confirmPasswordVisible) Icons.Filled.VisibilityOff else Icons.Filled.Visibility
                IconButton(onClick = {confirmPasswordVisible = !confirmPasswordVisible}){ Icon(image, null) }
            },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(8.dp))

        OutlinedTextField(
            value = birthDate?.format(DateTimeFormatter.ISO_LOCAL_DATE) ?: "",
            onValueChange = {}, // No editable
            label = { Text("Fecha de Nacimiento") },
            leadingIcon = { Icon(Icons.Filled.Cake, contentDescription = null) },
            isError = birthDateError != null,
            supportingText = { if (birthDateError != null) Text(birthDateError!!) },
            readOnly = true,
            modifier = Modifier.fillMaxWidth(),
            trailingIcon = {
                IconButton(onClick = {
                    val calendar = Calendar.getInstance()
                    val initialYear = birthDate?.year ?: calendar.get(Calendar.YEAR)
                    val initialMonth = birthDate?.monthValue?.minus(1) ?: calendar.get(Calendar.MONTH)
                    val initialDay = birthDate?.dayOfMonth ?: calendar.get(Calendar.DAY_OF_MONTH)

                    DatePickerDialog(
                        context,
                        { _: DatePicker, selectedYear: Int, selectedMonth: Int, selectedDayOfMonth: Int ->
                            registerViewModel.onBirthDateChange(
                                LocalDate.of(selectedYear, selectedMonth + 1, selectedDayOfMonth)
                            )
                        }, initialYear, initialMonth, initialDay
                    ).apply {
                        datePicker.maxDate = System.currentTimeMillis()
                        show()
                    }
                }) {
                    Icon(
                        Icons.Filled.DateRange,
                        contentDescription = "Seleccionar Fecha"
                    )
                }
            }
        )
        Spacer(modifier = Modifier.height(24.dp))

        Button(
            onClick = { registerViewModel.register() },
            enabled = uiState != RegisterUiState.Loading,
            modifier = Modifier.fillMaxWidth()
        ) {
            AnimatedVisibility(visible = uiState == RegisterUiState.Loading) {
                CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp, color = MaterialTheme.colorScheme.onPrimary)
                Spacer(modifier = Modifier.width(8.dp))
            }
            Text("Registrarse")
        }
        Spacer(modifier = Modifier.height(16.dp))
    }
}
