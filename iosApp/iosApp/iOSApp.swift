import SwiftUI
import ComposeApp
import UserNotifications

@main
struct iOSApp: App {
    @UIApplicationDelegateAdaptor(AppDelegate.self) var delegate
    
    init() {
        // Initialize Koin
        KoinHelperKt.doInitKoin()
        
        // Request notification permissions
        UNUserNotificationCenter.current().requestAuthorization(options: [.alert, .sound, .badge]) { granted, error in
            if granted {
                print("✅ iOS: Notification permission granted")
            } else {
                print("❌ iOS: Notification permission denied: \(error?.localizedDescription ?? "unknown error")")
            }
        }
        
        // Debug: Print pending notifications
        UNUserNotificationCenter.current().getPendingNotificationRequests { requests in
            print("📱 iOS: Pending notifications: \(requests.count)")
            for request in requests {
                let triggerDescription = request.trigger?.description ?? "no trigger"
                print("  - \(request.identifier): \(triggerDescription)")
            }
        }
    }
    
    var body: some Scene {
        WindowGroup {
            ContentView()
        }
    }
}

class AppDelegate: NSObject, UIApplicationDelegate, UNUserNotificationCenterDelegate {
    
    func application(
        _ application: UIApplication,
        didFinishLaunchingWithOptions launchOptions: [UIApplication.LaunchOptionsKey : Any]? = nil
    ) -> Bool {
        print("🍎 iOS: App launched")
        UNUserNotificationCenter.current().delegate = self
        
        // ✅ NEW: Process delivered notifications on launch
        processDeliveredNotificationsOnLaunch()
        
        return true
    }
    
    // ✅ NEW: Check and process delivered notifications
    private func processDeliveredNotificationsOnLaunch() {
        UNUserNotificationCenter.current().getDeliveredNotifications { notifications in
            print("🍎 iOS: Found \(notifications.count) delivered notifications")
            
            var deliveredIds: [String] = []
            
            for notification in notifications {
                let userInfo = notification.request.content.userInfo
                if let reminderId = userInfo["reminderId"] as? String,
                   let isPreReminderString = userInfo["isPreReminder"] as? String,
                   let isPreReminder = Bool(isPreReminderString),
                   !isPreReminder {  // Only process main reminders, not pre-reminders
                    
                    print("   Delivered while app was dead: \(reminderId)")
                    deliveredIds.append(reminderId)
                }
            }
            
            if !deliveredIds.isEmpty {
                print("🍎 Processing \(deliveredIds.count) delivered reminders")
                KoinHelperKt.processDeliveredNotifications(deliveredIds: deliveredIds)
                
                // ✅ Clear them from notification center after processing
                let identifiers = deliveredIds
                UNUserNotificationCenter.current().removeDeliveredNotifications(withIdentifiers: identifiers)
            }
        }
    }
    
    // Handle notification when app is in foreground
    func userNotificationCenter(
        _ center: UNUserNotificationCenter,
        willPresent notification: UNNotification,
        withCompletionHandler completionHandler: @escaping (UNNotificationPresentationOptions) -> Void
    ) {
        print("📱 iOS: Notification received while app in foreground")
        
        let userInfo = notification.request.content.userInfo
        if let reminderId = userInfo["reminderId"] as? String,
           let isPreReminderString = userInfo["isPreReminder"] as? String,
           let isPreReminder = Bool(isPreReminderString) {
            
            print("   reminderId: \(reminderId)")
            print("   isPreReminder: \(isPreReminder)")
            
            KoinHelperKt.handleIOSNotification(
                reminderId: reminderId,
                isPreReminder: isPreReminder
            )
        }
        
        completionHandler([.banner, .sound, .badge])
    }
    
    // Handle notification tap
    func userNotificationCenter(
        _ center: UNUserNotificationCenter,
        didReceive response: UNNotificationResponse,
        withCompletionHandler completionHandler: @escaping () -> Void
    ) {
        print("📱 iOS: Notification tapped")
        
        let userInfo = response.notification.request.content.userInfo
        if let reminderId = userInfo["reminderId"] as? String,
           let isPreReminderString = userInfo["isPreReminder"] as? String,
           let isPreReminder = Bool(isPreReminderString) {
            
            print("   Opening and processing reminder: \(reminderId)")
            
            // ✅ Process the reminder when user taps it
            KoinHelperKt.handleIOSNotification(
                reminderId: reminderId,
                isPreReminder: isPreReminder
            )
            
            // TODO: Navigate to reminder detail
        }
        
        completionHandler()
    }
    
    // Handle app becoming active
    func applicationDidBecomeActive(_ application: UIApplication) {
        print("🍎 iOS: App became active")
        
        // Check for missed reminders
        KoinHelperKt.checkIOSMissedReminders()
        
        // ✅ Process any delivered notifications
        processDeliveredNotificationsOnLaunch()
    }
}
