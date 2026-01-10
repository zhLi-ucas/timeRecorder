package com.example.timemanager.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.media.RingtoneManager
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.core.app.NotificationCompat
import com.example.timemanager.ui.activities.AmbientDisplayActivity
import com.example.timemanager.ui.activities.MainActivity
import com.example.timemanager.ui.components.ReminderType

object NotificationService {
    private const val CHANNEL_ID = "timer_notification_channel"
    private const val CHANNEL_NAME = "定时器提醒"
    
    private const val REMINDER_CHANNEL_ID = "reminder_notification_channel"
    private const val REMINDER_CHANNEL_NAME = "喝水/久坐提醒"
    
    private const val NOTIFICATION_ID = 1
    private const val REMINDER_NOTIFICATION_ID = 2

    fun createNotificationChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationManager =
                context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

            // Timer Channel
            val timerChannel = NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "定时器结束提醒"
                enableVibration(true)
                enableLights(true)
            }
            notificationManager.createNotificationChannel(timerChannel)
            
            // Reminder Channel
            val reminderChannel = NotificationChannel(
                REMINDER_CHANNEL_ID,
                REMINDER_CHANNEL_NAME,
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "喝水和久坐起身提醒"
                enableVibration(true)
                enableLights(true)
            }
            notificationManager.createNotificationChannel(reminderChannel)
        }
    }

    fun showTimerCompletedNotification(context: Context, taskTag: String, taskDescription: String) {
        createNotificationChannel(context)

        val intent = Intent(context, AmbientDisplayActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }

        val pendingIntent = PendingIntent.getActivity(
            context,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle("时间到！")
            .setContentText("$taskTag: $taskDescription")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setDefaults(NotificationCompat.DEFAULT_ALL)
            .setSound(RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION))
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()

        val notificationManager =
            context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(NOTIFICATION_ID, notification)

        // 震动提醒
        vibrate(context)
    }
    
    fun showReminderNotification(context: Context, type: ReminderType) {
        createNotificationChannel(context)
        
        val title = when(type) {
            ReminderType.WATER -> "记得喝水！"
            ReminderType.STAND -> "起来活动一下！"
        }
        
        val text = when(type) {
            ReminderType.WATER -> "已经很久没喝水了，补充一下水分吧。"
            ReminderType.STAND -> "久坐伤身，站起来走走吧。"
        }

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }

        val pendingIntent = PendingIntent.getActivity(
            context,
            REMINDER_NOTIFICATION_ID, // Use different ID for pending intent request code
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, REMINDER_CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
            .setContentTitle(title)
            .setContentText(text)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setDefaults(NotificationCompat.DEFAULT_ALL)
            .setSound(RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION))
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()

        val notificationManager =
            context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        // Use unique ID based on type to allow stacking if needed, or same ID to overwrite
        val notifyId = if (type == ReminderType.WATER) REMINDER_NOTIFICATION_ID else REMINDER_NOTIFICATION_ID + 1
        notificationManager.notify(notifyId, notification)

        vibrate(context)
    }

    private fun vibrate(context: Context) {
        val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val vibratorManager =
                context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
            vibratorManager.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            vibrator.vibrate(
                VibrationEffect.createWaveform(
                    longArrayOf(0, 500, 200, 500, 200, 500),
                    -1
                )
            )
        } else {
            @Suppress("DEPRECATION")
            vibrator.vibrate(longArrayOf(0, 500, 200, 500, 200, 500), -1)
        }
    }
}
