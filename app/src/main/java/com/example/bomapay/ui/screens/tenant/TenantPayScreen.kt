package com.example.bomapay.ui.screens.tenant

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier

@Composable
fun TenantPayScreen(
    onNavigateBack: () -> Unit // Accepts the callback from NavGraph
) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text("Tenant Rent Payment History & Actions")
    }
}