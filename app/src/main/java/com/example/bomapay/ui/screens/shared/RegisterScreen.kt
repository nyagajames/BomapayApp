package com.example.bomapay.ui.screens.shared

import android.util.Patterns
import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.bomapay.data.repository.AuthRepository
import kotlinx.coroutines.launch

@Composable
fun RegisterScreen(
    onRegisterSuccess: (String) -> Unit,
    onNavigateToLogin: () -> Unit
) {
    // New Input States for capturing profile particulars upfront
    var fullName by remember { mutableStateOf("") }
    var phoneNumber by remember { mutableStateOf("") }

    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var adminCode by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(false) }

    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val authRepository = remember { AuthRepository() }
    val scrollState = rememberScrollState()

    val isEmailError = email.isNotEmpty() && !Patterns.EMAIL_ADDRESS.matcher(email).matches()
    val isPasswordError = password.isNotEmpty() && password.length < 6
    val isPhoneError = phoneNumber.isNotEmpty() && phoneNumber.length < 10

    val SECRET_LANDLORD_CODE = "BomaAdmin2026"

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp)
            .verticalScroll(scrollState), // Added scrolling to perfectly fit all input elements comfortably
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(16.dp))
        Text(text = "Create Account", fontSize = 32.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
        Spacer(modifier = Modifier.height(24.dp))

        // --- FULL NAME FIELD ---
        OutlinedTextField(
            value = fullName,
            onValueChange = { fullName = it },
            label = { Text("Full Name") },
            placeholder = { Text("e.g. Juma Ochieng") },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text),
            singleLine = true
        )

        Spacer(modifier = Modifier.height(16.dp))

        // --- PHONE NUMBER FIELD ---
        OutlinedTextField(
            value = phoneNumber,
            onValueChange = { phoneNumber = it.trim() },
            label = { Text("Phone Number") },
            placeholder = { Text("e.g. +254 700 000000") },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            isError = isPhoneError,
            supportingText = {
                if (isPhoneError) {
                    Text("Please enter a valid telephone contact", color = MaterialTheme.colorScheme.error)
                }
            },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
            singleLine = true
        )

        Spacer(modifier = Modifier.height(16.dp))

        // --- EMAIL ADDRESS FIELD ---
        OutlinedTextField(
            value = email,
            onValueChange = { email = it.trim() },
            label = { Text("Email Address") },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            isError = isEmailError,
            supportingText = {
                if (isEmailError) {
                    Text("Please enter a valid email address", color = MaterialTheme.colorScheme.error)
                }
            },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
            singleLine = true
        )

        Spacer(modifier = Modifier.height(16.dp))

        // --- PASSWORD FIELD ---
        OutlinedTextField(
            value = password,
            onValueChange = { password = it },
            label = { Text("Password (6+ Characters)") },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            isError = isPasswordError,
            supportingText = {
                if (isPasswordError) {
                    Text("Password must be at least 6 characters long", color = MaterialTheme.colorScheme.error)
                }
            },
            visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
            trailingIcon = {
                val image = if (passwordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff
                IconButton(onClick = { passwordVisible = !passwordVisible }) {
                    Icon(imageVector = image, contentDescription = "Toggle Visibility")
                }
            },
            singleLine = true
        )

        Spacer(modifier = Modifier.height(16.dp))

        // --- LANDLORD CODE FIELD ---
        OutlinedTextField(
            value = adminCode,
            onValueChange = { adminCode = it },
            label = { Text("Landlord Admin Code (Leave blank if Tenant)") },
            placeholder = { Text("Enter property verification token") },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
            visualTransformation = PasswordVisualTransformation(),
            singleLine = true
        )

        Spacer(modifier = Modifier.height(32.dp))

        // --- SUBMIT REGISTRATION BUTTON ---
        Button(
            onClick = {
                if (fullName.isBlank() || phoneNumber.isBlank()) {
                    Toast.makeText(context, "Please supply your Name and Phone Number.", Toast.LENGTH_SHORT).show()
                    return@Button
                }

                if (email.isNotEmpty() && password.isNotEmpty() && !isEmailError && !isPasswordError && !isPhoneError) {
                    val attemptsLandlordAccess = adminCode.isNotEmpty()
                    val isAuthorizedLandlord = adminCode == SECRET_LANDLORD_CODE

                    if (attemptsLandlordAccess && !isAuthorizedLandlord) {
                        Toast.makeText(context, "Invalid Admin Verification Code!", Toast.LENGTH_LONG).show()
                        return@Button
                    }

                    isLoading = true
                    scope.launch {
                        val result = authRepository.registerUser(
                            email = email,
                            password = password,
                            fullName = fullName.trim(),
                            phoneNumber = phoneNumber.trim(),
                            isLandlord = attemptsLandlordAccess
                        )
                        isLoading = false
                        if (result.isSuccess) {
                            onRegisterSuccess(if (attemptsLandlordAccess) "landlord" else "tenant")
                        } else {
                            Toast.makeText(context, "Registration Failed: ${result.exceptionOrNull()?.message}", Toast.LENGTH_LONG).show()
                        }
                    }
                } else {
                    Toast.makeText(context, "Please complete form inputs accurately.", Toast.LENGTH_SHORT).show()
                }
            },
            modifier = Modifier.fillMaxWidth().height(50.dp),
            shape = RoundedCornerShape(12.dp),
            enabled = !isLoading
        ) {
            if (isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(24.dp),
                    color = MaterialTheme.colorScheme.onPrimary,
                    strokeWidth = 2.5.dp
                )
            } else {
                Text("Register", fontSize = 16.sp, fontWeight = FontWeight.Bold)
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "Already have an account? Sign In",
            modifier = Modifier.clickable { onNavigateToLogin() },
            color = MaterialTheme.colorScheme.outline
        )
        Spacer(modifier = Modifier.height(16.dp))
    }
}