# LaserDistancer-and-SerialMonitor
A project to calculate distance using lasers, camera(android phone), servo motor and arduino. (app includes a serial monitor)

# Description
(to be updated)

# Arduino Serial Monitor
The app includes a serial monitor to communicate with the arduino connected to an android phone using usb otg.  
It uses the [UsbSerial API](https://github.com/felHR85/UsbSerial).  

The communication is done using syncRead and syncWrite API.    
The commands are processed only after newline character is detected, arguments can also be passed to commands by sperating commands and arguments with a colon ':'    

Writing to arduino : "getAngle" or "setAngle:90" or "turnOnBothLasers"    
Reading from arduino : "angle:90" or "turning on lasers"    

Check out [Arduino Sketch](https://github.com/agnostic-apollo/LaserDistancer-and-SerialMonitor/blob/master/Arduino/Arduino.ino) 
and [ArduinoCommunicator Android Java Class](https://github.com/agnostic-apollo/LaserDistancer-and-SerialMonitor/blob/master/app/src/main/java/com/allonsy/laserdistancer/ArduinoCommunicator.java)    
  
[Screenshots](https://github.com/agnostic-apollo/LaserDistancer-and-SerialMonitor/tree/master/screenshots)  
[Download APK](https://github.com/agnostic-apollo/LaserDistancer-and-SerialMonitor/releases)   
(apk has large size since openCV is integrated in the app for the main project)   
(serail monitor can be seperated if requested)   

Credits  
[OpenCV 3.2.0 Android SDK](http://opencv.org/platforms/android/) ([Download](https://sourceforge.net/projects/opencvlibrary/files/opencv-android/))   
[UsbSerial](https://github.com/felHR85/UsbSerial)  
