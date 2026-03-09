package com.pocketrelay.ui

import android.content.Context
import android.content.Intent
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pocketrelay.service.RelayService
import com.pocketrelay.relay.ConnectionTracker
import com.pocketrelay.relay.EventTracker
import com.pocketrelay.relay.BroadcastEvent
import com.pocketrelay.util.NetworkUtil
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay

class MainViewModel : ViewModel() {
    var running = false
        private set

    private val _connectionCount = MutableStateFlow(0)
    val connectionCount = _connectionCount.asStateFlow()

    private val _eventsList = MutableStateFlow<List<BroadcastEvent>>(emptyList())
    val eventsList = _eventsList.asStateFlow()

    private val _eventCount = MutableStateFlow(0)
    val eventCount = _eventCount.asStateFlow()

    val status get() = if (running) "Running" else "Stopped"
    val relayUrl get() = "ws://${NetworkUtil.ip()}:4444"

    init {
        // Poll connection count and events every 500ms
        viewModelScope.launch {
            while (true) {
                try {
                    val count = ConnectionTracker.getConnectionCount()
                    _connectionCount.value = count
                    
                    val events = EventTracker.getRecentEvents(50)
                    _eventsList.value = events
                    
                    val eventCnt = EventTracker.getEventCount()
                    _eventCount.value = eventCnt
                } catch (e: Exception) {
                    _connectionCount.value = 0
                    _eventCount.value = 0
                }
                delay(500)
            }
        }
    }

    fun toggle(ctx: Context) {
        // Use application context to avoid memory leaks
        val appContext = ctx.applicationContext
        val i = Intent(appContext, RelayService::class.java)
        if (running) {
            appContext.stopService(i)
            viewModelScope.launch {
                ConnectionTracker.clearConnections()
                EventTracker.clearEvents()
                _connectionCount.value = 0
                _eventCount.value = 0
                _eventsList.value = emptyList()
            }
        } else {
            appContext.startForegroundService(i)
        }
        running = !running
    }
}

