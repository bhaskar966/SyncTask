const {
  onDocumentUpdated,
  onDocumentCreated,
  onDocumentDeleted,
} = require("firebase-functions/v2/firestore");
const {initializeApp} = require("firebase-admin/app");
const {getFirestore} = require("firebase-admin/firestore");
const {getMessaging} = require("firebase-admin/messaging");

initializeApp();

/**
 * Send FCM to all user devices
 * @param {string} userId - User ID
 * @param {object} data - FCM data payload
 * @return {Promise} FCM response
 */
async function sendFCMToUserDevices(userId, data) {
  try {
    const tokensSnapshot = await getFirestore()
        .collection("users")
        .doc(userId)
        .collection("fcmTokens")
        .get();

    if (tokensSnapshot.empty) {
      console.log("⚠️ No FCM tokens found");
      return null;
    }

    const tokens = [];
    tokensSnapshot.forEach((doc) => {
      const docData = doc.data();
      if (docData.token) {
        tokens.push(docData.token);
      }
    });

    const deviceCount = tokens.length;
    console.log(`📤 Sending FCM to ${deviceCount} devices`);
    console.log(`   Action: ${data.action}`);

    const messages = tokens.map((token) => ({
      token: token,
      data: data,
      android: {
        priority: "high",
      },
      apns: {
        headers: {
          "apns-priority": "10",
        },
        payload: {
          aps: {
            contentAvailable: true,
          },
        },
      },
    }));

    const response = await getMessaging().sendEach(messages);

    console.log("✅ FCM sent successfully");
    console.log(`  Success: ${response.successCount}`);
    console.log(`  Failure: ${response.failureCount}`);

    // Clean up invalid tokens
    const invalidTokens = [];
    response.responses.forEach((result, index) => {
      if (!result.success) {
        const code = result.error ?
          result.error.code : "unknown";
        console.error(`❌ Error device ${index}:`, code);

        const invalidCodes = [
          "messaging/invalid-registration-token",
          "messaging/registration-token-not-registered",
        ];

        const hasInvalidCode = result.error &&
          invalidCodes.includes(result.error.code);

        if (hasInvalidCode) {
          invalidTokens.push(tokens[index]);
        }
      }
    });

    // Remove invalid tokens
    if (invalidTokens.length > 0) {
      const count = invalidTokens.length;
      console.log(`🧹 Removing ${count} invalid tokens`);

      const deletePromises = [];
      tokensSnapshot.forEach((doc) => {
        const hasInvalidToken =
          invalidTokens.includes(doc.data().token);
        if (hasInvalidToken) {
          deletePromises.push(doc.ref.delete());
        }
      });

      await Promise.all(deletePromises);
    }

    return response;
  } catch (error) {
    console.error("❌ FCM Error:", error);
    throw error;
  }
}

// ========================
// CREATE
// ========================
exports.onReminderCreated = onDocumentCreated(
    "users/{userId}/reminders/{reminderId}",
    async (event) => {
      console.log("➕ Reminder created!");
      const reminder = event.data.data();

      if (reminder.status !== "ACTIVE") {
        console.log("⏭️ Skipping - not ACTIVE");
        return null;
      }

      return sendFCMToUserDevices(event.params.userId, {
        reminderId: event.params.reminderId,
        action: "reminder_created",
      });
    },
);

// ========================
// UPDATE (Status changes & Reschedules)
// ========================
exports.onReminderStatusChanged = onDocumentUpdated(
    "users/{userId}/reminders/{reminderId}",
    async (event) => {
      const userId = event.params.userId;
      const reminderId = event.params.reminderId;
      const before = event.data.before.data();
      const after = event.data.after.data();

      let shouldSendFCM = false;
      let action = "";

      // Check if status changed to terminal state
      const terminalStatuses = ["DISMISSED", "COMPLETED"];
      if (before.status !== after.status &&
      terminalStatuses.includes(after.status)) {
        shouldSendFCM = true;
        action = after.status.toLowerCase();
        console.log(`✅ Status: ${before.status} -> ${after.status}`);
      }

      // Check if snoozed
      if (before.status !== after.status && after.status === "SNOOZED") {
        shouldSendFCM = true;
        action = "snoozed";
        const time = after.snoozeUntil;
        console.log(`⏰ Snoozed until: ${time}`);
      }

      // Check if rescheduled
      const dueTimeChanged = before.dueTime !== after.dueTime;
      const reminderTimeChanged =
      before.reminderTime !== after.reminderTime;

      if (after.status === "ACTIVE" &&
      (dueTimeChanged || reminderTimeChanged)) {
        shouldSendFCM = true;
        action = "rescheduled";
        console.log("📅 Rescheduled:");
        console.log(`  dueTime: ${before.dueTime} -> ${after.dueTime}`);
        const oldRT = before.reminderTime;
        const newRT = after.reminderTime;
        console.log(`  reminderTime: ${oldRT} -> ${newRT}`);
      }

      if (!shouldSendFCM) {
        console.log("⏭️ No FCM needed");
        return null;
      }

      return sendFCMToUserDevices(userId, {
        reminderId: reminderId,
        action: action,
      });
    },
);

// ========================
// DELETE
// ========================
exports.onReminderDeleted = onDocumentDeleted(
    "users/{userId}/reminders/{reminderId}",
    async (event) => {
      console.log("🗑️ Reminder deleted!");

      return sendFCMToUserDevices(event.params.userId, {
        reminderId: event.params.reminderId,
        action: "reminder_deleted",
      });
    },
);

// ========================
// TOKEN MONITORING
// ========================
exports.onFCMTokenAdded = onDocumentCreated(
    "users/{userId}/fcmTokens/{deviceId}",
    (event) => {
      const data = event.data.data();
      const uid = event.params.userId;
      console.log(`✅ New FCM token for user ${uid}`);
      console.log(`  Platform: ${data.platform}`);
      const device = data.deviceModel || "unknown";
      console.log(`  Device: ${device}`);
      return null;
    },
);
