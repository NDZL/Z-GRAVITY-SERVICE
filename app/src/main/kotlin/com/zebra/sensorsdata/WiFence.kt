package com.zebra.sensorsdata

import android.content.Context
import android.media.AudioManager
import android.media.ToneGenerator
import android.net.wifi.WifiManager
import android.util.Log
import java.util.Timer
import kotlin.concurrent.timerTask

class WifiEvent {
    var rssi: Int = 0
    var bssid: String = ""
    var ssid: String = ""
}
public class WiFence(context: Context) {

        val localContext = context
        val wifiManager = context.getSystemService(Context.WIFI_SERVICE) as WifiManager

        init {

            val tg = ToneGenerator(AudioManager.STREAM_ALARM, 100)
            Timer().schedule(timerTask {
                // Log.i("WiFence","WIFI RSSI: ${wifiManager.connectionInfo.rssi}")

                val wfinfo = WifiEvent()
                wfinfo.rssi = wifiManager.connectionInfo.rssi
                wfinfo.bssid = wifiManager.connectionInfo.bssid?.toString().orEmpty()
                wfinfo.ssid = wifiManager.connectionInfo.ssid?.toString().orEmpty()

                GravityService.wifiEvents.add( wfinfo )
                if(isWiFenceON && wifiManager.connectionInfo.rssi<-80) {
                    tg.startTone(ToneGenerator.TONE_CDMA_EMERGENCY_RINGBACK, 200);
                    Log.i("Geofence Alarm","DEVICE LEFT THE WIFI AREA")
                }

            }, 2000, 3000)
        }
    companion object{
        var isWiFenceON = true
    }

}