package com.example.bomapay

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.navigation.compose.rememberNavController
import com.cloudinary.android.MediaManager
import com.example.bomapay.navigation.NavGraph
import com.example.bomapay.ui.theme.BomapayTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            BomapayTheme {
                val navController = rememberNavController()
                NavGraph(navController = navController)
            }
        }
    }
}