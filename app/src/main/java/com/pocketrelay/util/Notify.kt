package com.pocketrelay.util

import android.app.*
import android.content.Context
import androidx.core.app.NotificationCompat

object Notify {
    private const val ID = "relay"

    fun create(ctx: Context) {
        val c = NotificationChannel(ID, "Relay", NotificationManager.IMPORTANCE_LOW)
        ctx.getSystemService(NotificationManager::class.java).createNotificationChannel(c)
    }

    fun notification(ctx: Context): Notification =
        NotificationCompat.Builder(ctx, ID)
            .setContentTitle("Pocket Relay Running")
            .setContentText("Your phone is hosting a Nostr relay")
            .setSmallIcon(android.R.drawable.stat_notify_sync)
            .build()
}