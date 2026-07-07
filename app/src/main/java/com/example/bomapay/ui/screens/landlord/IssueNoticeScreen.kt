package com.example.bomapay.ui.screens.landlord

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.bomapay.data.repository.LandlordRepository
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun IssueNoticeScreen(
    onNoticeSent: () -> Unit
) {
    val repository = remember { LandlordRepository() }
    val scope = rememberCoroutineScope()
    val db = remember { com.google.firebase.firestore.FirebaseFirestore.getInstance() }

    var targetHouse by remember { mutableStateOf("All") }
    var noticeTitle by remember { mutableStateOf("") }
    var noticeMessage by remember { mutableStateOf("") }

    var houseNumbers by remember { mutableStateOf(listOf("All")) }
    var expanded by remember { mutableStateOf(false) }

    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    // Fetch existing house numbers from registered tenants
    LaunchedEffect(Unit) {
        db.collection("users")
            .whereEqualTo("role", "tenant")
            .get()
            .addOnSuccessListener { snapshot ->
                val houses = snapshot.documents
                    .mapNotNull { it.getString("houseNumber") }
                    .filter { it.isNotBlank() && it != "Unassigned" }
                    .distinct()
                    .sorted()
                houseNumbers = listOf("All") + houses
            }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Create Notice") },
                navigationIcon = {
                    IconButton(onClick = onNoticeSent) { // Navigate back
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Dropdown for Target Selection
            ExposedDropdownMenuBox(
                expanded = expanded,
                onExpandedChange = { expanded = !expanded },
                modifier = Modifier.fillMaxWidth()
            ) {
                OutlinedTextField(
                    value = targetHouse,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Target Audience") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                    modifier = Modifier.menuAnchor().fillMaxWidth(),
                    enabled = !isLoading
                )
                ExposedDropdownMenu(
                    expanded = expanded,
                    onDismissRequest = { expanded = false }
                ) {
                    houseNumbers.forEach { house ->
                        DropdownMenuItem(
                            text = { Text(house) },
                            onClick = {
                                targetHouse = house
                                expanded = false
                            }
                        )
                    }
                }
            }

            OutlinedTextField(
                value = noticeTitle,
                onValueChange = { noticeTitle = it },
                label = { Text("Notice Title") },
                modifier = Modifier.fillMaxWidth(),
                enabled = !isLoading
            )
            OutlinedTextField(
                value = noticeMessage,
                onValueChange = { noticeMessage = it },
                label = { Text("Message Body") },
                modifier = Modifier.fillMaxWidth(),
                minLines = 4,
                enabled = !isLoading
            )

            if (errorMessage != null) {
                Text(errorMessage!!, color = MaterialTheme.colorScheme.error)
            }

            Button(
                onClick = {
                    if (noticeTitle.isBlank() || noticeMessage.isBlank()) {
                        errorMessage = "Title and Message cannot be empty"
                        return@Button
                    }

                    isLoading = true
                    errorMessage = null

                    scope.launch {
                        val result = repository.sendNotice(noticeTitle, noticeMessage, targetHouse)
                        isLoading = false
                        if (result.isSuccess) {
                            onNoticeSent()
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
                    Text("Broadcast Notice")
                }
            }
        }
    }
}