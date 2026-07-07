package com.example.bomapay.data.model

data class UserProfile(
    val uid: String = "",
    val email: String = "",
    val role: String = "tenant",
    val houseNumber: String = "Unassigned",
    val rentBalance: Long = 0,
    val createdAt: Long = System.currentTimeMillis(),
    val fullName: String = "",       // Added to resolve TenantProfileScreen compile error
    val phoneNumber: String = ""     // Added to resolve TenantProfileScreen compile error
) {
    fun toMap(): Map<String, Any?> {
        return mapOf(
            "uid" to uid,
            "email" to email,
            "role" to role,
            "houseNumber" to houseNumber,
            "rentBalance" to rentBalance,
            "createdAt" to createdAt,
            "fullName" to fullName,
            "phoneNumber" to phoneNumber
        )
    }
}