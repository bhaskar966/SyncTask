# 📱 Sync Task: Cross-Platform Reminders, Perfectly Synced.

<div align="center">

**Built for [RevenueCat Shipyard](https://www.shipyard.fyi/)**

*"A beautiful fully functional reminders app that works properly on both iOS and Android."* - Sam Beckman's Brief

[![Watch the Demo](https://img.youtube.com/vi/XfObFwvGFFU/0.jpg)](https://www.youtube.com/watch?v=XfObFwvGFFU)

[![Get it on Google Play](https://play.google.com/intl/en_us/badges/static/images/badges/en_badge_web_generic.png)](https://play.google.com/store/apps/details?id=com.bhaskar.synctask)
*(Closed Testing - Invitation Required)*

[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](https://opensource.org/licenses/MIT)
[![Kotlin](https://img.shields.io/badge/Kotlin-2.3.0-purple.svg)](https://kotlinlang.org)
[![Compose Multiplatform](https://img.shields.io/badge/Compose_Multiplatform-1.10.0-blue.svg)](https://www.jetbrains.com/lp/compose-multiplatform/)
[![RevenueCat](https://img.shields.io/badge/RevenueCat-Shipyard-tomato.svg)](https://www.revenuecat.com)

[Features](#-features-meeting-the-brief) • [Hackathon Story](#-the-hackathon-story) • [Tech Stack](#-tech-stack) • [Setup Guide](#-setup-guide)

[![Technical Deep Dive](https://img.shields.io/badge/Read-Technical_Docs-black?style=for-the-badge&logo=github)](assets/TECHNICAL_DOCS.md)

</div>

---

## 💡 Inspiration

We've all been there: you dismiss a reminder on your phone, but it stays on your tablet. Most reminder apps claim to sync, but they only work when the app is open—a fundamental flaw in a multi-device world.

**Sync Task** was born from this frustration. When Sam Beckman proposed building "powerful reminders with cross-device sync" for the RevenueCat Shipyard Hackathon, we saw an opportunity to solve this problem the right way—using **Firebase Cloud Functions** to wake sleeping apps and maintain perfect synchronization, even when apps are completely closed.

---

## 📖 The Brief & The Solution

**The Challenge:** Sam Beckman needs a reminder app that:
1.  **Syncs Properly:** Dismiss on one device, it disappears everywhere.
2.  **Custom Snoozing:** Snooze for *exactly* 22 minutes without opening the app.
3.  **Powerful Recurrence:** "Every 3 days", "Every 6 months".
4.  **Beautiful Design:** Must look great and feel smooth on both iOS and Android.

**The Solution:** A **Local-First, Cloud-Synced** architecture that doesn't just sync data, but syncs *state*.

| Feature                   | How Sync Task Solves It                                                                                                                                                    |
|:--------------------------|:---------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| **Dead App Sync** 💀      | We use **Firebase Cloud Functions** to listen for changes and wake up closed apps via high-priority FCM messages. Sync happens even if you haven't opened the app in days. |
| **Smart Snooze** 💤       | **Android Overlay Activities** allow you to type *any* minute duration directly from the notification. No opening the app, no preset limits.                               |
| **Complex Recurrence** 🔁 | Custom recurrence engine supports "Every X Days/Weeks/Months", specific days of the week, and smart end dates.                                                             |
| **Native Feel** 📱        | **Compose Multiplatform** ensures pixel-perfect UI, while platform-specific code handles native notifications, precise alarms (Android), and Time Sensitive alerts (iOS).  |

---

## 🚀 Features: Meeting the Brief

### 1. True Cross-Device Sync
*   **Real-Time:** Firestore listeners propagate changes in <1 second.
*   **Background Sync:** If you complete a task on Android, your iPad (sitting in a drawer) wakes up silently, updates its local DB, and cancels the pending notification. **No more double notifications.**
*   **Conflict Resolution:** Last-Write-Wins strategy based on high-precision timestamps.

### 2. Powerful Notification Actions
*   **Android:**
    *   **Custom Snooze Overlay:** A transparent activity pops up over your lock screen/app to let you type a specific snooze duration.
    *   **Reschedule:** Pick a new date/time instantly.
*   **iOS:**
    *   **Native Actions:** Long-press to Snooze (1h, Tomorrow) or Complete.
    *   **Time Sensitive:** Critical reminders break through Focus modes.

### 3. Advanced Organization (Free & Premium)
*   **Free:** 15 active reminders, 3 groups, basic recurrence.
*   **Premium ($3.99/mo):**
    *   Unlimited reminders & groups.
    *   **Advanced Recurrence:** "Every 3rd Friday", "Every 2 days ending in December".
    *   **Subtasks:** Break down "Publish Video" into "Script", "Film", "Edit".
    *   **Tags:** Color-coded organization.

---

## 🛠 Tech Stack

**Sync Task** is a showcase of modern **Kotlin Multiplatform (KMP)** development.

### Shared Core (CommonMain)
*   **Language:** Kotlin 2.3.0
*   **UI:** Compose Multiplatform 1.10.0
*   **Architecture:** MVVM with Clean Architecture.
*   **DI:** **Koin 4.1.1** (Annotation-based, scoped ViewModels).
*   **Database:** **Room KMP 2.8.4** (SQLite) - Single Source of Truth for the UI.
*   **Networking:** **Ktor 3.4.0** + **GitLive Firebase** (Firestore, Auth).
*   **Business Logic:** `RecurrenceService`, `NotificationCalculator` (Shared 100%).

### Platform-Specific Superpowers
*   **Android:**
    *   `AlarmManager` for exact-time scheduling.
    *   `SnoozeDialogActivity` (Transparent theme) for the custom snooze UI.
    *   **Material 3** theming.
*   **iOS:**
    *   `UNUserNotificationCenter` for local scheduling.
    *   SwiftUI interoperability for specific native views.
    *   **Ktor Darwin** engine for networking.

### Backend (Serverless)
*   **Firebase Cloud Functions (Node.js):** The "Glue" that makes sync work.
    *   `onReminderStatusChanged`: Detects when a reminder is done on one device -> Sends silent push to others.
    *   `onReminderCreated/Deleted`: Keeps all devices in sync.
*   **Firebase Cloud Messaging (FCM):** High-priority data messages to wake apps.

---

## 🏗 Architecture & Logic

**Sync Task** is built on a "Local-First" philosophy. Updates commit to the local database immediately for instant UI responsiveness, then sync to the cloud in the background.

> **Want the deep dive?** Check out our [Technical Documentation](assets/TECHNICAL_DOCS.md) for full architecture diagrams, conflict resolution strategies, and code snippets.

### The Sync Flow (Simplified)
1.  **User Action:** You complete a task on your Android phone.
2.  **Local Commit:** Room DB updates, UI reflects change instantly.
3.  **Cloud Sync:** The app pushes the change to Firestore.
4.  **Cloud Function:** A Node.js function detects the change and sends a silent **FCM Data Message** to your iPad.
5.  **Remote Wake-Up:** The iPad (even if closed) wakes up, fetches the update, and cancels the notification.

No more "ghost notifications" on your other devices. 👻🚫

---

## 🔧 Setup Guide

### Prerequisites
*   Android Studio Ladybug+
*   Xcode 15+ (for iOS)
*   JDK 17
*   RevenueCat Account & API Keys
*   Firebase Project (Blaze Plan for Cloud Functions - Free tier generous)

### 1. Clone & Configure
```bash
git clone https://github.com/bhaskar966/SyncTask.git
cd SyncTask
```

### 2. Secrets Management
Create `composeApp/local.properties` (Android) and `iosApp/Configuration/Secrets.xcconfig` (iOS).

**Android (`local.properties`):**
```properties
GOOGLE_WEB_CLIENT_ID=your-client-id.apps.googleusercontent.com
REVENUECAT_API_KEY=goog_your_revenuecat_key
```

**iOS (`Secrets.xcconfig`):**
```properties
GOOGLE_IOS_CLIENT_ID=your-ios-client-id.apps.googleusercontent.com
GOOGLE_IOS_URL_SCHEME=com.googleusercontent.apps.your-ios-client-id
REVENUECAT_API_KEY=appl_your_revenuecat_key
```

### 3. Firebase Setup
1.  Add `google-services.json` to `composeApp/`.
2.  Add `GoogleService-Info.plist` to `iosApp/iosApp/`.
3.  **Deploy Functions:**
    ```bash
    cd functions
    npm install
    firebase deploy --only functions
    ```
  
### 4. iOS Dependency Setup (Xcode)
Since this is a Kotlin Multiplatform project with native iOS dependencies, you need to add the following **Swift Packages** directly in Xcode:

1.  Open `iosApp/iosApp.xcworkspace`.
2.  File > Add Package Dependencies...
3.  Add the following:
    *   **RevenueCat:** `purchases-hybrid-common` (Use Version 8.0.0+)
    *   **Firebase:** `firebase-ios-sdk` (Core, Auth, Firestore, Messaging)
    *   **Google Sign-In:** `GoogleSignIn-ios`
4.  Ensure these libraries are linked in "Build Phases" > "Link Binary With Libraries".

### 5. Build & Run
**Android:**
```bash
./gradlew :composeApp:installDebug
```
**iOS:** Open `iosApp/iosApp.xcworkspace` in Xcode -> Run.

---

## 💰 RevenueCat Integration

We use **RevenueCat** to power the "Pro" experience seamlessly across platforms.

*   **Entitlements:** `premium_access` (Mapped to Play Store & App Store products).
*   **Offerings:** Configured dynamically in the RevenueCat dashboard.
*   **Implementation:**
    *   `SubscriptionRepository` observes `Purchases.shared.customerInfo`.
    *   Compose UI updates instantly when `entitlement.isActive` becomes true.
    *   **Hackathon Implementation Note:**
        *   **Android:** Fully functional Google Play Billing.
        *   **iOS:** Since we do **not** have a paid Apple Developer Account, we cannot create In-App Purchase products in App Store Connect.
        *   **The Fallback:** On iOS, the app gracefully handles the "Product Not Found" error by showing a fallback UI. Ideally, a user would purchase Premium on Android (where we have products configured), and the entitlement would **sync instantly to iOS** via RevenueCat's cross-platform user ID system.

---

## ⚠️ Known Limitations

1.  **iOS Background Sync (Free Dev Account):** Without a paid Apple Developer account ($99/yr), "Silent Push" notifications needed for background sync won't be delivered reliably if the app is force-quit. They work fine in the simulator or if the app is suspended.
2.  **Exact Alarms:** Android 13+ requires user permission for exact alarms. The app handles this gracefully by prompting the user on the first schedule.

---

## 🏆 Hackathon Story: Why We Built This

We heard Sam's frustration. "Why can't I just have a reminder app that works?"

Existing solutions were either:
*   **Too Simple:** Basic lists, no complex recurrence.
*   **Platform Locked:** Apple Reminders is great, but iOS only. Android apps don't sync to iPad.
*   **Broken Sync:** "Why is my old phone buzzing for a task I finished an hour ago?"

## 🧠 What We Learned

### 1. The Real Cost of Cross-Platform
We learned that "Write Once, Run Everywhere" is a myth. The winning strategy is **"Share Logic, Respect Platform."** We share 100% of our business logic (ViewModels, Repositories), but we lean heavily into platform strengths: **Android Overlays** for interaction and **iOS Time Sensitive Notifications** for urgency.

### 2. Distributed Systems are Hard
Building reliable sync taught us about conflict resolution (Last-Write-Wins), clock skew, and why "eventual consistency" is a UI challenge as much as a backend one.

### 3. Constraints Breed Creativity
Without a paid Apple Developer Account, we couldn't use background APNs. Instead of giving up, we architected a robust **Foreground Sync** for iOS that feels instant, ensuring the app is still fully functional and impressive.

---

## 🔮 What's Next?

The Hackathon is just the beginning. 
*   **Desktop Apps:** Compose Multiplatform makes Windows/Mac/Linux support a natural next step.
*   **Widgets:** Home screen widgets for quick capture.
*   **Collaborative Lists:** Shared grocery lists with real-time sync.

---

**Enjoy the sync!** 🔄

---

<div align="center">

**[View Demo Video](https://youtu.be/XfObFwvGFFU?si=5Ej5A8Jl1oPMk5NS)** • **[Download APP](https://play.google.com/store/apps/details?id=com.bhaskar.synctask)** (Invite Only)

Built with ❤️ by [Bhaskar Dey](https://github.com/bhaskar966)

</div>
