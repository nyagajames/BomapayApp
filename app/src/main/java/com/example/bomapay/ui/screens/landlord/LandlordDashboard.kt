package com.example.bomapay.ui.screens.landlord

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.bomapay.data.model.UserProfile
import com.google.firebase.firestore.FirebaseFirestore

// Data Models
data class QuickMetric(val title: String, val value: String, val containerColor: Color)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LandlordDashboard(
    onNavigateToAssignHouse: () -> Unit = {},
    onNavigateToIssueNotice: () -> Unit = {},
    onNavigateToViewMaintenance: () -> Unit = {},
    onLogout: () -> Unit = {}
) {
    val db = FirebaseFirestore.getInstance()
    var pendingCount by remember { mutableStateOf(0) }
    var tenants by remember { mutableStateOf(listOf<UserProfile>()) }

    // Observe maintenance requests count
    LaunchedEffect(Unit) {
        db.collection("maintenance_requests").addSnapshotListener { snapshot, _ ->
            pendingCount = snapshot?.size() ?: 0
        }
    }

    // Observe registered tenants
    LaunchedEffect(Unit) {
        db.collection("users")
            .whereEqualTo("role", "tenant")
            .addSnapshotListener { snapshot, _ ->
                tenants = snapshot?.documents?.mapNotNull { it.toObject(UserProfile::class.java) } ?: emptyList()
            }
    }

    val metrics = listOf(
        QuickMetric("Collected", "KSh ---", Color(0xFFE8F5E9)),
        QuickMetric("Tenants", "${tenants.size}", Color(0xFFE3F2FD)),
        QuickMetric("Pending", "$pendingCount Tickets", Color(0xFFFFF3E0))
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("BomaPay", fontSize = 20.sp, fontWeight = FontWeight.Bold)
                        Text("Landlord Portal", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                },
                actions = {
                    IconButton(onClick = onNavigateToIssueNotice) {
                        Icon(Icons.Default.Notifications, contentDescription = "Notices")
                    }
                    IconButton(onClick = onLogout) {
                        Icon(Icons.AutoMirrored.Filled.ExitToApp, contentDescription = "Logout")
                    }
                }
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(innerPadding).padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Text("Welcome back, Landlord Office", fontSize = 24.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(vertical = 8.dp))
            }

            // Quick Metrics Row
            item {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    metrics.forEach { metric ->
                        Card(modifier = Modifier.weight(1f), colors = CardDefaults.cardColors(containerColor = metric.containerColor)) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Text(metric.title, fontSize = 12.sp, color = Color.Gray)
                                Text(metric.value, fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color.Black)
                            }
                        }
                    }
                }
            }

            // Quick Controls Block
            item {
                Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("Quick Controls", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Button(onClick = onNavigateToAssignHouse, modifier = Modifier.weight(1f)) {
                                Text("Assign House", fontSize = 11.sp)
                            }
                            Button(
                                onClick = onNavigateToViewMaintenance,
                                modifier = Modifier.weight(1f),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (pendingCount > 0) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondary
                                )
                            ) {
                                Text("Maintenance ($pendingCount)", fontSize = 11.sp)
                            }
                        }
                    }
                }
            }

            // Tenants List
            item { Text("Registered Tenants", fontSize = 18.sp, fontWeight = FontWeight.SemiBold) }
            
            if (tenants.isEmpty()) {
                item {
                    Text("No tenants registered yet.", modifier = Modifier.padding(16.dp), color = Color.Gray)
                }
            }

            items(tenants) { tenant ->
                Card(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                    Row(modifier = Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(modifier = Modifier.size(40.dp).background(MaterialTheme.colorScheme.primaryContainer, CircleShape), contentAlignment = Alignment.Center) {
                                Icon(Icons.Default.Person, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(tenant.fullName.ifBlank { "New Tenant" }, fontWeight = FontWeight.Bold)
                                Text("House: ${tenant.houseNumber}", fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                        Box(modifier = Modifier.background(if (tenant.rentBalance <= 0) Color(0xFFC8E6C9) else Color(0xFFFFCDD2), RoundedCornerShape(50)).padding(horizontal = 12.dp, vertical = 4.dp)) {
                            Text(if (tenant.rentBalance <= 0) "Paid" else "Due", fontSize = 12.sp, fontWeight = FontWeight.Medium)
                        }
                    }
                }
            }
        }
    }
}