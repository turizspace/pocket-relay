package com.pocketrelay.relay

import io.ktor.websocket.WebSocketSession

data class Subscription(
    val id: String,
    val filters: List<Filter>,
    val socket: WebSocketSession
)