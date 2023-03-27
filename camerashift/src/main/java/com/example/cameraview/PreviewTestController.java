package com.example.cameraview;

import com.example.cameraview.utils.Persist;
import com.example.cameraview.utils.Utils;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.geometry.Rectangle2D;
import javafx.scene.control.Button;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import org.opencv.core.Mat;
import org.opencv.core.Point;
import org.opencv.videoio.VideoCapture;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.opencv.videoio.Videoio.CAP_DSHOW;

public class PreviewTestController {
    @FXML
    public Button startCamera1;
    @FXML
    public ImageView previewTestImageView;
    @FXML
    public Button stopCamera2;
    @FXML
    public Button stopCamera1;
    @FXML
    public Button readAlignmentResult;
    @FXML
    public Button startCamera2;


    private VideoCapture capture = new VideoCapture();
    private VideoCapture capture2 = new VideoCapture();
    // a flag to change the button behavior
    private boolean cameraActive = false;
    // the id of the camera to be used
    private static int camera1Id = 0;
    private static int camera2Id = 1;
    private Mat frame;
    private static final AtomicBoolean running2 = new AtomicBoolean(false);
    private double rectX;
    private double rectY;
    private Thread th1;
    private Point resultPoint;
    private Thread th2;
    private CountDownLatch latch;
    private double rectX2;
    private double rectY2;
    private int step2 = 0;
    private int quantity = 20;
    private Mat frame2;
    private static final AtomicBoolean running1 = new AtomicBoolean(false);


    @FXML
    protected void startCamera(ActionEvent event)  {

        if (capture.isOpened() || capture2.isOpened()){
            return;
        }
        latch = new CountDownLatch(1);
        capture2.open(camera2Id, CAP_DSHOW);
        running2.set(true);
        running1.set(true);

        th1 = new Thread(new Runnable() {
            int step = 1;
            @Override
            public void run() {
                do {
                    //grab and process a single frame
                    frame = new Mat();

                    // check if the capture is open
                    if (capture2.isOpened()) {
                        try {
                            capture2.read(frame);

                            if (resultPoint != null) {
                                Rectangle2D rectangle2D = new Rectangle2D(rectX * step, rectY * step,
                                        640 - ((640 * Math.abs(resultPoint.y) / (480 * quantity) * step)), 480 - (Math.abs(resultPoint.y) * step) / quantity);
                                previewTestImageView.setViewport(rectangle2D);
                                step = step + 1;
                                // when the zoom finished, thread notify the other, release the capture of the camera and step out of the loop
                                if (step == quantity) {
                                    step2 = 0;
                                    Thread.sleep(80);
                                    latch.countDown();
                                    running1.set(false);
                                    capture2.release();
                                    return;
                                }
                            }
                        } catch (Exception e) {
                            // log the error
                            System.err.println("Exception during the image elaboration: " + e);
                        }
                    }
                    // convert and show the frame
                    Image imageToShow = Utils.mat2Image(frame);
                    updateImageView(previewTestImageView, imageToShow);
                }
                while (running1.get());
            }

        }, "Camera2Thread1");
        th1.start();

        th2 = new Thread(new Runnable() {

            boolean firstLoop = true;
            @Override
            public void run() {
                try {
                    capture.open(camera1Id, CAP_DSHOW);
                    latch.await();
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
                do {
                    //grab and process a single frame
                    frame2 = new Mat();

                    // check if the capture is open
                    if (capture.isOpened()) {
                        try {
                            // we don't want to use the first (dark) frame
                            if (firstLoop){
                            capture.read(frame2);
                            firstLoop = false;}

                             // calculating the viewport, depending on if the coordinates are bigger or smaller than 0
                            capture.read(frame2);
                                if (resultPoint != null) {
                                    //good for -- coordinate
                                    if (resultPoint.x < 0 && resultPoint.y < 0) {
                                        Rectangle2D rectangle2D = new Rectangle2D(rectX2 - (Math.abs(resultPoint.x) / quantity) * step2,
                                                rectY2 - (Math.abs(resultPoint.y) / quantity) * step2,
                                                ((640 - (640 * Math.abs(resultPoint.y) / 480)) + (640 * Math.abs(resultPoint.y)) / (480 * quantity) * step2),
                                                ((480 - Math.abs(resultPoint.y)) + (Math.abs(resultPoint.y) / quantity) * step2));
                                        previewTestImageView.setViewport(rectangle2D);
                                    }
                                    //good for +-
                                    if (resultPoint.x > 0 && resultPoint.y < 0) {
                                        Rectangle2D rectangle2D = new Rectangle2D(rectX2,
                                                rectY2 - (Math.abs(resultPoint.y) / quantity) * step2,
                                                ((640 - (640 * Math.abs(resultPoint.y) / 480)) + (640 * Math.abs(resultPoint.y)) / (480 * quantity) * step2),
                                                ((480 - Math.abs(resultPoint.y)) + (Math.abs(resultPoint.y) / quantity) * step2));
                                        previewTestImageView.setViewport(rectangle2D);
                                    }
                                    //good for ++
                                    if (resultPoint.x > 0 && resultPoint.y > 0) {
                                        Rectangle2D rectangle2D = new Rectangle2D(rectX2,
                                                rectY2,
                                                ((640 - (640 * Math.abs(resultPoint.y) / 480)) + (640 * Math.abs(resultPoint.y)) / (480 * quantity) * step2),
                                                ((480 - Math.abs(resultPoint.y)) + (Math.abs(resultPoint.y) / quantity) * step2));
                                        previewTestImageView.setViewport(rectangle2D);
                                    }
                                    //good for -+
                                    if (resultPoint.x < 0 && resultPoint.y > 0) {
                                        Rectangle2D rectangle2D = new Rectangle2D(rectX2 - (Math.abs(resultPoint.x) / quantity) * step2,
                                                rectY2,
                                                (640 + (Math.abs(resultPoint.x) / quantity) * step2),
                                                ((480 - Math.abs(resultPoint.y)) + (Math.abs(resultPoint.y) / quantity) * step2));
                                        previewTestImageView.setViewport(rectangle2D);
                                    }
                                }
                        } catch (Exception e) {
                            // log the error
                            System.err.println("Exception during the image elaboration: " + e);
                        }
                        // convert and show the frame
                        Image imageToShow = Utils.mat2Image(frame2);
                        updateImageView(previewTestImageView, imageToShow);
                        step2 = step2 + 1;
                    }
                    if (step2 >= quantity+1) {
                        previewTestImageView.setViewport(null);
                        resultPoint = null;
                    }
                }
                while (running2.get());
            }
        }, "Camera1Thread2");
        th2.start();
    }
     //Stop the acquisition from the camera and release all the resources
    private void stopAcquisition(VideoCapture anyCapture) throws InterruptedException {

        if (anyCapture.isOpened()) {
            capture.release();
            capture2.release();}
        if (latch != null)
        {
        latch.countDown();}
        running2.set(false);
        running1.set(false);
        if (th1 != null) {
        th1.join();}
        if (th2 != null){
        th2.join();}
    }
    //Update the {@link ImageView} in the JavaFX main thread
    private void updateImageView(ImageView view, Image image) {
        Utils.onFXThread(view.imageProperty(), image);
    }
    protected void setClosed2() throws InterruptedException {
       // latch.countDown();
        this.stopAcquisition(capture);
        this.stopAcquisition(capture2);
    }
    public void stopCamera(ActionEvent actionEvent) throws InterruptedException {
        if (capture.isOpened() || capture2.isOpened()){
       //latch.countDown();
       this.stopAcquisition(capture2);
       this.stopAcquisition(capture);}
    }
    public void readAlignmentResult(ActionEvent actionEvent) {

        resultPoint = Persist.readTheResult();

        if (resultPoint.x > 0 && resultPoint.y > 0) {
            rectX = (Math.abs(resultPoint.x) / quantity);
            rectY = (Math.abs(resultPoint.y) / quantity);
            rectX2 = 0;
            rectY2 = 0;
        }
        if (resultPoint.x < 0 && resultPoint.y < 0) {
            rectX = 0;
            rectY = 0;
            rectX2 = Math.abs(resultPoint.x);
            rectY2 = Math.abs(resultPoint.y);
        }
        if (resultPoint.x < 0 && resultPoint.y > 0) {
            rectX = 0;
            rectY = (Math.abs(resultPoint.y) / quantity);
            rectX2 = Math.abs(resultPoint.x);
            rectY2 = 0;
        }
        if (resultPoint.x > 0 && resultPoint.y < 0) {
            rectX = (Math.abs(resultPoint.x) / quantity);
            rectY = 0;
            rectX2 = 0;
            rectY2 = Math.abs(resultPoint.y);
        }
    }
}


