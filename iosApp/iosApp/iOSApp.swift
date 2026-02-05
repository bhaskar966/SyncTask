import SwiftUI
import ComposeApp
import UserNotifications
import FirebaseCore
import FirebaseMessaging
import GoogleSignIn

@main
struct iOSApp: App {
    @UIApplicationDelegateAdaptor(AppDelegate.self) var delegate
    @Environment(\.scenePhase) private var scenePhase

    init() {
        FirebaseApp.configure()
        print("✅ Firebase initialized successfully")
        KoinHelperKt.doInitKoin()
        let _ = GoogleSignInBridge.shared
        registerNotificationCategories()

        UNUserNotificationCenter.current().requestAuthorization(options: [.alert, .sound, .badge]) { granted, error in
            if granted {
                print("✅ iOS: Notification permission granted")
                DispatchQueue.main.async {
                    UIApplication.shared.registerForRemoteNotifications()
                }
            } else {
                print("❌ iOS: Notification permission denied: \(error?.localizedDescription ?? "unknown error")")
            }
        }

        UNUserNotificationCenter.current().getPendingNotificationRequests { requests in
            print("📱 iOS: Pending notifications: \(requests.count)")
            for request in requests {
                let triggerDescription = request.trigger?.description ?? "no trigger"
                let category = request.content.categoryIdentifier
                print("  - \(request.identifier): \(triggerDescription)")
                print("    Category: \(category)")
            }
        }
    }

    var body: some Scene {
        WindowGroup {
            ContentView()
            .onOpenURL { url in
                GIDSignIn.sharedInstance.handle(url)
            }
            .onChange(of: scenePhase) { oldPhase, newPhase in
                if newPhase == .active {
                    print("🍎 App became active - checking for updates")
                    KoinHelperKt.checkIOSMissedReminders()
                }
            }
        }
    }

    private func registerNotificationCategories() {
        let center = UNUserNotificationCenter.current()

        let completeAction = UNNotificationAction(
            identifier: "COMPLETE_ACTION",
            title: "Complete",
            options: []
        )

        let snoozeAction = UNNotificationAction(
            identifier: "SNOOZE_ACTION",
            title: "Snooze",
            options: [.foreground]
        )

        let dismissAction = UNNotificationAction(
            identifier: "DISMISS_ACTION",
            title: "Dismiss",
            options: [.destructive]
        )

        let normalCategory = UNNotificationCategory(
            identifier: "NORMAL_REMINDER",
            actions: [completeAction, snoozeAction, dismissAction],
            intentIdentifiers: [],
            options: []
        )

        let rescheduleAction = UNNotificationAction(
            identifier: "RESCHEDULE_ACTION",
            title: "Reschedule",
            options: [.foreground]
        )

        let preDismissAction = UNNotificationAction(
            identifier: "PRE_DISMISS_ACTION",
            title: "Dismiss",
            options: []
        )

        let preReminderCategory = UNNotificationCategory(
            identifier: "PRE_REMINDER",
            actions: [rescheduleAction, preDismissAction],
            intentIdentifiers: [],
            options: []
        )

        center.setNotificationCategories([normalCategory, preReminderCategory])
        print("✅ iOS: Notification categories registered")
    }
}

class AppDelegate: NSObject, UIApplicationDelegate, UNUserNotificationCenterDelegate, MessagingDelegate {
    func application(
    _ application: UIApplication,
    didFinishLaunchingWithOptions launchOptions: [UIApplication.LaunchOptionsKey : Any]? = nil
    ) -> Bool {
        print("🍎 iOS: App launched")
        UNUserNotificationCenter.current().delegate = self
        Messaging.messaging().delegate = self
        processDeliveredNotificationsOnLaunch()
        return true
    }

    func application(
    _ application: UIApplication,
    didRegisterForRemoteNotificationsWithDeviceToken deviceToken: Data
    ) {
        print("📱 iOS: APNS token received")
        Messaging.messaging().apnsToken = deviceToken
    }

    func application(
    _ application: UIApplication,
    didFailToRegisterForRemoteNotificationsWithError error: Error
    ) {
        print("❌ iOS: Failed to register for remote notifications: \(error.localizedDescription)")
    }

    func messaging(_ messaging: Messaging, didReceiveRegistrationToken fcmToken: String?) {
        guard let token = fcmToken else {
            print("⚠️ iOS: FCM token is nil")
            return
        }

        print("🔑 iOS: FCM Token received: \(token)")
        SwiftFCMBridge.shared.setToken(token)
    }

    func application(
    _ application: UIApplication,
    didReceiveRemoteNotification userInfo: [AnyHashable : Any],
    fetchCompletionHandler completionHandler: @escaping (UIBackgroundFetchResult) -> Void
    ) {
        print("📬 iOS: Remote notification received")
        handleFCMMessage(userInfo)
        completionHandler(.newData)
    }

    private func handleFCMMessage(_ userInfo: [AnyHashable: Any]) {
        guard let reminderId = userInfo["reminderId"] as? String,
        let action = userInfo["action"] as? String else {
            print("⚠️ iOS: Invalid FCM message - missing reminderId or action")
            return
        }

        print("📬 iOS: FCM Action=\(action), ReminderId=\(reminderId)")

        // Use SwiftFCMBridge to handle
        SwiftFCMBridge.shared.handleFCMMessage(reminderId: reminderId, action: action)
    }

    private func processDeliveredNotificationsOnLaunch() {
        UNUserNotificationCenter.current().getDeliveredNotifications { notifications in
            print("🍎 iOS: Found \(notifications.count) delivered notifications")
            var deliveredIds: [String] = []
            for notification in notifications {
                let userInfo = notification.request.content.userInfo
                if let reminderId = userInfo["reminderId"] as? String,
                let isPreReminderString = userInfo["isPreReminder"] as? String,
                let isPreReminder = Bool(isPreReminderString),
                !isPreReminder {
                    print("  Delivered while app was dead: \(reminderId)")
                    deliveredIds.append(reminderId)
                }
            }

            if !deliveredIds.isEmpty {
                print("🍎 Processing \(deliveredIds.count) delivered reminders")
                KoinHelperKt.processDeliveredNotifications(deliveredIds: deliveredIds)
                let identifiers = deliveredIds
                UNUserNotificationCenter.current().removeDeliveredNotifications(withIdentifiers: identifiers)
            }
        }
    }

    func userNotificationCenter(
    _ center: UNUserNotificationCenter,
    willPresent notification: UNNotification,
    withCompletionHandler completionHandler: @escaping @Sendable (UNNotificationPresentationOptions) -> Void
    ) {
        print("📱 iOS: Notification received while app in foreground")
        let userInfo = notification.request.content.userInfo

        if userInfo["gcm.message_id"] != nil {
            handleFCMMessage(userInfo)
            completionHandler([])
            return
        }

        if let reminderId = userInfo["reminderId"] as? String,
        let isPreReminderString = userInfo["isPreReminder"] as? String,
        let isPreReminder = Bool(isPreReminderString) {
            print("  reminderId: \(reminderId)")
            print("  isPreReminder: \(isPreReminder)")
            KoinHelperKt.handleIOSNotification(
                reminderId: reminderId,
                isPreReminder: isPreReminder
            )
        }

        completionHandler([.banner, .sound, .badge])
    }

    func userNotificationCenter(
    _ center: UNUserNotificationCenter,
    didReceive response: UNNotificationResponse,
    withCompletionHandler completionHandler: @escaping @Sendable () -> Void
    ) {
        print("📱 iOS: Notification action: \(response.actionIdentifier)")
        let userInfo = response.notification.request.content.userInfo

        if userInfo["gcm.message_id"] != nil {
            handleFCMMessage(userInfo)
            completionHandler()
            return
        }

        guard let reminderId = userInfo["reminderId"] as? String else {
            completionHandler()
            return
        }

        switch response.actionIdentifier {
        case "COMPLETE_ACTION":
            print("✅ iOS: Complete action - \(reminderId)")
            KoinHelperKt.handleIOSComplete(reminderId: reminderId)

        case "DISMISS_ACTION":
            print("🚫 iOS: Dismiss action - \(reminderId)")
            KoinHelperKt.handleIOSDismiss(reminderId: reminderId)

        case "SNOOZE_ACTION":
            print("⏰ iOS: Snooze action - \(reminderId)")
            presentSnoozeScreen(reminderId: reminderId, title: response.notification.request.content.title)

        case "RESCHEDULE_ACTION":
            print("📅 iOS: Reschedule action - \(reminderId)")
            presentEditScreen(reminderId: reminderId)

        case "PRE_DISMISS_ACTION":
            print("🚫 iOS: Pre-reminder dismissed - \(reminderId)")
            center.removeDeliveredNotifications(withIdentifiers: ["pre_\(reminderId)"])

        case UNNotificationDefaultActionIdentifier:
            print("👆 iOS: Notification tapped - opening app")

        default:
            print("⚠️ iOS: Unknown action: \(response.actionIdentifier)")
        }

        completionHandler()
    }

    // Helper to present Snooze screen
    private func presentSnoozeScreen(reminderId: String, title: String) {
        DispatchQueue.main.async {
            if let windowScene = UIApplication.shared.connectedScenes.first as? UIWindowScene,
            let rootVC = windowScene.windows.first?.rootViewController {
                let snoozeVC = MainViewControllerKt.SnoozeViewController(
                    reminderId: reminderId,
                    title: title,
                    onDismiss: {
                        rootVC.dismiss(animated: true, completion: nil)
                    }
                )
                rootVC.present(snoozeVC, animated: true)
            }
        }
    }

    // Helper to present Edit screen
    private func presentEditScreen(reminderId: String) {
        DispatchQueue.main.async {
            if let windowScene = UIApplication.shared.connectedScenes.first as? UIWindowScene,
            let rootVC = windowScene.windows.first?.rootViewController {
                let editVC = MainViewControllerKt.EditReminderViewController(
                    reminderId: reminderId,
                    onDismiss: {
                        rootVC.dismiss(animated: true, completion: nil)
                    }
                )
                rootVC.present(editVC, animated: true)
            }
        }
    }
}