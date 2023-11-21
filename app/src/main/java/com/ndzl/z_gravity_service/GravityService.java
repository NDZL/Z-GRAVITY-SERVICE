package com.ndzl.z_gravity_service;

import static android.app.PendingIntent.FLAG_IMMUTABLE;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.util.Log;
import android.widget.Toast;

import androidx.core.app.NotificationCompat;

import java.util.Objects;
import java.util.Stack;

//ON NOV.28, 2019: "z-gravity-service_v1.2.apk" and shared to Stephan Jacobs

public class GravityService extends Service implements SensorEventListener {

    Intent i_startscan;
    Intent i_stopscan;
    private SensorManager mySensorManager;
    private Sensor myGravitySensor;
    float standardGravity;
    float thresholdGraqvity;
    float GRAVITY_PERCENT = .90f;
    boolean scan_enabled=true;

    //public static ConcurrentLinkedQueue<SensorEvent> gravityEventsQueue = new ConcurrentLinkedQueue<SensorEvent>();
    public static Stack<SensorEvent> gravityEvents = new Stack<SensorEvent>();
    public static Stack<WifiEvent> wifiEvents = new Stack<WifiEvent>();

    public GravityService() {
        //ShowToastInIntentService("service constructor");
        gravityEvents.clear();
        //wifiEventsQueue.clear();
    }


    @Override
    public IBinder onBind(Intent intent) {
        //ShowToastInIntentService("service onBind");
        return null;
    }

    @Override
    public void onTaskRemoved(Intent rootIntent) {

        super.onTaskRemoved(rootIntent);

        this.stopSelf();
    }

    public static final String CHANNEL_ID = "Foreground Service Channel";
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        //ShowToastInIntentService("service onStartCommand");

        if(intent != null) {
            GRAVITY_PERCENT = (float) Math.cos(Math.PI * 10.0d * intent.getIntExtra("GRAVITY_THRESHOLD", 3) / 180);
            thresholdGraqvity = standardGravity * GRAVITY_PERCENT;
        }


        String input = intent.getStringExtra("inputExtra");
        createNotificationChannel();
        Intent notificationIntent = new Intent(this, MainActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(this,0, notificationIntent, FLAG_IMMUTABLE);
        Notification notification = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle("ZGravity-FGS")
                .setContentText(input)
                .setSmallIcon(R.drawable.ic_notification)
                .setContentIntent(pendingIntent)
                .build();
        startForeground(3003, notification);

        return START_NOT_STICKY;
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel serviceChannel = new NotificationChannel(
                    CHANNEL_ID,
                    "Foreground Service Channel",
                    NotificationManager.IMPORTANCE_DEFAULT
            );
            NotificationManager manager = getSystemService(NotificationManager.class);
            manager.createNotificationChannel(serviceChannel);
        }
    }

    @Override
    public void onCreate() {

        super.onCreate();

        //ShowToastInIntentService("service onCreate");

        i_startscan = new Intent();
        i_startscan.setAction("com.motorolasolutions.emdk.datawedge.api.ACTION_SOFTSCANTRIGGER");
        i_startscan.putExtra("com.motorolasolutions.emdk.datawedge.api.EXTRA_PARAMETER", "START_SCANNING");

        i_stopscan = new Intent();
        i_stopscan.setAction("com.motorolasolutions.emdk.datawedge.api.ACTION_SOFTSCANTRIGGER");
        i_stopscan.putExtra("com.motorolasolutions.emdk.datawedge.api.EXTRA_PARAMETER", "STOP_SCANNING");

        BarcodeReceiverKt.createDataWedgeProfile(this, new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                //Log.i("GravityService/onReceive", ""+intent.getStringExtra("com.symbol.datawedge.data_string"));
                logScanAndSensorsData( intent ) ;

            }
        });

        standardGravity = SensorManager.STANDARD_GRAVITY;
        thresholdGraqvity = standardGravity * GRAVITY_PERCENT;
        mySensorManager = (SensorManager)getSystemService(Context.SENSOR_SERVICE);
        myGravitySensor = mySensorManager.getDefaultSensor(Sensor.TYPE_GRAVITY);
        mySensorManager.registerListener(this, myGravitySensor, SensorManager.SENSOR_DELAY_NORMAL);
        //SAMPLING RATE: The value must be one of SENSOR_DELAY_NORMAL, SENSOR_DELAY_UI, SENSOR_DELAY_GAME, or SENSOR_DELAY_FASTEST or, the desired delay between events in microseconds. Specifying the delay in microseconds only works from Android 2.3 (API level 9) onwards.
    }

    private void logScanAndSensorsData(Intent dwScanIntent) {
        //record format: SCAN_DATA, SCAN_SYMBOLOGY, ANGLE
        //https://developer.android.com/reference/android/hardware/SensorEvent#values
        Log.i("Sensor Data", "SCAN DATA: "+ dwScanIntent.getStringExtra("com.symbol.datawedge.data_string"));
        Log.i("Sensor Data", "SCAN TYPE: "+ dwScanIntent.getStringExtra("com.symbol.datawedge.label_type"));
        if(gravityEvents.size()>0){
            float[] gv = Objects.requireNonNull(gravityEvents.peek()).values;
            if(gv != null)
                Log.i("Sensor Data", "GRAVITY XYZ: "+ gv[0] + ", "+ gv[1] + ", "+ gv[2] ) ;
        }
        if(wifiEvents.size()>0) {
            WifiEvent we = Objects.requireNonNull(wifiEvents.peek());
            if(we != null)
                Log.i("Sensor Data", "WIFI RSSI,BSSID,SSID: " + we.getRssi() + ", " + we.getBssid() + ", " + we.getSsid() );
        }
        //gravityEventsQueue.clear();
        //DO NOT CLEAR//wifiEventsQueue.clear();
    }


    @Override
    public void onDestroy() {
        super.onDestroy();
    }

    boolean isSensorchanged_notified=false;
    @Override
    public void onSensorChanged(SensorEvent event) {

        if(event.sensor.getType() == Sensor.TYPE_GRAVITY)
            gravityEvents.add(event);
        //Log.i("onSensorChanged", new Date(System.currentTimeMillis() - SystemClock.elapsedRealtime() + event.timestamp / 1000000).toString());


        if(!isSensorchanged_notified) {
            isSensorchanged_notified = true;
            //ShowToastInIntentService("service onSensorChanged");
        }

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

    public void ShowToastInIntentService(final String sText) {
        final Context MyContext = this;

        new Handler(Looper.getMainLooper()).post(new Runnable() {
            @Override
            public void run() {
                Toast toast1 = Toast.makeText(MyContext, sText, Toast.LENGTH_SHORT);
                toast1.show();
            }
        });
    };
}
