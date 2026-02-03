import Foundation
import FirebaseMessaging
import FirebaseFirestore
import UserNotifications
import ComposeApp

import Foundation
import FirebaseMessaging
import UserNotifications
import ComposeApp

public class SwiftFCMBridge {
    public static let shared = SwiftFCMBridge()

    private init() {}

    // Called from AppDelegate when FCM token is received
    public func setToken(_ token: String) {
        print("🔑 SwiftFCMBridge: Token received - \(token.prefix(20))...")
        FCMBridgeState.shared.onTokenReceived(token: token)
    }

    // Called from AppDelegate when FCM message is received
    public func handleFCMMessage(reminderId: String, action: String) {
        print("📬 SwiftFCMBridge: FCM Message - Action: \(action), ReminderId: \(reminderId)")

        switch action {
        case "dismissed", "completed":
            cancelNotification(reminderId)
        case "rescheduled", "reminder_created":
            cancelNotification(reminderId)
            // Trigger Kotlin bridge
            FCMBridgeState.shared.onMessageReceived(reminderId: reminderId, action: action)
            // Manually reschedule
            KoinHelperKt.rescheduleNotifications()
        case "snoozed":
            cancelNotification(reminderId)
            FCMBridgeState.shared.onMessageReceived(reminderId: reminderId, action: action)
        default:
            print("⚠️ Unknown action: \(action)")
        }
    }

    private func cancelNotification(_ reminderId: String) {
        let center = UNUserNotificationCenter.current()
        center.removePendingNotificationRequests(withIdentifiers: [reminderId, "pre_\(reminderId)"])
        center.removeDeliveredNotifications(withIdentifiers: [reminderId, "pre_\(reminderId)"])
        print("🗑️ Cancelled notifications for: \(reminderId)")
    }
}
