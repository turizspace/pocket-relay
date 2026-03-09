package com.pocketrelay.ui

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pocketrelay.relay.BroadcastEvent
import com.pocketrelay.relay.EventTracker

@Composable
fun EventFeedScreen(
    events: List<BroadcastEvent>,
    onBack: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0A1428))
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Header
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFF162749))
                    .padding(16.dp),
                contentAlignment = Alignment.CenterStart
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "Back",
                            tint = Color(0xFF00D9FF)
                        )
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "EVENT BROADCAST FEED",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF00D9FF),
                            letterSpacing = 2.sp
                        )
                        Text(
                            text = "${events.size} events",
                            fontSize = 11.sp,
                            color = Color(0xFFA0A0A0)
                        )
                    }
                }
            }

            // Events List
            if (events.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "No events broadcasted yet\nConnect clients to start receiving events",
                        fontSize = 14.sp,
                        color = Color(0xFFA0A0A0),
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(events) { event ->
                        EventCard(event)
                    }
                }
            }
        }
    }
}

@Composable
private fun EventCard(event: BroadcastEvent) {
    var expanded by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(Color(0xFF1E3A5F))
            .clickable { expanded = !expanded }
            .padding(12.dp)
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            // Header Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = EventTracker.getKindName(event.kind),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF00D9FF)
                    )
                    Text(
                        text = "${EventTracker.formatDate(event.createdAt)} @ ${EventTracker.formatTimestamp(event.createdAt)}",
                        fontSize = 10.sp,
                        color = Color(0xFFA0A0A0)
                    )
                }
                Text(
                    text = "▼",
                    fontSize = 12.sp,
                    color = Color(0xFF00FF41),
                    fontWeight = FontWeight.Bold
                )
            }

            if (expanded) {
                Spacer(modifier = Modifier.height(12.dp))

                // Event ID
                JsonField("id", EventTracker.truncateHash(event.id, 16))

                // Pubkey
                JsonField("pubkey", EventTracker.truncateHash(event.pubkey, 16))

                // Kind
                JsonField("kind", event.kind.toString())

                // Content Preview
                if (event.content.isNotEmpty()) {
                    JsonField("content", event.content.take(80) + if (event.content.length > 80) "..." else "")
                }

                // Tags
                if (event.tags.isNotEmpty()) {
                    Text(
                        text = "\"tags\": [",
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace,
                        color = Color(0xFF00D9FF)
                    )
                    event.tags.take(3).forEach { tag ->
                        Text(
                            text = "  ${tag}",
                            fontSize = 10.sp,
                            fontFamily = FontFamily.Monospace,
                            color = Color(0xFFA0A0A0),
                            modifier = Modifier.padding(start = 8.dp)
                        )
                    }
                    if (event.tags.size > 3) {
                        Text(
                            text = "  ... and ${event.tags.size - 3} more tags",
                            fontSize = 10.sp,
                            fontFamily = FontFamily.Monospace,
                            color = Color(0xFF00FF41)
                        )
                    }
                    Text(
                        text = "]",
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace,
                        color = Color(0xFF00D9FF)
                    )
                }

                // Signature
                JsonField("sig", EventTracker.truncateHash(event.signature, 20))
            }
        }
    }
}

@Composable
private fun JsonField(key: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.Top
    ) {
        Text(
            text = "\"$key\": ",
            fontSize = 11.sp,
            fontFamily = FontFamily.Monospace,
            color = Color(0xFF00D9FF),
            modifier = Modifier.widthIn(max = 80.dp)
        )
        Text(
            text = "\"$value\"",
            fontSize = 11.sp,
            fontFamily = FontFamily.Monospace,
            color = Color(0xFF00FF41),
            modifier = Modifier.weight(1f)
        )
    }
}
