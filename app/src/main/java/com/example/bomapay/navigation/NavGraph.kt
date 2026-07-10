package com.example.bomapay.navigation

import androidx.compose.runtime.*
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navigation
import com.example.bomapay.data.model.UserProfile
import com.example.bomapay.ui.screens.landlord.*
import com.example.bomapay.ui.screens.tenant.*
import com.example.bomapay.ui.screens.shared.*
import com.example.bomapay.ui.viewmodel.TenantViewModel
import com.example.bomapay.ui.viewmodel.MaintenanceViewModel
import com.example.bomapay.ui.viewmodel.PaymentViewModel
import com.example.bomapay.data.repository.MpesaRepository
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

@Composable
fun NavGraph(navController: NavHostController) {
    NavHost(
        navController = navController,
        route = "root_graph",
        startDestination = Graph.SHARED
    ) {
        // --- SHARED GRAPH ---
        navigation(route = Graph.SHARED, startDestination = Screen.Splash.route) {
            composable(route = Screen.Splash.route) {
                val auth = remember { FirebaseAuth.getInstance() }
                val firestore = remember { FirebaseFirestore.getInstance() }
                SplashScreen(onSplashFinished = {
                    val currentUser = auth.currentUser
                    if (currentUser == null) {
                        navController.navigate(Screen.Login.route) { popUpTo(Screen.Splash.route) { inclusive = true } }
                    } else {
                        firestore.collection("users").document(currentUser.uid).get()
                            .addOnSuccessListener { doc ->
                                val dest = if (doc.exists() && doc.getString("role") == "landlord") Graph.LANDLORD else Graph.TENANT
                                navController.navigate(dest) { popUpTo(Screen.Splash.route) { inclusive = true } }
                            }
                            .addOnFailureListener {
                                // Fallback to Login or Tenant if Firestore fetch fails
                                navController.navigate(Screen.Login.route) { popUpTo(Screen.Splash.route) { inclusive = true } }
                            }
                    }
                })
            }
            composable(route = Screen.Login.route) {
                LoginScreen(
                    onNavigateToRegister = { navController.navigate(Screen.Register.route) },
                    onNavigateToForgotPassword = { navController.navigate(Screen.ForgotPassword.route) },
                    onLoginSuccess = { role ->
                        navController.navigate(if (role == "landlord") Graph.LANDLORD else Graph.TENANT) {
                            popUpTo(Screen.Login.route) { inclusive = true }
                        }
                    }
                )
            }
            composable(route = Screen.Register.route) {
                RegisterScreen(
                    onNavigateToLogin = { navController.popBackStack() },
                    onRegisterSuccess = { role ->
                        navController.navigate(if (role == "landlord") Graph.LANDLORD else Graph.TENANT) {
                            popUpTo(Graph.SHARED) { inclusive = true }
                        }
                    }
                )
            }
            composable(route = Screen.ForgotPassword.route) {
                ForgotPasswordScreen(onNavigateBackToLogin = { navController.popBackStack() })
            }
        }

        // --- TENANT GRAPH ---
        navigation(route = Graph.TENANT, startDestination = Screen.TenantHome.route) {
            composable(route = Screen.TenantHome.route) {
                val parentEntry = remember(it) { navController.getBackStackEntry(Graph.TENANT) }
                val viewModel: TenantViewModel = viewModel(parentEntry)
                val tenantData by viewModel.tenantUiState.collectAsState()
                val notice by viewModel.latestNotice.collectAsState()
                val auth = remember { FirebaseAuth.getInstance() }

                LaunchedEffect(auth.currentUser?.uid) {
                    auth.currentUser?.uid?.let { viewModel.fetchTenantData(it) }
                }

                TenantDashboard(
                    userProfile = tenantData ?: UserProfile(houseNumber = "Unassigned"),
                    latestNotice = notice,
                    onNavigateToPay = { navController.navigate(Screen.TenantPay.route) },
                    onNavigateToMaintenance = { navController.navigate(Screen.TenantMaintenance.route) },
                    onNavigateToProfile = { navController.navigate(Screen.TenantProfile.route) },
                    onLogout = { logoutAndReset(navController) }
                )
            }

            composable(route = Screen.TenantPay.route) {
                // Initialize MpesaRepository with Sandbox Credentials
                val mpesaRepository = remember {
                    MpesaRepository(
                        consumerKey = "wYZ9LgFmignkR2hfnXEGo9cC0TBuqQ0jAPveI2PSTxVYwZgg",
                        consumerSecret = "4Hc8eB0UtdALrIpwwGnRa7MkjDPCr8eUFhOoBQVtgk0ZE6PtOEbiURhk1Yelx2GG",
                        passKey = "bfb279f9aa9bdbcf158e97dd71a467cd2e0c893059b10f78e6b72ada1ed2c919", // Standard Sandbox Passkey
                        shortCode = "174379", // Standard Sandbox Shortcode
                        callbackUrl = "https://webhook.site/9a07dc83-5f0a-4b1e-a9c2-c376b8d0df2a" // Replace with your actual callback URL or a dummy for sandbox
                    )
                }
                val paymentViewModel: PaymentViewModel = viewModel(factory = object : androidx.lifecycle.ViewModelProvider.Factory {
                    override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T {
                        return PaymentViewModel(mpesaRepository) as T
                    }
                })

                TenantPayScreen(
                    viewModel = paymentViewModel,
                    onNavigateBack = { navController.popBackStack() }
                )
            }

            composable(route = Screen.TenantMaintenance.route) {
                val viewModel: MaintenanceViewModel = viewModel()

                TenantMaintenanceScreen(
                    viewModel = viewModel,
                    onNavigateBack = { navController.popBackStack() }
                )
            }

            composable(route = Screen.TenantProfile.route) {
                val parentEntry = remember(it) { navController.getBackStackEntry(Graph.TENANT) }
                val viewModel: TenantViewModel = viewModel(parentEntry)
                val tenantData by viewModel.tenantUiState.collectAsState()

                TenantProfileScreen(
                    userProfile = tenantData ?: UserProfile(),
                    onNavigateBack = { navController.popBackStack() }
                )
            }
        }

        // --- LANDLORD GRAPH ---
        navigation(route = Graph.LANDLORD, startDestination = Screen.LandlordHome.route) {
            composable(route = Screen.LandlordHome.route) {
                LandlordDashboard(
                    onNavigateToAssignHouse = { navController.navigate(Screen.AssignHouse.route) },
                    onNavigateToIssueNotice = { navController.navigate(Screen.IssueNotice.route) },
                    onNavigateToViewMaintenance = { navController.navigate(Screen.LandlordMaintenanceList.route) },
                    onLogout = { logoutAndReset(navController) }
                )
            }
            composable(route = Screen.AssignHouse.route) {
                AssignHouseScreen(onAssignmentComplete = { navController.popBackStack() })
            }
            composable(route = Screen.IssueNotice.route) {
                IssueNoticeScreen(onNoticeSent = { navController.popBackStack() })
            }
            composable(route = Screen.LandlordMaintenanceList.route) {
                LandlordMaintenanceList(onNavigateBack = { navController.popBackStack() })
            }
        }
    }
}

// Helper function
fun logoutAndReset(navController: NavHostController) {
    FirebaseAuth.getInstance().signOut()
    navController.navigate(Graph.SHARED) {
        popUpTo("root_graph") { inclusive = true }
    }
}