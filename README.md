# Z-GRAVITY-SERVICE
LIKE Z-GRAVITY BUT WORKING THROUGH A BACKGROUND SERVICE. DOES NOT NEED DW CONFIG.

run the service just sending an Intent 
The adb syntax is as follows.

adb shell am startservice --ei GRAVITY_THRESHOLD 9 com.zebra.sensorsdata/com.zebra.sensorsdata.GravityService

where the number 9 is like setting the app sensitivity scroll bar rightmost.
Any numbers between 0 (zero) and 9 are allowed.

To pass no number just use
adb shell am startservice com.zebra.sensorsdata/com.zebra.sensorsdata.GravityService 
the sensitivity valur in that case is 3 by default

## APP SETUP AND SCREENSHOTS
- Grant all permissions - either manually or by installing with Stagenow

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
 
   

