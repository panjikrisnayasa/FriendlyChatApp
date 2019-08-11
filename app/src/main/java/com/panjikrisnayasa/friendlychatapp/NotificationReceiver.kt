package com.panjikrisnayasa.friendlychatapp

import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat

class NotificationReceiver : BroadcastReceiver() {

    private lateinit var mNotificationManager: NotificationManager

    override fun onReceive(context: Context, intent: Intent) {
        mNotificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        deliverNotification(context)
    }

    private fun deliverNotification(context: Context) {
        val notificationIntent = Intent(context, MainActivity::class.java)
        val notificationPendingIntent =
            PendingIntent.getActivity(
                context,
                MainActivity.NOTIFICATION_ID,
                notificationIntent,
                PendingIntent.FLAG_UPDATE_CURRENT
            )

        val notificationBuilder = NotificationCompat.Builder(context, MainActivity.PRIMARY_CHANNEL_ID)
            .setContentTitle("Talkie")
            .setContentText("There is new message for you")
            .setSmallIcon(R.drawable.ic_mood_24dp)
            .setContentIntent(notificationPendingIntent)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setDefaults(NotificationCompat.DEFAULT_ALL)
        mNotificationManager.notify(MainActivity.NOTIFICATION_ID, notificationBuilder.build())
    }
}
