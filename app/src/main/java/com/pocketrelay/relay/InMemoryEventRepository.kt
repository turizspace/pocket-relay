package com.pocketrelay.relay

import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

class InMemoryEventRepository : EventRepository {
    private val events = mutableListOf<NostrEvent>()
    private val mutex = Mutex() // Thread-safe access
    
    companion object {
        private const val MAX_EVENTS = 10000
        private const val MAX_EVENT_AGE_SECONDS = 86400L // 24 hours
    }

    override suspend fun save(event: NostrEvent) {
        mutex.withLock {
            events.add(event)
            
            // Implement size limit with FIFO eviction
            if (events.size > MAX_EVENTS) {
                events.removeAt(0)
            }
            
            // Remove old events
            val cutoffTime = System.currentTimeMillis() / 1000 - MAX_EVENT_AGE_SECONDS
            events.removeAll { it.created_at < cutoffTime }
        }
    }

    override suspend fun query(filters: List<Filter>): List<NostrEvent> =
        mutex.withLock {
            events.filter { e -> filters.any { it.matches(e) } }
        }
}