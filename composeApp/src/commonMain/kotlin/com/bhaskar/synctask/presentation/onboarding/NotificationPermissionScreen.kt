package com.bhaskar.synctask.presentation.onboarding

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import dev.icerock.moko.permissions.Permission
import dev.icerock.moko.permissions.compose.BindEffect
import dev.icerock.moko.permissions.compose.rememberPermissionsControllerFactory
import dev.icerock.moko.permissions.notifications.REMOTE_NOTIFICATION
import io.github.alexzhirkevich.compottie.Compottie
import io.github.alexzhirkevich.compottie.DotLottie
import io.github.alexzhirkevich.compottie.LottieCompositionSpec
import io.github.alexzhirkevich.compottie.rememberLottieComposition
import io.github.alexzhirkevich.compottie.rememberLottiePainter
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.ExperimentalResourceApi
import synctask.composeapp.generated.resources.Res

@OptIn(ExperimentalResourceApi::class)
@Composable
fun NotificationPermissionScreen(
    onPermissionResult: () -> Unit
) {
    val factory = rememberPermissionsControllerFactory()
    val controller = remember(factory) { factory.createPermissionsController() }
    val scope = rememberCoroutineScope()

    BindEffect(controller)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.fillMaxSize()
        ) {
            val composition by rememberLottieComposition {
                LottieCompositionSpec.DotLottie(
                    Res.readBytes("files/notification_anim.lottie")
                )
            }

            val painter = rememberLottiePainter(
                composition = composition,
                iterations = Compottie.IterateForever
            )

            androidx.compose.foundation.Image(
                painter = painter,
                contentDescription = "Notification Animation",
                modifier = Modifier.size(250.dp)
            )

            Spacer(modifier = Modifier.height(32.dp))

            Text(
                text = "Enable Notifications",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "Get timely alerts for your reminders—even when the app is closed.",
                style = MaterialTheme.typography.bodyLarge,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .padding(bottom = 16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Button(
                onClick = {
                    scope.launch {
                        try {
                            controller.providePermission(Permission.REMOTE_NOTIFICATION)
                        } catch (e: Exception) {
                            // Ignored
                        } finally {
                            onPermissionResult()
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Grant Permission")
            }

//            TextButton(
//                onClick = onPermissionResult,
//                modifier = Modifier.fillMaxWidth()
//            ) {
//                Text("Skip for Now")
//            }
        }
    }
}
