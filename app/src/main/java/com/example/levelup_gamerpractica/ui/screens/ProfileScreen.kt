package com.example.levelup_gamerpractica.ui.screens

import android.Manifest
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.rememberVectorPainter 
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.example.levelup_gamerpractica.data.local.LevelUpGamerApplication
import com.example.levelup_gamerpractica.viewmodel.ProfileViewModel
import com.example.levelup_gamerpractica.viewmodel.ProfileViewModelFactory
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun ProfileScreen(
    modifier: Modifier = Modifier,
    profileViewModel: ProfileViewModel = viewModel(
        factory = ProfileViewModelFactory((LocalContext.current.applicationContext as LevelUpGamerApplication).repository)
    )
) {
    val context = LocalContext.current

    // --- Recolectar estado del ViewModel ---
    val uiState by profileViewModel.uiState.collectAsState()
    val username by profileViewModel.username.collectAsState()
    val email by profileViewModel.email.collectAsState()
    val oldPassword by profileViewModel.oldPassword.collectAsState()
    val newPassword by profileViewModel.newPassword.collectAsState()
    val confirmNewPassword by profileViewModel.confirmNewPassword.collectAsState()

    var showPasswordDialog by remember { mutableStateOf(false) }
    var showEditDialog by remember { mutableStateOf(false) }
    var showPhotoDialog by remember { mutableStateOf(false) }

    var tempCameraUri by remember { mutableStateOf<Uri?>(null) }

    // --- Manejo de UI (Errores/Éxito) ---
    LaunchedEffect(uiState.error, uiState.isSuccess) {
        if (uiState.error != null) {
            Toast.makeText(context, uiState.error, Toast.LENGTH_LONG).show()
            profileViewModel.consumeUiState()
        }
        if (uiState.isSuccess) {
            Toast.makeText(context, "¡Actualizado con éxito!", Toast.LENGTH_SHORT).show()
            showEditDialog = false
            showPasswordDialog = false
            profileViewModel.consumeUiState()
        }
    }

    // --- Lanzador de Galería (Moderno) ---
    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia(),
        onResult = { uri: Uri? ->
            if (uri != null) {
                profileViewModel.updateProfilePicture(uri.toString())
            }
            showPhotoDialog = false
        }
    )

    // --- Lanzador de Cámara ---
    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture(),
        onResult = { success: Boolean ->
            if (success) {
                tempCameraUri?.let { uri ->
                    profileViewModel.updateProfilePicture(uri.toString())
                }
            }
            showPhotoDialog = false
        }
    )

    // --- Lanzador de Permisos ---
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = { isGranted: Boolean ->
            if (isGranted) {
                // Permiso concedido, lanzar la cámara
                val newUri = context.createImageUri()
                tempCameraUri = newUri
                cameraLauncher.launch(newUri)
            } else {
                Toast.makeText(context, "Permiso de cámara denegado", Toast.LENGTH_SHORT).show()
                showPhotoDialog = false
            }
        }
    )

    // --- UI Principal ---
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        val fallbackPainter = rememberVectorPainter(Icons.Default.Person)

        Box(
            modifier = Modifier
                .size(120.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.surfaceVariant)
                .clickable { showPhotoDialog = true },
            contentAlignment = Alignment.Center
        ) {
            AsyncImage(
                model = uiState.user?.profilePictureUri,
                contentDescription = "Foto de perfil",
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop,
                placeholder = fallbackPainter,
                error = fallbackPainter,
                fallback = fallbackPainter
            )
            // Icono de "Editar" sobre la foto
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.3f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.CameraAlt,
                    contentDescription = "Cambiar foto",
                    tint = Color.White
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = uiState.user?.username ?: "Usuario",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = uiState.user?.email ?: "email@ejemplo.com",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(32.dp))
        Divider()

        // --- Botones de Acción ---
        ProfileButton(
            text = "Editar Perfil (Nombre/Email)",
            icon = Icons.Default.Person,
            onClick = { showEditDialog = true }
        )
        ProfileButton(
            text = "Cambiar Contraseña",
            icon = Icons.Default.Lock,
            onClick = { showPasswordDialog = true }
        )
    }

    // --- Diálogo: Elegir Foto (Galería o Cámara) ---
    if (showPhotoDialog) {
        AlertDialog(
            onDismissRequest = { showPhotoDialog = false },
            title = { Text("Cambiar Foto de Perfil") },
            text = { Text("¿Desde dónde quieres elegir la foto?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        val request = PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                        galleryLauncher.launch(request)
                    }
                ) { Text("Galería") }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        // Pedir permiso de cámara antes de lanzarla
                        permissionLauncher.launch(Manifest.permission.CAMERA)
                    }
                ) { Text("Cámara") }
            }
        )
    }

    // --- Diálogo: Editar Perfil ---
    if (showEditDialog) {
        EditProfileDialog(
            username = username,
            email = email,
            onDismiss = { showEditDialog = false },
            onConfirm = {
                profileViewModel.updateDetails()
            },
            onUsernameChange = { profileViewModel.username.value = it },
            onEmailChange = { profileViewModel.email.value = it },
            isLoading = uiState.isLoading
        )
    }

    // --- Diálogo: Cambiar Contraseña ---
    if (showPasswordDialog) {
        ChangePasswordDialog(
            oldPassword = oldPassword,
            newPassword = newPassword,
            confirmNewPassword = confirmNewPassword,
            onDismiss = { showPasswordDialog = false },
            onConfirm = {
                profileViewModel.updatePassword()
            },
            onOldPasswordChange = { profileViewModel.oldPassword.value = it },
            onNewPasswordChange = { profileViewModel.newPassword.value = it },
            onConfirmNewPasswordChange = { profileViewModel.confirmNewPassword.value = it },
            isLoading = uiState.isLoading
        )
    }
}

// --- Componentes de Diálogos (Internos) ---

@Composable
private fun ProfileButton(text: String, icon: ImageVector, onClick: () -> Unit) {
    OutlinedButton(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
    ) {
        Icon(imageVector = icon, contentDescription = null, modifier = Modifier.size(20.dp))
        Spacer(modifier = Modifier.width(16.dp))
        Text(text)
        Spacer(modifier = Modifier.weight(1f))
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun EditProfileDialog(
    username: String,
    email: String,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
    onUsernameChange: (String) -> Unit,
    onEmailChange: (String) -> Unit,
    isLoading: Boolean
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Editar Perfil") },
        text = {
            Column {
                OutlinedTextField(
                    value = username,
                    onValueChange = onUsernameChange,
                    label = { Text("Nombre de usuario") },
                    leadingIcon = { Icon(Icons.Default.Person, null) },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(16.dp))
                OutlinedTextField(
                    value = email,
                    onValueChange = onEmailChange,
                    label = { Text("Email") },
                    leadingIcon = { Icon(Icons.Default.Email, null) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(onClick = onConfirm, enabled = !isLoading) {
                if (isLoading) {
                    CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp)
                } else {
                    Text("Guardar")
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancelar")
            }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ChangePasswordDialog(
    oldPassword: String,
    newPassword: String,
    confirmNewPassword: String,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
    onOldPasswordChange: (String) -> Unit,
    onNewPasswordChange: (String) -> Unit,
    onConfirmNewPasswordChange: (String) -> Unit,
    isLoading: Boolean
) {
    var oldVisible by remember { mutableStateOf(false) }
    var newVisible by remember { mutableStateOf(false) }
    var confirmVisible by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Cambiar Contraseña") },
        text = {
            Column {
                PasswordField(
                    value = oldPassword,
                    onValueChange = onOldPasswordChange,
                    label = "Contraseña actual",
                    isVisible = oldVisible,
                    onVisibilityToggle = { oldVisible = !oldVisible }
                )
                Spacer(modifier = Modifier.height(16.dp))
                PasswordField(
                    value = newPassword,
                    onValueChange = onNewPasswordChange,
                    label = "Nueva contraseña",
                    isVisible = newVisible,
                    onVisibilityToggle = { newVisible = !newVisible }
                )
                Spacer(modifier = Modifier.height(16.dp))
                PasswordField(
                    value = confirmNewPassword,
                    onValueChange = onConfirmNewPasswordChange,
                    label = "Confirmar nueva contraseña",
                    isVisible = confirmVisible,
                    onVisibilityToggle = { confirmVisible = !confirmVisible }
                )
            }
        },
        confirmButton = {
            Button(onClick = onConfirm, enabled = !isLoading) {
                if (isLoading) {
                    CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp)
                } else {
                    Text("Actualizar")
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancelar")
            }
        }
    )
}

@Composable
private fun PasswordField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    isVisible: Boolean,
    onVisibilityToggle: () -> Unit
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        leadingIcon = { Icon(Icons.Default.Lock, null) },
        visualTransformation = if (isVisible) VisualTransformation.None else PasswordVisualTransformation(),
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
        trailingIcon = {
            val icon = if (isVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility
            IconButton(onClick = onVisibilityToggle) {
                Icon(icon, if (isVisible) "Ocultar" else "Mostrar")
            }
        },
        modifier = Modifier.fillMaxWidth()
    )
}

// --- Helper para crear la URI de la cámara ---
private fun Context.createImageUri(): Uri {
    val file = File(filesDir, "camera_photos").apply { mkdirs() }
    val imageFile = File(
        file,
        "img_${SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())}.jpg"
    )
    return FileProvider.getUriForFile(
        this,
        "${applicationContext.packageName}.provider",
        imageFile
    )
}
