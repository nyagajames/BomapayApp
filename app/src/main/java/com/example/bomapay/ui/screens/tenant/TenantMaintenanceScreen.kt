package com.example.bomapay.ui.screens.tenant

import android.Manifest
import android.content.Context
import android.net.Uri
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
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var title by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var selectedImageUri by remember { mutableStateOf<Uri?>(null) }
    var showRationale by remember { mutableStateOf(false) }

    val isEditMode = uiState.currentEditRequest != null

    // Form handling
    LaunchedEffect(uiState.currentEditRequest) {
        uiState.currentEditRequest?.let { req ->
            title = req.title
            description = req.description
            selectedImageUri = null
        }
    }

    LaunchedEffect(isEditMode) {
        if (!isEditMode) {
            title = ""
            description = ""
            selectedImageUri = null
        }
    }

    // Launchers
    val takePictureLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.TakePicture()
    ) { success ->
        if (!success) selectedImageUri = null
    }

    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            val file = createImageFile(context)
            val uri = getFileProviderUri(context, file)
            selectedImageUri = uri
            takePictureLauncher.launch(uri)
        } else {
            showRationale = true
        }
    }

    val galleryLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri -> selectedImageUri = uri }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (isEditMode) "Update Request" else "New Request") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
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
                    onValueChange = { title = it },
                    label = { Text("Title") },
                    modifier = Modifier.fillMaxWidth()
                )
            }

            item {
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
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
                        onClick = { cameraPermissionLauncher.launch(Manifest.permission.CAMERA) },
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
                            val result = viewModel.uploadMaintenanceRequest(
                                context = context,
                                title = title,
                                description = description,
                                selectedImageUri = selectedImageUri,
                                existingRequest = uiState.currentEditRequest
                            )
                            if (result.isSuccess) {
                                viewModel.clearFormState()
                                title = ""
                                description = ""
                                selectedImageUri = null
                            }
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
