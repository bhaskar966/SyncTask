package com.bhaskar.synctask.data.repository

import com.bhaskar.synctask.data.auth.AuthManager
import com.bhaskar.synctask.data.auth.AuthState
import com.bhaskar.synctask.data.toDomain
import com.bhaskar.synctask.data.toEntity
import com.bhaskar.synctask.db.TagDao
import com.bhaskar.synctask.domain.model.Tag
import com.bhaskar.synctask.domain.repository.TagRepository
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

class TagRepositoryImpl(
    private val tagDao: TagDao,
    private val firestoreDataSource: FirestoreDataSource,
    private val authManager: AuthManager,
    private val coroutineScope: CoroutineScope
) : TagRepository {
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
                        println("🔄 Starting Tags Firestore sync for user: ${state.uid}")
                        syncFromFirestore(state.uid)
                    }
                    is AuthState.Unauthenticated -> {
                        println("🔄 User logged out - stopping Tags sync")
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
            firestoreDataSource.getTags(userId).collect { cloudTags ->
                println("☁️ Received ${cloudTags.size} tags from Firestore")

                // Push local unsynced tags to cloud first
                val unsyncedTags = tagDao.getUnsyncedTags()
                println("📤 Found ${unsyncedTags.size} unsynced local tags")
                unsyncedTags.forEach { entity ->
                    try {
                        val localTag = entity.toDomain()
                        println("📤 Pushing unsynced tag to cloud: ${localTag.name}")
                        firestoreDataSource.saveTag(localTag)
                        tagDao.updateSyncStatus(localTag.id, true)
                    } catch (e: Exception) {
                        println("❌ Failed to push tag ${entity.id}: ${e.message}")
                        e.printStackTrace()
                    }
                }

                cloudTags.forEach { cloudTag ->
                    val localTag = tagDao.getTagById(cloudTag.id).firstOrNull()
                    if (localTag == null) {
                        // New tag from cloud
                        println("📥 New tag from cloud: ${cloudTag.name}")
                        tagDao.insertTag(cloudTag.copy(isSynced = true).toEntity())
                    } else {
                        // Update from cloud
                        println("📥 Update tag from cloud: ${cloudTag.name}")
                        tagDao.insertTag(cloudTag.copy(isSynced = true).toEntity())
                    }
                }

                // Delete local tags that are not in cloud
                val cloudIds = cloudTags.map { it.id }.toSet()
                val allLocalTags = tagDao.getAllTags(userId).firstOrNull() ?: emptyList()
                allLocalTags.forEach { localTagEntity ->
                    if (localTagEntity.id !in cloudIds) {
                        println("🗑️ Tag ${localTagEntity.id} not in cloud (and cloud update received), deleting locally")
                        tagDao.deleteTag(localTagEntity.id)
                    }
                }
            }
        } catch (e: Exception) {
            println("❌ Tags Firestore sync error: ${e.message}")
            e.printStackTrace()
        }
    }

    // PUBLIC API - Only reads from local DB
    override fun getTags(userId: String): Flow<List<Tag>> {
        return tagDao.getAllTags(userId)
            .map { entities -> entities.map { it.toDomain() } }
            .flowOn(Dispatchers.IO)
    }

    override fun getTagById(tagId: String): Flow<Tag?> {
        return tagDao.getTagById(tagId)
            .map { it?.toDomain() }
            .flowOn(Dispatchers.IO)
    }

    override suspend fun createTag(tag: Tag) = withContext(Dispatchers.IO) {
        println("💾 Repository: Creating tag - ${tag.name}")
        println("💾 Repository: Tag userId before: ${tag.userId}")
        println("💾 Repository: Current userId: $userId")

        val now = Clock.System.now().toEpochMilliseconds()
        val withTimestamp = tag.copy(
            userId = userId,
            createdAt = now,
            isSynced = false
        )

        println("💾 Repository: Tag userId after: ${withTimestamp.userId}")
        println("💾 Repository: Inserting tag to local DB...")

        // Save to local DB
        tagDao.insertTag(withTimestamp.toEntity())

        println("💾 Repository: Tag inserted, now syncing to cloud...")

        // Sync to cloud
        syncToCloud(withTimestamp)

        println("💾 Repository: Tag creation complete!")
    }

    override suspend fun updateTag(tag: Tag) = withContext(Dispatchers.IO) {
        println("💾 Repository: Updating tag - ${tag.name}")
        val withTimestamp = tag.copy(
            userId = userId,
            isSynced = false
        )

        // Update local DB
        tagDao.insertTag(withTimestamp.toEntity())
        // Sync to cloud
        syncToCloud(withTimestamp)
    }

    override suspend fun deleteTag(userId: String, tagId: String) = withContext(Dispatchers.IO) {
        println("🗑️ Repository: Deleting tag $tagId")
        // Delete from local DB
        tagDao.deleteTag(tagId)
        // Delete from cloud
        try {
            firestoreDataSource.deleteTag(userId, tagId)
        } catch (e: Exception) {
            println("❌ Failed to delete tag from Firestore: ${e.message}")
        }
    }

    override suspend fun getTagCount(userId: String): Int = withContext(Dispatchers.IO) {
        tagDao.getTagCount(userId)
    }

    override suspend fun syncTags(userId: String) = withContext(Dispatchers.IO) {
        val unsyncedTags = tagDao.getUnsyncedTags()
        unsyncedTags.forEach { entity ->
            try {
                firestoreDataSource.saveTag(entity.toDomain())
                tagDao.updateSyncStatus(entity.id, true)
            } catch (e: Exception) {
                println("❌ Sync failed for tag ${entity.id}: ${e.message}")
            }
        }
    }

    private suspend fun syncToCloud(tag: Tag) {
        println("☁️ syncToCloud called for tag: ${tag.name} (userId: ${tag.userId})")
        try {
            println("☁️ Calling firestoreDataSource.saveTag...")
            firestoreDataSource.saveTag(tag)
            println("☁️ saveTag successful, updating sync status...")
            tagDao.updateSyncStatus(tag.id, true)
            println("✅ Tag synced to cloud: ${tag.name}")
        } catch (e: Exception) {
            println("⚠️ Tag cloud sync failed: ${e.message}")
            e.printStackTrace()
        }
    }
}
