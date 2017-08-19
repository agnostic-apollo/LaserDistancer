package com.allonsy.laserdistancer;


import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.ImageFormat;
import android.graphics.SurfaceTexture;
import android.hardware.Camera;
import android.media.AudioManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Date;
import java.util.List;

public class PhotoTaker {
    private DistanceCalculatorService distanceCalculatorService;
    private boolean alreadyTakingPhoto = false;
    private boolean focusModeAutoAvailable = false;
    private String cameraImageFolderPath;
    private int errorCount = 0;
    private Handler distanceCalculatorServiceHandler;
    private Camera camera;


    private static boolean enableAutofocus = true;

    public PhotoTaker(DistanceCalculatorService distanceCalculatorService)
    {
        this.distanceCalculatorService = distanceCalculatorService;
    }

    public boolean start() {
        if (!(distanceCalculatorService.getApplicationContext().getPackageManager().hasSystemFeature(PackageManager.FEATURE_CAMERA))) {
            logError("Camera missing on device");
            return false;
        }
        int cameraID = Camera.CameraInfo.CAMERA_FACING_BACK;

        //open the camera if it exists
        int cameraCount;
        boolean cameraExists = false;
        Camera.CameraInfo cameraInfo = new Camera.CameraInfo();
        cameraCount = Camera.getNumberOfCameras();
        for (int i = 0; i < cameraCount; i++) {
            Camera.getCameraInfo(i, cameraInfo);
            if (cameraInfo.facing == cameraID) {
                try {
                    camera = Camera.open(i);
                    cameraExists = true;
                    break;
                } catch (RuntimeException e) {
                    stop();
                    logError("Failed to open Back Camera");
                    return false;
                }
            }
        }

        if (!cameraExists) {
            logError("Back Camera does not exist");
            return false;
        }

        if (camera == null) {
            logError("Camera null, Failed to open Back Camera");
            return false;
        }

        SurfaceTexture surfaceTexture = new SurfaceTexture(0);
        try {
            //holder = view.getHolder();
            camera.setPreviewTexture(surfaceTexture);
            //this.setVisibility(INVISIBLE);
            //camera.setPreviewDisplay(holder);
        } catch (IOException e) {
            return false;
        }

        Camera.Parameters param;
        param = camera.getParameters();

        /*
        //set picture size to highest quality
        Camera.Size bestSize = null;
        List<Camera.Size> sizeList = camera.getParameters().getSupportedPictureSizes();
        bestSize = sizeList.get(0);
        for (int i = 1; i < sizeList.size(); i++) {
            // Logger.logDebug( "Supported Size: width : " + sizeList.get(i).width + "height : " + sizeList.get(i).height);
            if ((sizeList.get(i).width * sizeList.get(i).height) > (bestSize.width * bestSize.height)) {
                bestSize = sizeList.get(i);
            }
        }
        param.setPictureSize(bestSize.width, bestSize.height);
        */

        //set picture size to 720p
        Camera.Size lowSize = null;
        List<Camera.Size> sizeList = camera.getParameters().getSupportedPictureSizes();
        lowSize = sizeList.get(0);
        for (int i = 1; i < sizeList.size(); i++) {
            //Logger.logDebug( "Supported Size: width : " + sizeList.get(i).width + "height : " + sizeList.get(i).height);
            if (sizeList.get(i).height==720) {
                lowSize = sizeList.get(i);
                break;
            }
        }
        param.setPictureSize(lowSize.width, lowSize.height);

        param.setFlashMode(Camera.Parameters.FLASH_MODE_OFF);
        param.setPictureFormat(ImageFormat.JPEG);
        param.setJpegQuality(100);

            if (param.getSupportedWhiteBalance().contains(
                    Camera.Parameters.WHITE_BALANCE_AUTO)) {
                param.setWhiteBalance(Camera.Parameters.WHITE_BALANCE_AUTO);
            }
            /*
            param.setExposureCompensation(param.getMaxExposureCompensation());

            if (param.isAutoExposureLockSupported()) {
                param.setAutoExposureLock(false);
            }
            */
        //set autofocus if device supports it and is enabled by server
        boolean focusModeAutoAvailable = false;
        if (param.getSupportedFocusModes().contains(
                Camera.Parameters.FOCUS_MODE_AUTO)) {
            param.setFocusMode(Camera.Parameters.FOCUS_MODE_AUTO);
            focusModeAutoAvailable = true;

        }


        try {
            camera.setParameters(param);
        } catch (Exception e) {
            logError("Error setting camera params1: " + e.getMessage());
        }


        return true;
    }

    public boolean takePhoto()
    {
        if(camera==null)
        {
           distanceCalculatorService.logError("camera null, cant takePhoto");
          return false;
        }

        if (!alreadyTakingPhoto) {
            try {
            alreadyTakingPhoto = true;
            try {Thread.sleep(1000);} catch (InterruptedException e) {Logger.logStackTrace(e);}
            camera.startPreview();
            try {Thread.sleep(700);} catch (InterruptedException e) {Logger.logStackTrace(e);}

            setMuteAll(true);
            if (focusModeAutoAvailable && enableAutofocus){
                camera.autoFocus(new Camera.AutoFocusCallback() {
                    @Override
                    public void onAutoFocus(boolean success, Camera camera) {
                        //Logger.logDebug("autofocus");
                        if(success) camera.takePicture(null, null, pic);
                    }
                });
            }else{
                camera.takePicture(null, null, pic);
            }

            alreadyTakingPhoto = false;
            errorCount=0;

            return true;
            }
            catch (Exception e)
            {
                //logDebug(e.getMessage());
               if(e.getMessage().contains("startPreview failed") && errorCount==0)
               {
                   //logDebug("exception startPreview");
                   stop();
                   try {Thread.sleep(400);} catch (InterruptedException ex) {Logger.logStackTrace(ex);}
                   errorCount++;
                   if(!start())
                       return false;
                   return takePhoto();
               }
               else {
                   if(e.getMessage().contains("startPreview failed"))
                        logDebug(e.getMessage() + " twice, exiting");
                   else
                       logDebug(e.getMessage());

                   return false;
               }
            }
        }

        else
            return false;
    }

    private Camera.PictureCallback pic = new Camera.PictureCallback() {

        public void onPictureTaken(byte[] data, Camera camera) {
            setMuteAll(false);
            createCameraImageFile(data);
            if(distanceCalculatorServiceHandler!=null) {
                Message msg = new Message();
                Bundle bundle = new Bundle();
                bundle.putByteArray(DistanceCalculatorService.EXTRA_IMAGE_ARRAY, data);
                bundle.putString(DistanceCalculatorService.SENDER,DistanceCalculatorService.SENDER_PHOTO_TAKER);
                msg.setData(bundle);
                distanceCalculatorServiceHandler.sendMessage(msg);
                //  camera.stopPreview();
        }
        }
    };

    private void createCameraImageFile(byte[] data) {
        int minFreeSpace = 200;//dont record if space in external storage is less than 200mb

        //Set output file
        if(cameraImageFolderPath==null){
            logError("Output Directory to store Camera Image does not exists");
            return;
        }

        long freespace = new File(cameraImageFolderPath).getFreeSpace() / 1024;
        if (freespace < minFreeSpace) {
            logError("Space less than " + String.valueOf(freespace) + "mb on device, free space = " + String.valueOf(freespace) + "mb");
            return;
        }

        String cameraImageFilePath = cameraImageFolderPath + File.separator + ("IMG_CAMERA_" + new Date(System.currentTimeMillis())).replaceAll(" ", "_").replaceAll(":", "_") + ".jpeg";

        try{
            FileOutputStream outStream  = new FileOutputStream(cameraImageFilePath);
            outStream.write(data);
            outStream.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void stop()
    {
        alreadyTakingPhoto=false;
        releaseCamera();
    }

    private void releaseCamera(){

        if (camera != null){
            camera.stopPreview();
            camera.release();        // release the camera for other applications
            camera = null;
        }
    }


    public void setMuteAll(boolean mute) {
        AudioManager manager = (AudioManager) distanceCalculatorService.getApplicationContext().getSystemService(Context.AUDIO_SERVICE);

        int[] streams = new int[]{AudioManager.STREAM_ALARM,
                AudioManager.STREAM_DTMF, AudioManager.STREAM_MUSIC,
                AudioManager.STREAM_RING, AudioManager.STREAM_SYSTEM,
                AudioManager.STREAM_VOICE_CALL};

        for (int stream : streams)
            manager.setStreamMute(stream, mute);
    }

    public void setCameraImageFolderPath(String cameraImageFolderPath) {this.cameraImageFolderPath = cameraImageFolderPath;}

    public void setHandler( Handler handler) {
        distanceCalculatorServiceHandler = handler;
    }

    private void logDebug(String message) {distanceCalculatorService.logDebug(message);}

    private void logError(String message) {distanceCalculatorService.logError(message);}

    private void logStackTrace(Exception e) {distanceCalculatorService.logStackTrace(e);}
}
