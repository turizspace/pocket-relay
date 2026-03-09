package com.pocketrelay.service

import android.app.Service
import android.content.Intent
import android.os.IBinder
import android.util.Log
import com.pocketrelay.relay.*
import com.pocketrelay.util.Notify
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.File
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

class RelayService : Service() {

    private lateinit var server: RelayServer
    private lateinit var repo: EventRepository
    private val tag = "RelayService"

    override fun onCreate() {
        super.onCreate()
        try {
            Notify.create(this)
            Log.d(tag, "Notification channel created")
        } catch (e: Exception) {
            Log.e(tag, "Failed to create notification channel", e)
        }
        try {
            // Initialize disk-backed repository in app files directory
            val storageFile = File(filesDir, "events.json")
            repo = DiskEventRepository(storageFile)
            server = RelayServer(repo)

            // Load persisted events into EventTracker
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    EventTracker.loadFromRepository(repo)
                } catch (e: Exception) {
                    Log.e(tag, "Failed to load persisted events", e)
                }
            }
        } catch (e: Exception) {
            Log.e(tag, "Failed to initialize repository/server", e)
            repo = InMemoryEventRepository()
            server = RelayServer(repo)
        }
    }

    override fun onStartCommand(i: Intent?, f: Int, id: Int): Int {
        return try {
            val notification = Notify.notification(this)
            startForeground(1, notification)
            Log.d(tag, "Starting relay server...")
            server.start()
            Log.d(tag, "Relay server started successfully")
            START_STICKY
        } catch (e: SecurityException) {
            Log.e(tag, "Security error starting foreground service", e)
            stopSelf()
            START_NOT_STICKY
        } catch (e: Exception) {
            Log.e(tag, "Error starting relay service", e)
            stopSelf()
            START_NOT_STICKY
        }
    }

    override fun onDestroy() {
        try {
            server.stop()
            Log.d(tag, "Relay server stopped")
        } catch (e: Exception) {
            Log.e(tag, "Error stopping relay server", e)
        }
        super.onDestroy()
    }

    override fun onBind(i: Intent?): IBinder? = null
}