package com.example.bomapay.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.bomapay.data.repository.MpesaRepository
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class PaymentUiState(
    val isLoading: Boolean = false,
    val paymentSuccess: Boolean = false,
    val message: String? = null,
    val error: String? = null
)

class PaymentViewModel(
    private val mpesaRepository: MpesaRepository
) : ViewModel() {

    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    private val _uiState = MutableStateFlow(PaymentUiState())
    val uiState: StateFlow<PaymentUiState> = _uiState.asStateFlow()

    fun initiatePayment(phoneNumber: String, amount: Int, accountRef: String) {
        val uid = auth.currentUser?.uid ?: return
        
        viewModelScope.launch {
            _uiState.value = PaymentUiState(isLoading = true)
            val result = mpesaRepository.initiateStkPush(phoneNumber, amount, accountRef)
            
            result.onSuccess { response ->
                // SAVE PENDING PAYMENT TO FIRESTORE
                val pendingPayment = mapOf(
                    "uid" to uid,
                    "checkoutRequestID" to response.checkoutRequestID,
                    "merchantRequestID" to response.merchantRequestID,
                    "amount" to amount,
                    "phoneNumber" to phoneNumber,
                    "status" to "Pending",
                    "timestamp" to System.currentTimeMillis()
                )
                
                db.collection("payments").document(response.checkoutRequestID).set(pendingPayment)

                _uiState.value = _uiState.value.copy(
                    message = "STK Push sent! Please enter your PIN."
                )

                // START POLLING (Wait 10 seconds first)
                kotlinx.coroutines.delay(10000)
                
                var statusFound = false
                var attempts = 1
                val maxAttempts = 15 // 15 attempts * 5s = 75 seconds + initial delay

                while (!statusFound && attempts <= maxAttempts) {
                    val currentMessage = "Verifying payment (Attempt $attempts/$maxAttempts)..."
                    _uiState.value = _uiState.value.copy(message = currentMessage)

                    val queryResult = mpesaRepository.queryPaymentStatus(response.checkoutRequestID)
                    
                    queryResult.onSuccess { queryResponse ->
                        if (queryResponse.resultCode == "0") {
                            updateFirestoreBalance(uid, amount.toLong(), response.checkoutRequestID)
                            _uiState.value = PaymentUiState(
                                isLoading = false,
                                paymentSuccess = true,
                                message = "Payment Successful! Your balance has been updated."
                            )
                            statusFound = true
                        } else if (queryResponse.resultCode != null) {
                            _uiState.value = PaymentUiState(
                                isLoading = false,
                                error = "Payment failed: ${queryResponse.resultDesc}"
                            )
                            statusFound = true
                        }
                    }.onFailure { e ->
                        // In Sandbox, "Processing" often comes back as an error.
                        // We show the error message so the user knows it's still working.
                        val errorMsg = e.message ?: "Still waiting for Safaricom..."
                        _uiState.value = _uiState.value.copy(
                            message = "Status: $errorMsg (Attempt $attempts)"
                        )
                    }

                    if (!statusFound) {
                        attempts++
                        kotlinx.coroutines.delay(5000)
                    }
                }

                if (!statusFound) {
                    _uiState.value = PaymentUiState(
                        isLoading = false,
                        error = "We couldn't confirm the payment yet. Please check your balance in 1 minute."
                    )
                }

            }.onFailure { exception ->
                _uiState.value = PaymentUiState(
                    isLoading = false,
                    error = exception.message ?: "Payment initiation failed"
                )
            }
        }
    }

    private suspend fun updateFirestoreBalance(uid: String, amount: Long, checkoutID: String) {
        try {
            // 1. Update the User's balance
            db.collection("users").document(uid)
                .update("rentBalance", com.google.firebase.firestore.FieldValue.increment(-amount))
            
            // 2. Mark the payment record as Success
            db.collection("payments").document(checkoutID)
                .update("status", "Success", "completedAt", com.google.firebase.firestore.FieldValue.serverTimestamp())
        } catch (e: Exception) {
            android.util.Log.e("PaymentViewModel", "Error updating balance", e)
        }
    }

    fun clearState() {
        _uiState.value = PaymentUiState()
    }
}
