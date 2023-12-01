package com.zebra.sensorsdata

import android.Manifest
import android.annotation.SuppressLint
import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat.getSystemService
import com.zebra.sensorsdata.GravityService.WIFENCE_STATUS_OFF
import com.zebra.sensorsdata.GravityService.WIFENCE_STATUS_ON
import com.zebra.sensorsdata.GravityService.nmanager

class NotificationBC: BroadcastReceiver() {
    @SuppressLint("MissingPermission")
    override fun onReceive(context: Context, intent: Intent?) {

        val state = intent!!.action
        if (state == GravityService.ACTION_TOGGLE_WIFENCE) {
            WiFence.isWiFenceON = !WiFence.isWiFenceON

            if(WiFence.isWiFenceON)
                GravityService.notification.actions[0].title = WIFENCE_STATUS_ON
            else
                GravityService.notification.actions[0].title = WIFENCE_STATUS_OFF

            with(NotificationManagerCompat.from(context)) {

                notify(3003, GravityService.notification)
            }
        }
    }
}