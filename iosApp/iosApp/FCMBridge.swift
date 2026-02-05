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

        // ALWAYS cancel notification first
        cancelNotification(reminderId)

        // Then handle specific actions
        switch action {
        case "dismissed", "completed":
            print("✅ Status changed to \(action) - notification cancelled")
        // Firestore listener will update local DB

        case "snoozed":
            print("⏰ Snoozed - will reschedule via Firestore listener")
        // Notification already cancelled, Firestore will trigger reschedule

        case "rescheduled":
            print("📅 Rescheduled - will reschedule via Firestore listener")
        // Notification already cancelled, Firestore will trigger reschedule

        case "reminder_created":
            print("➕ New reminder created - will schedule via Firestore listener")
        // Firestore listener will add new reminder and schedule

        case "reminder_deleted":
            print("🗑️ Reminder deleted - notification cancelled")
        // Already cancelled above

        default:
            print("⚠️ Unknown action: \(action)")
        }

        // Notify KMP layer
        FCMBridgeState.shared.onMessageReceived(reminderId: reminderId, action: action)
    }

    // Better notification cancellation with fuzzy matching
    private func cancelNotification(_ reminderId: String) {
        let center = UNUserNotificationCenter.current()

        // Cancel pending notifications
        center.getPendingNotificationRequests { requests in
            let matchingIds = requests
            .filter { request in
                // Match by reminderId in userInfo OR by identifier
                if let storedId = request.content.userInfo["reminderId"] as? String {
                    return storedId == reminderId
                }
                // Also try direct identifier match
                return request.identifier == reminderId ||
                request.identifier == "pre_\(reminderId)"
            }
            .map { $0.identifier }

            if !matchingIds.isEmpty {
                center.removePendingNotificationRequests(withIdentifiers: matchingIds)
                print("🗑️ Cancelled \(matchingIds.count) pending notification(s) for: \(reminderId)")
            } else {
                print("ℹ️ No pending notifications found for: \(reminderId)")
            }
        }

        // Remove delivered notifications
        center.getDeliveredNotifications { notifications in
            let matchingIds = notifications
            .filter { notification in
                if let storedId = notification.request.content.userInfo["reminderId"] as? String {
                    return storedId == reminderId
                }
                return notification.request.identifier == reminderId ||
                notification.request.identifier == "pre_\(reminderId)"
            }
            .map { $0.request.identifier }

            if !matchingIds.isEmpty {
                center.removeDeliveredNotifications(withIdentifiers: matchingIds)
                print("🗑️ Removed \(matchingIds.count) delivered notification(s) for: \(reminderId)")
            }
        }
    }
}
