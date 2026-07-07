package com.example.bomapay.navigation

object Graph {
    const val SHARED = "shared_graph"
    const val TENANT = "tenant_graph"
    const val LANDLORD = "landlord_graph"
}

sealed class Screen(val route: String) {
    object Splash : Screen("splash")
    object Login : Screen("login")
    object Register : Screen("register")
    object ForgotPassword : Screen("forgot_password")
    object TenantHome : Screen("tenant_home")
    object TenantPay : Screen("tenant_pay")
    object TenantMaintenance : Screen("tenant_maintenance")
    object TenantProfile : Screen("tenant_profile")
    object LandlordHome : Screen("landlord_home")
    object AssignHouse : Screen("assign_house")
    object IssueNotice : Screen("issue_notice")
    object LandlordMaintenanceList : Screen("landlord_maintenance_list")
}