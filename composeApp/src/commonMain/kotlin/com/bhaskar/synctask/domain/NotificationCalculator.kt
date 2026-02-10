package com.bhaskar.synctask.domain

import com.bhaskar.synctask.domain.model.NextNotification
import com.bhaskar.synctask.domain.model.RecurrenceRule
import com.bhaskar.synctask.domain.model.Reminder
import com.bhaskar.synctask.domain.model.ReminderStatus
import com.bhaskar.synctask.domain.repository.ReminderRepository
import com.bhaskar.synctask.presentation.utils.toLocalDateTime
import kotlinx.coroutines.flow.first
import kotlinx.datetime.DatePeriod
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atTime
import kotlinx.datetime.daysUntil
import kotlinx.datetime.isoDayNumber
import kotlinx.datetime.number
import kotlinx.datetime.periodUntil
import kotlinx.datetime.plus
import kotlinx.datetime.toInstant
import kotlin.time.Clock
import kotlin.time.Instant

class NotificationCalculator(
    private val repository: ReminderRepository
) {

    suspend fun getNextNotification(): NextNotification? {
        println("🔵━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")
        println("🔵 getNextNotification() START")

        val now = Clock.System.now().toEpochMilliseconds()
        val activeReminders = repository.getReminders().first()
            .filter { it.status == ReminderStatus.ACTIVE || it.status == ReminderStatus.SNOOZED }

        println("🔵 Found ${activeReminders.size} active/snoozed reminders")

        val potentialNotifications = mutableListOf<NextNotification>()

        for (reminder in activeReminders) {
            println("🔵 Checking: ${reminder.title} (${reminder.status})")

            // 1. Snoozed
            if (reminder.status == ReminderStatus.SNOOZED
                && reminder.snoozeUntil != null
                && reminder.snoozeUntil > now) {

                println("   ⏰ SNOOZED until ${Instant.fromEpochMilliseconds(reminder.snoozeUntil)}")
                potentialNotifications.add(
                    NextNotification(
                        reminderId = reminder.id,
                        triggerTime = reminder.snoozeUntil,
                        isPreReminder = false,
                        title = reminder.title,
                        body = reminder.description ?: ""
                    )
                )
                continue
            }

            // 2. Deadline reminders
            if (reminder.deadline != null
                && reminder.status == ReminderStatus.ACTIVE
                && reminder.recurrence != null) {

                println("   📅 DEADLINE REMINDER")
                val deadlineNotification = getDeadlineNotification(reminder, now) 
                if (deadlineNotification != null) {
                    val notifTime = Instant.fromEpochMilliseconds(deadlineNotification.triggerTime)
                    println("   ✅ Next occurrence at $notifTime")
                    potentialNotifications.add(deadlineNotification)
                }
                continue
            }

            // 3. Pre-reminders
            if (reminder.reminderTime != null && reminder.reminderTime > now) {
                println("   🔔 PRE-REMINDER at ${Instant.fromEpochMilliseconds(reminder.reminderTime)}")
                potentialNotifications.add(
                    NextNotification(
                        reminderId = reminder.id,
                        triggerTime = reminder.reminderTime,
                        isPreReminder = true,
                        title = reminder.title,
                        body = reminder.description ?: ""
                    )
                )
            }

            // 4. Due time
            if (reminder.dueTime > now) {
                println("   ⏰ DUE TIME at ${Instant.fromEpochMilliseconds(reminder.dueTime)}")
                potentialNotifications.add(
                    NextNotification(
                        reminderId = reminder.id,
                        triggerTime = reminder.dueTime,
                        isPreReminder = false,
                        title = reminder.title,
                        body = reminder.description ?: ""
                    )
                )
            }
        }

        val result = potentialNotifications.minByOrNull { it.triggerTime }

        if (result != null) {
            println("🔵 NEXT NOTIFICATION: ${result.title}")
            println("   Trigger at: ${Instant.fromEpochMilliseconds(result.triggerTime)}")
            println("   isPreReminder: ${result.isPreReminder}")

            // 🆕 Format message for pre-reminders
            if (result.isPreReminder) {
                val timeZone = TimeZone.currentSystemDefault()
                val triggerDateTime = Instant.fromEpochMilliseconds(result.triggerTime).toLocalDateTime(timeZone)
                val hour = triggerDateTime.hour
                val minute = triggerDateTime.minute
                val amPm = if (hour < 12) "AM" else "PM"
                val hour12 = if (hour == 0 || hour == 12) 12 else hour % 12
                val timeString = "${hour12}:${minute.toString().padStart(2, '0')} $amPm"

                val formattedTitle = "⏰ Upcoming: ${result.title}"
                val formattedBody = "${result.title} is set for $timeString"

                return result.copy(title = formattedTitle, body = formattedBody)
            }
        } else {
            println("🔵 NO upcoming notifications")
        }

        println("🔵━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")

        return result
    }

    private fun getDeadlineNotificationForToday(
        reminder: Reminder,
        now: Long
    ): NextNotification? {
        val timeZone = TimeZone.currentSystemDefault()
        val nowDateTime = Instant.fromEpochMilliseconds(now).toLocalDateTime(timeZone)
        val today = nowDateTime.date
        val dueDateTime = Instant.fromEpochMilliseconds(reminder.dueTime).toLocalDateTime(timeZone)
        val dueDate = dueDateTime.date
        val dueTime = dueDateTime.time

        println("   🔍 Deadline Check Details:")
        println("      dueDate: $dueDate")
        println("      today: $today")
        println("      dueTime: $dueTime")
        println("      now: $nowDateTime")

        // Check if deadline passed
        if (reminder.deadline != null && now > reminder.deadline) {
            println("      ❌ Deadline already passed")
            return null
        }

        // Check if TODAY is a recurrence day (or future day within interval)
        val isRecurrenceDay = when (val rule = reminder.recurrence) {
            is RecurrenceRule.Daily -> {
                // For daily: check if today is N days after start
                val daysSince = dueDate.daysUntil(today)
                println("      Daily: daysSince=$daysSince, interval=${rule.interval}")

                when {
                    daysSince < 0 -> {
                        // Start date is in the future
                        println("      ❌ Start date is in future")
                        false
                    }
                    daysSince % rule.interval == 0 -> {
                        println("      ✅ Today is recurrence day (daysSince % interval = 0)")
                        true
                    }
                    else -> {
                        println("      ❌ Not a recurrence day (daysSince % interval != 0)")
                        false
                    }
                }
            }

            is RecurrenceRule.Weekly -> {
                println("      Weekly: daysOfWeek=${rule.daysOfWeek}, interval=${rule.interval}")

                // Don't schedule before start date
                if (today < dueDate) {
                    println("      ❌ Before start date")
                    return null
                }

                val daysSince = dueDate.daysUntil(today)
                val weeksSince = daysSince / 7

                println("      weeksSince=$weeksSince")

                // Check if correct week interval
                if (weeksSince % rule.interval != 0) {
                    println("      ❌ Wrong week interval")
                    false
                } else {
                    // Check if today's day of week matches
                    val todayDayOfWeek = today.dayOfWeek.isoDayNumber
                    println("      todayDayOfWeek=$todayDayOfWeek")

                    val matches = rule.daysOfWeek.contains(todayDayOfWeek)
                    if (matches) {
                        println("      ✅ Today's day matches!")
                    } else {
                        println("      ❌ Today's day doesn't match")
                    }
                    matches
                }
            }

            is RecurrenceRule.Monthly -> {
                if (today < dueDate) {
                    println("      ❌ Before start date")
                    return null
                }

                val period = dueDate.periodUntil(today)
                val monthsSince = period.years * 12 + period.months

                println("      Monthly: monthsSince=$monthsSince, interval=${rule.interval}, targetDay=${rule.dayOfMonth}")

                // Check if correct month interval
                if (monthsSince % rule.interval != 0) {
                    println("      ❌ Wrong month interval")
                    false
                } else {
                    // Check if correct day of month
                    val matches = today.day == rule.dayOfMonth
                    if (matches) {
                        println("      ✅ Today is target day!")
                    } else {
                        println("      ❌ Wrong day of month (today=${today.day})")
                    }
                    matches
                }
            }

            is RecurrenceRule.Yearly -> {
                if (today < dueDate) {
                    println("      ❌ Before start date")
                    return null
                }

                val period = dueDate.periodUntil(today)
                val yearsSince = period.years

                println("      Yearly: yearsSince=$yearsSince, interval=${rule.interval}")

                // Check if correct year interval
                if (yearsSince % rule.interval != 0) {
                    println("      ❌ Wrong year interval")
                    false
                } else {
                    // Check if correct date
                    val matches = today.month.number == rule.month && today.day == rule.dayOfMonth
                    if (matches) {
                        println("      ✅ Today is anniversary!")
                    } else {
                        println("      ❌ Wrong date")
                    }
                    matches
                }
            }

            else -> {
                println("      ❌ No recurrence rule")
                false
            }
        }

        if (!isRecurrenceDay) {
            return null // Not a recurrence day
        }

        // Build notification time for today
        val notificationDateTime = today.atTime(dueTime)
        val notificationTime = notificationDateTime.toInstant(timeZone).toEpochMilliseconds()

        println("      📅 Notification would be at: $notificationDateTime")
        println("      ⏰ notificationTime=$notificationTime, now=$now")

        // Calculate Pre-Reminder Time for Today (Dynamic)
        var triggerTime = notificationTime
        var isPreReminder = false

        if (reminder.reminderTime != null && reminder.reminderTime < reminder.dueTime) {
            val offset = reminder.dueTime - reminder.reminderTime
            val preReminderTime = notificationTime - offset

            // Check if pre-reminder is valid (in future and before deadline)
            if (preReminderTime > now) {
                println("      🔔 Found valid PRE-REMINDER for today at ${Instant.fromEpochMilliseconds(preReminderTime)}")
                triggerTime = preReminderTime
                isPreReminder = true
            } else {
                println("      ⚠️ Pre-reminder for today already passed")
            }
        }

        // Check if final trigger time (either pre or due) passed
        if (triggerTime <= now) {
            println("      ❌ Trigger time already passed today")
            return null 
        }

        // Check if before deadline
        if (reminder.deadline != null && triggerTime > reminder.deadline) {
            println("      ❌ After deadline")
            return null
        }

        println("      ✅ Scheduling for today! (isPre=$isPreReminder)")

        return NextNotification(
            reminderId = reminder.id,
            triggerTime = triggerTime,
            isPreReminder = isPreReminder,
            title = reminder.title,
            body = reminder.description ?: ""
        )
    }

    private fun getDeadlineNotification(
        reminder: Reminder,
        now: Long
    ): NextNotification? {
        println("   🔄 Checking deadline reminder for next occurrence...")

        // Check today first
        val todayNotification = getDeadlineNotificationForToday(reminder, now)
        if (todayNotification != null) {
            println("   ✅ Found occurrence today")
            return todayNotification
        }

        println("   ⏭️ Today passed, checking future days...")

        // If today passed, check next 60 days
        val timeZone = TimeZone.currentSystemDefault()
        val nowDateTime = Instant.fromEpochMilliseconds(now).toLocalDateTime(timeZone)

        for (daysAhead in 1..60) {
            val futureDate = nowDateTime.date.plus(DatePeriod(days = daysAhead))
            val futureStart = futureDate.atTime(0, 0).toInstant(timeZone).toEpochMilliseconds()

            // Stop if past deadline
            if (reminder.deadline != null && futureStart > reminder.deadline) {
                println("   ⏹️ Reached deadline, no more occurrences")
                break
            }

            val futureNotification = getDeadlineNotificationForToday(reminder, futureStart)
            if (futureNotification != null) {
                val futureDateTime = Instant.fromEpochMilliseconds(futureNotification.triggerTime)
                    .toLocalDateTime(timeZone)
                println("   ✅ Found next occurrence in $daysAhead days at $futureDateTime")
                return futureNotification
            }
        }

        println("   ❌ No future occurrences found within 60 days")
        return null
    }


}