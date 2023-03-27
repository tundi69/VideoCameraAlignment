package com.example.cameraview.utils;

import javafx.application.Platform;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.StringProperty;
import javafx.collections.ObservableList;
import javafx.embed.swing.SwingFXUtils;
import javafx.scene.chart.PieChart;
import javafx.scene.image.Image;
import org.opencv.core.Mat;

import java.awt.image.BufferedImage;
import java.awt.image.DataBufferByte;
import java.util.List;

public final class Utils {
    /*
     * Convert a Mat object (OpenCV) in the corresponding Image for JavaFX
     *
     * @param frame the {@link Mat} representing the current frame
     * @return the {@link Image} to show
     */
    public static Image mat2Image(Mat frame) {
        try {
            return SwingFXUtils.toFXImage(matToBufferedImage(frame), null);
        } catch (Exception e) {
            System.err.println("Cannot convert the Mat object: " + e);
            return null;
        }
    }
    /*
     * Generic method for putting element running on a non-JavaFX thread on the
     * JavaFX thread, to properly update the UI
     *
     * @param property a {@link ObjectProperty}
     * @param value    the value to set for the given {@link ObjectProperty}
     */
    public static <T> void onFXThread(final ObjectProperty<T> property, final T value) {
        Platform.runLater(() -> {
            property.set(value);
            // System.out.println(property);
        });
    }

    /*
     * Support for the {@link mat2image()} method
     *
     * @param original
     *            the {@link Mat} object in BGR or grayscale
     * @return the corresponding {@link BufferedImage}
     */
    private static BufferedImage matToBufferedImage(Mat original) {
        // init
        BufferedImage image;
        int width = original.width(), height = original.height(), channels = original.channels();
        byte[] sourcePixels = new byte[width * height * channels];
        original.get(0, 0, sourcePixels);

        if (original.channels() > 1) {
            image = new BufferedImage(width, height, BufferedImage.TYPE_3BYTE_BGR);
        } else {
            image = new BufferedImage(width, height, BufferedImage.TYPE_BYTE_GRAY);
        }
        final byte[] targetPixels = ((DataBufferByte) image.getRaster().getDataBuffer()).getData();
        System.arraycopy(sourcePixels, 0, targetPixels, 0, sourcePixels.length);

        return image;
    }

    public static void onFXThread2(StringProperty Property, String text) {
        Platform.runLater(() -> {
            Property.set(text);
        });
    }

    public static void onFXThread3(ObjectProperty<ObservableList<PieChart.Data>> dataProperty, List<PieChart.Data> slices) {
        Platform.runLater(() -> {
            dataProperty.set((ObservableList<PieChart.Data>) slices);
        });
    }
}
