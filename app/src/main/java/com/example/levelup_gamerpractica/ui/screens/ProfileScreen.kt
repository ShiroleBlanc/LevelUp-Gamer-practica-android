package com.example.levelup_gamerpractica.ui.screens

import android.Manifest
import android.content.Context
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import coil.request.ImageRequest
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
    onLogout: () -> Unit = {},
    profileViewModel: ProfileViewModel = viewModel(
        factory = ProfileViewModelFactory((LocalContext.current.applicationContext as LevelUpGamerApplication).repository)
    )
) {
    val context = LocalContext.current
    val uiState by profileViewModel.uiState.collectAsState()
    val user = uiState.user

    var imageRefreshTrigger by remember { mutableStateOf(System.currentTimeMillis()) }

    // Estados de diálogos
    var showEditDialog by remember { mutableStateOf(false) }
    var showPasswordDialog by remember { mutableStateOf(false) }
    var showPhotoDialog by remember { mutableStateOf(false) }
    var tempCameraUri by remember { mutableStateOf<Uri?>(null) }

    val formUsername by profileViewModel.username.collectAsState()
    val formEmail by profileViewModel.email.collectAsState()
    val formOldPass by profileViewModel.oldPassword.collectAsState()
    val formNewPass by profileViewModel.newPassword.collectAsState()
    val formConfirmPass by profileViewModel.confirmNewPassword.collectAsState()

    LaunchedEffect(uiState.error, uiState.isSuccess) {
        if (uiState.error != null) {
            Toast.makeText(context, uiState.error, Toast.LENGTH_LONG).show()
            profileViewModel.consumeUiState()
        }
        if (uiState.isSuccess) {
            Toast.makeText(context, "¡Actualizado con éxito!", Toast.LENGTH_SHORT).show()
            showEditDialog = false
            showPasswordDialog = false
            showPhotoDialog = false

            imageRefreshTrigger = System.currentTimeMillis()

            profileViewModel.consumeUiState()
            profileViewModel.refreshProfile()
        }
    }

    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia(),
        onResult = { uri ->
            if (uri != null) profileViewModel.updateProfilePicture(uri.toString())
            showPhotoDialog = false
        }
    )

    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture(),
        onResult = { success ->
            if (success) tempCameraUri?.let { profileViewModel.updateProfilePicture(it.toString()) }
            showPhotoDialog = false
        }
    )

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = { isGranted ->
            if (isGranted) {
                val uri = context.createImageUri()
                tempCameraUri = uri
                cameraLauncher.launch(uri)
            } else {
                Toast.makeText(context, "Permiso denegado", Toast.LENGTH_SHORT).show()
            }
        }
    )

    if (user == null) {
        Box(modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
        return
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(140.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.surfaceVariant)
                .clickable { showPhotoDialog = true },
            contentAlignment = Alignment.Center
        ) {
            val painter = rememberVectorPainter(Icons.Default.Person)

            if (user.profilePictureUrl != null) {

                val isUrl = user.profilePictureUrl.startsWith("http")

                val imageModel = if (isUrl) {
                    "${user.profilePictureUrl}?t=$imageRefreshTrigger"
                } else {
                    File(user.profilePictureUrl)
                }

                key(user.profilePictureUrl, imageRefreshTrigger) {
                    AsyncImage(
                        model = ImageRequest.Builder(LocalContext.current)
                            .data(imageModel)
                            .crossfade(true)
                            .build(),
                        contentDescription = "Foto de perfil",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop,
                        error = painter,
                        placeholder = painter
                    )
                }
            } else {
                Icon(painter, null, modifier = Modifier.size(60.dp))
            }

            Box(
                modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.3f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.CameraAlt, "Editar", tint = Color.White)
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text(text = user.username, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
        Text(text = user.email, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)

        if (user.userRole == "ROLE_DUOC") {
            Spacer(modifier = Modifier.height(8.dp))
            Badge(containerColor = MaterialTheme.colorScheme.primary) {
                Text("Miembro Duoc UC", modifier = Modifier.padding(4.dp))
            }
        }

        Spacer(modifier = Modifier.height(24.dp))
        Divider()
        Spacer(modifier = Modifier.height(24.dp))

        if (user.userRole == "ROLE_DUOC") {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Mis Recompensas", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(12.dp))

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Nivel Actual:")
                        Text("Nivel ${user.userLevel}", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    }
                    Divider(modifier = Modifier.padding(vertical = 8.dp), color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.2f))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Puntos Disponibles:")
                        Text("${user.pointsBalance} Pts.", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    }
                }
            }
            Spacer(modifier = Modifier.height(24.dp))
        }

        OutlinedButton(
            onClick = {
                profileViewModel.username.value = user.username
                profileViewModel.email.value = user.email
                showEditDialog = true
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(Icons.Default.Edit, null)
            Spacer(Modifier.width(8.dp))
            Text("Editar Perfil")
        }

        Spacer(modifier = Modifier.height(8.dp))

        OutlinedButton(
            onClick = { showPasswordDialog = true },
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(Icons.Default.Lock, null)
            Spacer(Modifier.width(8.dp))
            Text("Cambiar Contraseña")
        }

        Spacer(modifier = Modifier.height(24.dp))

        Button(
            onClick = {
                profileViewModel.logout()
                onLogout()
            },
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Cerrar Sesión")
        }
    }

    if (showPhotoDialog) {
        AlertDialog(
            onDismissRequest = { showPhotoDialog = false },
            title = { Text("Cambiar Foto de Perfil") },
            text = { Text("Elige una opción:") },
            confirmButton = {
                TextButton(
                    onClick = { galleryLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) }
                ) { Text("Galería") }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        if (ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == android.content.pm.PackageManager.PERMISSION_GRANTED) {
                            val uri = context.createImageUri()
                            tempCameraUri = uri
                            cameraLauncher.launch(uri)
                        } else {
                            permissionLauncher.launch(Manifest.permission.CAMERA)
                        }
                    }
                ) { Text("Cámara") }
            }
        )
    }

    if (showEditDialog) {
        EditProfileDialog(
            username = formUsername, email = formEmail,
            onDismiss = { showEditDialog = false },
            onConfirm = { profileViewModel.updateDetails() },
            onUsernameChange = { profileViewModel.username.value = it },
            onEmailChange = { profileViewModel.email.value = it },
            isLoading = uiState.isLoading
        )
    }

    if (showPasswordDialog) {
        ChangePasswordDialog(
            oldPass = formOldPass, newPass = formNewPass, confirmPass = formConfirmPass,
            onOldChange = { profileViewModel.oldPassword.value = it },
            onNewChange = { profileViewModel.newPassword.value = it },
            onConfirmChange = { profileViewModel.confirmNewPassword.value = it },
            onSubmit = { profileViewModel.updatePassword() },
            onDismiss = { showPasswordDialog = false },
            isLoading = uiState.isLoading
        )
    }
}

@Composable
private fun EditProfileDialog(
    username: String, email: String,
    onDismiss: () -> Unit, onConfirm: () -> Unit,
    onUsernameChange: (String) -> Unit, onEmailChange: (String) -> Unit,
    isLoading: Boolean
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Editar Perfil") },
        text = {
            Column {
                OutlinedTextField(value = username, onValueChange = onUsernameChange, label = { Text("Usuario") }, modifier = Modifier.fillMaxWidth())
                Spacer(Modifier.height(16.dp))
                OutlinedTextField(value = email, onValueChange = onEmailChange, label = { Text("Email") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email), modifier = Modifier.fillMaxWidth())
            }
        },
        confirmButton = { Button(onClick = onConfirm, enabled = !isLoading) { if(isLoading) CircularProgressIndicator(Modifier.size(20.dp), color = MaterialTheme.colorScheme.onPrimary) else Text("Guardar") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancelar") } }
    )
}

@Composable
private fun ChangePasswordDialog(
    oldPass: String, newPass: String, confirmPass: String,
    onOldChange: (String) -> Unit, onNewChange: (String) -> Unit, onConfirmChange: (String) -> Unit,
    onSubmit: () -> Unit, onDismiss: () -> Unit, isLoading: Boolean
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Cambiar Contraseña") },
        text = {
            Column {
                PasswordField(oldPass, onOldChange, "Contraseña Actual")
                Spacer(Modifier.height(8.dp))
                PasswordField(newPass, onNewChange, "Nueva Contraseña")
                Spacer(Modifier.height(8.dp))
                PasswordField(confirmPass, onConfirmChange, "Confirmar Nueva")
            }
        },
        confirmButton = { Button(onClick = onSubmit, enabled = !isLoading) { if(isLoading) CircularProgressIndicator(Modifier.size(20.dp), color = MaterialTheme.colorScheme.onPrimary) else Text("Actualizar") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancelar") } }
    )
}

@Composable
fun PasswordField(value: String, onValueChange: (String) -> Unit, label: String) {
    var visible by remember { mutableStateOf(false) }
    OutlinedTextField(
        value = value, onValueChange = onValueChange, label = { Text(label) },
        visualTransformation = if (visible) VisualTransformation.None else PasswordVisualTransformation(),
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
        trailingIcon = { IconButton(onClick = { visible = !visible }) { Icon(if (visible) Icons.Default.VisibilityOff else Icons.Default.Visibility, null) } },
        singleLine = true, modifier = Modifier.fillMaxWidth()
    )
}

private fun Context.createImageUri(): Uri {
    val file = File(filesDir, "camera_photos").apply { mkdirs() }
    val imageFile = File(file, "img_${SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())}.jpg")
    return FileProvider.getUriForFile(this, "${packageName}.provider", imageFile)
}