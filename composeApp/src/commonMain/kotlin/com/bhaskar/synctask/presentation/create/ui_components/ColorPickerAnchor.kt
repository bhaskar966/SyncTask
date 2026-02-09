package com.bhaskar.synctask.presentation.create.ui_components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties
import com.bhaskar.synctask.presentation.theme.ReminderColors
import com.bhaskar.synctask.presentation.utils.parseHexColor
import kotlin.math.roundToInt

@OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
fun ColorPickerAnchor(
    selectedColorHex: String?,
    onColorSelected: (String) -> Unit
) {
    var showPopup by remember { mutableStateOf(false) }
    val density = LocalDensity.current
    
    Box {
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(CircleShape)
                .background(selectedColorHex?.let { parseHexColor(it) } ?: MaterialTheme.colorScheme.primary)
                .border(1.dp, MaterialTheme.colorScheme.outlineVariant, CircleShape)
                .clickable { showPopup = true }
        )
        
        if (showPopup) {
            Popup(
                alignment = Alignment.TopStart,
                offset = with(density) { IntOffset(x = 18.dp.toPx().roundToInt(), y = 42.dp.toPx().roundToInt()) },
                onDismissRequest = { showPopup = false },
                properties = PopupProperties(focusable = true)
            ) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surface,
                    tonalElevation = 4.dp,
                    shadowElevation = 8.dp,
                    modifier = Modifier.width(200.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text("Select Color", style = MaterialTheme.typography.labelMedium, modifier = Modifier.padding(bottom = 8.dp))
                        
                        val allColors = ReminderColors.colors.chunked(9)
                        
                        val pagerState = rememberPagerState(pageCount = { allColors.size })
                        
                        HorizontalPager(
                            state = pagerState,
                            modifier = Modifier.fillMaxWidth().height(140.dp)
                        ) { page ->
                            val pageColors = allColors[page]
                            Column(
                                verticalArrangement = Arrangement.spacedBy(8.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier.fillMaxSize().padding(horizontal = 4.dp)
                            ) {
                                repeat(3) { r ->
                                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                        repeat(3) { c ->
                                            val index = r * 3 + c
                                            val colorOption = pageColors.getOrNull(index)
                                            if (colorOption != null) {
                                                Box(
                                                    modifier = Modifier
                                                        .size(32.dp)
                                                        .clip(CircleShape)
                                                        .background(colorOption.color)
                                                        .border(1.dp, MaterialTheme.colorScheme.outlineVariant, CircleShape)
                                                        .clickable { 
                                                            onColorSelected(colorOption.hex)
                                                            showPopup = false
                                                        }
                                                )
                                            } else {
                                                Spacer(Modifier.size(32.dp))
                                            }
                                        }
                                    }
                                }
                            }
                        }
                        
                        // Dots Indicator
                        Row(
                            Modifier.wrapContentHeight().fillMaxWidth().padding(top = 8.dp),
                            horizontalArrangement = Arrangement.Center
                        ) {
                            repeat(pagerState.pageCount) { iteration ->
                                val color = if (pagerState.currentPage == iteration) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant
                                Box(
                                    modifier = Modifier
                                        .padding(2.dp)
                                        .clip(CircleShape)
                                        .background(color)
                                        .size(6.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
