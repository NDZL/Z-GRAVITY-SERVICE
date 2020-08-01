package com.ndzl.z_gravity_service;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
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
import android.os.PowerManager;
import android.widget.Toast;

import androidx.core.app.NotificationCompat;
import androidx.core.content.ContextCompat;

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

    public GravityService() {
        //ShowToastInIntentService("service constructor");
    }


    @Override
    public IBinder onBind(Intent intent) {
        //ShowToastInIntentService("service onBind");
        return null;
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
        PendingIntent pendingIntent = PendingIntent.getActivity(this,0, notificationIntent, 0);
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

        standardGravity = SensorManager.STANDARD_GRAVITY;
        thresholdGraqvity = standardGravity * GRAVITY_PERCENT;
        mySensorManager = (SensorManager)getSystemService(Context.SENSOR_SERVICE);
        myGravitySensor = mySensorManager.getDefaultSensor(Sensor.TYPE_GRAVITY);
        mySensorManager.registerListener(this, myGravitySensor, SensorManager.SENSOR_DELAY_NORMAL);
    }



    @Override
    public void onDestroy() {
        super.onDestroy();
    }

    boolean isSensorchanged_notified=false;
    @Override
    public void onSensorChanged(SensorEvent event) {

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
