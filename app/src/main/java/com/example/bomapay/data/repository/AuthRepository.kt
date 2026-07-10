package com.example.bomapay.data.repository

import com.example.bomapay.data.model.UserProfile
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

class AuthRepository {
    private val auth = FirebaseAuth.getInstance()
    private val firestore = FirebaseFirestore.getInstance()

    /**
     * Registers a new user inside Firebase Authentication
     * and maps their extended record parameters directly into Cloud Firestore.
     */
    suspend fun registerUser(
        email: String,
        password: String,
        fullName: String,
        phoneNumber: String,
        isLandlord: Boolean
    ): Result<Unit> {
        return try {
            val authResult = auth.createUserWithEmailAndPassword(email, password).await()
            val uid = authResult.user?.uid ?: throw Exception("User identification token generation failed.")

            val profile = UserProfile(
                uid = uid,
                email = email,
                role = if (isLandlord) "landlord" else "tenant",
                fullName = fullName,
                phoneNumber = phoneNumber,
                houseNumber = "Unassigned",
                rentBalance = 0L
            )

            firestore.collection("users").document(uid).set(profile).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Signs in an existing user using email and password.
     */
    suspend fun loginUser(email: String, password: String): Result<Unit> {
        return try {
            auth.signInWithEmailAndPassword(email.trim(), password).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Gets the unique identifier token of the currently authenticated session user.
     */
    fun getCurrentUid(): String? {
        return auth.currentUser?.uid
    }

    /**
     * Fetches the role string ("landlord" or "tenant") directly from the document entry mapping.
     */
    suspend fun getUserRole(uid: String): String? {
        return try {
            val snapshot = firestore.collection("users").document(uid).get().await()
            snapshot.getString("role")
        } catch (e: Exception) {
            null
        }
    }
}