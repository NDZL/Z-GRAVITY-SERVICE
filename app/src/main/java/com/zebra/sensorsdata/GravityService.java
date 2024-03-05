package com.zebra.sensorsdata;

import static android.app.PendingIntent.FLAG_IMMUTABLE;

import android.app.Application;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.provider.Settings;
import android.util.Log;
import android.widget.TextView;
import android.widget.Toast;

import androidx.core.app.NotificationCompat;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.time.Clock;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Objects;
import java.util.Stack;
import java.util.UUID;

//ON NOV.28, 2019: "z-gravity-service_v1.2.apk" and shared to Stephan Jacobs

public class GravityService extends Service implements SensorEventListener {

    Intent i_startscan;
    Intent i_stopscan;
    private SensorManager mySensorManager;
    private Sensor myGravitySensor;
    private Sensor myStepDetectorSensor;
    private Sensor myProximitySensor;
    float standardGravity;
    float thresholdGravity;
    float GRAVITY_PERCENT = .90f;
    boolean scan_enabled=true;

    //public static ConcurrentLinkedQueue<SensorEvent> gravityEventsQueue = new ConcurrentLinkedQueue<SensorEvent>();
    public static Stack<SensorEvent> gravityEvents = new Stack<SensorEvent>();
    public static Stack<WifiEvent> wifiEvents = new Stack<WifiEvent>();

    public static int stepsPreviouslyDetected = 0;
    public static int stepsCurrentlyDetected = 0;

    final public static String ACTION_TOGGLE_WIFENCE = "TOGGLE_WIFENCE_STATE";
    final public static String WIFENCE_STATUS_ON = "WIFENCE is ON - Tap to toggle";
    final public static String WIFENCE_STATUS_OFF = "WIFENCE is OFF - Tap to toggle";
    public static Notification notification;

    public static NotificationManager nmanager;

    String shopID ="N/A";

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

        //this.stopSelf();
    }


    String getDeviceSerialNumber(){
        return  (new OEMInfoManager(this)).OEMINFO_DEVICE_SERIAL;
    }
    public static final String CHANNEL_ID = "Foreground Service Channel";

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        //ShowToastInIntentService("service onStartCommand");

        if(intent != null) {
            GRAVITY_PERCENT = (float) Math.cos(Math.PI * 10.0d * intent.getIntExtra("GRAVITY_THRESHOLD", 3) / 180);
            thresholdGravity = standardGravity * GRAVITY_PERCENT;
        }



        if(Build.VERSION.SDK_INT<29){
            OEMInfoManager.OEMINFO_DEVICE_SERIAL = Build.getSerial() ;  //REMINDER: MANUALLY GRANT THE PHONE STATE PERMISSION!
        }
        else {
            new OEMInfoManager(this.getApplicationContext());
        }

        shopID = getShopID();
        initCSVLogFile();
        Log.i("DeviceID", getDeviceSerialNumber());
        logToScreen("DeviceID: " + getDeviceSerialNumber());
        Log.i("shopID", shopID);
        logToScreen("ShopID: " + shopID);
        logToScreen("----------");


        String input = intent.getStringExtra("inputExtra");
        createNotificationChannel();
        Intent notificationIntent = new Intent(this, MainActivity.class);//opens MainActivity when the notification is tapped on
        PendingIntent notifPendingIntent = PendingIntent.getActivity(this,0, notificationIntent, FLAG_IMMUTABLE);

        Intent toggleWifenceIntent = new Intent();
        toggleWifenceIntent.setAction( ACTION_TOGGLE_WIFENCE );
        PendingIntent toggleWifencePendingIntent = PendingIntent.getBroadcast(getApplicationContext(), 4004, toggleWifenceIntent, PendingIntent.FLAG_UPDATE_CURRENT|FLAG_IMMUTABLE);

        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addCategory(Intent.CATEGORY_DEFAULT);
        intentFilter.addAction(ACTION_TOGGLE_WIFENCE);
        registerReceiver(new NotificationBR(), intentFilter); //RECEIVER TESTED WITH adb shell am broadcast -a TOGGLE_WIFENCE_STATE

        notification = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle("ZGravity-FGS")
                .setContentText(input)
                .setSmallIcon(R.drawable.ic_notification)
                .setContentIntent(notifPendingIntent)
                .addAction(R.drawable.ic_launcher_background, WIFENCE_STATUS_ON, toggleWifencePendingIntent)
                .build();

        startForeground(3003, notification);

        return START_NOT_STICKY;
    }

    private void createNotificationChannel() {
        NotificationChannel serviceChannel = new NotificationChannel(
                CHANNEL_ID,
                "Foreground Service Channel",
                NotificationManager.IMPORTANCE_DEFAULT
        );
        nmanager = getSystemService(NotificationManager.class);
        nmanager.createNotificationChannel(serviceChannel);
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

        //myProximitySensor = mySensorManager.getDefaultSensor(Sensor.TYPE_PROXIMITY);  //standard proximity sensor
        myProximitySensor = mySensorManager.getDefaultSensor(65537);  //from PS20 com.symbol.autotrigger
        mySensorManager.registerListener(this, myProximitySensor, SensorManager.SENSOR_DELAY_NORMAL);

        //TRYING TO LOG THE DEVICE SENSORS LIST
        StringBuilder _sbsesns=new StringBuilder();
        List<Sensor> sensorsList = mySensorManager.getSensorList(Sensor.TYPE_ALL);
        for (Sensor s : sensorsList) {
            Log.i("Sensors list", ""+s.getType()+"-"+s.getName()+"-"+s.getVendor());
        }

        new WiFence(this);

       // String serial = Build.getSerial();
    }

    private void logScanAndSensorsData(Intent dwScanIntent) {

        //https://developer.android.com/reference/android/hardware/SensorEvent#values

        StringBuilder sbAcquiredData = new StringBuilder();

        String scanner_Status = "N/A";
        if(dwScanIntent.getBundleExtra("com.symbol.datawedge.api.NOTIFICATION") != null) {
            scanner_Status= dwScanIntent.getBundleExtra("com.symbol.datawedge.api.NOTIFICATION").getString("STATUS");
            sbAcquiredData.append(scanner_Status).append(",");
        }
            Log.i("Sensor Data", "SCAN NOTIFICATION: " + scanner_Status);
            logToScreen("SCAN NOTIFICATION: "+scanner_Status);

        String scan_data = "N/A";
        String scan_type = "N/A";
        if (dwScanIntent.getStringExtra("com.symbol.datawedge.data_string") != null) {
            scan_data = cleanNonPrintableChars( dwScanIntent.getStringExtra("com.symbol.datawedge.data_string") );
            scan_type = dwScanIntent.getStringExtra("com.symbol.datawedge.label_type");
            sbAcquiredData.append("READ,");
        }
        Log.i("Sensor Data", "SCAN DATA: " + scan_data);
        Log.i("Sensor Data", "SCAN TYPE: " + scan_type);
        logToScreen("DATA: " + scan_data);
        logToScreen("TYPE: " + scan_type);
        sbAcquiredData.append(scan_data).append(",").append(scan_type).append(",");


        String wifi_rssi = "N/A";
        String wifi_bssid = "N/A";
        String wifi_ssid = "N/A";
        if (wifiEvents.size() > 0) {
            WifiEvent we = Objects.requireNonNull(wifiEvents.peek());
            if (we != null) {
                wifi_rssi = String.valueOf(we.getRssi());
                wifi_bssid = we.getBssid();
                wifi_ssid = we.getSsid();
            }
        }
        Log.i("Sensor Data", "WIFI RSSI,BSSID,SSID: " + wifi_rssi + ", " + wifi_bssid + ", " + wifi_ssid);
        logToScreen("WIFI RSSI,BSSID,SSID: " + wifi_rssi + ", " + wifi_bssid + ", " + wifi_ssid);
        sbAcquiredData.append(wifi_rssi).append(",").append(wifi_bssid).append(",").append(wifi_ssid).append(",");


        String devicePose="N/A";
        String gx="N/A";
        String gy="N/A";
        String gz="N/A";
        if (gravityEvents.size() > 0) {
            SensorEvent targetEvent = gravityEvents.peek();
            float[] gv = Objects.requireNonNull(targetEvent).values;

            if (gv != null) {
                devicePose = computeDevicePose(targetEvent);
                gx= String.valueOf(gv[0]);
                gy= String.valueOf(gv[1]);
                gz= String.valueOf(gv[2]);
            }
        }
        Log.i("Sensor Data", "GRAVITY XYZ: " + gx + ", " + gy+ ", " + gz + ", " + devicePose + ",");
        logToScreen("GRAVITY XYZ: " + gx + ", " + gy+ ", " + gz + ", " + devicePose + ",");
        sbAcquiredData.append(gx).append(",").append(gy).append(",").append(gz).append(",").append(devicePose).append(",");

        Log.i("Sensor Data", "STEPS SINCE LAST EVENT: " + (stepsCurrentlyDetected));
        logToScreen("STEPS SINCE LAST EVENT: " + (stepsCurrentlyDetected));
        sbAcquiredData.append(stepsCurrentlyDetected).append(",");
        stepsPreviouslyDetected = stepsCurrentlyDetected;
        stepsCurrentlyDetected = 0;

        Log.i("Sensor Data", "-----------------------------------------------------------------------------------");
        logToScreen("----------------");

        logToFile(sbAcquiredData.toString());
    }

    private String cleanNonPrintableChars(String stringExtra) {
        //replacing the comma since it's the field separator and all non printable chars
        return stringExtra.replaceAll(",","_").replaceAll("[^\\x20-\\x7e]", "_");
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

        String fullstring = getDeviceSerialNumber()+","+shopID+","+epochUTC+","+ datetimeNow +","+ dataToBeLogged;
        File file = new File("/enterprise/usr/persist/z-sensors-data-log.csv");
        FileWriter fr = null;
        try {
            fr = new FileWriter(file, true);
            fr.write(fullstring+"\n");
            fr.close();
        } catch (IOException e) {}
    }

    private String getShopID() {
        return UUID.randomUUID().toString(); //sample shopID to be re-defined for production use
    }

    private String getDeviceID() {
        //easily replaceable with another unique ID
        return Settings.Secure.getString(getApplicationContext().getContentResolver(), Settings.Secure.ANDROID_ID);
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
        if(!file.exists()) {
            FileWriter fr = null;
            try {
                fr = new FileWriter(file, false);
                fr.write("DEVICE ID,SHOP ID,EPOCH UTC,TIMESTAMP UTC,NOTIFICATION,SCAN DATA,SCAN TYPE,WIFI RSSI,BSSID,SSID,GRAVITY X,GRAVITY Y,GRAVITY Z,POSE,STEPS SINCE LAST EVENT\n");
                fr.close();
            } catch (IOException e) {
            }
        }
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
        else if(event.sensor.getType() == 65537)
                if(event.values.length>0)
                    Log.i("onSensorChanged", "TYPE_PROXIMITY "+event.values[0]);  //TC21 OK - NO EVENTS RAISED IN PS20
                else
                    Log.i("onSensorChanged", "TYPE_PROXIMITY -EMPTY EVENT");

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
        if(MainActivity.tvOut != null) //when this service is starting directly with an Intent, MainActivity is not defined
            MainActivity.tvOut.setText(sText+"\n"+MainActivity.tvOut.getText());
    };

}
