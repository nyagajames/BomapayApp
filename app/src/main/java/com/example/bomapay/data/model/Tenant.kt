package com.example.bomapay.data.model

data class Tenant(
    val tenantId: String = "",
    val name: String = "",
    val houseNumber: String = "",
    val email: String = "",
    val rentBalance: Double = 0.0,
    val leaseEndDate: String = "",
    val status: String = "Active"
)