package com.example.bomapay.ui.viewmodel

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cloudinary.android.MediaManager
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.tasks.await
import java.io.File
import java.util.UUID
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

data class MaintenanceRequest(
    val id: String = "",
    val title: String = "",
    val description: String = "",
    val imageUrl: String? = null,
    val status: String = "Pending", // Pending, In Progress, Resolved
    val timestamp: Long = System.currentTimeMillis(),
    val tenantId: String = "",
    val houseNumber: String = "" // Added for landlord visibility
)

data class MaintenanceUiState(
    val requests: List<MaintenanceRequest> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null,
    val currentEditRequest: MaintenanceRequest? = null, // For edit mode
    val uploadProgress: Float = 0f
)

class MaintenanceViewModel(
    private val db: FirebaseFirestore = FirebaseFirestore.getInstance(),
    private val auth: com.google.firebase.auth.FirebaseAuth = com.google.firebase.auth.FirebaseAuth.getInstance()
) : ViewModel() {

    private val tenantId: String get() = auth.currentUser?.uid ?: ""

    private val _uiState = MutableStateFlow(MaintenanceUiState())
    val uiState: StateFlow<MaintenanceUiState> = _uiState.asStateFlow()

    private var houseNumber: String = "Unassigned"
    private var listenerRegistration: com.google.firebase.firestore.ListenerRegistration? = null

    init {
        fetchHouseNumber()
        setupSnapshotListener()
    }

    private fun fetchHouseNumber() {
        viewModelScope.launch {
            try {
                val doc = db.collection("users").document(tenantId).get().await()
                houseNumber = doc.getString("houseNumber") ?: "Unassigned"
            } catch (e: Exception) {}
        }
    }

    private fun setupSnapshotListener() {
        _uiState.value = _uiState.value.copy(isLoading = true)

        listenerRegistration = db.collection("maintenance_requests")
            .whereEqualTo("tenantId", tenantId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    _uiState.value = _uiState.value.copy(
                        error = error.message ?: "Failed to load maintenance requests",
                        isLoading = false
                    )
                    return@addSnapshotListener
                }

                val requests = snapshot?.documents?.mapNotNull { doc ->
                    doc.toObject(MaintenanceRequest::class.java)?.copy(id = doc.id)
                }?.sortedByDescending { it.timestamp } ?: emptyList()

                _uiState.value = _uiState.value.copy(
                    requests = requests,
                    isLoading = false,
                    error = null
                )
            }
    }

    override fun onCleared() {
        super.onCleared()
        listenerRegistration?.remove()
    }

    fun startEdit(request: MaintenanceRequest) {
        _uiState.value = _uiState.value.copy(currentEditRequest = request)
    }

    fun cancelEdit() {
        _uiState.value = _uiState.value.copy(currentEditRequest = null)
    }

    suspend fun uploadMaintenanceRequest(
        context: android.content.Context,
        title: String,
        description: String,
        selectedImageUri: Uri?,
        existingRequest: MaintenanceRequest? = null // null for new
    ): Result<MaintenanceRequest> = runCatching {
        _uiState.value = _uiState.value.copy(isLoading = true, error = null)
        
        var imageUrl = existingRequest?.imageUrl

        // Upload new image only if selected
        if (selectedImageUri != null) {
            imageUrl = uploadImageToCloudinary(context, selectedImageUri)
        }

        val request = MaintenanceRequest(
            id = existingRequest?.id ?: UUID.randomUUID().toString(),
            title = title.trim(),
            description = description.trim(),
            imageUrl = imageUrl,
            status = existingRequest?.status ?: "Pending",
            timestamp = System.currentTimeMillis(),
            tenantId = tenantId,
            houseNumber = houseNumber
        )

        val docRef = db.collection("maintenance_requests").document(request.id)
        docRef.set(request).await()

        // Clear edit mode on success
        if (existingRequest != null) cancelEdit()

        _uiState.value = _uiState.value.copy(isLoading = false)
        request
    }.onFailure { e ->
        _uiState.value = _uiState.value.copy(isLoading = false, error = e.message)
    }

    private suspend fun uploadImageToCloudinary(context: android.content.Context, uri: Uri): String = suspendCancellableCoroutine { cont ->
        try {
            android.util.Log.d("CloudinaryUpload", "Starting upload for URI: $uri")
            val inputStream = context.contentResolver.openInputStream(uri)
            val tempFile = File.createTempFile("cloudinary_upload", ".jpg", context.cacheDir)
            
            inputStream?.use { input ->
                tempFile.outputStream().use { output ->
                    input.copyTo(output)
                }
            }
            android.util.Log.d("CloudinaryUpload", "Temp file created: ${tempFile.absolutePath}")

            MediaManager.get().upload(tempFile.absolutePath)
                .option("folder", "bomapay/maintenance")
                .callback(object : com.cloudinary.android.callback.UploadCallback {
                    override fun onStart(requestId: String?) {
                        android.util.Log.d("CloudinaryUpload", "Upload started: $requestId")
                    }
                    override fun onProgress(requestId: String?, bytes: Long, totalBytes: Long) {
                        val progress = bytes.toFloat() / totalBytes
                        _uiState.value = _uiState.value.copy(uploadProgress = progress)
                    }

                    override fun onSuccess(requestId: String?, resultData: Map<*, *>?) {
                        val url = resultData?.get("secure_url") as? String
                        android.util.Log.d("CloudinaryUpload", "Upload success: $url")
                        tempFile.delete()
                        if (url != null) cont.resume(url)
                        else cont.resumeWithException(Exception("Failed to get URL"))
                    }

                    override fun onError(requestId: String?, error: com.cloudinary.android.callback.ErrorInfo?) {
                        android.util.Log.e("CloudinaryUpload", "Upload error: ${error?.description}")
                        tempFile.delete()
                        cont.resumeWithException(Exception(error?.description ?: "Upload failed"))
                    }

                    override fun onReschedule(requestId: String?, error: com.cloudinary.android.callback.ErrorInfo?) {
                        android.util.Log.w("CloudinaryUpload", "Upload rescheduled")
                    }
                })
                .dispatch()
        } catch (e: Exception) {
            android.util.Log.e("CloudinaryUpload", "Exception in uploadImageToCloudinary", e)
            cont.resumeWithException(e)
        }
    }

    // Clear form after success (called from UI)
    fun clearFormState() {
        _uiState.value = _uiState.value.copy(currentEditRequest = null)
    }

    fun deleteRequest(requestId: String) {
        viewModelScope.launch {
            try {
                db.collection("maintenance_requests").document(requestId).delete().await()
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(error = "Failed to delete: ${e.message}")
            }
        }
    }
}

