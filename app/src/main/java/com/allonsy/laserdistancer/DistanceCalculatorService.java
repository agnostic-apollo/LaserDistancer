package com.allonsy.laserdistancer;


import android.content.Context;
import android.content.Intent;
import android.hardware.usb.UsbDevice;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;

import org.opencv.core.KeyPoint;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.List;

public class DistanceCalculatorService extends BaseService {
    Context context;
    private volatile boolean measuring = false;
    private volatile boolean alreadyStopping = false;
    private volatile int photoCount;
    private volatile int totalTries;
    private int size0Tries;
    private int size1Tries;
    private int size2PlusTries;
    private volatile PhotoTaker photoTaker;
    private volatile BlobDetector blobDetector;
    private volatile ArduinoCommunicator mArduinoCommunicator;
    private volatile LaserUtil mLaserUtil;
    private volatile Thread distanceCalculatingThread;
    private volatile Handler distanceCalculatingThreadHandler;

    public static final String ACTION_SERVICE_START = "startService";
    public static final String ACTION_SERVICE_STOP = "stopService";
    public static final String EXTRA_IMAGE_ARRAY = "imageArray";
    public static final String SENDER = "sender";
    public static final String SENDER_PHOTO_TAKER = "photoTaker";
    public static final String SENDER_BLOB_DETECTOR = "blobDetector";

    public static final int MAX_PHOTO_COUNT = 20;

    private String currentLogsFilePath = "";
    private String logsFilePath = "";



    public int laserAngle = 0;


    public DistanceCalculatorService() {
        super();
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    public void onCreate() {
        context = this;
        unCaughtExceptionHandler();
        photoTaker = new PhotoTaker(this);
        blobDetector = new BlobDetector(this);
        mArduinoCommunicator = new ArduinoCommunicator(this);
        mLaserUtil = new LaserUtil(this,blobDetector);
    }

    public int onStartCommand(final Intent intent, int flags, int startId) {
        logDebug("onStart Command");
        Thread distanceCalculatorServiceThread = new Thread(new Runnable() {
            public void run() {
                if (intent.getAction().equals((DistanceCalculatorService.ACTION_SERVICE_START))) {
                    if (!measuring) {
                        measuring = true;

                        boolean success = true;
                        unCaughtExceptionHandler();
                        acquireWakelocks();

                        currentLogsFilePath = intent.getStringExtra(BaseActivity.EXTRA_CURRENT_LOGS_FILE_PATH);
                        logsFilePath = intent.getStringExtra(BaseActivity.EXTRA_LOGS_FILE_PATH);
                        blobDetector.setBlobImageFolderPath(intent.getStringExtra(BaseActivity.EXTRA_IMAGES_FOLDER_PATH));
                        photoTaker.setCameraImageFolderPath(intent.getStringExtra(BaseActivity.EXTRA_IMAGES_FOLDER_PATH));

                        if (!blobDetector.start()) {
                            logError("Failed to start blobDetector");
                            success = false;
                            stopService();
                        }

                        if (success && !photoTaker.start()) {
                            logError("Failed to start photoTaker");
                            success = false;
                            stopService();
                        }

                        if (success && !mArduinoCommunicator.start((UsbDevice) intent.getParcelableExtra(BaseActivity.EXTRA_USB_DEVICE))) {
                                logError("Failed to connect to device");
                            success = false;
                            stopService();
                        }
                        // mArduinoCommunicator.write(String.valueOf(90));
                        // try {Thread.sleep(2000);} catch (Exception e) {Logger.logStackTrace(e);} //wait for laser to move
                        // mArduinoCommunicator.write(String.valueOf(0));
                        // mArduinoCommunicator.write("getAngle");
                        if (success) {
                            totalTries = 0;
                            photoCount = 0;
                            distanceCalculatingThread = new DistanceCalculatingThread();
                            distanceCalculatingThread.start();

                            long startTime = System.currentTimeMillis();
                            while (distanceCalculatingThreadHandler == null && (System.currentTimeMillis() - startTime) < 10000) {
                                try {
                                    Thread.sleep(100);
                                } catch (Exception e) {
                                    Logger.logStackTrace(e);
                                }
                            }
                            if (distanceCalculatingThreadHandler == null) {
                                logError("Failed to setup distanceCalculatingThreadHandler");
                                stopService();
                            } else {
                                //set up handlers and take 1st image
                                photoTaker.setHandler(distanceCalculatingThreadHandler);
                                blobDetector.setHandler(distanceCalculatingThreadHandler);
                                size0Tries =0;
                                size1Tries =0;
                                size2PlusTries =0;

                                laserAngle = mLaserUtil.resetAndGetInitialAngle();
                                mArduinoCommunicator.resetArduino();
                                mArduinoCommunicator.setLaserAngle(laserAngle);
                                mArduinoCommunicator.turnOnBothLasers();
                                if (!photoTaker.takePhoto())
                                    stopService();
                            }

                        }

                    } else
                        logDebug("Already measuring, exiting");
                }
                else if (intent.getAction().equals((DistanceCalculatorService.ACTION_SERVICE_STOP)))
                {
                    if (measuring) {
                        logDebug("force stopping service");
                        stopService();
                    }
                    else
                        logDebug("not measuring, ignoring stop command");
                }
            }
        });
        distanceCalculatorServiceThread.start();
        return START_NOT_STICKY;
    }



    public void stop() {
        if(photoTaker!=null) {
            photoTaker.stop();
            photoTaker=null;
        }
        if(mArduinoCommunicator!=null) {
            if(!mArduinoCommunicator.isSerialPortNull()) {
                mArduinoCommunicator.resetArduino();
            }
            mArduinoCommunicator.stop();
            mArduinoCommunicator=null;
        }
        if (distanceCalculatingThreadHandler != null) {
            distanceCalculatingThreadHandler.getLooper().quit();
            distanceCalculatingThreadHandler=null;
        }
        measuring = false;
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

    private void sendUpdateDistanceIntentToActivity(float distance) {

        QueryPreferences.setDistance(this,distance);
        Intent intent = new Intent();
        intent.setAction(DistanceCalculatorActivity.ACTION_UPDATE_DISTANCE);
        sendBroadcast(intent);
    }
    public void sendUpdateImageIntentToActivity(String filePath) {
        QueryPreferences.setImagePath(this,filePath);
        Intent intent = new Intent();
        intent.setAction(DistanceCalculatorActivity.ACTION_UPDATE_IMAGE);
        sendBroadcast(intent);
    }

    public void logDebug(String message) {

        Intent intent = new Intent();
        intent.setAction(DistanceCalculatorActivity.ACTION_UPDATE_LOG);
        intent.putExtra(DistanceCalculatorActivity.EXTRA_LOG,message);
        intent.putExtra(DistanceCalculatorActivity.EXTRA_LOG_TYPE, DistanceCalculatorActivity.EXTRA_LOG_TYPE_DEBUG);
        sendBroadcast(intent);
        Logger.logDebug(message);
        if(currentLogsFilePath!=null)
            FileUtil.writeStringToTextFile(currentLogsFilePath, message, true);
        if(logsFilePath!=null)
            FileUtil.writeStringToTextFile(logsFilePath, message, true);
    }

    public void logError(String message) {
        Intent intent = new Intent();
        intent.setAction(DistanceCalculatorActivity.ACTION_UPDATE_LOG);
        intent.putExtra(DistanceCalculatorActivity.EXTRA_LOG,message);
        intent.putExtra(DistanceCalculatorActivity.EXTRA_LOG_TYPE, DistanceCalculatorActivity.EXTRA_LOG_TYPE_ERROR);
        sendBroadcast(intent);
        Logger.logError(message);
        if(currentLogsFilePath!=null)
            FileUtil.writeStringToTextFile(currentLogsFilePath, message, true);
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

    private class DistanceCalculatingThread extends Thread {

        @Override
        public void run() {
            unCaughtExceptionHandler();
            Looper.prepare();

            distanceCalculatingThreadHandler = new Handler(){

            @Override
            public void handleMessage(Message msg) {
                Bundle bundle = msg.getData();
                String sender = bundle.getString(DistanceCalculatorService.SENDER);
                if(sender!=null) {
                    if (sender.equals(SENDER_PHOTO_TAKER)) {
                        if(totalTries==2) {
                            sendUpdateDistanceIntentToActivity(-1);
                            logError("Failed to calculate distance even after 2 tries");
                            stopService();
                            return;
                        }
                        byte[] imageArray = bundle.getByteArray(DistanceCalculatorService.EXTRA_IMAGE_ARRAY);
                        photoCount++;
                        if(photoCount==1) {
                           blobDetector.storeFirstImage(imageArray);
                           laserAngle = mLaserUtil.getInitialAngleChange();
                           mArduinoCommunicator.setLaserAngle(laserAngle);
                         }
                         else {
                            logDebug("Detecting Blobs of photoCount = " + String.valueOf(photoCount));
                            List<KeyPoint> blobs;
                            if (photoCount == 2)
                                blobs = blobDetector.storeSecondImageAndDetectBlobs(imageArray);
                            else
                                blobs = blobDetector.detectBlobs(imageArray);

                            if (blobs == null) {
                                sendUpdateDistanceIntentToActivity(-1);
                                logError("Error Detecting Blobs");
                                stopService();
                                return;
                            }

                            for (int i = 0; i != blobs.size(); i++) {
                                logDebug("Blob " + String.valueOf(i) + " at " + String.valueOf(blobs.get(i).pt.x) + ", " + String.valueOf(blobs.get(i).pt.y)+ " with diameter " + String.valueOf(blobs.get(i).size) + " and octave " + String.valueOf(blobs.get(i).octave));
                            }
                            logDebug(String.valueOf(blobs.size()) + " blobs detected");

                            //if something went wrong in the first 2 picture, try again
                            if (photoCount == 2 && blobs.size() != 1 && blobs.size() != 2) {
                                logDebug("Restarting measurement since blobs size not equal to 1 or 2");
                                restartMeasurement();
                            }
                            else {

                                //String filePath = blobDetector.getBlobImageFilePath();
                                //if (!filePath.equals(""))
                                    //sendUpdateImageIntentToActivity(filePath);

                                if (blobs.size() == 0) {//failed to detect any blobs
                                    size1Tries = 0;
                                    size2PlusTries = 0;
                                    if (size0Tries < 1) { //try once again to detect blobs
                                        logDebug("trying again to make sure");
                                        size0Tries++;
                                    } else {
                                        sendUpdateDistanceIntentToActivity(-1);
                                        logError("Failed to find any blobs in image even after 1 retry");
                                        stopService();
                                    }
                                } else if (blobs.size() == 1) { //if only 1 blob detected lasers are aligned, so calculate distance
                                    size0Tries = 0;
                                    size2PlusTries = 0;
                                    if (size1Tries < 1) { //try once again to be sure
                                        logDebug("trying again to make sure");
                                        size1Tries++;
                                    } else {
                                        logDebug("Angle = " + String.valueOf(laserAngle));
                                        sendUpdateDistanceIntentToActivity(mLaserUtil.calculateDistance());
                                        logDebug("Distance = " + String.valueOf(mLaserUtil.calculateDistance()) + " pixelChangeAfterTwoDegreeChange =  " + String.valueOf(blobDetector.getPixelChangeAfterTwoDegreeChange()));
                                        try {
                                            Thread.sleep(1000);
                                        } catch (Exception e) {
                                            Logger.logStackTrace(e);
                                        }
                                        stopService();
                                    }
                                } else if (blobs.size() == 2) { //if 2 blobs detected lasers are not aligned yet
                                    //sendUpdateDistanceIntentToActivity(0);
                                    size0Tries = 0;
                                    size1Tries = 0;
                                    size2PlusTries = 0;

                                    laserAngle = mLaserUtil.calculateNewAngle(blobs);
                                    //move laser...
                                    logDebug("Angle = " + String.valueOf(laserAngle));
                                    mArduinoCommunicator.setLaserAngle(laserAngle);
                                    //mArduinoCommunicator.write("getAngle");
                                } else { //if more than 2 blobs
                                    size0Tries = 0;
                                    size1Tries = 0;
                                    if (size2PlusTries < 2) { //try once again to be sure
                                        logDebug("trying again to make sure");
                                        size2PlusTries++;
                                    } else {
                                        sendUpdateDistanceIntentToActivity(-1);
                                        logError("Finding more than 2 blobs even after 2 retries");
                                        stopService();
                                    }
                                }
                            }
                        }
                        //take next photo
                        if (photoCount < MAX_PHOTO_COUNT){
                            if (photoTaker!=null && !photoTaker.takePhoto())
                                stopService();
                        }
                        else{
                            sendUpdateDistanceIntentToActivity(-1);
                            logError("Failed to find only 1 blob even after " + String.valueOf(photoCount) + " images");
                            stopService();
                        }

                    }
                    //else if (sender.equals(SENDER_BLOB_DETECTOR)) {
                        //logDebug("sending image update intent");
                       // sendUpdateImageIntentToActivity(bundle.getString(DistanceCalculatorService.EXTRA_IMAGE_PATH));
                   // }
                }

            }};

            Looper.loop(); //start receiving messages from photoTaker
        }
    }

    public void restartMeasurement() {
        totalTries++;
        photoCount = 0;
        size0Tries =0;
        size1Tries =0;
        size2PlusTries =0;
        laserAngle = mLaserUtil.resetAndGetInitialAngle();
        mArduinoCommunicator.setLaserAngle(laserAngle);
        if(!blobDetector.setDefaultOpenCVParameters()){
            logError("Failed to restartMeasurement");
            stopService();
        }
    }

    public void unCaughtExceptionHandler() {
        Thread.setDefaultUncaughtExceptionHandler(new Thread.UncaughtExceptionHandler() {
            public void uncaughtException(final Thread thread, final Throwable ex) {
                ex.printStackTrace();
                logError("Uncaught Exception caught: " + ex.getMessage());
                //release camera just in case stopService fails to
                if(photoTaker!=null) {
                    photoTaker.stop();
                    photoTaker=null;
                }
                stopService();
            }
        });
    }


}
