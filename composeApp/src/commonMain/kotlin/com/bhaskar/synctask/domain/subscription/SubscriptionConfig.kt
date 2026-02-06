package com.bhaskar.synctask.domain.subscription

object SubscriptionConfig {
    // RevenueCat entitlement ID - matches what you configured in RevenueCat dashboard
    const val PREMIUM_ENTITLEMENT_ID = "premium_test"
    
    // Testing flag - strictly for dev use, kept as constant reference if needed, 
    // but logic should prefer passed-in state.
    const val IS_TESTING_PREMIUM_FEATURES = false
    
    // Helper to check access given a state
    fun hasPremiumAccess(isPremiumSubscribed: Boolean): Boolean {
        return IS_TESTING_PREMIUM_FEATURES || isPremiumSubscribed
    }

    // Limits
    object Limits {
        // Free tier
        const val FREE_MAX_ACTIVE_REMINDERS = 15
        const val FREE_MAX_GROUPS = 3
        const val FREE_MAX_SUBTASKS_PER_REMINDER = 10
        const val FREE_MAX_PINNED_REMINDERS = 3
        const val FREE_MAX_TAGS = 10

        // Premium tier
        const val PREMIUM_MAX_ACTIVE_REMINDERS = 1000
        const val PREMIUM_MAX_GROUPS = 500
        const val PREMIUM_MAX_SUBTASKS_PER_REMINDER = 50
        const val PREMIUM_MAX_PINNED_REMINDERS = 10
        const val PREMIUM_MAX_TAGS = 100
    }

    object UpgradeMessages {
        const val REMINDERS = "You've reached the free limit of 15 reminders. Upgrade to Premium for 1000 reminders!"
        const val GROUPS = "Free users can create up to 3 groups. Upgrade for 500 groups!"
        const val PINNED = "You can pin up to 3 reminders on free plan. Get 10 with Premium!"
        const val SUBTASKS = "Free plan allows 10 subtasks per reminder. Upgrade for 50!"
        const val TAGS = "You've reached 10 tags limit. Premium users get 100 tags!"
    }

    // Check functions - now PURE (require state injection)
    fun canAddReminder(currentCount: Int, isPremium: Boolean): Boolean {
        val limit = if (hasPremiumAccess(isPremium)) Limits.PREMIUM_MAX_ACTIVE_REMINDERS
        else Limits.FREE_MAX_ACTIVE_REMINDERS
        return currentCount < limit
    }

    fun canAddGroup(currentCount: Int, isPremium: Boolean): Boolean {
        val limit = if (hasPremiumAccess(isPremium)) Limits.PREMIUM_MAX_GROUPS
        else Limits.FREE_MAX_GROUPS
        return currentCount < limit
    }

    fun canAddSubtask(currentCount: Int, isPremium: Boolean): Boolean {
        val limit = if (hasPremiumAccess(isPremium)) Limits.PREMIUM_MAX_SUBTASKS_PER_REMINDER
        else Limits.FREE_MAX_SUBTASKS_PER_REMINDER
        return currentCount < limit
    }

    fun canPinReminder(currentPinnedCount: Int, isPremium: Boolean): Boolean {
        val limit = if (hasPremiumAccess(isPremium)) Limits.PREMIUM_MAX_PINNED_REMINDERS
        else Limits.FREE_MAX_PINNED_REMINDERS
        return currentPinnedCount < limit
    }

    fun canAddTag(currentCount: Int, isPremium: Boolean): Boolean {
        val limit = if (hasPremiumAccess(isPremium)) Limits.PREMIUM_MAX_TAGS
        else Limits.FREE_MAX_TAGS
        return currentCount < limit
    }

    fun getMaxReminders(isPremium: Boolean): Int {
        return if (hasPremiumAccess(isPremium)) Limits.PREMIUM_MAX_ACTIVE_REMINDERS
        else Limits.FREE_MAX_ACTIVE_REMINDERS
    }

    fun getMaxGroups(isPremium: Boolean): Int {
        return if (hasPremiumAccess(isPremium)) Limits.PREMIUM_MAX_GROUPS
        else Limits.FREE_MAX_GROUPS
    }

    fun getMaxSubtasksPerReminder(isPremium: Boolean): Int {
        return if (hasPremiumAccess(isPremium)) Limits.PREMIUM_MAX_SUBTASKS_PER_REMINDER
        else Limits.FREE_MAX_SUBTASKS_PER_REMINDER
    }

    fun getMaxPinnedReminders(isPremium: Boolean): Int {
        return if (hasPremiumAccess(isPremium)) Limits.PREMIUM_MAX_PINNED_REMINDERS
        else Limits.FREE_MAX_PINNED_REMINDERS
    }

    fun getMaxTags(isPremium: Boolean): Int {
        return if (hasPremiumAccess(isPremium)) Limits.PREMIUM_MAX_TAGS
        else Limits.FREE_MAX_TAGS
    }
}