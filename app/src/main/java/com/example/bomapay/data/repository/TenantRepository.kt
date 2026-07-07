package com.example.bomapay.data.repository


import com.example.bomapay.data.model.Tenant
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

class TenantRepository {
    private val db = FirebaseFirestore.getInstance()
    private val tenantsRef = db.collection("tenants")

    suspend fun getTenantDetails(tenantId: String): Tenant? {
        return tenantsRef.document(tenantId).get().await().toObject(Tenant::class.java)
    }

    // Additional methods for paying rent or maintenance requests go here
}