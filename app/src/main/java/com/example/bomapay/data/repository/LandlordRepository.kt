package com.example.bomapay.data.repository

import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import com.example.bomapay.data.model.Notice

class LandlordRepository {
    private val firestore = FirebaseFirestore.getInstance()
    private val usersCollection = firestore.collection("users")
    private val noticesCollection = firestore.collection("notices")

    // 🏠 Assign a house number and rent balance to a tenant by email
    suspend fun assignHouseToTenant(tenantEmail: String, assignedHouse: String, initialRent: Long): Result<Unit> {
        return try {
            val querySnapshot = usersCollection
                .whereEqualTo("email", tenantEmail.trim())
                .whereEqualTo("role", "tenant")
                .get()
                .await()

            if (querySnapshot.isEmpty) {
                return Result.failure(Exception("No tenant found with email $tenantEmail"))
            }

            val documentId = querySnapshot.documents.first().id

            usersCollection.document(documentId).update(
                mapOf(
                    "houseNumber" to assignedHouse,
                    "rentBalance" to initialRent
                )
            ).await()

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // 📢 Send out an official notice to Firestore
    suspend fun sendNotice(title: String, message: String, targetHouse: String): Result<Unit> {
        return try {
            val docRef = noticesCollection.document()
            val notice = Notice(
                noticeId = docRef.id,
                title = title,
                message = message,
                targetHouse = targetHouse.ifBlank { "All" }
            )

            docRef.set(notice.toMap()).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}