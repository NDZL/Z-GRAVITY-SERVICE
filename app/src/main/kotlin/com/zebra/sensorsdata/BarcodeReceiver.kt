package com.zebra.sensorsdata

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Bundle
import com.zebra.sensorsdata.MainActivity

//constants
const val Activity_Intent_Filter = "com.zebra.sensors"
const val Intent_Key_Data = "com.symbol.datawedge.data_string"

const val NOTIFICATION_ACTION = "com.symbol.datawedge.api.NOTIFICATION_ACTION"
const val ACTION = "com.symbol.datawedge.api.ACTION"
const val PROFILE_NAME = "PROFILE_NAME"
const val PROFILE_STATUS = "PROFILE_ENABLED"
const val CONFIG_MODE = "CONFIG_MODE"
const val CONFIG_MODE_CREATE = "CREATE_IF_NOT_EXIST"
const val SET_CONFIG = "com.symbol.datawedge.api.SET_CONFIG"
val TAG: String = MainActivity::class.java.simpleName



fun createDataWedgeProfile(context: Context, barcodeReceiver: BroadcastReceiver) {
    val configBundle = Bundle()
    val bConfig = Bundle()
    val bParams = Bundle()
    val bundleApp1 = Bundle()
    val appName = context.packageName
    bParams.putString("scanner_selection", "auto")
    bParams.putString("intent_output_enabled", "true")
    bParams.putString("intent_action", Activity_Intent_Filter)
    bParams.putString("intent_category", "android.intent.category.DEFAULT")
    bParams.putString("intent_delivery", "2")
    bParams.putString("keystroke_output_enabled", "false")
    configBundle.putString(PROFILE_NAME, "ZebraSensors")
    configBundle.putString(PROFILE_STATUS, "true")
    configBundle.putString(CONFIG_MODE, CONFIG_MODE_CREATE)

    bundleApp1.putString("PACKAGE_NAME", appName)
    val activityName = arrayOf("*")
    bundleApp1.putStringArray("ACTIVITY_LIST", activityName)

    configBundle.putParcelableArray("APP_LIST", arrayOf(bundleApp1))

    bConfig.putString("PLUGIN_NAME", "INTENT")
    bConfig.putString("RESET_CONFIG", "false")

    bConfig.putBundle("PARAM_LIST", bParams)
    configBundle.putBundle("PLUGIN_CONFIG", bConfig)

    val i = Intent()
    i.action = ACTION
    i.putExtra(SET_CONFIG, configBundle)
    val bNotification = Bundle()
    bNotification.putString("com.symbol.datawedge.api.APPLICATION_NAME", appName)
    bNotification.putString("com.symbol.datawedge.api.NOTIFICATION_TYPE", "SCANNER_STATUS")
    i.action = "com.symbol.datawedge.api.ACTION"
    i.putExtra("com.symbol.datawedge.api.REGISTER_FOR_NOTIFICATION", bNotification)
    context.sendBroadcast(i)
    val filter = IntentFilter()
    filter.addCategory(Intent.CATEGORY_DEFAULT)
    filter.addAction(Activity_Intent_Filter)
    filter.addAction(NOTIFICATION_ACTION)
    context.registerReceiver(barcodeReceiver, filter)//register broadcast receiver
}

fun datawedgeRegisterForNotifications(context: Context) {
    val bNotification = Bundle()
    bNotification.putString("com.symbol.datawedge.api.APPLICATION_NAME", "com.zebra.sensorsdata")
    bNotification.putString("com.symbol.datawedge.api.NOTIFICATION_TYPE", "SCANNER_STATUS")
    val i = Intent()
    i.action = "com.symbol.datawedge.api.ACTION"
    i.putExtra("com.symbol.datawedge.api.REGISTER_FOR_NOTIFICATION", bNotification)
    context.sendBroadcast(i)
}

