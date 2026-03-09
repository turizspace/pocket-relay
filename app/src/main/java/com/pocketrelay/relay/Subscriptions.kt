package com.pocketrelay.relay

import io.ktor.websocket.WebSocketSession
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

object Subscriptions {
    private val subs = mutableSetOf<Subscription>()
    private val mutex = Mutex() // Thread-safe access

    suspend fun add(sub: Subscription) = mutex.withLock {
        subs.add(sub)
    }
    
    suspend fun remove(id: String, socket: WebSocketSession) = mutex.withLock {
        subs.removeIf { it.id == id && it.socket == socket }
    }

    suspend fun matching(event: NostrEvent): List<Subscription> = mutex.withLock {
        subs.filter { s -> s.filters.any { it.matches(event) } }
    }
}