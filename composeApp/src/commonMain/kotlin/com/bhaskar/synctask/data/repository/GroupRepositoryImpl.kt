package com.bhaskar.synctask.data.repository

import com.bhaskar.synctask.data.auth.AuthManager
import com.bhaskar.synctask.data.auth.AuthState
import com.bhaskar.synctask.data.toDomain
import com.bhaskar.synctask.data.toEntity
import com.bhaskar.synctask.db.GroupDao
import com.bhaskar.synctask.db.ReminderDao
import com.bhaskar.synctask.domain.model.ReminderGroup
import com.bhaskar.synctask.domain.repository.GroupRepository
import com.bhaskar.synctask.platform.FirestoreDataSource
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.time.Clock

class GroupRepositoryImpl(
    private val groupDao: GroupDao,
    private val firestoreDataSource: FirestoreDataSource,
    private val authManager: AuthManager,
    private val coroutineScope: CoroutineScope,
    private val reminderDao: ReminderDao
) : GroupRepository {
    private var syncJob: Job? = null

    private val userId: String
        get() = authManager.currentUserId ?: "anonymous"

    init {
        // Start real-time sync when repository is created
        startRealtimeSync()
    }

    private fun startRealtimeSync() {
        syncJob?.cancel()
        syncJob = coroutineScope.launch(Dispatchers.IO) {
            authManager.authState.collect { state ->
                when (state) {
                    is AuthState.Authenticated -> {
                        println("🔄 Starting Groups Firestore sync for user: ${state.uid}")
                        syncFromFirestore(state.uid)
                    }
                    is AuthState.Unauthenticated -> {
                        println("🔄 User logged out - stopping Groups sync")
                    }
                    is AuthState.Loading -> {
                        // Do nothing
                    }
                }
            }
        }
    }

    private suspend fun syncFromFirestore(userId: String) {
        try {
            firestoreDataSource.getGroups(userId).collect { cloudGroups ->
                println("☁️ Received ${cloudGroups.size} groups from Firestore")

                // Push local unsynced groups to cloud first
                val unsyncedGroups = groupDao.getUnsyncedGroups()
                println("📤 Found ${unsyncedGroups.size} unsynced local groups")
                unsyncedGroups.forEach { entity ->
                    try {
                        val localGroup = entity.toDomain()
                        println("📤 Pushing unsynced group to cloud: ${localGroup.name}")
                        firestoreDataSource.saveGroup(localGroup)
                        groupDao.updateSyncStatus(localGroup.id, true)
                    } catch (e: Exception) {
                        println("❌ Failed to push group ${entity.id}: ${e.message}")
                        e.printStackTrace()
                    }
                }

                cloudGroups.forEach { cloudGroup ->
                    val localGroup = groupDao.getGroupById(cloudGroup.id).firstOrNull()
                    if (localGroup == null) {
                        // New group from cloud
                        println("📥 New group from cloud: ${cloudGroup.name}")
                        groupDao.insertGroup(cloudGroup.copy(isSynced = true).toEntity())
                    } else {
                        // Resolve conflicts (last-write-wins)
                        val local = localGroup.toDomain()
                        if (cloudGroup.lastModified > local.lastModified) {
                            println("📥 Update group from cloud: ${cloudGroup.name}")
                            groupDao.insertGroup(cloudGroup.copy(isSynced = true).toEntity())
                        } else if (local.lastModified > cloudGroup.lastModified && !local.isSynced) {
                            // Local is newer and not synced - push to cloud
                            println("📤 Pushing newer local group to cloud: ${local.name}")
                            firestoreDataSource.saveGroup(local)
                            groupDao.updateSyncStatus(local.id, true)
                        }
                    }
                }
            }
        } catch (e: Exception) {
            println("❌ Groups Firestore sync error: ${e.message}")
            e.printStackTrace()
        }
    }

    // PUBLIC API - Only reads from local DB
    override fun getGroups(userId: String): Flow<List<ReminderGroup>> {
        return groupDao.getAllGroups(userId)
            .map { entities -> entities.map { it.toDomain() } }
            .flowOn(Dispatchers.IO)
    }

    override fun getGroupById(groupId: String): Flow<ReminderGroup?> {
        return groupDao.getGroupById(groupId)
            .map { it?.toDomain() }
            .flowOn(Dispatchers.IO)
    }

    override suspend fun createGroup(group: ReminderGroup) = withContext(Dispatchers.IO) {
        println("💾 Repository: Creating group - ${group.name}")
        println("💾 Repository: Group userId before: ${group.userId}")
        println("💾 Repository: Current userId: $userId")

        val now = Clock.System.now().toEpochMilliseconds()
        val withTimestamp = group.copy(
            userId = userId,
            createdAt = now,
            lastModified = now,
            isSynced = false
        )

        println("💾 Repository: Group userId after: ${withTimestamp.userId}")
        println("💾 Repository: Inserting group to local DB...")

        // Save to local DB
        groupDao.insertGroup(withTimestamp.toEntity())

        println("💾 Repository: Group inserted, now syncing to cloud...")

        // Sync to cloud
        syncToCloud(withTimestamp)

        println("💾 Repository: Group creation complete!")
    }

    override suspend fun updateGroup(group: ReminderGroup) = withContext(Dispatchers.IO) {
        println("💾 Repository: Updating group - ${group.name}")
        val now = Clock.System.now().toEpochMilliseconds()
        val withTimestamp = group.copy(
            userId = userId,
            lastModified = now,
            isSynced = false
        )

        // Update local DB
        groupDao.insertGroup(withTimestamp.toEntity())
        // Sync to cloud
        syncToCloud(withTimestamp)
    }

    override suspend fun deleteGroup(userId: String, groupId: String) = withContext(Dispatchers.IO) {
        println("🗑️ Repository: Deleting group $groupId")

        // STEP 1: Unassign all reminders from this group
        println("🗑️ Unassigning reminders from group $groupId")
        reminderDao.unassignRemindersFromGroup(groupId)

        // STEP 2: Delete from local DB
        groupDao.deleteGroup(groupId)

        // STEP 3: Delete from cloud
        try {
            firestoreDataSource.deleteGroup(userId, groupId)
        } catch (e: Exception) {
            println("❌ Failed to delete group from Firestore: ${e.message}")
        }

        println("✅ Group deleted and reminders unassigned successfully")
    }

    override suspend fun getGroupCount(userId: String): Int = withContext(Dispatchers.IO) {
        groupDao.getGroupCount(userId)
    }

    override suspend fun syncGroups(userId: String) = withContext(Dispatchers.IO) {
        val unsyncedGroups = groupDao.getUnsyncedGroups()
        unsyncedGroups.forEach { entity ->
            try {
                firestoreDataSource.saveGroup(entity.toDomain())
                groupDao.updateSyncStatus(entity.id, true)
            } catch (e: Exception) {
                println("❌ Sync failed for group ${entity.id}: ${e.message}")
            }
        }
    }

    private suspend fun syncToCloud(group: ReminderGroup) {
        println("☁️ syncToCloud called for group: ${group.name} (userId: ${group.userId})")
        try {
            println("☁️ Calling firestoreDataSource.saveGroup...")
            firestoreDataSource.saveGroup(group)
            println("☁️ saveGroup successful, updating sync status...")
            groupDao.updateSyncStatus(group.id, true)
            println("✅ Group synced to cloud: ${group.name}")
        } catch (e: Exception) {
            println("⚠️ Group cloud sync failed: ${e.message}")
            e.printStackTrace()
        }
    }


    override suspend fun unassignRemindersFromGroup(groupId: String) = withContext(Dispatchers.IO) {
        reminderDao.unassignRemindersFromGroup(groupId)
    }
}
