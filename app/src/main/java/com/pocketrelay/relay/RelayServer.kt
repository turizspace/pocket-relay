package com.pocketrelay.relay

import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.server.websocket.*
import io.ktor.server.routing.*
import io.ktor.server.application.*
import io.ktor.websocket.Frame
import android.util.Log

class RelayServer(private val repo: EventRepository) {
    private var engine: ApplicationEngine? = null
    private val tag = "RelayServer"

    fun start(port: Int = 4444) {
        try {
            Log.d(tag, "Starting relay server on port $port...")
            
            engine = embeddedServer(Netty, port) {
                install(WebSockets) {
                    maxFrameSize = 65536
                }
                routing {
                    webSocket("/") {
                        val remoteAddress = try {
                            call.request.local.remoteHost ?: "unknown"
                        } catch (e: Exception) {
                            "unknown"
                        }
                        
                        try {
                            // Track connection
                            ConnectionTracker.addConnection(this, remoteAddress)
                            Log.d(tag, "Client connected from $remoteAddress")
                            
                            for (frame in incoming) {
                                if (frame is Frame.Text) {
                                    try {
                                        val text = frame.data.decodeToString()
                                        NostrHandler.handle(text, this, repo)
                                    } catch (e: Exception) {
                                        Log.e(tag, "Error handling frame", e)
                                        send(Frame.Text("[\"ERROR\",\"decode failed\"]"))
                                    }
                                }
                            }
                        } finally {
                            // Remove connection on disconnect
                            ConnectionTracker.removeConnection(remoteAddress)
                            Log.d(tag, "Client disconnected from $remoteAddress")
                        }
                    }
                }
            }.start(false)

            Log.d(tag, "Relay server started successfully on ws://0.0.0.0:$port/")
        } catch (e: Exception) {
            Log.e(tag, "Failed to start relay server", e)
            throw e
        }
    }

    fun stop() {
        try {
            engine?.stop()
            Log.d(tag, "Relay server stopped")
        } catch (e: Exception) {
            Log.e(tag, "Error stopping relay server", e)
        }
    }
}
