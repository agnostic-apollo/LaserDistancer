package com.allonsy.laserdistancer;


import org.opencv.core.KeyPoint;
import org.opencv.core.Point;

import java.util.List;


public class LaserUtil {

    private DistanceCalculatorService distanceCalculatorService;
    private BlobDetector blobDetector;
    private static float DISTANCE_BETWEEN_LASERS = 0.8562992f; //26.1cm
    private static int laserAngleOffset = 0;
    private static int laserInitialAngle = 0;
    private static int laserInitialAngleChange = 2;
    private int laserAngle = 0;
    private boolean laserMovedLeftLast = true;
    private int angleChangeCount=0;

    public LaserUtil(DistanceCalculatorService distanceCalculatorService, BlobDetector blobDetector)
    {
        this.distanceCalculatorService = distanceCalculatorService;
        this.blobDetector = blobDetector;
    }

    public int resetAndGetInitialAngle()
    {
        angleChangeCount = 1;
        laserAngle = laserInitialAngle+laserAngleOffset;
        return laserAngle;
    }

    public float calculateDistance()
    {
        return (float)(DISTANCE_BETWEEN_LASERS * Math.tan( Math.toRadians(90-(laserAngle)))) ;
    }

    public int calculateEstimatedFinalAngle(double estimatedDistance)
    {
        return 90-((int) Math.toDegrees(Math.atan(estimatedDistance/DISTANCE_BETWEEN_LASERS))) ;
    }

    public int getInitialAngleChange() {
        angleChangeCount++;
        laserAngle=laserAngle+laserInitialAngleChange;
        return laserAngle;
    }

    public int calculateNewAngle(List<KeyPoint> keyPoints)
    {

        if(keyPoints!=null && keyPoints.size()==2) {
            angleChangeCount++;
            if(angleChangeCount==3) //if angle is at the third change
            {
                laserMovedLeftLast = true;
                double estimatedDistance = blobDetector.getEstimatedeDistance();
                if(estimatedDistance>0) { //if blob detector already calculated the estimated distance move directly close to it
                    laserAngle = calculateEstimatedFinalAngle(estimatedDistance);
                    return laserAngle;
                }
            }

            KeyPoint keyPoint1 = keyPoints.get(0);
            KeyPoint keyPoint2 = keyPoints.get(1);

            int i = blobDetector.findLeftLasersKeypoint(keyPoint1,keyPoint2);
            logDebug("Left Laser Keypoints found at " + String.valueOf(keyPoints.get(i).pt.x) + ", " + String.valueOf(keyPoints.get(i).pt.y));
            double leftLasersXCoord = keyPoints.get(i).pt.x;

            double rightLasersXCoord;
            if(i==0)
                rightLasersXCoord = keyPoints.get(1).pt.x;
            else
                rightLasersXCoord = keyPoints.get(0).pt.x;

            if(leftLasersXCoord < rightLasersXCoord)
            {
                laserAngle=laserAngle+2;
                if(laserAngle>90) {
                    logDebug("Restarting measurement since laserAngle>90");
                    distanceCalculatorService.restartMeasurement();
                    laserAngle = resetAndGetInitialAngle();
                }
                laserMovedLeftLast = true;
                return laserAngle;
            }

            else{
                laserAngle=laserAngle-2;
                if(laserAngle<1){
                    logDebug("Restarting measurement since laserAngle<1");
                    distanceCalculatorService.restartMeasurement();
                    laserAngle = resetAndGetInitialAngle();
                }
                laserMovedLeftLast = false;
                return laserAngle;
            }

        }
        else
        {
            logError("Called calculateDistance when size of keyPoints is not 2");
            distanceCalculatorService.stopService();
            return 0;
        }

    }



    private void logDebug(String message) {distanceCalculatorService.logDebug(message);}

    private void logError(String message) {distanceCalculatorService.logError(message);}

    private void logStackTrace(Exception e) {distanceCalculatorService.logStackTrace(e);}

}
