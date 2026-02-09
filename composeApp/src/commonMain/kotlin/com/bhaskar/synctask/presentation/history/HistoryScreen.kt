package com.bhaskar.synctask.presentation.history

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import com.bhaskar.synctask.domain.model.Reminder
import com.bhaskar.synctask.presentation.history.components.HistoryEvent
import com.bhaskar.synctask.presentation.history.components.HistoryState
import com.bhaskar.synctask.presentation.history.components.HistoryTab
import com.bhaskar.synctask.presentation.history.ui_components.HistoryItem
import com.bhaskar.synctask.presentation.list.ui_components.ContextMenuOverlay
import com.bhaskar.synctask.presentation.list.ui_components.ContextMenuItem
import com.bhaskar.synctask.presentation.list.ui_components.HeaderSection

@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)
@Composable
fun HistoryScreen(
    state: HistoryState,
    onEvent: (HistoryEvent) -> Unit,
    onNavigateToSettings: () -> Unit,
    onNavigateToReminder: (String) -> Unit
) {
    // Context Menu State
    var activeReminder by remember { mutableStateOf<Reminder?>(null) }
    var contextMenuPosition by remember { mutableStateOf(Offset.Zero) }
    var contextMenuSize by remember { mutableStateOf(IntSize.Zero) }
    var isContextMenuVisible by remember { mutableStateOf(false) }
    val blurRadius by animateDpAsState(targetValue = if (isContextMenuVisible) 16.dp else 0.dp)

    // Context Menu Overlay
    val contextMenuErrorCol = MaterialTheme.colorScheme.error
    val contextMenuItems = remember(activeReminder) {
        listOf(
            ContextMenuItem(
                label = "Delete from History",
                icon = androidx.compose.material.icons.Icons.Default.Delete,
                color = contextMenuErrorCol,
                onClick = {
                    activeReminder?.let { onEvent(HistoryEvent.OnDeleteReminder(it.id)) }
                    isContextMenuVisible = false
                }
            )
        )
    }

    ContextMenuOverlay(
        visible = isContextMenuVisible,
        position = contextMenuPosition,
        size = contextMenuSize,
        onDismiss = { isContextMenuVisible = false },
        menuItems = contextMenuItems,
        content = {
            activeReminder?.let {
                HistoryItem(
                   reminder = it,
                   onClick = {},
                   onLongClick = { _, _ -> }
                )
            }
        }
    )

    val pagerState = rememberPagerState(pageCount = { HistoryTab.entries.size })
    
    // Sync Pager with Tab Selection (State -> UI)
    LaunchedEffect(state.selectedTab) {
        pagerState.animateScrollToPage(state.selectedTab.index)
    }
    
    // Sync UI -> Event
    LaunchedEffect(pagerState.currentPage) {
        onEvent(HistoryEvent.OnTabSelected(HistoryTab.getByIndex(pagerState.currentPage)))
    }

    Scaffold(
        topBar = {
             // Header Section
             HeaderSection(
                 title = "History",
                 syncDeviceCount = 0, // Placeholder
                 searchQuery = state.searchQuery,
                 onSearchQueryChanged = { onEvent(HistoryEvent.OnSearchQueryChanged(it)) },
                 onNavigateToSettings = onNavigateToSettings,
                 searchPlaceholder = "Search in history..."
             )
        }
    ) { paddingValues ->
        Box(modifier = Modifier
            .fillMaxSize()
            .padding(paddingValues)
            .blur(blurRadius)
            .background(MaterialTheme.colorScheme.background)
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                // Search Overlay Mode
                if (state.searchQuery.isNotBlank()) {
                     LazyColumn(
                         modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp, vertical = 8.dp),
                         verticalArrangement = Arrangement.spacedBy(8.dp)
                     ) {
                         if (state.searchResults.isEmpty()) {
                             item {
                                 Text("No results found", modifier = Modifier.padding(16.dp), color = MaterialTheme.colorScheme.onSurfaceVariant)
                             }
                         } else {
                             items(state.searchResults, key = { it.id }) { reminder ->
                                 HistoryItem(
                                     reminder = reminder,
                                     onClick = { onNavigateToReminder(reminder.id) },
                                     onLongClick = { pos, size ->
                                         activeReminder = reminder
                                         contextMenuPosition = pos
                                         contextMenuSize = size
                                         isContextMenuVisible = true
                                     },
                                     modifier = Modifier.animateItem()
                                 )
                             }
                         }
                     }
                } else {
                    // Tabs
                    TabRow(
                        selectedTabIndex = state.selectedTab.index,
                        containerColor = MaterialTheme.colorScheme.surface,
                        contentColor = MaterialTheme.colorScheme.primary,
                        divider = {},
                        indicator = { tabPositions ->
                            TabRowDefaults.Indicator(
                                modifier = Modifier.tabIndicatorOffset(tabPositions[state.selectedTab.index]),
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    ) {
                        HistoryTab.entries.forEach { tab ->
                            Tab(
                                selected = state.selectedTab == tab,
                                onClick = { onEvent(HistoryEvent.OnTabSelected(tab)) },
                                text = { Text(tab.title) }
                            )
                        }
                    }

                    // Pager
                    HorizontalPager(
                        state = pagerState,
                        modifier = Modifier.weight(1f)
                    ) { page ->
                        val reminders = when (HistoryTab.getByIndex(page)) {
                            HistoryTab.Completed -> state.completedReminders
                            HistoryTab.Missed -> state.missedReminders
                            HistoryTab.Dismissed -> state.dismissedReminders
                        }
                        
                        if (reminders.isEmpty()) {
                            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                Text("No items", color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        } else {
                            LazyColumn(
                                modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp, vertical = 8.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                items(reminders, key = { it.id }) { reminder ->
                                    HistoryItem(
                                        reminder = reminder,
                                        onClick = { onNavigateToReminder(reminder.id) },
                                        onLongClick = { pos, size ->
                                             activeReminder = reminder
                                             contextMenuPosition = pos
                                             contextMenuSize = size
                                             isContextMenuVisible = true
                                        },
                                        modifier = Modifier.animateItem()
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
