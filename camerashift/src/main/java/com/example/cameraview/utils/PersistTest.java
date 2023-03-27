package com.example.cameraview.utils;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.opencv.core.Point;

import static org.junit.jupiter.api.Assertions.assertEquals;

class PersistTest {
    private static Point testPoint;
    static Persist persist;

    @BeforeAll
    static void setUp() {
        persist = new Persist();
        testPoint = new Point(1, 20);
    }

    @Test
    void saveResult() {
        Persist.saveResult(testPoint);
    }

    @Test
    void readTheResult() {
        Point result = Persist.readTheResult();
        assertEquals(result.y, 20);
        assertEquals(result.x, 1);
    }
}