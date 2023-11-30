# Z-GRAVITY-SERVICE
LIKE Z-GRAVITY BUT WORKING THROUGH A BACKGROUND SERVICE. DOES NOT NEED DW CONFIG.


## APP SETUP, LAUNCH, SCREENSHOTS
- Grant all permissions - either manually or by installing with Stagenow
- or install with adb install -g z-sensors-data-v1.32d.apk (adjust the apk name)



- Run the app with the following adb command

adb shell am start com.zebra.sensorsdata/com.zebra.sensorsdata.MainActivity

- this in turn starts also the Gravity Service.

- To start just the GravistyService, run

adb shell am start-foreground-service com.zebra.sensorsdata/com.zebra.sensorsdata.GravityService

- Note that you need a proper Datawedge profile setup to get scan data when a different app is in the foreground.



  ![image](https://github.com/NDZL/Z-GRAVITY-SERVICE/assets/11386676/4c602ed0-a393-4711-bb08-fa46f2fe7941)
![image](https://github.com/NDZL/Z-GRAVITY-SERVICE/assets/11386676/794588f1-9675-4fa7-8a3a-44f4db0731b8)


## APP INTERNALS
- Designed for A13, Supports Android 9+

  ![image](https://github.com/NDZL/Z-GRAVITY-SERVICE/assets/11386676/b38b9333-c2f0-4443-883c-8fb2b80dee58)

* To retrieve the log file
  * adb pull /enterprise/usr/persist/z-sensors-data-log.csv

* To view the log file
  * adb shell more /enterprise/usr/persist/z-sensors-data-log.csv
 
* To remove an existing log (specifically if you migrated from a previous app version)
  1. restart the device 
  1. adb shell rm /enterprise/usr/persist/z-sensors-data-log.csv
 
   

