package com.pocketrelay.relay

import io.ktor.websocket.WebSocketSession
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

data class Connection(
    val id: String,
    val connectedAt: LocalDateTime,
    val remoteAddress: String
)

object ConnectionTracker {
    private val connections = mutableMapOf<String, Connection>()
    private val mutex = Mutex()

    suspend fun addConnection(session: WebSocketSession, remoteAddress: String) {
        val id = "${System.currentTimeMillis()}-${(Math.random() * 100000).toInt()}"
        mutex.withLock {
            connections[id] = Connection(id, LocalDateTime.now(), remoteAddress)
        }
    }

    suspend fun removeConnection(address: String) {
        mutex.withLock {
            val toRemove = connections.filter { it.value.remoteAddress == address }.keys
            toRemove.forEach { connections.remove(it) }
        }
    }

    suspend fun getActiveConnections(): List<Connection> {
        return mutex.withLock {
            connections.values.toList()
        }
    }

    suspend fun getConnectionCount(): Int {
        return mutex.withLock {
            connections.size
        }
    }

    suspend fun clearConnections() {
        mutex.withLock {
            connections.clear()
        }
    }

    fun formatConnectionTime(connection: Connection): String {
        val formatter = DateTimeFormatter.ofPattern("HH:mm:ss")
        return connection.connectedAt.format(formatter)
    }
}
