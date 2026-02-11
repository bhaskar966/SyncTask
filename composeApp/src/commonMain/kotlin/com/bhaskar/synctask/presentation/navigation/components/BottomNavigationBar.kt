package com.bhaskar.synctask.presentation.navigation.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Folder
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.compose.currentBackStackEntryAsState
import com.bhaskar.synctask.presentation.utils.BottomNavRoutes
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.animation.*
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.ui.draw.alpha
import androidx.compose.foundation.layout.offset
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInParent
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.setValue

data class BottomNavItem(
    val route: BottomNavRoutes,
    val label: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector
)

val bottomNavItems = listOf(
    BottomNavItem(
        route = BottomNavRoutes.RemindersScreen,
        label = "Reminders",
        selectedIcon = Icons.Filled.Notifications,
        unselectedIcon = Icons.Outlined.Notifications
    ),
    BottomNavItem(
        route = BottomNavRoutes.GroupsScreen,
        label = "Groups",
        selectedIcon = Icons.Filled.Folder,
        unselectedIcon = Icons.Outlined.Folder
    ),
    BottomNavItem(
        route = BottomNavRoutes.HistoryScreen,
        label = "History",
        selectedIcon = Icons.Filled.CheckCircle,
        unselectedIcon = Icons.Outlined.CheckCircle
    )
)

@Composable
fun BottomNavigationBar(
    navController: NavController,
    showAddButton: Boolean,
    onAddClick: () -> Unit
) {
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination

    // Animations - Reduced outer padding to give more space
    val pillStartPadding by animateDpAsState(
        targetValue = if (showAddButton) 12.dp else 48.dp,
        label = "pillStart"
    )
    
    val pillEndPadding by animateDpAsState(
        targetValue = if (showAddButton) 92.dp else 48.dp,
        label = "pillEnd"
    )

    val fabOffset by animateDpAsState(
        targetValue = if (showAddButton) 0.dp else (-80).dp,
        label = "fabOffset"
    )

    val fabAlpha by animateFloatAsState(
        targetValue = if (showAddButton) 1f else 0f,
        label = "fabAlpha"
    )

    val selectedIndex = bottomNavItems.indexOfFirst { item ->
         currentDestination?.hierarchy?.any { it.hasRoute(item.route::class) } == true
    }.let { if (it == -1) 0 else it }

    // Container for the floating bar
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 24.dp, top = 12.dp)
    ) {
        // Add Button (Behind - Rendered First)
        FloatingActionButton(
            onClick = { if (showAddButton) onAddClick() },
            containerColor = MaterialTheme.colorScheme.tertiary,
            contentColor = MaterialTheme.colorScheme.onTertiary,
            shape = CircleShape,
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .padding(end = 16.dp)
                .offset(x = fabOffset)
                .alpha(fabAlpha)
                .size(64.dp),
            elevation = FloatingActionButtonDefaults.elevation(
                defaultElevation = if (showAddButton) 4.dp else 0.dp,
                pressedElevation = 8.dp
            )
        ) {
            Icon(
                imageVector = Icons.Default.Add,
                contentDescription = "Add",
                modifier = Modifier.size(32.dp)
            )
        }

        // Navigation Pill Container
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = pillStartPadding, end = pillEndPadding)
                .height(64.dp),
            shape = CircleShape,
            color = MaterialTheme.colorScheme.surfaceVariant,
            tonalElevation = 4.dp,
            shadowElevation = 4.dp
        ) {
            androidx.compose.foundation.layout.BoxWithConstraints(
                modifier = Modifier.fillMaxSize()
            ) {
                // Dynamic Weights
                val weights = bottomNavItems.mapIndexed { index, _ ->
                    animateFloatAsState(
                        targetValue = if (index == selectedIndex) 2f else 1f,
                        animationSpec = tween(300)
                    ).value
                }
                
                val totalWeight = weights.sum()

                // Calculate Pill Position and Width based on weights
                // Note: maxWidth is Dp in BoxWithConstraints
                val pillWidth = maxWidth * (weights[selectedIndex] / totalWeight)
                
                // Sum of weights before selected index
                val weightBefore = weights.take(selectedIndex).sum()
                val pillOffset = maxWidth * (weightBefore / totalWeight)

                // Sliding Background Pill
                Box(
                    modifier = Modifier
                        .offset(x = pillOffset)
                        .width(pillWidth)
                        .fillMaxHeight()
                        .padding(6.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.surface)
                )

                // Foreground Items
                Row(
                    modifier = Modifier.fillMaxSize(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    bottomNavItems.forEachIndexed { index, item ->
                        NavItem(
                            item = item,
                            isSelected = index == selectedIndex,
                            modifier = Modifier.weight(weights[index]),
                            onClick = {
                                navController.navigate(item.route) {
                                    popUpTo(navController.graph.startDestinationId) {
                                        saveState = true
                                    }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun NavItem(
    item: BottomNavItem,
    isSelected: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val contentColor by animateColorAsState(
        targetValue = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
        animationSpec = tween(300)
    )
    
    val icon = if (isSelected) item.selectedIcon else item.unselectedIcon

    Box(
        modifier = modifier
            .fillMaxHeight()
            .clickable(
                onClick = onClick,
                interactionSource = remember { MutableInteractionSource() },
                indication = null 
            ),
        contentAlignment = Alignment.Center
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
            modifier = Modifier.padding(horizontal = 2.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = item.label,
                tint = contentColor,
                modifier = Modifier.size(24.dp)
            )

            androidx.compose.animation.AnimatedVisibility(
                visible = isSelected,
               enter = fadeIn() + expandHorizontally(),
               exit = fadeOut() + shrinkHorizontally()
            ) {
                Text(
                    text = item.label,
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
                    color = contentColor,
                    maxLines = 1,
                    modifier = Modifier.padding(start = 8.dp)
                )
            }
        }
    }
}
