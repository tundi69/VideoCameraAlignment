package com.example.cameraview;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ResultTest {
    static Result result = null;

    @BeforeAll
    static void setUp() {
        result = new Result(8, 10);

    }

    @Test
    void getCoordinateX() {
        assertEquals(result.getCoordinateX(), 8);
    }

    @Test
    void setCoordinateX() {
    }

    @Test
    void getCoordinateY() {
        assertEquals(result.getCoordinateY(), 10);
    }

    @Test
    void setCoordinateY() {
    }
}