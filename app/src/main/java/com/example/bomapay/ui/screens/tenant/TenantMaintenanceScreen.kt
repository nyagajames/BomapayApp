package com.example.bomapay.ui.screens.tenant

import android.Manifest
import android.content.Context
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.example.bomapay.ui.viewmodel.MaintenanceRequest
import com.example.bomapay.ui.viewmodel.MaintenanceViewModel
import kotlinx.coroutines.launch
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TenantMaintenanceScreen(
    viewModel: MaintenanceViewModel,
    onNavigateBack: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val title by viewModel.title.collectAsStateWithLifecycle()
    val description by viewModel.description.collectAsStateWithLifecycle()
    val selectedImageUri by viewModel.selectedImageUri.collectAsStateWithLifecycle()
    
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var showRationale by remember { mutableStateOf(false) }

    val isEditMode = uiState.currentEditRequest != null

    // Launchers
    val takePictureLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.TakePicture()
    ) { success ->
        if (!success) viewModel.selectedImageUri.value = null
    }

    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            try {
                val file = createImageFile(context)
                val uri = getFileProviderUri(context, file)
                viewModel.selectedImageUri.value = uri
                takePictureLauncher.launch(uri)
            } catch (e: Exception) {
                Toast.makeText(context, "Error starting camera: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        } else {
            showRationale = true
        }
    }

    val galleryLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri -> viewModel.selectedImageUri.value = uri }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (isEditMode) "Update Request" else "New Request") },
                navigationIcon = {
                    IconButton(onClick = {
                        if (isEditMode) viewModel.cancelEdit() else onNavigateBack()
                    }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                )
            )
        },
        containerColor = MaterialTheme.colorScheme.surfaceVariant // Changes the white background
    ) { padding ->
        LazyColumn(
            modifier = modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = title,
                    onValueChange = { viewModel.title.value = it },
                    label = { Text("Title") },
                    modifier = Modifier.fillMaxWidth()
                )
            }

            item {
                OutlinedTextField(
                    value = description,
                    onValueChange = { viewModel.description.value = it },
                    label = { Text("Description") },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 3
                )
            }

            // Image Preview
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(220.dp),
                    contentAlignment = Alignment.Center
                ) {
                    when {
                        selectedImageUri != null -> {
                            AsyncImage(
                                model = selectedImageUri,
                                contentDescription = "Selected preview",
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop
                            )
                        }
                        isEditMode && uiState.currentEditRequest?.imageUrl != null -> {
                            AsyncImage(
                                model = uiState.currentEditRequest?.imageUrl,
                                contentDescription = "Existing image",
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop
                            )
                        }
                        else -> {
                            Text("No image selected", style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                }
            }

            item {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Button(onClick = { galleryLauncher.launch("image/*") }, modifier = Modifier.weight(1f)) {
                        Text("Gallery")
                    }
                    Button(
                        onClick = {
                            val permission = Manifest.permission.CAMERA
                            val isGranted = androidx.core.content.ContextCompat.checkSelfPermission(
                                context, permission
                            ) == android.content.pm.PackageManager.PERMISSION_GRANTED

                            if (isGranted) {
                                try {
                                    val file = createImageFile(context)
                                    val uri = getFileProviderUri(context, file)
                                    viewModel.selectedImageUri.value = uri
                                    takePictureLauncher.launch(uri)
                                } catch (e: Exception) {
                                    Toast.makeText(context, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
                                }
                            } else {
                                cameraPermissionLauncher.launch(permission)
                            }
                        },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Camera")
                    }
                }
            }

            item {
                Button(
                    onClick = {
                        scope.launch {
                            viewModel.uploadMaintenanceRequest(
                                context = context,
                                existingRequest = uiState.currentEditRequest
                            )
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = title.isNotBlank() && description.isNotBlank() && !uiState.isLoading
                ) {
                    Text(if (isEditMode) "Update Request" else "Submit Request")
                }
            }

            item {
                if (uiState.uploadProgress in 0.01f..0.99f) {
                    LinearProgressIndicator(progress = uiState.uploadProgress, modifier = Modifier.fillMaxWidth())
                }

                if (uiState.error != null) {
                    Text(uiState.error.orEmpty(), color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(vertical = 8.dp))
                }
            }

            // History List Header
            item {
                Text(
                    "Maintenance History",
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.padding(top = 16.dp)
                )
            }

            items(items = uiState.requests, key = { it.id }) { request ->
                MaintenanceRequestCard(
                    request = request,
                    onEdit = { viewModel.startEdit(it) },
                    onDelete = { viewModel.deleteRequest(it) }
                )
            }
        }
    }

    if (showRationale) {
        AlertDialog(
            onDismissRequest = { showRationale = false },
            title = { Text("Camera Permission Required") },
            text = { Text("This app needs camera permission to take photos for maintenance requests.") },
            confirmButton = {
                TextButton(onClick = { showRationale = false }) { Text("OK") }
            }
        )
    }
}

// Maintenance Request Card
@Composable
private fun MaintenanceRequestCard(
    request: MaintenanceRequest,
    onEdit: (MaintenanceRequest) -> Unit,
    onDelete: (String) -> Unit
) {
    Card(modifier = Modifier.fillMaxWidth(), elevation = CardDefaults.cardElevation(4.dp)) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(request.title, style = MaterialTheme.typography.titleMedium)
            Text(request.description, style = MaterialTheme.typography.bodyMedium)
            Text("Status: ${request.status}", style = MaterialTheme.typography.labelMedium)

            request.imageUrl?.let { url ->
                Spacer(Modifier.height(8.dp))
                AsyncImage(
                    model = url,
                    contentDescription = null,
                    modifier = Modifier.fillMaxWidth().height(160.dp),
                    contentScale = ContentScale.Crop
                )
            }

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                TextButton(onClick = { onEdit(request) }) { Text("Edit") }
                TextButton(
                    onClick = { onDelete(request.id) },
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Delete")
                }
            }
        }
    }
}

// Helpers
private fun createImageFile(context: Context): File {
    val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
    val storageDir = context.getExternalFilesDir(null)
    return File.createTempFile("JPEG_${timeStamp}_${UUID.randomUUID().toString().take(8)}_", ".jpg", storageDir)
}

private fun getFileProviderUri(context: Context, file: File): Uri {
    return androidx.core.content.FileProvider.getUriForFile(
        context,
        "${context.packageName}.provider",
        file
    )
}
