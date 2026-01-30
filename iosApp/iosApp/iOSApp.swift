import SwiftUI
import UserNotifications
import composeApp

class AppDelegate: NSObject, UIApplicationDelegate, UNUserNotificationCenterDelegate {
    func application(_ application: UIApplication,
                     didFinishLaunchingWithOptions launchOptions: [UIApplication.LaunchOptionsKey : Any]? = nil) -> Bool {

        UNUserNotificationCenter.current().delegate = self

        // Request permission
        UNUserNotificationCenter.current().requestAuthorization(options: [.alert, .sound, .badge]) { granted, error in
            if granted {
                print("✅ Notification permission granted")
            } else {
                print("❌ Notification permission denied")
            }
        }

        UNUserNotificationCenter.current().getNotificationSettings { settings in
            print("📱 iOS Notification Status: \(settings.authorizationStatus.rawValue)")
        }

        UNUserNotificationCenter.current().getPendingNotificationRequests { requests in
            print("📱 Pending notifications: \(requests.count)")
            for request in requests {
                print("  - \(request.identifier): \(request.trigger)")
            }
        }

        return true
    }

    // Show notification when app is in foreground
    func userNotificationCenter(_ center: UNUserNotificationCenter,
                                willPresent notification: UNNotification,
                                withCompletionHandler completionHandler: @escaping (UNNotificationPresentationOptions) -> Void) {

        // ✅ Extract notification data
        let userInfo = notification.request.content.userInfo
        let reminderId = userInfo["reminderId"] as? String ?? ""
        let isPreReminder = userInfo["isPreReminder"] as? Bool ?? false

        // ✅ Call handleNotificationDelivered
        handleNotification(reminderId: reminderId, isPreReminder: isPreReminder)

        completionHandler([.banner, .sound])
    }

    // Handle notification tap
    func userNotificationCenter(_ center: UNUserNotificationCenter,
                                didReceive response: UNNotificationResponse,
                                withCompletionHandler completionHandler: @escaping () -> Void) {

        // ✅ Extract notification data
        let userInfo = response.notification.request.content.userInfo
        let reminderId = userInfo["reminderId"] as? String ?? ""
        let isPreReminder = userInfo["isPreReminder"] as? Bool ?? false

        // ✅ Call handleNotificationDelivered
        handleNotification(reminderId: reminderId, isPreReminder: isPreReminder)

        completionHandler()
    }

    // ✅ NEW: Helper function to handle notifications
    private func handleNotification(reminderId: String, isPreReminder: Bool) {
        // Get scheduler from Koin
        let koinApp = KoinHelperKt.doInitKoin()
        let scheduler = koinApp.koin.get(objCClass: PlatformNotificationScheduler.self) as! PlatformNotificationScheduler

        // Call handleNotificationDelivered (async)
        Task {
            await scheduler.handleNotificationDelivered(reminderId: reminderId, isPreReminder: isPreReminder)
        }
    }
}

@main
struct iOSApp: App {
    @UIApplicationDelegateAdaptor(AppDelegate.self) var appDelegate

    init() {
        KoinHelperKt.doInitKoin()
    }

    var body: some Scene {
        WindowGroup {
            ContentView()
        }
    }
}
