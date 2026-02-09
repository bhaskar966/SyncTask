package com.bhaskar.synctask.presentation.list.ui_components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import com.bhaskar.synctask.domain.model.Reminder

import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.material3.HorizontalDivider
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Arrangement

data class ContextMenuItem(
    val label: String,
    val icon: ImageVector,
    val color: Color,
    val onClick: () -> Unit
)

@Composable
fun ContextMenuOverlay(
    visible: Boolean,
    position: Offset?,
    size: IntSize?,
    onDismiss: () -> Unit,
    menuItems: List<ContextMenuItem>,
    content: @Composable () -> Unit
) {
    if (!visible || position == null || size == null) return

    val density = LocalDensity.current
    val animScale = remember { Animatable(0.9f) }

    LaunchedEffect(visible) {
        if (visible) {
            animScale.animateTo(
                targetValue = 1.05f,
                animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow)
            )
        }
    }

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .zIndex(100f)
    ) {
        val screenHeight = constraints.maxHeight.toFloat()
        
        val itemHeightPx = size.height.toFloat()
        val menuHeightPx = with(density) { (menuItems.size * 56).dp.toPx() } // Approx height
        val paddingPx = with(density) { 24.dp.toPx() }
        
        val totalContentHeight = itemHeightPx + menuHeightPx + paddingPx
        
        // Calculate Target Y
        var targetY = position.y
        
        // Check if closer to top or bottom
        val screenCenterY = screenHeight / 2
        val itemCenterY = position.y + (itemHeightPx / 2)
        
        // Dynamic Offset based on position (Tend to middle)
        if (itemCenterY < screenCenterY * 0.5) {
             targetY += paddingPx
        } else if (itemCenterY > screenCenterY * 1.5) {
            targetY -= paddingPx
        }

        // Hard Screen Bounds Check
        if (targetY + totalContentHeight > screenHeight - paddingPx) {
            targetY = screenHeight - totalContentHeight - paddingPx
        }
        if (targetY < paddingPx) {
            targetY = paddingPx
        }

        val finalYOffset = targetY.toInt()

        // Scrim
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.6f))
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null
                ) { onDismiss() }
        )

        // Content (Card + Menu)
        Box(
            modifier = Modifier
                .offset { IntOffset(position.x.toInt(), finalYOffset) }
                .width(with(density) { size.width.toDp() })
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // The Reminder Card (Scaled)
                Box(
                    modifier = Modifier
                        .scale(animScale.value)
                        .clickable(enabled = false) {}
                ) {
                    content()
                }

                Spacer(modifier = Modifier.height(12.dp))

                // The Menu Actions
                Surface(
                    color = MaterialTheme.colorScheme.surface,
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.wrapContentSize(),
                    shadowElevation = 8.dp,
                    tonalElevation = 4.dp
                ) {
                    Column(
                        modifier = Modifier.width(IntrinsicSize.Max)
                    ) {
                        menuItems.forEachIndexed { index, item ->
                            if (index > 0) {
                                HorizontalDivider(
                                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                                )
                            }
                            
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { 
                                        item.onClick()
                                        onDismiss()
                                    }
                                    .padding(horizontal = 20.dp, vertical = 16.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(16.dp)
                            ) {
                                Icon(
                                    imageVector = item.icon,
                                    contentDescription = item.label,
                                    tint = item.color
                                )
                                Text(
                                    text = item.label,
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                                    color = item.color
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
