package com.omprakash.swingtracker.notification

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat

object NotificationHelper {

    private const val CHANNEL_ID = "swing_tracker_alerts"
    private const val CHANNEL_NAME = "Stock Alerts"

    fun ensureChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val manager = context.getSystemService(NotificationManager::class.java)
            val channel = NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Alerts when a watchlist stock matches the swing-trade screener"
            }
            manager.createNotificationChannel(channel)
        }
    }

    /** Shows one notification for a stock that just qualified. Uses the symbol's hash so each stock gets its own notification slot (new ones don't overwrite old ones). */
    fun notifyMatch(context: Context, symbol: String, price: Double) {
        ensureChannel(context)

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle("$symbol matches your screener")
            .setContentText("Price ₹%.2f - uptrend, outperforming Nifty, volume confirmed".format(price))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .build()

        NotificationManagerCompat.from(context).apply {
            // Permission is requested in MainActivity; if somehow not granted, skip silently.
            try {
                notify(symbol.hashCode(), notification)
            } catch (_: SecurityException) {
                // POST_NOTIFICATIONS not granted - nothing we can do here.
            }
        }
    }
}
