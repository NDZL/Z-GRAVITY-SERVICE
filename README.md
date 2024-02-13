# Z-GRAVITY-SERVICE


## APP SETUP, LAUNCH, SCREENSHOTS
- Grant all permissions - either manually or by installing with Stagenow
- or install with
```adb install -g z-sensors-data-v1.32d.apk (adjust the apk name)```



- Run the app with the following adb command

```adb shell am start com.zebra.sensorsdata/com.zebra.sensorsdata.MainActivity```

- this in turn starts also the Gravity Service.

- To start just the GravistyService, run

```adb shell am start-foreground-service com.zebra.sensorsdata/com.zebra.sensorsdata.GravityService```

- Note that you need a proper Datawedge profile setup to get scan data when a different app is in the foreground.


### Permissions needed on A13 and A8

![image](https://github.com/NDZL/Z-GRAVITY-SERVICE/assets/11386676/482d0542-5a6c-4ab3-9fd7-d5c8a2d998eb) ![image](https://github.com/NDZL/Z-GRAVITY-SERVICE/assets/11386676/a3d0c2b5-1308-4c1f-b861-cb48066abe01)

![image](https://github.com/NDZL/Z-GRAVITY-SERVICE/assets/11386676/794588f1-9675-4fa7-8a3a-44f4db0731b8)


## APP INTERNALS
- Designed for A13, Supports Android 9+
- Successfully tested on PS20 A9 and A11; TC58 A11 and A13; TC21 A13

  ![image](https://github.com/NDZL/Z-GRAVITY-SERVICE/assets/11386676/b38b9333-c2f0-4443-883c-8fb2b80dee58)

* To retrieve the log file
  * ```adb pull /enterprise/usr/persist/z-sensors-data-log.csv```

* To view the log file
  * ```adb shell more /enterprise/usr/persist/z-sensors-data-log.csv```
 
* To remove an existing log (specifically if you migrated from a previous app version)
  1. restart the device 
  1. ```adb shell rm /enterprise/usr/persist/z-sensors-data-log.csv```
 
   

`
