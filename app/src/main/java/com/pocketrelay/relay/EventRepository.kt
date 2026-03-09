package com.pocketrelay.relay

interface EventRepository {
    suspend fun save(event: NostrEvent)
    suspend fun query(filters: List<Filter>): List<NostrEvent>
}