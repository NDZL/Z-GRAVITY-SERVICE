package com.ndzl.z_gravity_service;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.IBinder;

public class GravityService extends Service implements SensorEventListener {

    Intent i_startscan;
    Intent i_stopscan;
    private SensorManager mySensorManager;
    private Sensor myGravitySensor;
    float standardGravity;
    float thresholdGraqvity;
    boolean scan_enabled=true;

    public GravityService() {
    }


    @Override
    public IBinder onBind(Intent intent) {
        // TODO Auto-generated method stub
        return null;
    }
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {

        return START_STICKY;
    }
    @Override
    public void onCreate() {

        super.onCreate();

        i_startscan = new Intent();
        i_startscan.setAction("com.motorolasolutions.emdk.datawedge.api.ACTION_SOFTSCANTRIGGER");
        i_startscan.putExtra("com.motorolasolutions.emdk.datawedge.api.EXTRA_PARAMETER", "START_SCANNING");

        i_stopscan = new Intent();
        i_stopscan.setAction("com.motorolasolutions.emdk.datawedge.api.ACTION_SOFTSCANTRIGGER");
        i_stopscan.putExtra("com.motorolasolutions.emdk.datawedge.api.EXTRA_PARAMETER", "STOP_SCANNING");

        standardGravity = SensorManager.STANDARD_GRAVITY;
        thresholdGraqvity = standardGravity*90/100;
        mySensorManager = (SensorManager)getSystemService(Context.SENSOR_SERVICE);
        myGravitySensor = mySensorManager.getDefaultSensor(Sensor.TYPE_GRAVITY);
        mySensorManager.registerListener(this, myGravitySensor, SensorManager.SENSOR_DELAY_NORMAL);
    }



    @Override
    public void onDestroy() {

        super.onDestroy();
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        //SensorEventListener
        Sensor source = event.sensor;
        float z = event.values[2];
        if(source.getType() == Sensor.TYPE_GRAVITY){
            if (z >= thresholdGraqvity){
                if(scan_enabled) {
                    sendBroadcast(i_startscan);
                    scan_enabled=false;
                }
            }else if(z <= -thresholdGraqvity){
                sendBroadcast(i_stopscan);
                scan_enabled=true;
            }else{
                scan_enabled=true;
                sendBroadcast(i_stopscan);
            }
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int i) {

    }
}
