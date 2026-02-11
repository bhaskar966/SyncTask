package com.bhaskar.synctask.presentation.onboarding

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import chaintech.videoplayer.host.MediaPlayerHost
import chaintech.videoplayer.model.VideoPlayerConfig
import chaintech.videoplayer.ui.video.VideoPlayerComposable
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.ExperimentalResourceApi
import synctask.composeapp.generated.resources.Res

@OptIn(ExperimentalFoundationApi::class, ExperimentalResourceApi::class)
@Composable
fun OnboardingScreen(
    onNavigateNext: () -> Unit
) {
    val pagerState = rememberPagerState(pageCount = { 2 })
    val scope = rememberCoroutineScope()

    Box(
        modifier = Modifier
            .fillMaxSize()
    ) {
        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            
            // Video and Content Area
            HorizontalPager(
                state = pagerState,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) { page ->
                OnboardingSlideContent(page = page)
            }

            // Bottom Control Area
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
                    .windowInsetsPadding(WindowInsets.navigationBars),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Skip Button
                TextButton(
                    onClick = onNavigateNext,
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Skip")
                }

                // Indicators
                Row(
                    modifier = Modifier.weight(1f),
                    horizontalArrangement = Arrangement.Center
                ) {
                    repeat(2) { iteration ->
                        val color = if (pagerState.currentPage == iteration) 
                            MaterialTheme.colorScheme.primary 
                        else 
                            MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)
                        
                        Box(
                            modifier = Modifier
                                .padding(4.dp)
                                .clip(CircleShape)
                                .background(color)
                                .size(8.dp)
                        )
                    }
                }

                // Next Button
                Button(
                    onClick = {
                        if (pagerState.currentPage < 1) {
                            scope.launch {
                                pagerState.animateScrollToPage(pagerState.currentPage + 1)
                            }
                        } else {
                            onNavigateNext()
                        }
                    },
                    modifier = Modifier.weight(1f)
                ) {
                    Text(if (pagerState.currentPage == 1) "Next" else "Next")
                }
            }
        }
    }
}

@OptIn(ExperimentalResourceApi::class)
@Composable
fun OnboardingSlideContent(page: Int) {
    val title = if (page == 0) "Always In Sync" else "Your Rules, Your Way"
    val body = if (page == 0) 
        "Snooze, complete, Dismiss on one device—it updates everywhere instantly! Android, iOS, always in sync."
    else 
        "Create reminders with powerful recurring options, pre-reminders, subtasks, and custom snoozing. Everything you need, beautifully organized."


    val videoFile = remember(page) {
        if (page == 0) Res.getUri("files/onboarding_slide_1.mp4")
        else Res.getUri("files/onboarding_slide_2.mp4")
    }

    val playerHost = remember { MediaPlayerHost(
        mediaUrl = videoFile,
    ) }

    Column(
        modifier = Modifier
            .fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Top // Align to top as requested
    ) {
        // Video Player Container
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1.2f) // Give more space to video
                .clip(RoundedCornerShape(16.dp))
                .background(Color.Black)
        ) {
            VideoPlayerComposable(
                modifier = Modifier.fillMaxSize(),
                playerHost = playerHost,
                playerConfig = VideoPlayerConfig(
                    showControls = false,
                    isPauseResumeEnabled = false,
                    isSeekBarVisible = false,
                    isDurationVisible = false,
                    isAutoHideControlEnabled = true,
                    isFastForwardBackwardEnabled = false,
                    isMuteControlEnabled = false,
                    isSpeedControlEnabled = false,
                    isFullScreenEnabled = false,
                    isScreenResizeEnabled = false,
                )
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Text Content
        Column(
            modifier = Modifier
                .weight(0.8f)
                .fillMaxWidth()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = body,
                style = MaterialTheme.typography.bodyLarge,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
