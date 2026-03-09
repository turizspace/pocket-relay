package com.pocketrelay.relay

import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

data class BroadcastEvent(
    val id: String,
    val pubkey: String,
    val createdAt: Long,
    val kind: Int,
    val tags: List<List<String>>,
    val content: String,
    val signature: String,
    val receivedAt: LocalDateTime = LocalDateTime.now()
)

object EventTracker {
    private val events = mutableListOf<BroadcastEvent>()
    private val mutex = Mutex()
    private const val MAX_RECENT_EVENTS = 100

    suspend fun addEvent(
        id: String,
        pubkey: String,
        createdAt: Long,
        kind: Int,
        tags: List<List<String>>,
        content: String,
        signature: String
    ) {
        mutex.withLock {
            val event = BroadcastEvent(id, pubkey, createdAt, kind, tags, content, signature)
            events.add(0, event) // Add to front for chronological order (newest first)
            
            // Keep only recent events
            if (events.size > MAX_RECENT_EVENTS) {
                events.removeAt(events.size - 1)
            }
        }
    }

    suspend fun getRecentEvents(limit: Int = 50): List<BroadcastEvent> {
        return mutex.withLock {
            events.take(limit)
        }
    }

    suspend fun getEventById(id: String): BroadcastEvent? {
        return mutex.withLock {
            events.find { it.id == id }
        }
    }

    suspend fun getEventCount(): Int {
        return mutex.withLock {
            events.size
        }
    }

    suspend fun clearEvents() {
        mutex.withLock {
            events.clear()
        }
    }

    fun getKindName(kind: Int): String {
        return when (kind) {
            0 -> "Metadata"
            1 -> "Text"
            2 -> "Recommend Relay"
            3 -> "Contacts"
            4 -> "Encrypted DM"
            5 -> "Event Deletion"
            6 -> "Repost"
            7 -> "Reaction"
            40 -> "Channel Creation"
            41 -> "Channel Metadata"
            42 -> "Channel Message"
            43 -> "Channel Hide"
            44 -> "Channel Mute"
            1000 -> "Mute List"
            10000 -> "Muting"
            10001 -> "Pinned"
            else -> "Kind $kind"
        }
    }

    fun formatTimestamp(timestamp: Long): String {
        val sdf = java.text.SimpleDateFormat("HH:mm:ss", java.util.Locale.getDefault())
        return sdf.format(java.util.Date(timestamp * 1000))
    }

    fun formatDate(timestamp: Long): String {
        val sdf = java.text.SimpleDateFormat("MMM dd, yyyy", java.util.Locale.getDefault())
        return sdf.format(java.util.Date(timestamp * 1000))
    }

    fun truncateHash(hash: String, length: Int = 12): String {
        return if (hash.length > length) hash.take(length) + "..." else hash
    }

    // Load persisted events from repository into the in-memory tracker
    suspend fun loadFromRepository(repo: EventRepository, limit: Int = 100) {
        mutex.withLock {
            events.clear()
            val persisted = try {
                repo.query(listOf(Filter()))
            } catch (e: Exception) {
                emptyList()
            }
            // Add newest first
            persisted.sortedByDescending { it.created_at }
                .take(limit)
                .forEach { e ->
                    val be = BroadcastEvent(
                        id = e.id,
                        pubkey = e.pubkey,
                        createdAt = e.created_at,
                        kind = e.kind,
                        tags = e.tags,
                        content = e.content,
                        signature = e.sig
                    )
                    events.add(be)
                }
        }
    }
}
