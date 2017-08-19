package com.allonsy.laserdistancer;


import org.opencv.core.KeyPoint;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.List;

import static android.R.attr.format;


public class OpenCVParametersUtil {

    private DistanceCalculatorService distanceCalculatorService;

    private static float thresholdStepDefault = 10.0f;
    private static float minThresholdDefault = 170.0f;
    private static float maxThresholdDefault = 240.0f;
    private static int minRepeatabilityDefault = 2;
    private static float minDistBetweenBlobsDefault = 50.0f;
    private static int filterByColorDefault = 0;
    private static int blobColorDefault = 0;
    private static int filterByAreaDefault = 1;
    private static float minAreaDefault = 50.0f;
    private static float maxAreaDefault = 400.0f;
    private static int filterByCircularityDefault = 1;
    private static float minCircularityDefault = 0.85f;
    private static float maxCircularityDefault = 1.0f;
    private static int filterByInertiaDefault = 0;
    private static float minInertiaRatioDefault = 0.0f;
    private static float maxInertiaRatioDefault = 0.0f;
    private static int filterByConvexityDefault = 0;
    private static float minConvexityDefault = 0.0f;
    private static float maxConvexityDefault = 0.0f;


    private float thresholdStep;
    private float minThreshold;
    private float maxThreshold;
    private int minRepeatability;
    private float minDistBetweenBlobs;
    private int filterByColor;
    private int blobColor;
    private int filterByArea;
    private float minArea;
    private float maxArea;
    private int filterByCircularity;
    private float minCircularity;
    private float maxCircularity;
    private int filterByInertia;
    private float minInertiaRatio;
    private float maxInertiaRatio;
    private int filterByConvexity;
    private float minConvexity;
    private float maxConvexity;

    String paramsFilePath="";


    public OpenCVParametersUtil(DistanceCalculatorService distanceCalculatorService)
    {
        this.distanceCalculatorService = distanceCalculatorService;
        setDefaultParameters();
    }


    public boolean writeOpenCVParametersToFile()
    {
        File outDir;
        outDir = distanceCalculatorService.getFilesDir();
        String outDirPath="";
        if(outDir!=null && outDir.exists() && outDir.isDirectory())
            outDirPath = outDir.getAbsolutePath() + File.separator;
        else{
            logError("Output Directory to store Blob Param does not exists");
            return false;
        }

        //paramsFilePath = "android.resource://" + distanceCalculatorService.getPackageName() + "/xml/params.xml";
        paramsFilePath = outDirPath + "param.xml";
        String text =
            "<?xml version=\"1.0\"?>\n" +
            "<opencv_storage>\n" +
            "<format>3</format>\n" +
            "<thresholdStep>"+ thresholdStep + "</thresholdStep>\n" +
            "<minThreshold>"+ minThreshold + "</minThreshold>\n" +
            "<maxThreshold>"+ maxThreshold + "</maxThreshold>\n" +
            "<minRepeatability>"+ minRepeatability + "</minRepeatability>\n" +
            "<minDistBetweenBlobs>"+ minDistBetweenBlobs + "</minDistBetweenBlobs>\n" +
            "<filterByColor>"+ filterByColor + "</filterByColor>\n" +
            "<blobColor>"+ blobColor + "</blobColor>\n" +
            "<filterByArea>"+ filterByArea + "</filterByArea>\n" +
            "<minArea>"+ minArea + "</minArea>\n" +
            "<maxArea>"+ maxArea + "</maxArea>\n" +
            "<filterByCircularity>"+ filterByCircularity + "</filterByCircularity>\n" +
            "<minCircularity>"+ minCircularity + "</minCircularity>\n" +
            "<maxCircularity>"+ maxCircularity + "</maxCircularity>\n" +
            "<filterByInertia>"+ filterByInertia + "</filterByInertia>\n" +
            "<minInertiaRatio>"+ minInertiaRatio + "</minInertiaRatio>\n" +
            "<maxInertiaRatio>"+ maxInertiaRatio + "</maxInertiaRatio>\n" +
            "<filterByConvexity>"+ filterByConvexity + "</filterByConvexity>\n" +
            "<minConvexity>"+ minConvexity + "</minConvexity>\n" +
            "<maxConvexity>"+ maxConvexity + "</maxConvexity>\n" +
            "</opencv_storage>\n";

        if(FileUtil.writeStringToTextFile(paramsFilePath,text,false))
            return true;
        else
            return false;
    }

    public void setDefaultParameters()
    {
        thresholdStep = thresholdStepDefault;
        minThreshold = minThresholdDefault;
        maxThreshold = maxThresholdDefault;
        minRepeatability = minRepeatabilityDefault;
        minDistBetweenBlobs = minDistBetweenBlobsDefault;
        filterByColor = filterByColorDefault;
        blobColor = blobColorDefault;
        filterByArea = filterByAreaDefault;
        minArea = minAreaDefault;
        maxArea = maxAreaDefault;
        filterByCircularity = filterByCircularityDefault;
        minCircularity = minCircularityDefault;
        maxCircularity = maxCircularityDefault;
        filterByInertia = filterByInertiaDefault;
        minInertiaRatio = minInertiaRatioDefault;
        maxInertiaRatio = maxInertiaRatioDefault;
        filterByConvexity = filterByConvexityDefault;
        minConvexity = minConvexityDefault;
        maxConvexity = maxConvexityDefault;
    }

    public float getThresholdStep() {
        return thresholdStep;
    }

    public boolean setThresholdStep(float thresholdStep) {
        if(thresholdStep>=0)
            this.thresholdStep = thresholdStep;
        else {
            logError("error setting thresholdStep, resetting to default value");
            this.thresholdStep = thresholdStepDefault;
            return false;
        }
        return true;
    }

    public float getMinThreshold() {
        return minThreshold;
    }

    public boolean setMinThreshold(float minThreshold) {
        if(minThreshold>=0 && minThreshold<=255)
            this.minThreshold = minThreshold;
        else {
            logError("error setting minThreshold, resetting thresholds to default values");
            this.minThreshold = minThresholdDefault;
            this.maxThreshold = maxThresholdDefault;
            return false;
        }
        return true;
    }

    public float getMaxThreshold() {
        return maxThreshold;
    }

    public boolean setMaxThreshold(float maxThreshold) {
        if(maxThreshold>=0 && maxThreshold<=255)
            this.maxThreshold = maxThreshold;
        else{
            logError("error setting maxThreshold, resetting thresholds to default values");
            this.minThreshold = minThresholdDefault;
            this.maxThreshold = maxThresholdDefault;
            return false;
        }
        return true;
    }

    public int getMinRepeatability() {
        return minRepeatability;
    }

    public boolean setMinRepeatability(int minRepeatability) {
        if(minRepeatability>=0)
            this.minRepeatability = minRepeatability;
        else {
            logError("error setting minRepeatability, resetting to default value");
            this.minRepeatability = minRepeatabilityDefault;
            return false;
        }
        return true;
    }

    public float getMinDistBetweenBlobs() {
        return minDistBetweenBlobs;
    }

    public boolean setMinDistBetweenBlobs(float minDistBetweenBlobs) {
        if(minDistBetweenBlobs>=0)
            this.minDistBetweenBlobs = minDistBetweenBlobs;
        else {
            logError("error setting minDistBetweenBlobs, resetting to default value");
            this.minDistBetweenBlobs = minDistBetweenBlobsDefault;
            return false;
        }
        return true;
    }

    public int getFilterByColor() {
        return filterByColor;
    }

    public boolean setFilterByColor(int filterByColor) {
        if(filterByColor==0 || filterByColor==1)
            this.filterByColor = filterByColor;
        else{
            logError("error setting filterByColor, resetting filterByColor to default values");
            this.filterByColor = filterByColorDefault;
            this.blobColor = blobColorDefault;
            return false;
        }
        return true;
    }

    public int getBlobColor() {
        return blobColor;
    }

    public boolean setBlobColor(int blobColor) {
        if(blobColor>=0 && blobColor<=255)
            this.blobColor = blobColor;
        else{
            logError("error setting blobColor, resetting filterByColor to default values");
            this.filterByColor = filterByColorDefault;
            this.blobColor = blobColorDefault;
            return false;
        }
        return true;
    }

    public int getFilterByArea() {
        return filterByArea;
    }

    public boolean setFilterByArea(int filterByArea) {
        if(filterByArea==0 || filterByArea==1)
            this.filterByArea = filterByArea;
        else{
            logError("error setting filterByArea, resetting filterByArea to default values");
            this.filterByArea = filterByAreaDefault;
            this.minArea = minAreaDefault;
            this.maxArea = maxAreaDefault;
            return false;
        }
        return true;
    }

    public float getMinArea() {
        return minArea;
    }

    public boolean setMinArea(float minArea) {
        if(minArea>=0)
            this.minArea = minArea;
        else {
            logError("error setting minArea, resetting filterByArea to default values");
            this.filterByArea = filterByAreaDefault;
            this.minArea = minAreaDefault;
            this.maxArea = maxAreaDefault;
            return false;
        }
        return true;
    }

    public float getMaxArea() {
        return maxArea;
    }

    public boolean setMaxArea(float maxArea) {
        if(maxArea>=0)
            this.maxArea = maxArea;
        else {
            logError("error setting maxArea, resetting filterByArea to default values");
            this.filterByArea = filterByAreaDefault;
            this.minArea = minAreaDefault;
            this.maxArea = maxAreaDefault;
            return false;
        }
        return true;
    }

    public int getFilterByCircularity() {
        return filterByCircularity;
    }

    public boolean setFilterByCircularity(int filterByCircularity) {
        if(filterByCircularity==0 || filterByCircularity==1)
            this.filterByCircularity = filterByCircularity;
        else{
            logError("error setting filterByCircularity, resetting filterByCircularity to default values");
            this.filterByCircularity = filterByCircularityDefault;
            this.minCircularity = minCircularityDefault;
            this.maxCircularity = maxCircularityDefault;
            return false;
        }
        return true;
    }

    public float getMinCircularity() {
        return minCircularity;
    }

    public boolean setMinCircularity(float minCircularity) {
        if(minCircularity>=0 && minCircularity<=1)
            this.minCircularity = minCircularity;
        else{
            logError("error setting minCircularity, resetting filterByCircularity to default values");
            this.filterByCircularity = filterByCircularityDefault;
            this.minCircularity = minCircularityDefault;
            this.maxCircularity = maxCircularityDefault;
            return false;
        }
        return true;
    }

    public float getMaxCircularity() {
        return maxCircularity;
    }

    public boolean setMaxCircularity(float maxCircularity) {
        if(maxCircularity>=0 && maxCircularity<=1)
            this.maxCircularity = maxCircularity;
        else{
            logError("error setting maxCircularity, resetting filterByCircularity to default values");
            this.filterByCircularity = filterByCircularityDefault;
            this.minCircularity = minCircularityDefault;
            this.maxCircularity = maxCircularityDefault;
            return false;
        }
        return true;
    }

    public int getFilterByInertia() {
        return filterByInertia;
    }

    public boolean setFilterByInertia(int filterByInertia) {
        if(filterByInertia==0 || filterByInertia==1)
            this.filterByInertia = filterByInertia;
        else{
            logError("error setting filterByInertia, resetting filterByInertia to default values");
            this.filterByInertia = filterByInertiaDefault;
            this.minInertiaRatio = minInertiaRatioDefault;
            this.maxInertiaRatio = maxInertiaRatioDefault;
            return false;
        }
        return true;
    }

    public float getMinInertiaRatio() {
        return minInertiaRatio;
    }

    public boolean setMinInertiaRatio(float minInertiaRatio) {
        if(minInertiaRatio>=0 && minInertiaRatio<=1)
            this.minInertiaRatio = minInertiaRatio;
        else{
            logError("error setting minInertiaRatio, resetting filterByInertia to default values");
            this.filterByInertia = filterByInertiaDefault;
            this.minInertiaRatio = minInertiaRatioDefault;
            this.maxInertiaRatio = maxInertiaRatioDefault;
            return false;
        }
        return true;
    }

    public float getMaxInertiaRatio() {
        return maxInertiaRatio;
    }

    public boolean setMaxInertiaRatio(float maxInertiaRatio) {
        if(maxInertiaRatio>=0 && maxInertiaRatio<=1)
            this.maxInertiaRatio = maxInertiaRatio;
        else{
            logError("error setting maxInertiaRatio, resetting filterByInertia to default values");
            this.filterByInertia = filterByInertiaDefault;
            this.minInertiaRatio = minInertiaRatioDefault;
            this.maxInertiaRatio = maxInertiaRatioDefault;
            return false;
        }
        return true;
    }

    public int getFilterByConvexity() {
        return filterByConvexity;
    }

    public boolean setFilterByConvexity(int filterByConvexity) {
        if(filterByConvexity==0 || filterByConvexity==1)
            this.filterByConvexity = filterByConvexity;
        else{
            logError("error setting filterByConvexity, resetting filterByConvexity to default values");
            this.filterByConvexity = filterByConvexityDefault;
            this.minConvexity = minConvexityDefault;
            this.maxConvexity = maxConvexityDefault;
            return false;
        }
        return true;
    }

    public float getMinConvexity() {
        return minConvexity;
    }

    public boolean setMinConvexity(float minConvexity) {
        if(minConvexity>=0 && minConvexity<=1)
            this.minConvexity = minConvexity;
        else{
            logError("error setting minConvexity, resetting filterByConvexity to default values");
            this.filterByConvexity = filterByConvexityDefault;
            this.minConvexity = minConvexityDefault;
            this.maxConvexity = maxConvexityDefault;
            return false;
        }
        return true;
    }

    public float getMaxConvexity() {
        return maxConvexity;
    }

    public boolean setMaxConvexity(float maxConvexity) {
        if(maxConvexity>=0 && maxConvexity<=1)
            this.maxConvexity = maxConvexity;
        else{
            logError("error setting maxConvexity, resetting filterByConvexity to default values");
            this.filterByConvexity = filterByConvexityDefault;
            this.minConvexity = minConvexityDefault;
            this.maxConvexity = maxConvexityDefault;
            return false;
        }
        return true;
    }

    public String getParamsFilePath() {
        return paramsFilePath;
    }

    private void logDebug(String message) {distanceCalculatorService.logDebug(message);}

    private void logError(String message) {distanceCalculatorService.logError(message);}

    private void logStackTrace(Exception e) {distanceCalculatorService.logStackTrace(e);}
}
