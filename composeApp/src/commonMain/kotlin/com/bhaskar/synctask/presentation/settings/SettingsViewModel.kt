@file:OptIn(kotlin.uuid.ExperimentalUuidApi::class)

package com.bhaskar.synctask.presentation.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bhaskar.synctask.data.auth.AuthManager
import com.bhaskar.synctask.domain.model.Tag
import com.bhaskar.synctask.domain.subscription.SubscriptionConfig
import com.bhaskar.synctask.presentation.settings.components.SettingsEvent
import com.bhaskar.synctask.presentation.settings.components.SettingsState

import kotlinx.coroutines.IO

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlin.time.Clock
import kotlin.uuid.Uuid

class SettingsViewModel(
    private val authManager: AuthManager,
    private val reminderRepository: com.bhaskar.synctask.domain.repository.ReminderRepository,
    private val groupRepository: com.bhaskar.synctask.domain.repository.GroupRepository,
    private val tagRepository: com.bhaskar.synctask.domain.repository.TagRepository,
    private val subscriptionRepository: com.bhaskar.synctask.domain.repository.SubscriptionRepository,
    private val profileRepository: com.bhaskar.synctask.domain.repository.ProfileRepository
) : ViewModel() {

    private val _state = MutableStateFlow(SettingsState())
    val state = _state.asStateFlow()

    init {
        observeAuthState()
        observeSubscription()
    }

    private fun observeAuthState() {
        viewModelScope.launch {
            authManager.authState.collect { authState ->
                if (authState is com.bhaskar.synctask.data.auth.AuthState.Authenticated) {
                    _state.update { 
                        it.copy(
                            userName = authState.displayName ?: "User",
                            userEmail = authState.email ?: "",
                            userPhotoUrl = authState.photoUrl
                        ) 
                    }
                    
                    // Fetch profile image if URL is present and image is not yet loaded
                    if (authState.photoUrl != null && _state.value.userProfileImage == null) {
                        fetchProfileImage(authState.photoUrl)
                    }
                }
            }
        }
    }

    private fun fetchProfileImage(url: String) {
        viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {
            _state.update { it.copy(isLoadingImage = true) }
            val result = profileRepository.fetchProfileImage(url)
            result.fold(
                onSuccess = { bytes ->
                    val bitmap = com.bhaskar.synctask.platform.byteArrayToImageBitmap(bytes)
                    _state.update { it.copy(userProfileImage = bitmap) }
                },
                onFailure = {
                    // Ignore failure, UI will show placeholder
                }
            )
            _state.update { it.copy(isLoadingImage = false) }
        }
    }

    private fun observeSubscription() {
        viewModelScope.launch {
            kotlinx.coroutines.flow.combine(
                subscriptionRepository.isPremiumSubscribed,
                subscriptionRepository.managementUrl,
                subscriptionRepository.activeSubscriptionInfo
            ) { isPremium, url, info ->
                Triple(isPremium, url, info)
            }.collect { (isPremium, url, info) ->
                _state.update { 
                    it.copy(
                        isPremium = isPremium,
                        managementUrl = url,
                        activeSubscriptionInfo = info
                    )
                }
            }
        }
    }

    fun updatePushPermissionState(isGranted: Boolean) {
        _state.update { it.copy(isPushEnabled = isGranted) }
    }

    fun onEvent(event: SettingsEvent) {
        when (event) {
            is SettingsEvent.OnThemeChanged -> {
                _state.update { it.copy(themeMode = event.mode) }
            }
            is SettingsEvent.OnPushToggled -> {
                // UI handles the actual permission request/toggle logic
                // Pass the RESULT back to VM via this event or updatePushPermissionState
                _state.update { it.copy(isPushEnabled = event.enabled) }
            }
            is SettingsEvent.OnEmailToggled -> {
                _state.update { it.copy(isEmailEnabled = event.enabled) }
            }
            is SettingsEvent.OnBadgeToggled -> {
                _state.update { it.copy(isBadgeEnabled = event.enabled) }
            }
            SettingsEvent.OnLogout -> {
                viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {
                    try {
                        authManager.signOut()
                        // Use repositories to clear data
                        reminderRepository.deleteAllLocalReminders()
                        groupRepository.deleteAllLocalGroups()
                        tagRepository.deleteAllLocalTags()
                    } catch (e: Exception) {
                        println("Error during logout: ${e.message}")
                    }
                }
            }
            SettingsEvent.OnRestorePurchases -> {
                viewModelScope.launch {
                    _state.update { it.copy(isRestoring = true) }
                    try {
                        val result = subscriptionRepository.restorePurchases()
                        result.fold(
                            onSuccess = { customerInfo ->
                                val hasPremium = customerInfo.entitlements[com.bhaskar.synctask.domain.subscription.SubscriptionConfig.PREMIUM_ENTITLEMENT_ID]?.isActive == true
                                if (hasPremium) {
                                    com.bhaskar.synctask.platform.showToast("✅ Purchase restored successfully!")
                                } else {
                                    com.bhaskar.synctask.platform.showToast("No previous purchases found")
                                }
                            },
                            onFailure = { error ->
                                com.bhaskar.synctask.platform.showToast("❌ Restore failed: ${error.message}")
                            }
                        )
                    } catch (e: Exception) {
                        com.bhaskar.synctask.platform.showToast("❌ Restore error: ${e.message}")
                    } finally {
                        _state.update { it.copy(isRestoring = false) }
                    }
                }
            }

            // Tag Events
            SettingsEvent.OnShowTagsDialog -> {
                observeTags()
                _state.update { it.copy(showTagsDialog = true, newTagName = "", tagsDialogError = null) }
            }
            SettingsEvent.OnHideTagsDialog -> {
                _state.update { it.copy(showTagsDialog = false) }
            }
            is SettingsEvent.OnNewTagNameChanged -> {
                _state.update { it.copy(newTagName = event.name, tagsDialogError = null) }
            }
            is SettingsEvent.OnNewTagColorChanged -> {
                _state.update { it.copy(newTagColor = event.color) }
            }
            SettingsEvent.OnCreateTag -> createTag()
            is SettingsEvent.OnDeleteTag -> {
                viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {
                    val userId = authManager.currentUserId ?: return@launch
                    tagRepository.deleteTag(userId, event.tagId)
                }
            }
            SettingsEvent.OnDismissPremiumDialog -> {
                _state.update { it.copy(showPremiumDialog = false, premiumDialogMessage = "", isMaxLimitReached = false) }
            }
        }
    }

    private var tagJob: kotlinx.coroutines.Job? = null
    private fun observeTags() {
        tagJob?.cancel()
        tagJob = viewModelScope.launch {
            val userId = authManager.currentUserId ?: return@launch
            tagRepository.getTags(userId).collect { tags ->
                _state.update { it.copy(tags = tags) }
            }
        }
    }

    private fun createTag() {
        val currentName = _state.value.newTagName.trim()
        val currentColor = _state.value.newTagColor
        
        if (currentName.isBlank()) {
            _state.update { it.copy(tagsDialogError = "Tag name cannot be empty") }
            return
        }
        
        // OPTIONAL: Check for duplicates if needed, but not strictly required by prompt.
        if (_state.value.tags.any { it.name.equals(currentName, ignoreCase = true) }) {
             _state.update { it.copy(tagsDialogError = "Tag already exists") }
             return
        }

        viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {
             val userId = authManager.currentUserId ?: return@launch
             val isPremium = subscriptionRepository.isPremiumSubscribed.value
             val currentCount = _state.value.tags.size // Or fetch from repo if preferred
             
             if (SubscriptionConfig.canAddTag(currentCount, isPremium)) {
                 try {
                     val newTag = Tag(
                         id = Uuid.random().toString(),
                         userId = userId,
                         name = currentName,
                         colorHex = currentColor,
                         createdAt = Clock.System.now().toEpochMilliseconds(),
                         isSynced = false
                     )
                     tagRepository.createTag(newTag)
                     _state.update { it.copy(newTagName = "", newTagColor = "#6366F1", tagsDialogError = null) }
                 } catch (e: Exception) {
                     _state.update { it.copy(tagsDialogError = "Failed to create tag") }
                 }
             } else {
                 val message = if (isPremium) {
                     "You have reached the maximum limit of ${SubscriptionConfig.Limits.PREMIUM_MAX_TAGS} tags."
                 } else {
                     SubscriptionConfig.UpgradeMessages.TAGS
                 }
                 _state.update { 
                     it.copy(
                         showPremiumDialog = true, 
                         premiumDialogMessage = message, 
                         isMaxLimitReached = isPremium 
                     ) 
                 }
             }
        }
    }
}
