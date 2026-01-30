package com.bhaskar.synctask.util

import androidx.room.InvalidationTracker
import com.bhaskar.synctask.db.ReminderDao
import com.bhaskar.synctask.db.SyncTaskDatabase

class TestDatabase(
    private val fakeDao: FakeReminderDao
) : SyncTaskDatabase() {

    override fun reminderDao(): ReminderDao = fakeDao

    override fun createInvalidationTracker(): InvalidationTracker {
        // Return a minimal no-op tracker for testing
        return InvalidationTracker(
            this,
            emptyMap(),
            emptyMap(),
            "ReminderEntity", "SyncQueueEntity"
        )
    }

    override fun clearAllTables() {
        // Clear all fake data for testing
        fakeDao.clear()
    }
}