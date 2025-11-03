package com.example.levelup_gamerpractica.ui.screens

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest // <-- IMPORT AÑADIDO
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.rememberAsyncImagePainter
import com.example.levelup_gamerpractica.R
import com.example.levelup_gamerpractica.data.local.LevelUpGamerApplication
import com.example.levelup_gamerpractica.viewmodel.ProfileUiState
import com.example.levelup_gamerpractica.viewmodel.ProfileViewModel
import com.example.levelup_gamerpractica.viewmodel.ProfileViewModelFactory
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun ProfileScreen(
    modifier: Modifier = Modifier,
    profileViewModel: ProfileViewModel = viewModel(
        factory = ProfileViewModelFactory((LocalContext.current.applicationContext as LevelUpGamerApplication).repository)
    )
) {
    // --- CORRECCIÓN DE ESTADO ---
    // El ViewModel expone un solo UiState que contiene todo.
    val uiState by profileViewModel.uiState.collectAsState()
    // El usuario se deriva del uiState
    val user = uiState.user
    // --- FIN DE CORRECCIÓN ---

    val context = LocalContext.current
    var showEditDialog by remember { mutableStateOf<String?>(null) }
    var showImageSourceDialog by remember { mutableStateOf(false) }

    // --- Controladores para permisos e imágenes ---
    var tempCameraUri by remember { mutableStateOf<Uri?>(null) }

    // 1. Lanzador para la CÁMARA
    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture(),
        onResult = { success ->
            if (success) {
                tempCameraUri?.let { uri ->
                    profileViewModel.onProfilePictureChanged(uri.toString())
                }
            }
        }
    )

    // 2. Lanzador para la GALERÍA
    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia(),
        onResult = { uri ->
            if (uri != null) {
                try {
                    // El selector PickVisualMedia ya nos da acceso temporal.
                    profileViewModel.onProfilePictureChanged(uri.toString())

                } catch (e: SecurityException) {
                    Toast.makeText(context, "Error de permisos: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    )

    // 3. Lanzador para el permiso de CÁMARA
    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = { isGranted ->
            if (isGranted) {
                // Permiso concedido, crear URI y lanzar cámara
                val newUri = createImageUri(context)
                tempCameraUri = newUri
                cameraLauncher.launch(newUri) // Usamos la variable local
            } else {
                Toast.makeText(context, "Permiso de cámara denegado", Toast.LENGTH_SHORT).show()
            }
        }
    )


    // --- CORRECCIÓN DE LaunchedEffect ---
    // Se comprueban las propiedades del UiState, no las subclases
    LaunchedEffect(uiState) {
        if (uiState.isSuccess) {
            Toast.makeText(context, "Perfil actualizado", Toast.LENGTH_SHORT).show()
            profileViewModel.consumeUiState()
        }
        if (uiState.error != null) {
            Toast.makeText(context, "Error: ${uiState.error}", Toast.LENGTH_LONG).show()
            profileViewModel.consumeUiState()
        }
    }
    // --- FIN DE CORRECCIÓN ---


    // --- UI Principal ---
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(20.dp))

        // --- Imagen de Perfil ---
        Box(
            modifier = Modifier
                .size(150.dp)
                .clip(CircleShape)
                .border(2.dp, MaterialTheme.colorScheme.primary, CircleShape)
                .clickable { showImageSourceDialog = true },
            contentAlignment = Alignment.Center
        ) {
            Image(
                // Los errores de 'profilePictureUri' se arreglan al corregir 'user'
                painter = rememberAsyncImagePainter(
                    model = user?.profilePictureUri ?: R.drawable.ic_launcher_background // TODO: Reemplaza con un placeholder
                ),
                contentDescription = "Foto de Perfil",
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
            // Icono de "editar" sobre la imagen
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clip(CircleShape)
                    .clickable { showImageSourceDialog = true }
                    .padding(8.dp),
                contentAlignment = Alignment.BottomEnd
            ) {
                Icon(
                    Icons.Filled.CameraAlt,
                    contentDescription = "Cambiar Foto",
                    tint = Color.White,
                    modifier = Modifier.size(30.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // --- Detalles del Usuario ---
        // Los errores de 'username' y 'email' se arreglan al corregir 'user'
        ProfileInfoRow(
            label = "Nombre de Usuario",
            value = user?.username ?: "Cargando...",
            onEditClick = { showEditDialog = "username" }
        )
        ProfileInfoRow(
            label = "Correo Electrónico",
            value = user?.email ?: "Cargando...",
            onEditClick = { showEditDialog = "email" }
        )
        ProfileInfoRow(
            label = "Contraseña",
            value = "••••••••",
            onEditClick = { showEditDialog = "password" }
        )
    }

    // --- Diálogo para elegir Fuente de Imagen ---
    if (showImageSourceDialog) {
        AlertDialog(
            onDismissRequest = { showImageSourceDialog = false },
            title = { Text("Cambiar Foto de Perfil") },
            text = { Text("¿Desde dónde quieres elegir la foto?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        showImageSourceDialog = false
                        // Verificar permiso de cámara
                        when (PackageManager.PERMISSION_GRANTED) {
                            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) -> {
                                val newUri = createImageUri(context)
                                tempCameraUri = newUri
                                cameraLauncher.launch(newUri) // Usamos la variable local
                            }
                            else -> {
                                cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
                            }
                        }
                    }
                ) { Text("Cámara") }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        showImageSourceDialog = false
                        // --- CORRECCIÓN DEL LAUNCHER ---
                        // Se debe usar PickVisualMediaRequest
                        galleryLauncher.launch(
                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                        )
                        // --- FIN DE CORRECCIÓN ---
                    }
                ) { Text("Galería") }
            }
        )
    }

    // --- Diálogo para Editar Campos ---
    if (showEditDialog != null) {
        EditProfileDialog(
            field = showEditDialog!!,
            onDismiss = { showEditDialog = null },
            onConfirm = { field, value1, value2 ->
                when (field) {
                    "username" -> profileViewModel.updateDetails(username = value1)
                    "email" -> profileViewModel.updateDetails(email = value1)
                    // El error de 'updatePassword' se soluciona al tener el ViewModel correcto
                    "password" -> profileViewModel.updatePassword(oldPassword = value1, newPassword = value2)
                }
                showEditDialog = null
            }
        )
    }
}

// --- Composable para las filas de información (Nombre, Email, etc.) ---
@Composable
private fun ProfileInfoRow(label: String, value: String, onEditClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
                Spacer(modifier = Modifier.height(4.dp))
                Text(value, style = MaterialTheme.typography.bodyLarge)
            }
            IconButton(onClick = onEditClick) {
                Icon(Icons.Filled.Edit, contentDescription = "Editar $label")
            }
        }
    }
}

// --- Diálogo genérico para editar ---
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun EditProfileDialog(
    field: String,
    onDismiss: () -> Unit,
    onConfirm: (String, String, String) -> Unit
) {
    var value1 by remember { mutableStateOf("") }
    var value2 by remember { mutableStateOf("") }
    val isPassword = field == "password"

    val title = when (field) {
        "username" -> "Cambiar Nombre de Usuario"
        "email" -> "Cambiar Correo Electrónico"
        "password" -> "Cambiar Contraseña"
        else -> ""
    }
    val label1 = when (field) {
        "username" -> "Nuevo nombre de usuario"
        "email" -> "Nuevo correo electrónico"
        "password" -> "Contraseña actual"
        else -> ""
    }
    val label2 = if (isPassword) "Nueva contraseña" else ""

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column {
                OutlinedTextField(
                    value = value1,
                    onValueChange = { value1 = it },
                    label = { Text(label1) },
                    modifier = Modifier.fillMaxWidth()
                )
                if (isPassword) {
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = value2,
                        onValueChange = { value2 = it },
                        label = { Text(label2) },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        },
        confirmButton = {
            Button(onClick = { onConfirm(field, value1, value2) }) {
                Text("Guardar")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancelar")
            }
        }
    )
}

// --- Función Helper para crear la URI de la cámara ---
private fun createImageUri(context: Context): Uri {
    val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
    val imageFile = File(context.cacheDir, "JPEG_${timeStamp}_my_image.jpg")
    return FileProvider.getUriForFile(
        context,
        "${context.packageName}.provider", // Asegúrate que coincida con el AndroidManifest
        imageFile
    )
}

