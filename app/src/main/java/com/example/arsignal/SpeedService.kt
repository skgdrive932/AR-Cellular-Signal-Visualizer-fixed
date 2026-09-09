package com.example.arsignal

import android.app.Service
import android.content.Intent
import android.os.IBinder
import com.example.arsignal.MainActivity

class SpeedService : Service() {

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        // Service initialization logic
        return START_STICKY
    }

    override fun onDestroy() {
        super.onDestroy()
    }
}
