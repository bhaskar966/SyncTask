import SwiftUI
import ComposeApp
import UserNotifications

@main
struct iOSApp: App {
    @UIApplicationDelegateAdaptor(AppDelegate.self) var delegate

    init() {
        KoinHelperKt.doInitKoin()
        registerNotificationCategories()

        UNUserNotificationCenter.current().requestAuthorization(options: [.alert, .sound, .badge]) { granted, error in
            if granted {
                print("✅ iOS: Notification permission granted")
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

class AppDelegate: NSObject, UIApplicationDelegate, UNUserNotificationCenterDelegate {

    func application(
    _ application: UIApplication,
    didFinishLaunchingWithOptions launchOptions: [UIApplication.LaunchOptionsKey : Any]? = nil
    ) -> Bool {
        print("🍎 iOS: App launched")
        UNUserNotificationCenter.current().delegate = self
        processDeliveredNotificationsOnLaunch()
        return true
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

    // ✅ FIXED: Only ONE didReceive method, with @Sendable
    func userNotificationCenter(
    _ center: UNUserNotificationCenter,
    didReceive response: UNNotificationResponse,
    withCompletionHandler completionHandler: @escaping @Sendable () -> Void
    ) {
        print("📱 iOS: Notification action: \(response.actionIdentifier)")
        let userInfo = response.notification.request.content.userInfo

        guard let reminderId = userInfo["reminderId"] as? String else {
            completionHandler()
            return
        }

        let title = response.notification.request.content.title
        let isPreReminderString = userInfo["isPreReminder"] as? String ?? "false"
        let isPreReminder = Bool(isPreReminderString) ?? false

        switch response.actionIdentifier {
        case "COMPLETE_ACTION":
            KoinHelperKt.handleIOSComplete(reminderId: reminderId)
            showLocalNotification(message: "✅ Completed: \(title)")

        case "DISMISS_ACTION":
            KoinHelperKt.handleIOSDismiss(reminderId: reminderId)
            showLocalNotification(message: "🚫 Dismissed: \(title)")

        case "PRE_DISMISS_ACTION":
            showLocalNotification(message: "Pre-reminder dismissed")

        case "SNOOZE_ACTION":
            DispatchQueue.main.asyncAfter(deadline: .now() + 0.5) {
                self.presentSnoozeScreen(reminderId: reminderId, title: title)
            }

        case "RESCHEDULE_ACTION":
            DispatchQueue.main.asyncAfter(deadline: .now() + 0.5) {
                self.presentEditScreen(reminderId: reminderId)
            }

        case UNNotificationDefaultActionIdentifier:
            KoinHelperKt.handleIOSNotification(
                reminderId: reminderId,
                isPreReminder: isPreReminder
            )

        default:
            break
        }

        completionHandler()
    }

    func applicationDidBecomeActive(_ application: UIApplication) {
        print("🍎 iOS: App became active")
        KoinHelperKt.checkIOSMissedReminders()
        processDeliveredNotificationsOnLaunch()
    }

    private func presentSnoozeScreen(reminderId: String, title: String) {
        let snoozeVC = MainViewControllerKt.SnoozeViewController(
            reminderId: reminderId,
            title: title,
            onDismiss: { @MainActor in
                if let scene = UIApplication.shared.connectedScenes.first as? UIWindowScene,
                let window = scene.windows.first,
                let rootVC = window.rootViewController {

                    var topVC = rootVC
                    while let presented = topVC.presentedViewController {
                        topVC = presented
                    }

                    topVC.dismiss(animated: true)
                }
            }
        )

        snoozeVC.modalPresentationStyle = .fullScreen

        if let scene = UIApplication.shared.connectedScenes.first as? UIWindowScene,
        let window = scene.windows.first,
        let rootVC = window.rootViewController {

            var topVC = rootVC
            while let presented = topVC.presentedViewController {
                topVC = presented
            }

            topVC.present(snoozeVC, animated: true) {
                print("✅ Snooze screen presented")
            }
        }
    }

    private func presentEditScreen(reminderId: String) {
        let editVC = MainViewControllerKt.EditReminderViewController(
            reminderId: reminderId,
            onDismiss: { @MainActor in
                if let scene = UIApplication.shared.connectedScenes.first as? UIWindowScene,
                let window = scene.windows.first,
                let rootVC = window.rootViewController {

                    var topVC = rootVC
                    while let presented = topVC.presentedViewController {
                        topVC = presented
                    }

                    topVC.dismiss(animated: true)
                }
            }
        )

        editVC.modalPresentationStyle = .fullScreen

        if let scene = UIApplication.shared.connectedScenes.first as? UIWindowScene,
        let window = scene.windows.first,
        let rootVC = window.rootViewController {

            var topVC = rootVC
            while let presented = topVC.presentedViewController {
                topVC = presented
            }

            topVC.present(editVC, animated: true) {
                print("✅ Edit screen presented")
            }
        }
    }

    private func showLocalNotification(message: String) {
        let content = UNMutableNotificationContent()
        content.body = message
        content.sound = nil

        let request = UNNotificationRequest(
            identifier: UUID().uuidString,
            content: content,
            trigger: nil
        )

        UNUserNotificationCenter.current().add(request)
    }
}
