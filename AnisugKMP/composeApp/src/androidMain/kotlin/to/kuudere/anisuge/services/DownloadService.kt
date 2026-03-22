package to.kuudere.anisuge.services

import android.app.Service
import android.content.Intent
import android.os.IBinder
import to.kuudere.anisuge.platform.androidAppContext
import android.app.NotificationManager
import android.os.Build
import android.app.NotificationChannel
import androidx.core.app.NotificationCompat

class DownloadService : Service() {
    private companion object {
        const val NOTIFICATION_ID = 1001
        const val CHANNEL_ID = "anisurge_downloads"
    }

    private var wakeLock: android.os.PowerManager.WakeLock? = null

    override fun onCreate() {
        super.onCreate()
        val powerManager = getSystemService(android.content.Context.POWER_SERVICE) as android.os.PowerManager
        wakeLock = powerManager.newWakeLock(android.os.PowerManager.PARTIAL_WAKE_LOCK, "Anisurge::DownloadWakeLock").apply {
            acquire()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        wakeLock?.let {
            if (it.isHeld) it.release()
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val manager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        
        // Ensure channel exists
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            if (manager.getNotificationChannel(CHANNEL_ID) == null) {
                val channel = NotificationChannel(
                    CHANNEL_ID,
                    "Active Downloads",
                    NotificationManager.IMPORTANCE_DEFAULT
                ).apply {
                    description = "Shows progress of active anime downloads"
                    setSound(null, null)
                    enableVibration(false)
                    setShowBadge(true)
                }
                manager.createNotificationChannel(channel)
            }
        }

        // We use a persistent notification for the service.
        // Platform.kt will update this same notification ID regularly.
        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(to.kuudere.anisuge.R.mipmap.ic_launcher_foreground)
            .setContentTitle("Anisurge Downloads")
            .setContentText("Downloads are running in background...")
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .build()

        startForeground(NOTIFICATION_ID, notification)
        
        return START_NOT_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
