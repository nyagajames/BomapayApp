package com.example.bomapay.ui.screens.landlord

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.bomapay.data.repository.LandlordRepository
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AssignHouseScreen(
    onAssignmentComplete: () -> Unit
) {
    val repository = remember { LandlordRepository() }
    val scope = rememberCoroutineScope()

    var tenantEmail by remember { mutableStateOf("") }
    var houseNumber by remember { mutableStateOf("") }
    var rentAmount by remember { mutableStateOf("") }

    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    Scaffold(
        topBar = { TopAppBar(title = { Text("Assign House Number") }) }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            OutlinedTextField(
                value = tenantEmail,
                onValueChange = { tenantEmail = it },
                label = { Text("Tenant Email") },
                modifier = Modifier.fillMaxWidth(),
                enabled = !isLoading
            )
            OutlinedTextField(
                value = houseNumber,
                onValueChange = { houseNumber = it },
                label = { Text("House Number (e.g., A4)") },
                modifier = Modifier.fillMaxWidth(),
                enabled = !isLoading
            )
            OutlinedTextField(
                value = rentAmount,
                onValueChange = { rentAmount = it },
                label = { Text("Initial Rent Balance (KSh)") },
                modifier = Modifier.fillMaxWidth(),
                enabled = !isLoading
            )

            if (errorMessage != null) {
                Text(errorMessage!!, color = MaterialTheme.colorScheme.error)
            }

            Button(
                onClick = {
                    if (tenantEmail.isBlank() || houseNumber.isBlank()) {
                        errorMessage = "Please fill in all fields"
                        return@Button
                    }

                    isLoading = true
                    errorMessage = null
                    val rentLong = rentAmount.toLongOrNull() ?: 0L

                    scope.launch {
                        val result = repository.assignHouseToTenant(tenantEmail, houseNumber, rentLong)
                        isLoading = false
                        if (result.isSuccess) {
                            onAssignmentComplete()
                        } else {
                            errorMessage = result.exceptionOrNull()?.message
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = !isLoading
            ) {
                if (isLoading) {
                    CircularProgressIndicator(modifier = Modifier.size(24.dp), color = MaterialTheme.colorScheme.onPrimary)
                } else {
                    Text("Confirm Assignment")
                }
            }
        }
    }
}