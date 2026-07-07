package com.example.bomapay

import android.app.Application
import android.util.Log
import com.cloudinary.android.MediaManager

class BomaPayApplication : Application() {

    override fun onCreate() {
        super.onCreate()

        try {
            // Check if MediaManager is already initialized to avoid crashes
            val config = mapOf(
                "cloud_name" to "dj2jsxrto",
                "api_key" to "729588195655316",
                "api_secret" to "HivsB7uxsUwGv-wCxkLK-_JyPzc" // Ensure this is your REAL api_secret from Cloudinary
            )
            MediaManager.init(this, config)
            Log.d("BomaPayApp", "Cloudinary initialized successfully")
        } catch (e: Exception) {
            Log.e("BomaPayApp", "Cloudinary initialization error (already init or bad config): ${e.message}")
        }
    }
}