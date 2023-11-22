package com.zebra.sensorsdata

import android.content.Context
import android.media.AudioManager
import android.media.ToneGenerator
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.net.wifi.WifiInfo
import android.net.wifi.WifiManager
import android.util.Log
import com.zebra.sensorsdata.GravityService
import java.util.Timer
import kotlin.concurrent.timerTask



public class WiFence(context: Context) {

        val localContext = context
      //  val wifiManager = context.getSystemService(Context.WIFI_SERVICE) as WifiManager

        val request = NetworkRequest.Builder()
            .addTransportType(NetworkCapabilities.TRANSPORT_WIFI)
            .build()
        val connectivityManager = context.getSystemService(ConnectivityManager::class.java)

        var networkIsAvailable : Boolean = false
        val networkCallback = object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) {
                networkIsAvailable = true
            }

            override fun onLost(network: Network) {
                super.onLost(network)
                networkIsAvailable = false
            }

            override fun onUnavailable() {
                super.onUnavailable()
                networkIsAvailable = false
            }


            override fun onCapabilitiesChanged(network: Network, networkCapabilities: NetworkCapabilities) {
                val wifiInfo = networkCapabilities.transportInfo as WifiInfo
                GravityService.wifiEvents.add( wifiInfo )
            }
        }

        init {

            connectivityManager.requestNetwork(request, networkCallback)
            connectivityManager.registerNetworkCallback(request, networkCallback)

            val tg = ToneGenerator(AudioManager.STREAM_ALARM, 100)
            Timer().schedule(timerTask {
                // Log.i("WiFence","WIFI RSSI: ${wifiManager.connectionInfo.rssi}")


                if(GravityService.wifiEvents.size>0 && (GravityService.wifiEvents.peek().rssi<-80 || !networkIsAvailable) ) {
                    tg.startTone(ToneGenerator.TONE_CDMA_EMERGENCY_RINGBACK, 200);
                    Log.i("Geofence Alarm","DEVICE LEFT THE WIFI AREA")
                }

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