package com.couriertracker.test;

import com.couriertracker.pattern.strategy.HaversineDistanceStrategy;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class HaversineDistanceStrategyTest {

    private final HaversineDistanceStrategy strategy = new HaversineDistanceStrategy();

    @Test
    void shouldReturnZeroForIdenticalCoordinates() {
        assertEquals(0.0, strategy.calculateDistanceMeters(40.0, 29.0, 40.0, 29.0));
    }

    @Test
    void shouldReturnPositiveSymmetricDistance() {
        double forward = strategy.calculateDistanceMeters(40.9923307, 29.1244229, 40.986106, 29.1161293);
        double reverse = strategy.calculateDistanceMeters(40.986106, 29.1161293, 40.9923307, 29.1244229);

        assertTrue(forward > 0.0);
        assertEquals(forward, reverse, 0.000001);
        assertTrue(forward > 900.0 && forward < 1100.0);
    }
}
