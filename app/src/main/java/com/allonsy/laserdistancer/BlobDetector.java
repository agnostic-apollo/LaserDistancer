package com.allonsy.laserdistancer;

import android.os.Handler;

import org.opencv.android.OpenCVLoader;
import org.opencv.core.KeyPoint;
import org.opencv.core.Mat;
import org.opencv.core.MatOfByte;
import org.opencv.core.MatOfKeyPoint;
import org.opencv.core.Point;
import org.opencv.features2d.FeatureDetector;
import org.opencv.imgcodecs.Imgcodecs;

import java.io.File;
import java.util.Date;
import java.util.List;

public class BlobDetector {

    private DistanceCalculatorService distanceCalculatorService;
    private OpenCVParametersUtil openCVParametersUtil;
    private FeatureDetector blobDetector;
    private String paramsFilePath;
    private Handler distanceCalculatorServiceHandler;
    private String blobImageFilePath;
    private String blobImageFolderPath;
    private Point leftLaserCords;
    private Mat firstImageMatrix;
    private Mat secondImageMatrix;
    private MatOfKeyPoint firstImageBlobKeyPoints;
    private MatOfKeyPoint secondImageBlobKeyPoints;
    private int firstImageWidth = 0;
    private int firstImageHeight = 0;
    private double estimatedDistance = 0;
    private double pixelChangeAfterTwoDegreeChange = 0;
    private static double estimatedDistanceLinearGraphGradient = 1;


    public BlobDetector(DistanceCalculatorService distanceCalculatorService)
    {
        this.distanceCalculatorService = distanceCalculatorService;
        openCVParametersUtil = new OpenCVParametersUtil(distanceCalculatorService);
        blobImageFolderPath = null;
    }

    public boolean start() {

        try {
            if (!OpenCVLoader.initDebug()) {
                logError("Error initializing openCV");
                return false;
            }
            blobDetector = FeatureDetector.create(FeatureDetector.SIMPLEBLOB);

            if(!setOpenCVParameters()){
                logError("Error setting OpenCV Parameters");
                return false;
            }

            blobImageFilePath="";
        }
        catch (Exception e)
        {
            Logger.logStackTrace(e);
            return false;
        }
       return true;
    }

    public void storeFirstImage(byte[] image) {

        firstImageMatrix = Imgcodecs.imdecode(new MatOfByte(image), Imgcodecs.CV_LOAD_IMAGE_GRAYSCALE);
        firstImageWidth = firstImageMatrix.cols();
        firstImageHeight = firstImageMatrix.rows();
    }

    public List<KeyPoint> storeSecondImageAndDetectBlobs(byte[] image) {

        try {
            logDebug("inside storeSecondImageAndDetectBlobs");
            secondImageMatrix = Imgcodecs.imdecode(new MatOfByte(image), Imgcodecs.CV_LOAD_IMAGE_GRAYSCALE);
            if(secondImageMatrix.cols()!=firstImageWidth || secondImageMatrix.rows()!=firstImageHeight)
            {
                logError("Image of different resolution then first image, cant continue");
                return  null;
            }

            if(!findAndSetCorrectParameters())
                return (new MatOfKeyPoint()).toList();  //return 0 blobs detected



            findEstimatedDistance(); //calculate estimated distance here
            createBlobImageFile(secondImageMatrix, secondImageBlobKeyPoints);
            setLeftLaserCoords(firstImageBlobKeyPoints.toList());

            return discardBlobsIfAlignedOnTheYaxis(secondImageBlobKeyPoints.toList());

        }catch(Exception e)
        {
            logError(e.getMessage());
            return null;
        }
    }


    public List<KeyPoint> detectBlobs(byte[] image)
    {
        try {
            Mat imageMatrix = Imgcodecs.imdecode(new MatOfByte(image), Imgcodecs.CV_LOAD_IMAGE_GRAYSCALE);
            if(imageMatrix.cols()!=firstImageWidth || imageMatrix.rows()!=firstImageHeight)
            {
                logError("Image of different resolution then first image, cant continue");
                return  null;
            }

            MatOfKeyPoint blobKeyPoints = new MatOfKeyPoint();
            //logDebug("detecting blob started");
            blobDetector.detect(imageMatrix, blobKeyPoints);
            //logDebug("blob detected");

            createBlobImageFile(imageMatrix, blobKeyPoints);


            return discardBlobsIfAlignedOnTheYaxis(blobKeyPoints.toList());

        }catch(Exception e)
        {
            logError(e.getMessage());
            return null;
        }
    }

    private boolean findAndSetCorrectParameters() {

        firstImageBlobKeyPoints = new MatOfKeyPoint();
        blobDetector.detect(firstImageMatrix, firstImageBlobKeyPoints);

        secondImageBlobKeyPoints = new MatOfKeyPoint();
        blobDetector.detect(firstImageMatrix, secondImageBlobKeyPoints);

        if(firstImageBlobKeyPoints.toList().size()!=2 && secondImageBlobKeyPoints.toList().size()!=2) {
            //decrease min circularity from default(0.85) to 0.60 until center blobs are seen
            for (int i=0;i<5;i++) {
                openCVParametersUtil.setMinCircularity(openCVParametersUtil.getMinCircularity()-0.05f);
                if(!setOpenCVParameters()){
                    logError("Error setting OpenCV Parameters");
                    return false;
                }

                firstImageBlobKeyPoints = new MatOfKeyPoint();
                blobDetector.detect(firstImageMatrix, firstImageBlobKeyPoints);

                secondImageBlobKeyPoints = new MatOfKeyPoint();
                blobDetector.detect(firstImageMatrix, secondImageBlobKeyPoints);

                logDebug(String.valueOf(firstImageBlobKeyPoints.toList().size()) + "," + String.valueOf(secondImageBlobKeyPoints.toList().size()) + " blobs detected at circularity " + String.valueOf(openCVParametersUtil.getMinCircularity()));

                if(firstImageBlobKeyPoints.toList().size()>=2 && secondImageBlobKeyPoints.toList().size()>=2) {
                    if(checkIfBothLaserBlobsAreDetected(firstImageBlobKeyPoints.toList()) &&  checkIfBothLaserBlobsAreDetected(secondImageBlobKeyPoints.toList()))
                    {
                        logDebug("final circularity set at " + String.valueOf(openCVParametersUtil.getMinCircularity()));
                        break;
                    }
                }
            }
        }


        //decrease area range from default(50-400) until center blobs are seen
        boolean centerBlobsDetectedLast=false;
        for (int i=0;i<10;i++) {
            if(checkIfBothLaserBlobsAreDetected(firstImageBlobKeyPoints.toList()) &&  checkIfBothLaserBlobsAreDetected(secondImageBlobKeyPoints.toList()))
            {
                centerBlobsDetectedLast=true;
            }
            else
                centerBlobsDetectedLast=false;

            openCVParametersUtil.setMinArea(openCVParametersUtil.getMinArea()+10);
            openCVParametersUtil.setMaxArea(openCVParametersUtil.getMaxArea()-20);
            if(!setOpenCVParameters()){
                logError("Error setting OpenCV Parameters");
                return false;
            }

            firstImageBlobKeyPoints = new MatOfKeyPoint();
            blobDetector.detect(firstImageMatrix, firstImageBlobKeyPoints);

            secondImageBlobKeyPoints = new MatOfKeyPoint();
            blobDetector.detect(firstImageMatrix, secondImageBlobKeyPoints);

            logDebug(String.valueOf(firstImageBlobKeyPoints.toList().size()) + "," + String.valueOf(secondImageBlobKeyPoints.toList().size()) + " blobs detected at min,max area " +
                    String.valueOf(openCVParametersUtil.getMinArea())  + "," + String.valueOf(openCVParametersUtil.getMaxArea()));

            if(firstImageBlobKeyPoints.toList().size()<2 && secondImageBlobKeyPoints.toList().size()<2 && centerBlobsDetectedLast) { //if blobs disappear
                openCVParametersUtil.setMinArea(openCVParametersUtil.getMinArea()-10);
                openCVParametersUtil.setMaxArea(openCVParametersUtil.getMaxArea()+20);
                if(!setOpenCVParameters()){
                    logError("Error setting OpenCV Parameters");
                    return false;
                }
                firstImageBlobKeyPoints = new MatOfKeyPoint();
                blobDetector.detect(firstImageMatrix, firstImageBlobKeyPoints);

                secondImageBlobKeyPoints = new MatOfKeyPoint();
                blobDetector.detect(firstImageMatrix, secondImageBlobKeyPoints);

                logDebug("final min,max area set at " + String.valueOf(openCVParametersUtil.getMinArea())  + "," + String.valueOf(openCVParametersUtil.getMaxArea()));
                break;
            }
        }




        if(firstImageBlobKeyPoints.toList().size()==2 && secondImageBlobKeyPoints.toList().size()==2 &&
                checkIfBothLaserBlobsAreDetected(firstImageBlobKeyPoints.toList()) &&  checkIfBothLaserBlobsAreDetected(secondImageBlobKeyPoints.toList())) {
            return true;
        }
        else
            return false;

    }
    private boolean checkIfBothLaserBlobsAreDetected(List<KeyPoint> blobKeyPointsList) {
        if (blobKeyPointsList == null || blobKeyPointsList.size() < 2)
            return false;
        else {

            for (int i = 0; i != blobKeyPointsList.size() - 1; i++) {
                KeyPoint keyPoint1 = blobKeyPointsList.get(i);
                if(ifBlobIsInCenterOfImage(keyPoint1)) {
                    for (int j = i + 1; j != blobKeyPointsList.size(); j++) {
                        KeyPoint keyPoint2 = blobKeyPointsList.get(j);

                        double keypointsYCoordDifference = Math.abs(keyPoint1.pt.y - keyPoint2.pt.y);
                        double blobsSizeDifference = Math.abs(keyPoint1.size - keyPoint2.size);
                        double blobsSizeDifferenceRange = keyPoint1.size*0.2; //20% size difference allowed

                        //difference in blob sizes should not be greater than blobsSizeDifferenceRange
                        //difference in y axis is 10 times the diameter of blob
                        //should reevaluate these after testing
                        if (keypointsYCoordDifference <= keyPoint1.size * 10
                                && keyPoint2.size <= keyPoint1.size+blobsSizeDifferenceRange
                                     && keyPoint2.size >= keyPoint1.size-blobsSizeDifferenceRange) {
                            logDebug("Both Laser Blobs detected");
                          return true;
                        }
                    }
                }
            }
        }
        return false;
    }

    private boolean ifBlobIsInCenterOfImage(KeyPoint blob) {
        int offset = (int)((firstImageHeight*0.2) + (firstImageHeight*0.1)); //within 20% of the (center+10%)since lasers are a bit higher
        int max = (firstImageHeight/2)+ offset;
        int min = (firstImageHeight/2) - offset;

        if(blob.pt.y>=min && blob.pt.y<=max)
            return true;
        else {
            logDebug("Blob at " + String.valueOf(blob.pt.x) + ", " + String.valueOf(blob.pt.y) + " not in center");
            return false;
        }

    }

    private List<KeyPoint> discardBlobsIfAlignedOnTheYaxis(List<KeyPoint> blobKeyPointsList) {
        if(blobKeyPointsList==null || blobKeyPointsList.size()!=2)
            return blobKeyPointsList;
        else
        {
            KeyPoint keyPoint1 = blobKeyPointsList.get(0);
            KeyPoint keyPoint2 = blobKeyPointsList.get(1);

            double keypointsXCoordDifference = Math.abs(keyPoint1.pt.x - keyPoint2.pt.x);
            double keypointsYCoordDifference = Math.abs(keyPoint1.pt.y - keyPoint2.pt.y);

            //difference in x axis equal to diameter of blob
            //difference in y axis is 10 times the diameter of blob
            //should reevaluate these after testing
            if(keypointsXCoordDifference<=keyPoint1.size && keypointsYCoordDifference<=keyPoint1.size*10) {
                logDebug("Removing blob2 since lasers are alighned at y axis");
                blobKeyPointsList.remove(1);
            }

            return blobKeyPointsList;
        }
    }

    public double getEstimatedeDistance()
    {
        return estimatedDistance;
    }

    public boolean setDefaultOpenCVParameters(){
        openCVParametersUtil.setDefaultParameters();
        if(!setOpenCVParameters()){
            logError("Error setting OpenCV Parameters");
            return false;
        }

        return true;
    }

    public boolean setOpenCVParameters()
    {
        try {

            if(!openCVParametersUtil.writeOpenCVParametersToFile())
                return false;

            blobDetector.read(openCVParametersUtil.getParamsFilePath());

            if(blobImageFolderPath!=null){
                String parameterFilePath = blobImageFolderPath + File.separator + ("param_" + new Date(System.currentTimeMillis())).replaceAll(" ", "_").replaceAll(":", "_") + ".xml";
                blobDetector.write(parameterFilePath);
            }
        }
        catch (Exception e)
        {
            Logger.logStackTrace(e);
            return false;
        }
        return true;

    }

    private void createBlobImageFile(Mat imageMatrix, MatOfKeyPoint blobKeyPoints)
    {
        int minFreeSpace= 200;//dont record if space in external storage is less than 200mb

        //Set output file
        if(blobImageFolderPath==null){
            logError("Output Directory to store Blob Image does not exists");
            return;
        }

        long freespace= new File(blobImageFolderPath).getFreeSpace() / 1024;
        if(freespace<minFreeSpace)
        {
            logError("Space less than "+ String.valueOf(freespace) + "mb on device, free space = " + String.valueOf(freespace) +"mb");
            return;
        }

        blobImageFilePath = blobImageFolderPath + File.separator + ("IMG_COPENCV_" + new Date(System.currentTimeMillis())).replaceAll(" ", "_").replaceAll(":", "_") + ".jpeg";


        Mat blobMatrix= new Mat();
        org.opencv.core.Scalar cores = new org.opencv.core.Scalar(0,0,255);
        org.opencv.features2d.Features2d.drawKeypoints(imageMatrix, blobKeyPoints, blobMatrix, cores, org.opencv.features2d.Features2d.DRAW_RICH_KEYPOINTS );

        Imgcodecs.imwrite(blobImageFilePath, blobMatrix);

    }

    private boolean setLeftLaserCoords(List<KeyPoint> keyPoints)
    {
        leftLaserCords = null;
        if(keyPoints!=null && keyPoints.size()==2) {
            //find the fixed left laser
            if (keyPoints.get(0).pt.x < keyPoints.get(1).pt.x) {
                leftLaserCords = keyPoints.get(0).pt;
            } else {
                leftLaserCords = keyPoints.get(1).pt;
            }
            logDebug("Left Laser Initial Coords " + String.valueOf(leftLaserCords.x) + ", " + String.valueOf(leftLaserCords.y));
            return true;
        }
        else
        {
            logError("Called setLeftLaserCoords when size of keyPoints is not 2");
            return false;
        }
    }

    private void findEstimatedDistance() {
        List<KeyPoint> firstImageKeyPointsList = firstImageBlobKeyPoints.toList();
        List<KeyPoint> secondImageKeyPointsList = secondImageBlobKeyPoints.toList();

        double rightLaserFirstImageXCoord;
        double rightLaserSecondImageXCoord;

        leftLaserCords = null;
        if(firstImageKeyPointsList!=null && firstImageKeyPointsList.size()==2 &&
                secondImageKeyPointsList!=null && secondImageKeyPointsList.size()==2) {
            //find right laser xcoord in first image
            if (firstImageKeyPointsList.get(0).pt.x < firstImageKeyPointsList.get(1).pt.x) {
                rightLaserFirstImageXCoord = firstImageKeyPointsList.get(0).pt.x;
            } else {
                rightLaserFirstImageXCoord = firstImageKeyPointsList.get(1).pt.x;
            }

            //find right laser xcoord in second image
            if (secondImageKeyPointsList.get(0).pt.x < secondImageKeyPointsList.get(1).pt.x) {
                rightLaserSecondImageXCoord = secondImageKeyPointsList.get(0).pt.x;
            } else {
                rightLaserSecondImageXCoord = secondImageKeyPointsList.get(1).pt.x;
            }
            pixelChangeAfterTwoDegreeChange = Math.abs(rightLaserFirstImageXCoord-rightLaserSecondImageXCoord);
            estimatedDistance = estimatedDistanceLinearGraphGradient * pixelChangeAfterTwoDegreeChange;


            logDebug("pixelChangeAfterTwoDegreeChange =  " + String.valueOf(pixelChangeAfterTwoDegreeChange));
            estimatedDistance = 0; //since not ready yet
        }
        else
        {
            logError("Called findEstimatedDistance when size of keyPoints is not 2");
        }
    }

    public int findLeftLasersKeypoint(KeyPoint keyPoint1,KeyPoint keyPoint2)
    {
        Point leftLaserCoords = getLeftLaserCords();
        double keypoint1sXCoordDifference = Math.abs(leftLaserCoords.x - keyPoint1.pt.x);
        double keypoint1sYCoordDifference = Math.abs(leftLaserCoords.y - keyPoint1.pt.y);

        double keypoint2sXCoordDifference = leftLaserCoords.x - keyPoint2.pt.x;
        double keypoint2sYCoordDifference = leftLaserCoords.y - keyPoint2.pt.y;

        if(keypoint1sXCoordDifference<keypoint2sXCoordDifference && keypoint1sYCoordDifference<keypoint2sYCoordDifference)
            return 0;
        else
            return 1;
    }

    public void setBlobImageFolderPath(String blobImageFolderPath) {this.blobImageFolderPath = blobImageFolderPath;}

    public Point getLeftLaserCords() {return leftLaserCords;}

    public String getBlobImageFilePath() {
        return blobImageFilePath;
    }

    public double getPixelChangeAfterTwoDegreeChange() {return pixelChangeAfterTwoDegreeChange;}

    public void setHandler( Handler handler) {
        distanceCalculatorServiceHandler = handler;
    }

    private void logDebug(String message) {distanceCalculatorService.logDebug(message);}

    private void logError(String message) {distanceCalculatorService.logError(message);}

    private void logStackTrace(Exception e) {distanceCalculatorService.logStackTrace(e);}

     /*
    private void sendUpdateImageIntentToActivity(String filePath)
    {
        if(distanceCalculatorServiceHandler!=null) {
            Message msg = new Message();
            Bundle bundle = new Bundle();
            bundle.putString(DistanceCalculatorService.EXTRA_IMAGE_PATH, filePath);
            bundle.putString(DistanceCalculatorService.SENDER, DistanceCalculatorService.SENDER_BLOB_DETECTOR);
            msg.setData(bundle);
            distanceCalculatorServiceHandler.sendMessage(msg);
        }
    }
    */
}
