package com.example.bomapay.ui.screens.landlord

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LandlordMaintenanceList(onNavigateBack: () -> Unit) {
    val db = FirebaseFirestore.getInstance()
    // Storing requests as a list of Maps that include the document ID
    var requests by remember { mutableStateOf(listOf<Map<String, Any>>()) }

    LaunchedEffect(Unit) {
        db.collection("maintenance_requests")
            .addSnapshotListener { snapshot, _ ->
                requests = snapshot?.documents?.mapNotNull { doc ->
                    doc.data?.toMutableMap()?.apply { put("id", doc.id) }
                }?.sortedByDescending { it["timestamp"] as? Long ?: 0L } ?: emptyList()
            }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Maintenance Requests", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            items(requests) { request ->
                val requestId = request["id"] as? String ?: ""

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("House: ${request["houseNumber"]}", fontWeight = FontWeight.Bold)
                            Text("Status: ${request["status"] ?: "Pending"}", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(request["description"] as? String ?: "")

                        Spacer(modifier = Modifier.height(12.dp))
                        AsyncImage(
                            model = request["imageUrl"] as? String ?: "",
                            contentDescription = "Maintenance Photo",
                            modifier = Modifier.fillMaxWidth().height(150.dp),
                            contentScale = ContentScale.Crop
                        )

                        Spacer(modifier = Modifier.height(16.dp))
                        // Status Update Buttons
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Button(onClick = { updateStatus(requestId, "In Progress") }, modifier = Modifier.weight(1f)) {
                                Text("In Progress")
                            }
                            Button(onClick = { updateStatus(requestId, "Resolved") }, modifier = Modifier.weight(1f)) {
                                Text("Resolved")
                            }
                        }
                    }
                }
            }
        }
    }
}

fun updateStatus(requestId: String, newStatus: String) {
    FirebaseFirestore.getInstance()
        .collection("maintenance_requests")
        .document(requestId)
        .update("status", newStatus)
}