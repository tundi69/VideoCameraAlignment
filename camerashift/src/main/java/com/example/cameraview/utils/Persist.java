package com.example.cameraview.utils;

import com.example.cameraview.Result;
import org.opencv.core.Point;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;

public class Persist {


    public static void saveResult(Point bestPointToSave) {

        double pointX = bestPointToSave.x;
        double pointY = bestPointToSave.y;
        Result result = new Result((int) pointX, (int) pointY);
        // define the path to file, serialize the object, save the ByteStream, close the resource
        try {
            final Path of = Path.of("camerashift/src/main/resources/fileToStore.dat");
            ObjectOutputStream output = new ObjectOutputStream(Files.newOutputStream(of));
            output.writeObject(result);
            output.close();
        } catch (IOException ioe) {
            System.err.println("error saving the file");
        }
    }

    public static Point readTheResult() {

        Result readResult = null;
        Point po = null;
        try {
            ObjectInputStream input = new ObjectInputStream(new FileInputStream("camerashift/src/main/resources/fileToStore.dat"));
            readResult = (Result) input.readObject();
            po = new Point(readResult.getCoordinateX(), readResult.getCoordinateY());
            input.close();
        } catch (IOException | ClassNotFoundException ioe) {
            System.err.println("error opening file");
        }
        return po;
    }
}
