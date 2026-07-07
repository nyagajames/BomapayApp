package com.example.bomapay.data.model

data class MaintenanceRequest(
    val id: String = "",
    val tenantId: String = "",
    val description: String = "",
    val status: String = "Pending", // e.g., Pending, In Progress, Resolved
    val timestamp: Long = System.currentTimeMillis()
)