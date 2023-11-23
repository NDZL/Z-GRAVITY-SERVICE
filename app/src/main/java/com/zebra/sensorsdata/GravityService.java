package com.zebra.sensorsdata;

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
import android.net.wifi.WifiInfo;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.SystemClock;
import android.util.Log;
import android.widget.Toast;

import androidx.core.app.NotificationCompat;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.time.Clock;
import java.time.LocalDateTime;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import java.util.Objects;
import java.util.Stack;
import java.util.TimeZone;

//ON NOV.28, 2019: "z-gravity-service_v1.2.apk" and shared to Stephan Jacobs

public class GravityService extends Service implements SensorEventListener {

    Intent i_startscan;
    Intent i_stopscan;
    private SensorManager mySensorManager;
    private Sensor myGravitySensor;
    private Sensor myStepDetectorSensor;
    float standardGravity;
    float thresholdGravity;
    float GRAVITY_PERCENT = .90f;
    boolean scan_enabled=true;

    //public static ConcurrentLinkedQueue<SensorEvent> gravityEventsQueue = new ConcurrentLinkedQueue<SensorEvent>();
    public static Stack<SensorEvent> gravityEvents = new Stack<SensorEvent>();
    public static Stack<WifiEvent> wifiEvents = new Stack<WifiEvent>();

    public static int stepsPreviouslyDetected = 0;
    public static int stepsCurrentlyDetected = 0;

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
            thresholdGravity = standardGravity * GRAVITY_PERCENT;
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

        initCSVLogFile();

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
        BarcodeReceiverKt.datawedgeRegisterForNotifications(this);

        standardGravity = SensorManager.STANDARD_GRAVITY;
        thresholdGravity = standardGravity * GRAVITY_PERCENT;
        mySensorManager = (SensorManager)getSystemService(Context.SENSOR_SERVICE);

        myGravitySensor = mySensorManager.getDefaultSensor(Sensor.TYPE_GRAVITY);
        mySensorManager.registerListener(this, myGravitySensor, SensorManager.SENSOR_DELAY_NORMAL);
        //SAMPLING RATE: The value must be one of SENSOR_DELAY_NORMAL, SENSOR_DELAY_UI, SENSOR_DELAY_GAME, or SENSOR_DELAY_FASTEST or, the desired delay between events in microseconds. Specifying the delay in microseconds only works from Android 2.3 (API level 9) onwards.

        myStepDetectorSensor = mySensorManager.getDefaultSensor(Sensor.TYPE_STEP_DETECTOR);
        mySensorManager.registerListener(this, myStepDetectorSensor, SensorManager.SENSOR_DELAY_NORMAL);
    }

    private void logScanAndSensorsData(Intent dwScanIntent) {

        //https://developer.android.com/reference/android/hardware/SensorEvent#values
        StringBuilder sbSCAN_DATA = new StringBuilder();
        StringBuilder sbNOTIFICATIONS = new StringBuilder();

        if(dwScanIntent.getBundleExtra("com.symbol.datawedge.api.NOTIFICATION") != null){
            String _status = dwScanIntent.getBundleExtra("com.symbol.datawedge.api.NOTIFICATION").getString("STATUS");
            Log.i("Sensor Data", "SCAN NOTIFICATION: " + _status);
            logToScreen("SCAN NOTIFICATION: "+_status);
            sbNOTIFICATIONS.append(_status).append(",");

            logToFile(sbNOTIFICATIONS.toString());

        }
        if(dwScanIntent.getStringExtra("com.symbol.datawedge.data_string") == null ) return; //not to log to many times the same data

        if(dwScanIntent.getStringExtra("com.symbol.datawedge.data_string") != null){
            Log.i("Sensor Data", "SCAN DATA: " + dwScanIntent.getStringExtra("com.symbol.datawedge.data_string"));
            Log.i("Sensor Data", "SCAN TYPE: " + dwScanIntent.getStringExtra("com.symbol.datawedge.label_type"));
            logToScreen("DATA: "+dwScanIntent.getStringExtra("com.symbol.datawedge.data_string"));
            logToScreen("TYPE: "+dwScanIntent.getStringExtra("com.symbol.datawedge.label_type"));
            sbSCAN_DATA.append(",").append(dwScanIntent.getStringExtra("com.symbol.datawedge.data_string")).append(",").append( dwScanIntent.getStringExtra("com.symbol.datawedge.label_type") ).append(",");
        }

        if(wifiEvents.size()>0) {
            WifiEvent we = Objects.requireNonNull(wifiEvents.peek());
            if(we != null) {
                Log.i("Sensor Data", "WIFI RSSI,BSSID,SSID: " + we.getRssi()+ ", " + we.getBssid() + ", " + we.getSsid());
                logToScreen("WIFI RSSI,BSSID,SSID: " + we.getRssi()+ ", " + we.getBssid() + ", " + we.getSsid() );
                sbSCAN_DATA.append(we.getRssi()).append(",").append(we.getBssid()).append(",").append(we.getSsid()).append(",");
            }
        }

        if(gravityEvents.size()>0){
            SensorEvent targetEvent = gravityEvents.peek();
            float[] gv = Objects.requireNonNull(targetEvent).values;
            if(gv != null) {
                String devicePose = computeDevicePose(targetEvent);
                Log.i("Sensor Data", "GRAVITY XYZ: " + gv[0] + ", " + gv[1] + ", " + gv[2] + ", " + devicePose + ",");
                logToScreen("GRAVITY XYZ: " + gv[0] + ", " + gv[1] + ", " + gv[2] + ", " + devicePose + ",");
                sbSCAN_DATA.append(gv[0]).append(",").append(gv[1]).append(",").append(gv[2]).append(",").append(devicePose).append(",");
            }
        }

        Log.i("Sensor Data", "STEPS SINCE LAST EVENT: "+ (stepsCurrentlyDetected) );
        logToScreen("STEPS SINCE LAST EVENT: "+(stepsCurrentlyDetected));
        sbSCAN_DATA.append(stepsCurrentlyDetected).append(",");
        stepsPreviouslyDetected = stepsCurrentlyDetected;
        stepsCurrentlyDetected= 0;

        Log.i("Sensor Data", "-----------------------------------------------------------------------------------");
        logToScreen("----------------");

        logToFile(sbSCAN_DATA.toString());
    }

    private String computeDevicePose(SensorEvent targetEvent) {
        float[] gv = Objects.requireNonNull(targetEvent).values;
        float angleToHorizPlane = (float)(Math.atan(  gv[1] /  Math.sqrt( gv[2]*gv[2]+ gv[0]*gv[0] ) ) *180/Math.PI  );
        String pose ="";
        if(angleToHorizPlane > 15 && angleToHorizPlane<75)   pose = "UP";
        else if(angleToHorizPlane>75) pose = "SKY";
        else if(angleToHorizPlane < -15 && angleToHorizPlane>-75) pose = "DW";
        else if(angleToHorizPlane<-75) pose = "GROUND";
        else pose = "FLAT";
        return pose+"("+Math.abs(  Math.round(angleToHorizPlane) )+"DEG)";
    }

    private void logToFile(String dataToBeLogged) {
        Clock clockUTC = Clock.systemUTC();
        long epochUTC = clockUTC.millis();
        LocalDateTime zdt = LocalDateTime.now(clockUTC);
        zdt.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);
        String datetimeNow = zdt.toString();

        //String header = "EPOCH,TIMESTAMP,SCAN DATA,SCAN TYPE,WIFI RSSI,BSSID,SSID,GRAVITY X,GRAVITY Y,GRAVITY Z,STEPS SINCE LAST EVENT";
        String fullstring = ""+epochUTC+","+ datetimeNow +","+ dataToBeLogged;
        File file = new File("/enterprise/usr/persist/z-sensors-data-log.csv");
        FileWriter fr = null;
        try {
            fr = new FileWriter(file, true);
            fr.write(fullstring+"\n");
            fr.close();
        } catch (IOException e) {}



    }

    private void chmodFile(String sourcePath){
        try {
            Process _p = Runtime.getRuntime().exec("chmod 666 " + sourcePath); //chmod needed for /enterprise
            _p.waitFor();
        } catch (IOException e) {}
        catch (InterruptedException e) {}

    }

    private void initCSVLogFile() {
        File file = new File("/enterprise/usr/persist/z-sensors-data-log.csv");
        FileWriter fr = null;
        try {
            fr = new FileWriter(file, false);
            fr.write("EPOCH UTC,TIMESTAMP UTC,NOTIFICATION,SCAN DATA,SCAN TYPE,WIFI RSSI,BSSID,SSID,GRAVITY X,GRAVITY Y,GRAVITY Z,POSE,STEPS SINCE LAST EVENT\n");
            fr.close();
        } catch (IOException e) {}

        chmodFile("/enterprise/usr/persist/z-sensors-data-log.csv");
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
    }

    boolean isSensorchanged_notified=false;
    @Override
    public void onSensorChanged(SensorEvent event) {
        if (!isSensorchanged_notified) {
            isSensorchanged_notified = true;
            //ShowToastInIntentService("service onSensorChanged");
        }

        if(event.sensor.getType() == Sensor.TYPE_GRAVITY) {
            gravityEvents.add(event);
            //Log.i("onSensorChanged", new Date(System.currentTimeMillis() - SystemClock.elapsedRealtime() + event.timestamp / 1000000).toString());
        }
        else if(event.sensor.getType() == Sensor.TYPE_STEP_DETECTOR && event.values[0] == 1.0f){
            //Log.i("onSensorChanged", "TYPE_STEP_DETECTOR "+event.values[0]);
            stepsCurrentlyDetected++;
            //logToScreen("TYPE_STEP_DETECTOR "+stepsDetected);
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

    public void logToScreen(final String sText) {
        MainActivity.tvOut.setText(sText+"\n"+MainActivity.tvOut.getText());
    };

}
