package com.ndzl.z_gravity_service

import android.Manifest
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.media.AudioManager
import android.media.ToneGenerator
import android.net.wifi.WifiManager
import android.util.Log
import androidx.core.app.ActivityCompat
import java.util.Timer
import kotlin.concurrent.timerTask


public class WiFence(context: Context) {

        val localContext = context
        val wifiManager = context.getSystemService(Context.WIFI_SERVICE) as WifiManager
        init {

            val tg = ToneGenerator(AudioManager.STREAM_ALARM, 100)
            Timer().schedule(timerTask {
                Log.i("WiFence","WIFI RSSI: ${wifiManager.connectionInfo.rssi}")
                if(wifiManager.connectionInfo.rssi<-80)
                    tg.startTone(ToneGenerator.TONE_CDMA_EMERGENCY_RINGBACK,200);

            }, 2000, 3000)
        }

/*      //CODE FOR WIFI SCAN - NOT NEEDED NOW
        //REQUIRES THE FOLLOWING PERMISSIONS
    <uses-permission android:name="android.permission.CHANGE_WIFI_STATE" />
    <uses-permission android:name="android.permission.ACCESS_COARSE_LOCATION" />
    <uses-permission android:name="android.permission.ACCESS_FINE_LOCATION" />
    <uses-permission android:name="android.permission.ACCESS_WIFI_STATE" />

        val wifiScanReceiver = object : BroadcastReceiver() {

            override fun onReceive(context: Context, intent: Intent) {
                val success = intent.getBooleanExtra(WifiManager.EXTRA_RESULTS_UPDATED, false)
                if (success) {
                    scanSuccess()
                } else {
                    scanFailure()
                }
            }
        }

        private val intentFilter = IntentFilter()

        //init{intentFilter.addAction(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION)} //SCAN==LIST ALL AVAILABLE WIFI IN THE SURROUNDINGS
        init{intentFilter.addAction(WifiManager.EXTRA_NEW_RSSI)}
        init{ context.registerReceiver(wifiScanReceiver, intentFilter) }


        val success = wifiManager.startScan()

        private fun scanSuccess() {
            val results = if (ActivityCompat.checkSelfPermission(localContext, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED
            ) {
                return
            }
            else
                wifiManager.scanResults

        }

        private fun scanFailure() {
            // handle failure: new scan did NOT succeed
            // consider using old scan results: these are the OLD results!
            val results = wifiManager.scanResults

        }*/

}