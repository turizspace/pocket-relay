package com.pocketrelay.relay

import io.ktor.websocket.*
import kotlinx.serialization.json.*

object NostrHandler {

    private val json = Json { ignoreUnknownKeys = true }
    private const val MAX_MESSAGE_SIZE = 65536 // 64KB

    suspend fun handle(text: String, socket: WebSocketSession, repo: EventRepository) {
        // Validate message size
        if (text.length > MAX_MESSAGE_SIZE) {
            socket.send("[\"ERROR\",\"message too large\"]")
            return
        }
        
        try {
            val arr = json.parseToJsonElement(text).jsonArray
            
            // Validate minimum array size
            if (arr.isEmpty()) {
                socket.send("[\"ERROR\",\"empty message\"]")
                return
            }

            when (arr[0].jsonPrimitive.content) {

                "EVENT" -> {
                    if (arr.size < 2) {
                        socket.send("[\"ERROR\",\"EVENT requires 2 elements\"]")
                        return
                    }
                    val event = json.decodeFromJsonElement<NostrEvent>(arr[1])
                    val isValid = EventValidator.valid(event)
                    if (!isValid) {
                        // Save and track invalid events as well for testing/visibility
                        try {
                            repo.save(event)
                        } catch (e: Exception) {
                            // ignore persistence errors
                        }
                        try {
                            EventTracker.addEvent(
                                id = event.id,
                                pubkey = event.pubkey,
                                createdAt = event.created_at,
                                kind = event.kind,
                                tags = event.tags,
                                content = event.content,
                                signature = event.sig
                            )
                        } catch (e: Exception) {
                            // ignore tracking errors
                        }
                        socket.send("[\"OK\",\"${event.id}\",false,\"invalid\"]")
                    } else {
                        repo.save(event)
                        try {
                            EventTracker.addEvent(
                                id = event.id,
                                pubkey = event.pubkey,
                                createdAt = event.created_at,
                                kind = event.kind,
                                tags = event.tags,
                                content = event.content,
                                signature = event.sig
                            )
                        } catch (e: Exception) {
                            // Ignore tracking errors
                        }
                        socket.send("[\"OK\",\"${event.id}\",true,\"\"]")
                    }
                    try {
                        Subscriptions.matching(event).forEach {
                            // Send event to matched subscriptions
                            val response = "[\"EVENT\",\"${it.id}\",${eventToJson(event)}]"
                            socket.send(response)
                        }
                    } catch(e: Exception) {
                        // Ignore send errors
                    }
                }

                "REQ" -> {
                    if (arr.size < 2) {
                        socket.send("[\"ERROR\",\"REQ requires at least 2 elements\"]")
                        return
                    }
                    val subId = arr[1].jsonPrimitive.content
                    val filters = arr.drop(2).map { json.decodeFromJsonElement<Filter>(it) }
                    Subscriptions.add(Subscription(subId, filters, socket))
                    try {
                        repo.query(filters).forEach {
                            val response = "[\"EVENT\",\"$subId\",${eventToJson(it)}]"
                            socket.send(response)
                        }
                    } catch(e: Exception) {
                        // Ignore send errors
                    }
                    socket.send("[\"EOSE\",\"$subId\"]")
                }

                "CLOSE" -> {
                    if (arr.size < 2) {
                        socket.send("[\"ERROR\",\"CLOSE requires 2 elements\"]")
                        return
                    }
                    Subscriptions.remove(arr[1].jsonPrimitive.content, socket)
                }
                
                else -> {
                    socket.send("[\"ERROR\",\"unknown command\"]")
                }
            }
        } catch (e: Exception) {
            val errorMsg = escapeJson(e.message.orEmpty().take(100))
            socket.send("[\"ERROR\",\"$errorMsg\"]")
        }
    }
    
    private fun eventToJson(event: NostrEvent): String {
        val tagsJson = event.tags.joinToString(",") { tag ->
            "[" + tag.joinToString(",") { "\"${escapeJson(it)}\"" } + "]"
        }
        return "{\"id\":\"${event.id}\",\"pubkey\":\"${event.pubkey}\",\"created_at\":${event.created_at},\"kind\":${event.kind},\"tags\":[$tagsJson],\"content\":\"${escapeJson(event.content)}\",\"sig\":\"${event.sig}\"}"
    }
    
    private fun escapeJson(s: String): String {
        return s.replace("\\", "\\\\")
            .replace("\"", "\\\"")
            .replace("\n", "\\n")
            .replace("\r", "\\r")
            .replace("\t", "\\t")
    }
}