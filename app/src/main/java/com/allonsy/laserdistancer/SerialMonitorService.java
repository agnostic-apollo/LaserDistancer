package com.allonsy.laserdistancer;


import android.content.Context;
import android.content.Intent;
import android.hardware.usb.UsbDevice;
import android.os.IBinder;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;

public class SerialMonitorService extends BaseService {

    Context context;
    UsbDevice device;
    private ArduinoCommunicator mArduinoCommunicator;
    private volatile boolean serialMonitor = false;
    private volatile boolean alreadyStopping = false;
    public static final String ACTION_SERVICE_START = "startSerialMonitorService";
    public static final String ACTION_SERVICE_STOP = "stopSerialMonitorService";
    public static final String ACTION_SEND_COMMAND = "sendCommand";
    public static final String EXTRA_COMMAND = "command";
    private String logsFilePath = "";


    public SerialMonitorService() {
        super();
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    public void onCreate() {
        context = this;
        unCaughtExceptionHandler();

        mArduinoCommunicator = new ArduinoCommunicator(this);
    }

    public int onStartCommand(final Intent intent, int flags, int startId) {
        Thread serialMonitorServiceThread = new Thread(new Runnable() {
            public void run() {
                unCaughtExceptionHandler();
                acquireWakelocks();

                if (intent.getAction().equals((SerialMonitorService.ACTION_SERVICE_START))) {
                    logsFilePath = intent.getStringExtra(BaseActivity.EXTRA_LOGS_FILE_PATH);
                    if (!serialMonitor) {
                        device = (UsbDevice) intent.getParcelableExtra(BaseActivity.EXTRA_USB_DEVICE);
                        start();
                    } else
                        logDebug("serial monitor already running, exiting");
                }
                else if (intent.getAction().equals((SerialMonitorService.ACTION_SERVICE_STOP)))
                {
                    if (serialMonitor) {
                        mArduinoCommunicator.resetArduino();
                        logDebug("stopping serial monitor");
                        stopService();
                    }
                    else
                        logDebug("serial monitor running, ignoring stop command");
                }
                else if (intent.getAction().equals((SerialMonitorService.ACTION_SEND_COMMAND))) {
                    String command =  intent.getStringExtra(SerialMonitorService.EXTRA_COMMAND);
                    if(mArduinoCommunicator!=null && command!=null)
                         mArduinoCommunicator.write(command);
                }
            }
        });
        serialMonitorServiceThread.start();

        return START_NOT_STICKY;
    }

    private void start()
    {
        serialMonitor = true;
        if (!mArduinoCommunicator.start(device)) {
            logError("Failed to connect to device");
            stopService();
        }
        else {
            logDebug("serial monitor started");
        }
    }
    public void stop() {
        if(mArduinoCommunicator!=null) {
            if(!mArduinoCommunicator.isSerialPortNull()) {
                mArduinoCommunicator.resetArduino();
            }
            mArduinoCommunicator.stop();
            mArduinoCommunicator=null;
        }
        serialMonitor = false;
    }

    public void stopService()
    {
        if(!alreadyStopping) {
            alreadyStopping = true;
            stop();
        }
        releaseWakelocks();
        logDebug("Stopping Service");
        stopSelf();
    }


    public void logDebug(String message) {

        Intent intent = new Intent();
        intent.setAction(SerialMonitorActivity.ACTION_UPDATE_LOG);
        intent.putExtra(SerialMonitorActivity.EXTRA_LOG,message);
        intent.putExtra(SerialMonitorActivity.EXTRA_LOG_TYPE,SerialMonitorActivity.EXTRA_LOG_TYPE_DEBUG);
        sendBroadcast(intent);
        Logger.logDebug(message);
        if(logsFilePath!=null)
            FileUtil.writeStringToTextFile(logsFilePath, message, true);
    }

    public void logError(String message) {

        Intent intent = new Intent();
        intent.setAction(SerialMonitorActivity.ACTION_UPDATE_LOG);
        intent.putExtra(SerialMonitorActivity.EXTRA_LOG,message);
        intent.putExtra(SerialMonitorActivity.EXTRA_LOG_TYPE,SerialMonitorActivity.EXTRA_LOG_TYPE_ERROR);
        sendBroadcast(intent);
        Logger.logError(message);
        if(logsFilePath!=null)
            FileUtil.writeStringToTextFile(logsFilePath, message, true);
    }

    public void logStackTrace(Exception e)
    {
        try {StringWriter errors = new StringWriter();
            PrintWriter pw = new PrintWriter(errors);
            e.printStackTrace(pw);
            pw.close();
            logError(errors.toString());
            errors.close();
        } catch (IOException e1) {
            e1.printStackTrace();
        }
    }

    public void unCaughtExceptionHandler() {
        Thread.setDefaultUncaughtExceptionHandler(new Thread.UncaughtExceptionHandler() {
            public void uncaughtException(final Thread thread, final Throwable ex) {
                ex.printStackTrace();
                logError("Uncaught Exception caught: " + ex.getMessage());
                stopService();
            }
        });
    }


}
