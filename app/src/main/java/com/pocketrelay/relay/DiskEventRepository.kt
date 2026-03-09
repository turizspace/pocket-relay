package com.pocketrelay.relay

import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.encodeToString
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import java.io.File

class DiskEventRepository(private val storageFile: File) : EventRepository {
    private val mutex = Mutex()
    private val json = Json { prettyPrint = false; ignoreUnknownKeys = true }

    init {
        // ensure file exists
        if (!storageFile.exists()) {
            storageFile.parentFile?.mkdirs()
            storageFile.writeText("[]")
        }
    }

    private suspend fun loadAll(): MutableList<NostrEvent> = mutex.withLock {
        val text = storageFile.readText()
        try {
            json.decodeFromString<List<NostrEvent>>(text).toMutableList()
        } catch (e: Exception) {
            mutableListOf()
        }
    }

    private suspend fun persistAll(events: List<NostrEvent>) = mutex.withLock {
        val text = json.encodeToString(events)
        storageFile.writeText(text)
    }

    override suspend fun save(event: NostrEvent) {
        val events = loadAll()
        events.add(event)
        // trim size
        if (events.size > 10000) events.removeAt(0)
        persistAll(events)
    }

    override suspend fun query(filters: List<Filter>): List<NostrEvent> {
        val events = loadAll()
        // Return events that match any of the provided filters
        return events.filter { e -> filters.isEmpty() || filters.any { it.matches(e) } }
    }
}
