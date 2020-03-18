# Z-GRAVITY-SERVICE
LIKE Z-GRAVITY BUT WORKING THROUGH A BACKGROUND SERVICE. DOES NOT NEED DW CONFIG.

run the service just sending an Intent 
The adb syntax is as follows.

adb shell am startservice --ei GRAVITY_THRESHOLD 9 com.ndzl.z_gravity_service/com.ndzl.z_gravity_service.GravityService

where the number 9 is like setting the app sensitivity scroll bar rightmost.
Any numbers between 0 (zero) and 9 are allowed.

To pass no number just use
adb shell am startservice com.ndzl.z_gravity_service/com.ndzl.z_gravity_service.GravityService 
the sensitivity valur in that case is 3 by default
