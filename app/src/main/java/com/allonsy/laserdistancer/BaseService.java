package com.allonsy.laserdistancer;


import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.hardware.usb.UsbDevice;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.os.PowerManager;

import org.opencv.core.KeyPoint;

import java.util.List;

public class BaseService extends Service {

    PowerManager.WakeLock wakeLock;

    public BaseService() {
        super();
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    public void onCreate() {
    }

    public int onStartCommand(final Intent intent, int flags, int startId) {

        return START_NOT_STICKY;
    }

    public void stopService()
    {
    }


    public void logDebug(String message) {

    }

    public void logError(String message) {

    }

    public void logStackTrace(Exception e) {

    }

    public void unCaughtExceptionHandler() {
        Thread.setDefaultUncaughtExceptionHandler(new Thread.UncaughtExceptionHandler() {
            public void uncaughtException(final Thread thread, final Throwable ex) {
                logStackTrace(new Exception(ex));
                logError("Uncaught Exception caught: " + ex.getMessage());
                stopService();
            }
        });
    }

    public void acquireWakelocks() {
        if(wakeLock==null) {
            PowerManager pMgr = (PowerManager) getApplicationContext().getSystemService(Context.POWER_SERVICE);
            wakeLock = pMgr.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "com.allonsy.laserdistancer.wakelock");
        }

        try {
            if (wakeLock != null && !wakeLock.isHeld())
                wakeLock.acquire();
        } catch (Exception e) {
            //Logger.logError("Error getting wifiLock: " + e.getMessage());
        }
    }

    public void releaseWakelocks() {
        Logger.logDebug("releasing wakelock ");
        try {
            if (wakeLock != null && wakeLock.isHeld()) {
                wakeLock.release();
                wakeLock=null;
            }
        } catch (Exception e) {
            //Logger.logError("Error releasing wakeLock: " + e.getMessage());
        }
    }
}
