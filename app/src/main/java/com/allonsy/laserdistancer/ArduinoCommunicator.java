package com.allonsy.laserdistancer;


import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbDeviceConnection;
import android.hardware.usb.UsbManager;

import com.felhr.usbserial.UsbSerialDevice;
import com.felhr.usbserial.UsbSerialInterface;

import java.nio.charset.StandardCharsets;

public class ArduinoCommunicator {

    BaseService distanceCalculatorService;
    UsbDevice device;
    UsbManager usbManager;
    UsbSerialDevice serialPort;
    UsbDeviceConnection connection;
    private static final String ACTION_USB_PERMISSION = "usbPermmision";
    private volatile boolean read;
    String readString;

    public ArduinoCommunicator(BaseService distanceCalculatorService) {
        this.distanceCalculatorService = distanceCalculatorService;
    }

    public boolean start(UsbDevice inputDevice) {
        this.device = inputDevice;
        try {
        registerUsbDisconnectnBroadcastReceiver();

        if (device == null)
            return false;


            usbManager = (UsbManager) distanceCalculatorService.getSystemService(Context.USB_SERVICE);
            connection = usbManager.openDevice(device);

            if (connection == null) {
                logError("Failed to open connection to device");
                return false;
            }

            serialPort = UsbSerialDevice.createUsbSerialDevice(device, connection);
            if (serialPort != null) {
                if (serialPort.syncOpen()) { //Set Serial Connection Parameters.
                    serialPort.setBaudRate(19200);
                    serialPort.setDataBits(UsbSerialInterface.DATA_BITS_8);
                    serialPort.setStopBits(UsbSerialInterface.STOP_BITS_1);
                    serialPort.setParity(UsbSerialInterface.PARITY_NONE);
                    serialPort.setFlowControl(UsbSerialInterface.FLOW_CONTROL_OFF);
                    //serialPort.read(mUsbReadCallback);
                    read = true;
                    readString = "";
                    new ReadThread().start();
                    try {
                        Thread.sleep(3000);
                    } catch (Exception e) {
                        Logger.logStackTrace(e);
                    }
                } else {
                    logError("Port not open");
                    return false;
                }
            } else {
                logError("Port is null");
                return false;
            }
        }
        catch(Exception e)
        {
            logError(e.getMessage());
            return false;
        }

        return true;
    }

    public void stop(){
        unregisterUsbDisconnectnBroadcastReceiver();
        if(serialPort!=null)
            serialPort.syncClose();
        serialPort=null;
        read = false;

    }

    private final BroadcastReceiver usbDisconnectnBroadcastReceiver = new BroadcastReceiver() { //Broadcast Receiver to automatically start and stop the Serial connection.
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent.getAction().equals(UsbManager.ACTION_USB_DEVICE_DETACHED)) {
                logDebug("Device Disconnected");
                distanceCalculatorService.stopService();
            }
        };
    };

    public void registerUsbDisconnectnBroadcastReceiver() {

        IntentFilter filter = new IntentFilter(ACTION_USB_PERMISSION);
        filter.addAction(UsbManager.ACTION_USB_DEVICE_ATTACHED);
        filter.addAction(UsbManager.ACTION_USB_DEVICE_DETACHED);
        distanceCalculatorService.registerReceiver(usbDisconnectnBroadcastReceiver, filter);
    }

    public void unregisterUsbDisconnectnBroadcastReceiver() {
        try {
            distanceCalculatorService.unregisterReceiver(usbDisconnectnBroadcastReceiver);
        }
        catch(IllegalArgumentException e) {
            //distanceCalculatorService.logError(e.getMessage());
        }
    }

    UsbSerialInterface.UsbReadCallback mUsbReadCallback = new UsbSerialInterface.UsbReadCallback() {
        //Defining a Callback which triggers whenever data is read.
        @Override
        public void onReceivedData(byte[] arg0) {
            String data = null;
            data = new String(arg0,StandardCharsets.UTF_8);
            logDebug("Arduino (" + String.valueOf(arg0.length) + ") : " + data);
            try {Thread.sleep(100);} catch (Exception e) {Logger.logStackTrace(e);}
        }
    };

    public boolean write(String command)
    {
        if(serialPort==null)
            return false;

        serialPort.syncWrite((command + "\n").getBytes(StandardCharsets.UTF_8),0);
        try {Thread.sleep(100);} catch (Exception e) {Logger.logStackTrace(e);}
        //serialPort.write("\n".getBytes());
        return true;
    }

    private class ReadThread extends Thread {
        @Override
        public void run() {
            distanceCalculatorService.unCaughtExceptionHandler();
            while(read){
                if(serialPort!=null)
                {
                    byte[] buffer = new byte[100];
                    int n = serialPort.syncRead(buffer, 0);
                    if(n > 0) {
                        byte[] received = new byte[n];
                        System.arraycopy(buffer, 0, received, 0, n);
                        readString += new String(received, StandardCharsets.UTF_8);
                        int i = 0;
                        while (true) {
                            char ch = readString.charAt(i);

                            if (ch == '\n') {
                                if (i > 0) { //dont read if only newline received
                                    String subString = readString.substring(0, i - 1);
                                    processCommand(subString);

                                }

                                if (i + 1 != readString.length())
                                    readString = readString.substring(i + 1);
                                else
                                    readString = "";
                                i = -1;
                            }
                            i++;
                            if (i == readString.length())
                                break;

                        }
                    }

                }
            }
        }
    }

    private void processCommand(String command)
    {
        if(command.contains("angle:"))
        {
            int index = "angle:".length();
            if(index<command.length())
            {
                logDebug("Arduino : Servo Angle = " + command.substring(index));
            }
        }
        else
        {
            logDebug("Arduino : " + command);
        }

    }
    private void logDebug(String message) {distanceCalculatorService.logDebug(message);}

    private void logError(String message) {distanceCalculatorService.logError(message);}

    private void logStackTrace(Exception e) {distanceCalculatorService.logStackTrace(e);}



    public void setLaserAngle(int laserAngle)
    {
        if(laserAngle>=0 && laserAngle<=180) {
            if (!write("setAngle:" + String.valueOf(laserAngle))) {
                logError("Failed to send command to set laser angle");
                distanceCalculatorService.stopService();
            }
            try {
                Thread.sleep(500);
            } catch (Exception e) {
                Logger.logStackTrace(e);
            } //wait for laser to move
        }
    }

    public void getLaserAngle()
    {
        if (!write("getAngle")) {
            logError("Failed to send command to get laser angle");
            distanceCalculatorService.stopService();
        }
    }

    public void turnOnBothLasers()
    {
        if (!write("turnOnBothLasers")) {
            logError("Failed to send command to turn on both lasers");
            distanceCalculatorService.stopService();
        }
    }

    public void turnOffBothLasers()
    {
        if (!write("turnOffBothLasers")) {
            logError("Failed to send command to turn off both lasers");
            distanceCalculatorService.stopService();
        }
    }

    public void resetArduino()
    {
        if (!write("reset")) {
            logError("Failed to send command to reset arduino");
            distanceCalculatorService.stopService();
        }
    }

    public boolean isSerialPortNull()
    {
        if(serialPort==null)
            return true;
        else
            return false;
    }

}

