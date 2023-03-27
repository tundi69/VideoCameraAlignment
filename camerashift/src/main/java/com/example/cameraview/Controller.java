package com.example.cameraview;

import com.example.cameraview.utils.Persist;
import com.example.cameraview.utils.Utils;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.geometry.Rectangle2D;
import javafx.scene.chart.PieChart;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.Slider;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import org.opencv.calib3d.Calib3d;
import org.opencv.core.*;
import org.opencv.features2d.BFMatcher;
import org.opencv.features2d.DescriptorMatcher;
import org.opencv.features2d.Features2d;
import org.opencv.features2d.ORB;
import org.opencv.imgproc.Imgproc;
import org.opencv.videoio.VideoCapture;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.opencv.videoio.Videoio.CAP_DSHOW;

public class Controller {

    @FXML
    public Button StartButton;
    @FXML
    public Button StopButton;
    @FXML
    public TextField keypointSubset;
    @FXML
    public TextField goodResultThr;
    @FXML
    public Slider confidence;
    @FXML
    public Slider reprojectError;
    @FXML
    public Button saveResult;
    @FXML
    private ImageView currentFrame;
    @FXML
    private ImageView currentFrame2;
    @FXML
    private ImageView currentFrame3;
    @FXML
    private Label resultDisplay;
    @FXML
    private Label otherGoodResultDisplay;
    @FXML
    private PieChart pieChart;

    private VideoCapture capture = new VideoCapture();
    private VideoCapture capture2 = new VideoCapture();
    private static int camera1Id = 0;
    private static int camera2Id = 1;
    private static final int CAMERA_1_HEIGHT = 375;
    private static final int CAMERA_1_WIDTH = 501;
    private static final Object syncObject = new Object();
    private Mat dst;
    private Mat frame2;
    private double AverageKeypointX1;
    private double AverageKeypointY1;
    private double AverageKeypointX2;
    private double AverageKeypointY2;
    private static final AtomicBoolean running = new AtomicBoolean(false);
    private Thread t1;
    private Thread t2;
    private Thread t3;
    private static final int ORIGINAL_CENTERPOINT_X = 320;
    private static final int ORIGINAL_CENTERPOINT_Y = 240;
    private CountDownLatch latch;
    private Image imageToShow2;
    int startPointX = 0;
    int startPointY = 0;
    private MatOfKeyPoint matOfKeyPoints1;
    private MatOfKeyPoint matOfKeyPoints2;
    private MatOfKeyPoint matOfKeyPoints;
    private Mat descriptor;
    private Mat descriptor1;
    private Mat descriptor2;
    private List<MatOfDMatch> matches = new ArrayList<MatOfDMatch>();
    private MatOfDMatch better_matches_mat;
    private LinkedList<DMatch> good_matches;
    private CountDownLatch latch3;
    private CountDownLatch latch4;
    private List<Point> betterPTS1;
    private List<Point> betterPTS2;
    private Mat frame1;
    private int selectedQuantity;
    private Thread timerThread;
    private List<Point> startPoints = new ArrayList<>();
    private LinkedList<DMatch> better_matches;
    private int declaredGood;
    private double confidenceValue;
    private int reprojectErrorValue;
    private Point bestPointToSave;


    @FXML
    protected void startCameras(ActionEvent event) {

        if (capture.isOpened() || capture2.isOpened()) {
            return;
        }
        if (keypointSubset.getText().matches("^[0-9]{2,4}$")) {
            selectedQuantity = Integer.parseInt(keypointSubset.getText());
        } else {
            keypointSubset.setText("wrong input");
            return;
        }
        if (goodResultThr.getText().matches("^[0-9]{2,3}$")) {
            declaredGood = Integer.parseInt(goodResultThr.getText());
        } else {
            goodResultThr.setText("wrong input");
            return;
        }
        confidenceValue = confidence.getValue() / 10 - 0.000001;
        System.out.println(confidenceValue);
        reprojectErrorValue = (int) reprojectError.getValue();
        System.out.println(reprojectErrorValue);

        capture.open(camera1Id, CAP_DSHOW);
        capture2.open(camera2Id, CAP_DSHOW);

        running.set(true);
        startPoints = new ArrayList<>();

        timerThread = new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    //Let the calibration run for half minute)
                    Thread.sleep(30000);
                    running.set(false);
                    capture.release();
                    capture2.release();
                    //giving threads time to finish the given loop, before reading fields for data
                    Thread.sleep(100);
                    showCalibrationResult();

                } catch (InterruptedException e) {
                    //throw new RuntimeException(e);
                }
            }
        }, "timerThread");
        timerThread.start();
        t1 = new Thread(new Runnable() {

            @Override
            public void run() {

                do {
                    frame1 = new Mat();
                    if (capture.isOpened()) {
                        try {
                            capture.read(frame1);
                            if (!frame1.empty()) {
                                Imgproc.cvtColor(frame1, frame1, Imgproc.COLOR_BGR2GRAY);
                            }
                        } catch (Exception e) {
                            // log the error
                            System.err.println("Exception during the image elaboration: " + e);
                        }
                    }
                    synchronized (syncObject) {

                        detectKeyPoints(frame1);
                    }
                    matOfKeyPoints1 = matOfKeyPoints;

                    descriptor1 = descriptor;
                    latch3.countDown();

                    try {
                        latch4.await();
                    } catch (InterruptedException e) {
                        throw new RuntimeException(e);
                    }
                    synchronized (syncObject) {
                        if (betterPTS1 != null) {
                            drawKeyPoints(frame1, (int) AverageKeypointX1, (int) AverageKeypointY1, matOfKeyPoints1, betterPTS1);
                            Image imageToShow = Utils.mat2Image(dst);
                            updateImageView(currentFrame, imageToShow);
                        }
                    }
                } while (running.get());
                System.out.println("Exiting camera1 thread");
            }
        }, "Camera1Thread");
        t1.start();

        t2 = new Thread(new Runnable() {

            @Override
            public void run() {
                do {
                    latch = new CountDownLatch(1);
                    latch3 = new CountDownLatch(2);

                    frame2 = new Mat();

                    if (capture2.isOpened()) {
                        try {
                            capture2.read(frame2);

                            if (!frame2.empty()) {
                                Imgproc.cvtColor(frame2, frame2, Imgproc.COLOR_BGR2GRAY);
                            }
                        } catch (Exception e) {
                            // log the error
                            System.err.println("Exception during the image elaboration: " + e);
                        }
                    }
                    synchronized (syncObject) {
                        detectKeyPoints(frame2);
                    }
                    matOfKeyPoints2 = matOfKeyPoints;

                    descriptor2 = descriptor;

                    latch3.countDown();

                    try {
                        latch4.await();
                    } catch (InterruptedException e) {
                        throw new RuntimeException(e);
                    }

                    synchronized (syncObject) {
                        if (betterPTS2 != null) { //&& frame2 != null && matOfKeyPoints2 != null){
                            drawKeyPoints(frame2, (int) AverageKeypointX2, (int) AverageKeypointY2, matOfKeyPoints2, betterPTS2);
                            imageToShow2 = Utils.mat2Image(dst);
                        }

                        updateImageView(currentFrame2, imageToShow2);
                        latch.countDown();
                    }
                } while (running.get());
                System.out.println("Exiting camera2 thread");
            }
        }, "Camera2Thread");
        t2.start();

        t3 = new Thread(new Runnable() {

            @Override
            public void run() {
                do {
                    latch4 = new CountDownLatch(1);
                    // myMonitorObject.doWait(myMonitorObject);
                    try {
                        latch3.await();
                        if (matOfKeyPoints1.toList().size() > 2 && matOfKeyPoints2.toList().size() > 2) {
                            //long start1 = System.currentTimeMillis();
                            detectMatchingKeypoints();
                             //long end1 = System.currentTimeMillis();
                             //System.out.println("Elapsed Time in milliseconds: "+ (end1-start1));
                        }
                        latch4.countDown();
                        latch.await();
                        if (AverageKeypointX1 != 0 && AverageKeypointY1 != 0 && AverageKeypointX2 != 0 && AverageKeypointY2 != 0) {
                            displayImagePart();
                            updateImageView(currentFrame3, imageToShow2);
                        }
                    } catch (InterruptedException e) {
                        throw new RuntimeException(e);
                    }
                } while (running.get());
                System.out.println("Exiting AdjCam thread");
            }
        }, "AdjCamThread");
        t3.start();
    }

    private void showCalibrationResult() throws InterruptedException {

        int mostFrequentPointAmount = 0;
        Point bestPoint = null;
        int sumOfGoodStartPointAmount = 0;
        ArrayList<Point> stillGoodPoints = new ArrayList<>();

        // hash set is created and elements of
        // list are inserted into it
        Set<Point> st = new HashSet<Point>(startPoints);

        for (Point s : st) {

            if (Collections.frequency(startPoints, s) > declaredGood) {
                stillGoodPoints.add(s);
                if (Collections.frequency(startPoints, s) > mostFrequentPointAmount) {
                    mostFrequentPointAmount = Collections.frequency(startPoints, s);
                    bestPoint = s;
                }
                sumOfGoodStartPointAmount = sumOfGoodStartPointAmount + Collections.frequency(startPoints, s);
            }
        }

        bestPointToSave = bestPoint;
        String text = bestPoint + ": " + (Collections.frequency(startPoints, bestPoint)) + " out of " + startPoints.size();

        updateLabel(resultDisplay, text);

        stillGoodPoints.remove(bestPoint);
        updateLabel(otherGoodResultDisplay, "");

        if (stillGoodPoints.size() != 0) {

            for (int i = 0; i < stillGoodPoints.size(); i++) {
                String text2 = (stillGoodPoints.get(i) + ": " + (Collections.frequency(startPoints, stillGoodPoints.get(i))));
                // give time to display the text before get it in the next loop
                Thread.sleep(100);
                //add more text to the existing one in every loop
                updateLabel(otherGoodResultDisplay, otherGoodResultDisplay.getText() + System.lineSeparator() + text2);
            }
        }
        ObservableList<PieChart.Data> slices = FXCollections.observableArrayList();
        PieChart.Data slice = new PieChart.Data("frequent data", sumOfGoodStartPointAmount);
        PieChart.Data badResult = new PieChart.Data("sum of rare data", startPoints.size() - sumOfGoodStartPointAmount);

        slices.add(slice);
        slices.add(badResult);
        updateChart(pieChart, slices);
    }

    public void detectMatchingKeypoints() throws InterruptedException {

        BFMatcher matcher = BFMatcher.create(DescriptorMatcher.BRUTEFORCE_HAMMING, false);
        matcher.knnMatch(descriptor1, descriptor2, matches, 2);

        // ratio test LOWE
        good_matches = new LinkedList<DMatch>();
        for (Iterator<MatOfDMatch> iterator = matches.iterator(); iterator.hasNext(); ) {
            MatOfDMatch matOfDMatch = (MatOfDMatch) iterator.next();

            if (matOfDMatch.toArray()[0].distance / matOfDMatch.toArray()[1].distance < 0.9) {
                good_matches.add(matOfDMatch.toArray()[0]);
            }
        }
        // get keypoint coordinates of good matches to find homography and remove outliers using ransac
        List<Point> pts1 = new ArrayList<>();
        List<Point> pts2 = new ArrayList<>();

        for (int i = 0; i < good_matches.size(); i++) {
            pts1.add(matOfKeyPoints1.toList().get(good_matches.get(i).queryIdx).pt);
            pts2.add(matOfKeyPoints2.toList().get(good_matches.get(i).trainIdx).pt);
        }
        // convertion of data types
        Mat outputMask = new Mat();
        MatOfPoint2f pts1Mat = new MatOfPoint2f();
        pts1Mat.fromList(pts1);
        MatOfPoint2f pts2Mat = new MatOfPoint2f();
        pts2Mat.fromList(pts2);

        if (pts1Mat.toList().size() < 4 || pts2Mat.toList().size() < 4) {
            return;
        }
        // Find homography - here just used to perform match filtering with RANSAC, but could be used to e.g. stitch images
        // the smaller the allowed reprojection error, the more matches are filtered
        Mat Homog = Calib3d.findHomography(pts1Mat, pts2Mat, Calib3d.RANSAC, reprojectErrorValue, outputMask, 2000, confidenceValue);

        // outputMask contains zeros and ones indicating which matches are filtered
        better_matches = new LinkedList<DMatch>();
        for (int i = 0; i < good_matches.size(); i++) {
            if (outputMask.get(i, 0)[0] != 0.0) {
                better_matches.add(good_matches.get(i));
            }
        }
        double sumX1 = 0.0;
        double sumY1 = 0.0;
        double sumX2 = 0.0;
        double sumY2 = 0.0;

        if (better_matches != null) {
            for (int i = 0; i < better_matches.size(); i++) {

                sumX1 = sumX1 + matOfKeyPoints1.toList().get(better_matches.get(i).queryIdx).pt.x;
                sumY1 = sumY1 + matOfKeyPoints1.toList().get(better_matches.get(i).queryIdx).pt.y;
                sumX2 = sumX2 + matOfKeyPoints2.toList().get(better_matches.get(i).trainIdx).pt.x;
                sumY2 = sumY2 + matOfKeyPoints2.toList().get(better_matches.get(i).trainIdx).pt.y;
            }
            AverageKeypointX1 = sumX1 / better_matches.size();
            AverageKeypointY1 = sumY1 / better_matches.size();
            AverageKeypointX2 = sumX2 / better_matches.size();
            AverageKeypointY2 = sumY2 / better_matches.size();

            better_matches_mat = new MatOfDMatch();
            better_matches_mat.fromList(better_matches);

            betterPTS1 = new ArrayList<>();
            betterPTS2 = new ArrayList<>();

            for (int ind = 0; ind < better_matches.size(); ind++) {
                betterPTS1.add(matOfKeyPoints1.toList().get(better_matches.get(ind).queryIdx).pt);
                betterPTS2.add(matOfKeyPoints2.toList().get(better_matches.get(ind).trainIdx).pt);
            }
        }
    }

    private void detectKeyPoints(Mat frame) {

        dst = new Mat();

        matOfKeyPoints = new MatOfKeyPoint();
        descriptor = new Mat();
        Mat mask = new Mat();

        ORB orb = ORB.create(selectedQuantity);
        orb.detectAndCompute(frame, mask, matOfKeyPoints, descriptor);

        if (matOfKeyPoints.toList().size() == 0) {
            AverageKeypointX1 = 0;
            AverageKeypointY1 = 0;
            AverageKeypointX2 = 0;
            AverageKeypointY2 = 0;
        }

    }

    private void displayImagePart() throws InterruptedException {
        double xDifferency = Math.abs(AverageKeypointX2 - AverageKeypointX1);
        double yDifferency = Math.abs(AverageKeypointY2 - AverageKeypointY1);

        if (AverageKeypointX1 < AverageKeypointX2 && AverageKeypointY1 < AverageKeypointY2) {
            startPointX = (int) xDifferency;
            startPointY = (int) yDifferency;
        }
        if (AverageKeypointX1 > AverageKeypointX2 && AverageKeypointY1 < AverageKeypointY2) {
            startPointX = -(int) xDifferency;
            startPointY = (int) yDifferency;
        }
        if (AverageKeypointX1 > AverageKeypointX2 && AverageKeypointY1 > AverageKeypointY2) {
            startPointX = -(int) xDifferency;
            startPointY = -(int) yDifferency;
        }
        if (AverageKeypointX1 < AverageKeypointX2 && AverageKeypointY1 > AverageKeypointY2) {
            startPointX = (int) xDifferency;
            startPointY = -(int) yDifferency;
        }
        Point startPoint = new Point(startPointX, startPointY);
        startPoints.add(startPoint);
    }


    private void updateImageView(ImageView view, Image image) {

        Utils.onFXThread(view.imageProperty(), image);

        Rectangle2D rectangle2D = new Rectangle2D(startPointX, startPointY, 640, 480);
        currentFrame3.setViewport(rectangle2D);
    }

    private void updateLabel(Label label, String text) {

        Utils.onFXThread2(label.textProperty(), text);
    }

    private void updateChart(PieChart pieChart, List<PieChart.Data> slices) {

        Utils.onFXThread3(pieChart.dataProperty(), slices);
    }

    private void drawKeyPoints(Mat frame, int x, int y, MatOfKeyPoint matOfKeyPoints, List<Point> betterPTS) {

        /*Mat dst2 = new Mat();
        Features2d.drawMatches(frame1, matOfKeyPoints1, frame2, matOfKeyPoints2, better_matches_mat, dst2, new Scalar(255,0,0),new Scalar(0,0,255));
        HighGui.imshow("Feature Matching", dst2);
        HighGui.waitKey();*/

        //Drawing the detected key points (red)
        Features2d.drawKeypoints(frame, matOfKeyPoints, dst, new Scalar(0, 0, 255));

        // drawing the matching keypoints (blue)
        for (int ind = 0; ind < betterPTS.size(); ind++) {
            Imgproc.circle(dst, new Point((int) betterPTS.get(ind).x, (int) betterPTS.get(ind).y), 5, new Scalar(255, 0, 0), 2);
        }
        //drawing calculated average point (green)
        Imgproc.circle(dst, new Point((int) x, (int) y), 5, new Scalar(0, 255, 0), 2);

        //drawing image centerPoint (white)
        Imgproc.circle(dst, new Point(ORIGINAL_CENTERPOINT_X, ORIGINAL_CENTERPOINT_Y), 5, new Scalar(255, 255, 255), 2);
    }

    public void stopCameras(ActionEvent event) throws InterruptedException {
        this.stopAcquisition(capture2);
        this.stopAcquisition(capture);
    }

    private void stopAcquisition(VideoCapture anyCapture) throws InterruptedException {

        running.set(false);

        if (t1 != null) {
            t1.join();
        }
        if (t2 != null) {
            t2.join();
        }
        if (t3 != null) {
            t3.join();
        }
        if (timerThread != null) {
            timerThread.interrupt();
            //timerThread.join();
        }
        if (anyCapture.isOpened()) {
            // release the camera
            anyCapture.release();
        }
    }

    public void setClosed() throws InterruptedException {
        this.stopAcquisition(capture);
        this.stopAcquisition(capture2);
    }

    @FXML
    public void saveResult(ActionEvent event) throws InterruptedException, IOException {

        if (bestPointToSave != null) {
            Persist.saveResult(bestPointToSave);
            FXHelloCV.startNewStage();
        }
    }
}
