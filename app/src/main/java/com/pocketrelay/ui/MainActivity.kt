package com.pocketrelay.ui

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.activity.compose.BackHandler
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.background
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.clickable
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.animation.core.*
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.Alignment
import android.content.ClipboardManager
import android.content.ClipData
import android.content.Context
import com.pocketrelay.util.NetworkUtil
import androidx.compose.ui.graphics.graphicsLayer

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            val vm: MainViewModel = viewModel()
            MaterialTheme(
                colorScheme = darkColorScheme(
                    primary = Color(0xFF00D9FF),
                    onPrimary = Color(0xFF0A1428),
                    secondary = Color(0xFF00FF41),
                    onSecondary = Color(0xFF0A1428),
                    background = Color(0xFF0A1428),
                    surface = Color(0xFF162749),
                    surfaceVariant = Color(0xFF1E3A5F),
                    onBackground = Color(0xFFFFFFFF),
                    onSurface = Color(0xFFFFFFFF)
                )
            ) {
                MatrixRelayUI(vm, this@MainActivity)
            }
        }
    }
}

@Composable
fun MatrixRelayUI(vm: MainViewModel, context: Context) {
    var showEventFeed by remember { mutableStateOf(false) }
    
    if (showEventFeed) {
        // Intercept system back / swipe-to-go-back and return to dashboard
        BackHandler(enabled = true) {
            showEventFeed = false
        }
        val events = vm.eventsList.collectAsState()
        EventFeedScreen(events.value) { showEventFeed = false }
    } else {
        MainDashboard(vm, context) { showEventFeed = true }
    }
}

@Composable
private fun MainDashboard(vm: MainViewModel, context: Context, onShowFeed: () -> Unit) {
    val infiniteTransition = rememberInfiniteTransition(label = "matrixGlow")
    val glowAlpha by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = EaseInOutQuad),
            repeatMode = RepeatMode.Reverse
        ),
        label = "glowAnimation"
    )

    val pulsing = rememberInfiniteTransition(label = "pulseAnimation")
    val pulseScale by pulsing.animateFloat(
        initialValue = 1f,
        targetValue = 1.1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = EaseInOutQuad),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseScale"
    )

    var copied by remember { mutableStateOf(false) }
    val clipboardManager = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0A1428))
    ) {
        // Animated background glow effect
        Box(
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer(alpha = glowAlpha * 0.05f)
                .background(Color(0xFF00D9FF))
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(32.dp))

            // Logo Section with Glow Effect
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(120.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(Color(0xFF162749))
                    .shadow(
                        elevation = 12.dp,
                        shape = RoundedCornerShape(16.dp),
                        ambientColor = Color(0xFF00D9FF).copy(alpha = 0.3f),
                        spotColor = Color(0xFF00FF41).copy(alpha = 0.2f)
                    )
                    .padding(16.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "POCKET",
                        fontSize = 32.sp,
                        fontWeight = FontWeight.Black,
                        color = Color(0xFF00D9FF),
                        letterSpacing = 3.sp
                    )
                    Text(
                        text = "RELAY",
                        fontSize = 28.sp,
                        fontWeight = FontWeight.Light,
                        color = Color(0xFF00FF41),
                        letterSpacing = 4.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(40.dp))

            // Status Indicator Section
            StatusIndicatorSection(vm.running, pulseScale)

            Spacer(modifier = Modifier.height(24.dp))

            // Active Connections Section
            val connectionCount = vm.connectionCount.collectAsState()
            ConnectionsSection(connectionCount.value)

            Spacer(modifier = Modifier.height(32.dp))

            // Main Control Button
            ControlButton(
                running = vm.running,
                onClick = { vm.toggle(context) }
            )

            Spacer(modifier = Modifier.height(32.dp))

            // Event Feed Button
            val eventCount = vm.eventCount.collectAsState()
            EventFeedButton(eventCount.value, onClick = { onShowFeed() })

            Spacer(modifier = Modifier.height(40.dp))

            // Relay URL + IP
            val relayUrl = vm.relayUrl
            val ipAddress = NetworkUtil.ip()
            RelayURLSection(relayUrl = relayUrl, ipAddress = ipAddress, copied = copied, clipboardManager = clipboardManager) { copied = true }

            Spacer(modifier = Modifier.height(24.dp))

            // Info Panels
            InfoPanelsSection()

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
private fun StatusIndicatorSection(running: Boolean, pulseScale: Float) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(Color(0xFF1E3A5F))
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "STATUS",
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFFA0A0A0),
            letterSpacing = 2.sp
        )

        Spacer(modifier = Modifier.height(12.dp))

        Box(
            modifier = Modifier
                .size(60.dp)
                .scale(pulseScale)
                .clip(CircleShape)
                .background(
                    if (running) Color(0xFF00FF41)
                    else Color(0xFF404040)
                ),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = if (running) "●" else "○",
                fontSize = 36.sp,
                color = if (running) Color(0xFF0A1428)
                else Color(0xFF606060),
                fontWeight = FontWeight.Bold
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        Text(
            text = if (running) "RELAY ACTIVE" else "RELAY OFFLINE",
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            color = if (running) Color(0xFF00FF41) else Color(0xFF909090)
        )
    }
}

@Composable
private fun ConnectionsSection(count: Int) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(Color(0xFF1E3A5F))
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "ACTIVE CONNECTIONS",
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFFA0A0A0),
            letterSpacing = 2.sp
        )

        Spacer(modifier = Modifier.height(12.dp))

        Box(
            modifier = Modifier
                .size(80.dp)
                .clip(CircleShape)
                .background(
                    Color(0xFF1A2942)
                ),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
                modifier = Modifier.fillMaxSize()
            ) {
                Text(
                    text = count.toString(),
                    fontSize = 32.sp,
                    color = Color(0xFF00D9FF),
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = if (count == 1) "client" else "clients",
                    fontSize = 10.sp,
                    color = Color(0xFF00FF41)
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        Text(
            text = if (count > 0) "✓ Connected devices" else "• Waiting for connections",
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium,
            color = if (count > 0) Color(0xFF00FF41) else Color(0xFFA0A0A0)
        )
    }
}

@Composable
private fun RelayURLSection(
    relayUrl: String,
    ipAddress: String,
    copied: Boolean,
    clipboardManager: ClipboardManager,
    onCopied: () -> Unit
) {
    LaunchedEffect(copied) {
        if (copied) {
            kotlinx.coroutines.delay(2000)
            onCopied()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(Color(0xFF162749))
            .padding(16.dp)
    ) {
        Text(
            text = "RELAY ENDPOINT",
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFFA0A0A0),
            letterSpacing = 2.sp
        )

        Spacer(modifier = Modifier.height(12.dp))

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(8.dp))
                .background(Color(0xFF0A1428).copy(alpha = 0.5f))
                .clickable {
                    val clip = android.content.ClipData.newPlainText("relay_url", relayUrl)
                    clipboardManager.setPrimaryClip(clip)
                    onCopied.invoke()
                }
                .padding(12.dp),
            contentAlignment = Alignment.Center
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = relayUrl,
                    fontSize = 13.sp,
                    color = Color(0xFF00D9FF),
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.weight(1f)
                )

                AnimatedVisibility(
                    visible = copied,
                    enter = fadeIn(),
                    exit = fadeOut()
                ) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = "Copied",
                        tint = Color(0xFF00FF41),
                        modifier = Modifier.size(20.dp)
                    )
                }

                if (!copied) {
                    Text(
                        text = "📋",
                        fontSize = 16.sp,
                        color = Color(0xFFA0A0A0),
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }

    }
}

@Composable
private fun EventFeedButton(count: Int, onClick: () -> Unit) {
    Button(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp)
            .clip(RoundedCornerShape(16.dp)),
        colors = ButtonDefaults.buttonColors(
            containerColor = Color(0xFF1E3A5F)
        ),
        shape = RoundedCornerShape(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            Text(
                text = "📊 VIEW EVENT FEED",
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF00D9FF),
                letterSpacing = 1.sp
            )
            Box(
                modifier = Modifier
                    .clip(CircleShape)
                    .background(Color(0xFF00FF41))
                    .padding(horizontal = 8.dp, vertical = 4.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = count.toString(),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF0A1428)
                )
            }
        }
    }
}

@Composable
private fun ControlButton(running: Boolean, onClick: () -> Unit) {
    val animatedCornerRadius by animateDpAsState(
        targetValue = if (running) 12.dp else 16.dp,
        animationSpec = tween(300),
        label = "cornerRadiusAnimation"
    )

    Button(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp)
            .clip(RoundedCornerShape(animatedCornerRadius))
            .shadow(
                elevation = 16.dp,
                shape = RoundedCornerShape(animatedCornerRadius),
                ambientColor = if (running) Color(0xFF00FF41).copy(alpha = 0.4f)
                else Color(0xFFFF6B6B).copy(alpha = 0.3f),
                spotColor = if (running) Color(0xFF00FF41).copy(alpha = 0.5f)
                else Color(0xFFFF6B6B).copy(alpha = 0.4f)
            ),
        colors = ButtonDefaults.buttonColors(
            containerColor = if (running) Color(0xFF00FF41)
            else Color(0xFF1E3A5F)
        ),
        shape = RoundedCornerShape(animatedCornerRadius)
    ) {
        Text(
            text = if (running) "⊕ STOP RELAY" else "⊙ START RELAY",
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            color = if (running) Color(0xFF0A1428) else Color(0xFF00FF41),
            letterSpacing = 1.sp
        )
    }
}

@Composable
private fun InfoPanelsSection() {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text = "SYSTEM INFO",
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFFA0A0A0),
            letterSpacing = 2.sp
        )

        InfoCard(
            title = "PROTOCOL",
            value = "Nostr NIP-01",
            icon = "◆"
        )

        InfoCard(
            title = "CONNECTION",
            value = "WebSocket (WSS)",
            icon = "⟷"
        )

        InfoCard(
            title = "SECURITY",
            value = "Schnorr Verified",
            icon = "🔐"
        )

        InfoCard(
            title = "MODE",
            value = "Anonymous Relay",
            icon = "⚔"
        )
    }
}

@Composable
private fun InfoCard(title: String, value: String, icon: String) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(Color(0xFF162749))
            .padding(12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFFA0A0A0),
                    letterSpacing = 1.sp
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = value,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                    color = Color(0xFF00D9FF)
                )
            }
            Text(
                text = icon,
                fontSize = 24.sp,
                color = Color(0xFF00FF41).copy(alpha = 0.6f)
            )
        }
    }
}