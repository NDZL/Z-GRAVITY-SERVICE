package com.zebra.sensorsdata

import android.annotation.SuppressLint
import android.app.ActivityManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Context.ACTIVITY_SERVICE
import android.content.Intent
import android.util.Log
import android.widget.Toast
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.core.content.ContextCompat.getSystemService
import com.zebra.sensorsdata.GravityService.WIFENCE_STATUS_OFF
import com.zebra.sensorsdata.GravityService.WIFENCE_STATUS_ON

class NotificationBR: BroadcastReceiver() {
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
        else if (intent != null && intent.action == "android.intent.action.LOCKED_BOOT_COMPLETED") {
                if (!isMyServiceRunning(context, GravityService::class.java)) {
                    Log.i("GNotificationBR","startService GravityService after LOCKED_BOOT_COMPLETED")
                    context.startForegroundService(Intent(context, GravityService::class.java))
                }
            }
        else if (intent != null && intent.action == "android.intent.action.USER_UNLOCKED") {
            if (!isMyServiceRunning(context, GravityService::class.java)) {
                Log.i("GNotificationBR","startService GravityService after USER_UNLOCKED")
                context.startForegroundService(Intent(context, GravityService::class.java))
            }
        }
        else if (intent != null && intent.action == "android.intent.action.BOOT_COMPLETED") {
            if (!isMyServiceRunning(context, GravityService::class.java)) {
                Log.i("GNotificationBR","startService GravityService after BOOT_COMPLETED")
                context.startForegroundService(Intent(context, GravityService::class.java))
            }
        }

    }

    private fun isMyServiceRunning(ctx: Context, serviceClass: Class<*>): Boolean {
        val manager =  getSystemService(ctx, ActivityManager::class.java)
        if (manager != null) {
            for (service in manager.getRunningServices(Int.MAX_VALUE)) {
                if (serviceClass.name == service.service.className) {
                    return true
                }
            }
        }
        return false
    }
}