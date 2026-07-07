# BomaPay - Property Management System

BomaPay is a modern Android application designed to streamline property management between landlords and tenants. Built with Jetpack Compose and Firebase, it provides a seamless interface for handling rent payments, maintenance requests, and communication.

## Features

### 🏠 For Landlords
- **Dashboard Overview**: Track total rent collected, number of tenants, and pending maintenance tickets at a glance.
- **House Assignment**: Easily assign units to registered tenants.
- **Maintenance Management**: View and respond to maintenance requests submitted by tenants.
- **Notice System**: Broadcast notifications and notices to all tenants.
- **Tenant Tracking**: Monitor tenant lists and their rent payment status (Paid vs. Due).

### 🔑 For Tenants
- **Maintenance Requests**: Submit and track the status of maintenance issues in their units.
- **Rent Management**: View rent balances and payment history.
- **Notifications**: Receive important updates and notices from the landlord.

## Tech Stack

- **UI Framework**: [Jetpack Compose](https://developer.android.com/jetpack/compose) (Material 3)
- **Backend**: [Firebase](https://firebase.google.com/) (Firestore for database, Auth for user management)
- **Image Management**: [Cloudinary](https://cloudinary.com/) (For uploading and hosting maintenance request images)
- **Navigation**: Compose Navigation
- **Image Loading**: [Coil](https://coil-kt.github.io/coil/)
- **Asynchronous Programming**: Kotlin Coroutines

## Getting Started

### Prerequisites
- Android Studio Ladybug (or newer)
- Firebase Project
- Cloudinary Account (for image uploads)

### Setup
1. **Clone the repository**:
   ```bash
   git clone https://github.com/yourusername/Bomapay.git
   ```
2. **Firebase Setup**:
   - Add your `google-services.json` file to the `app/` directory.
   - Enable Email/Password authentication in the Firebase Console.
   - Create a Firestore database.
3. **Cloudinary Setup**:
   - Configure your Cloudinary credentials in the application (check `build.gradle.kts` or your configuration files).
4. **Build and Run**:
   - Sync the project with Gradle files.
   - Run the app on an emulator or physical device.

## Project Structure
- `ui/screens`: Contains the Compose screens for both Landlord and Tenant portals.
- `ui/viewmodel`: Logic handling and state management using ViewModels.
- `data/model`: Data classes representing Users, Tenants, and Maintenance Requests.
- `data/repository`: Data abstraction layer for Firestore operations.
- `navigation`: Navigation graph defining the app's routing.

---
Developed as a robust solution for modern property management.
