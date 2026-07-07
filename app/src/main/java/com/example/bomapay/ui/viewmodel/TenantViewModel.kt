package com.example.bomapay.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.bomapay.data.model.UserProfile
import com.example.bomapay.data.model.Notice
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class TenantViewModel : ViewModel() {
    private val db = FirebaseFirestore.getInstance()

    private val _tenantUiState = MutableStateFlow<UserProfile?>(null)
    val tenantUiState: StateFlow<UserProfile?> = _tenantUiState

    private val _latestNotice = MutableStateFlow<Notice?>(null)
    val latestNotice: StateFlow<Notice?> = _latestNotice

    private var noticeListener: com.google.firebase.firestore.ListenerRegistration? = null

    fun fetchTenantData(uid: String) {
        viewModelScope.launch {
            try {
                val doc = db.collection("users").document(uid).get().await()
                _tenantUiState.value = doc.toObject(UserProfile::class.java)

                // Real-time listener for the latest notice
                noticeListener?.remove()
                noticeListener = db.collection("notices")
                    .orderBy("createdAt", com.google.firebase.firestore.Query.Direction.DESCENDING)
                    .limit(1)
                    .addSnapshotListener { snapshot, _ ->
                        _latestNotice.value = snapshot?.documents?.firstOrNull()?.toObject(Notice::class.java)
                    }
            } catch (e: Exception) {
                // Handle error
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        noticeListener?.remove()
    }
}