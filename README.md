# CampusConnect

CampusConnect is an Android app built with Jetpack Compose to help students and campus admins stay in sync.

It supports role-based experiences (student/admin), real-time announcements and events, club management, and an AI assistant that can answer questions using live campus data.

## Features

- Role-based authentication with Firebase Auth (`student` and `admin`)
- Student dashboard:
  - View campus announcements
  - Browse upcoming events
  - Join/leave clubs
  - Access club announcements (for joined clubs)
  - Ask the Campus AI Assistant
- Admin dashboard:
  - Post, edit, and delete campus announcements
  - Create, edit, and delete events (with optional image upload)
  - Create, edit, and delete clubs (with optional image upload)
  - View club members
  - Post club-specific announcements
- Real-time data updates via Firestore snapshot listeners
- AI chat backed by Gemini API with campus context (announcements, events, clubs, club updates)

## Tech Stack

- Kotlin + Jetpack Compose (Material 3)
- AndroidX Lifecycle ViewModel + StateFlow
- Firebase:
  - Authentication
  - Cloud Firestore
  - Cloud Storage
- Google Services Gradle plugin
- Coil (image loading)

## Project Structure

```text
app/src/main/java/com/example/campusconnect/
  auth/            Login, register, and auth state
  dashboard/       Student/Admin dashboards
  announcements/   Announcement models, screens, view model
  events/          Event models, screens, view model
  clubs/           Club models, screens, view model
  ai/              Campus AI chat screen + view model
  ui/theme/        Theme, colors, typography
  ui/components/   Shared UI components
```

## Requirements

- Android Studio (latest stable recommended)
- JDK 11
- Android SDK:
  - `compileSdk = 35`
  - `targetSdk = 35`
  - `minSdk = 24`
- A Firebase project
- A Gemini API key

## Setup

### 1) Clone and open

```bash
git clone <your-repo-url>
cd campusconnect
```

Open the project in Android Studio.

### 2) Firebase configuration

1. Create a Firebase project.
2. Add an Android app with package name:
   - `com.example.campusconnect`
3. Download `google-services.json` and place it at:
   - `app/google-services.json`
4. Enable these Firebase services:
   - Authentication (Email/Password)
   - Cloud Firestore
   - Cloud Storage

### 3) Gemini API key

This app reads `GEMINI_API_KEY` from either:
- `gradle.properties` (as Gradle property), or
- `local.properties`

Use one of these options:

```properties
# local.properties
GEMINI_API_KEY=your_key_here
```

or

```properties
# gradle.properties
GEMINI_API_KEY=your_key_here
```

### 4) Firestore data model

Expected top-level collections:

- `users/{uid}`
  - fields: `name`, `email`, `role`, `createdAt`
  - subcollection: `clubs/{clubId}`
- `announcements/{announcementId}`
- `events/{eventId}`
- `clubs/{clubId}`
  - subcollection: `members/{uid}`
  - subcollection: `announcements/{announcementId}`

### 5) Run the app

From Android Studio: Run on emulator/device.

Or via CLI:

```bash
./gradlew assembleDebug
```

Windows:

```powershell
.\gradlew.bat assembleDebug
```

## Default Admin Access

Admin role assignment is currently controlled by a secret admin code in `AuthViewModel`.

- File: `app/src/main/java/com/example/campusconnect/auth/AuthViewModel.kt`
- Update `SECRET_ADMIN_CODE` before production use.

## Notes

- Network access is required (`INTERNET` permission is declared).
- Event and club images are uploaded to Firebase Storage.
- AI chat history is stored per user in Firestore (`users/{uid}/ai_chats`).
- The dashboard includes placeholders like `Messages`, `Reports & Analytics`, and `System Settings` that can be expanded in future iterations.

## Troubleshooting

- **`Missing GEMINI_API_KEY in local.properties.`**
  - Add `GEMINI_API_KEY` to `local.properties` or `gradle.properties`, then rebuild.
- **Firebase auth/login issues**
  - Confirm Email/Password sign-in is enabled in Firebase Auth.
- **No Firestore data appears**
  - Check Firestore rules and verify your collections/fields match expected names.
- **Image upload fails**
  - Confirm Firebase Storage is enabled and storage rules allow the operation.

## Roadmap Ideas

- Move from manual screen switching to Navigation Compose
- Add proper role/permission enforcement with Firebase Security Rules
- Add messaging module implementation
- Add analytics and admin reporting
- Add automated tests for ViewModels and UI flows

## License

Add your preferred license (MIT/Apache-2.0/etc.) in a `LICENSE` file.
