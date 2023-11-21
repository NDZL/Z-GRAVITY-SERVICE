package com.ndzl.z_gravity_service

import android.Manifest
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.net.wifi.WifiManager
import androidx.core.app.ActivityCompat


public class WiFence(context: Context) {

        val localContext = context
        val wifiManager = context.getSystemService(Context.WIFI_SERVICE) as WifiManager

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

    init{intentFilter.addAction(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION)} //SCAN==LIST ALL AVAILABLE WIFI IN THE SURROUNDINGS
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

    }

}