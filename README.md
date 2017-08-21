# LaserDistancer-and-SerialMonitor
A project to calculate distance using lasers, camera(android phone), servo motor and arduino. (app includes a serial monitor)

# Description
(to be updated)

# Arduino Serial Monitor
The app includes a serial monitor to communicate with the arduino connected to an android phone using usb otg.
It uses the [UsbSerial API](https://github.com/felHR85/UsbSerial).   
The communication is done using syncRead and syncWrite API. The commands are processed only after newline character is detected. Also Arguments can be passed to commands by sperating commands and arguments with a colon ':'.  
writing to arduino example "setAngle:90"  
reading from arduino example "angle:90"  
Check out [Arduino Sketch](https://github.com/agnostic-apollo/LaserDistancer-and-SerialMonitor/blob/master/Arduino/Arduino.ino) 
and [Android Java Class]

[Screenshots](https://github.com/agnostic-apollo/LaserDistancer-and-SerialMonitor/tree/master/screenshots)  
[Download APK](https://github.com/agnostic-apollo/LaserDistancer-and-SerialMonitor/releases)  

Credits  
[OpenCV 3.2.0 Android SDK](http://opencv.org/platforms/android/) ([Download](https://sourceforge.net/projects/opencvlibrary/files/opencv-android/))   
[UsbSerial](https://github.com/felHR85/UsbSerial)  
